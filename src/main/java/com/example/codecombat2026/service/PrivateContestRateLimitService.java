package com.example.codecombat2026.service;

import com.example.codecombat2026.exception.TooManyRequestsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Rate limiting service for private contest operations using Valkey.
 * Implements sliding window rate limits with INCR + TTL pattern.
 * 
 * Requirements:
 * - 24.1: Contest creation - 5 per hour per user
 * - 24.2: AI problem generation - 5 per day per user
 * - 24.3: Invite regeneration - 10 per hour per contest
 * - 24.4: Invite acceptance - 100 per hour per contest
 * - 24.5: Throw TooManyRequestsException with Retry-After information
 */
@Service
public class PrivateContestRateLimitService {

    private static final Logger log = LoggerFactory.getLogger(PrivateContestRateLimitService.class);

    @Autowired
    private StringRedisTemplate redis;

    // Rate limits per requirement 24
    private static final int CONTEST_CREATION_LIMIT = 5;
    private static final int CONTEST_CREATION_WINDOW_SECONDS = 3600; // 1 hour

    private static final int AI_PROBLEM_GEN_LIMIT = 5;
    private static final int AI_PROBLEM_GEN_WINDOW_SECONDS = 86400; // 1 day

    private static final int INVITE_REGEN_LIMIT = 10;
    private static final int INVITE_REGEN_WINDOW_SECONDS = 3600; // 1 hour

    private static final int INVITE_ACCEPT_LIMIT = 100;
    private static final int INVITE_ACCEPT_WINDOW_SECONDS = 3600; // 1 hour

    /**
     * Check contest creation rate limit for a Contest_Host.
     * Enforces 5 contest creation requests per hour sliding window.
     * 
     * @param userId Contest_Host user ID
     * @throws TooManyRequestsException if rate limit exceeded
     */
    public void checkContestCreationLimit(Long userId) {
        String key = "ratelimit:contest:create:user:" + userId;
        checkRateLimit(
            key,
            CONTEST_CREATION_LIMIT,
            CONTEST_CREATION_WINDOW_SECONDS,
            "You have reached your hourly limit of 5 contest creation requests. Please try again later."
        );
    }

    /**
     * Check AI problem generation rate limit for a Contest_Host.
     * Enforces 5 AI generation requests per day sliding window.
     * 
     * @param userId Contest_Host user ID
     * @throws TooManyRequestsException if rate limit exceeded
     */
    public void checkAIProblemGenLimit(Long userId) {
        String key = "ratelimit:ai:problem:gen:user:" + userId;
        checkRateLimit(
            key,
            AI_PROBLEM_GEN_LIMIT,
            AI_PROBLEM_GEN_WINDOW_SECONDS,
            "You have reached your daily limit of 5 AI-generated problems. The limit resets in {{retryAfter}}."
        );
    }

    /**
     * Check invite link regeneration rate limit for a Private_Contest.
     * Enforces 10 regenerations per hour sliding window.
     * 
     * @param contestId Private_Contest ID
     * @throws TooManyRequestsException if rate limit exceeded
     */
    public void checkInviteRegenLimit(Long contestId) {
        String key = "ratelimit:invite:regen:contest:" + contestId;
        checkRateLimit(
            key,
            INVITE_REGEN_LIMIT,
            INVITE_REGEN_WINDOW_SECONDS,
            "You have reached your hourly limit of 10 invite link regenerations for this contest."
        );
    }

    /**
     * Check invite acceptance rate limit for a Private_Contest.
     * Enforces 100 acceptances per hour sliding window to prevent bot registrations.
     * 
     * @param contestId Private_Contest ID
     * @throws TooManyRequestsException if rate limit exceeded
     */
    public void checkInviteAcceptLimit(Long contestId) {
        String key = "ratelimit:invite:accept:contest:" + contestId;
        checkRateLimit(
            key,
            INVITE_ACCEPT_LIMIT,
            INVITE_ACCEPT_WINDOW_SECONDS,
            "This contest has received too many join requests in a short time. Please try again later."
        );
    }

