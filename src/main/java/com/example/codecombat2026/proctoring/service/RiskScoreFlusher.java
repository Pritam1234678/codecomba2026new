package com.example.codecombat2026.proctoring.service;

import com.example.codecombat2026.proctoring.entity.ProctoringSession;
import com.example.codecombat2026.proctoring.entity.RiskBand;
import com.example.codecombat2026.proctoring.repository.ProctoringSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Periodic Valkey → Postgres flusher for the live per-session risk
 * counters owned by {@link RiskScoringEngine}.
 *
 * <p>{@code RiskScoringEngine} writes every event's {@code score_delta}
 * into Valkey via {@code INCRBY proctoring:session:{sid}:score} so the
 * candidate WS push and the admin dashboard live grid never have to
 * round-trip through Postgres on the hot path. The durable projection on
 * {@code proctoring_sessions.risk_score}/{@code risk_band}/{@code flagged}
 * therefore lags the cache, and Req 18.1/18.2 caps that lag at five
 * seconds.
 *
 * <p>This class closes that gap. Every five seconds it walks every
 * active proctoring session (rows with {@code ended_at IS NULL}), reads
 * the current Valkey {@code :score} and {@code :band} keys, and — when
 * both are present and at least one differs from the DB row — issues a
 * single conditional UPDATE so the durable projection catches up. The
 * method body is wrapped in a top-level try/catch so a transient Valkey
 * or DB hiccup degrades to "no flush this tick" instead of poisoning the
 * Spring scheduler thread.
 *
 * <p>Why walk the DB rather than {@code ProctoringSessionRegistry.connectedSessionIds()}?
 * The registry only knows sessions whose WebSocket is currently bound to
 * <em>this JVM</em>. A session whose WS just dropped (mid-reconnect) can
 * still have stale Valkey counters that need flushing — and on a
 * single-instance MVP deployment the registry view and the DB-active set
 * are identical anyway. The active-rows-from-DB pattern stays correct
 * under future horizontal scaling without coordination, and the active
 * set is small (< 100 for the MVP target — Req 18.4) so the per-tick
 * SELECT is cheap.
 *
 * <p>Validates: Req 18.1, 18.2.
 */
@Component
public class RiskScoreFlusher {

    private static final Logger log = LoggerFactory.getLogger(RiskScoreFlusher.class);

    /** Valkey key prefix for the per-session score and band hot keys. */
    private static final String SCORE_KEY_PREFIX = "proctoring:session:";
    private static final String SCORE_KEY_SUFFIX = ":score";
    private static final String BAND_KEY_SUFFIX = ":band";

    @Autowired
    private ProctoringSessionRepository sessionRepo;

    @Autowired
    private StringRedisTemplate redis;

    /**
     * Walk active sessions and flush stale Valkey counters into the DB.
     *
     * <p>Schedule contract:
     * <ul>
     *   <li>{@code fixedDelay = 5_000} — five seconds between the end of
     *       one run and the start of the next. Combined with the cap on
     *       active sessions (≤ 100 for MVP), this keeps the durable
     *       projection within the five-second SLA from Req 18.2.</li>
     *   <li>{@code initialDelay = 5_000} — give the JVM a beat to warm
     *       up after boot before competing with startup work.</li>
     * </ul>
     *
     * <p>Failure model: every Valkey GET and every DB write is best-effort.
     * The outer try/catch ensures that an exception bubbling out of the
     * loop never propagates to the scheduler — the scheduler would
     * otherwise stop firing the {@code @Scheduled} method for the rest of
     * the JVM's lifetime, and a five-second cache hiccup must not break
     * the durable projection forever. On any failure path the durable
     * projection simply stays at its previous (within-five-second) state
     * and the next tick will retry.
     */
    @Scheduled(fixedDelay = 5_000, initialDelay = 5_000)
    public void flush() {
        int active = 0;
        int updated = 0;
        try {
            List<ProctoringSession> activeSessions = sessionRepo.findByEndedAtIsNull();
            active = activeSessions.size();

            for (ProctoringSession s : activeSessions) {
                if (flushOne(s)) {
                    updated++;
                }
            }

            if (active > 0) {
                log.debug("Proctoring risk-score flush: active={}, updated={}", active, updated);
            }
        } catch (Exception ex) {
            log.warn("Proctoring risk-score flush tick failed (active={}, updated={}): {}",
                    active, updated, ex.getMessage(), ex);
        }
    }

