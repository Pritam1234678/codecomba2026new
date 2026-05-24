package com.example.codecombat2026.service;

import com.example.codecombat2026.duel.SeatAssigner;
import com.example.codecombat2026.entity.DuelMatch;
import com.example.codecombat2026.exception.DuelStateConflictException;
import com.example.codecombat2026.repository.DuelMatchRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Live Duel Mode matchmaking service.
 *
 * <p>Owns the {@code duel:queue} Valkey list and the companion
 * {@code duel:queue:enqueued_at} hash, plus all idempotency / cooldown
 * gates that absorb double-clicks and post-match cooldowns. The pair-loop
 * runs as a Spring {@link Scheduled @Scheduled} fixed-delay tick and is
 * single-threaded by design — with fewer than 500 expected concurrent
 * users a 250 ms cadence delivers acceptable pairing latency without any
 * blocking-pop / reactive-LMOVE machinery.
 *
 * <p>Three Spring scheduler ticks live on this bean:
 * <ul>
 *   <li>{@link #pairLoopTick()} — drains pairs out of {@code duel:queue}
 *       and hands them to {@link DuelService#pairAndStart(Long, Long)}.</li>
 *   <li>{@link #queueTimeoutSweep()} — evicts users that have been queued
 *       longer than {@code DUEL_QUEUE_TIMEOUT_SEC} and emits a
 *       {@code queue_timeout} SSE event on the per-user channel.</li>
 * </ul>
 *
 * <p>{@code DuelService} is injected with {@link Lazy @Lazy} because the
 * full implementation (task 3.6) will eventually depend on the
 * {@link SubmissionWorkerPool} which itself sits downstream of this
 * service — the lazy proxy is the cheapest way to break the indirect
 * cycle that would otherwise refuse to start the context.
 */
@Service
public class MatchmakingService {

    private static final Logger log = LoggerFactory.getLogger(MatchmakingService.class);

    /** The Valkey list used as the matchmaking queue. Each element is a {@code userId} as a decimal string. */
    static final String QUEUE_KEY = "duel:queue";

    /** Companion hash storing the millisecond enqueue timestamp per user. Pruned in the same sweep as the list. */
    static final String QUEUE_ENQUEUED_AT_KEY = "duel:queue:enqueued_at";

    /** Idempotency-lock prefix for the 5 s {@code SET NX EX 5} guard on enqueue. */
    static final String ENQUEUE_LOCK_PREFIX = "duel:enqueue:";

    /**
     * Suffix for the companion key that stores the {@code queueToken} so
     * that a second call within the idempotency window returns the same
     * payload (Requirements 1.2 / 8.1).
     */
    static final String ENQUEUE_TOKEN_SUFFIX = ":token";

    /** Per-user post-match cooldown key prefix (Requirements 10.3 / 10.4). */
    static final String COOLDOWN_PREFIX = "duel:cooldown:";

    /** Sorted-pair create-lock prefix used by the pair loop (Requirement 8.4). */
    static final String CREATE_LOCK_PREFIX = "duel:create:";

    /** Idempotency window for enqueue requests (Requirement 8.3). */
    private static final long ENQUEUE_LOCK_TTL_SEC = 5L;

    /** Sorted-pair create-lock TTL (Requirement 8.4). 60 s is far longer than the 250 ms pair-loop tick. */
    private static final long CREATE_LOCK_TTL_SEC = 60L;

    /** Configurable post-match cooldown duration (Requirement 10.5). */
    @Value("${DUEL_COOLDOWN_SEC:5}")
    long cooldownSec;

    /** Configurable queue-timeout window. Users queued longer than this are evicted (Requirement 1.5). */
    @Value("${DUEL_QUEUE_TIMEOUT_SEC:120}")
    long queueTimeoutSec;

    private final StringRedisTemplate redis;
    private final DuelMatchRepository duelMatchRepository;
    private final SseEmitterRegistry sseEmitterRegistry;
    private final DuelService duelService;
    private final ObjectMapper objectMapper;

    public MatchmakingService(StringRedisTemplate redis,
                              DuelMatchRepository duelMatchRepository,
                              SseEmitterRegistry sseEmitterRegistry,
                              @Lazy DuelService duelService,
                              ObjectMapper objectMapper) {
        this.redis = redis;
        this.duelMatchRepository = duelMatchRepository;
        this.sseEmitterRegistry = sseEmitterRegistry;
        this.duelService = duelService;
        this.objectMapper = objectMapper;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Public surface
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Add a user to the matchmaking queue, idempotent within a 5 s window.
     *
     * <p>Implements the gate ladder in order:
     * <ol>
     *   <li>{@code SET NX EX 5 duel:enqueue:{userId} <token>} — if the
     *       key already exists, look up the companion token and return
     *       the same payload with {@code alreadyQueued = true}
     *       (Requirements 1.2 / 1.7 / 8.1 / 8.2 / 8.3).</li>
     *   <li>{@code EXISTS duel:cooldown:{userId}} — if set, throw
     *       {@code COOLDOWN_ACTIVE} carrying the remaining TTL so the
     *       controller can map to HTTP 429 with {@code Retry-After}
     *       (Requirements 10.3 / 10.4 / 10.5).</li>
     *   <li>{@link DuelMatchRepository#findActiveByUser(Long)} — if the
     *       user already has a {@code WAITING}/{@code IN_PROGRESS} row,
     *       throw {@code ALREADY_IN_MATCH} carrying the existing
     *       {@code matchId} so the controller can map to HTTP 409
     *       (Requirements 1.4 / 10.1 / 12.6).</li>
     *   <li>Defensive {@code LREM duel:queue 0 userId} to scrub any
     *       stale entry left over from a crashed prior session.</li>
     *   <li>{@code RPUSH duel:queue userId} + {@code HSET duel:queue:enqueued_at userId now} —
     *       the actual queue entry plus its timestamp for the
     *       120 s timeout sweep (Requirement 1.5).</li>
     * </ol>
     *
     * @return a fresh {@link EnqueueResult} on success, or the cached
     *         result with {@code alreadyQueued=true} on idempotent retry
     */
    public EnqueueResult enqueue(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }

        // 1) Idempotency lock — absorb double-clicks within a 5 s window.
        String lockKey = ENQUEUE_LOCK_PREFIX + userId;
        String tokenKey = lockKey + ENQUEUE_TOKEN_SUFFIX;
        String fresh = UUID.randomUUID().toString();

        Boolean acquired = redis.opsForValue()
                .setIfAbsent(lockKey, fresh, ENQUEUE_LOCK_TTL_SEC, TimeUnit.SECONDS);

        if (Boolean.FALSE.equals(acquired)) {
            // Another enqueue request already holds the lock — return the
            // same token if we have it, otherwise synthesize a fresh one
            // (the prior request must have raced and crashed).
            String existingToken = redis.opsForValue().get(tokenKey);
            if (existingToken != null) {
                log.debug("Enqueue idempotent: user={} token={}", userId, existingToken);
                return new EnqueueResult(existingToken, Instant.now(), true);
            }
            // Lock held but no companion token — replay with the lock value.
            String lockValue = redis.opsForValue().get(lockKey);
            if (lockValue != null) {
                return new EnqueueResult(lockValue, Instant.now(), true);
            }
            // Lost the race entirely; keep going as if first-time.
        }

        // Persist the companion token so a follow-up retry sees the same payload.
        // Same TTL as the lock so they expire together.
        redis.opsForValue().set(tokenKey, fresh, ENQUEUE_LOCK_TTL_SEC, TimeUnit.SECONDS);

        // 2) Cooldown gate.
        String cooldownKey = COOLDOWN_PREFIX + userId;
        Long cooldownTtl = redis.getExpire(cooldownKey, TimeUnit.SECONDS);
        if (cooldownTtl != null && cooldownTtl > 0) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("retryAfterSec", cooldownTtl);
            throw new DuelStateConflictException("COOLDOWN_ACTIVE", payload);
        }

        // 3) Active-match gate — reject if the user already has a row in
        //    WAITING / IN_PROGRESS state. The list ought to contain at most
        //    one row thanks to the partial unique indexes.
        List<DuelMatch> active = duelMatchRepository.findActiveByUser(userId);
        Optional<DuelMatch> existing = active.stream().findFirst();
        if (existing.isPresent()) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("matchId", existing.get().getMatchId().toString());
            throw new DuelStateConflictException("ALREADY_IN_MATCH", payload);
        }

        // 4) Defensive cleanup — scrub stale list entries the user might
        //    have left behind. count=0 removes all matches.
        String userIdStr = userId.toString();
        try {
            redis.opsForList().remove(QUEUE_KEY, 0, userIdStr);
        } catch (Exception e) {
            log.debug("Defensive LREM failed for user {}: {}", userId, e.getMessage());
        }

        // 5) Enqueue + timestamp.
        long nowMs = System.currentTimeMillis();
        redis.opsForList().rightPush(QUEUE_KEY, userIdStr);
        redis.opsForHash().put(QUEUE_ENQUEUED_AT_KEY, userIdStr, Long.toString(nowMs));

        log.debug("Enqueued user={} token={} queueDepth={}", userId, fresh, queueDepth());
        return new EnqueueResult(fresh, Instant.ofEpochMilli(nowMs), false);
    }

    /**
     * Remove the user from the matchmaking queue. Idempotent — silently
     * succeeds if the user was never queued (Requirement 1.3).
     */
    public void cancel(Long userId) {
        if (userId == null) {
            return;
        }
        String userIdStr = userId.toString();
        String lockKey = ENQUEUE_LOCK_PREFIX + userId;

        try {
            redis.opsForList().remove(QUEUE_KEY, 0, userIdStr);
        } catch (Exception e) {
            log.debug("LREM during cancel failed for user {}: {}", userId, e.getMessage());
        }
        try {
            redis.opsForHash().delete(QUEUE_ENQUEUED_AT_KEY, userIdStr);
        } catch (Exception e) {
            log.debug("HDEL during cancel failed for user {}: {}", userId, e.getMessage());
        }
        try {
            redis.delete(lockKey);
            redis.delete(lockKey + ENQUEUE_TOKEN_SUFFIX);
        } catch (Exception e) {
            log.debug("DEL during cancel failed for user {}: {}", userId, e.getMessage());
        }

        log.debug("Cancelled queue entry for user={} queueDepth={}", userId, queueDepth());
    }

    /** Current queue depth — admin metric (Requirement 11.1). */
    public int queueDepth() {
        try {
            Long size = redis.opsForList().size(QUEUE_KEY);
            return size != null ? size.intValue() : 0;
        } catch (Exception e) {
            log.debug("queueDepth lookup failed: {}", e.getMessage());
            return 0;
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Scheduled ticks
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Drain pairs from the queue and hand them off to
     * {@link DuelService#pairAndStart(Long, Long)}. Single-threaded by
     * design — Spring's scheduled-task pool serializes invocations of the
     * same {@link Scheduled @Scheduled} method, so two LPOPs always see a
     * coherent queue state.
     *
     * <p>If only one user remains after the first {@code LPOP}, the loop
     * pushes them back to the head and exits the iteration so they remain
     * eligible for the next tick. The 60 s sorted-pair create-lock
     * absorbs any duplicate pairing attempt across ticks.
     */
    @Scheduled(fixedDelay = 250)
    public void pairLoopTick() {
        try {
            // Hard cap iterations per tick so a runaway queue cannot starve
            // other scheduled tasks.
            int maxPairsPerTick = 16;
            for (int i = 0; i < maxPairsPerTick; i++) {
                if (queueDepth() < 2) {
                    return;
                }

                String firstStr = redis.opsForList().leftPop(QUEUE_KEY);
                if (firstStr == null) {
                    return;
                }
                String secondStr = redis.opsForList().leftPop(QUEUE_KEY);
                if (secondStr == null) {
                    // Race: the queue had ≥2 entries when we checked depth
                    // but only one was popped. Push the first back to the
                    // head so the next tick can re-attempt.
                    redis.opsForList().leftPush(QUEUE_KEY, firstStr);
                    return;
                }

                Long first;
                Long second;
                try {
                    first = Long.parseLong(firstStr);
                    second = Long.parseLong(secondStr);
                } catch (NumberFormatException nfe) {
                    log.warn("Pair-loop dropped malformed queue entries: {}, {}", firstStr, secondStr);
                    continue;
                }

                if (first.equals(second)) {
                    // Should never happen — defensive only. Re-enqueue once.
                    log.warn("Pair-loop saw duplicate userId {} in queue, re-enqueuing once", first);
                    redis.opsForList().rightPush(QUEUE_KEY, firstStr);
                    continue;
                }

                long[] sorted = SeatAssigner.orderedPair(first, second);
                Long minId = sorted[0];
                Long maxId = sorted[1];

                String createLockKey = CREATE_LOCK_PREFIX + minId + "_" + maxId;
                Boolean lockAcquired = redis.opsForValue()
                        .setIfAbsent(createLockKey, "1", CREATE_LOCK_TTL_SEC, TimeUnit.SECONDS);
                if (Boolean.FALSE.equals(lockAcquired)) {
                    // Another tick is already creating this match — push both
                    // back to the head so they remain eligible.
                    log.debug("Pair-loop create-lock contended for ({},{}) — re-enqueueing", minId, maxId);
                    redis.opsForList().leftPush(QUEUE_KEY, secondStr);
                    redis.opsForList().leftPush(QUEUE_KEY, firstStr);
                    continue;
                }

                // Both users have been removed from the queue and the
                // sorted-pair lock is held. Clear their enqueue-timestamp
                // hash entries so the timeout sweep does not later try to
                // act on them.
                try {
                    redis.opsForHash().delete(QUEUE_ENQUEUED_AT_KEY, firstStr, secondStr);
                } catch (Exception e) {
                    log.debug("Failed to HDEL enqueued_at after pair: {}", e.getMessage());
                }

                try {
                    UUID matchId = duelService.pairAndStart(minId, maxId);
                    log.debug("Pair-loop started match {} for ({},{})", matchId, minId, maxId);
                } catch (DataIntegrityViolationException dive) {
                    // Partial-unique-index violation: at least one of the
                    // users already has an active match. Notify both and
                    // do NOT re-enqueue — see design.md "Race-Condition
                    // Resolution Table" row 8.4 / Req 10.2.
                    log.info("Pair-loop concurrent_match for ({},{}): {}",
                            minId, maxId, dive.getMessage());
                    emitPairingFailed(minId, "concurrent_match");
                    emitPairingFailed(maxId, "concurrent_match");
                } catch (DuelStateConflictException conflict) {
                    String reason = mapConflictReason(conflict.getCode());
                    log.info("Pair-loop {} for ({},{}): {}",
                            reason, minId, maxId, conflict.getMessage());
                    emitPairingFailed(minId, reason);
                    emitPairingFailed(maxId, reason);
                } catch (Exception ex) {
                    log.error("Pair-loop pairAndStart failed for ({},{}): {}",
                            minId, maxId, ex.getMessage(), ex);
                    emitPairingFailed(minId, "internal");
                    emitPairingFailed(maxId, "internal");
                }
            }
        } catch (Exception outer) {
            // Never let an exception escape — scheduler would suppress further
            // invocations of this method.
            log.error("pairLoopTick crashed: {}", outer.getMessage(), outer);
        }
    }

    /**
     * Evict users that have been queued longer than
     * {@code DUEL_QUEUE_TIMEOUT_SEC} and emit a {@code queue_timeout}
     * event on the per-user SSE channel (Requirement 1.5).
     *
     * <p>Runs every 5 s — well below the 120 s default timeout, so the
     * worst-case eviction latency is timeout + 5 s.
     */
    @Scheduled(fixedDelay = 5000)
    public void queueTimeoutSweep() {
        try {
            List<String> entries = redis.opsForList().range(QUEUE_KEY, 0, -1);
            if (entries == null || entries.isEmpty()) {
                return;
            }
            long nowMs = System.currentTimeMillis();
            long thresholdMs = queueTimeoutSec * 1000L;

            for (String userIdStr : entries) {
                Object enqueuedAtObj = redis.opsForHash().get(QUEUE_ENQUEUED_AT_KEY, userIdStr);
                if (enqueuedAtObj == null) {
                    continue;
                }
                long enqueuedAtMs;
                try {
                    enqueuedAtMs = Long.parseLong(enqueuedAtObj.toString());
                } catch (NumberFormatException nfe) {
                    log.warn("Queue-timeout sweep dropped malformed timestamp for user {}", userIdStr);
                    redis.opsForHash().delete(QUEUE_ENQUEUED_AT_KEY, userIdStr);
                    continue;
                }
                if (nowMs - enqueuedAtMs <= thresholdMs) {
                    continue;
                }

                Long userId;
                try {
                    userId = Long.parseLong(userIdStr);
                } catch (NumberFormatException nfe) {
                    log.warn("Queue-timeout sweep dropped malformed userId {}", userIdStr);
                    continue;
                }

                evictTimedOut(userId, userIdStr, nowMs - enqueuedAtMs);
            }
        } catch (Exception outer) {
            log.error("queueTimeoutSweep crashed: {}", outer.getMessage(), outer);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Internals
    // ─────────────────────────────────────────────────────────────────────

    private void evictTimedOut(Long userId, String userIdStr, long ageMs) {
        try {
            redis.opsForList().remove(QUEUE_KEY, 0, userIdStr);
        } catch (Exception e) {
            log.debug("LREM during eviction failed for user {}: {}", userId, e.getMessage());
        }
        try {
            redis.opsForHash().delete(QUEUE_ENQUEUED_AT_KEY, userIdStr);
        } catch (Exception e) {
            log.debug("HDEL during eviction failed for user {}: {}", userId, e.getMessage());
        }
        try {
            redis.delete(ENQUEUE_LOCK_PREFIX + userId);
            redis.delete(ENQUEUE_LOCK_PREFIX + userId + ENQUEUE_TOKEN_SUFFIX);
        } catch (Exception e) {
            log.debug("DEL during eviction failed for user {}: {}", userId, e.getMessage());
        }

        log.info("Queue timeout: evicted user={} ageMs={}", userId, ageMs);

        Map<String, Object> payload = new HashMap<>();
        payload.put("ts", Instant.now().toString());
        try {
            sseEmitterRegistry.sendEvent(userId, "queue_timeout", payload);
        } catch (Exception e) {
            log.debug("Failed to emit queue_timeout for user {}: {}", userId, e.getMessage());
        }
    }

    private void emitPairingFailed(Long userId, String reason) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("reason", reason);
        payload.put("ts", Instant.now().toString());
        try {
            sseEmitterRegistry.sendEvent(userId, "pairing_failed", payload);
        } catch (Exception e) {
            log.debug("Failed to emit pairing_failed for user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Translate a {@link DuelStateConflictException} code from
     * {@code DuelService.pairAndStart} into the {@code reason} string
     * carried by the {@code pairing_failed} SSE event. Unknown codes
     * fall back to {@code internal} so the frontend always sees a
     * recognized value.
     */
    private static String mapConflictReason(String code) {
        if (code == null) {
            return "internal";
        }
        return switch (code) {
            case "CONCURRENT_MATCH" -> "concurrent_match";
            case "NO_ELIGIBLE_PROBLEM" -> "no_eligible_problem";
            default -> "internal";
        };
    }

    /**
     * Reference to the Jackson mapper, retained for future SSE payload
     * serialization. Currently the registry handles serialization itself
     * via {@link SseEmitterRegistry#sendEvent(Long, String, Object)}, but
     * holding the bean here means downstream tasks (e.g. richer event
     * payloads added in 3.6) can move to manual JSON shaping without
     * re-wiring the constructor.
     */
    @SuppressWarnings("unused")
    ObjectMapper objectMapper() {
        return objectMapper;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Public DTO
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Response payload returned by {@link #enqueue(Long)}.
     *
     * <p>{@code alreadyQueued} is {@code true} when the call was a
     * no-op idempotent retry — the controller can use this to keep the
     * HTTP status at 200 (Requirement 1.2) while still letting clients
     * detect the retry case if they care.
     */
    public static final class EnqueueResult {
        public final String queueToken;
        public final Instant queuedAt;
        public final boolean alreadyQueued;

        public EnqueueResult(String queueToken, Instant queuedAt, boolean alreadyQueued) {
            this.queueToken = queueToken;
            this.queuedAt = queuedAt;
            this.alreadyQueued = alreadyQueued;
        }

        public String getQueueToken() {
            return queueToken;
        }

        public Instant getQueuedAt() {
            return queuedAt;
        }

        public boolean isAlreadyQueued() {
            return alreadyQueued;
        }
    }
}
