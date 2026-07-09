package com.example.codecombat2026.service;

import com.example.codecombat2026.dto.PrivateContestDTO;
import com.example.codecombat2026.entity.Contest.ContestStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PrivateContestCacheService.
 * 
 * Tests all caching operations including:
 * - Contest metadata caching and retrieval (6-hour TTL)
 * - Participant set caching and membership checks (6-hour TTL)
 * - Cache invalidation
 * - TTL expiration behavior
 * - Cache hit/miss scenarios
 * - Error handling when Valkey is unavailable
 * - Edge cases (null inputs, empty sets)
 */
@ExtendWith(MockitoExtension.class)
class PrivateContestCacheServiceTest {

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private PrivateContestCacheService cacheService;

    private PrivateContestDTO contestDTO;
    private Long contestId;
    private Long userId;
    private Set<Long> participantIds;
    private String contestJson;
    private LocalDateTime now;

    @BeforeEach
    void setUp() throws Exception {
        // Set up mock Redis operations
        lenient().when(redis.opsForValue()).thenReturn(valueOperations);
        lenient().when(redis.opsForSet()).thenReturn(setOperations);

        // Set up test data
        contestId = 501L;
        userId = 42L;
        now = LocalDateTime.now();

        contestDTO = new PrivateContestDTO();
        contestDTO.setId(101L);
        contestDTO.setContestId(contestId);
        contestDTO.setName("CS101 Midterm");
        contestDTO.setDescription("Data structures test");
        contestDTO.setStartTime(now.plusHours(2));
        contestDTO.setEndTime(now.plusHours(5));
        contestDTO.setStatus(ContestStatus.UPCOMING);
        contestDTO.setHostUserId(10L);
        contestDTO.setHostUsername("prof_smith");
        contestDTO.setEnableProctoring(true);
        contestDTO.setCancelled(false);
        contestDTO.setParticipantCount(25L);

        participantIds = new HashSet<>();
        participantIds.add(42L);
        participantIds.add(55L);
        participantIds.add(88L);

        contestJson = "{\"id\":101,\"contestId\":501,\"name\":\"CS101 Midterm\"}";

        // Set up ObjectMapper mocks
        lenient().when(objectMapper.writeValueAsString(any(PrivateContestDTO.class)))
            .thenReturn(contestJson);
        lenient().when(objectMapper.readValue(anyString(), eq(PrivateContestDTO.class)))
            .thenReturn(contestDTO);
    }

    // ─── Cache Contest Metadata Tests ─────────────────────────────────────────

    @Test
    void cacheContestMetadata_WithValidData_CachesSuccessfully() throws Exception {
        // When
        cacheService.cacheContestMetadata(contestId, contestDTO);

        // Then
        verify(objectMapper).writeValueAsString(contestDTO);
        verify(valueOperations).set(
            eq("private:contest:" + contestId),
            eq(contestJson),
            eq(Duration.ofHours(6))
        );
    }

    @Test
    void cacheContestMetadata_UsesSixHourTTL() throws Exception {
        // When
        cacheService.cacheContestMetadata(contestId, contestDTO);

        // Then
        ArgumentCaptor<Duration> durationCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(valueOperations).set(anyString(), anyString(), durationCaptor.capture());
        
        Duration ttl = durationCaptor.getValue();
        assertEquals(Duration.ofHours(6), ttl);
    }

    @Test
    void cacheContestMetadata_WithNullContestId_DoesNotCache() {
        // When
        cacheService.cacheContestMetadata(null, contestDTO);

        // Then
        verifyNoInteractions(valueOperations);
    }

    @Test
    void cacheContestMetadata_WithNullDTO_DoesNotCache() {
        // When
        cacheService.cacheContestMetadata(contestId, null);

        // Then
        verifyNoInteractions(valueOperations);
    }

