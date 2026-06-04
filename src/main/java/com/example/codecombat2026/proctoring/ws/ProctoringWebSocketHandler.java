package com.example.codecombat2026.proctoring.ws;

import com.example.codecombat2026.proctoring.config.ProctoringConfig;
import com.example.codecombat2026.proctoring.entity.EndReason;
import com.example.codecombat2026.proctoring.entity.RiskBand;
import com.example.codecombat2026.proctoring.service.ProctoringEventService;
import com.example.codecombat2026.proctoring.service.ProctoringRateLimiter;
import com.example.codecombat2026.proctoring.service.ProctoringRateLimiter.RateLimitDecision;
import com.example.codecombat2026.proctoring.service.ProctoringSessionService;
import com.example.codecombat2026.proctoring.ws.frame.BufferAckFrame;
import com.example.codecombat2026.proctoring.ws.frame.EventAckFrame;
import com.example.codecombat2026.proctoring.ws.frame.EventFrame;
import com.example.codecombat2026.proctoring.ws.frame.HeartbeatAckFrame;
import com.example.codecombat2026.proctoring.ws.frame.RateLimitExceededFrame;
import com.example.codecombat2026.proctoring.ws.frame.RiskUpdateFrame;
import com.example.codecombat2026.proctoring.ws.frame.SessionTerminatedFrame;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * WebSocket handler for the proctoring channel at {@code /api/proctoring/ws}.
 *
 * <p>Lifecycle (mirrors {@code design.md}):
 * <ul>
 *   <li>{@code afterConnectionEstablished}: register the bound
 *       {@code (sessionId, ws)} pair in {@link ProctoringSessionRegistry},
 *       send the initial {@code RISK_UPDATE} from current Valkey state, and
 *       schedule a per-session heartbeat-timeout task on the shared
 *       {@link #heartbeatScheduler} with delay
 *       {@link ProctoringConfig#getHeartbeatTimeoutSeconds}.</li>
 *   <li>{@code handleTextMessage}: parse the JSON {@code type} field and
 *       dispatch:
 *       <ul>
 *         <li>{@code EVENT} → forward to
 *             {@link ProctoringEventService#ingest}, reply with
 *             {@code EVENT_ACK { client_correlation_id, event_id }}.</li>
 *         <li>{@code HEARTBEAT} → reset the timeout task, reply with
 *             {@code HEARTBEAT_ACK { server_time }}.</li>
 *         <li>{@code FINISH} → close the session as
 *             {@link EndReason#SELF_FINISHED}, push
 *             {@code SESSION_TERMINATED}, close 1000.</li>
 *         <li>{@code QUIT} → close the session as
 *             {@link EndReason#SELF_QUIT}, push
 *             {@code SESSION_TERMINATED}, close 1000.</li>
 *       </ul>
 *   </li>
 *   <li>Heartbeat timeout: emit a {@code HEARTBEAT_TIMEOUT} event into the
 *       session via {@link ProctoringEventService#ingest}, push
 *       {@code SESSION_TERMINATED}, close 4408.</li>
 *   <li>{@code afterConnectionClosed} / {@code handleTransportError}:
 *       cancel the timeout task and unregister from the registry. The
 *       session row itself is <b>not</b> terminated — the candidate may
 *       reconnect within {@code maxOfflineSeconds} (Req 11.5).</li>
 * </ul>
 *
 * <p>Refreshes {@code proctoring:session:{sid}:lastEventAt} (90 s TTL) on
 * every inbound frame so admin dashboards can compute liveness without
 * polling the registry.
 *
 * <p>Close codes are encoded as
 * {@code CloseStatus(4000+offset, reason)} per the IANA convention for
 * application-defined codes — see the table in {@code design.md}.
 *
 * <p>Validates: Req 9.1, 9.3, 9.5, 9.6, 9.7, 9.8, 10.1, 10.2, 13.6, 13.7,
 * 13.8, 24.2, 24.5.
 */
@Component
public class ProctoringWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ProctoringWebSocketHandler.class);

    // ── Application close codes (CloseStatus(4000+offset, reason)) ─────────
    /** Admin force-end (Req 10.2). Reserved for the admin path; not used here directly. */
    public static final int CLOSE_CODE_ADMIN_FORCED = 4003;
    /** Unauthenticated — ticket missing/invalid/expired (Req 9.2, 16.5). */
    public static final int CLOSE_CODE_UNAUTHENTICATED = 4401;
    /** Forbidden — session does not belong to this user, or session ended. */
    public static final int CLOSE_CODE_FORBIDDEN = 4403;
    /** Heartbeat-timeout exceeded (Req 9.8). */
    public static final int CLOSE_CODE_HEARTBEAT_TIMEOUT = 4408;
    /** Duplicate connection for an already-bound session (Req 9.4). */
    public static final int CLOSE_CODE_DUPLICATE = 4409;
    /** Inbound frame exceeds {@code maxEventBytes} (Req 17.6). */
    public static final int CLOSE_CODE_PAYLOAD_TOO_LARGE = 4413;
    /** Unhandled server error. */
    public static final int CLOSE_CODE_SERVER_ERROR = 4500;

    // ── Frame {@code type} discriminators ─────────────────────────────────
    private static final String TYPE_EVENT = "EVENT";
    private static final String TYPE_HEARTBEAT = "HEARTBEAT";
    private static final String TYPE_FINISH = "FINISH";
    private static final String TYPE_QUIT = "QUIT";

    /** Synthetic event type fired into the session on heartbeat-timeout (Req 9.8 → 12.2). */
    private static final String EVENT_TYPE_HEARTBEAT_TIMEOUT = "HEARTBEAT_TIMEOUT";

    /** Liveness key TTL — admin dashboards use this to compute "last seen". */
    private static final Duration LAST_EVENT_AT_TTL = Duration.ofSeconds(90);
    private static final String LAST_EVENT_AT_KEY_PREFIX = "proctoring:session:";
    private static final String LAST_EVENT_AT_KEY_SUFFIX = ":lastEventAt";
    /** Score / band key suffixes — read on connect for the initial RISK_UPDATE. */
    private static final String SCORE_KEY_SUFFIX = ":score";
    private static final String BAND_KEY_SUFFIX = ":band";

    @Autowired private ObjectMapper objectMapper;
    @Autowired private ProctoringConfig config;
    @Autowired private ProctoringSessionRegistry registry;
    @Autowired private ProctoringEventService eventService;
    @Autowired private ProctoringSessionService sessionService;
    @Autowired private StringRedisTemplate redis;
    @Autowired private PayloadSizeGuard payloadSizeGuard;
    @Autowired private ProctoringRateLimiter rateLimiter;

    /**
     * Shared scheduler for per-session heartbeat-timeout tasks. Sized to a
     * small core pool since each task is cheap (a single conditional UPDATE
     * + WS close); the pool only has to keep up with the rate at which
     * sessions reconnect after a network blip.
     */
    private ScheduledExecutorService heartbeatScheduler;

    /**
     * Per-session heartbeat-timeout futures, so that an inbound
     * {@code HEARTBEAT} (or any other frame that resets the deadline) can
     * cancel the prior task before scheduling a fresh one. Keyed on
     * {@code proctoring_sessions.id} — the same key used by the registry.
     */
    private final ConcurrentHashMap<Long, ScheduledFuture<?>> heartbeatTasks = new ConcurrentHashMap<>();

    /**
     * Per-session replay-batch state (Req 11.2, 11.3). Tracks the count of
     * replayed events ingested in the current contiguous batch and a
     * scheduled idle-flush future. When the candidate drains its IndexedDB
     * after reconnect, every {@code replayed=true} {@code EVENT} frame
     * increments the counter and reschedules the 1-second idle timer; the
     * counter is flushed back to the candidate as a single
     * {@code BUFFER_ACK { replayed_count }} frame on whichever event
     * happens first:
     * <ul>
     *   <li>the next non-replayed {@code EVENT} arrives (immediate flush);</li>
     *   <li>a {@code HEARTBEAT}/{@code FINISH}/{@code QUIT} arrives
     *       (immediate flush — the candidate has clearly switched to live
     *       mode);</li>
     *   <li>the 1-second idle timer fires (delayed flush — covers the case
     *       where the candidate is purely replay-only and never sends
     *       another non-replayed frame).</li>
     * </ul>
     * The same flush also runs when the WS closes, in case the candidate
     * disconnected mid-replay; the count is dropped silently in that case
     * because there is no live socket to ACK on.
     */
    private final ConcurrentHashMap<Long, ReplayBatchState> replayBatches = new ConcurrentHashMap<>();

    /** Idle-flush deadline for a contiguous replayed-event batch (Req 11.2, 11.3). */
    private static final long REPLAY_BATCH_IDLE_FLUSH_MS = 1_000L;

    /** Mutable replay-batch state, guarded by synchronisation on the instance. */
    private static final class ReplayBatchState {
        int count;
        ScheduledFuture<?> idleFlush;
    }

    @PostConstruct
    void init() {
        // Daemon threads so the JVM can shut down even if a task is
        // mid-execution; pool size of 2 is plenty for the MVP target of
        // 100 concurrent sessions, since each fired task completes in
        // sub-100 ms.
        AtomicLong counter = new AtomicLong();
        ThreadFactory tf = r -> {
            Thread t = new Thread(r, "proctoring-heartbeat-" + counter.incrementAndGet());
            t.setDaemon(true);
            return t;
        };
        this.heartbeatScheduler = Executors.newScheduledThreadPool(2, tf);
    }

    @PreDestroy
    void shutdown() {
        if (heartbeatScheduler == null) return;
        // Cancel any in-flight idle-flush futures so they cannot fire
        // against a half-shut-down scheduler. The map is cleared by the
        // cancel-then-remove cycle inside discardReplayBatch.
        for (Long sid : replayBatches.keySet()) {
            discardReplayBatch(sid);
        }
        try {
            heartbeatScheduler.shutdown();
            if (!heartbeatScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                heartbeatScheduler.shutdownNow();
            }
        } catch (InterruptedException ie) {
            heartbeatScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // ── Connection lifecycle ───────────────────────────────────────────────

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession ws) {
        Long sessionId = sessionIdOf(ws);
        if (sessionId == null) {
            // The handshake interceptor populates this attribute. Its absence
            // means something has gone wrong in the upgrade pipeline; close
            // forbidden so the candidate retries through the REST mint path.
            closeQuietly(ws, CLOSE_CODE_FORBIDDEN, "missing session binding");
            return;
        }

        if (!registry.register(sessionId, ws)) {
            // Duplicate handshake: the registry already closed `ws` with 4409
            // and left the existing connection intact (Req 9.4).
            return;
        }

        // 1. Send the initial RISK_UPDATE from current Valkey state. A brand
        //    new session legitimately has no key yet → defaults to (0, LOW).
        try {
            RiskUpdateFrame initial = readInitialRiskUpdate(sessionId);
            sendFrame(ws, initial);
        } catch (Exception e) {
            // Don't fail the connection on a Valkey hiccup — the candidate
            // can still operate against the durable state and the next band
            // crossing will refresh the frontend.
            log.warn("Proctoring: failed to send initial RISK_UPDATE for session {}: {}",
                    sessionId, e.getMessage());
        }

        // 2. Schedule the heartbeat-timeout task. The candidate is expected
        //    to send HEARTBEAT every heartbeatIntervalSeconds; we time out
        //    after heartbeatTimeoutSeconds with no inbound frame at all.
        scheduleHeartbeatTimeout(sessionId);

        // 3. Refresh the liveness key — connection itself counts as activity.
        touchLastEventAt(sessionId);
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession ws, @NonNull TextMessage msg) {
        Long sessionId = sessionIdOf(ws);
        if (sessionId == null) {
            closeQuietly(ws, CLOSE_CODE_FORBIDDEN, "missing session binding");
            return;
        }

        // Payload size cap (Req 17.6). Closes 4413 internally if exceeded.
        String payload = msg.getPayload();
        if (!payloadSizeGuard.check(ws, payload)) {
            return;
        }

        // Refresh last-seen on every inbound frame, regardless of type.
        touchLastEventAt(sessionId);

        JsonNode root;
        try {
            root = objectMapper.readTree(payload);
        } catch (JsonProcessingException e) {
            // Malformed JSON — log and ignore. Returning here keeps the
            // session alive; a hostile client gets nothing, a benign bug
            // gets a single line in the logs.
            log.debug("Proctoring: malformed JSON on session {}: {}", sessionId, e.getOriginalMessage());
            return;
        }

        String type = root.path("type").asText("");
        try {
            switch (type) {
                case TYPE_EVENT     -> dispatchEvent(ws, sessionId, root);
                case TYPE_HEARTBEAT -> dispatchHeartbeat(ws, sessionId);
                case TYPE_FINISH    -> dispatchFinish(ws, sessionId);
                case TYPE_QUIT      -> dispatchQuit(ws, sessionId);
                default -> {
                    // Unknown frame type — drop with a debug log so that a
                    // future detector can introduce new client-side frames
                    // without a server release.
                    log.debug("Proctoring: dropping unknown frame type '{}' on session {}", type, sessionId);
                }
            }
        } catch (RuntimeException e) {
            log.error("Proctoring: unhandled error dispatching '{}' on session {}: {}",
                    type, sessionId, e.getMessage(), e);
            closeQuietly(ws, CLOSE_CODE_SERVER_ERROR, "internal error");
        }
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession ws, @NonNull CloseStatus status) {
        Long sessionId = sessionIdOf(ws);
        if (sessionId == null) return;
        cancelHeartbeatTimeout(sessionId);
        // Drop any pending replay-batch state without sending — the WS is
        // already closing, so the candidate will replay again on reconnect
        // and the soft + hard dedup paths in ProctoringEventService will
        // keep the score consistent (Req 11.2, 11.3, 12.7).
        discardReplayBatch(sessionId);
        // Only unregister if this WS is still the bound one — terminate()
        // already calls unregister and a duplicate unregister is a no-op.
        registry.unregister(sessionId);
    }

    @Override
    public void handleTransportError(@NonNull WebSocketSession ws, @NonNull Throwable e) {
        Long sessionId = sessionIdOf(ws);
        log.debug("Proctoring: transport error on session {}: {}",
                sessionId == null ? "?" : sessionId, e.getMessage());
        // afterConnectionClosed will run next; cleanup happens there.
    }

    // ── Dispatch ───────────────────────────────────────────────────────────

    private void dispatchEvent(WebSocketSession ws, Long sessionId, JsonNode root) {
        // Per-session sliding-window rate limit (Req 17.1, 17.2). On
        // rejection we drop the frame entirely — no INSERT, no risk-engine
        // delta — and emit at most one RATE_LIMIT_EXCEEDED frame per 10-s
        // window (Req 17.3). HEARTBEAT/FINISH/QUIT are dispatched outside
        // this method and are intentionally exempt from the limiter; only
        // candidate-driven Suspicious_Event frames are throttled.
        RateLimitDecision decision = rateLimiter.allowEventFrame(sessionId);
        if (!decision.allowed()) {
            if (decision.shouldNotify()) {
                sendFrame(ws, RateLimitExceededFrame.of(10));
            }
            return;
        }

        EventFrame frame;
        try {
            frame = objectMapper.treeToValue(root, EventFrame.class);
        } catch (JsonProcessingException e) {
            log.debug("Proctoring: malformed EVENT on session {}: {}", sessionId, e.getOriginalMessage());
            return;
        }

        if (frame.eventType() == null || frame.eventType().isBlank()) {
            log.debug("Proctoring: dropping EVENT with missing event_type on session {}", sessionId);
            return;
        }

        LocalDateTime clientTs = parseClientTimestamp(frame.clientTimestamp());
        if (clientTs == null) {
            // No client timestamp ⇒ stamp now() so the event still persists.
            // Forensic ordering will use server_timestamp as the tiebreaker.
            clientTs = LocalDateTime.now();
        }

        Map<String, Object> payload = frame.payload() == null ? Map.of() : frame.payload();
        boolean replayed = Boolean.TRUE.equals(frame.replayed());

        Long eventId = eventService.ingest(
                sessionId,
                new ProctoringEventService.EventFrame(
                        frame.eventType(), clientTs, payload, frame.clientCorrelationId()),
                replayed);

        // EVENT_ACK round-trips the client_correlation_id (may be null) so the
        // frontend's sendEvent() helper can resolve the inflight promise.
        sendFrame(ws, EventAckFrame.of(frame.clientCorrelationId(), eventId));

        // Replay-batch bookkeeping (Req 11.2, 11.3): every replayed=true frame
        // adds to the running count and rearms the 1-second idle-flush timer;
        // the next non-replayed frame flushes a single BUFFER_ACK so the
        // candidate can purge its IndexedDB rows. See trackReplayBatch /
        // flushReplayBatch below for the full state machine.
        if (replayed) {
            trackReplayBatch(sessionId);
        } else {
            flushReplayBatch(sessionId);
        }
    }

    private void dispatchHeartbeat(WebSocketSession ws, Long sessionId) {
        // Reset the per-session timeout task and reply with the server clock
        // so the frontend can surface a clock-skew indicator. A heartbeat
        // also closes any pending replay batch — the candidate has clearly
        // moved on from the replay-burst phase.
        flushReplayBatch(sessionId);
        scheduleHeartbeatTimeout(sessionId);
        sendFrame(ws, HeartbeatAckFrame.of(Instant.now().toString()));
    }

    private void dispatchFinish(WebSocketSession ws, Long sessionId) {
        Long userId = userIdOf(ws);
        if (userId == null) {
            closeQuietly(ws, CLOSE_CODE_FORBIDDEN, "missing user binding");
            return;
        }

        // Flush any pending BUFFER_ACK before the WS closes — the candidate
        // may have replayed events and immediately FINISHed without sending
        // any non-replayed frame in between. Doing the flush first means the
        // BUFFER_ACK lands ahead of SESSION_TERMINATED in wire order, so the
        // frontend can purge IndexedDB before navigating to the terminal
        // screen.
        flushReplayBatch(sessionId);

        boolean closed;
        try {
            closed = sessionService.finish(sessionId, userId);
        } catch (RuntimeException e) {
            // Ownership / not-found errors → 4403. The durable layer is
            // authoritative; surface a clean close so the frontend can
            // render the matching terminal screen.
            log.debug("Proctoring: FINISH failed for session {}: {}", sessionId, e.getMessage());
            closeQuietly(ws, CLOSE_CODE_FORBIDDEN, "session unavailable");
            return;
        }

        // First writer wins: if `closed` is false the session was already
        // terminated by another path, but we still send SESSION_TERMINATED
        // and close 1000 so the candidate's UI flushes deterministically.
        if (!closed) {
            log.debug("Proctoring: FINISH on already-ended session {}", sessionId);
        }
        cancelHeartbeatTimeout(sessionId);
        sendFrame(ws, SessionTerminatedFrame.of("completed", EndReason.SELF_FINISHED.name()));
        closeQuietly(ws, CloseStatus.NORMAL.getCode(), "completed");
        registry.unregister(sessionId);
    }

    private void dispatchQuit(WebSocketSession ws, Long sessionId) {
        Long userId = userIdOf(ws);
        if (userId == null) {
            closeQuietly(ws, CLOSE_CODE_FORBIDDEN, "missing user binding");
            return;
        }

        // Same pre-close BUFFER_ACK flush rationale as dispatchFinish.
        flushReplayBatch(sessionId);

        boolean closed;
        try {
            closed = sessionService.quit(sessionId, userId);
        } catch (RuntimeException e) {
            log.debug("Proctoring: QUIT failed for session {}: {}", sessionId, e.getMessage());
            closeQuietly(ws, CLOSE_CODE_FORBIDDEN, "session unavailable");
            return;
        }

        if (!closed) {
            log.debug("Proctoring: QUIT on already-ended session {}", sessionId);
        }
        cancelHeartbeatTimeout(sessionId);
        sendFrame(ws, SessionTerminatedFrame.of("quit", EndReason.SELF_QUIT.name()));
        closeQuietly(ws, CloseStatus.NORMAL.getCode(), "quit");
        registry.unregister(sessionId);
    }

    // ── Heartbeat-timeout scheduling ──────────────────────────────────────

    /**
     * Schedule (or reschedule) the heartbeat-timeout task for
     * {@code sessionId}. Cancels any prior task atomically so a fast
     * burst of heartbeats does not leak schedulings.
     */
    private void scheduleHeartbeatTimeout(Long sessionId) {
        long delay = Math.max(1, config.getHeartbeatTimeoutSeconds());
        ScheduledFuture<?> next = heartbeatScheduler.schedule(
                () -> fireHeartbeatTimeout(sessionId),
                delay,
                TimeUnit.SECONDS);
        ScheduledFuture<?> prev = heartbeatTasks.put(sessionId, next);
        if (prev != null) {
            prev.cancel(false);
        }
    }

    /** Cancel and remove the per-session heartbeat-timeout task. */
    private void cancelHeartbeatTimeout(Long sessionId) {
        ScheduledFuture<?> task = heartbeatTasks.remove(sessionId);
        if (task != null) {
            task.cancel(false);
        }
    }

    // ── Replay-batch tracking (BUFFER_ACK) ─────────────────────────────────

    /**
     * Record one inbound replayed event for {@code sessionId} and rearm the
     * 1-second idle-flush timer. Idempotent under concurrent inbound frames
     * thanks to the {@link ConcurrentHashMap#compute} call — only one
     * {@link ReplayBatchState} ever exists per session, and updates run
     * atomically with respect to other replay/non-replay/timeout paths.
     */
    private void trackReplayBatch(Long sessionId) {
        replayBatches.compute(sessionId, (sid, existing) -> {
            ReplayBatchState state = existing == null ? new ReplayBatchState() : existing;
            state.count++;
            if (state.idleFlush != null) {
                state.idleFlush.cancel(false);
            }
            state.idleFlush = heartbeatScheduler.schedule(
                    () -> flushReplayBatch(sid),
                    REPLAY_BATCH_IDLE_FLUSH_MS,
                    TimeUnit.MILLISECONDS);
            return state;
        });
    }

    /**
     * Send a single {@code BUFFER_ACK { replayed_count }} frame to the
     * candidate (if any replayed events have accumulated) and clear the
     * per-session state. Safe to call from multiple paths — the
     * {@link ConcurrentHashMap#remove} guarantees only one ACK is emitted
     * even under racing flushes (e.g. a HEARTBEAT and the idle timer firing
     * simultaneously).
     */
    private void flushReplayBatch(Long sessionId) {
        ReplayBatchState state = replayBatches.remove(sessionId);
        if (state == null || state.count == 0) {
            // No batch in flight, or already flushed by another path.
            return;
        }
        if (state.idleFlush != null) {
            state.idleFlush.cancel(false);
        }
        // The registry's send() helper resolves the bound WS by sessionId,
        // serialises with the same lock used by other server-pushed frames,
        // and silently drops if the session is no longer connected — which
        // is exactly the behaviour we want when this flush races with
        // afterConnectionClosed.
        registry.send(sessionId, BufferAckFrame.of(state.count));
    }

    /**
     * Drop any pending replay-batch state for {@code sessionId} without
     * emitting a {@code BUFFER_ACK}. Used on socket close paths where
     * there is no live WS to deliver the frame on; the candidate will
     * replay again on reconnect and the dedup paths keep the durable
     * state consistent.
     */
    private void discardReplayBatch(Long sessionId) {
        ReplayBatchState state = replayBatches.remove(sessionId);
        if (state != null && state.idleFlush != null) {
            state.idleFlush.cancel(false);
        }
    }

    /**
     * Heartbeat-timeout fired (Req 9.8 / 13.7). Emit a synthetic
     * {@code HEARTBEAT_TIMEOUT} event into the session log (so it counts
     * toward the risk score per Req 12.2 defaults), close the session row
     * with {@link EndReason#HEARTBEAT_TIMEOUT}, push
     * {@code SESSION_TERMINATED}, and close 4408.
     */
    private void fireHeartbeatTimeout(Long sessionId) {
        // Drop ourselves from the task map first so a re-entrant cancel from
        // afterConnectionClosed doesn't NPE on a vanished future.
        heartbeatTasks.remove(sessionId);
        // Discard any pending replay batch without ACKing — the WS is about
        // to close 4408 and there is no live socket to deliver BUFFER_ACK on.
        discardReplayBatch(sessionId);

        try {
            // Persist the synthetic event so the audit trail explains the close.
            ProctoringEventService.EventFrame synthetic = new ProctoringEventService.EventFrame(
                    EVENT_TYPE_HEARTBEAT_TIMEOUT,
                    LocalDateTime.now(),
                    Map.of());
            eventService.ingest(sessionId, synthetic, false);
        } catch (RuntimeException e) {
            log.warn("Proctoring: failed to persist HEARTBEAT_TIMEOUT for session {}: {}",
                    sessionId, e.getMessage());
        }

        try {
            sessionService.heartbeatTimeout(sessionId);
        } catch (RuntimeException e) {
            // The session may have been terminated by another path in the
            // meantime; the registry.terminate call below is still safe.
            log.debug("Proctoring: heartbeatTimeout close was a no-op for session {}: {}",
                    sessionId, e.getMessage());
        }

        // Push SESSION_TERMINATED and close 4408 atomically through the registry.
        registry.terminate(
                sessionId,
                "heartbeat timeout",
                EndReason.HEARTBEAT_TIMEOUT,
                CLOSE_CODE_HEARTBEAT_TIMEOUT);
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    /**
     * Read {@code (score, band)} from Valkey for the initial
     * {@code RISK_UPDATE}, falling back to {@code (0, LOW)} on any miss.
     * The first frame the candidate receives must be {@code RISK_UPDATE}
     * regardless of cache state — see the sequence diagram in design.md.
     */
    private RiskUpdateFrame readInitialRiskUpdate(Long sessionId) {
        int score = 0;
        RiskBand band = RiskBand.LOW;
        try {
            String s = redis.opsForValue().get(LAST_EVENT_AT_KEY_PREFIX + sessionId + SCORE_KEY_SUFFIX);
            if (s != null) {
                try {
                    score = Integer.parseInt(s);
                } catch (NumberFormatException ignored) {
                    // Stay at 0 — the next event will overwrite the key.
                }
            }
        } catch (Exception e) {
            log.debug("Proctoring: Valkey GET score failed for session {}: {}", sessionId, e.getMessage());
        }
        try {
            String b = redis.opsForValue().get(LAST_EVENT_AT_KEY_PREFIX + sessionId + BAND_KEY_SUFFIX);
            if (b != null) {
                try {
                    band = RiskBand.valueOf(b);
                } catch (IllegalArgumentException ignored) {
                    // Stay at LOW.
                }
            }
        } catch (Exception e) {
            log.debug("Proctoring: Valkey GET band failed for session {}: {}", sessionId, e.getMessage());
        }
        return RiskUpdateFrame.of(score, band.name());
    }

    /**
     * Refresh the {@code proctoring:session:{sid}:lastEventAt} key with a
     * 90 s TTL. Best-effort — a Valkey hiccup must not break ingest.
     */
    private void touchLastEventAt(Long sessionId) {
        try {
            redis.opsForValue().set(
                    LAST_EVENT_AT_KEY_PREFIX + sessionId + LAST_EVENT_AT_KEY_SUFFIX,
                    Long.toString(System.currentTimeMillis()),
                    LAST_EVENT_AT_TTL);
        } catch (Exception e) {
            log.debug("Proctoring: failed to refresh lastEventAt for session {}: {}",
                    sessionId, e.getMessage());
        }
    }

    /** Parse a client timestamp in either local or offset ISO form. */
    private static LocalDateTime parseClientTimestamp(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            // ISO_OFFSET_DATE_TIME (e.g. "2025-01-15T10:23:45.123Z")
            return Instant.parse(raw).atZone(ZoneOffset.UTC).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            // fall through
        }
        try {
            // ISO_LOCAL_DATE_TIME (e.g. "2025-01-15T10:23:45.123")
            return LocalDateTime.parse(raw);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Serialise {@code frame} to JSON and send it as a {@link TextMessage}.
     * Synchronised on the {@link WebSocketSession} so concurrent server
     * pushes (e.g. an admin warning racing with a band change) cannot
     * interleave bytes on the wire.
     */
    private void sendFrame(WebSocketSession ws, Object frame) {
        if (!ws.isOpen()) return;
        String json;
        try {
            json = objectMapper.writeValueAsString(frame);
        } catch (JsonProcessingException e) {
            log.warn("Proctoring: failed to serialise outbound frame: {}", e.getMessage());
            return;
        }
        try {
            synchronized (ws) {
                if (ws.isOpen()) {
                    ws.sendMessage(new TextMessage(json));
                }
            }
        } catch (IOException e) {
            log.debug("Proctoring: failed to send outbound frame: {}", e.getMessage());
        }
    }

    private void closeQuietly(WebSocketSession ws, int code, String reason) {
        try {
            ws.close(new CloseStatus(code, reason));
        } catch (IOException e) {
            // Already on the way out — nothing useful to do here.
            log.debug("Proctoring: close({}) failed: {}", code, e.getMessage());
        }
    }

    private static Long sessionIdOf(WebSocketSession ws) {
        Object v = ws.getAttributes().get("sessionId");
        return v instanceof Long ? (Long) v : null;
    }

    private static Long userIdOf(WebSocketSession ws) {
        Object v = ws.getAttributes().get("userId");
        return v instanceof Long ? (Long) v : null;
    }
}
