package com.example.codecombat2026.proctoring.service;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import com.example.codecombat2026.proctoring.config.ProctoringConfig;

/**
 * Per-{@code Proctoring_Session} rate limiter for inbound event frames and
 * screenshot uploads, backed by Valkey.
 *
 * <p>Two independent surfaces:
 *
 * <ul>
 *   <li><b>Event frames</b> — {@link #allowEventFrame(Long)} maintains a
 *       sorted-set sliding window {@code proctoring:rl:events:{sid}} keyed on
 *       millisecond client-receive timestamp. Admission cap is
 *       {@code eventRateLimitPerSecond * 10} entries inside the trailing
 *       10-second window (Req 17.1, 17.2). On rejection the same call
 *       atomically attempts {@code SET NX EX 10} on
 *       {@code proctoring:rl:events:{sid}:notified} and reports back via
 *       {@link RateLimitDecision#shouldNotify()} so the caller emits at most
 *       one {@code RATE_LIMIT_EXCEEDED} Suspicious_Event per 10 s window
 *       (Req 17.3).</li>
 *   <li><b>Screenshot uploads</b> — {@link #allowScreenshotUpload(Long)} runs
 *       {@code INCR proctoring:rl:shots:{sid}} with a 60 s expiry seeded on
 *       the first hit; admission cap is
 *       {@code screenshotRateLimitPerMinute} (Req 17.4, 17.5).</li>
 * </ul>
 *
 * <p><b>Failure mode.</b> Both methods <em>fail open</em> on Valkey errors so
 * that a Cache outage cannot lock every Candidate out of the contest — this
 * matches the repo convention used by {@code AuthRateLimiterService} and the
 * existing event-ingestion path (Req 17 narrative + Req 18.1 keep-state-in-
 * Cache discipline). The notification flag fails <em>closed</em> on Valkey
 * errors: if we cannot prove this is the first rejection in the window we
 * suppress the {@code RATE_LIMIT_EXCEEDED} event rather than risk spamming
 * the candidate WS.
 *
 * <p>Validates: Req 17.1, 17.2, 17.3, 17.4, 17.5.
 */
