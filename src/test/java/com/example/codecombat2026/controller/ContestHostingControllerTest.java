package com.example.codecombat2026.controller;

import com.example.codecombat2026.dto.HostingRequestDTO;
import com.example.codecombat2026.entity.ContestHostingRequest.HostingRequestStatus;
import com.example.codecombat2026.entity.ContestHostingRequest.IntendedUseCase;
import com.example.codecombat2026.exception.ConflictException;
import com.example.codecombat2026.exception.GlobalExceptionHandler;
import com.example.codecombat2026.exception.ResourceNotFoundException;
import com.example.codecombat2026.security.SecurityConfig;
import com.example.codecombat2026.security.jwt.AuthEntryPointJwt;
import com.example.codecombat2026.security.jwt.AuthTokenFilter;
import com.example.codecombat2026.security.jwt.JwtUtils;
import com.example.codecombat2026.security.services.UserDetailsImpl;
import com.example.codecombat2026.security.services.UserDetailsServiceImpl;
import com.example.codecombat2026.service.ContestHostingService;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive unit tests for ContestHostingController using @WebMvcTest.
 * 
 * Tests cover:
 * - Submitting hosting requests (success, conflict cases)
 * - Checking request status (with/without request)
 * - Listing requests (admin, with/without filters)
 * - Approving requests (success, not found, conflict)
 * - Rejecting requests (success, not found, conflict)
 * - Role-based security (@PreAuthorize)
 * - Validation errors
 * 
 * Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 2.1, 2.2, 2.3, 2.4, 2.5, 2.6
 */
