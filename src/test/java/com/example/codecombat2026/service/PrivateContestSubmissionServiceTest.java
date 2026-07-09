package com.example.codecombat2026.service;

import com.example.codecombat2026.dto.SubmissionJob;
import com.example.codecombat2026.entity.Contest;
import com.example.codecombat2026.entity.Contest.ContestStatus;
import com.example.codecombat2026.entity.PrivateContest;
import com.example.codecombat2026.entity.Problem;
import com.example.codecombat2026.entity.Submission;
import com.example.codecombat2026.entity.Submission.ProgrammingLanguage;
import com.example.codecombat2026.entity.Submission.SubmissionStatus;
import com.example.codecombat2026.entity.User;
import com.example.codecombat2026.exception.ResourceNotFoundException;
import com.example.codecombat2026.repository.ContestRepository;
import com.example.codecombat2026.repository.PrivateContestRepository;
import com.example.codecombat2026.repository.ProblemRepository;
import com.example.codecombat2026.repository.SubmissionRepository;
import com.example.codecombat2026.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PrivateContestSubmissionService.
 *
 * Test coverage:
 * - Successful submission by participant during LIVE contest
 * - Non-participant attempting submission (should fail)
 * - Submission before contest starts (should fail)
 * - Submission after contest ends (should fail)
 * - Submission job creation with correct flags
 * - Queue integration
 *
 * Requirements: 11.5, 13.1, 13.2
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PrivateContestSubmissionService Tests")
class PrivateContestSubmissionServiceTest {

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProblemRepository problemRepository;

    @Mock
    private ContestRepository contestRepository;

    @Mock
    private PrivateContestRepository privateContestRepository;

    @Mock
    private PrivateContestAccessValidator accessValidator;

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private ListOperations<String, String> listOperations;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private com.example.codecombat2026.config.PrivateContestMetricsConfig metricsConfig;

    @InjectMocks
    private PrivateContestSubmissionService submissionService;

    private User testUser;
    private Contest testContest;
    private PrivateContest testPrivateContest;
    private Problem testProblem;
    private Submission testSubmission;

    private static final Long USER_ID = 100L;
    private static final Long CONTEST_ID = 200L;
    private static final Long PRIVATE_CONTEST_ID = 300L;
    private static final Long PROBLEM_ID = 400L;
    private static final Long SUBMISSION_ID = 500L;
    private static final String TEST_CODE = "public class Solution { }";
    private static final String PRIVATE_QUEUE_KEY = "private:submission:queue";

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new User();
        testUser.setId(USER_ID);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        // Setup test contest (LIVE status)
        testContest = new Contest();
        testContest.setId(CONTEST_ID);
        testContest.setName("Test Private Contest");
        testContest.setStatus(ContestStatus.LIVE);
        testContest.setStartTime(LocalDateTime.now().minusHours(1));
        testContest.setEndTime(LocalDateTime.now().plusHours(1));

        // Setup private contest
        testPrivateContest = new PrivateContest();
        testPrivateContest.setId(PRIVATE_CONTEST_ID);
        testPrivateContest.setContest(testContest);
        testPrivateContest.setHostUser(testUser);

        // Setup test problem
        testProblem = new Problem();
        testProblem.setId(PROBLEM_ID);
        testProblem.setTitle("Test Problem");
        testProblem.setTimeLimit(2.0);
        testProblem.setMemoryLimit(128);
        
        // Setup problem-contest relationship
        List<Contest> contests = new ArrayList<>();
        contests.add(testContest);
        testProblem.setContests(contests);

