package com.example.codecombat2026.service;

import com.example.codecombat2026.entity.DuelMatch;
import com.example.codecombat2026.repository.DuelMatchRepository;
import com.example.codecombat2026.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-duel-room SSE registry, parallel to the per-user {@link SseEmitterRegistry}.
 *
 * <p>Mirrors the structure of the user-scoped registry but keyed by
 * {@code (matchId, userId, subId)} so we can answer two questions at once:
 * <ol>
 *   <li>"Push event X to every subscriber of duel M" — {@link #emit}.</li>
 *   <li>"Is user U still subscribed to duel M?" — {@link #hasActiveSubscription},
 *       which backs the reconnect-grace timer logic in {@code DuelService}.</li>
 * </ol>
 *
 * <p>Multi-tab safety: each open stream gets its own {@code subId} so closing
 * one tab while another is open does NOT count as "user disconnected" — the
 * lifecycle callback only fires when the inner {@code subId → emitter} map
 * for that {@code (matchId, userId)} becomes empty.
 *
 * <p>Late-event guard: every {@code emit*} call consults
 * {@link DuelMatchRepository#findById(Object)} for the row's {@code endedAt}
 * and silently drops the event with a {@code duel.event.late_drop} WARN log
 * if the match has ended (Requirement 14.5 / Property 17).
 *
 * <p>To avoid a circular dependency between {@code DuelService} and this
 * registry — the service emits through us, we call back into the service on
 * last-subscription close — we accept the close-callback via a setter rather
 * than a constructor injection. {@code DuelService} registers itself in
 * {@code @PostConstruct}; until then the {@link #EMPTY_CALLBACK} no-op is
 * used so tests and startup are safe.
 */
@Service
public class DuelSseEmitterRegistry {

    private static final Logger log = LoggerFactory.getLogger(DuelSseEmitterRegistry.class);

    /**
     * Functional callback invoked when the last open SSE subscription for a
     * given {@code (matchId, userId)} closes. {@code DuelService} implements
     * this to start the 30-second reconnect-grace timer.
     */
    @FunctionalInterface
    public interface DuelLifecycleCallback {
        void onLastSubscriptionClosed(UUID matchId, Long userId);
    }

    /** No-op callback used until {@code DuelService} registers itself. */
    private static final DuelLifecycleCallback EMPTY_CALLBACK = (m, u) -> {};

    /**
     * matchId → (userId → (subscriptionId → emitter)).
     *
     * <p>All three layers are {@link ConcurrentHashMap} so SSE-thread
     * register/remove never corrupts state; callers also iterate snapshots
     * via {@link Map#entrySet()} iterators that tolerate concurrent removal
     * during fan-out.
     */
    private final ConcurrentHashMap<UUID, ConcurrentHashMap<Long, ConcurrentHashMap<String, SseEmitter>>> emitters =
        new ConcurrentHashMap<>();

    private final DuelMatchRepository duelMatchRepository;

    /**
     * Mutable so {@code DuelService.@PostConstruct} can register itself
     * without participating in the constructor wiring (which would create a
     * cycle). Defaults to a no-op so startup ordering is irrelevant.
     */
    private volatile DuelLifecycleCallback lastSubscriptionClosedCallback = EMPTY_CALLBACK;

    public DuelSseEmitterRegistry(@Lazy DuelMatchRepository duelMatchRepository) {
        this.duelMatchRepository = duelMatchRepository;
    }

    /**
     * Registers the close-callback. Idempotent — safe to call multiple times,
     * the latest registration wins. {@code DuelService} should call this in
     * its {@code @PostConstruct}.
     */
    public void setLastSubscriptionClosedCallback(DuelLifecycleCallback callback) {
        this.lastSubscriptionClosedCallback = callback != null ? callback : EMPTY_CALLBACK;
    }

    /**
     * Open a fresh SSE stream for the given {@code (matchId, userId)}. Each
     * call returns a brand-new emitter and a unique subscription id, so
     * multiple browser tabs are independent. The emitter has an effectively
     * infinite timeout ({@link Long#MAX_VALUE}); idle connections are pruned
     * by the {@link #sendHeartbeat()} sweep on send-failure.
     */
    public SseEmitter register(UUID matchId, Long userId) {
        String subId = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        emitters
            .computeIfAbsent(matchId, k -> new ConcurrentHashMap<>())
            .computeIfAbsent(userId, k -> new ConcurrentHashMap<>())
            .put(subId, emitter);

        emitter.onCompletion(() -> remove(matchId, userId, subId, "completed"));
        emitter.onTimeout(() -> remove(matchId, userId, subId, "timeout"));
        emitter.onError(e -> remove(matchId, userId, subId, "error: " + e.getMessage()));

        log.debug("Duel SSE subscribed: match={} user={} sub={} (totalConns={})",
            matchId, userId, subId, connectionCount());

        // Send an immediate "connected" event so the browser confirms the
        // SSE stream is live (mirrors SseEmitterRegistry.register).
        try {
            emitter.send(SseEmitter.event().name("connected").data("{\"status\":\"connected\"}"));
        } catch (IOException e) {
            log.debug("Failed to send connected event: match={} user={} sub={} err={}",
                matchId, userId, subId, e.getMessage());
            remove(matchId, userId, subId, "send-failed");
        }

        return emitter;
    }

    /**
     * Fan {@code payload} out as an SSE event named {@code eventName} to
     * every subscription on this match. Drops the event with a WARN log if
     * the match has already ended. A single dead emitter does not abort the
     * fan-out — the broken subscription is removed and the loop continues.
     *
     * <p>Two event names are exempt from the late-event guard:
     * {@code room_state} (snapshot for new subscribers, including those who
     * connect after a match finished) and {@code match_finished} (terminal
     * event that MUST reach late joiners so they can render the result
     * modal). Without these exemptions a user who opens the arena page
     * after the match ended would see "Loading…" forever because the
     * registry would silently swallow the only event that closes the page.
     */
    public void emit(UUID matchId, String eventName, Object payload) {
        if (isTerminalOrSnapshot(eventName) ? false : isLate(matchId, eventName)) return;

        ConcurrentHashMap<Long, ConcurrentHashMap<String, SseEmitter>> users = emitters.get(matchId);
        if (users == null || users.isEmpty()) return;

        users.forEach((userId, subs) -> sendToSubs(matchId, userId, subs, eventName, payload));
    }

    /**
     * Send {@code payload} only to the given user's subscriptions on this
     * match. Used for typing-to-opponent events where only one side should
     * see the heartbeat. Same late-event exemptions as {@link #emit}.
     */
    public void emitTo(UUID matchId, Long userId, String eventName, Object payload) {
        if (isTerminalOrSnapshot(eventName) ? false : isLate(matchId, eventName)) return;

        ConcurrentHashMap<Long, ConcurrentHashMap<String, SseEmitter>> users = emitters.get(matchId);
        if (users == null) return;
        ConcurrentHashMap<String, SseEmitter> subs = users.get(userId);
        if (subs == null || subs.isEmpty()) return;

        sendToSubs(matchId, userId, subs, eventName, payload);
    }

    /** True for events that MUST be delivered even after the match ended. */
    private static boolean isTerminalOrSnapshot(String eventName) {
        return "room_state".equals(eventName) || "match_finished".equals(eventName);
    }

    /**
     * Whether the given user currently has at least one open subscription on
     * this match. Used by the reconnect-grace timer logic.
     */
    public boolean hasActiveSubscription(UUID matchId, Long userId) {
        ConcurrentHashMap<Long, ConcurrentHashMap<String, SseEmitter>> users = emitters.get(matchId);
        if (users == null) return false;
        ConcurrentHashMap<String, SseEmitter> subs = users.get(userId);
        return subs != null && !subs.isEmpty();
    }

    /** Total live SSE connections across all duels and users. Admin metric. */
    public int connectionCount() {
        int total = 0;
        for (ConcurrentHashMap<Long, ConcurrentHashMap<String, SseEmitter>> users : emitters.values()) {
            for (ConcurrentHashMap<String, SseEmitter> subs : users.values()) {
                total += subs.size();
            }
        }
        return total;
    }

    /**
     * Heartbeat sweep — keeps connections alive through reverse proxies and
     * mobile carrier idle-kills. Sends an empty {@code ping} event on every
     * emitter and prunes any that throw. Mirrors the cadence of
     * {@link SseEmitterRegistry#sendHeartbeat()} so the user-scoped and
     * duel-scoped registries tick together.
     */
    @Scheduled(fixedRate = 25000)
    public void sendHeartbeat() {
        if (emitters.isEmpty()) return;
        emitters.forEach((matchId, users) -> {
            users.forEach((userId, subs) -> {
                Iterator<Map.Entry<String, SseEmitter>> it = subs.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, SseEmitter> e = it.next();
                    try {
                        e.getValue().send(SseEmitter.event().name("ping").data(""));
                    } catch (Exception ex) {
                        it.remove();
                    }
                }
                if (subs.isEmpty()) users.remove(userId);
            });
            if (users.isEmpty()) emitters.remove(matchId);
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // Internals
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Best-effort fan-out helper. Iterates a snapshot of the subId map so
     * concurrent removal from {@code onError} never trips a
     * {@code ConcurrentModificationException}. Dead emitters are removed
     * inline.
     */
    private void sendToSubs(UUID matchId, Long userId,
                            ConcurrentHashMap<String, SseEmitter> subs,
                            String eventName, Object payload) {
        Iterator<Map.Entry<String, SseEmitter>> it = subs.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, SseEmitter> e = it.next();
            try {
                e.getValue().send(SseEmitter.event().name(eventName).data(payload));
            } catch (IOException ex) {
                it.remove();
                log.debug("Duel SSE send failed: match={} user={} sub={} err={}",
                    matchId, userId, e.getKey(), ex.getMessage());
            } catch (IllegalStateException ex) {
                // Spring throws this when the emitter has already completed.
                it.remove();
                log.debug("Duel SSE emitter already completed: match={} user={} sub={}",
                    matchId, userId, e.getKey());
            }
        }
    }

    /**
     * Remove a single subscription. If the user's inner map empties as a
     * result, also remove the userId entry AND fire the lifecycle callback —
     * exactly once, even if multiple tabs close in rapid succession.
     */
    private void remove(UUID matchId, Long userId, String subId, String reason) {
        ConcurrentHashMap<Long, ConcurrentHashMap<String, SseEmitter>> users = emitters.get(matchId);
        if (users == null) return;

        ConcurrentHashMap<String, SseEmitter> subs = users.get(userId);
        if (subs == null) return;

        if (subs.remove(subId) == null) {
            // Already removed by another callback (e.g. onCompletion + onError racing).
            return;
        }

        log.debug("Duel SSE unsubscribed: match={} user={} sub={} reason={}",
            matchId, userId, subId, reason);

        boolean userBecameEmpty = false;
        if (subs.isEmpty()) {
            // Atomically remove the userId entry only if it is still empty.
            // Using remove(key, value) with the snapshot reference defends
            // against a register() racing in to add a fresh subId.
            if (users.remove(userId, subs)) {
                userBecameEmpty = true;
            }
        }

        // Best-effort outer cleanup — does not affect lifecycle semantics.
        if (users.isEmpty()) {
            emitters.remove(matchId, users);
        }

        if (userBecameEmpty) {
            try {
                lastSubscriptionClosedCallback.onLastSubscriptionClosed(matchId, userId);
            } catch (RuntimeException ex) {
                // Never let a callback failure mask the close — log and continue.
                log.warn("DuelLifecycleCallback threw on close: match={} user={} err={}",
                    matchId, userId, ex.getMessage());
            }
        }
    }

    /**
     * Late-event guard. Returns {@code true} (and emits a marker WARN) if
     * the match has ended; the caller should then drop the event.
     */
    private boolean isLate(UUID matchId, String eventName) {
        try {
            DuelMatch match = duelMatchRepository.findById(matchId).orElse(null);
            if (match == null) {
                // No row — treat as not late, fan-out will just hit no subs.
                return false;
            }
            LocalDateTime endedAt = match.getEndedAt();
            if (endedAt != null && TimeUtil.now().isAfter(endedAt)) {
                log.warn("duel.event.late_drop match={} event={} endedAt={}",
                    matchId, eventName, endedAt);
                return true;
            }
            return false;
        } catch (RuntimeException ex) {
            // Repository failure (e.g. transient DB hiccup) — better to deliver
            // than to silently drop. Log and proceed.
            log.warn("Duel late-event check failed: match={} err={}", matchId, ex.getMessage());
            return false;
        }
    }
}
