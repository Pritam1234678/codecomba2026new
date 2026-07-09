package com.example.codecombat2026.service;

import com.example.codecombat2026.dto.ContestAnalyticsDTO;
import com.example.codecombat2026.dto.EngagementTimelineEntryDTO;
import com.example.codecombat2026.dto.ProblemStatDTO;
import com.example.codecombat2026.entity.*;
import com.example.codecombat2026.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PrivateContestAnalyticsService.
 * 
 * Tests the analytics calculation, caching logic, and validation.
 * 
 * Requirements: 16.1, 16.4
 */
@ExtendWith(MockitoExtension.class)
class PrivateContestAnalyticsServiceTest {

    @Mock
    private PrivateContestRepository privateContestRepository;

    @Mock
    private ContestRepository contestRepository;

    @Mock
    private PrivateContestParticipantRepository participantRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private ContestProblemRepository contestProblemRepository;

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private PrivateContestAccessValidator accessValidator;

    @InjectMocks
    private PrivateContestAnalyticsService analyticsService;

    private User hostUser;
    private User participant1;
    private User participant2;
    private Contest contest;
    private PrivateContest privateContest;
    private Problem problem1;
    private Problem problem2;
    private ContestProblem contestProblem1;
    private ContestProblem contestProblem2;

    @BeforeEach
    void setUp() {
        // Mock redis.opsForValue()
        when(redis.opsForValue()).thenReturn(valueOperations);

        // Setup test data
        hostUser = new User();
        hostUser.setId(1L);
        hostUser.setUsername("host");

        participant1 = new User();
        participant1.setId(2L);
        participant1.setUsername("participant1");

        participant2 = new User();
        participant2.setId(3L);
        participant2.setUsername("participant2");

        problem1 = new Problem();
        problem1.setId(10L);
        problem1.setTitle("Two Sum");

        problem2 = new Problem();
        problem2.setId(20L);
        problem2.setTitle("Binary Search");

        contest = new Contest();
        contest.setId(100L);
        contest.setName("Test Contest");
        contest.setStartTime(LocalDateTime.of(2026, 1, 15, 10, 0));
        contest.setEndTime(LocalDateTime.of(2026, 1, 15, 13, 0)); // 3 hours
        contest.setStatus(Contest.ContestStatus.ENDED);

        privateContest = new PrivateContest();
        privateContest.setId(50L);
        privateContest.setContest(contest);
        privateContest.setHostUser(hostUser);

        contestProblem1 = new ContestProblem();
        contestProblem1.setContestId(100L);
        contestProblem1.setProblemId(10L);
        contestProblem1.setProblem(problem1);
        contestProblem1.setDisplayOrder(1);

        contestProblem2 = new ContestProblem();
        contestProblem2.setContestId(100L);
        contestProblem2.setProblemId(20L);
        contestProblem2.setProblem(problem2);
        contestProblem2.setDisplayOrder(2);
    }

