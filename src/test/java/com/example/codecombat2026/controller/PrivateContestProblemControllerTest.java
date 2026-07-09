package com.example.codecombat2026.controller;

import com.example.codecombat2026.dto.ProblemDTO;
import com.example.codecombat2026.entity.Contest;
import com.example.codecombat2026.entity.ContestProblem;
import com.example.codecombat2026.entity.Problem;
import com.example.codecombat2026.exception.BadRequestException;
import com.example.codecombat2026.exception.ConflictException;
import com.example.codecombat2026.exception.ForbiddenException;
import com.example.codecombat2026.exception.GlobalExceptionHandler;
import com.example.codecombat2026.exception.ResourceNotFoundException;
import com.example.codecombat2026.exception.TooManyRequestsException;
import com.example.codecombat2026.repository.ProblemRepository;
import com.example.codecombat2026.security.SecurityConfig;
import com.example.codecombat2026.security.jwt.AuthEntryPointJwt;
import com.example.codecombat2026.security.jwt.AuthTokenFilter;
import com.example.codecombat2026.security.jwt.JwtUtils;
import com.example.codecombat2026.security.services.UserDetailsImpl;
import com.example.codecombat2026.security.services.UserDetailsServiceImpl;
import com.example.codecombat2026.service.AIProblemGeneratorService;
import com.example.codecombat2026.service.ContestProblemService;
import com.example.codecombat2026.service.PrivateContestAccessValidator;
import com.example.codecombat2026.service.RateLimiterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive unit tests for PrivateContestProblemController using @WebMvcTest.
 * 
 * Tests cover all endpoints with access control validation:
 * - GET /api/contests/private/{id}/problems/available - browse problems
 * - POST /api/contests/private/{id}/problems - attach problems
 * - DELETE /api/contests/private/{id}/problems/{problemId} - remove problem
 * - POST /api/contests/private/{id}/problems/generate - AI generate
 * - PUT /api/problems/{id} - edit problem
 * 
 * Access control scenarios:
 * - Contest host authorization
 * - Owner/admin authorization for editing
 * - Rate limiting for AI generation
 * - Validation errors
 * 
 * Requirements: 8.1, 8.4, 9.1, 10.1
 */
