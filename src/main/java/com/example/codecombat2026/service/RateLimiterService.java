package com.example.codecombat2026.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Sliding-window rate limiter using Valkey atomic INCR + TTL.
 * Prevents submission spamming — max 5 submissions per user per 30 seconds.
 */
@Component
public class RateLimiterService {

    @Autowired
    private StringRedisTemplate redis;

    private static final int MAX_SUBMISSIONS = 5;
    private static final int WINDOW_SECONDS  = 30;

    /**
     * Returns true if the user is allowed to submit, false if rate-limited.
     * Atomic: INCR is a single Redis command — safe under concurrency.
     */
    public boolean allowSubmission(Long userId) {
        String key = "ratelimit:submit:" + userId;
        try {
            Long count = redis.opsForValue().increment(key);
            if (count != null && count == 1) {
                // First request in this window — set expiry
                redis.expire(key, Duration.ofSeconds(WINDOW_SECONDS));
            }
            return count != null && count <= MAX_SUBMISSIONS;
        } catch (Exception e) {
            // If Valkey is unreachable, allow the request (fail open)
            return true;
        }
    }

    /** Returns seconds until the rate limit window resets. */
    public long getRetryAfterSeconds(Long userId) {
        String key = "ratelimit:submit:" + userId;
        try {
            Long ttl = redis.getExpire(key, TimeUnit.SECONDS);
            return ttl != null && ttl > 0 ? ttl : WINDOW_SECONDS;
        } catch (Exception e) {
            return WINDOW_SECONDS;
        }
    }
}