    @Test
    @DisplayName("Should calculate analytics for a contest with submissions")
    void testGetAnalytics_WithSubmissions() {
        // Arrange
        Long contestId = 100L;
        Long hostUserId = 1L;

        when(privateContestRepository.findByContestId(contestId))
            .thenReturn(Optional.of(privateContest));
        
        when(participantRepository.countByContestId(contestId)).thenReturn(2L);

        // Create submissions
        List<Submission> submissions = new ArrayList<>();
        
        // Participant 1: AC on problem 1 at 10:20 (20 mins from start)
        Submission sub1 = createSubmission(1L, participant1, problem1, contest, 
            Submission.SubmissionStatus.AC, LocalDateTime.of(2026, 1, 15, 10, 20));
        submissions.add(sub1);

        // Participant 1: WA on problem 2
        Submission sub2 = createSubmission(2L, participant1, problem2, contest,
            Submission.SubmissionStatus.WA, LocalDateTime.of(2026, 1, 15, 10, 30));
        submissions.add(sub2);

        // Participant 2: AC on problem 1 at 10:40 (40 mins from start)
        Submission sub3 = createSubmission(3L, participant2, problem1, contest,
            Submission.SubmissionStatus.AC, LocalDateTime.of(2026, 1, 15, 10, 40));
        submissions.add(sub3);

        when(submissionRepository.findByContest_Id(contestId)).thenReturn(submissions);

        when(contestProblemRepository.findByContestIdOrderByDisplayOrderAscAddedAtAsc(contestId))
            .thenReturn(Arrays.asList(contestProblem1, contestProblem2));

        // Mock cache miss
        when(valueOperations.get("private:analytics:100")).thenReturn(null);

        // Act
        ContestAnalyticsDTO result = analyticsService.getAnalytics(contestId, hostUserId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getTotalParticipants());
        assertEquals(2, result.getActiveParticipants()); // Both participants have submissions
        assertEquals(3, result.getTotalSubmissions());

        // Verify problem stats
        assertEquals(2, result.getProblemStats().size());
        
        ProblemStatDTO problem1Stats = result.getProblemStats().get(0);
        assertEquals(10L, problem1Stats.getProblemId());
        assertEquals("Two Sum", problem1Stats.getProblemTitle());
        assertEquals(2, problem1Stats.getSubmissionCount());
        assertEquals(2, problem1Stats.getAcceptedSubmissions());
        assertEquals(100.0, problem1Stats.getAcceptanceRate());
        assertEquals(30.0, problem1Stats.getAvgSolveTimeMinutes()); // (20 + 40) / 2

        ProblemStatDTO problem2Stats = result.getProblemStats().get(1);
        assertEquals(20L, problem2Stats.getProblemId());
        assertEquals(1, problem2Stats.getSubmissionCount());
        assertEquals(0, problem2Stats.getAcceptedSubmissions());
        assertEquals(0.0, problem2Stats.getAcceptanceRate());
        assertNull(problem2Stats.getAvgSolveTimeMinutes()); // No one solved it

        // Verify engagement timeline exists
        assertNotNull(result.getEngagementTimeline());
        assertTrue(result.getEngagementTimeline().size() > 0);

        // Verify cache was called for ENDED contest
        verify(valueOperations).set(eq("private:analytics:100"), anyString(), eq(Duration.ofHours(24)));
    }