    /**
     * Flush a single session if Valkey holds fresher state than the DB
     * row. Returns {@code true} when an UPDATE was issued so the outer
     * loop can count it for the DEBUG log line.
     *
     * <p>Skip cases (each is the correct behaviour, not an error):
     * <ul>
     *   <li>Either Valkey key is missing — the cache hasn't seen any
     *       events for this session yet, so the DB is already truth.</li>
     *   <li>The cached score is non-numeric or the cached band is not a
     *       known {@link RiskBand} value — log at WARN and skip; the
     *       next admin {@code rescore} will resync.</li>
     *   <li>The cached values match the DB row — nothing to flush.</li>
     * </ul>
     *
     * <p>The actual write is the same single UPDATE used by
     * {@link RiskScoringEngine#rescore(Long)} — score, band, and the
     * derived {@code flagged} flag move atomically so an admin reading
     * the dashboard cannot observe a torn (band, flagged) pair.
     */
    private boolean flushOne(ProctoringSession s) {
        Long sid = s.getId();
        String scoreKey = SCORE_KEY_PREFIX + sid + SCORE_KEY_SUFFIX;
        String bandKey = SCORE_KEY_PREFIX + sid + BAND_KEY_SUFFIX;

        String rawScore;
        String rawBand;
        try {
            rawScore = redis.opsForValue().get(scoreKey);
            rawBand = redis.opsForValue().get(bandKey);
        } catch (Exception ex) {
            log.warn("Proctoring risk-score flush: Valkey GET failed for session {}: {}",
                    sid, ex.getMessage());
            return false;
        }

        // The "either missing" case is the steady state for a brand-new
        // session — no events yet, so cache and DB agree by construction.
        if (rawScore == null || rawBand == null) {
            return false;
        }

        int cachedScore;
        try {
            // Long parse first, then clamp into int range — the engine
            // clamps INCRBY at Integer.MAX_VALUE for the same reason.
            long parsed = Long.parseLong(rawScore);
            cachedScore = (parsed > Integer.MAX_VALUE) ? Integer.MAX_VALUE
                        : (parsed < Integer.MIN_VALUE) ? Integer.MIN_VALUE
                        : (int) parsed;
        } catch (NumberFormatException nfe) {
            log.warn("Proctoring risk-score flush: non-numeric score at {} ('{}'); skipping.",
                    scoreKey, rawScore);
            return false;
        }

        RiskBand cachedBand;
        try {
            cachedBand = RiskBand.valueOf(rawBand);
        } catch (IllegalArgumentException iae) {
            log.warn("Proctoring risk-score flush: invalid band at {} ('{}'); skipping.",
                    bandKey, rawBand);
            return false;
        }

        int dbScore = (s.getRiskScore() == null) ? 0 : s.getRiskScore();
        RiskBand dbBand = (s.getRiskBand() == null) ? RiskBand.LOW : s.getRiskBand();
        boolean dbFlagged = Boolean.TRUE.equals(s.getFlagged());
        boolean cachedFlagged = (cachedBand == RiskBand.HIGH);

        if (cachedScore == dbScore && cachedBand == dbBand && cachedFlagged == dbFlagged) {
            return false;
        }

        try {
            sessionRepo.updateScoreBandAndFlag(sid, cachedScore, cachedBand, cachedFlagged);
            return true;
        } catch (DataAccessException dae) {
            log.warn("Proctoring risk-score flush: DB UPDATE failed for session {}: {}",
                    sid, dae.getMessage());
            return false;
        }
    }
}
