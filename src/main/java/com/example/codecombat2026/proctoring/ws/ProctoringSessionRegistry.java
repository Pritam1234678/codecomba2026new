package com.example.codecombat2026.proctoring.ws;

import com.example.codecombat2026.proctoring.entity.EndReason;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory registry of live proctoring WebSocket sessions, keyed by the
 * {@code proctoring_sessions.id} value of the bound session. Mirrors the
 * pattern used by {@link com.example.codecombat2026.service.SseEmitterRegistry}
 * for SSE emitters but for WebSocket sessions specifically.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Bind exactly one live {@link WebSocketSession} per proctoring
 *       session id (Req 9.4). A duplicate registration is rejected by
 *       closing the new connection with code 4409 and leaving the
 *       existing one untouched.</li>
 *   <li>Project the bound state into Valkey at
 *       {@code proctoring:session:{sid}:connected="1"} so the admin live
 *       dashboard can read the connection flag without an in-memory hop
 *       (Req 18.3).</li>
 *   <li>Provide {@link #send(Long, Object)} for the band-change push
 *       (Req 10.2) and {@link #terminate(Long, String, EndReason, int)}
 *       for the heartbeat-timeout (Req 13.7) and admin force-end paths.</li>
 * </ul>
 *
 * <p>This class is thread-safe — the underlying {@link ConcurrentHashMap}
 * handles concurrent register/unregister calls from the WS thread pool,
 * and the Valkey writes are best-effort: a Valkey hiccup must not bring
 * down the WS layer.
 */
@Component
public class ProctoringSessionRegistry {

    private static final Logger log = LoggerFactory.getLogger(ProctoringSessionRegistry.class);

    /** Valkey key prefix for the connected projection (Req 18.3). */
    private static final String CONNECTED_KEY_PREFIX = "proctoring:session:";
    private static final String CONNECTED_KEY_SUFFIX = ":connected";

    /** Close code 4409 — duplicate WS for an already-bound session id (Req 9.4). */
    private static final int CLOSE_CODE_DUPLICATE = 4409;

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private ObjectMapper objectMapper;

    /** Bound WS sessions, keyed by {@code proctoring_sessions.id}. */
    private final ConcurrentHashMap<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();

    /**
     * Bind {@code ws} to {@code sessionId}. Returns {@code true} on
     * success. If a different WS is already bound for this session id,
     * the new {@code ws} is closed with {@link CloseStatus} 4409 and the
     * existing binding is preserved (Req 9.4).
     */
    public boolean register(Long sessionId, WebSocketSession ws) {
        WebSocketSession existing = sessions.putIfAbsent(sessionId, ws);
        if (existing != null) {
            log.debug("Duplicate proctoring WS for session {} (existing wsId={}, new wsId={}); closing new with 4409",
                sessionId, existing.getId(), ws.getId());
            try {
                ws.close(new CloseStatus(CLOSE_CODE_DUPLICATE, "duplicate connection"));
            } catch (IOException e) {
                log.debug("Failed to close duplicate proctoring WS for session {}: {}", sessionId, e.getMessage());
            }
            return false;
        }

        try {
            redis.opsForValue().set(connectedKey(sessionId), "1", Duration.ofSeconds(90));
        } catch (Exception e) {
            // Best-effort projection; do not unbind on Valkey failure.
            log.warn("Failed to set Valkey connected flag for proctoring session {}: {}", sessionId, e.getMessage());
        }
        return true;
    }

    /**
     * Remove the binding for {@code sessionId}, if any, and delete the
     * Valkey {@code connected} projection. Idempotent — safe to call from
     * both {@code afterConnectionClosed} and an explicit terminate path.
     */
    public void unregister(Long sessionId) {
        WebSocketSession removed = sessions.remove(sessionId);
        if (removed != null) {
            log.debug("Unregistered proctoring WS for session {} (wsId={})", sessionId, removed.getId());
        }
        try {
            redis.delete(connectedKey(sessionId));
        } catch (Exception e) {
            log.warn("Failed to delete Valkey connected flag for proctoring session {}: {}", sessionId, e.getMessage());
        }
    }

    /** Whether {@code sessionId} currently has a bound WS in this JVM. */
    public boolean isConnected(Long sessionId) {
        return sessions.containsKey(sessionId);
    }

    /**
     * Serialise {@code frame} to JSON and send it as a {@link TextMessage}
     * to the WS bound to {@code sessionId}. Best-effort: a closed session
     * or IO failure is logged at WARN and swallowed so callers (the risk
     * engine, admin dashboard) are not coupled to socket lifecycle.
     */
    public void send(Long sessionId, Object frame) {
        WebSocketSession ws = sessions.get(sessionId);
        if (ws == null || !ws.isOpen()) {
            return;
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(frame);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialise proctoring frame for session {}: {}", sessionId, e.getMessage());
            return;
        }
        try {
            synchronized (ws) {
                if (ws.isOpen()) {
                    ws.sendMessage(new TextMessage(json));
                }
            }
        } catch (IOException e) {
            log.warn("Failed to send proctoring frame to session {} (wsId={}): {}",
                sessionId, ws.getId(), e.getMessage());
        }
    }

    /**
     * Push a {@code SESSION_TERMINATED} frame, close the WS with the
     * given application close code, and unregister the binding. Used by
     * the heartbeat-timeout path (Req 13.7), the admin force-end path
     * (Req 10.2), and the contest-ended sweep.
     *
     * <p>To prevent the race where a candidate reconnects between the
     * heartbeat-timeout firing and the terminate call reaching the WS
     * layer, this method only terminates the WS that is CURRENTLY
     * bound to {@code sessionId} at CALL time — not the one that was
     * bound when the timeout was originally scheduled. If a newer WS
     * (from a fresh handshake) has replaced the stale one in the
     * registry, this terminate call is a no-op.
     *
     * <p>The frame shape is:
     * <pre>{@code
     * { "type": "SESSION_TERMINATED",
     *   "reason": "<reason>",
     *   "end_reason": "<EndReason.name()>" }
     * }</pre>
     */
    public void terminate(Long sessionId, String reason, EndReason endReason, int closeCode) {
        WebSocketSession ws = sessions.get(sessionId);
        if (ws == null) {
            return;
        }
        // CAS remove: only remove (and close) the exact WebSocketSession we
        // observed. If a reconnect has already replaced `ws` with a new session
        // between get() and remove(k,v), the remove returns false and we leave
        // the fresh connection untouched. This eliminates the get-then-remove
        // race that would otherwise close a legitimate new connection.
        if (!sessions.remove(sessionId, ws)) {
            return; // another thread already replaced or removed this binding
        }
        WebSocketSession stale = ws;

        // Use a LinkedHashMap so the JSON key order matches the documented
        // shape — easier for log scanning and frontend snapshots.
        Map<String, Object> frame = new LinkedHashMap<>();
        frame.put("type", "SESSION_TERMINATED");
        frame.put("reason", reason);
        frame.put("end_reason", endReason.name());

        String json;
        try {
            json = objectMapper.writeValueAsString(frame);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialise SESSION_TERMINATED frame for session {}: {}",
                    sessionId, e.getMessage());
            json = null;
        }

        if (json != null) {
            try {
                synchronized (stale) {
                    if (stale.isOpen()) {
                        stale.sendMessage(new TextMessage(json));
                    }
                }
            } catch (IOException e) {
                log.warn("Failed to send SESSION_TERMINATED frame to session {} (wsId={}): {}",
                        sessionId, stale.getId(), e.getMessage());
            }
        }

        try {
            stale.close(new CloseStatus(closeCode, reason));
        } catch (IOException e) {
            log.warn("Failed to close proctoring WS for session {} (wsId={}): {}",
                    sessionId, stale.getId(), e.getMessage());
        }

        try {
            redis.delete(connectedKey(sessionId));
        } catch (Exception e) {
            log.warn("Failed to delete Valkey connected flag for proctoring session {}: {}",
                    sessionId, e.getMessage());
        }
    }

    /**
     * Snapshot of currently connected session ids in this JVM. Used by
     * the risk-score flusher to know which sessions to drain from Valkey
     * into the durable projection (Req 18.1, 18.2).
     */
    public Set<Long> connectedSessionIds() {
        return Set.copyOf(sessions.keySet());
    }

    private static String connectedKey(Long sessionId) {
        // Pre-sized StringBuilder via concat — these are short hot keys.
        return CONNECTED_KEY_PREFIX + sessionId + CONNECTED_KEY_SUFFIX;
    }

    /**
     * Test/diagnostic seam — returns a defensive copy of the underlying
     * map. Not part of the public contract.
     */
    Map<Long, WebSocketSession> snapshot() {
        return new HashMap<>(sessions);
    }
}
