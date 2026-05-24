package com.example.codecombat2026.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of active SSE connections.
 *
 * Multi-tab safe: each open stream gets its own subscription id, and any
 * verdict for the user is fanned out to every active subscription. So a user
 * with two tabs open both see their submission verdicts.
 *
 * When a judge worker finishes, it calls sendVerdict() to push the result
 * directly to every subscription registered for that user — no polling needed.
 */
@Component
public class SseEmitterRegistry {

    private static final Logger log = LoggerFactory.getLogger(SseEmitterRegistry.class);

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * userId → (subscriptionId → emitter). The inner map is a {@link ConcurrentHashMap}
     * so concurrent register/remove from the SSE thread doesn't corrupt state.
     */
    private final ConcurrentHashMap<Long, ConcurrentHashMap<String, SseEmitter>> emitters = new ConcurrentHashMap<>();

    /**
     * Register a new SSE connection for a user. Each call returns a fresh
     * emitter and unique subscriptionId, so multiple tabs are independent.
     */
    public SseEmitter register(Long userId) {
        String subId = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(300_000L); // 5 min timeout

        // Insert into the per-user map atomically
        emitters.computeIfAbsent(userId, k -> new ConcurrentHashMap<>()).put(subId, emitter);

        emitter.onCompletion(() -> remove(userId, subId, "completed"));
        emitter.onTimeout(() -> remove(userId, subId, "timeout"));
        emitter.onError(e -> remove(userId, subId, "error: " + e.getMessage()));

        log.debug("SSE subscribed: user={} sub={} (totalUsers={}, totalSubs={})",
            userId, subId, emitters.size(), totalSubscriptions());

        // Send an immediate "connected" event so the browser confirms the SSE stream is live
        try {
            emitter.send(SseEmitter.event().name("connected").data("{\"status\":\"connected\"}"));
        } catch (IOException e) {
            log.debug("Failed to send connected event to user {}: {}", userId, e.getMessage());
            remove(userId, subId, "send-failed");
        }

        return emitter;
    }

    /**
     * Push a verdict event to every subscription for the user.
     * Called by the judge worker after code execution completes.
     * Best-effort — a single dead emitter does not abort the fan-out.
     */
    public void sendVerdict(Long userId, Object verdictData) {
        ConcurrentHashMap<String, SseEmitter> subs = emitters.get(userId);
        if (subs == null || subs.isEmpty()) {
            log.debug("No SSE subscriptions for user {} — verdict not pushed", userId);
            return;
        }

        String json;
        try {
            json = objectMapper.writeValueAsString(verdictData);
        } catch (IOException e) {
            log.error("Failed to serialise verdict for user {}: {}", userId, e.getMessage());
            return;
        }

        // Iterate a snapshot so concurrent removal from onError doesn't trip ConcurrentModification
        Iterator<Map.Entry<String, SseEmitter>> it = subs.entrySet().iterator();
        int delivered = 0;
        while (it.hasNext()) {
            Map.Entry<String, SseEmitter> e = it.next();
            try {
                e.getValue().send(SseEmitter.event().name("verdict").data(json));
                delivered++;
            } catch (IOException ex) {
                // Subscription is broken — drop it
                it.remove();
                log.debug("SSE send failed for user {} sub {}: {}", userId, e.getKey(), ex.getMessage());
            }
        }
        if (subs.isEmpty()) {
            emitters.remove(userId);
        }
        log.debug("Verdict delivered to {} subscription(s) for user {}", delivered, userId);
    }

    /**
     * Push an arbitrary named event to every subscription for the user.
     *
     * <p>Used by Live Duel Mode to deliver per-user events that are not
     * submission verdicts — {@code queue_timeout} when the matchmaking
     * sweep evicts a user from the queue (Requirement 1.5),
     * {@code pairing_failed} when the pair-loop cannot create a match
     * (Requirements 3.4 / 10.2), and {@code matched} when a duel is paired
     * (Requirement 13.2). The registry itself is generic over event name —
     * only the browser consumer (the duel lobby page) cares which name it
     * is listening for.
     *
     * <p>Best-effort: a single dead emitter does not abort the fan-out.
     * Mirrors {@link #sendVerdict(Long, Object)} for serialization and
     * cleanup behavior.
     */
    public void sendEvent(Long userId, String eventName, Object payload) {
        ConcurrentHashMap<String, SseEmitter> subs = emitters.get(userId);
        if (subs == null || subs.isEmpty()) {
            log.debug("No SSE subscriptions for user {} — '{}' event not pushed", userId, eventName);
            return;
        }

        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (IOException e) {
            log.error("Failed to serialise '{}' payload for user {}: {}", eventName, userId, e.getMessage());
            return;
        }

        Iterator<Map.Entry<String, SseEmitter>> it = subs.entrySet().iterator();
        int delivered = 0;
        while (it.hasNext()) {
            Map.Entry<String, SseEmitter> e = it.next();
            try {
                e.getValue().send(SseEmitter.event().name(eventName).data(json));
                delivered++;
            } catch (IOException ex) {
                it.remove();
                log.debug("SSE send failed for user {} sub {} event {}: {}",
                    userId, e.getKey(), eventName, ex.getMessage());
            }
        }
        if (subs.isEmpty()) {
            emitters.remove(userId);
        }
        log.debug("'{}' delivered to {} subscription(s) for user {}", eventName, delivered, userId);
    }

    /**
     * Send a heartbeat to keep connections alive through proxies / load balancers.
     * Called every 25 seconds by a scheduler.
     */
    public void sendHeartbeat() {
        if (emitters.isEmpty()) return;
        emitters.forEach((userId, subs) -> {
            Iterator<Map.Entry<String, SseEmitter>> it = subs.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, SseEmitter> e = it.next();
                try {
                    e.getValue().send(SseEmitter.event().name("ping").data(""));
                } catch (Exception ex) {
                    it.remove();
                }
            }
            if (subs.isEmpty()) emitters.remove(userId);
        });
    }

    public int getActiveConnectionCount() {
        return totalSubscriptions();
    }

    private int totalSubscriptions() {
        int total = 0;
        for (ConcurrentHashMap<String, SseEmitter> m : emitters.values()) total += m.size();
        return total;
    }

    private void remove(Long userId, String subId, String reason) {
        ConcurrentHashMap<String, SseEmitter> subs = emitters.get(userId);
        if (subs == null) return;
        if (subs.remove(subId) != null) {
            log.debug("SSE unsubscribed: user={} sub={} reason={}", userId, subId, reason);
        }
        if (subs.isEmpty()) emitters.remove(userId);
    }
}
