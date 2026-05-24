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
 * <p>V4 rework — three difficulty-bucketed queues
 * ({@code duel:queue:EASY}, {@code duel:queue:MEDIUM}, {@code duel:queue:HARD})
 * replace the single {@code duel:queue} list. The pair loop drains each
 * bucket independently; users are only matched against opponents who picked
 * the same difficulty.
 *
 * <p>The idempotency lock at {@code duel:enqueue:{userId}} now stores the
 * difficulty value the user picked (instead of the random fresh token) so
 * cancel knows which queue to LREM from. The companion key
 * {@code duel:enqueue:{userId}:token} keeps the queueToken so a retry inside
 * the 5 s window returns the same payload.
 */
@Service
public class MatchmakingService {

    private static final Logger log = LoggerFactory.getLogger(MatchmakingService.class);

    /** Per-difficulty queue key prefix. Final key = {@code duel:queue:EASY|MEDIUM|HARD}. */
    static final String QUEUE_KEY_PREFIX = "duel:queue:";

    /** Companion hash storing the millisecond enqueue timestamp per user. Pruned in the same sweep as the list. */
    static final String QUEUE_ENQUEUED_AT_KEY = "duel:queue:enqueued_at";

    /** Idempotency-lock prefix for the 5 s {@code SET NX EX 5} guard on enqueue. Stores the chosen difficulty. */
    static final String ENQUEUE_LOCK_PREFIX = "duel:enqueue:";

    /**
     * Suffix for the companion key that stores the {@code queueToken} so
     * that a second call within the idempotency window returns the same
     * payload.
     */
    static final String ENQUEUE_TOKEN_SUFFIX = ":token";

    /** Per-user post-match cooldown key prefix. */
    static final String COOLDOWN_PREFIX = "duel:cooldown:";

    /** Sorted-pair create-lock prefix used by the pair loop. Now keyed by difficulty too. */
    static final String CREATE_LOCK_PREFIX = "duel:create:";

    /** Idempotency window for enqueue requests. */
    private static final long ENQUEUE_LOCK_TTL_SEC = 5L;

    /** Sorted-pair create-lock TTL. */
    private static final long CREATE_LOCK_TTL_SEC = 60L;

    /** Allowed difficulty values. */
    private static final String DIFF_EASY = "EASY";
    private static final String DIFF_MEDIUM = "MEDIUM";
    private static final String DIFF_HARD = "HARD";
    private static final List<String> ALL_DIFFICULTIES = List.of(DIFF_EASY, DIFF_MEDIUM, DIFF_HARD);

    @Value("${DUEL_COOLDOWN_SEC:5}")
    long cooldownSec;

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
     * Add a user to the matchmaking queue for the given difficulty.
     *
     * <p>Difficulty validation is strict: only {@code EASY/MEDIUM/HARD} are
     * accepted (case-insensitive). Anything else throws
     * {@link IllegalArgumentException} which the controller maps to 400.
     *
     * @param userId     the authenticated user
     * @param difficulty one of {@code EASY/MEDIUM/HARD}; case-insensitive
     */
    public EnqueueResult enqueue(Long userId, String difficulty) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        String normalized = normalizeDifficulty(difficulty);
        String queueKey = queueKeyFor(normalized);

        // 1) Idempotency lock — absorb double-clicks within a 5 s window.
        //    Stores the difficulty so cancel knows which queue to scrub.
        String lockKey = ENQUEUE_LOCK_PREFIX + userId;
        String tokenKey = lockKey + ENQUEUE_TOKEN_SUFFIX;
        String fresh = UUID.randomUUID().toString();

        Boolean acquired = redis.opsForValue()
                .setIfAbsent(lockKey, normalized, ENQUEUE_LOCK_TTL_SEC, TimeUnit.SECONDS);

        if (Boolean.FALSE.equals(acquired)) {
            String existingToken = redis.opsForValue().get(tokenKey);
            String existingDifficulty = redis.opsForValue().get(lockKey);
            if (existingToken != null) {
                log.debug("Enqueue idempotent: user={} token={} diff={}", userId, existingToken, existingDifficulty);
                return new EnqueueResult(existingToken, Instant.now(), true,
                        existingDifficulty != null ? existingDifficulty : normalized);
            }
            // Lost the race entirely; keep going as if first-time.
        }

        // Persist token + difficulty companion keys with the same TTL.
        redis.opsForValue().set(tokenKey, fresh, ENQUEUE_LOCK_TTL_SEC, TimeUnit.SECONDS);

