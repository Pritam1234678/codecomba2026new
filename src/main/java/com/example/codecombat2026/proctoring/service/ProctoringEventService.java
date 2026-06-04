package com.example.codecombat2026.proctoring.service;

import com.example.codecombat2026.proctoring.entity.ProctoringEvent;
import com.example.codecombat2026.proctoring.entity.ProctoringSession;
import com.example.codecombat2026.proctoring.event.ProctoringRiskBandChangedEvent;
import com.example.codecombat2026.proctoring.exception.ProctoringNotFoundException;
import com.example.codecombat2026.proctoring.repository.ProctoringEventRepository;
import com.example.codecombat2026.proctoring.repository.ProctoringSessionRepository;
import com.example.codecombat2026.proctoring.service.RiskScoringEngine.RiskUpdate;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

/**
 * Core inbound proctoring-event ingest path (Req 9.5, 9.6, 11.2, 11.3,
 * 12.4, 12.7, 14.1, 22.2, 22.3).
 *
 * <p>The ingest flow is fixed by {@code design.md} and runs in this
 * order for every accepted frame (real-time or replayed):
 * <ol>
 *   <li>{@link RiskScoringEngine#weightFor(String)} → resolve
 *       {@code score_delta} for the {@code event_type} (zero for
 *       unknown types, with a single warning log per type per JVM —
 *       Req 22.2, 22.3).</li>
 *   <li>If {@code replayed}, run the soft dedup check
 *       {@code SETNX proctoring:dedup:{sid}:{ts_ms}:{type} 1 EX 600}.
 *       If the key already exists, the second replay is forensically
 *       persisted but with {@code score_delta = 0} so the live counter
 *       does not double-score (Req 11.2, 11.3, 12.7).</li>
 *   <li>INSERT a row into {@code proctoring_events} with the resolved
 *       {@code scoreDelta} (effective delta after dedup), the raw
 *       payload as {@code jsonb}, and {@code replayed} set
 *       accordingly. The newly-generated id is what we hand back to
 *       the caller (so the screenshot upload path can FK to it —
 *       Req 14.1).</li>
 *   <li>{@link RiskScoringEngine#applyDelta(Long, int)} — Valkey
 *       {@code INCRBY} on the live counter, with a DB fallback baked
 *       into the engine.</li>
 *   <li>If a band crossing was returned,
 *       {@link RiskScoringEngine#persistBand(Long, com.example.codecombat2026.proctoring.entity.RiskBand)}
 *       and publish a {@link ProctoringRiskBandChangedEvent} so the
 *       admin SSE bridge (task 10.1) can fan out. The candidate-side
 *       WebSocket push (task 5.3) plugs in here once
 *       {@code ProctoringSessionRegistry} integration lands.</li>
 *   <li>Return the inserted {@code event_id}.</li>
 * </ol>
 *
 * <p>Transaction boundary: the entire ingest is run inside a single
 * {@link Transactional} so the INSERT and the {@code persistBand}
 * UPDATE commit together. The Valkey ops ({@code INCRBY}, {@code SETNX})
 * intentionally live outside the JPA contract — they are best-effort
 * projections and the engine handles their failure modes internally.
 *
 * <p>Validates: Requirements 9.5, 9.6, 11.2, 11.3, 12.4, 12.7, 14.1,
 * 22.2, 22.3.
 */
@Service
public class ProctoringEventService {

    private static final Logger log = LoggerFactory.getLogger(ProctoringEventService.class);

    /**
     * Soft dedup window for replayed frames — design.md, Valkey key
     * {@code proctoring:dedup:{sid}:{ts}:{type}}. 600 s comfortably
     * covers the 60 s {@code maxOfflineSeconds} ceiling from Req 11.5
     * with an order-of-magnitude headroom for clock skew.
     */
    private static final Duration DEDUP_TTL = Duration.ofSeconds(600);
    private static final String DEDUP_KEY_PREFIX = "proctoring:dedup:";

    /**
     * Empty-payload sentinel — we still write a valid {@code jsonb}
     * value when callers omit the payload so the Postgres column
     * predicate can rely on the column being non-null in JSON terms.
     */
    private static final String EMPTY_JSON_OBJECT = "{}";

