package com.example.codecombat2026.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Prometheus/Micrometer metrics configuration for Private Contest Hosting.
 * 
 * Tracks key metrics:
 * - Contest creation count
 * - Invitation acceptance count
 * - Submission queue depth (private contests)
 * - Cache hit/miss ratios
 * - Active contest and participant counts
 * 
 * All metrics are prefixed with "private_contest_" for easy filtering in Prometheus.
 * 
 * Exposed at /actuator/prometheus endpoint.
 * 
 * @see <a href="https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html">Spring Boot Actuator</a>
 * @see <a href="https://micrometer.io/docs">Micrometer Documentation</a>
 */
@Configuration
@Getter
public class PrivateContestMetricsConfig {

    // ═══════════════════════════════════════════════════════════════════════════
    // COUNTERS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Total number of private contests created.
     * Incremented in PrivateContestService.createPrivateContest().
     */
    private final Counter contestsCreatedCounter;

    /**
     * Total number of contest invitations accepted (participants joined).
     * Incremented in PrivateInviteService.acceptInvite().
     */
    private final Counter invitationsAcceptedCounter;

    /**
     * Total number of submissions to private contests.
     * Incremented in PrivateContestSubmissionService.submitCode().
     */
    private final Counter submissionsCounter;

    /**
     * Private contest cache hits (successful cache reads).
     * Incremented in PrivateContestCacheService when cache hit occurs.
     */
    private final Counter cacheHitsCounter;

    /**
     * Private contest cache misses (cache read failures, fallback to DB).
     * Incremented in PrivateContestCacheService when cache miss occurs.
     */
    private final Counter cacheMissesCounter;

    // ═══════════════════════════════════════════════════════════════════════════
    // GAUGES
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Current length of private submission queue (private:submission:queue).
     * Gauge reads from Redis LLEN on each scrape.
     */
    private final AtomicLong submissionQueueLength = new AtomicLong(0);

    /**
     * Current count of active private contests (status = LIVE).
     * Updated by scheduler on status transitions.
     */
    private final AtomicLong activeContestsCount = new AtomicLong(0);

    /**
     * Current total count of participants across all private contests.
     * Updated periodically or on participant join/remove.
     */
    private final AtomicLong totalParticipantsCount = new AtomicLong(0);

    // ═══════════════════════════════════════════════════════════════════════════
    // TIMERS / HISTOGRAMS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Submission processing time (from queue push to verdict write).
     * Timer records duration in SubmissionWorkerPool for private contest jobs.
     */
    private final Timer submissionProcessingTimer;

    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Initialize all metrics and register with MeterRegistry.
     * 
     * @param meterRegistry Spring Boot auto-configured registry (Prometheus)
     * @param redisTemplate Redis client for queue length gauge
     */
    public PrivateContestMetricsConfig(MeterRegistry meterRegistry, RedisTemplate<String, Object> redisTemplate) {
        
        // ─── Counters ──────────────────────────────────────────────────────
        
        this.contestsCreatedCounter = Counter.builder("private_contest_created_total")
                .description("Total number of private contests created")
                .register(meterRegistry);

        this.invitationsAcceptedCounter = Counter.builder("private_contest_invitation_accepted_total")
                .description("Total number of invitation acceptances (participant joins)")
                .register(meterRegistry);

        this.submissionsCounter = Counter.builder("private_contest_submission_total")
                .description("Total number of submissions to private contests")
                .register(meterRegistry);

        this.cacheHitsCounter = Counter.builder("private_contest_cache_hits_total")
                .description("Total cache hits for private contest data")
                .register(meterRegistry);

        this.cacheMissesCounter = Counter.builder("private_contest_cache_misses_total")
                .description("Total cache misses for private contest data")
                .register(meterRegistry);

        // ─── Gauges ────────────────────────────────────────────────────────
        
        // Submission queue length (read from Redis on every scrape)
        Gauge.builder("private_contest_submission_queue_length", () -> {
                    try {
                        Long length = redisTemplate.opsForList().size("private:submission:queue");
                        submissionQueueLength.set(length != null ? length : 0);
                        return submissionQueueLength.get();
                    } catch (Exception e) {
                        return 0.0; // Return 0 on Redis connection error
                    }
                })
                .description("Current length of private contest submission queue")
                .register(meterRegistry);

        // Active contests gauge
        Gauge.builder("private_contest_active_count", activeContestsCount, AtomicLong::get)
                .description("Number of currently active (LIVE) private contests")
                .register(meterRegistry);

        // Total participants gauge
        Gauge.builder("private_contest_participants_total", totalParticipantsCount, AtomicLong::get)
                .description("Total number of participants across all private contests")
                .register(meterRegistry);

        // ─── Timers ────────────────────────────────────────────────────────
        
        this.submissionProcessingTimer = Timer.builder("private_contest_submission_processing_seconds")
                .description("Time taken to process a private contest submission (queue to verdict)")
                .publishPercentileHistogram()
                .register(meterRegistry);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Update active contests gauge.
     * Called by PrivateContestScheduler on status transitions.
     * 
     * @param count Current count of LIVE contests
     */
    public void setActiveContestsCount(long count) {
        this.activeContestsCount.set(count);
    }

    /**
     * Update total participants gauge.
     * Called periodically or on participant join/remove events.
     * 
     * @param count Current total participant count
     */
    public void setTotalParticipantsCount(long count) {
        this.totalParticipantsCount.set(count);
    }

    /**
     * Increment contest creation counter.
     * Call this in PrivateContestService.createPrivateContest() after successful creation.
     */
    public void incrementContestsCreated() {
        this.contestsCreatedCounter.increment();
    }

    /**
     * Increment invitation acceptance counter.
     * Call this in PrivateInviteService.acceptInvite() after successful join.
     */
    public void incrementInvitationsAccepted() {
        this.invitationsAcceptedCounter.increment();
    }

    /**
     * Increment submissions counter.
     * Call this in PrivateContestSubmissionService.submitCode() after queue push.
     */
    public void incrementSubmissions() {
        this.submissionsCounter.increment();
    }

    /**
     * Increment cache hits counter.
     * Call this in PrivateContestCacheService on cache hit.
     */
    public void incrementCacheHits() {
        this.cacheHitsCounter.increment();
    }

    /**
     * Increment cache misses counter.
     * Call this in PrivateContestCacheService on cache miss.
     */
    public void incrementCacheMisses() {
        this.cacheMissesCounter.increment();
    }

    /**
     * Record submission processing time.
     * Call this in SubmissionWorkerPool after completing a private contest submission.
     * 
     * @param durationMillis Processing duration in milliseconds
     */
    public void recordSubmissionProcessingTime(long durationMillis) {
        this.submissionProcessingTimer.record(durationMillis, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /**
     * Get a Timer.Sample for recording submission processing time.
     * Start timing before job processing, stop after verdict write.
     * 
     * Usage:
     * <pre>
     * Timer.Sample sample = config.startSubmissionTimer();
     * // ... process submission ...
     * sample.stop(config.getSubmissionProcessingTimer());
     * </pre>
     * 
     * @return Timer.Sample for manual timing
     */
    public Timer.Sample startSubmissionTimer() {
        return Timer.start();
    }
}