        // 2) Cooldown gate.
        String cooldownKey = COOLDOWN_PREFIX + userId;
        Long cooldownTtl = redis.getExpire(cooldownKey, TimeUnit.SECONDS);
        if (cooldownTtl != null && cooldownTtl > 0) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("retryAfterSec", cooldownTtl);
            throw new DuelStateConflictException("COOLDOWN_ACTIVE", payload);
        }

        // 3) Active-match gate.
        List<DuelMatch> active = duelMatchRepository.findActiveByUser(userId);
        Optional<DuelMatch> existing = active.stream().findFirst();
        if (existing.isPresent()) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("matchId", existing.get().getMatchId().toString());
            throw new DuelStateConflictException("ALREADY_IN_MATCH", payload);
        }

        // 4) Defensive cleanup — scrub stale entries the user may have left
        //    behind in any of the three buckets.
        String userIdStr = userId.toString();
        for (String diff : ALL_DIFFICULTIES) {
            try {
                redis.opsForList().remove(QUEUE_KEY_PREFIX + diff, 0, userIdStr);
            } catch (Exception e) {
                log.debug("Defensive LREM failed for user {} bucket {}: {}", userId, diff, e.getMessage());
            }
        }

        // 5) Enqueue + timestamp.
        long nowMs = System.currentTimeMillis();
        redis.opsForList().rightPush(queueKey, userIdStr);
        redis.opsForHash().put(QUEUE_ENQUEUED_AT_KEY, userIdStr, Long.toString(nowMs));

        log.debug("Enqueued user={} token={} difficulty={} queueDepth={}",
                userId, fresh, normalized, queueDepth(normalized));
        return new EnqueueResult(fresh, Instant.ofEpochMilli(nowMs), false, normalized);
    }

    /**
     * Remove the user from the matchmaking queue. Idempotent.
     *
     * <p>Reads the lock key to discover which difficulty bucket the user
     * landed in, then LREMs from that specific list. Falls back to scrubbing
     * all three buckets if the lock key is missing (e.g. expired).
     */
    public void cancel(Long userId) {
        if (userId == null) {
            return;
        }
        String userIdStr = userId.toString();
        String lockKey = ENQUEUE_LOCK_PREFIX + userId;

        String stored = null;
        try {
            stored = redis.opsForValue().get(lockKey);
        } catch (Exception e) {
            log.debug("GET lock during cancel failed for user {}: {}", userId, e.getMessage());
        }

        if (stored != null && ALL_DIFFICULTIES.contains(stored)) {
            try {
                redis.opsForList().remove(QUEUE_KEY_PREFIX + stored, 0, userIdStr);
            } catch (Exception e) {
                log.debug("LREM during cancel failed for user {} bucket {}: {}",
                        userId, stored, e.getMessage());
            }
        } else {
            // Defensive: lock may have expired or stored something unexpected.
            // Scrub all three buckets just in case.
            for (String diff : ALL_DIFFICULTIES) {
                try {
                    redis.opsForList().remove(QUEUE_KEY_PREFIX + diff, 0, userIdStr);
                } catch (Exception e) {
                    log.debug("LREM during cancel-fallback failed for user {} bucket {}: {}",
                            userId, diff, e.getMessage());
                }
            }
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

        log.debug("Cancelled queue entry for user={} bucket={} totalDepth={}",
                userId, stored, queueDepth());
    }

    /** Total queue depth across all three difficulty buckets. */
    public int queueDepth() {
        int total = 0;
        for (String diff : ALL_DIFFICULTIES) {
            total += queueDepth(diff);
        }
        return total;
    }

    /** Queue depth for a single difficulty bucket. */
    public int queueDepth(String difficulty) {
        try {
            String key = QUEUE_KEY_PREFIX + normalizeDifficulty(difficulty);
            Long size = redis.opsForList().size(key);
            return size != null ? size.intValue() : 0;
        } catch (Exception e) {
            log.debug("queueDepth lookup failed for {}: {}", difficulty, e.getMessage());
            return 0;
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Scheduled ticks
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Drain pairs from each per-difficulty queue. Single-threaded by design —
     * Spring serializes invocations of this method.
     */
    @Scheduled(fixedDelay = 250)
    public void pairLoopTick() {
        try {
            int maxPairsPerTickPerBucket = 8;
            for (String difficulty : ALL_DIFFICULTIES) {
                String queueKey = QUEUE_KEY_PREFIX + difficulty;
                for (int i = 0; i < maxPairsPerTickPerBucket; i++) {
                    if (queueDepth(difficulty) < 2) {
                        break;
                    }

                    String firstStr = redis.opsForList().leftPop(queueKey);
                    if (firstStr == null) {
                        break;
                    }
                    String secondStr = redis.opsForList().leftPop(queueKey);
                    if (secondStr == null) {
                        redis.opsForList().leftPush(queueKey, firstStr);
                        break;
                    }

                    Long first;
                    Long second;
                    try {
                        first = Long.parseLong(firstStr);
                        second = Long.parseLong(secondStr);
                    } catch (NumberFormatException nfe) {
                        log.warn("Pair-loop dropped malformed queue entries [{}]: {}, {}",
                                difficulty, firstStr, secondStr);
                        continue;
                    }

                    if (first.equals(second)) {
                        log.warn("Pair-loop saw duplicate userId {} in {} queue, re-enqueuing once",
                                first, difficulty);
                        redis.opsForList().rightPush(queueKey, firstStr);
                        continue;
                    }

                    long[] sorted = SeatAssigner.orderedPair(first, second);
                    Long minId = sorted[0];
                    Long maxId = sorted[1];

                    String createLockKey = CREATE_LOCK_PREFIX + difficulty + ":" + minId + "_" + maxId;
                    Boolean lockAcquired = redis.opsForValue()
                            .setIfAbsent(createLockKey, "1", CREATE_LOCK_TTL_SEC, TimeUnit.SECONDS);
                    if (Boolean.FALSE.equals(lockAcquired)) {
                        log.debug("Pair-loop create-lock contended for ({},{}) [{}] — re-enqueueing",
                                minId, maxId, difficulty);
                        redis.opsForList().leftPush(queueKey, secondStr);
                        redis.opsForList().leftPush(queueKey, firstStr);
                        continue;
                    }

                    try {
                        redis.opsForHash().delete(QUEUE_ENQUEUED_AT_KEY, firstStr, secondStr);
                    } catch (Exception e) {
                        log.debug("Failed to HDEL enqueued_at after pair: {}", e.getMessage());
                    }

                    try {
                        UUID matchId = duelService.pairAndStart(minId, maxId, difficulty);
                        log.debug("Pair-loop started match {} for ({},{}) [{}]",
                                matchId, minId, maxId, difficulty);
                    } catch (DataIntegrityViolationException dive) {
                        log.info("Pair-loop concurrent_match for ({},{}) [{}]: {}",
                                minId, maxId, difficulty, dive.getMessage());
                        emitPairingFailed(minId, "concurrent_match");
                        emitPairingFailed(maxId, "concurrent_match");
                    } catch (DuelStateConflictException conflict) {
                        String reason = mapConflictReason(conflict.getCode());
                        log.info("Pair-loop {} for ({},{}) [{}]: {}",
                                reason, minId, maxId, difficulty, conflict.getMessage());
                        emitPairingFailed(minId, reason);
                        emitPairingFailed(maxId, reason);
                    } catch (Exception ex) {
                        log.error("Pair-loop pairAndStart failed for ({},{}) [{}]: {}",
                                minId, maxId, difficulty, ex.getMessage(), ex);
                        emitPairingFailed(minId, "internal");
                        emitPairingFailed(maxId, "internal");
                    }
                }
            }
        } catch (Exception outer) {
            log.error("pairLoopTick crashed: {}", outer.getMessage(), outer);
        }
    }

    /**
     * Evict users that have been queued longer than
     * {@code DUEL_QUEUE_TIMEOUT_SEC} from any bucket.
     */
    @Scheduled(fixedDelay = 5000)
    public void queueTimeoutSweep() {
        try {
            long nowMs = System.currentTimeMillis();
            long thresholdMs = queueTimeoutSec * 1000L;

            for (String difficulty : ALL_DIFFICULTIES) {
                String queueKey = QUEUE_KEY_PREFIX + difficulty;
                List<String> entries = redis.opsForList().range(queueKey, 0, -1);
                if (entries == null || entries.isEmpty()) {
                    continue;
                }

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

                    evictTimedOut(userId, userIdStr, queueKey, nowMs - enqueuedAtMs);
                }
            }
        } catch (Exception outer) {
            log.error("queueTimeoutSweep crashed: {}", outer.getMessage(), outer);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Internals
    // ─────────────────────────────────────────────────────────────────────

    /** Normalize an incoming difficulty string. Throws on anything outside the allowed set. */
    private static String normalizeDifficulty(String difficulty) {
        if (difficulty == null) {
            throw new IllegalArgumentException("difficulty is required");
        }
        String upper = difficulty.trim().toUpperCase();
        if (!ALL_DIFFICULTIES.contains(upper)) {
            throw new IllegalArgumentException(
                    "difficulty must be one of EASY, MEDIUM, HARD (got: " + difficulty + ")");
        }
        return upper;
    }

    private static String queueKeyFor(String normalizedDifficulty) {
        return QUEUE_KEY_PREFIX + normalizedDifficulty;
    }

    private void evictTimedOut(Long userId, String userIdStr, String queueKey, long ageMs) {
        try {
            redis.opsForList().remove(queueKey, 0, userIdStr);
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

        log.info("Queue timeout: evicted user={} from {} ageMs={}", userId, queueKey, ageMs);

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

    @SuppressWarnings("unused")
    ObjectMapper objectMapper() {
        return objectMapper;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Public DTO
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Response payload returned by {@link #enqueue(Long, String)}.
     */
    public static final class EnqueueResult {
        public final String queueToken;
        public final Instant queuedAt;
        public final boolean alreadyQueued;
        public final String difficulty;

        public EnqueueResult(String queueToken, Instant queuedAt, boolean alreadyQueued, String difficulty) {
            this.queueToken = queueToken;
            this.queuedAt = queuedAt;
            this.alreadyQueued = alreadyQueued;
            this.difficulty = difficulty;
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

        public String getDifficulty() {
            return difficulty;
        }
    }
}
