package com.example.codecombat2026.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Auth-flow rate limiter and account lockout.
 *
 * Mirrors the {@link RateLimiterService} pattern: Valkey INCR + EX as the
 * primary path, per-JVM ConcurrentHashMap as a fallback when Valkey is
 * unreachable. This means a partial cache outage cannot turn auth into a
 * fail-open free-for-all.
 *
 * Key namespaces:
 *   auth:rl:login:{ip}        — 5 / 60s
 *   auth:rl:register:{ip}     — 3 / 3600s
 *   auth:rl:reset:{email}     — 3 / 3600s
 *   auth:rl:reset:ip:{ip}     — 3 / 3600s
 *   auth:rl:forgot-user:{ip}  — 3 / 3600s
 *
 *   auth:fail:{username}      — failed-login counter, 15min sliding TTL
 *   auth:locked:{username}    — lock flag (presence == locked), 15min TTL
 */
@Service
public class AuthRateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(AuthRateLimiterService.class);

    @Autowired
    private StringRedisTemplate redis;

    // ── Config (tunable) ──
    private static final int LOGIN_MAX        = 5;
    private static final int LOGIN_WINDOW_S   = 60;
    private static final int REGISTER_MAX     = 3;
    private static final int REGISTER_WINDOW_S = 3600;
    private static final int RESET_MAX        = 3;
    private static final int RESET_WINDOW_S   = 3600;
    private static final int FORGOT_USER_MAX  = 3;
    private static final int FORGOT_USER_WINDOW_S = 3600;

    private static final int LOCK_THRESHOLD   = 5;
    private static final int LOCK_TTL_S       = 900; // 15 minutes
    private static final int FAIL_TTL_S       = 900; // failed counter expires with lockout

    // ── Local fallback maps ──
    private final ConcurrentHashMap<String, LocalBucket> localBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long>        localLocked  = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> localFails = new ConcurrentHashMap<>();

    // ───────────────────── Public rate-limit API ─────────────────────

    public boolean allowLogin(String ip) {
        return allow("auth:rl:login:" + safe(ip), LOGIN_MAX, LOGIN_WINDOW_S);
    }

    public long loginRetryAfter(String ip) {
        return retryAfter("auth:rl:login:" + safe(ip), LOGIN_WINDOW_S);
    }

    public boolean allowRegister(String ip) {
        return allow("auth:rl:register:" + safe(ip), REGISTER_MAX, REGISTER_WINDOW_S);
    }

    public long registerRetryAfter(String ip) {
        return retryAfter("auth:rl:register:" + safe(ip), REGISTER_WINDOW_S);
    }

    /** Both the per-email AND per-IP buckets must allow. */
    public boolean allowPasswordReset(String email, String ip) {
        boolean emailOk = allow("auth:rl:reset:" + safe(email), RESET_MAX, RESET_WINDOW_S);
        boolean ipOk    = allow("auth:rl:reset:ip:" + safe(ip), RESET_MAX, RESET_WINDOW_S);
        return emailOk && ipOk;
    }

    public long passwordResetRetryAfter(String email) {
        return retryAfter("auth:rl:reset:" + safe(email), RESET_WINDOW_S);
    }

    public boolean allowForgotUsername(String ip) {
        return allow("auth:rl:forgot-user:" + safe(ip), FORGOT_USER_MAX, FORGOT_USER_WINDOW_S);
    }

    public long forgotUsernameRetryAfter(String ip) {
        return retryAfter("auth:rl:forgot-user:" + safe(ip), FORGOT_USER_WINDOW_S);
    }

    // ───────────────────── Account lockout ─────────────────────

    /**
     * Increment failed-login counter for username. If it reaches the
     * threshold, set the lock flag. Username is stored lowercased for case
     * insensitivity; logins typically already match exactly but lockout
     * should not be bypassed by case toggling.
     */
    public void recordFailedLogin(String username) {
        if (username == null || username.isBlank()) return;
        String u = safe(username.toLowerCase());
        String failKey = "auth:fail:" + u;
        String lockKey = "auth:locked:" + u;

        try {
            Long count = redis.opsForValue().increment(failKey);
            if (count != null && count == 1) {
                redis.expire(failKey, Duration.ofSeconds(FAIL_TTL_S));
            }
            if (count != null && count >= LOCK_THRESHOLD) {
                redis.opsForValue().set(lockKey, "1", Duration.ofSeconds(LOCK_TTL_S));
                log.warn("Account locked due to failed logins: username={} count={}", username, count);
            }
        } catch (Exception e) {
            log.warn("Valkey unavailable for recordFailedLogin, using local: {}", e.getMessage());
            int n = localFails.computeIfAbsent(u, k -> new AtomicInteger(0)).incrementAndGet();
            if (n >= LOCK_THRESHOLD) {
                localLocked.put(u, System.currentTimeMillis() + LOCK_TTL_S * 1000L);
                log.warn("Account locked (local) due to failed logins: username={} count={}", username, n);
            }
        }
    }

    public boolean isAccountLocked(String username) {
        if (username == null || username.isBlank()) return false;
        String u = safe(username.toLowerCase());
        try {
            return Boolean.TRUE.equals(redis.hasKey("auth:locked:" + u));
        } catch (Exception e) {
            Long until = localLocked.get(u);
            if (until == null) return false;
            if (until <= System.currentTimeMillis()) {
                localLocked.remove(u);
                localFails.remove(u);
                return false;
            }
            return true;
        }
    }

    /** Seconds remaining on the current lock, or 0 if not locked. */
    public long lockedUntilSec(String username) {
        if (username == null || username.isBlank()) return 0;
        String u = safe(username.toLowerCase());
        try {
            Long ttl = redis.getExpire("auth:locked:" + u, TimeUnit.SECONDS);
            return ttl != null && ttl > 0 ? ttl : 0;
        } catch (Exception e) {
            Long until = localLocked.get(u);
            if (until == null) return 0;
            long remaining = (until - System.currentTimeMillis()) / 1000L;
            return remaining > 0 ? remaining : 0;
        }
    }

    /** Call after a successful login to clear the failure counter and any lock. */
    public void clearFailedLogins(String username) {
        if (username == null || username.isBlank()) return;
        String u = safe(username.toLowerCase());
        try {
            redis.delete("auth:fail:" + u);
            redis.delete("auth:locked:" + u);
        } catch (Exception e) {
            log.debug("Valkey unavailable for clearFailedLogins, clearing locally: {}", e.getMessage());
        }
        localFails.remove(u);
        localLocked.remove(u);
    }

    // ───────────────────── Internals ─────────────────────

    private boolean allow(String key, int max, int windowSec) {
        try {
            Long count = redis.opsForValue().increment(key);
            if (count != null && count == 1) {
                redis.expire(key, Duration.ofSeconds(windowSec));
            }
            return count != null && count <= max;
        } catch (Exception e) {
            log.warn("Valkey rate-limiter unavailable, falling back to local: {}", e.getMessage());
            return allowLocally(key, max, windowSec);
        }
    }

    private long retryAfter(String key, int windowSec) {
        try {
            Long ttl = redis.getExpire(key, TimeUnit.SECONDS);
            return ttl != null && ttl > 0 ? ttl : windowSec;
        } catch (Exception e) {
            LocalBucket b = localBuckets.get(key);
            if (b == null) return windowSec;
            long elapsed = (System.currentTimeMillis() - b.windowStartMs) / 1000L;
            long remaining = windowSec - elapsed;
            return remaining > 0 ? remaining : windowSec;
        }
    }

    private boolean allowLocally(String key, int max, int windowSec) {
        long now = System.currentTimeMillis();
        LocalBucket b = localBuckets.compute(key, (k, existing) -> {
            if (existing == null || (now - existing.windowStartMs) >= windowSec * 1000L) {
                LocalBucket fresh = new LocalBucket();
                fresh.windowStartMs = now;
                fresh.count = new AtomicInteger(0);
                fresh.windowSec = windowSec;
                return fresh;
            }
            return existing;
        });
        int count = b.count.incrementAndGet();
        return count <= max;
    }

    /** Replace anything outside ASCII alnum/._@-/: with underscore so keys are tame. */
    private String safe(String s) {
        if (s == null) return "_";
        return s.replaceAll("[^A-Za-z0-9._@\\-:]", "_");
    }

    private static class LocalBucket {
        long windowStartMs;
        int  windowSec;
        AtomicInteger count;
    }
}
