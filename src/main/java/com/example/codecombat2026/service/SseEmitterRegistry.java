package com.example.codecombat2026.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of active SSE connections.
 * When a judge worker finishes, it calls sendVerdict() to push
 * the result directly to the user's browser — no polling needed.
 */
@Component
public class SseEmitterRegistry {

    private static final Logger log = LoggerFactory.getLogger(SseEmitterRegistry.class);

    @Autowired
    private ObjectMapper objectMapper;

    // userId → SseEmitter (one active connection per user)
    private final ConcurrentHashMap<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * Register a new SSE connection for a user.
     * Called when user opens GET /api/submissions/stream.
     */
    public SseEmitter register(Long userId) {
        // Remove any existing connection for this user
        SseEmitter existing = emitters.remove(userId);
        if (existing != null) {
            try { existing.complete(); } catch (Exception ignored) {}
        }

        SseEmitter emitter = new SseEmitter(300_000L); // 5 min timeout

        emitter.onCompletion(() -> {
            emitters.remove(userId);
            log.debug("SSE connection closed for user {}", userId);
        });
        emitter.onTimeout(() -> {
            emitters.remove(userId);
            log.debug("SSE connection timed out for user {}", userId);
        });
        emitter.onError(e -> {
            emitters.remove(userId);
            log.debug("SSE error for user {}: {}", userId, e.getMessage());
        });

        emitters.put(userId, emitter);
        log.debug("SSE connection registered for user {} (total: {})", userId, emitters.size());
        return emitter;
    }

    /**
     * Push a verdict event to a specific user.
     * Called by the judge worker after code execution completes.
     */
    public void sendVerdict(Long userId, Object verdictData) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) {
            log.debug("No SSE connection for user {} — verdict not pushed", userId);
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(verdictData);
            emitter.send(SseEmitter.event()
                .name("verdict")
                .data(json));
            log.debug("Verdict pushed to user {}", userId);
        } catch (IOException e) {
            emitters.remove(userId);
            log.debug("SSE send failed for user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Send a heartbeat to keep connections alive through proxies/load balancers.
     * Called every 25 seconds by a scheduler.
     */
    public void sendHeartbeat() {
        emitters.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event().name("ping").data(""));
            } catch (Exception e) {
                emitters.remove(userId);
            }
        });
    }

    public int getActiveConnectionCount() {
        return emitters.size();
    }
}
