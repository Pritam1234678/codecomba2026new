package com.example.codecombat2026.service;

import com.example.codecombat2026.exception.TooManyRequestsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PrivateContestRateLimitService.
 * Tests rate limiting for contest creation, AI problem generation,
 * invite regeneration, and invite acceptance.
 */
@ExtendWith(MockitoExtension.class)
class PrivateContestRateLimitServiceTest {

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private PrivateContestRateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(valueOperations);
    }

    // ========== Contest Creation Rate Limit Tests ==========

    @Test
    void checkContestCreationLimit_FirstRequest_ShouldPass() {
        // Given
        Long userId = 1L;
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(redis.expire(anyString(), any(Duration.class))).thenReturn(true);

        // When & Then - Should not throw exception
        assertDoesNotThrow(() -> rateLimitService.checkContestCreationLimit(userId));

        verify(valueOperations).increment("ratelimit:contest:create:user:1");
        verify(redis).expire(eq("ratelimit:contest:create:user:1"), any(Duration.class));
    }

    @Test
    void checkContestCreationLimit_FifthRequest_ShouldPass() {
        // Given
        Long userId = 1L;
        when(valueOperations.increment(anyString())).thenReturn(5L);

        // When & Then - Should not throw exception (at limit)
        assertDoesNotThrow(() -> rateLimitService.checkContestCreationLimit(userId));
    }

    @Test
    void checkContestCreationLimit_SixthRequest_ShouldThrowException() {
        // Given
        Long userId = 1L;
        when(valueOperations.increment(anyString())).thenReturn(6L);
        when(redis.getExpire(anyString(), any(TimeUnit.class))).thenReturn(1800L); // 30 minutes remaining

        // When & Then
        TooManyRequestsException exception = assertThrows(
            TooManyRequestsException.class,
            () -> rateLimitService.checkContestCreationLimit(userId)
        );

        assertTrue(exception.getMessage().contains("hourly limit of 5 contest creation requests"));
        assertEquals(1800L, exception.getRetryAfterSeconds());
    }

    // ========== AI Problem Generation Rate Limit Tests ==========

    @Test
    void checkAIProblemGenLimit_FirstRequest_ShouldPass() {
        // Given
        Long userId = 2L;
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(redis.expire(anyString(), any(Duration.class))).thenReturn(true);

        // When & Then
        assertDoesNotThrow(() -> rateLimitService.checkAIProblemGenLimit(userId));

        verify(valueOperations).increment("ratelimit:ai:problem:gen:user:2");
        verify(redis).expire(eq("ratelimit:ai:problem:gen:user:2"), any(Duration.class));
    }

    @Test
    void checkAIProblemGenLimit_FifthRequest_ShouldPass() {
        // Given
        Long userId = 2L;
        when(valueOperations.increment(anyString())).thenReturn(5L);

        // When & Then
        assertDoesNotThrow(() -> rateLimitService.checkAIProblemGenLimit(userId));
    }

    @Test
    void checkAIProblemGenLimit_SixthRequest_ShouldThrowException() {
        // Given
        Long userId = 2L;
        when(valueOperations.increment(anyString())).thenReturn(6L);
        when(redis.getExpire(anyString(), any(TimeUnit.class))).thenReturn(43200L); // 12 hours remaining

        // When & Then
        TooManyRequestsException exception = assertThrows(
            TooManyRequestsException.class,
            () -> rateLimitService.checkAIProblemGenLimit(userId)
        );

        assertTrue(exception.getMessage().contains("daily limit of 5 AI-generated problems"));
        assertTrue(exception.getMessage().contains("12 hours"));
        assertEquals(43200L, exception.getRetryAfterSeconds());
    }

    // ========== Invite Regeneration Rate Limit Tests ==========

    @Test
    void checkInviteRegenLimit_FirstRequest_ShouldPass() {
        // Given
        Long contestId = 100L;
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(redis.expire(anyString(), any(Duration.class))).thenReturn(true);

        // When & Then
        assertDoesNotThrow(() -> rateLimitService.checkInviteRegenLimit(contestId));

        verify(valueOperations).increment("ratelimit:invite:regen:contest:100");
        verify(redis).expire(eq("ratelimit:invite:regen:contest:100"), any(Duration.class));
    }

    @Test
    void checkInviteRegenLimit_TenthRequest_ShouldPass() {
        // Given
        Long contestId = 100L;
        when(valueOperations.increment(anyString())).thenReturn(10L);

        // When & Then
        assertDoesNotThrow(() -> rateLimitService.checkInviteRegenLimit(contestId));
    }

    @Test
    void checkInviteRegenLimit_EleventhRequest_ShouldThrowException() {
        // Given
        Long contestId = 100L;
        when(valueOperations.increment(anyString())).thenReturn(11L);
        when(redis.getExpire(anyString(), any(TimeUnit.class))).thenReturn(2400L); // 40 minutes remaining

        // When & Then
        TooManyRequestsException exception = assertThrows(
            TooManyRequestsException.class,
            () -> rateLimitService.checkInviteRegenLimit(contestId)
        );

        assertTrue(exception.getMessage().contains("hourly limit of 10 invite link regenerations"));
        assertEquals(2400L, exception.getRetryAfterSeconds());
    }

    // ========== Invite Acceptance Rate Limit Tests ==========

    @Test
    void checkInviteAcceptLimit_FirstRequest_ShouldPass() {
        // Given
        Long contestId = 200L;
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(redis.expire(anyString(), any(Duration.class))).thenReturn(true);

        // When & Then
        assertDoesNotThrow(() -> rateLimitService.checkInviteAcceptLimit(contestId));

        verify(valueOperations).increment("ratelimit:invite:accept:contest:200");
        verify(redis).expire(eq("ratelimit:invite:accept:contest:200"), any(Duration.class));
    }

    @Test
    void checkInviteAcceptLimit_HundredthRequest_ShouldPass() {
        // Given
        Long contestId = 200L;
        when(valueOperations.increment(anyString())).thenReturn(100L);

        // When & Then
        assertDoesNotThrow(() -> rateLimitService.checkInviteAcceptLimit(contestId));
    }

    @Test
    void checkInviteAcceptLimit_HundredFirstRequest_ShouldThrowException() {
        // Given
        Long contestId = 200L;
        when(valueOperations.increment(anyString())).thenReturn(101L);
        when(redis.getExpire(anyString(), any(TimeUnit.class))).thenReturn(3000L); // 50 minutes remaining

        // When & Then
        TooManyRequestsException exception = assertThrows(
            TooManyRequestsException.class,
            () -> rateLimitService.checkInviteAcceptLimit(contestId)
        );

        assertTrue(exception.getMessage().contains("too many join requests"));
        assertEquals(3000L, exception.getRetryAfterSeconds());
    }

    // ========== Retry-After Helper Tests ==========

    @Test
    void getContestCreationRetryAfter_ShouldReturnTTL() {
        // Given
        Long userId = 1L;
        when(redis.getExpire(anyString(), any(TimeUnit.class))).thenReturn(1800L);

        // When
        long retryAfter = rateLimitService.getContestCreationRetryAfter(userId);

        // Then
        assertEquals(1800L, retryAfter);
        verify(redis).getExpire("ratelimit:contest:create:user:1", TimeUnit.SECONDS);
    }

    @Test
    void getAIProblemGenRetryAfter_ShouldReturnTTL() {
        // Given
        Long userId = 2L;
        when(redis.getExpire(anyString(), any(TimeUnit.class))).thenReturn(43200L);

        // When
        long retryAfter = rateLimitService.getAIProblemGenRetryAfter(userId);

        // Then
        assertEquals(43200L, retryAfter);
        verify(redis).getExpire("ratelimit:ai:problem:gen:user:2", TimeUnit.SECONDS);
    }

    @Test
    void getInviteRegenRetryAfter_ShouldReturnTTL() {
        // Given
        Long contestId = 100L;
        when(redis.getExpire(anyString(), any(TimeUnit.class))).thenReturn(2400L);

        // When
        long retryAfter = rateLimitService.getInviteRegenRetryAfter(contestId);

        // Then
        assertEquals(2400L, retryAfter);
        verify(redis).getExpire("ratelimit:invite:regen:contest:100", TimeUnit.SECONDS);
    }

    @Test
    void getInviteAcceptRetryAfter_ShouldReturnTTL() {
        // Given
        Long contestId = 200L;
        when(redis.getExpire(anyString(), any(TimeUnit.class))).thenReturn(3000L);

        // When
        long retryAfter = rateLimitService.getInviteAcceptRetryAfter(contestId);

        // Then
        assertEquals(3000L, retryAfter);
        verify(redis).getExpire("ratelimit:invite:accept:contest:200", TimeUnit.SECONDS);
    }

    // ========== Error Handling Tests ==========

    @Test
    void checkContestCreationLimit_ValkeyError_ShouldFailOpen() {
        // Given
        Long userId = 1L;
        when(valueOperations.increment(anyString())).thenThrow(new RuntimeException("Valkey connection error"));

        // When & Then - Should not throw exception (fail open)
        assertDoesNotThrow(() -> rateLimitService.checkContestCreationLimit(userId));
    }

    @Test
    void checkAIProblemGenLimit_ValkeyError_ShouldFailOpen() {
        // Given
        Long userId = 2L;
        when(valueOperations.increment(anyString())).thenThrow(new RuntimeException("Valkey connection error"));

        // When & Then - Should not throw exception (fail open)
        assertDoesNotThrow(() -> rateLimitService.checkAIProblemGenLimit(userId));
    }

    @Test
    void getRetryAfter_ValkeyError_ShouldReturnDefaultWindow() {
        // Given
        Long userId = 1L;
        when(redis.getExpire(anyString(), any(TimeUnit.class))).thenThrow(new RuntimeException("Valkey error"));

        // When
        long retryAfter = rateLimitService.getContestCreationRetryAfter(userId);

        // Then - Should return default window (3600 seconds for contest creation)
        assertEquals(3600L, retryAfter);
    }

    // ========== Null Count Handling Tests ==========

    @Test
    void checkContestCreationLimit_NullCount_ShouldTreatAsOne() {
        // Given
        Long userId = 1L;
        when(valueOperations.increment(anyString())).thenReturn(null);

        // When & Then - Should not throw exception (treats null as 1)
        assertDoesNotThrow(() -> rateLimitService.checkContestCreationLimit(userId));
    }

    @Test
    void getRetryAfter_NullTTL_ShouldReturnDefaultWindow() {
        // Given
        Long userId = 1L;
        when(redis.getExpire(anyString(), any(TimeUnit.class))).thenReturn(null);

        // When
        long retryAfter = rateLimitService.getContestCreationRetryAfter(userId);

        // Then - Should return default window (3600 seconds)
        assertEquals(3600L, retryAfter);
    }

    @Test
    void getRetryAfter_NegativeTTL_ShouldReturnDefaultWindow() {
        // Given
        Long userId = 1L;
        when(redis.getExpire(anyString(), any(TimeUnit.class))).thenReturn(-1L);

        // When
        long retryAfter = rateLimitService.getContestCreationRetryAfter(userId);

        // Then - Should return default window (3600 seconds)
        assertEquals(3600L, retryAfter);
    }
}
