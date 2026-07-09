package com.example.codecombat2026.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PrivateContestMetricsConfig.
 * 
 * Tests:
 * - Counter metrics increment correctly
 * - Gauge metrics update correctly
 * - Timer metrics record correctly
 * - Redis-backed gauges handle failures gracefully
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PrivateContestMetricsConfig Tests")
class PrivateContestMetricsConfigTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ListOperations<String, Object> listOperations;

    private MeterRegistry meterRegistry;
    private PrivateContestMetricsConfig metricsConfig;

    @BeforeEach
    void setUp() {
        // Use SimpleMeterRegistry for testing
        meterRegistry = new SimpleMeterRegistry();
        
        // Mock Redis operations
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        
        // Create config with mocked dependencies
        metricsConfig = new PrivateContestMetricsConfig(meterRegistry, redisTemplate);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // COUNTER TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should increment contests created counter")
    void testIncrementContestsCreated() {
        // Act
        metricsConfig.incrementContestsCreated();
        metricsConfig.incrementContestsCreated();
        metricsConfig.incrementContestsCreated();

        // Assert
        Counter counter = meterRegistry.find("private_contest_created_total").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(3.0);
    }

    @Test
    @DisplayName("Should increment invitations accepted counter")
    void testIncrementInvitationsAccepted() {
        // Act
        metricsConfig.incrementInvitationsAccepted();
        metricsConfig.incrementInvitationsAccepted();

        // Assert
        Counter counter = meterRegistry.find("private_contest_invitation_accepted_total").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("Should increment submissions counter")
    void testIncrementSubmissions() {
        // Act
        metricsConfig.incrementSubmissions();

        // Assert
        Counter counter = meterRegistry.find("private_contest_submission_total").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should increment cache hits counter")
    void testIncrementCacheHits() {
        // Act
        metricsConfig.incrementCacheHits();
        metricsConfig.incrementCacheHits();
        metricsConfig.incrementCacheHits();
        metricsConfig.incrementCacheHits();

        // Assert
        Counter counter = meterRegistry.find("private_contest_cache_hits_total").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(4.0);
    }

    @Test
    @DisplayName("Should increment cache misses counter")
    void testIncrementCacheMisses() {
        // Act
        metricsConfig.incrementCacheMisses();
        metricsConfig.incrementCacheMisses();

        // Assert
        Counter counter = meterRegistry.find("private_contest_cache_misses_total").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(2.0);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GAUGE TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should update active contests gauge")
    void testSetActiveContestsCount() {
        // Act
        metricsConfig.setActiveContestsCount(5);

        // Assert
        Gauge gauge = meterRegistry.find("private_contest_active_count").gauge();
        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isEqualTo(5.0);

        // Update again
        metricsConfig.setActiveContestsCount(10);
        assertThat(gauge.value()).isEqualTo(10.0);
    }

    @Test
    @DisplayName("Should update total participants gauge")
    void testSetTotalParticipantsCount() {
        // Act
        metricsConfig.setTotalParticipantsCount(250);

        // Assert
        Gauge gauge = meterRegistry.find("private_contest_participants_total").gauge();
        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isEqualTo(250.0);

        // Update again
        metricsConfig.setTotalParticipantsCount(300);
        assertThat(gauge.value()).isEqualTo(300.0);
    }

    @Test
    @DisplayName("Should read submission queue length from Redis")
    void testSubmissionQueueLengthGauge() {
        // Arrange
        when(listOperations.size("private:submission:queue")).thenReturn(15L);

        // Act & Assert
        Gauge gauge = meterRegistry.find("private_contest_submission_queue_length").gauge();
        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isEqualTo(15.0);
    }

    @Test
    @DisplayName("Should return 0 when Redis is unavailable for queue length")
    void testSubmissionQueueLengthGauge_RedisFailure() {
        // Arrange
        when(listOperations.size("private:submission:queue"))
            .thenThrow(new RuntimeException("Redis connection failed"));

        // Act & Assert
        Gauge gauge = meterRegistry.find("private_contest_submission_queue_length").gauge();
        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should return 0 when Redis returns null for queue length")
    void testSubmissionQueueLengthGauge_NullResult() {
        // Arrange
        when(listOperations.size("private:submission:queue")).thenReturn(null);

        // Act & Assert
        Gauge gauge = meterRegistry.find("private_contest_submission_queue_length").gauge();
        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isEqualTo(0.0);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TIMER TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should record submission processing time")
    void testRecordSubmissionProcessingTime() {
        // Act
        metricsConfig.recordSubmissionProcessingTime(1500); // 1.5 seconds
        metricsConfig.recordSubmissionProcessingTime(2000); // 2 seconds

        // Assert
        Timer timer = meterRegistry.find("private_contest_submission_processing_seconds").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(2);
        assertThat(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS))
            .isGreaterThanOrEqualTo(3500); // 1500 + 2000
    }

    @Test
    @DisplayName("Should start and stop submission timer correctly")
    void testSubmissionTimerSample() {
        // Act
        Timer.Sample sample = metricsConfig.startSubmissionTimer();
        
        // Simulate processing time
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        sample.stop(metricsConfig.getSubmissionProcessingTimer());

        // Assert
        Timer timer = meterRegistry.find("private_contest_submission_processing_seconds").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
        assertThat(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS))
            .isGreaterThanOrEqualTo(100);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // INTEGRATION TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should register all metrics correctly")
    void testAllMetricsRegistered() {
        // Assert counters
        assertThat(meterRegistry.find("private_contest_created_total").counter()).isNotNull();
        assertThat(meterRegistry.find("private_contest_invitation_accepted_total").counter()).isNotNull();
        assertThat(meterRegistry.find("private_contest_submission_total").counter()).isNotNull();
        assertThat(meterRegistry.find("private_contest_cache_hits_total").counter()).isNotNull();
        assertThat(meterRegistry.find("private_contest_cache_misses_total").counter()).isNotNull();

        // Assert gauges
        assertThat(meterRegistry.find("private_contest_submission_queue_length").gauge()).isNotNull();
        assertThat(meterRegistry.find("private_contest_active_count").gauge()).isNotNull();
        assertThat(meterRegistry.find("private_contest_participants_total").gauge()).isNotNull();

        // Assert timer
        assertThat(meterRegistry.find("private_contest_submission_processing_seconds").timer()).isNotNull();
    }

    @Test
    @DisplayName("Should handle concurrent counter increments")
    void testConcurrentCounterIncrements() throws InterruptedException {
        // Arrange
        int threadCount = 10;
        int incrementsPerThread = 100;
        Thread[] threads = new Thread[threadCount];

        // Act
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < incrementsPerThread; j++) {
                    metricsConfig.incrementContestsCreated();
                }
            });
            threads[i].start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Assert
        Counter counter = meterRegistry.find("private_contest_created_total").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(threadCount * incrementsPerThread);
    }

    @Test
    @DisplayName("Should expose counter descriptions for Prometheus scraping")
    void testCounterDescriptions() {
        Counter contestsCreated = meterRegistry.find("private_contest_created_total").counter();
        assertThat(contestsCreated).isNotNull();
        assertThat(contestsCreated.getId().getDescription())
            .isEqualTo("Total number of private contests created");

        Counter invitationsAccepted = meterRegistry.find("private_contest_invitation_accepted_total").counter();
        assertThat(invitationsAccepted).isNotNull();
        assertThat(invitationsAccepted.getId().getDescription())
            .isEqualTo("Total number of invitation acceptances (participant joins)");
    }
}
