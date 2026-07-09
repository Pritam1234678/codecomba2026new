package com.example.codecombat2026.service;

import com.example.codecombat2026.dto.LeaderboardEntry;
import com.example.codecombat2026.entity.PrivateContestParticipant;
import com.example.codecombat2026.entity.Submission;
import com.example.codecombat2026.entity.User;
import com.example.codecombat2026.repository.PrivateContestParticipantRepository;
import com.example.codecombat2026.repository.SubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Duration;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PrivateContestLeaderboardService.
 * 
 * Tests the leaderboard initialization, retrieval, and persistence logic
 * for private contests using Valkey ZSET operations.
 * 
 * Requirements: 12.4, 12.5, 14.1, 14.3
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PrivateContestLeaderboardService Tests")
class PrivateContestLeaderboardServiceTest {

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private ZSetOperations<String, String> zSetOps;

    @Mock
    private PrivateContestParticipantRepository participantRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @InjectMocks
    private PrivateContestLeaderboardService service;

    private static final Long CONTEST_ID = 101L;
    private static final String LEADERBOARD_KEY = "private:leaderboard:101";

    @BeforeEach
    void setUp() {
        when(redis.opsForZSet()).thenReturn(zSetOps);
    }

    @Test
    @DisplayName("initializeLeaderboard - should create empty ZSET with TTL")
    void testInitializeLeaderboard_Success() {
        // Given
        when(redis.hasKey(LEADERBOARD_KEY)).thenReturn(false);
        when(zSetOps.add(LEADERBOARD_KEY, "__init__", 0)).thenReturn(true);
        when(zSetOps.remove(LEADERBOARD_KEY, "__init__")).thenReturn(1L);
        when(redis.expire(eq(LEADERBOARD_KEY), any(Duration.class))).thenReturn(true);

        // When
        service.initializeLeaderboard(CONTEST_ID);

        // Then
        verify(redis).hasKey(LEADERBOARD_KEY);
        verify(zSetOps).add(LEADERBOARD_KEY, "__init__", 0);
        verify(zSetOps).remove(LEADERBOARD_KEY, "__init__");
        verify(redis).expire(eq(LEADERBOARD_KEY), any(Duration.class));
    }

    @Test
    @DisplayName("initializeLeaderboard - should skip if already exists")
    void testInitializeLeaderboard_AlreadyExists() {
        // Given
        when(redis.hasKey(LEADERBOARD_KEY)).thenReturn(true);

        // When
        service.initializeLeaderboard(CONTEST_ID);

        // Then
        verify(redis).hasKey(LEADERBOARD_KEY);
        verify(zSetOps, never()).add(anyString(), anyString(), anyDouble());
    }

    @Test
    @DisplayName("initializeLeaderboard - should handle null contestId gracefully")
    void testInitializeLeaderboard_NullContestId() {
        // When
        service.initializeLeaderboard(null);

        // Then
        verify(redis, never()).hasKey(anyString());
    }