    @Test
    @DisplayName("Should throw exception when user is not the host")
    void testGetAnalytics_NotHost() {
        // Arrange
        Long contestId = 100L;
        Long nonHostUserId = 999L;

        when(privateContestRepository.findByContestId(contestId))
            .thenReturn(Optional.of(privateContest));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            analyticsService.getAnalytics(contestId, nonHostUserId);
        });

        assertTrue(exception.getMessage().contains("not the host"));
    }

    @Test
    @DisplayName("Should throw exception when contest doesn't exist")
    void testGetAnalytics_ContestNotFound() {
        // Arrange
        Long contestId = 999L;
        Long hostUserId = 1L;

        when(privateContestRepository.findByContestId(contestId))
            .thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            analyticsService.getAnalytics(contestId, hostUserId);
        });

        assertTrue(exception.getMessage().contains("not found"));
    }

    @Test
    @DisplayName("Should return cached analytics for ENDED contest")
    void testGetAnalytics_CacheHit() throws Exception {
        // Arrange
        Long contestId = 100L;
        Long hostUserId = 1L;

        when(privateContestRepository.findByContestId(contestId))
            .thenReturn(Optional.of(privateContest));

        ContestAnalyticsDTO cachedData = new ContestAnalyticsDTO(
            5, 4, 20, new ArrayList<>(), new ArrayList<>()
        );
        String cachedJson = "{\"totalParticipants\":5}";

        when(valueOperations.get("private:analytics:100")).thenReturn(cachedJson);
        when(objectMapper.readValue(cachedJson, ContestAnalyticsDTO.class)).thenReturn(cachedData);

        // Act
        ContestAnalyticsDTO result = analyticsService.getAnalytics(contestId, hostUserId);

        // Assert
        assertNotNull(result);
        assertEquals(5, result.getTotalParticipants());
        assertEquals(4, result.getActiveParticipants());
        assertEquals(20, result.getTotalSubmissions());

        // Verify database was not queried
        verify(participantRepository, never()).countByContestId(anyLong());
        verify(submissionRepository, never()).findByContest_Id(anyLong());
    }

    @Test
    @DisplayName("Should not cache analytics for LIVE contest")
    void testGetAnalytics_LiveContest_NoCache() {
        // Arrange
        Long contestId = 100L;
        Long hostUserId = 1L;

        contest.setStatus(Contest.ContestStatus.LIVE); // Change to LIVE

        when(privateContestRepository.findByContestId(contestId))
            .thenReturn(Optional.of(privateContest));
        
        when(participantRepository.countByContestId(contestId)).thenReturn(3L);
        when(submissionRepository.findByContest_Id(contestId)).thenReturn(new ArrayList<>());
        when(contestProblemRepository.findByContestIdOrderByDisplayOrderAscAddedAtAsc(contestId))
            .thenReturn(new ArrayList<>());

        // Act
        ContestAnalyticsDTO result = analyticsService.getAnalytics(contestId, hostUserId);

        // Assert
        assertNotNull(result);

        // Verify cache set was NOT called
        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("Should calculate engagement timeline with 15-minute intervals")
    void testCalculateEngagementTimeline() {
        // Arrange
        Long contestId = 100L;
        Long hostUserId = 1L;

        when(privateContestRepository.findByContestId(contestId))
            .thenReturn(Optional.of(privateContest));
        
        when(participantRepository.countByContestId(contestId)).thenReturn(1L);

        // Create submissions at different times within the contest
        List<Submission> submissions = new ArrayList<>();
        
        // 3 submissions in first 15-min interval (10:00-10:15)
        submissions.add(createSubmission(1L, participant1, problem1, contest,
            Submission.SubmissionStatus.AC, LocalDateTime.of(2026, 1, 15, 10, 5)));
        submissions.add(createSubmission(2L, participant1, problem1, contest,
            Submission.SubmissionStatus.WA, LocalDateTime.of(2026, 1, 15, 10, 10)));
        submissions.add(createSubmission(3L, participant1, problem1, contest,
            Submission.SubmissionStatus.AC, LocalDateTime.of(2026, 1, 15, 10, 14)));

        // 2 submissions in second interval (10:15-10:30)
        submissions.add(createSubmission(4L, participant1, problem2, contest,
            Submission.SubmissionStatus.AC, LocalDateTime.of(2026, 1, 15, 10, 20)));
        submissions.add(createSubmission(5L, participant1, problem2, contest,
            Submission.SubmissionStatus.WA, LocalDateTime.of(2026, 1, 15, 10, 28)));

        when(submissionRepository.findByContest_Id(contestId)).thenReturn(submissions);
        when(contestProblemRepository.findByContestIdOrderByDisplayOrderAscAddedAtAsc(contestId))
            .thenReturn(Arrays.asList(contestProblem1));

        // Act
        ContestAnalyticsDTO result = analyticsService.getAnalytics(contestId, hostUserId);

        // Assert
        assertNotNull(result.getEngagementTimeline());
        
        // Contest is 3 hours (180 minutes) = 12 fifteen-minute intervals
        assertEquals(13, result.getEngagementTimeline().size()); // 13 buckets (0, 15, 30, ..., 180)

        // Check first two intervals have correct counts
        EngagementTimelineEntryDTO firstInterval = result.getEngagementTimeline().get(0);
        assertEquals(3, firstInterval.getSubmissionCount());

        EngagementTimelineEntryDTO secondInterval = result.getEngagementTimeline().get(1);
        assertEquals(2, secondInterval.getSubmissionCount());
    }

    @Test
    @DisplayName("Should handle contest with no submissions")
    void testGetAnalytics_NoSubmissions() {
        // Arrange
        Long contestId = 100L;
        Long hostUserId = 1L;

        when(privateContestRepository.findByContestId(contestId))
            .thenReturn(Optional.of(privateContest));
        
        when(participantRepository.countByContestId(contestId)).thenReturn(5L);
        when(submissionRepository.findByContest_Id(contestId)).thenReturn(new ArrayList<>());
        when(contestProblemRepository.findByContestIdOrderByDisplayOrderAscAddedAtAsc(contestId))
            .thenReturn(Arrays.asList(contestProblem1, contestProblem2));

        // Act
        ContestAnalyticsDTO result = analyticsService.getAnalytics(contestId, hostUserId);

        // Assert
        assertNotNull(result);
        assertEquals(5, result.getTotalParticipants());
        assertEquals(0, result.getActiveParticipants()); // No submissions
        assertEquals(0, result.getTotalSubmissions());

        // Problem stats should show zero submissions
        assertEquals(2, result.getProblemStats().size());
        result.getProblemStats().forEach(stat -> {
            assertEquals(0, stat.getSubmissionCount());
            assertEquals(0, stat.getAcceptedSubmissions());
            assertEquals(0.0, stat.getAcceptanceRate());
            assertNull(stat.getAvgSolveTimeMinutes());
        });
    }

    @Test
    @DisplayName("Should invalidate cache successfully")
    void testInvalidateCache() {
        // Arrange
        Long contestId = 100L;

        // Act
        analyticsService.invalidateCache(contestId);

        // Assert
        verify(redis).delete("private:analytics:100");
    }

    @Test
    @DisplayName("Requirement 16.1: Calculate total participants, active participants, total submissions")
    void testRequirement_16_1_BasicMetrics() {
        // Arrange
        Long contestId = 100L;
        Long hostUserId = 1L;

        when(privateContestRepository.findByContestId(contestId))
            .thenReturn(Optional.of(privateContest));
        
        // 3 participants invited
        when(participantRepository.countByContestId(contestId)).thenReturn(3L);

        // Only 2 participants made submissions
        List<Submission> submissions = Arrays.asList(
            createSubmission(1L, participant1, problem1, contest, Submission.SubmissionStatus.AC, 
                LocalDateTime.of(2026, 1, 15, 10, 10)),
            createSubmission(2L, participant2, problem1, contest, Submission.SubmissionStatus.WA,
                LocalDateTime.of(2026, 1, 15, 10, 20))
        );

        when(submissionRepository.findByContest_Id(contestId)).thenReturn(submissions);
        when(contestProblemRepository.findByContestIdOrderByDisplayOrderAscAddedAtAsc(contestId))
            .thenReturn(Arrays.asList(contestProblem1));

        // Act
        ContestAnalyticsDTO result = analyticsService.getAnalytics(contestId, hostUserId);

        // Assert - Requirement 16.1
        assertEquals(3, result.getTotalParticipants(), "Total invited participants");
        assertEquals(2, result.getActiveParticipants(), "Active participants with submissions");
        assertEquals(2, result.getTotalSubmissions(), "Total submissions");
    }

    @Test
    @DisplayName("Requirement 16.4: Cache analytics for ENDED contests with 24-hour TTL")
    void testRequirement_16_4_CachingForEndedContests() {
        // Arrange
        Long contestId = 100L;
        Long hostUserId = 1L;

        contest.setStatus(Contest.ContestStatus.ENDED);

        when(privateContestRepository.findByContestId(contestId))
            .thenReturn(Optional.of(privateContest));
        
        when(participantRepository.countByContestId(contestId)).thenReturn(2L);
        when(submissionRepository.findByContest_Id(contestId)).thenReturn(new ArrayList<>());
        when(contestProblemRepository.findByContestIdOrderByDisplayOrderAscAddedAtAsc(contestId))
            .thenReturn(new ArrayList<>());

        // Act
        analyticsService.getAnalytics(contestId, hostUserId);

        // Assert - Requirement 16.4: Cache with 24-hour TTL
        verify(valueOperations).set(
            eq("private:analytics:100"),
            anyString(),
            eq(Duration.ofHours(24))
        );
    }

    @Test
    @DisplayName("Should export analytics as CSV format")
    void testExportAnalyticsCSV() {
        // Arrange
        Long contestId = 100L;
        Long hostUserId = 1L;

        when(privateContestRepository.findByContestId(contestId))
            .thenReturn(Optional.of(privateContest));
        
        when(participantRepository.countByContestId(contestId)).thenReturn(2L);

        // Create submissions
        List<Submission> submissions = new ArrayList<>();
        
        // Participant 1: AC on problem 1 at 10:20
        Submission sub1 = createSubmission(1L, participant1, problem1, contest, 
            Submission.SubmissionStatus.AC, LocalDateTime.of(2026, 1, 15, 10, 20));
        submissions.add(sub1);

        // Participant 2: AC on problem 1 at 10:40
        Submission sub2 = createSubmission(2L, participant2, problem1, contest,
            Submission.SubmissionStatus.AC, LocalDateTime.of(2026, 1, 15, 10, 40));
        submissions.add(sub2);

        when(submissionRepository.findByContest_Id(contestId)).thenReturn(submissions);
        when(contestProblemRepository.findByContestIdOrderByDisplayOrderAscAddedAtAsc(contestId))
            .thenReturn(Arrays.asList(contestProblem1, contestProblem2));

        // Act
        String csv = analyticsService.exportAnalyticsCSV(contestId, hostUserId);

        // Assert
        assertNotNull(csv);
        assertTrue(csv.contains("Contest Name,Test Contest"));
        assertTrue(csv.contains("Host,host"));
        assertTrue(csv.contains("Start Time,2026-01-15T10:00:00"));
        assertTrue(csv.contains("End Time,2026-01-15T13:00:00"));
        assertTrue(csv.contains("Total Participants,2"));
        assertTrue(csv.contains("Active Participants,2"));
        assertTrue(csv.contains("Total Submissions,2"));
        
        // Verify CSV header
        assertTrue(csv.contains("Problem ID,Problem Title,Total Submissions,Accepted Submissions,Acceptance Rate (%),Avg Solve Time (min)"));
        
        // Verify problem data rows
        assertTrue(csv.contains("10,Two Sum,2,2,100.00,30.00"));
        assertTrue(csv.contains("20,Binary Search,0,0,0.00,N/A"));
    }

    @Test
    @DisplayName("Should escape CSV values with commas and quotes")
    void testExportAnalyticsCSV_EscapingSpecialCharacters() {
        // Arrange
        Long contestId = 100L;
        Long hostUserId = 1L;

        // Contest with special characters in name
        contest.setName("Test, \"Special\" Contest");
        
        // Problem with special characters
        problem1.setTitle("Problem: \"Sort, Search\"");

        when(privateContestRepository.findByContestId(contestId))
            .thenReturn(Optional.of(privateContest));
        
        when(participantRepository.countByContestId(contestId)).thenReturn(1L);
        when(submissionRepository.findByContest_Id(contestId)).thenReturn(new ArrayList<>());
        when(contestProblemRepository.findByContestIdOrderByDisplayOrderAscAddedAtAsc(contestId))
            .thenReturn(Arrays.asList(contestProblem1));

        // Act
        String csv = analyticsService.exportAnalyticsCSV(contestId, hostUserId);

        // Assert
        assertNotNull(csv);
        // CSV should escape the contest name with quotes
        assertTrue(csv.contains("Contest Name,\"Test, \"\"Special\"\" Contest\""));
        // CSV should escape the problem title with quotes
        assertTrue(csv.contains("\"Problem: \"\"Sort, Search\"\"\""));
    }

    @Test
    @DisplayName("Should throw exception when non-host tries to export CSV")
    void testExportAnalyticsCSV_NotHost() {
        // Arrange
        Long contestId = 100L;
        Long nonHostUserId = 999L;

        when(privateContestRepository.findByContestId(contestId))
            .thenReturn(Optional.of(privateContest));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            analyticsService.exportAnalyticsCSV(contestId, nonHostUserId);
        });

        assertTrue(exception.getMessage().contains("not the host"));
    }

    @Test
    @DisplayName("Should export CSV with N/A for unsolved problems")
    void testExportAnalyticsCSV_UnsolvedProblems() {
        // Arrange
        Long contestId = 100L;
        Long hostUserId = 1L;

        when(privateContestRepository.findByContestId(contestId))
            .thenReturn(Optional.of(privateContest));
        
        when(participantRepository.countByContestId(contestId)).thenReturn(2L);
        
        // All submissions are wrong answer
        List<Submission> submissions = Arrays.asList(
            createSubmission(1L, participant1, problem1, contest, 
                Submission.SubmissionStatus.WA, LocalDateTime.of(2026, 1, 15, 10, 20)),
            createSubmission(2L, participant2, problem1, contest,
                Submission.SubmissionStatus.WA, LocalDateTime.of(2026, 1, 15, 10, 30))
        );

        when(submissionRepository.findByContest_Id(contestId)).thenReturn(submissions);
        when(contestProblemRepository.findByContestIdOrderByDisplayOrderAscAddedAtAsc(contestId))
            .thenReturn(Arrays.asList(contestProblem1));

        // Act
        String csv = analyticsService.exportAnalyticsCSV(contestId, hostUserId);

        // Assert
        assertNotNull(csv);
        // No one solved the problem, so avg solve time should be N/A
        assertTrue(csv.contains("10,Two Sum,2,0,0.00,N/A"));
    }

    @Test
    @DisplayName("Requirement 16.3: Export analytics as CSV file")
    void testRequirement_16_3_CSVExport() {
        // Arrange
        Long contestId = 100L;
        Long hostUserId = 1L;

        when(privateContestRepository.findByContestId(contestId))
            .thenReturn(Optional.of(privateContest));
        
        when(participantRepository.countByContestId(contestId)).thenReturn(3L);
        
        List<Submission> submissions = Arrays.asList(
            createSubmission(1L, participant1, problem1, contest, 
                Submission.SubmissionStatus.AC, LocalDateTime.of(2026, 1, 15, 10, 15))
        );

        when(submissionRepository.findByContest_Id(contestId)).thenReturn(submissions);
        when(contestProblemRepository.findByContestIdOrderByDisplayOrderAscAddedAtAsc(contestId))
            .thenReturn(Arrays.asList(contestProblem1, contestProblem2));

        // Act - Requirement 16.3: Export analytics as CSV
        String csv = analyticsService.exportAnalyticsCSV(contestId, hostUserId);

        // Assert - CSV format with proper headers and data
        assertNotNull(csv, "CSV should be generated");
        
        // Verify CSV structure
        String[] lines = csv.split("\n");
        assertTrue(lines.length >= 9, "CSV should have at least 9 lines (metadata + headers + data)");
        
        // Verify metadata section
        assertEquals("Contest Name,Test Contest", lines[0]);
        assertEquals("Host,host", lines[1]);
        assertTrue(lines[2].startsWith("Start Time,"));
        assertTrue(lines[3].startsWith("End Time,"));
        assertEquals("Total Participants,3", lines[4]);
        assertEquals("Active Participants,1", lines[5]);
        assertEquals("Total Submissions,1", lines[6]);
        assertEquals("", lines[7]); // Blank line separator
        
        // Verify problem stats header
        assertEquals("Problem ID,Problem Title,Total Submissions,Accepted Submissions,Acceptance Rate (%),Avg Solve Time (min)", lines[8]);
        
        // Verify data rows exist
        assertTrue(lines[9].startsWith("10,"), "First problem data row");
        assertTrue(lines[10].startsWith("20,"), "Second problem data row");
    }

    // Helper method to create submissions
    private Submission createSubmission(Long id, User user, Problem problem, Contest contest,
                                       Submission.SubmissionStatus status, LocalDateTime submittedAt) {
        Submission submission = new Submission();
        submission.setId(id);
        submission.setUser(user);
        submission.setProblem(problem);
        submission.setContest(contest);
        submission.setStatus(status);
        submission.setSubmittedAt(submittedAt);
        return submission;
    }
}