@WebMvcTest(controllers = PrivateContestProblemController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
    "codecombat.jwt.secret=dGhpcy1pcy1hLXRlc3Qtc2VjcmV0LXBhZGRlZC10by0zMi1ieXRlcw==",
    "codecombat.jwt.expiration=86400000",
    "APP_ALLOWED_ORIGINS=http://localhost:5173"
})
class PrivateContestProblemControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProblemRepository problemRepository;

    @MockBean
    private ContestProblemService contestProblemService;

    @MockBean
    private AIProblemGeneratorService aiProblemGeneratorService;

    @MockBean
    private PrivateContestAccessValidator accessValidator;

    @MockBean
    private RateLimiterService rateLimiterService;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private AuthEntryPointJwt entryPoint;

    @MockBean
    private AuthTokenFilter authTokenFilter;

    @MockBean
    private JwtUtils jwtUtils;

    private UserDetailsImpl hostUser;
    private UserDetailsImpl nonHostUser;
    private UserDetailsImpl adminUser;

    @BeforeEach
    void setUp() throws Exception {
        // Create test users
        hostUser = new UserDetailsImpl(42L, "host", "host@example.com", "password", 
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        nonHostUser = new UserDetailsImpl(99L, "user", "user@example.com", "password", 
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        adminUser = new UserDetailsImpl(1L, "admin", "admin@example.com", "password", 
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));

        // Make auth filter pass-through
        doAnswer((InvocationOnMock inv) -> {
            ServletRequest req = inv.getArgument(0);
            ServletResponse res = inv.getArgument(1);
            FilterChain chain = inv.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(authTokenFilter).doFilter(any(), any(), any());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GET /api/contests/private/{id}/problems/available - Browse Problems
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/contests/private/{contestId}/problems/available")
    class BrowseAvailableProblems {

        @Test
        @DisplayName("Should return available problems for contest host")
        @WithMockUser
        void browseProblems_Success() throws Exception {
            Long contestId = 100L;

            // Setup
            when(accessValidator.isHost(contestId, hostUser.getId())).thenReturn(true);

            Problem problem1 = new Problem();
            problem1.setId(1L);
            problem1.setTitle("Two Sum");
            problem1.setLevel("EASY");
            problem1.setVisibility("PUBLIC");

            Problem problem2 = new Problem();
            problem2.setId(2L);
            problem2.setTitle("Binary Tree");
            problem2.setLevel("MEDIUM");
            problem2.setVisibility("PRIVATE_AVAILABLE");

            when(problemRepository.findAvailableForContest(eq(contestId), isNull(), isNull()))
                .thenReturn(Arrays.asList(problem1, problem2));

            // Execute & Verify
            mvc.perform(get("/api/contests/private/{contestId}/problems/available", contestId)
                    .with(user(hostUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].title", is("Two Sum")))
                .andExpect(jsonPath("$[0].level", is("EASY")))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].title", is("Binary Tree")))
                .andExpect(jsonPath("$[1].level", is("MEDIUM")));

            verify(accessValidator).isHost(contestId, hostUser.getId());
            verify(problemRepository).findAvailableForContest(contestId, null, null);
        }

        @Test
        @DisplayName("Should filter by difficulty")
        @WithMockUser
        void browseProblems_WithDifficultyFilter() throws Exception {
            Long contestId = 100L;
            when(accessValidator.isHost(contestId, hostUser.getId())).thenReturn(true);

            Problem problem = new Problem();
            problem.setId(1L);
            problem.setTitle("Two Sum");
            problem.setLevel("EASY");
            problem.setVisibility("PUBLIC");

            when(problemRepository.findAvailableForContest(eq(contestId), isNull(), eq("EASY")))
                .thenReturn(Collections.singletonList(problem));

            mvc.perform(get("/api/contests/private/{contestId}/problems/available", contestId)
                    .param("difficulty", "EASY")
                    .with(user(hostUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].level", is("EASY")));

            verify(problemRepository).findAvailableForContest(contestId, null, "EASY");
        }

        @Test
        @DisplayName("Should filter by search query")
        @WithMockUser
        void browseProblems_WithSearchFilter() throws Exception {
            Long contestId = 100L;
            when(accessValidator.isHost(contestId, hostUser.getId())).thenReturn(true);

            Problem problem = new Problem();
            problem.setId(1L);
            problem.setTitle("Two Sum");
            problem.setVisibility("PUBLIC");

            when(problemRepository.findAvailableForContest(eq(contestId), eq("sum"), isNull()))
                .thenReturn(Collections.singletonList(problem));

            mvc.perform(get("/api/contests/private/{contestId}/problems/available", contestId)
                    .param("search", "sum")
                    .with(user(hostUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Two Sum")));

            verify(problemRepository).findAvailableForContest(contestId, "sum", null);
        }

        @Test
        @DisplayName("Should return 403 when user is not the host")
        @WithMockUser
        void browseProblems_NotHost_Forbidden() throws Exception {
            Long contestId = 100L;
            when(accessValidator.isHost(contestId, nonHostUser.getId())).thenReturn(false);

            mvc.perform(get("/api/contests/private/{contestId}/problems/available", contestId)
                    .with(user(nonHostUser)))
                .andExpect(status().isForbidden());

            verify(accessValidator).isHost(contestId, nonHostUser.getId());
            verifyNoInteractions(problemRepository);
        }

        @Test
        @DisplayName("Should return 400 for invalid difficulty")
        @WithMockUser
        void browseProblems_InvalidDifficulty_BadRequest() throws Exception {
            Long contestId = 100L;
            when(accessValidator.isHost(contestId, hostUser.getId())).thenReturn(true);

            mvc.perform(get("/api/contests/private/{contestId}/problems/available", contestId)
                    .param("difficulty", "INVALID")
                    .with(user(hostUser)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should filter out ADMIN_ONLY and PRIVATE_OWNED problems")
        @WithMockUser
        void browseProblems_FiltersVisibility() throws Exception {
            Long contestId = 100L;
            when(accessValidator.isHost(contestId, hostUser.getId())).thenReturn(true);

            Problem publicProblem = new Problem();
            publicProblem.setId(1L);
            publicProblem.setTitle("Public Problem");
            publicProblem.setVisibility("PUBLIC");

            Problem adminProblem = new Problem();
            adminProblem.setId(2L);
            adminProblem.setTitle("Admin Problem");
            adminProblem.setVisibility("ADMIN_ONLY");

            Problem privateProblem = new Problem();
            privateProblem.setId(3L);
            privateProblem.setTitle("Private Owned Problem");
            privateProblem.setVisibility("PRIVATE_OWNED");

            when(problemRepository.findAvailableForContest(eq(contestId), isNull(), isNull()))
                .thenReturn(Arrays.asList(publicProblem, adminProblem, privateProblem));

            // Should only return PUBLIC (ADMIN_ONLY and PRIVATE_OWNED filtered out)
            mvc.perform(get("/api/contests/private/{contestId}/problems/available", contestId)
                    .with(user(hostUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // POST /api/contests/private/{id}/problems - Attach Problems
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/contests/private/{contestId}/problems")
    class AttachProblems {

        @Test
        @DisplayName("Should attach problems successfully")
        @WithMockUser
        void attachProblems_Success() throws Exception {
            Long contestId = 100L;
            List<Long> problemIds = Arrays.asList(1L, 2L, 3L);

            when(accessValidator.isHost(contestId, hostUser.getId())).thenReturn(true);

            List<ContestProblem> attachedProblems = new ArrayList<>();
            for (Long problemId : problemIds) {
                ContestProblem cp = new ContestProblem();
                cp.setContestId(contestId);
                cp.setProblemId(problemId);
                attachedProblems.add(cp);
            }
            when(contestProblemService.attachMany(contestId, problemIds)).thenReturn(attachedProblems);

            String requestBody = """
                {
                    "problemIds": [1, 2, 3]
                }
                """;

            mvc.perform(post("/api/contests/private/{contestId}/problems", contestId)
                    .with(user(hostUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attachedCount", is(3)))
                .andExpect(jsonPath("$.message", containsString("Successfully attached")));

            verify(accessValidator).isHost(contestId, hostUser.getId());
            verify(contestProblemService).attachMany(contestId, problemIds);
        }

        @Test
        @DisplayName("Should return 403 when user is not the host")
        @WithMockUser
        void attachProblems_NotHost_Forbidden() throws Exception {
            Long contestId = 100L;
            when(accessValidator.isHost(contestId, nonHostUser.getId())).thenReturn(false);

            String requestBody = """
                {
                    "problemIds": [1, 2, 3]
                }
                """;

            mvc.perform(post("/api/contests/private/{contestId}/problems", contestId)
                    .with(user(nonHostUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isForbidden());

            verify(accessValidator).isHost(contestId, nonHostUser.getId());
            verifyNoInteractions(contestProblemService);
        }

        @Test
        @DisplayName("Should return 400 when problemIds is missing")
        @WithMockUser
        void attachProblems_MissingProblemIds_BadRequest() throws Exception {
            Long contestId = 100L;
            when(accessValidator.isHost(contestId, hostUser.getId())).thenReturn(true);

            String requestBody = "{}";

            mvc.perform(post("/api/contests/private/{contestId}/problems", contestId)
                    .with(user(hostUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when problemIds is empty")
        @WithMockUser
        void attachProblems_EmptyProblemIds_BadRequest() throws Exception {
            Long contestId = 100L;
            when(accessValidator.isHost(contestId, hostUser.getId())).thenReturn(true);

            String requestBody = """
                {
                    "problemIds": []
                }
                """;

            mvc.perform(post("/api/contests/private/{contestId}/problems", contestId)
                    .with(user(hostUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isBadRequest());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DELETE /api/contests/private/{id}/problems/{problemId} - Remove Problem
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("DELETE /api/contests/private/{contestId}/problems/{problemId}")
    class RemoveProblem {

        @Test
        @DisplayName("Should remove problem successfully")
        @WithMockUser
        void removeProblem_Success() throws Exception {
            Long contestId = 100L;
            Long problemId = 1L;

            when(accessValidator.isHost(contestId, hostUser.getId())).thenReturn(true);
            doNothing().when(contestProblemService).detach(contestId, problemId);

            mvc.perform(delete("/api/contests/private/{contestId}/problems/{problemId}", contestId, problemId)
                    .with(user(hostUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Problem removed from contest successfully")));

            verify(accessValidator).isHost(contestId, hostUser.getId());
            verify(contestProblemService).detach(contestId, problemId);
        }

        @Test
        @DisplayName("Should return 403 when user is not the host")
        @WithMockUser
        void removeProblem_NotHost_Forbidden() throws Exception {
            Long contestId = 100L;
            Long problemId = 1L;

            when(accessValidator.isHost(contestId, nonHostUser.getId())).thenReturn(false);

            mvc.perform(delete("/api/contests/private/{contestId}/problems/{problemId}", contestId, problemId)
                    .with(user(nonHostUser)))
                .andExpect(status().isForbidden());

            verify(accessValidator).isHost(contestId, nonHostUser.getId());
            verifyNoInteractions(contestProblemService);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // POST /api/contests/private/{id}/problems/generate - AI Generate Problem
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/contests/private/{contestId}/problems/generate")
    class GenerateProblem {

        @Test
        @DisplayName("Should generate problem successfully")
        @WithMockUser
        void generateProblem_Success() throws Exception {
            Long contestId = 100L;
            when(accessValidator.isHost(contestId, hostUser.getId())).thenReturn(true);

            Problem generatedProblem = new Problem();
            generatedProblem.setId(123L);
            generatedProblem.setTitle("Generated Problem");
            generatedProblem.setLevel("MEDIUM");
            generatedProblem.setVisibility("PRIVATE_OWNED");
            generatedProblem.setCreatedBy(hostUser.getId());

            when(aiProblemGeneratorService.generateProblem(
                    eq("Create a problem about sorting"),
                    eq("MEDIUM"),
                    eq("Algorithms"),
                    eq(hostUser.getId())))
                .thenReturn(generatedProblem);

            String requestBody = """
                {
                    "prompt": "Create a problem about sorting",
                    "difficulty": "MEDIUM",
                    "topic": "Algorithms"
                }
                """;

            mvc.perform(post("/api/contests/private/{contestId}/problems/generate", contestId)
                    .with(user(hostUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(123)))
                .andExpect(jsonPath("$.title", is("Generated Problem")))
                .andExpect(jsonPath("$.level", is("MEDIUM")))
                .andExpect(jsonPath("$.visibility", is("PRIVATE_OWNED")))
                .andExpect(jsonPath("$.createdBy", is(42)));

            verify(accessValidator).isHost(contestId, hostUser.getId());
            verify(aiProblemGeneratorService).generateProblem(
                    "Create a problem about sorting", "MEDIUM", "Algorithms", hostUser.getId());
        }

        @Test
        @DisplayName("Should return 403 when user is not the host")
        @WithMockUser
        void generateProblem_NotHost_Forbidden() throws Exception {
            Long contestId = 100L;
            when(accessValidator.isHost(contestId, nonHostUser.getId())).thenReturn(false);

            String requestBody = """
                {
                    "prompt": "Create a problem",
                    "difficulty": "MEDIUM"
                }
                """;

            mvc.perform(post("/api/contests/private/{contestId}/problems/generate", contestId)
                    .with(user(nonHostUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isForbidden());

            verifyNoInteractions(aiProblemGeneratorService);
        }

        @Test
        @DisplayName("Should return 400 when prompt is missing")
        @WithMockUser
        void generateProblem_MissingPrompt_BadRequest() throws Exception {
            Long contestId = 100L;
            when(accessValidator.isHost(contestId, hostUser.getId())).thenReturn(true);

            String requestBody = """
                {
                    "difficulty": "MEDIUM"
                }
                """;

            mvc.perform(post("/api/contests/private/{contestId}/problems/generate", contestId)
                    .with(user(hostUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when prompt exceeds 1000 characters")
        @WithMockUser
        void generateProblem_PromptTooLong_BadRequest() throws Exception {
            Long contestId = 100L;
            when(accessValidator.isHost(contestId, hostUser.getId())).thenReturn(true);

            String longPrompt = "a".repeat(1001);
            String requestBody = String.format("""
                {
                    "prompt": "%s",
                    "difficulty": "MEDIUM"
                }
                """, longPrompt);

            mvc.perform(post("/api/contests/private/{contestId}/problems/generate", contestId)
                    .with(user(hostUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 429 when rate limit exceeded")
        @WithMockUser
        void generateProblem_RateLimitExceeded() throws Exception {
            Long contestId = 100L;
            when(accessValidator.isHost(contestId, hostUser.getId())).thenReturn(true);

            when(aiProblemGeneratorService.generateProblem(anyString(), anyString(), anyString(), anyLong()))
                .thenThrow(new TooManyRequestsException("You have reached your daily limit of 5 AI-generated problems"));

            String requestBody = """
                {
                    "prompt": "Create a problem",
                    "difficulty": "MEDIUM"
                }
                """;

            mvc.perform(post("/api/contests/private/{contestId}/problems/generate", contestId)
                    .with(user(hostUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isTooManyRequests());
        }

        @Test
        @DisplayName("Should return 400 when topic exceeds 100 characters")
        @WithMockUser
        void generateProblem_TopicTooLong_BadRequest() throws Exception {
            Long contestId = 100L;
            when(accessValidator.isHost(contestId, hostUser.getId())).thenReturn(true);

            String longTopic = "a".repeat(101);
            String requestBody = String.format("""
                {
                    "prompt": "Create a problem",
                    "difficulty": "MEDIUM",
                    "topic": "%s"
                }
                """, longTopic);

            mvc.perform(post("/api/contests/private/{contestId}/problems/generate", contestId)
                    .with(user(hostUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isBadRequest());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PUT /api/problems/{id} - Edit Problem
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("PUT /api/problems/{problemId}")
    class EditProblem {

        @Test
        @DisplayName("Should allow owner to edit PRIVATE_OWNED problem")
        @WithMockUser
        void editProblem_Owner_Success() throws Exception {
            Long problemId = 1L;

            Problem problem = new Problem();
            problem.setId(problemId);
            problem.setTitle("Old Title");
            problem.setDescription("Old Description");
            problem.setLevel("EASY");
            problem.setVisibility("PRIVATE_OWNED");
            problem.setCreatedBy(hostUser.getId());
            problem.setContests(new ArrayList<>());

            when(problemRepository.findById(problemId)).thenReturn(Optional.of(problem));
            when(problemRepository.save(any(Problem.class))).thenAnswer(inv -> inv.getArgument(0));

            String requestBody = """
                {
                    "title": "New Title",
                    "description": "New Description",
                    "level": "MEDIUM"
                }
                """;

            mvc.perform(put("/api/problems/{problemId}", problemId)
                    .with(user(hostUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("New Title")))
                .andExpect(jsonPath("$.description", is("New Description")))
                .andExpect(jsonPath("$.level", is("MEDIUM")));

            verify(problemRepository).findById(problemId);
            verify(problemRepository).save(any(Problem.class));
        }

        @Test
        @DisplayName("Should allow admin to edit any problem")
        @WithMockUser
        void editProblem_Admin_Success() throws Exception {
            Long problemId = 1L;

            Problem problem = new Problem();
            problem.setId(problemId);
            problem.setTitle("Old Title");
            problem.setVisibility("PUBLIC");
            problem.setCreatedBy(99L); // Different owner
            problem.setContests(new ArrayList<>());

            when(problemRepository.findById(problemId)).thenReturn(Optional.of(problem));
            when(problemRepository.save(any(Problem.class))).thenAnswer(inv -> inv.getArgument(0));

            String requestBody = """
                {
                    "title": "Updated by Admin"
                }
                """;

            mvc.perform(put("/api/problems/{problemId}", problemId)
                    .with(user(adminUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Updated by Admin")));

            verify(problemRepository).save(any(Problem.class));
        }

        @Test
        @DisplayName("Should return 403 when non-owner tries to edit PRIVATE_OWNED problem")
        @WithMockUser
        void editProblem_NonOwner_Forbidden() throws Exception {
            Long problemId = 1L;

            Problem problem = new Problem();
            problem.setId(problemId);
            problem.setVisibility("PRIVATE_OWNED");
            problem.setCreatedBy(99L); // Different owner

            when(problemRepository.findById(problemId)).thenReturn(Optional.of(problem));

            String requestBody = """
                {
                    "title": "Trying to edit"
                }
                """;

            mvc.perform(put("/api/problems/{problemId}", problemId)
                    .with(user(nonHostUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isForbidden());

            verify(problemRepository).findById(problemId);
            verify(problemRepository, never()).save(any(Problem.class));
        }

        @Test
        @DisplayName("Should return 409 when problem is attached to LIVE contest")
        @WithMockUser
        void editProblem_AttachedToLiveContest_Conflict() throws Exception {
            Long problemId = 1L;

            Contest liveContest = new Contest();
            liveContest.setId(100L);
            liveContest.setStatus(Contest.ContestStatus.LIVE);

            Problem problem = new Problem();
            problem.setId(problemId);
            problem.setVisibility("PRIVATE_OWNED");
            problem.setCreatedBy(hostUser.getId());
            problem.setContests(Collections.singletonList(liveContest));

            when(problemRepository.findById(problemId)).thenReturn(Optional.of(problem));

            String requestBody = """
                {
                    "title": "Trying to edit"
                }
                """;

            mvc.perform(put("/api/problems/{problemId}", problemId)
                    .with(user(hostUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isConflict());

            verify(problemRepository).findById(problemId);
            verify(problemRepository, never()).save(any(Problem.class));
        }

        @Test
        @DisplayName("Should return 404 when problem doesn't exist")
        @WithMockUser
        void editProblem_NotFound() throws Exception {
            Long problemId = 999L;

            when(problemRepository.findById(problemId)).thenReturn(Optional.empty());

            String requestBody = """
                {
                    "title": "Trying to edit"
                }
                """;

            mvc.perform(put("/api/problems/{problemId}", problemId)
                    .with(user(hostUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isNotFound());

            verify(problemRepository).findById(problemId);
            verify(problemRepository, never()).save(any(Problem.class));
        }

        @Test
        @DisplayName("Should update multiple fields")
        @WithMockUser
        void editProblem_MultipleFields() throws Exception {
            Long problemId = 1L;

            Problem problem = new Problem();
            problem.setId(problemId);
            problem.setTitle("Old Title");
            problem.setDescription("Old Description");
            problem.setInputFormat("Old Input");
            problem.setOutputFormat("Old Output");
            problem.setConstraints("Old Constraints");
            problem.setTimeLimit(2.0);
            problem.setMemoryLimit(128);
            problem.setLevel("EASY");
            problem.setVisibility("PRIVATE_OWNED");
            problem.setCreatedBy(hostUser.getId());
            problem.setContests(new ArrayList<>());

            when(problemRepository.findById(problemId)).thenReturn(Optional.of(problem));
            when(problemRepository.save(any(Problem.class))).thenAnswer(inv -> inv.getArgument(0));

            String requestBody = """
                {
                    "title": "New Title",
                    "description": "New Description",
                    "inputFormat": "New Input",
                    "outputFormat": "New Output",
                    "constraints": "New Constraints",
                    "timeLimit": 5.0,
                    "memoryLimit": 256,
                    "level": "HARD"
                }
                """;

            mvc.perform(put("/api/problems/{problemId}", problemId)
                    .with(user(hostUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("New Title")))
                .andExpect(jsonPath("$.description", is("New Description")))
                .andExpect(jsonPath("$.inputFormat", is("New Input")))
                .andExpect(jsonPath("$.outputFormat", is("New Output")))
                .andExpect(jsonPath("$.constraints", is("New Constraints")))
                .andExpect(jsonPath("$.timeLimit", is(5.0)))
                .andExpect(jsonPath("$.memoryLimit", is(256)))
                .andExpect(jsonPath("$.level", is("HARD")));

            verify(problemRepository).save(any(Problem.class));
        }

        @Test
        @DisplayName("Should return 400 for invalid difficulty level")
        @WithMockUser
        void editProblem_InvalidDifficulty_BadRequest() throws Exception {
            Long problemId = 1L;

            Problem problem = new Problem();
            problem.setId(problemId);
            problem.setVisibility("PRIVATE_OWNED");
            problem.setCreatedBy(hostUser.getId());
            problem.setContests(new ArrayList<>());

            when(problemRepository.findById(problemId)).thenReturn(Optional.of(problem));

            String requestBody = """
                {
                    "level": "INVALID"
                }
                """;

            mvc.perform(put("/api/problems/{problemId}", problemId)
                    .with(user(hostUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isBadRequest());

            verify(problemRepository, never()).save(any(Problem.class));
        }
    }
}
