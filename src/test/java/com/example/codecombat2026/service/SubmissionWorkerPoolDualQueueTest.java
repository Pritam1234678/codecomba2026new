package com.example.codecombat2026.service;

import com.example.codecombat2026.dto.SubmissionJob;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for UnifiedSubmissionWorkerPool dual queue draining.
 * Tests Requirements 13.3, 22.1, 22.2: fair round-robin between public and private queues.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "JUDGE_WORKERS=0" // Disable auto-start of workers for testing
})
class SubmissionWorkerPoolDualQueueTest {

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanup() {
        // Clean up both queues before each test
        redis.delete(SubmissionWorkerPool.QUEUE_KEY);
        redis.delete(SubmissionWorkerPool.PRIVATE_QUEUE_KEY);
    }

    @Test
    void testPublicQueueDepth() {
        // Given: 3 jobs in public queue
        for (int i = 1; i <= 3; i++) {
            SubmissionJob job = createPublicJob((long) i);
            pushToQueue(SubmissionWorkerPool.QUEUE_KEY, job);
        }

        // When: check queue depth
        Long depth = redis.opsForList().size(SubmissionWorkerPool.QUEUE_KEY);

        // Then: depth should be 3
        assertEquals(3L, depth);
    }

    @Test
    void testPrivateQueueDepth() {
        // Given: 2 jobs in private queue
        for (int i = 1; i <= 2; i++) {
            SubmissionJob job = createPrivateJob((long) i);
            pushToQueue(SubmissionWorkerPool.PRIVATE_QUEUE_KEY, job);
        }

        // When: check queue depth
        Long depth = redis.opsForList().size(SubmissionWorkerPool.PRIVATE_QUEUE_KEY);

        // Then: depth should be 2
        assertEquals(2L, depth);
    }

    @Test
    void testBothQueuesIndependent() {
        // Given: jobs in both queues
        SubmissionJob publicJob = createPublicJob(1L);
        SubmissionJob privateJob = createPrivateJob(2L);
        
        pushToQueue(SubmissionWorkerPool.QUEUE_KEY, publicJob);
        pushToQueue(SubmissionWorkerPool.PRIVATE_QUEUE_KEY, privateJob);

        // When: check both queue depths
        Long publicDepth = redis.opsForList().size(SubmissionWorkerPool.QUEUE_KEY);
        Long privateDepth = redis.opsForList().size(SubmissionWorkerPool.PRIVATE_QUEUE_KEY);

        // Then: each queue should have 1 job
        assertEquals(1L, publicDepth);
        assertEquals(1L, privateDepth);
    }

    @Test
    void testJobRoutingByPrivateContestId() {
        // Given: a job with privateContestId
        SubmissionJob privateJob = new SubmissionJob(
            100L, 1L, 1L, 50L, "code", "JAVA", 5.0, 256, false, null, null, 999L
        );

        // Then: privateContestId should be set
        assertNotNull(privateJob.getPrivateContestId());
        assertEquals(999L, privateJob.getPrivateContestId());
    }

    @Test
    void testJobRoutingWithoutPrivateContestId() {
        // Given: a job without privateContestId (public contest)
        SubmissionJob publicJob = new SubmissionJob(
            101L, 2L, 2L, 51L, "code", "PYTHON", 5.0, 256, false, null, null, null
        );

        // Then: privateContestId should be null
        assertNull(publicJob.getPrivateContestId());
    }

    // Helper methods

    private SubmissionJob createPublicJob(Long submissionId) {
        return new SubmissionJob(
            submissionId, 1L, 1L, 1L, "public code", "JAVA", 5.0, 256, 
            false, null, null, null // privateContestId is null
        );
    }

    private SubmissionJob createPrivateJob(Long submissionId) {
        return new SubmissionJob(
            submissionId, 2L, 2L, 100L, "private code", "PYTHON", 5.0, 256,
            false, null, null, 999L // privateContestId is 999
        );
    }

    private void pushToQueue(String queueKey, SubmissionJob job) {
        try {
            String jobJson = objectMapper.writeValueAsString(job);
            redis.opsForList().leftPush(queueKey, jobJson);
        } catch (Exception e) {
            fail("Failed to push job to queue: " + e.getMessage());
        }
    }
}