    @Test
    void cacheContestMetadata_WithSerializationFailure_DoesNotThrow() throws Exception {
        // Given
        when(objectMapper.writeValueAsString(any(PrivateContestDTO.class)))
            .thenThrow(new RuntimeException("JSON serialization error"));

        // When / Then
        assertDoesNotThrow(() -> cacheService.cacheContestMetadata(contestId, contestDTO));
        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void cacheContestMetadata_WithRedisFailure_DoesNotThrow() throws Exception {
        // Given
        doThrow(new RuntimeException("Redis connection failed"))
            .when(valueOperations).set(anyString(), anyString(), any(Duration.class));

        // When / Then
        assertDoesNotThrow(() -> cacheService.cacheContestMetadata(contestId, contestDTO));
    }

    // ─── Get Cached Contest Tests ─────────────────────────────────────────

    @Test
    void getCachedContest_WithCacheHit_ReturnsDTO() throws Exception {
        // Given
        when(valueOperations.get("private:contest:" + contestId))
            .thenReturn(contestJson);

        // When
        Optional<PrivateContestDTO> result = cacheService.getCachedContest(contestId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(contestDTO, result.get());
        verify(objectMapper).readValue(contestJson, PrivateContestDTO.class);
    }

    @Test
    void getCachedContest_WithCacheMiss_ReturnsEmpty() {
        // Given
        when(valueOperations.get("private:contest:" + contestId))
            .thenReturn(null);

        // When
        Optional<PrivateContestDTO> result = cacheService.getCachedContest(contestId);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void getCachedContest_WithNullContestId_ReturnsEmpty() {
        // When
        Optional<PrivateContestDTO> result = cacheService.getCachedContest(null);

        // Then
        assertTrue(result.isEmpty());
        verifyNoInteractions(valueOperations);
    }

    @Test
    void getCachedContest_WithDeserializationFailure_ReturnsEmpty() throws Exception {
        // Given
        when(valueOperations.get("private:contest:" + contestId))
            .thenReturn(contestJson);
        when(objectMapper.readValue(anyString(), eq(PrivateContestDTO.class)))
            .thenThrow(new RuntimeException("JSON parsing error"));

        // When
        Optional<PrivateContestDTO> result = cacheService.getCachedContest(contestId);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void getCachedContest_WithRedisFailure_ReturnsEmpty() {
        // Given
        when(valueOperations.get("private:contest:" + contestId))
            .thenThrow(new RuntimeException("Redis connection failed"));

        // When
        Optional<PrivateContestDTO> result = cacheService.getCachedContest(contestId);

        // Then
        assertTrue(result.isEmpty());
    }

    // ─── Cache Participant Set Tests ─────────────────────────────────────────

    @Test
    void cacheParticipantSet_WithValidSet_CachesSuccessfully() {
        // When
        cacheService.cacheParticipantSet(contestId, participantIds);

        // Then
        verify(redis).delete("private:participants:" + contestId + ":empty");
        // Verify add was called with the key and 3 string arguments (order may vary due to HashSet)
        verify(setOperations).add(
            eq("private:participants:" + contestId),
            any(String.class), any(String.class), any(String.class)
        );
        verify(redis).expire(
            eq("private:participants:" + contestId),
            eq(Duration.ofHours(6))
        );
    }

    @Test
    void cacheParticipantSet_UsesSixHourTTL() {
        // When
        cacheService.cacheParticipantSet(contestId, participantIds);

        // Then
        ArgumentCaptor<Duration> durationCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(redis).expire(anyString(), durationCaptor.capture());
        
        Duration ttl = durationCaptor.getValue();
        assertEquals(Duration.ofHours(6), ttl);
    }

    @Test
    void cacheParticipantSet_WithEmptySet_CachesEmptyMarker() {
        // When
        cacheService.cacheParticipantSet(contestId, new HashSet<>());

        // Then
        verify(redis).delete("private:participants:" + contestId);
        verify(valueOperations).set(
            eq("private:participants:" + contestId + ":empty"),
            eq("1"),
            eq(Duration.ofHours(6))
        );
        verify(setOperations, never()).add(anyString(), any(String[].class));
    }

    @Test
    void cacheParticipantSet_WithNullSet_CachesEmptyMarker() {
        // When
        cacheService.cacheParticipantSet(contestId, null);

        // Then
        verify(redis).delete("private:participants:" + contestId);
        verify(valueOperations).set(
            eq("private:participants:" + contestId + ":empty"),
            eq("1"),
            eq(Duration.ofHours(6))
        );
    }

    @Test
    void cacheParticipantSet_WithNullContestId_DoesNotCache() {
        // When
        cacheService.cacheParticipantSet(null, participantIds);

        // Then
        verifyNoInteractions(setOperations);
    }

    @Test
    void cacheParticipantSet_WithRedisFailure_DoesNotThrow() {
        // Given
        doThrow(new RuntimeException("Redis connection failed"))
            .when(setOperations).add(anyString(), any(String[].class));

        // When / Then
        assertDoesNotThrow(() -> cacheService.cacheParticipantSet(contestId, participantIds));
    }

    @Test
    void cacheParticipantSet_ClearsEmptyMarkerForNonEmptySet() {
        // When
        cacheService.cacheParticipantSet(contestId, participantIds);

        // Then
        verify(redis).delete("private:participants:" + contestId + ":empty");
    }

    // ─── Is Cached Participant Tests ─────────────────────────────────────────

    @Test
    void isCachedParticipant_WithParticipantInSet_ReturnsTrue() {
        // Given
        when(redis.hasKey("private:participants:" + contestId + ":empty"))
            .thenReturn(false);
        when(redis.hasKey("private:participants:" + contestId))
            .thenReturn(true);
        when(setOperations.isMember("private:participants:" + contestId, "42"))
            .thenReturn(true);

        // When
        boolean result = cacheService.isCachedParticipant(contestId, userId);

        // Then
        assertTrue(result);
    }

    @Test
    void isCachedParticipant_WithParticipantNotInSet_ReturnsFalse() {
        // Given
        when(redis.hasKey("private:participants:" + contestId + ":empty"))
            .thenReturn(false);
        when(redis.hasKey("private:participants:" + contestId))
            .thenReturn(true);
        when(setOperations.isMember("private:participants:" + contestId, "99"))
            .thenReturn(false);

        // When
        boolean result = cacheService.isCachedParticipant(contestId, 99L);

        // Then
        assertFalse(result);
    }

    @Test
    void isCachedParticipant_WithEmptyMarker_ReturnsFalse() {
        // Given
        when(redis.hasKey("private:participants:" + contestId + ":empty"))
            .thenReturn(true);

        // When
        boolean result = cacheService.isCachedParticipant(contestId, userId);

        // Then
        assertFalse(result);
        verify(setOperations, never()).isMember(anyString(), anyString());
    }

    @Test
    void isCachedParticipant_WithCacheMiss_ReturnsFalse() {
        // Given
        when(redis.hasKey("private:participants:" + contestId + ":empty"))
            .thenReturn(false);
        when(redis.hasKey("private:participants:" + contestId))
            .thenReturn(false);

        // When
        boolean result = cacheService.isCachedParticipant(contestId, userId);

        // Then
        assertFalse(result);
        verify(setOperations, never()).isMember(anyString(), anyString());
    }

    @Test
    void isCachedParticipant_WithNullContestId_ReturnsFalse() {
        // When
        boolean result = cacheService.isCachedParticipant(null, userId);

        // Then
        assertFalse(result);
        verifyNoInteractions(setOperations);
    }

    @Test
    void isCachedParticipant_WithNullUserId_ReturnsFalse() {
        // When
        boolean result = cacheService.isCachedParticipant(contestId, null);

        // Then
        assertFalse(result);
        verifyNoInteractions(setOperations);
    }

    @Test
    void isCachedParticipant_WithRedisFailure_ReturnsFalse() {
        // Given
        when(redis.hasKey(anyString()))
            .thenThrow(new RuntimeException("Redis connection failed"));

        // When
        boolean result = cacheService.isCachedParticipant(contestId, userId);

        // Then
        assertFalse(result);
    }

    // ─── Invalidate Contest Cache Tests ─────────────────────────────────────────

    @Test
    void invalidateContestCache_DeletesAllCacheKeys() {
        // When
        cacheService.invalidateContestCache(contestId);

        // Then
        verify(redis).delete("private:contest:" + contestId);
        verify(redis).delete("private:participants:" + contestId);
        verify(redis).delete("private:participants:" + contestId + ":empty");
    }

    @Test
    void invalidateContestCache_WithNullContestId_DoesNothing() {
        // When
        cacheService.invalidateContestCache(null);

        // Then
        verify(redis, never()).delete(anyString());
    }

    @Test
    void invalidateContestCache_WithRedisFailure_DoesNotThrow() {
        // Given
        doThrow(new RuntimeException("Redis connection failed"))
            .when(redis).delete(anyString());

        // When / Then
        assertDoesNotThrow(() -> cacheService.invalidateContestCache(contestId));
    }

    // ─── Is Contest Cached Tests ─────────────────────────────────────────

    @Test
    void isContestCached_WithCachedContest_ReturnsTrue() {
        // Given
        when(redis.hasKey("private:contest:" + contestId))
            .thenReturn(true);

        // When
        boolean result = cacheService.isContestCached(contestId);

        // Then
        assertTrue(result);
    }

    @Test
    void isContestCached_WithNonCachedContest_ReturnsFalse() {
        // Given
        when(redis.hasKey("private:contest:" + contestId))
            .thenReturn(false);

        // When
        boolean result = cacheService.isContestCached(contestId);

        // Then
        assertFalse(result);
    }

    @Test
    void isContestCached_WithNullContestId_ReturnsFalse() {
        // When
        boolean result = cacheService.isContestCached(null);

        // Then
        assertFalse(result);
        verify(redis, never()).hasKey(anyString());
    }

    @Test
    void isContestCached_WithRedisFailure_ReturnsFalse() {
        // Given
        when(redis.hasKey(anyString()))
            .thenThrow(new RuntimeException("Redis connection failed"));

        // When
        boolean result = cacheService.isContestCached(contestId);

        // Then
        assertFalse(result);
    }

    // ─── Is Participant Set Cached Tests ─────────────────────────────────────────

    @Test
    void isParticipantSetCached_WithCachedSet_ReturnsTrue() {
        // Given
        when(redis.hasKey("private:participants:" + contestId))
            .thenReturn(true);
        when(redis.hasKey("private:participants:" + contestId + ":empty"))
            .thenReturn(false);

        // When
        boolean result = cacheService.isParticipantSetCached(contestId);

        // Then
        assertTrue(result);
    }

    @Test
    void isParticipantSetCached_WithEmptyMarker_ReturnsTrue() {
        // Given
        when(redis.hasKey("private:participants:" + contestId))
            .thenReturn(false);
        when(redis.hasKey("private:participants:" + contestId + ":empty"))
            .thenReturn(true);

        // When
        boolean result = cacheService.isParticipantSetCached(contestId);

        // Then
        assertTrue(result);
    }

    @Test
    void isParticipantSetCached_WithNoCachedSet_ReturnsFalse() {
        // Given
        when(redis.hasKey("private:participants:" + contestId))
            .thenReturn(false);
        when(redis.hasKey("private:participants:" + contestId + ":empty"))
            .thenReturn(false);

        // When
        boolean result = cacheService.isParticipantSetCached(contestId);

        // Then
        assertFalse(result);
    }

    @Test
    void isParticipantSetCached_WithNullContestId_ReturnsFalse() {
        // When
        boolean result = cacheService.isParticipantSetCached(null);

        // Then
        assertFalse(result);
        verify(redis, never()).hasKey(anyString());
    }

    @Test
    void isParticipantSetCached_WithRedisFailure_ReturnsFalse() {
        // Given
        when(redis.hasKey(anyString()))
            .thenThrow(new RuntimeException("Redis connection failed"));

        // When
        boolean result = cacheService.isParticipantSetCached(contestId);

        // Then
        assertFalse(result);
    }

    // ─── Integration Scenario Tests ─────────────────────────────────────────

    @Test
    void integrationScenario_CacheThenRetrieve() throws Exception {
        // Given - cache contest
        when(valueOperations.get("private:contest:" + contestId))
            .thenReturn(null) // First call: cache miss
            .thenReturn(contestJson); // Second call: cache hit

        // When - first retrieval (cache miss)
        Optional<PrivateContestDTO> firstResult = cacheService.getCachedContest(contestId);

        // Then
        assertTrue(firstResult.isEmpty());

        // When - cache the contest
        cacheService.cacheContestMetadata(contestId, contestDTO);

        // Then
        verify(valueOperations).set(
            eq("private:contest:" + contestId),
            eq(contestJson),
            eq(Duration.ofHours(6))
        );

        // When - second retrieval (cache hit)
        Optional<PrivateContestDTO> secondResult = cacheService.getCachedContest(contestId);

        // Then
        assertTrue(secondResult.isPresent());
        assertEquals(contestDTO, secondResult.get());
    }

    @Test
    void integrationScenario_CacheParticipantsThenCheck() {
        // Given
        when(redis.hasKey("private:participants:" + contestId + ":empty"))
            .thenReturn(false);
        when(redis.hasKey("private:participants:" + contestId))
            .thenReturn(false) // First check: not cached
            .thenReturn(true); // After caching: cached
        when(setOperations.isMember("private:participants:" + contestId, "42"))
            .thenReturn(true);
        when(setOperations.isMember("private:participants:" + contestId, "99"))
            .thenReturn(false);

        // When - first check (cache miss)
        boolean firstCheck = cacheService.isCachedParticipant(contestId, userId);

        // Then
        assertFalse(firstCheck);

        // When - cache participants
        cacheService.cacheParticipantSet(contestId, participantIds);

        // Then - verify add was called with correct key and 3 arguments (order may vary)
        verify(setOperations).add(
            eq("private:participants:" + contestId),
            any(String.class), any(String.class), any(String.class)
        );

        // When - second check (cache hit, user is participant)
        boolean secondCheck = cacheService.isCachedParticipant(contestId, userId);

        // Then
        assertTrue(secondCheck);

        // When - check non-participant
        boolean thirdCheck = cacheService.isCachedParticipant(contestId, 99L);

        // Then
        assertFalse(thirdCheck);
    }

    @Test
    void integrationScenario_CacheThenInvalidate() throws Exception {
        // Given - cache both contest and participants
        when(redis.hasKey("private:contest:" + contestId))
            .thenReturn(true);
        when(redis.hasKey("private:participants:" + contestId))
            .thenReturn(true);

        // When
        cacheService.cacheContestMetadata(contestId, contestDTO);
        cacheService.cacheParticipantSet(contestId, participantIds);

        // Then - both should be cached
        assertTrue(cacheService.isContestCached(contestId));
        assertTrue(cacheService.isParticipantSetCached(contestId));

        // When - invalidate cache
        cacheService.invalidateContestCache(contestId);

        // Then - all keys should be deleted
        verify(redis, atLeastOnce()).delete("private:contest:" + contestId);
        verify(redis, atLeastOnce()).delete("private:participants:" + contestId);
        verify(redis, atLeastOnce()).delete("private:participants:" + contestId + ":empty");
    }

    @Test
    void integrationScenario_EmptyParticipantSetHandling() {
        // Given
        when(redis.hasKey("private:participants:" + contestId + ":empty"))
            .thenReturn(false) // First: not cached
            .thenReturn(true);  // After caching: has empty marker

        // When - cache empty set
        cacheService.cacheParticipantSet(contestId, new HashSet<>());

        // Then
        verify(valueOperations).set(
            eq("private:participants:" + contestId + ":empty"),
            eq("1"),
            eq(Duration.ofHours(6))
        );

        // When - check participant (should return false due to empty marker)
        boolean result = cacheService.isCachedParticipant(contestId, userId);

        // Then
        assertFalse(result);
        verify(setOperations, never()).isMember(anyString(), anyString());
    }
}