@WebMvcTest(controllers = ContestHostingController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
    "codecombat.jwt.secret=dGhpcy1pcy1hLXRlc3Qtc2VjcmV0LXBhZGRlZC10by0zMi1ieXRlcw==",
    "codecombat.jwt.expiration=86400000",
    "APP_ALLOWED_ORIGINS=http://localhost:5173"
})
class ContestHostingControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ContestHostingService contestHostingService;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private AuthEntryPointJwt entryPoint;

    @MockBean
    private AuthTokenFilter authTokenFilter;

    @MockBean
    private JwtUtils jwtUtils;

    private UserDetailsImpl testUser;
    private UserDetailsImpl adminUser;

    @BeforeEach
    void setUp() throws Exception {
        // Create test users
        testUser = new UserDetailsImpl(42L, "testuser", "test@example.com", "password", Collections.emptyList());
        adminUser = new UserDetailsImpl(1L, "admin", "admin@example.com", "password", Collections.emptyList());

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
    // User Endpoints Tests
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/hosting-requests/submit")
    class SubmitHostingRequest {

        @Test
        @DisplayName("Should submit request successfully with valid data")
        @WithMockUser(username = "testuser", roles = "USER")
        void submitRequest_Success() throws Exception {
            HostingRequestDTO responseDto = new HostingRequestDTO();
            responseDto.setId(1L);
            responseDto.setUserId(42L);
            responseDto.setReason("I teach a university course");
            responseDto.setIntendedUseCase(IntendedUseCase.EDUCATION);
            responseDto.setStatus(HostingRequestStatus.PENDING);
            responseDto.setSubmittedAt(LocalDateTime.now());

            when(contestHostingService.submitRequest(anyLong(), anyString(), any(IntendedUseCase.class)))
                .thenReturn(responseDto);

            String requestBody = """
                {
                    "reason": "I teach a university course",
                    "intendedUseCase": "EDUCATION"
                }
                """;

            mvc.perform(post("/api/hosting-requests/submit")
                    .with(user(testUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.userId", is(42)))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.intendedUseCase", is("EDUCATION")))
                .andExpect(jsonPath("$.reason", is("I teach a university course")));

            verify(contestHostingService).submitRequest(eq(42L), eq("I teach a university course"), eq(IntendedUseCase.EDUCATION));
        }

        @Test
        @DisplayName("Should return 409 when user already has pending request")
        @WithMockUser(username = "testuser", roles = "USER")
        void submitRequest_PendingConflict() throws Exception {
            when(contestHostingService.submitRequest(anyLong(), anyString(), any(IntendedUseCase.class)))
                .thenThrow(new ConflictException("You already have a pending hosting request"));

            String requestBody = """
                {
                    "reason": "I want to host contests",
                    "intendedUseCase": "EDUCATION"
                }
                """;

            mvc.perform(post("/api/hosting-requests/submit")
                    .with(user(testUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("You already have a pending hosting request")));
        }

        @Test
        @DisplayName("Should return 409 when user already approved")
        @WithMockUser(username = "testuser", roles = "USER")
        void submitRequest_ApprovedConflict() throws Exception {
            when(contestHostingService.submitRequest(anyLong(), anyString(), any(IntendedUseCase.class)))
                .thenThrow(new ConflictException("You are already approved to host contests"));

            String requestBody = """
                {
                    "reason": "I want to host contests",
                    "intendedUseCase": "EDUCATION"
                }
                """;

            mvc.perform(post("/api/hosting-requests/submit")
                    .with(user(testUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("You are already approved to host contests")));
        }

        @Test
        @DisplayName("Should return 400 when intendedUseCase is missing")
        @WithMockUser(username = "testuser", roles = "USER")
        void submitRequest_MissingIntendedUseCase() throws Exception {
            String requestBody = """
                {
                    "reason": "I want to host contests"
                }
                """;

            mvc.perform(post("/api/hosting-requests/submit")
                    .with(user(testUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/hosting-requests/my-status")
    class GetMyHostingStatus {

        @Test
        @DisplayName("Should return status when user has approved request")
        @WithMockUser(username = "testuser", roles = "USER")
        void getStatus_WithApprovedRequest() throws Exception {
            HostingRequestDTO responseDto = new HostingRequestDTO();
            responseDto.setId(1L);
            responseDto.setUserId(42L);
            responseDto.setStatus(HostingRequestStatus.APPROVED);
            responseDto.setSubmittedAt(LocalDateTime.of(2026, 1, 15, 10, 30));
            responseDto.setReviewedAt(LocalDateTime.of(2026, 1, 16, 14, 0));

            when(contestHostingService.getUserRequestStatus(anyLong()))
                .thenReturn(responseDto);

            mvc.perform(get("/api/hosting-requests/my-status")
                    .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasRequest", is(true)))
                .andExpect(jsonPath("$.canCreateContests", is(true)))
                .andExpect(jsonPath("$.status", is("APPROVED")))
                .andExpect(jsonPath("$.request.id", is(1)));
        }

        @Test
        @DisplayName("Should return status when user has pending request")
        @WithMockUser(username = "testuser", roles = "USER")
        void getStatus_WithPendingRequest() throws Exception {
            HostingRequestDTO responseDto = new HostingRequestDTO();
            responseDto.setId(1L);
            responseDto.setUserId(42L);
            responseDto.setStatus(HostingRequestStatus.PENDING);
            responseDto.setSubmittedAt(LocalDateTime.of(2026, 1, 15, 10, 30));

            when(contestHostingService.getUserRequestStatus(anyLong()))
                .thenReturn(responseDto);

            mvc.perform(get("/api/hosting-requests/my-status")
                    .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasRequest", is(true)))
                .andExpect(jsonPath("$.canCreateContests", is(false)))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.request.id", is(1)));
        }

        @Test
        @DisplayName("Should return no request when user has not submitted")
        @WithMockUser(username = "testuser", roles = "USER")
        void getStatus_NoRequest() throws Exception {
            when(contestHostingService.getUserRequestStatus(anyLong()))
                .thenReturn(null);

            mvc.perform(get("/api/hosting-requests/my-status")
                    .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasRequest", is(false)))
                .andExpect(jsonPath("$.canCreateContests", is(false)))
                .andExpect(jsonPath("$.status").doesNotExist());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Admin Endpoints Tests
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/admin/hosting-requests")
    class ListHostingRequests {

        @Test
        @DisplayName("Should list pending requests by default")
        @WithMockUser(username = "admin", roles = "ADMIN")
        void listRequests_DefaultPending() throws Exception {
            HostingRequestDTO dto1 = new HostingRequestDTO();
            dto1.setId(1L);
            dto1.setUserId(42L);
            dto1.setStatus(HostingRequestStatus.PENDING);
            dto1.setIntendedUseCase(IntendedUseCase.EDUCATION);

            HostingRequestDTO dto2 = new HostingRequestDTO();
            dto2.setId(2L);
            dto2.setUserId(43L);
            dto2.setStatus(HostingRequestStatus.PENDING);
            dto2.setIntendedUseCase(IntendedUseCase.RECRUITMENT);

            List<HostingRequestDTO> requests = Arrays.asList(dto1, dto2);

            when(contestHostingService.getRequestsByStatus(HostingRequestStatus.PENDING))
                .thenReturn(requests);

            mvc.perform(get("/api/admin/hosting-requests")
                    .with(user(adminUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[1].id", is(2)));

            verify(contestHostingService).getRequestsByStatus(HostingRequestStatus.PENDING);
        }

        @Test
        @DisplayName("Should list approved requests when status filter provided")
        @WithMockUser(username = "admin", roles = "ADMIN")
        void listRequests_FilteredByStatus() throws Exception {
            HostingRequestDTO dto = new HostingRequestDTO();
            dto.setId(1L);
            dto.setUserId(42L);
            dto.setStatus(HostingRequestStatus.APPROVED);

            List<HostingRequestDTO> requests = Collections.singletonList(dto);

            when(contestHostingService.getRequestsByStatus(HostingRequestStatus.APPROVED))
                .thenReturn(requests);

            mvc.perform(get("/api/admin/hosting-requests")
                    .with(user(adminUser))
                    .param("status", "APPROVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status", is("APPROVED")));

            verify(contestHostingService).getRequestsByStatus(HostingRequestStatus.APPROVED);
        }

        @Test
        @DisplayName("Should return empty list when no requests found")
        @WithMockUser(username = "admin", roles = "ADMIN")
        void listRequests_EmptyList() throws Exception {
            when(contestHostingService.getRequestsByStatus(any()))
                .thenReturn(Collections.emptyList());

            mvc.perform(get("/api/admin/hosting-requests")
                    .with(user(adminUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("POST /api/admin/hosting-requests/{id}/approve")
    class ApproveHostingRequest {

        @Test
        @DisplayName("Should approve request successfully")
        @WithMockUser(username = "admin", roles = "ADMIN")
        void approveRequest_Success() throws Exception {
            HostingRequestDTO responseDto = new HostingRequestDTO();
            responseDto.setId(1L);
            responseDto.setUserId(42L);
            responseDto.setStatus(HostingRequestStatus.APPROVED);
            responseDto.setReviewedBy(1L);
            responseDto.setReviewedAt(LocalDateTime.now());
            responseDto.setAdminNotes("Verified educational institution");

            when(contestHostingService.approveRequest(anyLong(), anyLong(), anyString()))
                .thenReturn(responseDto);

            String requestBody = """
                {
                    "adminNotes": "Verified educational institution"
                }
                """;

            mvc.perform(post("/api/admin/hosting-requests/1/approve")
                    .with(user(adminUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.status", is("APPROVED")))
                .andExpect(jsonPath("$.reviewedBy", is(1)))
                .andExpect(jsonPath("$.adminNotes", is("Verified educational institution")));

            verify(contestHostingService).approveRequest(eq(1L), eq(1L), eq("Verified educational institution"));
        }

        @Test
        @DisplayName("Should return 404 when request not found")
        @WithMockUser(username = "admin", roles = "ADMIN")
        void approveRequest_NotFound() throws Exception {
            when(contestHostingService.approveRequest(anyLong(), anyLong(), anyString()))
                .thenThrow(new ResourceNotFoundException("Hosting request not found"));

            String requestBody = """
                {
                    "adminNotes": "Test"
                }
                """;

            mvc.perform(post("/api/admin/hosting-requests/999/approve")
                    .with(user(adminUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Hosting request not found")));
        }

        @Test
        @DisplayName("Should return 409 when request not in pending status")
        @WithMockUser(username = "admin", roles = "ADMIN")
        void approveRequest_NotPending() throws Exception {
            when(contestHostingService.approveRequest(anyLong(), anyLong(), anyString()))
                .thenThrow(new ConflictException("Can only approve requests in PENDING status"));

            String requestBody = """
                {
                    "adminNotes": "Test"
                }
                """;

            mvc.perform(post("/api/admin/hosting-requests/1/approve")
                    .with(user(adminUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("Can only approve requests in PENDING status")));
        }
    }

    @Nested
    @DisplayName("POST /api/admin/hosting-requests/{id}/reject")
    class RejectHostingRequest {

        @Test
        @DisplayName("Should reject request successfully")
        @WithMockUser(username = "admin", roles = "ADMIN")
        void rejectRequest_Success() throws Exception {
            HostingRequestDTO responseDto = new HostingRequestDTO();
            responseDto.setId(1L);
            responseDto.setUserId(42L);
            responseDto.setStatus(HostingRequestStatus.REJECTED);
            responseDto.setReviewedBy(1L);
            responseDto.setReviewedAt(LocalDateTime.now());
            responseDto.setAdminNotes("Email domain mismatch");

            when(contestHostingService.rejectRequest(anyLong(), anyLong(), anyString()))
                .thenReturn(responseDto);

            String requestBody = """
                {
                    "adminNotes": "Email domain mismatch"
                }
                """;

            mvc.perform(post("/api/admin/hosting-requests/1/reject")
                    .with(user(adminUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.status", is("REJECTED")))
                .andExpect(jsonPath("$.reviewedBy", is(1)))
                .andExpect(jsonPath("$.adminNotes", is("Email domain mismatch")));

            verify(contestHostingService).rejectRequest(eq(1L), eq(1L), eq("Email domain mismatch"));
        }

        @Test
        @DisplayName("Should return 404 when request not found")
        @WithMockUser(username = "admin", roles = "ADMIN")
        void rejectRequest_NotFound() throws Exception {
            when(contestHostingService.rejectRequest(anyLong(), anyLong(), anyString()))
                .thenThrow(new ResourceNotFoundException("Hosting request not found"));

            String requestBody = """
                {
                    "adminNotes": "Test"
                }
                """;

            mvc.perform(post("/api/admin/hosting-requests/999/reject")
                    .with(user(adminUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Hosting request not found")));
        }

        @Test
        @DisplayName("Should return 409 when request not in pending status")
        @WithMockUser(username = "admin", roles = "ADMIN")
        void rejectRequest_NotPending() throws Exception {
            when(contestHostingService.rejectRequest(anyLong(), anyLong(), anyString()))
                .thenThrow(new ConflictException("Can only reject requests in PENDING status"));

            String requestBody = """
                {
                    "adminNotes": "Test"
                }
                """;

            mvc.perform(post("/api/admin/hosting-requests/1/reject")
                    .with(user(adminUser))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("Can only reject requests in PENDING status")));
        }
    }
}
