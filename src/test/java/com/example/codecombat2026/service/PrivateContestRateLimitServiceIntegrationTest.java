package com.example.codecombat2026.service;

import com.example.codecombat2026.exception.TooManyRequestsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for PrivateContestRateLimitService with real Valkey.
 * Tests the complete rate limiting flow end-to-end.
 */
@SpringBootTest
@ActiveProfiles("test")
class PrivateContestRateLimitServiceIntegrationTest {

    @Autowired
    private PrivateContestRateLimitService rateLimitService;

    @Autowired
    private StringRedisTemplate redis;

    @BeforeEach
    void setUp() {
        // Clean up any existing rate limit keys
        redis.delete("ratelimit:contest:create:user:1000");
        redis.delete("ratelimit:ai:problem:gen:user:1000");
        redis.delete("ratelimit:invite:regen:contest:1000");
        redis.delete("ratelimit:invite:accept:contest:1000");
    }

    @Test
    void contestCreationRateLimit_IntegrationTest() {
        Long userId = 1000L;

        // First 5 requests should pass
        for (int i = 1; i <= 5; i++) {
            assertDoesNotThrow(() -> rateLimitService.checkContestCreationLimit(userId),
                "Request " + i + " should pass");
        }

        // 6th request should fail
        TooManyRequestsException exception = assertThrows(
            TooManyRequestsException.class,
            () -> rateLimitService.checkContestCreationLimit(userId)
        );

        assertTrue(exception.getMessage().contains("hourly limit of 5 contest creation requests"));
        assertNotNull(exception.getRetryAfterSeconds());
        assertTrue(exception.getRetryAfterSeconds() > 0);
    }

    @Test
    void aiProblemGenRateLimit_IntegrationTest() {
        Long userId = 1000L;

        // First 5 requests should pass
        for (int i = 1; i <= 5; i++) {
            assertDoesNotThrow(() -> rateLimitService.checkAIProblemGenLimit(userId),
                "Request " + i + " should pass");
        }

        // 6th request should fail
        TooManyRequestsException exception = assertThrows(
            TooManyRequestsException.class,
            () -> rateLimitService.checkAIProblemGenLimit(userId)
        );

        assertTrue(exception.getMessage().contains("daily limit of 5 AI-generated problems"));
        assertNotNull(exception.getRetryAfterSeconds());
    }

    @Test
    void inviteRegenRateLimit_IntegrationTest() {
        Long contestId = 1000L;

        // First 10 requests should pass
        for (int i = 1; i <= 10; i++) {
            assertDoesNotThrow(() -> rateLimitService.checkInviteRegenLimit(contestId),
                "Request " + i + " should pass");
        }

        // 11th request should fail
        TooManyRequestsException exception = assertThrows(
            TooManyRequestsException.class,
            () -> rateLimitService.checkInviteRegenLimit(contestId)
        );

        assertTrue(exception.getMessage().contains("hourly limit of 10 invite link regenerations"));
        assertNotNull(exception.getRetryAfterSeconds());
    }

    @Test
    void inviteAcceptRateLimit_IntegrationTest() {
        Long contestId = 1000L;

        // First 100 requests should pass
        for (int i = 1; i <= 100; i++) {
            assertDoesNotThrow(() -> rateLimitService.checkInviteAcceptLimit(contestId),
                "Request " + i + " should pass");
        }

        // 101st request should fail
        TooManyRequestsException exception = assertThrows(
            TooManyRequestsException.class,
            () -> rateLimitService.checkInviteAcceptLimit(contestId)
        );

        assertTrue(exception.getMessage().contains("too many join requests"));
        assertNotNull(exception.getRetryAfterSeconds());
    }
}