    @Test
    @DisplayName("getLeaderboard - should return sorted leaderboard from cache")
    void testGetLeaderboard_FromCache() {
        // Given
        Long userId1 = 1L;
        Long userId2 = 2L;
        Long userId3 = 3L;

        // Mock cache exists
        when(redis.hasKey(LEADERBOARD_KEY)).thenReturn(true);

        // Mock ZSET entries (sorted by score descending)
        Set<ZSetOperations.TypedTuple<String>> mockEntries = new LinkedHashSet<>();
        mockEntries.add(createTuple(userId1.toString(), 300.0)); // Rank 1
        mockEntries.add(createTuple(userId2.toString(), 200.0)); // Rank 2
        mockEntries.add(createTuple(userId3.toString(), 100.0)); // Rank 3

        when(zSetOps.reverseRangeWithScores(LEADERBOARD_KEY, 0, -1)).thenReturn(mockEntries);

        // Mock participant users
        List<PrivateContestParticipant> participants = Arrays.asList(
            createParticipant(userId1, "Alice", "alice_dev"),
            createParticipant(userId2, "Bob", "bob_coder"),
            createParticipant(userId3, "Charlie", "charlie_algo")
        );
        when(participantRepository.findByContestId(CONTEST_ID)).thenReturn(participants);

        // When
        List<LeaderboardEntry> result = service.getLeaderboard(CONTEST_ID);

        // Then
        assertThat(result).hasSize(3);
        
        // Verify rank 1
        assertThat(result.get(0).getRank()).isEqualTo(1);
        assertThat(result.get(0).getUserId()).isEqualTo(userId1);
        assertThat(result.get(0).getUserName()).isEqualTo("Alice");
        assertThat(result.get(0).getUserRoll()).isEqualTo("alice_dev");
        assertThat(result.get(0).getTotalScore()).isEqualTo(300.0);

        // Verify rank 2
        assertThat(result.get(1).getRank()).isEqualTo(2);
        assertThat(result.get(1).getUserId()).isEqualTo(userId2);
        assertThat(result.get(1).getTotalScore()).isEqualTo(200.0);

        // Verify rank 3
        assertThat(result.get(2).getRank()).isEqualTo(3);
        assertThat(result.get(2).getUserId()).isEqualTo(userId3);
        assertThat(result.get(2).getTotalScore()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("getLeaderboard - should return empty list when cache is empty")
    void testGetLeaderboard_EmptyCache() {
        // Given
        when(redis.hasKey(LEADERBOARD_KEY)).thenReturn(true);
        when(zSetOps.reverseRangeWithScores(LEADERBOARD_KEY, 0, -1))
            .thenReturn(Collections.emptySet());

        // When
        List<LeaderboardEntry> result = service.getLeaderboard(CONTEST_ID);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getLeaderboard - should fallback to database when cache missing")
    void testGetLeaderboard_CacheMiss_FallbackToDatabase() {
        // Given
        when(redis.hasKey(LEADERBOARD_KEY)).thenReturn(false);

        Long userId = 1L;
        List<PrivateContestParticipant> participants = Arrays.asList(
            createParticipant(userId, "Alice", "alice_dev")
        );
        when(participantRepository.findByContestId(CONTEST_ID)).thenReturn(participants);

        // Mock submissions
        List<Submission> submissions = Arrays.asList(
            createSubmission(userId, 1L, 100, Submission.SubmissionStatus.AC),
            createSubmission(userId, 2L, 80, Submission.SubmissionStatus.AC)
        );
        when(submissionRepository.findByContestIdWithUser(CONTEST_ID)).thenReturn(submissions);

        // When
        List<LeaderboardEntry> result = service.getLeaderboard(CONTEST_ID);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(userId);
        assertThat(result.get(0).getTotalScore()).isEqualTo(180.0); // 100 + 80
        assertThat(result.get(0).getProblemsSolved()).isEqualTo(1); // Only 1 problem with score 100
    }

    @Test
    @DisplayName("getLeaderboard - should handle null contestId gracefully")
    void testGetLeaderboard_NullContestId() {
        // When
        List<LeaderboardEntry> result = service.getLeaderboard(null);

        // Then
        assertThat(result).isEmpty();
        verify(redis, never()).hasKey(anyString());
    }

    @Test
    @DisplayName("persistLeaderboard - should log final rankings")
    void testPersistLeaderboard_Success() {
        // Given
        when(redis.hasKey(LEADERBOARD_KEY)).thenReturn(true);

        Set<ZSetOperations.TypedTuple<String>> mockEntries = new LinkedHashSet<>();
        mockEntries.add(createTuple("1", 300.0));
        mockEntries.add(createTuple("2", 200.0));

        when(zSetOps.reverseRangeWithScores(LEADERBOARD_KEY, 0, -1)).thenReturn(mockEntries);

        // When
        service.persistLeaderboard(CONTEST_ID);

        // Then
        verify(redis).hasKey(LEADERBOARD_KEY);
        verify(zSetOps).reverseRangeWithScores(LEADERBOARD_KEY, 0, -1);
        // Persistence is logged but not stored (submissions table is source of truth)
    }

    @Test
    @DisplayName("persistLeaderboard - should handle missing cache gracefully")
    void testPersistLeaderboard_CacheMissing() {
        // Given
        when(redis.hasKey(LEADERBOARD_KEY)).thenReturn(false);

        // When
        service.persistLeaderboard(CONTEST_ID);

        // Then
        verify(redis).hasKey(LEADERBOARD_KEY);
        verify(zSetOps, never()).reverseRangeWithScores(anyString(), anyLong(), anyLong());
    }

    @Test
    @DisplayName("persistLeaderboard - should handle null contestId gracefully")
    void testPersistLeaderboard_NullContestId() {
        // When
        service.persistLeaderboard(null);

        // Then
        verify(redis, never()).hasKey(anyString());
    }

    @Test
    @DisplayName("updateScore - should increment user score atomically")
    void testUpdateScore_Success() {
        // Given
        Long userId = 1L;
        double scoreToAdd = 100.0;
        
        when(zSetOps.incrementScore(LEADERBOARD_KEY, userId.toString(), scoreToAdd))
            .thenReturn(100.0);
        when(redis.expire(eq(LEADERBOARD_KEY), any(Duration.class))).thenReturn(true);

        // When
        service.updateScore(CONTEST_ID, userId, scoreToAdd);

        // Then
        verify(zSetOps).incrementScore(LEADERBOARD_KEY, userId.toString(), scoreToAdd);
        verify(redis).expire(eq(LEADERBOARD_KEY), any(Duration.class));
    }

    @Test
    @DisplayName("updateScore - should handle null parameters gracefully")
    void testUpdateScore_NullParameters() {
        // When
        service.updateScore(null, 1L, 100.0);
        service.updateScore(CONTEST_ID, null, 100.0);

        // Then
        verify(zSetOps, never()).incrementScore(anyString(), anyString(), anyDouble());
    }

    @Test
    @DisplayName("getUserRank - should return 1-indexed rank")
    void testGetUserRank_Success() {
        // Given
        Long userId = 1L;
        when(zSetOps.reverseRank(LEADERBOARD_KEY, userId.toString())).thenReturn(0L); // 0-indexed

        // When
        Long rank = service.getUserRank(CONTEST_ID, userId);

        // Then
        assertThat(rank).isEqualTo(1L); // Converted to 1-indexed
    }

    @Test
    @DisplayName("getUserRank - should return null when user not found")
    void testGetUserRank_UserNotFound() {
        // Given
        Long userId = 1L;
        when(zSetOps.reverseRank(LEADERBOARD_KEY, userId.toString())).thenReturn(null);

        // When
        Long rank = service.getUserRank(CONTEST_ID, userId);

        // Then
        assertThat(rank).isNull();
    }

    @Test
    @DisplayName("getUserScore - should return user's score")
    void testGetUserScore_Success() {
        // Given
        Long userId = 1L;
        when(zSetOps.score(LEADERBOARD_KEY, userId.toString())).thenReturn(250.0);

        // When
        Double score = service.getUserScore(CONTEST_ID, userId);

        // Then
        assertThat(score).isEqualTo(250.0);
    }

    @Test
    @DisplayName("getUserScore - should return null when user not found")
    void testGetUserScore_UserNotFound() {
        // Given
        Long userId = 1L;
        when(zSetOps.score(LEADERBOARD_KEY, userId.toString())).thenReturn(null);

        // When
        Double score = service.getUserScore(CONTEST_ID, userId);

        // Then
        assertThat(score).isNull();
    }

    @Test
    @DisplayName("leaderboardExists - should return true when cache exists")
    void testLeaderboardExists_True() {
        // Given
        when(redis.hasKey(LEADERBOARD_KEY)).thenReturn(true);

        // When
        boolean exists = service.leaderboardExists(CONTEST_ID);

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("leaderboardExists - should return false when cache missing")
    void testLeaderboardExists_False() {
        // Given
        when(redis.hasKey(LEADERBOARD_KEY)).thenReturn(false);

        // When
        boolean exists = service.leaderboardExists(CONTEST_ID);

        // Then
        assertThat(exists).isFalse();
    }

    // Helper methods

    private ZSetOperations.TypedTuple<String> createTuple(String value, Double score) {
        return new ZSetOperations.TypedTuple<String>() {
            @Override
            public String getValue() {
                return value;
            }

            @Override
            public Double getScore() {
                return score;
            }

            @Override
            public int compareTo(ZSetOperations.TypedTuple<String> o) {
                return Double.compare(this.getScore(), o.getScore());
            }
        };
    }

    private PrivateContestParticipant createParticipant(Long userId, String fullName, String username) {
        User user = new User();
        user.setId(userId);
        user.setFullName(fullName);
        user.setUsername(username);

        PrivateContestParticipant participant = new PrivateContestParticipant();
        participant.setUser(user);
        return participant;
    }

    private Submission createSubmission(Long userId, Long problemId, Integer score, 
                                        Submission.SubmissionStatus status) {
        Submission submission = new Submission();
        submission.setScore(score);
        submission.setStatus(status);
        
        User user = new User();
        user.setId(userId);
        user.setFullName("Test User");
        user.setUsername("test_user");
        submission.setUser(user);
        
        // Create problem entity
        com.example.codecombat2026.entity.Problem problem = 
            new com.example.codecombat2026.entity.Problem();
        problem.setId(problemId);
        submission.setProblem(problem);
        
        return submission;
    }
}