    /**
     * Generic rate limit checker using Valkey INCR with TTL.
     * 
     * @param key Valkey key for the rate limit counter
     * @param limit Maximum allowed count in the window
     * @param windowSeconds Time window in seconds
     * @param errorMessage Message to include in exception (may contain {{retryAfter}} placeholder)
     * @throws TooManyRequestsException if rate limit exceeded
     */
    private void checkRateLimit(String key, int limit, int windowSeconds, String errorMessage) {
        try {
            // Increment counter
            Long count = redis.opsForValue().increment(key);
            
            if (count == null) {
                count = 1L;
            }
            
            // Set TTL on first increment
            if (count == 1) {
                redis.expire(key, Duration.ofSeconds(windowSeconds));
            }
            
            // Check if limit exceeded
            if (count > limit) {
                long retryAfterSeconds = getRetryAfterSeconds(key, windowSeconds);
                String message = errorMessage.replace("{{retryAfter}}", formatDuration(retryAfterSeconds));
                
                log.warn("Rate limit exceeded for key={}, count={}, limit={}, retryAfter={}s", 
                    key, count, limit, retryAfterSeconds);
                
                // Throw exception with Retry-After information (requirement 24.5)
                throw new TooManyRequestsException(message, retryAfterSeconds);
            }
            
        } catch (TooManyRequestsException e) {
            // Re-throw rate limit exceptions
            throw e;
        } catch (Exception e) {
            // Log Valkey errors but don't block the operation
            // In production, you might want stricter handling
            log.error("Error checking rate limit for key={}: {}", key, e.getMessage(), e);
            // Fail open - allow the request if Valkey is down
        }
    }

    /**
     * Get the number of seconds until the rate limit window resets.
     * 
     * @param key Valkey key for the rate limit counter
     * @param defaultWindowSeconds Default window size if TTL cannot be determined
     * @return Seconds until reset
     */
    private long getRetryAfterSeconds(String key, int defaultWindowSeconds) {
        try {
            Long ttl = redis.getExpire(key, TimeUnit.SECONDS);
            return (ttl != null && ttl > 0) ? ttl : defaultWindowSeconds;
        } catch (Exception e) {
            log.warn("Error getting TTL for key={}: {}", key, e.getMessage());
            return defaultWindowSeconds;
        }
    }

    /**
     * Format duration in seconds to a human-readable string.
     * 
     * @param seconds Duration in seconds
     * @return Formatted string (e.g., "45 minutes", "2 hours", "23 hours")
     */
    private String formatDuration(long seconds) {
        if (seconds < 60) {
            return seconds + " seconds";
        } else if (seconds < 3600) {
            long minutes = seconds / 60;
            return minutes + (minutes == 1 ? " minute" : " minutes");
        } else {
            long hours = seconds / 3600;
            return hours + (hours == 1 ? " hour" : " hours");
        }
    }

    /**
     * Get retry-after information for contest creation rate limit.
     * Used by exception handlers to set Retry-After header.
     * 
     * @param userId Contest_Host user ID
     * @return Seconds until the limit resets
     */
    public long getContestCreationRetryAfter(Long userId) {
        String key = "ratelimit:contest:create:user:" + userId;
        return getRetryAfterSeconds(key, CONTEST_CREATION_WINDOW_SECONDS);
    }

    /**
     * Get retry-after information for AI problem generation rate limit.
     * 
     * @param userId Contest_Host user ID
     * @return Seconds until the limit resets
     */
    public long getAIProblemGenRetryAfter(Long userId) {
        String key = "ratelimit:ai:problem:gen:user:" + userId;
        return getRetryAfterSeconds(key, AI_PROBLEM_GEN_WINDOW_SECONDS);
    }

    /**
     * Get retry-after information for invite regeneration rate limit.
     * 
     * @param contestId Private_Contest ID
     * @return Seconds until the limit resets
     */
    public long getInviteRegenRetryAfter(Long contestId) {
        String key = "ratelimit:invite:regen:contest:" + contestId;
        return getRetryAfterSeconds(key, INVITE_REGEN_WINDOW_SECONDS);
    }

    /**
     * Get retry-after information for invite acceptance rate limit.
     * 
     * @param contestId Private_Contest ID
     * @return Seconds until the limit resets
     */
    public long getInviteAcceptRetryAfter(Long contestId) {
        String key = "ratelimit:invite:accept:contest:" + contestId;
        return getRetryAfterSeconds(key, INVITE_ACCEPT_WINDOW_SECONDS);
    }
}
