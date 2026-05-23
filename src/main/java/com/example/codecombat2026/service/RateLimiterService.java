package com.example.codecombat2026.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Sliding-window rate limiter.
 *
 * Primary path: atomic Valkey INCR + TTL — survives across multiple
 * application instances and matches across HTTP nodes.
 *
 * Fallback path: per-JVM ConcurrentHashMap with a fixed window. Activated
 * automatically when Valkey errors are detected. This way a partial Valkey
 * outage does NOT remove rate limits during a contest spike — the very
 * moment they matter most. Per-JVM enforcement is weaker than per-cluster,
 * but strictly tighter than "fail open" because each instance still drops
 * abusers.
 */
@Component
public class RateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);

    @Autowired
    private StringRedisTemplate redis;

    private static final int MAX_SUBMISSIONS = 5;
    private static final int WINDOW_SECONDS  = 30;

    /** Local fallback window-bucket: userId → (windowStartMs, count). */
    private final ConcurrentHashMap<Long, LocalBucket> localBuckets = new ConcurrentHashMap<>();

    /**
     * Returns true if the user is allowed to submit, false if rate-limited.
     */
    public boolean allowSubmission(Long userId) {
        try {
            String key = "ratelimit:submit:" + userId;
            Long count = redis.opsForValue().increment(key);
            if (count != null && count == 1) {
                redis.expire(key, Duration.ofSeconds(WINDOW_SECONDS));
            }
            return count != null && count <= MAX_SUBMISSIONS;
        } catch (Exception e) {
            // Valkey unreachable — fall back to per-JVM tracking instead of fail-open
            log.warn("Valkey rate-limiter unavailable, falling back to local: {}", e.getMessage());
            return allowLocally(userId);
        }
    }

    /** Returns seconds until the rate limit window resets. */
    public long getRetryAfterSeconds(Long userId) {
        try {
            String key = "ratelimit:submit:" + userId;
            Long ttl = redis.getExpire(key, TimeUnit.SECONDS);
            return ttl != null && ttl > 0 ? ttl : WINDOW_SECONDS;
        } catch (Exception e) {
            // Local fallback — compute from bucket
            LocalBucket b = localBuckets.get(userId);
            if (b == null) return WINDOW_SECONDS;
            long elapsed = (System.currentTimeMillis() - b.windowStartMs) / 1000L;
            long remaining = WINDOW_SECONDS - elapsed;
            return remaining > 0 ? remaining : WINDOW_SECONDS;
        }
    }

    private boolean allowLocally(Long userId) {
        long now = System.currentTimeMillis();
        LocalBucket b = localBuckets.compute(userId, (k, existing) -> {
            if (existing == null || (now - existing.windowStartMs) >= WINDOW_SECONDS * 1000L) {
                LocalBucket fresh = new LocalBucket();
                fresh.windowStartMs = now;
                fresh.count = new AtomicInteger(0);
                return fresh;
            }
            return existing;
        });
        int count = b.count.incrementAndGet();
        return count <= MAX_SUBMISSIONS;
    }

    private static class LocalBucket {
        long windowStartMs;
        AtomicInteger count;
    }
}
