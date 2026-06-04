package com.example.codecombat2026.proctoring.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-contest SSE registry for the proctoring admin live channel
 * (Req 15.1, 15.2, 18.3). Mirrors the structure of
 * {@link com.example.codecombat2026.service.DuelSseEmitterRegistry}
 * but keyed by {@code contestId} rather than {@code matchId}.
 *
 * <p>Multi-tab safety: each open admin stream gets a unique
 * {@code subId}, so closing one tab while another is open does not
 * dislodge the second. Best-effort fan-out: a single dead emitter
 * does not abort the loop — the broken subscription is removed inline.
 *
 * <p>Heartbeat sweep runs every 25 s on the same cadence as the
 * existing user-scoped {@code SseEmitterRegistry} and the duel
 * registry so all SSE channels tick together through reverse proxies
 * and mobile carrier idle-kills.
 */
@Component
public class ProctoringAdminSseRegistry {

    private static final Logger log = LoggerFactory.getLogger(ProctoringAdminSseRegistry.class);

    /** 5-minute emitter timeout — connection is refreshed via heartbeat ping. */
    private static final long EMITTER_TIMEOUT_MS = 300_000L;

    /** contestId → (subId → emitter). */
    private final ConcurrentHashMap<Long, ConcurrentHashMap<String, SseEmitter>> emitters =
            new ConcurrentHashMap<>();

    /**
     * Open a fresh SSE stream for {@code contestId}. Each call returns a
     * brand-new emitter and a unique subscription id. The browser receives
     * an immediate {@code connected} event so it can confirm the stream
     * is live before the first proctoring frame arrives.
     */
    public SseEmitter register(Long contestId) {
        String subId = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);

        emitters
                .computeIfAbsent(contestId, k -> new ConcurrentHashMap<>())
                .put(subId, emitter);

        emitter.onCompletion(() -> remove(contestId, subId, "completed"));
        emitter.onTimeout(() -> remove(contestId, subId, "timeout"));
        emitter.onError(e -> remove(contestId, subId, "error: " + e.getMessage()));

        log.debug("Proctoring admin SSE subscribed: contest={} sub={} (totalConns={})",
                contestId, subId, connectionCount());

        try {
            emitter.send(SseEmitter.event().name("connected").data("{\"status\":\"connected\"}"));
        } catch (IOException e) {
            log.debug("Failed to send connected event for contest {}: {}", contestId, e.getMessage());
            remove(contestId, subId, "send-failed");
        }
        return emitter;
    }

    /**
     * Fan {@code payload} out as an SSE event named {@code eventName} to
     * every subscriber on this contest. A single dead emitter does not
     * abort the loop — the broken subscription is removed and we
     * continue.
     */
    public void emit(Long contestId, String eventName, Object payload) {
        ConcurrentHashMap<String, SseEmitter> subs = emitters.get(contestId);
        if (subs == null || subs.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<String, SseEmitter>> it = subs.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, SseEmitter> e = it.next();
            try {
                e.getValue().send(SseEmitter.event().name(eventName).data(payload));
            } catch (IOException ex) {
                it.remove();
                log.debug("Proctoring admin SSE send failed: contest={} sub={} err={}",
                        contestId, e.getKey(), ex.getMessage());
            } catch (IllegalStateException ex) {
                // Spring throws this when the emitter has already completed.
                it.remove();
                log.debug("Proctoring admin SSE emitter already completed: contest={} sub={}",
                        contestId, e.getKey());
            }
        }
        if (subs.isEmpty()) {
            emitters.remove(contestId, subs);
        }
    }

    /** Total live admin SSE connections across all contests. */
    public int connectionCount() {
        int total = 0;
        for (ConcurrentHashMap<String, SseEmitter> subs : emitters.values()) {
            total += subs.size();
        }
        return total;
    }

    /**
     * Heartbeat sweep — keeps connections alive through reverse proxies
     * and mobile carrier idle-kills. Same cadence (25 s) as the existing
     * SSE registries.
     */
    @Scheduled(fixedRate = 25_000L)
    public void sendHeartbeat() {
        if (emitters.isEmpty()) return;
        emitters.forEach((contestId, subs) -> {
            Iterator<Map.Entry<String, SseEmitter>> it = subs.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, SseEmitter> e = it.next();
                try {
                    e.getValue().send(SseEmitter.event().name("ping").data(""));
                } catch (Exception ex) {
                    it.remove();
                }
            }
            if (subs.isEmpty()) emitters.remove(contestId);
        });
    }

    private void remove(Long contestId, String subId, String reason) {
        ConcurrentHashMap<String, SseEmitter> subs = emitters.get(contestId);
        if (subs == null) return;
        if (subs.remove(subId) != null) {
            log.debug("Proctoring admin SSE unsubscribed: contest={} sub={} reason={}",
                    contestId, subId, reason);
        }
        if (subs.isEmpty()) emitters.remove(contestId, subs);
    }
}
