package com.example.codecombat2026.controller;

import com.example.codecombat2026.entity.*;
import com.example.codecombat2026.exception.GlobalExceptionHandler;
import com.example.codecombat2026.repository.PrivateContestParticipantRepository;
import com.example.codecombat2026.repository.PrivateContestRepository;
import com.example.codecombat2026.repository.SubmissionRepository;
import com.example.codecombat2026.security.SecurityConfig;
import com.example.codecombat2026.security.jwt.*;
import com.example.codecombat2026.security.services.UserDetailsImpl;
import com.example.codecombat2026.security.services.UserDetailsServiceImpl;
import com.example.codecombat2026.service.RateLimiterService;
import com.example.codecombat2026.service.SubmissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for PrivateContestSubmissionController.
 * 
 * Tests submission endpoints for private contests:
 * - POST /api/contests/private/{id}/submit
 * - GET /api/contests/private/{id}/submissions
 * 
 * Verifies:
 * - Access control (participant vs host)
 * - Contest status validation (LIVE required for submissions)
 * - Rate limiting integration
 * - Submission filtering (userId, problemId, status)
 * 
 * Requirements: 11.5, 13.1, 13.6
 */
@WebMvcTest(controllers = PrivateContestSubmissionController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
    "codecombat.jwt.secret=dGhpcy1pcy1hLXRlc3Qtc2VjcmV0LXBhZGRlZC10by0zMi1ieXRlcw==",
    "codecombat.jwt.expiration=86400000",
    "APP_ALLOWED_ORIGINS=http://localhost:5173"
})
@DisplayName("PrivateContestSubmissionController Tests")
class PrivateContestSubmissionControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private SubmissionService submissionService;

    @MockBean
    private PrivateContestRepository privateContestRepository;

    @MockBean
    private PrivateContestParticipantRepository participantRepository;

    @MockBean
    private SubmissionRepository submissionRepository;

    @MockBean
    private RateLimiterService rateLimiter;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private AuthEntryPointJwt entryPoint;

    @MockBean
    private AuthTokenFilter authTokenFilter;

    @MockBean
    private JwtUtils jwtUtils;

    private User testUser;
    private UserDetailsImpl testUserDetails;
    private User hostUser;
    private Contest contest;
    private PrivateContest privateContest;
    private Problem problem;

    @BeforeEach
    void setUp() {
        // Setup test user (participant)
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("participant_user");
        testUser.setEmail("participant@example.com");
        testUser.setEnabled(true);

        testUserDetails = UserDetailsImpl.build(testUser);

        // Setup host user
        hostUser = new User();
        hostUser.setId(2L);
        hostUser.setUsername("host_user");
        hostUser.setEmail("host@example.com");
        hostUser.setEnabled(true);

        // Setup contest
        contest = new Contest();
        contest.setId(100L);
        contest.setName("Test Private Contest");
        contest.setStatus(Contest.ContestStatus.LIVE);
        contest.setStartTime(LocalDateTime.now().minusHours(1));
        contest.setEndTime(LocalDateTime.now().plusHours(2));

        // Setup private contest
        privateContest = new PrivateContest();
        privateContest.setId(1L);
        privateContest.setContest(contest);
        privateContest.setHostUser(hostUser);
        privateContest.setEnableProctoring(false);

        // Setup problem
        problem = new Problem();
        problem.setId(10L);
        problem.setTitle("Test Problem");
    }

    @Nested
    @DisplayName("POST /api/contests/private/{id}/submit")
    class SubmitCode {

        @Test
        @WithMockUser(username = "participant_user", roles = {"USER"})
        @DisplayName("Should accept submission from participant when contest is LIVE")
        void testSubmitCode_Success() throws Exception {
            // Arrange
            when(privateContestRepository.findById(1L)).thenReturn(Optional.of(privateContest));
            when(participantRepository.existsByContestIdAndUserId(100L, 1L)).thenReturn(true);
            when(rateLimiter.allowSubmission(1L)).thenReturn(true);
            when(submissionService.countContestSubmits(1L, 10L)).thenReturn(2L);

            Submission mockSubmission = new Submission();
            mockSubmission.setId(500L);
            mockSubmission.setStatus(Submission.SubmissionStatus.PENDING);
            mockSubmission.setSubmittedAt(LocalDateTime.now());

            when(submissionService.submitCodeAsync(eq(1L), eq(10L), anyString(), any(Submission.ProgrammingLanguage.class)))
                    .thenReturn(mockSubmission);

            String requestBody = """
                {
                    "problemId": 10,
                    "code": "public class Solution { }",
                    "language": "JAVA"
                }
                """;

            // Act & Assert
            mvc.perform(post("/api/contests/private/1/submit")
                    .with(user(testUserDetails))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.id").value(500))
                    .andExpect(jsonPath("$.status").value("PENDING"));

            verify(submissionService).submitCodeAsync(eq(1L), eq(10L), anyString(), any(Submission.ProgrammingLanguage.class));
        }

        @Test
        @WithMockUser(username = "non_participant", roles = {"USER"})
        @DisplayName("Should return 403 when user is not a participant")
        void testSubmitCode_NotParticipant() throws Exception {
            // Arrange
            when(privateContestRepository.findById(1L)).thenReturn(Optional.of(privateContest));
            when(participantRepository.existsByContestIdAndUserId(100L, 1L)).thenReturn(false);

            String requestBody = """
                {
                    "problemId": 10,
                    "code": "public class Solution { }",
                    "language": "JAVA"
                }
                """;

            // Act & Assert
            mvc.perform(post("/api/contests/private/1/submit")
                    .with(user(testUserDetails))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("You are not a participant in this private contest"));

            verify(submissionService, never()).submitCodeAsync(anyLong(), anyLong(), anyString(), any(Submission.ProgrammingLanguage.class));
        }

        @Test
        @WithMockUser(username = "participant_user", roles = {"USER"})
        @DisplayName("Should return 409 when contest is not LIVE")
        void testSubmitCode_ContestNotLive() throws Exception {
            // Arrange
            contest.setStatus(Contest.ContestStatus.UPCOMING);
            when(privateContestRepository.findById(1L)).thenReturn(Optional.of(privateContest));
            when(participantRepository.existsByContestIdAndUserId(100L, 1L)).thenReturn(true);

            String requestBody = """
                {
                    "problemId": 10,
                    "code": "public class Solution { }",
                    "language": "JAVA"
                }
                """;

            // Act & Assert
            mvc.perform(post("/api/contests/private/1/submit")
                    .with(user(testUserDetails))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Contest is not currently live. Submissions are only allowed during LIVE status."));

            verify(submissionService, never()).submitCodeAsync(anyLong(), anyLong(), anyString(), any(Submission.ProgrammingLanguage.class));
        }

        @Test
        @WithMockUser(username = "participant_user", roles = {"USER"})
        @DisplayName("Should return 429 when rate limit exceeded")
        void testSubmitCode_RateLimitExceeded() throws Exception {
            // Arrange
            when(privateContestRepository.findById(1L)).thenReturn(Optional.of(privateContest));
            when(participantRepository.existsByContestIdAndUserId(100L, 1L)).thenReturn(true);
            when(rateLimiter.allowSubmission(1L)).thenReturn(false);
            when(rateLimiter.getRetryAfterSeconds(1L)).thenReturn(30L);

            String requestBody = """
                {
                    "problemId": 10,
                    "code": "public class Solution { }",
                    "language": "JAVA"
                }
                """;

            // Act & Assert
            mvc.perform(post("/api/contests/private/1/submit")
                    .with(user(testUserDetails))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(header().string("Retry-After", "30"))
                    .andExpect(jsonPath("$.message").value("Too many submissions. Try again in 30s"));

            verify(submissionService, never()).submitCodeAsync(anyLong(), anyLong(), anyString(), any(Submission.ProgrammingLanguage.class));
        }

        @Test
        @WithMockUser(username = "participant_user", roles = {"USER"})
        @DisplayName("Should return 429 when submit limit reached (5/5)")
        void testSubmitCode_SubmitLimitReached() throws Exception {
            // Arrange
            when(privateContestRepository.findById(1L)).thenReturn(Optional.of(privateContest));
            when(participantRepository.existsByContestIdAndUserId(100L, 1L)).thenReturn(true);
            when(rateLimiter.allowSubmission(1L)).thenReturn(true);
            when(submissionService.countContestSubmits(1L, 10L)).thenReturn(5L);

            String requestBody = """
                {
                    "problemId": 10,
                    "code": "public class Solution { }",
                    "language": "JAVA"
                }
                """;

            // Act & Assert
            mvc.perform(post("/api/contests/private/1/submit")
                    .with(user(testUserDetails))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.message").value("Submit limit reached (5/5). No more submissions allowed for this problem."));

            verify(submissionService, never()).submitCodeAsync(anyLong(), anyLong(), anyString(), any(Submission.ProgrammingLanguage.class));
        }

        @Test
        @WithMockUser(username = "participant_user", roles = {"USER"})
        @DisplayName("Should return 404 when contest not found")
        void testSubmitCode_ContestNotFound() throws Exception {
            // Arrange
            when(privateContestRepository.findById(999L)).thenReturn(Optional.empty());

            String requestBody = """
                {
                    "problemId": 10,
                    "code": "public class Solution { }",
                    "language": "JAVA"
                }
                """;

            // Act & Assert
            mvc.perform(post("/api/contests/private/999/submit")
                    .with(user(testUserDetails))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Private contest not found"));

            verify(submissionService, never()).submitCodeAsync(anyLong(), anyLong(), anyString(), any(Submission.ProgrammingLanguage.class));
        }
    }

    @Nested
    @DisplayName("GET /api/contests/private/{id}/submissions")
    class GetContestSubmissions {

        @Test
        @WithMockUser(username = "host_user", roles = {"USER"})
        @DisplayName("Should return submissions for contest host")
        void testGetSubmissions_Success() throws Exception {
            // Arrange
            UserDetailsImpl hostUserDetails = UserDetailsImpl.build(hostUser);

            Submission sub1 = new Submission();
            sub1.setId(1L);
            sub1.setUser(testUser);
            sub1.setProblem(problem);
            sub1.setStatus(Submission.SubmissionStatus.AC);
            sub1.setSubmittedAt(LocalDateTime.now());

            Submission sub2 = new Submission();
            sub2.setId(2L);
            sub2.setUser(testUser);
            sub2.setProblem(problem);
            sub2.setStatus(Submission.SubmissionStatus.WA);
            sub2.setSubmittedAt(LocalDateTime.now().minusMinutes(5));

            List<Submission> submissions = Arrays.asList(sub1, sub2);
            Page<Submission> page = new PageImpl<>(submissions);

            when(privateContestRepository.findById(1L)).thenReturn(Optional.of(privateContest));
            when(submissionRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(page);

            // Act & Assert
            mvc.perform(get("/api/contests/private/1/submissions")
                    .with(user(hostUserDetails)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.totalElements").value(2));

            verify(submissionRepository).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        @WithMockUser(username = "participant_user", roles = {"USER"})
        @DisplayName("Should return 403 when user is not the host")
        void testGetSubmissions_NotHost() throws Exception {
            // Arrange
            when(privateContestRepository.findById(1L)).thenReturn(Optional.of(privateContest));

            // Act & Assert
            mvc.perform(get("/api/contests/private/1/submissions")
                    .with(user(testUserDetails)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("Only the contest host can view all submissions"));

            verify(submissionRepository, never()).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        @WithMockUser(username = "host_user", roles = {"USER"})
        @DisplayName("Should filter submissions by userId")
        void testGetSubmissions_FilterByUserId() throws Exception {
            // Arrange
            UserDetailsImpl hostUserDetails = UserDetailsImpl.build(hostUser);

            Submission sub1 = new Submission();
            sub1.setId(1L);
            sub1.setUser(testUser);
            sub1.setProblem(problem);
            sub1.setStatus(Submission.SubmissionStatus.AC);

            List<Submission> submissions = Arrays.asList(sub1);
            Page<Submission> page = new PageImpl<>(submissions);

            when(privateContestRepository.findById(1L)).thenReturn(Optional.of(privateContest));
            when(submissionRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(page);

            // Act & Assert
            mvc.perform(get("/api/contests/private/1/submissions")
                    .param("userId", "1")
                    .with(user(hostUserDetails)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.totalElements").value(1));

            verify(submissionRepository).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        @WithMockUser(username = "host_user", roles = {"USER"})
        @DisplayName("Should filter submissions by problemId")
        void testGetSubmissions_FilterByProblemId() throws Exception {
            // Arrange
            UserDetailsImpl hostUserDetails = UserDetailsImpl.build(hostUser);

            Submission sub1 = new Submission();
            sub1.setId(1L);
            sub1.setUser(testUser);
            sub1.setProblem(problem);
            sub1.setStatus(Submission.SubmissionStatus.AC);

            List<Submission> submissions = Arrays.asList(sub1);
            Page<Submission> page = new PageImpl<>(submissions);

            when(privateContestRepository.findById(1L)).thenReturn(Optional.of(privateContest));
            when(submissionRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(page);

            // Act & Assert
            mvc.perform(get("/api/contests/private/1/submissions")
                    .param("problemId", "10")
                    .with(user(hostUserDetails)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.totalElements").value(1));

            verify(submissionRepository).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        @WithMockUser(username = "host_user", roles = {"USER"})
        @DisplayName("Should filter submissions by status")
        void testGetSubmissions_FilterByStatus() throws Exception {
            // Arrange
            UserDetailsImpl hostUserDetails = UserDetailsImpl.build(hostUser);

            Submission sub1 = new Submission();
            sub1.setId(1L);
            sub1.setUser(testUser);
            sub1.setProblem(problem);
            sub1.setStatus(Submission.SubmissionStatus.AC);

            List<Submission> submissions = Arrays.asList(sub1);
            Page<Submission> page = new PageImpl<>(submissions);

            when(privateContestRepository.findById(1L)).thenReturn(Optional.of(privateContest));
            when(submissionRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(page);

            // Act & Assert
            mvc.perform(get("/api/contests/private/1/submissions")
                    .param("status", "AC")
                    .with(user(hostUserDetails)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.totalElements").value(1));

            verify(submissionRepository).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        @WithMockUser(username = "host_user", roles = {"USER"})
        @DisplayName("Should return 404 when contest not found")
        void testGetSubmissions_ContestNotFound() throws Exception {
            // Arrange
            UserDetailsImpl hostUserDetails = UserDetailsImpl.build(hostUser);
            when(privateContestRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            mvc.perform(get("/api/contests/private/999/submissions")
                    .with(user(hostUserDetails)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Private contest not found"));

            verify(submissionRepository, never()).findAll(any(Specification.class), any(Pageable.class));
        }
    }
}
