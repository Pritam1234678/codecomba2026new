package com.example.codecombat2026.proctoring.service;

import com.example.codecombat2026.proctoring.config.ProctoringConfig;
import com.example.codecombat2026.proctoring.entity.ProctoringEvent;
import com.example.codecombat2026.proctoring.entity.ProctoringSession;
import com.example.codecombat2026.proctoring.entity.RiskBand;
import com.example.codecombat2026.proctoring.repository.ProctoringEventRepository;
import com.example.codecombat2026.proctoring.repository.ProctoringSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns Risk_Weight_Config lookup, Risk_Band classification, the Valkey
 * hot counters that drive the live admin dashboard, and the deterministic
 * replay path used by admin-triggered rescore.
 *
 * <p>The engine is the single source of "what does this event do to the
 * score?" Higher layers ({@code ProctoringEventService}, the WebSocket
 * handler, the admin SSE bridge) only ever ask this class — the weight
 * table, the band thresholds, and the Valkey key layout are intentionally
 * private to this service so a future change (e.g. per-contest weights,
 * Lua-scripted INCRBY+CAS) does not bleed across the codebase.
 *
 * <p>Hot path layout matches {@code design.md}:
 * <ul>
 *   <li>{@code proctoring:session:{sid}:score} — STRING used as an
 *       {@code INCRBY} counter; the durable projection on
 *       {@code proctoring_sessions.risk_score} lags by ≤ 5 s
 *       (Req 18.1, 18.2).</li>
 *   <li>{@code proctoring:session:{sid}:band} — last computed band, set
 *       only on a transition so the WS handler can detect a band crossing
 *       without recomputing on every event (Req 12.5).</li>
 * </ul>
 *
 * <p>Failure model: every Valkey operation is wrapped so a transient cache
 * outage degrades scoring rather than aborting it. {@link #applyDelta}
 * falls back to a DB UPDATE when the {@code INCRBY} throws, mirroring the
 * "fail-open scoring continues against DB" contract from the design's
 * failure-mode table; on recovery, an admin-triggered {@link #rescore}
 * resyncs Valkey from the persisted event log.
 *
 * <p>Validates: Requirements 12.1, 12.2, 12.3, 12.4, 12.5, 12.6, 12.7,
 * 12.8, 18.1, 22.2, 22.3.
 */
@Service
public class RiskScoringEngine {

    private static final Logger log = LoggerFactory.getLogger(RiskScoringEngine.class);

    /** Hot-path key for the per-session running risk score. */
    private static final String SCORE_KEY_PREFIX = "proctoring:session:";
    private static final String SCORE_KEY_SUFFIX = ":score";
    /** Hot-path key for the per-session last-computed risk band. */
    private static final String BAND_KEY_SUFFIX = ":band";

    @Autowired private ProctoringConfig config;
    @Autowired private StringRedisTemplate redis;
    @Autowired private ProctoringEventRepository eventRepo;
    @Autowired private ProctoringSessionRepository sessionRepo;

    /**
     * Set of {@code event_type} values for which we have already emitted
     * the "unknown event type" warning during this JVM's lifetime. Backed
     * by {@link ConcurrentHashMap#newKeySet()} so the
     * {@link Set#add(Object)}-returns-true idiom is the warning latch
     * without a separate guard (Req 22.3 — exactly one log per (type, JVM)
     * tuple).
     */
    private final Set<String> warnedUnknownTypes = ConcurrentHashMap.newKeySet();

    /**
     * Outcome of an {@link #applyDelta} call. {@code newScore} is the
     * post-INCRBY total, {@code oldBand}/{@code newBand} bracket the
     * potential band transition, and {@code changed} is the precomputed
     * predicate the WS handler observes to decide whether to broadcast
     * {@code RISK_UPDATE} to the candidate and {@code RISK_BAND_CHANGED}
     * to the admin SSE channel (Req 12.5).
     */
    public record RiskUpdate(int newScore, RiskBand oldBand, RiskBand newBand, boolean changed) {
    }

    // ── Public surface ─────────────────────────────────────────────────────

    /**
     * Resolve the {@code score_delta} for an {@code event_type} via
     * {@link ProctoringConfig#getWeights()}. Unknown types resolve to
     * {@code 0} (so the event still persists — Req 22.2) and trigger
     * exactly one warning log per (type, JVM lifetime) tuple — Req 22.3.
     *
     * @param eventType the {@code proctoring_events.event_type} value
     * @return the configured weight, or {@code 0} for an unregistered type
     */
    public int weightFor(String eventType) {
        Integer weight = config.getWeights().get(eventType);
        if (weight == null) {
            if (warnedUnknownTypes.add(eventType)) {
                log.warn("Proctoring: unknown event_type '{}' — defaulting score_delta=0. " +
                        "Add 'proctoring.weights.{}=<int>' to register a weight.", eventType, eventType);
            }
            return 0;
        }
        return weight;
    }

    /**
     * Pure band classifier. Inclusive upper bounds: {@code score == lowMax}
     * is still LOW, {@code score == mediumMax} is still MEDIUM. The
     * function depends only on its argument and on
     * {@link ProctoringConfig#getBands()}; no I/O, no side effects, fully
     * deterministic — this is the property that lets {@link #rescore}
     * faithfully reconstruct truth from the event log (Req 12.7).
     *
     * @param score current {@code risk_score}
     * @return {@link RiskBand#LOW LOW}, {@link RiskBand#MEDIUM MEDIUM}, or {@link RiskBand#HIGH HIGH}
     */
    public RiskBand bandFor(int score) {
        ProctoringConfig.Bands bands = config.getBands();
        if (score <= bands.lowMax()) return RiskBand.LOW;
        if (score <= bands.mediumMax()) return RiskBand.MEDIUM;
        return RiskBand.HIGH;
    }

    /**
     * Apply a {@code score_delta} to the live Valkey counter and detect a
     * band crossing in one round-trip.
     *
     * <p>Path:
     * <ol>
     *   <li>{@code delta == 0} short-circuit: read the current score and
     *       band from Valkey (with DB fallback) and return a no-op
     *       {@link RiskUpdate} so callers can still emit a fresh
     *       {@code RISK_UPDATE} during admin-driven flows.</li>
     *   <li>Otherwise, {@code INCRBY proctoring:session:{sid}:score} by
     *       {@code delta}, read the previously-persisted band, compute
     *       {@link #bandFor} on the new score, and on a transition
     *       {@code SET proctoring:session:{sid}:band} to the new band.</li>
     * </ol>
     *
     * <p>Exception policy: any Valkey error short-circuits to a DB
     * UPDATE that recomputes {@code (score, band, flagged)} from the
     * persisted projection plus the delta — scoring continues, the cache
     * heals on the next admin {@link #rescore} or the scheduled flush
     * (Req 18.2).
     *
     * @param sessionId owning {@code proctoring_sessions.id}
     * @param delta     the {@code score_delta} to apply (may be 0, may be negative)
     * @return new score, old band, new band, and a {@code changed} flag
     */
    public RiskUpdate applyDelta(Long sessionId, int delta) {
        String scoreKey = SCORE_KEY_PREFIX + sessionId + SCORE_KEY_SUFFIX;
        String bandKey = SCORE_KEY_PREFIX + sessionId + BAND_KEY_SUFFIX;

        if (delta == 0) {
            int score = readScore(sessionId, scoreKey);
            RiskBand band = readBand(sessionId, bandKey);
            return new RiskUpdate(score, band, band, false);
        }

        Long incremented;
        try {
            incremented = redis.opsForValue().increment(scoreKey, delta);
        } catch (Exception ex) {
            log.warn("Proctoring: Valkey INCRBY failed for {} (delta={}); falling back to DB.",
                    scoreKey, delta, ex);
            return applyDeltaViaDb(sessionId, delta);
        }

        // Belt-and-braces: if the Lettuce client returned null (driver
        // edge case under reconnect) treat it the same as an exception.
        if (incremented == null) {
            log.warn("Proctoring: INCRBY returned null for {}; falling back to DB.", scoreKey);
            return applyDeltaViaDb(sessionId, delta);
        }

        int newScore = (incremented > Integer.MAX_VALUE) ? Integer.MAX_VALUE : incremented.intValue();
        RiskBand oldBand = readBand(sessionId, bandKey);
        RiskBand newBand = bandFor(newScore);

        if (newBand != oldBand) {
            try {
                redis.opsForValue().set(bandKey, newBand.name());
            } catch (Exception ex) {
                // Band SET failed: the score INCRBY already happened, so
                // we still report the transition. The next event or the
                // scheduled flush will resync the band key.
                log.warn("Proctoring: Valkey SET failed for {} (band transition {}→{}); will resync.",
                        bandKey, oldBand, newBand, ex);
            }
            return new RiskUpdate(newScore, oldBand, newBand, true);
        }
        return new RiskUpdate(newScore, oldBand, newBand, false);
    }

    /**
     * Persist a band/flagged transition to the DB.
     *
     * <p>Issued as a single conditional UPDATE
     * ({@code SET risk_band=:b, flagged=(b=='HIGH') WHERE id=:id})
     * so the band column and the derived {@code flagged} column cannot
     * tear under concurrent writers. Callers (typically
     * {@code ProctoringEventService} after observing
     * {@link RiskUpdate#changed}) invoke this exactly when a band
     * crossing has been detected (Req 12.5, 12.6).
     *
     * @param sessionId owning {@code proctoring_sessions.id}
     * @param band      new band to persist
     */
    public void persistBand(Long sessionId, RiskBand band) {
        sessionRepo.updateBandAndFlag(sessionId, band, band == RiskBand.HIGH);
    }

    /**
     * Recompute a session's score and band from the persisted event log
     * and rewrite both the DB row and the Valkey counters (Req 12.7,
     * 12.8). Determinism: the same ordered event log produces the same
     * {@code (score, band, flagged)} triple, regardless of how many
     * times this method is called.
     *
     * <p>Order of operations matters: DB write happens first so a Valkey
     * outage cannot leave the durable projection stale; the cache writes
     * are best-effort and a Valkey failure is logged but not propagated.
     *
     * @param sessionId owning {@code proctoring_sessions.id}
     * @return the recomputed {@code risk_score}
     */
    public int rescore(Long sessionId) {
        List<ProctoringEvent> events = eventRepo.findBySessionIdOrderByServerTimestampAsc(sessionId);
        int sum = 0;
        for (ProctoringEvent e : events) {
            sum += weightFor(e.getEventType());
        }
        RiskBand newBand = bandFor(sum);
        int updated = sessionRepo.updateScoreBandAndFlag(sessionId, sum, newBand, newBand == RiskBand.HIGH);
        if (updated == 0) {
            log.warn("Proctoring rescore: session {} not found or already ended — no rows updated.", sessionId);
        }

        String scoreKey = SCORE_KEY_PREFIX + sessionId + SCORE_KEY_SUFFIX;
        String bandKey = SCORE_KEY_PREFIX + sessionId + BAND_KEY_SUFFIX;
        try {
            redis.opsForValue().set(scoreKey, Integer.toString(sum));
            redis.opsForValue().set(bandKey, newBand.name());
        } catch (Exception ex) {
            log.warn("Proctoring: Valkey resync failed during rescore of session {}; DB is authoritative.",
                    sessionId, ex);
        }
        return sum;
    }

    // ── Internals ──────────────────────────────────────────────────────────

    /**
     * Read the current score from Valkey, falling back to the DB
     * projection on any cache failure (Valkey down, key evicted, value
     * non-numeric). Returning {@code 0} when neither source has data is
     * intentional — a brand-new session legitimately has no row in
     * either projection until its first event lands.
     */
    private int readScore(Long sessionId, String scoreKey) {
        try {
            String v = redis.opsForValue().get(scoreKey);
            if (v != null) {
                try {
                    return Integer.parseInt(v);
                } catch (NumberFormatException nfe) {
                    log.warn("Proctoring: non-numeric value at {} ('{}'); falling back to DB.", scoreKey, v);
                }
            }
        } catch (Exception ex) {
            log.warn("Proctoring: Valkey GET failed for {}; falling back to DB.", scoreKey, ex);
        }
        return sessionRepo.findById(sessionId)
                .map(ProctoringSession::getRiskScore)
                .orElse(0);
    }

    /**
     * Read the current band from Valkey, falling back to the DB
     * projection on any cache failure. Returns {@link RiskBand#LOW}
     * when nothing has been persisted yet.
     */
    private RiskBand readBand(Long sessionId, String bandKey) {
        try {
            String v = redis.opsForValue().get(bandKey);
            if (v != null) {
                try {
                    return RiskBand.valueOf(v);
                } catch (IllegalArgumentException iae) {
                    log.warn("Proctoring: invalid band value at {} ('{}'); falling back to DB.", bandKey, v);
                }
            }
        } catch (Exception ex) {
            log.warn("Proctoring: Valkey GET failed for {}; falling back to DB.", bandKey, ex);
        }
        return sessionRepo.findById(sessionId)
                .map(ProctoringSession::getRiskBand)
                .orElse(RiskBand.LOW);
    }

    /**
     * Cache-down fallback for {@link #applyDelta}: read the durable
     * score and band from Postgres, apply {@code delta} arithmetically,
     * and rewrite the durable projection in one UPDATE. Returning a
     * {@link RiskUpdate} with the new band lets the caller still observe
     * a transition even when the cache is unavailable.
     */
    private RiskUpdate applyDeltaViaDb(Long sessionId, int delta) {
        ProctoringSession s = sessionRepo.findById(sessionId).orElse(null);
        if (s == null) {
            // Session vanished mid-flight (force-end + delete race): the
            // safest answer is a no-op so the caller does not push a WS
            // frame to a session that no longer exists.
            return new RiskUpdate(0, RiskBand.LOW, RiskBand.LOW, false);
        }
        int oldScore = (s.getRiskScore() == null) ? 0 : s.getRiskScore();
        RiskBand oldBand = (s.getRiskBand() == null) ? RiskBand.LOW : s.getRiskBand();
        long raw = (long) oldScore + delta;
        int newScore = (raw > Integer.MAX_VALUE) ? Integer.MAX_VALUE
                     : (raw < 0)                ? 0
                     : (int) raw;
        RiskBand newBand = bandFor(newScore);
        sessionRepo.updateScoreBandAndFlag(sessionId, newScore, newBand, newBand == RiskBand.HIGH);
        return new RiskUpdate(newScore, oldBand, newBand, newBand != oldBand);
    }
}