    private final ProctoringEventRepository eventRepo;
    private final ProctoringSessionRepository sessionRepo;
    private final RiskScoringEngine riskEngine;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    public ProctoringEventService(ProctoringEventRepository eventRepo,
                                  ProctoringSessionRepository sessionRepo,
                                  RiskScoringEngine riskEngine,
                                  StringRedisTemplate redis,
                                  ObjectMapper objectMapper,
                                  ApplicationEventPublisher eventPublisher) {
        this.eventRepo = eventRepo;
        this.sessionRepo = sessionRepo;
        this.riskEngine = riskEngine;
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Inbound suspicious-event frame, decoupled from the WebSocket
     * transport so unit/property tests can drive the ingest path
     * directly. The shape mirrors the JSON envelope sent by the
     * candidate frontend: {@code event_type}, {@code client_timestamp},
     * a free-form JSON {@code payload} that varies per type, and an
     * optional browser-generated {@code client_correlation_id}
     * (UUID) used by the replay dedup path.
     *
     * @param eventType            one of the documented suspicious-event types
     * @param clientTimestamp      client-side wall clock when the event fired
     *                             (used both for forensic ordering and for
     *                             the soft Valkey dedup key)
     * @param payload              arbitrary JSON-shaped payload; may be
     *                             {@code null} or empty
     * @param clientCorrelationId  browser-generated UUID round-tripped
     *                             through the offline buffer; used by the
     *                             hard DB-level replay dedup path (Req 11.2,
     *                             11.3). May be {@code null} for synthetic
     *                             server-side events (e.g.
     *                             {@code HEARTBEAT_TIMEOUT}).
     */
    public record EventFrame(String eventType,
                             LocalDateTime clientTimestamp,
                             Map<String, Object> payload,
                             String clientCorrelationId) {

        /**
         * Backward-compatible factory for callers that do not yet supply a
         * correlation id (e.g. the synthetic {@code HEARTBEAT_TIMEOUT}
         * event the WS handler emits on a heartbeat-timeout). New code
         * should prefer the canonical 4-arg constructor.
         */
        public EventFrame(String eventType,
                          LocalDateTime clientTimestamp,
                          Map<String, Object> payload) {
            this(eventType, clientTimestamp, payload, null);
        }
    }

    /**
     * Persist {@code frame} for {@code sessionId}, apply the score
     * delta, and (on band crossing) persist+broadcast the transition.
     *
     * <p>Replay dedup runs in two layers:
     * <ol>
     *   <li><b>Hard, durable layer</b> (Req 11.2, 11.3): when
     *       {@code replayed} is {@code true} and the frame carries a
     *       {@code client_correlation_id}, the service looks up an
     *       existing row by {@code (session_id, client_correlation_id)};
     *       on hit it returns the existing {@code event_id} <em>without</em>
     *       inserting and <em>without</em> applying any score delta. The
     *       matching partial unique index (V8 migration) backs this
     *       contract at the DB level so a racing duplicate replay still
     *       fails closed instead of double-counting.</li>
     *   <li><b>Soft, best-effort layer</b> (legacy fallback): when no
     *       correlation id is supplied — or the soft Valkey dedup key
     *       was already claimed by an earlier replay — the row is still
     *       persisted forensically but with {@code score_delta = 0} so
     *       the running counter cannot double-score (Req 11.3, 12.7).</li>
     * </ol>
     *
     * @param sessionId owning {@code proctoring_sessions.id}
     * @param frame     decoded inbound event frame
     * @param replayed  {@code true} for frames replayed from the
     *                  candidate's offline buffer (Req 11.3)
     * @return the inserted {@code proctoring_events.id} — or, on a
     *         correlation-id replay hit, the existing row's id
     */
    @Transactional
    public Long ingest(Long sessionId, EventFrame frame, boolean replayed) {
        // ── 0. Hard replay dedup by client_correlation_id ──────────────────
        // Browser stamps a UUID at original capture time and round-trips it
        // through IndexedDB; on a replayed frame we look up an existing row
        // by (session_id, client_correlation_id) and return its id without
        // any further side-effects. This is the contractual idempotency
        // boundary for the offline-buffer flow (Req 11.2, 11.3) — the soft
        // Valkey dedup below is the legacy fallback for frames captured
        // before the client started stamping correlation ids.
        if (replayed && frame.clientCorrelationId() != null && !frame.clientCorrelationId().isBlank()) {
            var existing = eventRepo.findFirstBySessionIdAndClientCorrelationId(
                    sessionId, frame.clientCorrelationId());
            if (existing.isPresent()) {
                return existing.get().getId();
            }
        }

        // ── 1. Weight lookup ───────────────────────────────────────────────
        // The engine guarantees a single warning log per unknown type per
        // JVM (Req 22.3) and returns 0 for unknowns so the row still
        // persists for forensic audit (Req 22.2).
        int weight = riskEngine.weightFor(frame.eventType());

        // ── 2. Replay dedup → effective delta (legacy soft path) ───────────
        // For replayed frames lacking a correlation id we attempt SETNX on
        // the soft dedup key; if the key already exists we still persist
        // the row (audit trail) but force scoreDelta=0 so the running
        // counter cannot double-score (Req 11.3, 12.7).
        int effectiveDelta = weight;
        if (replayed && weight != 0) {
            if (!claimReplaySlot(sessionId, frame)) {
                effectiveDelta = 0;
            }
        }

        // ── 3. INSERT proctoring_events ────────────────────────────────────
        ProctoringEvent row = new ProctoringEvent();
        row.setSessionId(sessionId);
        row.setEventType(frame.eventType());
        row.setClientTimestamp(frame.clientTimestamp());
        row.setServerTimestamp(LocalDateTime.now());
        row.setPayloadJson(serializePayload(frame.payload()));
        row.setReplayed(replayed);
        row.setScoreDelta(effectiveDelta);
        row.setClientCorrelationId(
                sanitizeCorrelationId(frame.clientCorrelationId()));
        ProctoringEvent saved = eventRepo.save(row);
        Long eventId = saved.getId();

        // ── 4. Apply delta to the live Valkey counter ──────────────────────
        // INCRBY happens FIRST so the band transition is detected from the
        // accurate live score. The DB persistBand (JPA) follows immediately
        // inside the same transaction. The gap between Valkey INCRBY and JPA
        // commit is bounded: if the JPA transaction rolls back, the scheduled
        // RiskScoreFlusher resyncs the DB projection within 5 s, and the
        // admin rescore path replays from the persisted event log.
        RiskUpdate update = riskEngine.applyDelta(sessionId, effectiveDelta);

        // ── 5. On band change: persist + broadcast ────────────────────────
        // persistBand writes riskBand + flagged to the DB projection. This
        // must happen inside the same @Transactional boundary as the INSERT
        // above — both commit together or neither does.
        if (update.changed()) {
            riskEngine.persistBand(sessionId, update.newBand());

            Long contestId = sessionRepo.findById(sessionId)
                    .map(ProctoringSession::getContestId)
                    .orElseThrow(() -> new ProctoringNotFoundException(
                            "Proctoring session not found: " + sessionId));

            eventPublisher.publishEvent(new ProctoringRiskBandChangedEvent(
                    sessionId, contestId, update.newScore(), update.oldBand(), update.newBand()));
        }

        // ── 6. Return the inserted id ──────────────────────────────────────
        return eventId;
    }

    // ── Internals ──────────────────────────────────────────────────────────

    /**
     * Sanitise a client-supplied correlation id to fit the
     * {@code VARCHAR(64)} column constraint (V8 migration). UUIDs are
     * 36 chars — well within the limit — but the value arrives from the
     * browser and a malicious frame could carry an arbitrarily long
     * string. Truncating to 64 chars prevents a DB constraint violation
     * from aborting the entire ingest transaction (fixes Bug 8).
     */
    private static final int CORRELATION_ID_MAX_LENGTH = 64;

    private static String sanitizeCorrelationId(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return raw.length() > CORRELATION_ID_MAX_LENGTH
                ? raw.substring(0, CORRELATION_ID_MAX_LENGTH)
                : raw;
    }

    /**
     * Attempt to claim the once-per-(session, ts_ms, type) replay slot
     * via {@code SET NX EX 600}. Returns {@code true} when the slot
     * was claimed (i.e. this is the first time we have seen this exact
     * triple), {@code false} when a previous replay already claimed it
     * (so the caller should zero out the score delta).
     *
     * <p>Fail-open semantics: a Valkey outage returns {@code true} so
     * scoring continues against the DB rather than blackholing replays.
     * The bounded 600 s window plus the dedupKey design (timestamp +
     * type) keep the worst-case double-score blast radius small.
     */
    private boolean claimReplaySlot(Long sessionId, EventFrame frame) {
        if (frame.clientTimestamp() == null) {
            // No timestamp ⇒ no stable dedup key. Persist as-is and
            // let the higher-level frame validation reject malformed
            // frames upstream.
            return true;
        }
        long tsMs = frame.clientTimestamp().toInstant(ZoneOffset.UTC).toEpochMilli();
        String key = DEDUP_KEY_PREFIX + sessionId + ":" + tsMs + ":" + frame.eventType();
        try {
            Boolean acquired = redis.opsForValue().setIfAbsent(key, "1", DEDUP_TTL);
            // setIfAbsent returns null on certain pipelining/transaction
            // states; treat it the same as "claimed" so we err on the
            // side of scoring rather than silently dropping deltas.
            return acquired == null || acquired;
        } catch (Exception ex) {
            log.warn("Proctoring: Valkey SETNX failed for {}; treating replay as first-seen.", key, ex);
            return true;
        }
    }

    /**
     * Serialise the raw payload map to a {@code jsonb}-compatible
     * string. A null/empty payload becomes {@code "{}"} so the column
     * is always a valid JSON object — easier for downstream queries
     * (admin timeline, deterministic rescore) which can rely on the
     * shape without per-row null checks.
     */
    private String serializePayload(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return EMPTY_JSON_OBJECT;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            // Should be unreachable for a Map<String,Object> built from
            // Jackson's own decoder, but if a caller hands us a value
            // that Jackson refuses to serialise we'd rather persist an
            // empty object than abort ingest — the event still counts
            // for scoring and the upstream validation can be tightened.
            log.warn("Proctoring: failed to serialise payload for event_type={}; persisting empty object.",
                    payload, ex);
            return EMPTY_JSON_OBJECT;
        }
    }
}