@Service
public class ProctoringRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(ProctoringRateLimiter.class);

    /** 10-second sliding window matches the {@code 30/sec averaged over 10s} spec (Req 17.2). */
    private static final long EVENT_WINDOW_MS = 10_000L;

    /** Idle-session cleanup so a disconnected sid does not leak its sorted set forever. */
    private static final Duration EVENT_KEY_TTL = Duration.ofSeconds(30);

    /** Notification flag lives exactly one event window so the next window can re-fire. */
    private static final Duration NOTIFY_TTL = Duration.ofSeconds(10);

    /** Screenshot bucket window — Req 17.4 specifies "per minute". */
    private static final Duration SHOT_KEY_TTL = Duration.ofSeconds(60);

    /** Decision shared with both fail-open and fail-closed branches when a frame is admitted. */
    private static final RateLimitDecision ALLOW = new RateLimitDecision(true, false);

    private final StringRedisTemplate redis;
    private final ProctoringConfig config;

    public ProctoringRateLimiter(StringRedisTemplate redis, ProctoringConfig config) {
        this.redis = redis;
        this.config = config;
    }

    /**
     * Decision returned to the WS handler for every inbound event frame.
     *
     * @param allowed       whether the frame should be admitted to the
     *                      ingestion pipeline
     * @param shouldNotify  whether the caller should emit a
     *                      {@code RATE_LIMIT_EXCEEDED} Suspicious_Event for
     *                      this rejection. Always {@code false} when
     *                      {@code allowed} is {@code true}; at most
     *                      {@code true} once per 10 s window when
     *                      {@code allowed} is {@code false} (Req 17.3).
     */
    public record RateLimitDecision(boolean allowed, boolean shouldNotify) {
    }

    /**
     * Admit one inbound Suspicious_Event frame for the given session and, on
     * rejection, decide whether the caller should emit the once-per-window
     * {@code RATE_LIMIT_EXCEEDED} notification.
     *
     * @return {@link RateLimitDecision}; never {@code null}.
     */
    public RateLimitDecision allowEventFrame(Long sessionId) {
        if (sessionId == null) return ALLOW;
        String key = "proctoring:rl:events:" + sessionId;
        long now = System.currentTimeMillis();
        long windowStart = now - EVENT_WINDOW_MS;
        int cap = config.getEventRateLimitPerSecond() * 10;
        try {
            ZSetOperations<String, String> z = redis.opsForZSet();
            // Trim entries that fell out of the trailing 10 s window.
            z.removeRangeByScore(key, 0, windowStart);
            Long card = z.zCard(key);
            if (card != null && card >= cap) {
                return new RateLimitDecision(false, acquireNotifyFlag(sessionId));
            }
            // Use the millisecond timestamp as score and a (ts:nano) tuple as
            // member so multiple frames within the same ms still map to
            // distinct sorted-set entries.
            String member = now + ":" + System.nanoTime();
            z.add(key, member, now);
            // Auto-expire so an idle/closed session does not leak its sorted set.
            redis.expire(key, EVENT_KEY_TTL);
            return ALLOW;
        } catch (Exception e) {
            // Fail open — a Valkey outage must not silently lock candidates out.
            log.warn("Valkey unavailable for event rate limit on session {}, failing open: {}",
                    sessionId, e.getMessage());
            return ALLOW;
        }
    }

    /**
     * Atomically attempt to claim the once-per-window notification slot for
     * this session via {@code SET NX EX 10}. Fails closed on Valkey errors so
     * an outage cannot trigger duplicate notifications.
     */
    private boolean acquireNotifyFlag(Long sessionId) {
        String key = "proctoring:rl:events:" + sessionId + ":notified";
        try {
            Boolean acquired = redis.opsForValue().setIfAbsent(key, "1", NOTIFY_TTL);
            return Boolean.TRUE.equals(acquired);
        } catch (Exception e) {
            log.warn("Valkey unavailable for rate-limit notify flag on session {}, suppressing: {}",
                    sessionId, e.getMessage());
            return false;
        }
    }

    /**
     * Admit one screenshot upload for the given session.
     *
     * <p>{@code INCR} the per-session minute counter. The 60 s expiry is
     * set on first hit (v==1) and re-checked: if a prior EXPIRE call
     * was lost (transient Valkey error), we detect the missing TTL via
     * {@code TTL < 0} and re-set it on any subsequent INCR (fixes Bug 6).
     * Subsequent INCRs where the TTL is already valid do NOT reset the
     * expiry, preserving the original "60 s from first hit" window.
     *
     * @return {@code true} if admitted; {@code false} if the session exceeded
     *         its screenshot upload quota for the current minute.
     */
    public boolean allowScreenshotUpload(Long sessionId) {
        if (sessionId == null) return true;
        String key = "proctoring:rl:shots:" + sessionId;
        int cap = config.getScreenshotRateLimitPerMinute();
        try {
            Long v = redis.opsForValue().increment(key);
            // Set expiry on first hit OR if TTL was lost due to prior error.
            if (v != null && v == 1L) {
                redis.expire(key, SHOT_KEY_TTL);
            } else if (v != null) {
                // Defence against transient EXPIRE failure on the first hit:
                // if the key exists but has no TTL (< 0), re-set it now.
                Long ttl = redis.getExpire(key);
                if (ttl != null && ttl < 0) {
                    redis.expire(key, SHOT_KEY_TTL);
                }
            }
            return v == null || v <= cap;
        } catch (Exception e) {
            log.warn("Valkey unavailable for screenshot rate limit on session {}, failing open: {}",
                    sessionId, e.getMessage());
            return true;
        }
    }
}