        // Setup test submission
        testSubmission = new Submission();
        testSubmission.setId(SUBMISSION_ID);
        testSubmission.setUser(testUser);
        testSubmission.setProblem(testProblem);
        testSubmission.setContest(testContest);
        testSubmission.setStatus(SubmissionStatus.PENDING);
    }

    @Test
    @DisplayName("Successful submission by participant during LIVE contest")
    void testSuccessfulSubmission() throws Exception {
        // Arrange
        when(redis.opsForList()).thenReturn(listOperations);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(contestRepository.findById(CONTEST_ID)).thenReturn(Optional.of(testContest));
        when(privateContestRepository.findByContestId(CONTEST_ID)).thenReturn(Optional.of(testPrivateContest));
        when(accessValidator.isParticipant(CONTEST_ID, USER_ID)).thenReturn(true);
        when(problemRepository.findById(PROBLEM_ID)).thenReturn(Optional.of(testProblem));
        when(submissionRepository.save(any(Submission.class))).thenReturn(testSubmission);
        
        String expectedJobJson = "{\"submissionId\":500}";
        when(objectMapper.writeValueAsString(any(SubmissionJob.class))).thenReturn(expectedJobJson);
        when(listOperations.leftPush(eq(PRIVATE_QUEUE_KEY), anyString())).thenReturn(1L);

        // Act
        Submission result = submissionService.submitCode(
            CONTEST_ID, PROBLEM_ID, USER_ID, TEST_CODE, ProgrammingLanguage.JAVA
        );

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(SUBMISSION_ID);
        assertThat(result.getStatus()).isEqualTo(SubmissionStatus.PENDING);

        // Verify submission was saved
        ArgumentCaptor<Submission> submissionCaptor = ArgumentCaptor.forClass(Submission.class);
        verify(submissionRepository).save(submissionCaptor.capture());
        Submission savedSubmission = submissionCaptor.getValue();
        assertThat(savedSubmission.getUser()).isEqualTo(testUser);
        assertThat(savedSubmission.getProblem()).isEqualTo(testProblem);
        assertThat(savedSubmission.getContest()).isEqualTo(testContest);
        assertThat(savedSubmission.getCode()).isEqualTo(TEST_CODE);
        assertThat(savedSubmission.getLanguage()).isEqualTo(ProgrammingLanguage.JAVA);
        assertThat(savedSubmission.getStatus()).isEqualTo(SubmissionStatus.PENDING);
        assertThat(savedSubmission.isTestRun()).isFalse();

        // Verify job was created and pushed to private queue
        ArgumentCaptor<SubmissionJob> jobCaptor = ArgumentCaptor.forClass(SubmissionJob.class);
        verify(objectMapper).writeValueAsString(jobCaptor.capture());
        SubmissionJob job = jobCaptor.getValue();
        assertThat(job.getSubmissionId()).isEqualTo(SUBMISSION_ID);
        assertThat(job.getUserId()).isEqualTo(USER_ID);
        assertThat(job.getProblemId()).isEqualTo(PROBLEM_ID);
        assertThat(job.getContestId()).isEqualTo(CONTEST_ID);
        assertThat(job.getPrivateContestId()).isEqualTo(PRIVATE_CONTEST_ID);
        assertThat(job.isTestRun()).isFalse();
        assertThat(job.getDuelId()).isNull();
        assertThat(job.getTimeLimit()).isEqualTo(2.0);
        assertThat(job.getMemoryLimit()).isEqualTo(128);

        verify(listOperations).leftPush(PRIVATE_QUEUE_KEY, expectedJobJson);
        
        // Verify cache eviction
        verify(redis).delete("submissions:user:" + USER_ID);
        verify(redis).delete("submission:user:problem:" + USER_ID + ":" + PROBLEM_ID);
    }

    @Test
    @DisplayName("Non-participant attempting submission should fail with 403 FORBIDDEN")
    void testNonParticipantSubmissionFails() {
        // Arrange
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(contestRepository.findById(CONTEST_ID)).thenReturn(Optional.of(testContest));
        when(privateContestRepository.findByContestId(CONTEST_ID)).thenReturn(Optional.of(testPrivateContest));
        when(accessValidator.isParticipant(CONTEST_ID, USER_ID)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> submissionService.submitCode(
            CONTEST_ID, PROBLEM_ID, USER_ID, TEST_CODE, ProgrammingLanguage.JAVA
        ))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("You must be a participant to submit to this private contest");

        // Verify no submission was saved
        verify(submissionRepository, never()).save(any(Submission.class));
        verify(listOperations, never()).leftPush(anyString(), anyString());
    }

    @Test
    @DisplayName("Submission before contest starts (UPCOMING status) should fail with 409 CONFLICT")
    void testSubmissionBeforeContestStartsFails() {
        // Arrange
        testContest.setStatus(ContestStatus.UPCOMING);
        testContest.setStartTime(LocalDateTime.now().plusHours(1));
        
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(contestRepository.findById(CONTEST_ID)).thenReturn(Optional.of(testContest));
        when(privateContestRepository.findByContestId(CONTEST_ID)).thenReturn(Optional.of(testPrivateContest));
        when(accessValidator.isParticipant(CONTEST_ID, USER_ID)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> submissionService.submitCode(
            CONTEST_ID, PROBLEM_ID, USER_ID, TEST_CODE, ProgrammingLanguage.JAVA
        ))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Submissions are only allowed when the contest is LIVE");

        // Verify no submission was saved
        verify(submissionRepository, never()).save(any(Submission.class));
        verify(listOperations, never()).leftPush(anyString(), anyString());
    }

    @Test
    @DisplayName("Submission after contest ends (ENDED status) should fail with 409 CONFLICT")
    void testSubmissionAfterContestEndsFails() {
        // Arrange
        testContest.setStatus(ContestStatus.ENDED);
        testContest.setEndTime(LocalDateTime.now().minusHours(1));
        
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(contestRepository.findById(CONTEST_ID)).thenReturn(Optional.of(testContest));
        when(privateContestRepository.findByContestId(CONTEST_ID)).thenReturn(Optional.of(testPrivateContest));
        when(accessValidator.isParticipant(CONTEST_ID, USER_ID)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> submissionService.submitCode(
            CONTEST_ID, PROBLEM_ID, USER_ID, TEST_CODE, ProgrammingLanguage.JAVA
        ))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Submissions are only allowed when the contest is LIVE");

        // Verify no submission was saved
        verify(submissionRepository, never()).save(any(Submission.class));
        verify(listOperations, never()).leftPush(anyString(), anyString());
    }

    @Test
    @DisplayName("Submission with non-existent user should fail with ResourceNotFoundException")
    void testSubmissionWithNonExistentUserFails() {
        // Arrange
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> submissionService.submitCode(
            CONTEST_ID, PROBLEM_ID, USER_ID, TEST_CODE, ProgrammingLanguage.JAVA
        ))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("User not found");

        verify(submissionRepository, never()).save(any(Submission.class));
    }

    @Test
    @DisplayName("Submission with non-existent contest should fail with ResourceNotFoundException")
    void testSubmissionWithNonExistentContestFails() {
        // Arrange
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(contestRepository.findById(CONTEST_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> submissionService.submitCode(
            CONTEST_ID, PROBLEM_ID, USER_ID, TEST_CODE, ProgrammingLanguage.JAVA
        ))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Contest not found");

        verify(submissionRepository, never()).save(any(Submission.class));
    }

    @Test
    @DisplayName("Submission with non-existent private contest should fail with ResourceNotFoundException")
    void testSubmissionWithNonExistentPrivateContestFails() {
        // Arrange
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(contestRepository.findById(CONTEST_ID)).thenReturn(Optional.of(testContest));
        when(privateContestRepository.findByContestId(CONTEST_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> submissionService.submitCode(
            CONTEST_ID, PROBLEM_ID, USER_ID, TEST_CODE, ProgrammingLanguage.JAVA
        ))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Private contest not found");

        verify(submissionRepository, never()).save(any(Submission.class));
    }

    @Test
    @DisplayName("Submission with non-existent problem should fail with ResourceNotFoundException")
    void testSubmissionWithNonExistentProblemFails() {
        // Arrange
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(contestRepository.findById(CONTEST_ID)).thenReturn(Optional.of(testContest));
        when(privateContestRepository.findByContestId(CONTEST_ID)).thenReturn(Optional.of(testPrivateContest));
        when(accessValidator.isParticipant(CONTEST_ID, USER_ID)).thenReturn(true);
        when(problemRepository.findById(PROBLEM_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> submissionService.submitCode(
            CONTEST_ID, PROBLEM_ID, USER_ID, TEST_CODE, ProgrammingLanguage.JAVA
        ))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Problem not found");

        verify(submissionRepository, never()).save(any(Submission.class));
    }

    @Test
    @DisplayName("Submission for problem not in contest should fail with BAD_REQUEST")
    void testSubmissionForProblemNotInContestFails() {
        // Arrange
        // Create a problem that doesn't belong to the test contest
        Problem unrelatedProblem = new Problem();
        unrelatedProblem.setId(PROBLEM_ID);
        unrelatedProblem.setTitle("Unrelated Problem");
        unrelatedProblem.setContests(new ArrayList<>()); // Empty contest list
        
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(contestRepository.findById(CONTEST_ID)).thenReturn(Optional.of(testContest));
        when(privateContestRepository.findByContestId(CONTEST_ID)).thenReturn(Optional.of(testPrivateContest));
        when(accessValidator.isParticipant(CONTEST_ID, USER_ID)).thenReturn(true);
        when(problemRepository.findById(PROBLEM_ID)).thenReturn(Optional.of(unrelatedProblem));

        // Act & Assert
        assertThatThrownBy(() -> submissionService.submitCode(
            CONTEST_ID, PROBLEM_ID, USER_ID, TEST_CODE, ProgrammingLanguage.JAVA
        ))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Problem does not belong to this contest");

        verify(submissionRepository, never()).save(any(Submission.class));
    }

    @Test
    @DisplayName("Queue failure should mark submission as failed and throw exception")
    void testQueueFailureHandling() throws Exception {
        // Arrange
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(contestRepository.findById(CONTEST_ID)).thenReturn(Optional.of(testContest));
        when(privateContestRepository.findByContestId(CONTEST_ID)).thenReturn(Optional.of(testPrivateContest));
        when(accessValidator.isParticipant(CONTEST_ID, USER_ID)).thenReturn(true);
        when(problemRepository.findById(PROBLEM_ID)).thenReturn(Optional.of(testProblem));
        when(submissionRepository.save(any(Submission.class))).thenReturn(testSubmission);
        
        // Simulate queue failure
        when(objectMapper.writeValueAsString(any(SubmissionJob.class))).thenThrow(new RuntimeException("Queue error"));

        // Act & Assert
        assertThatThrownBy(() -> submissionService.submitCode(
            CONTEST_ID, PROBLEM_ID, USER_ID, TEST_CODE, ProgrammingLanguage.JAVA
        ))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Failed to queue submission for judging");

        // Verify submission was marked as failed
        verify(submissionRepository, times(2)).save(any(Submission.class));
        verify(listOperations, never()).leftPush(anyString(), anyString());
    }

    @Test
    @DisplayName("Submission job should have correct privateContestId flag")
    void testSubmissionJobHasCorrectPrivateContestIdFlag() throws Exception {
        // Arrange
        when(redis.opsForList()).thenReturn(listOperations);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(contestRepository.findById(CONTEST_ID)).thenReturn(Optional.of(testContest));
        when(privateContestRepository.findByContestId(CONTEST_ID)).thenReturn(Optional.of(testPrivateContest));
        when(accessValidator.isParticipant(CONTEST_ID, USER_ID)).thenReturn(true);
        when(problemRepository.findById(PROBLEM_ID)).thenReturn(Optional.of(testProblem));
        when(submissionRepository.save(any(Submission.class))).thenReturn(testSubmission);
        
        String expectedJobJson = "{\"submissionId\":500}";
        when(objectMapper.writeValueAsString(any(SubmissionJob.class))).thenReturn(expectedJobJson);
        when(listOperations.leftPush(eq(PRIVATE_QUEUE_KEY), anyString())).thenReturn(1L);

        // Act
        submissionService.submitCode(CONTEST_ID, PROBLEM_ID, USER_ID, TEST_CODE, ProgrammingLanguage.PYTHON);

        // Assert
        ArgumentCaptor<SubmissionJob> jobCaptor = ArgumentCaptor.forClass(SubmissionJob.class);
        verify(objectMapper).writeValueAsString(jobCaptor.capture());
        SubmissionJob job = jobCaptor.getValue();
        
        // Verify the privateContestId is set correctly
        assertThat(job.getPrivateContestId()).isEqualTo(PRIVATE_CONTEST_ID);
        assertThat(job.getContestId()).isEqualTo(CONTEST_ID);
        assertThat(job.isTestRun()).isFalse();
        assertThat(job.getDuelId()).isNull();
        assertThat(job.getProctoringSessionId()).isNull();
    }

    @Test
    @DisplayName("Cache eviction should not fail submission even if Redis is down")
    void testCacheEvictionFailureDoesNotFailSubmission() throws Exception {
        // Arrange
        when(redis.opsForList()).thenReturn(listOperations);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(contestRepository.findById(CONTEST_ID)).thenReturn(Optional.of(testContest));
        when(privateContestRepository.findByContestId(CONTEST_ID)).thenReturn(Optional.of(testPrivateContest));
        when(accessValidator.isParticipant(CONTEST_ID, USER_ID)).thenReturn(true);
        when(problemRepository.findById(PROBLEM_ID)).thenReturn(Optional.of(testProblem));
        when(submissionRepository.save(any(Submission.class))).thenReturn(testSubmission);
        
        String expectedJobJson = "{\"submissionId\":500}";
        when(objectMapper.writeValueAsString(any(SubmissionJob.class))).thenReturn(expectedJobJson);
        when(listOperations.leftPush(eq(PRIVATE_QUEUE_KEY), anyString())).thenReturn(1L);
        
        // Simulate cache eviction failure
        when(redis.delete(anyString())).thenThrow(new RuntimeException("Redis error"));

        // Act
        Submission result = submissionService.submitCode(
            CONTEST_ID, PROBLEM_ID, USER_ID, TEST_CODE, ProgrammingLanguage.JAVA
        );

        // Assert - submission should still succeed
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(SUBMISSION_ID);
        verify(submissionRepository).save(any(Submission.class));
        verify(listOperations).leftPush(eq(PRIVATE_QUEUE_KEY), anyString());
    }

    @Test
    @DisplayName("Submission with default time and memory limits when problem limits are null")
    void testSubmissionWithDefaultLimits() throws Exception {
        // Arrange
        testProblem.setTimeLimit(null);
        testProblem.setMemoryLimit(null);
        
        when(redis.opsForList()).thenReturn(listOperations);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(contestRepository.findById(CONTEST_ID)).thenReturn(Optional.of(testContest));
        when(privateContestRepository.findByContestId(CONTEST_ID)).thenReturn(Optional.of(testPrivateContest));
        when(accessValidator.isParticipant(CONTEST_ID, USER_ID)).thenReturn(true);
        when(problemRepository.findById(PROBLEM_ID)).thenReturn(Optional.of(testProblem));
        when(submissionRepository.save(any(Submission.class))).thenReturn(testSubmission);
        
        String expectedJobJson = "{\"submissionId\":500}";
        when(objectMapper.writeValueAsString(any(SubmissionJob.class))).thenReturn(expectedJobJson);
        when(listOperations.leftPush(eq(PRIVATE_QUEUE_KEY), anyString())).thenReturn(1L);

        // Act
        submissionService.submitCode(CONTEST_ID, PROBLEM_ID, USER_ID, TEST_CODE, ProgrammingLanguage.JAVA);

        // Assert
        ArgumentCaptor<SubmissionJob> jobCaptor = ArgumentCaptor.forClass(SubmissionJob.class);
        verify(objectMapper).writeValueAsString(jobCaptor.capture());
        SubmissionJob job = jobCaptor.getValue();
        
        // Verify default limits are used
        assertThat(job.getTimeLimit()).isEqualTo(5.0);
        assertThat(job.getMemoryLimit()).isEqualTo(256);
    }
}
