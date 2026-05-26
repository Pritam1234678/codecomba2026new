package com.example.codecombat2026.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * JWT revocation list backed by Valkey. Blacklist entries are keyed by jti
 * and expire automatically when the underlying token would have expired
 * anyway, so the set never grows unbounded.
 *
 * Failure mode: if Valkey is unreachable we fail OPEN — i.e. report tokens
 * as not-blacklisted. Rationale: signature validation still gates auth, and
 * the alternative (fail closed) would lock everyone out during a cache
 * outage. Lockouts and password-change invalidation use a separate
 * "invalidate-before" timestamp scheme that is also fail-open for the same
 * reason.
 */
@Service
public class JwtBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(JwtBlacklistService.class);

    @Autowired
    private StringRedisTemplate redis;

    /** Add a jti to the blacklist with TTL equal to its remaining lifetime. */
    public void blacklist(String jti, long ttlSec) {
        if (jti == null || jti.isBlank() || ttlSec <= 0) return;
        try {
            redis.opsForValue().set("jwt:blacklist:" + jti, "1", Duration.ofSeconds(ttlSec));
        } catch (Exception e) {
            log.warn("Failed to write JWT blacklist entry for jti={}: {}", jti, e.getMessage());
        }
    }

    public boolean isBlacklisted(String jti) {
        if (jti == null || jti.isBlank()) return false;
        try {
            return Boolean.TRUE.equals(redis.hasKey("jwt:blacklist:" + jti));
        } catch (Exception e) {
            // Fail open — JWT signature validation still applies.
            return false;
        }
    }

    // ── Password-change invalidation (per-user iat cutoff) ──────────────────
    //
    // When a user changes their password we set a "min issued-at" timestamp.
    // The auth filter compares each incoming token's iat claim against this
    // value. Tokens issued before the cutoff are rejected.

    public void invalidateAllUserTokens(Long userId) {
        if (userId == null) return;
        try {
            // 7-day TTL — well past any reasonable JWT lifetime.
            redis.opsForValue().set(
                    "auth:invalidate-before:" + userId,
                    String.valueOf(System.currentTimeMillis()),
                    Duration.ofDays(7));
        } catch (Exception e) {
            log.warn("Failed to set invalidate-before for userId={}: {}", userId, e.getMessage());
        }
    }

    /** Returns the cutoff in epoch millis, or 0 if no cutoff is set / Valkey down. */
    public long invalidateBeforeMillis(Long userId) {
        if (userId == null) return 0L;
        try {
            String v = redis.opsForValue().get("auth:invalidate-before:" + userId);
            if (v == null || v.isBlank()) return 0L;
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            return 0L;
        } catch (Exception e) {
            return 0L;
        }
    }
}
