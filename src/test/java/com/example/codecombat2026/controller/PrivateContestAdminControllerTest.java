package com.example.codecombat2026.controller;

import com.example.codecombat2026.dto.PrivateContestDTO;
import com.example.codecombat2026.entity.Contest.ContestStatus;
import com.example.codecombat2026.exception.GlobalExceptionHandler;
import com.example.codecombat2026.exception.ResourceNotFoundException;
import com.example.codecombat2026.security.SecurityConfig;
import com.example.codecombat2026.security.jwt.AuthEntryPointJwt;
import com.example.codecombat2026.security.jwt.AuthTokenFilter;
import com.example.codecombat2026.security.jwt.JwtUtils;
import com.example.codecombat2026.security.services.UserDetailsImpl;
import com.example.codecombat2026.security.services.UserDetailsServiceImpl;
import com.example.codecombat2026.service.PrivateContestAdminService;
import com.example.codecombat2026.service.PrivateContestAdminService.ContestFilters;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
 * Comprehensive unit tests for PrivateContestAdminController using @WebMvcTest.
 * 
 * Tests cover:
 * - Listing all private contests (with/without filters, pagination)
 * - Getting contest details (success, not found)
 * - Deleting contests (success, not found)
 * - Getting judge statistics
 * - Role-based security (@PreAuthorize ADMIN only)
 * - Error handling
 * 
 * Requirements: 19.1, 19.2, 19.3, 22.3
 */
@WebMvcTest(controllers = PrivateContestAdminController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
    "codecombat.jwt.secret=dGhpcy1pcy1hLXRlc3Qtc2VjcmV0LXBhZGRlZC10by0zMi1ieXRlcw==",
    "codecombat.jwt.expiration=86400000",
    "APP_ALLOWED_ORIGINS=http://localhost:5173"
})
class PrivateContestAdminControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PrivateContestAdminService adminService;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private AuthEntryPointJwt entryPoint;

    @MockBean
    private AuthTokenFilter authTokenFilter;

    @MockBean
    private JwtUtils jwtUtils;

    private UserDetailsImpl adminUser;
    private UserDetailsImpl regularUser;
    private PrivateContestDTO sampleContest1;
    private PrivateContestDTO sampleContest2;

    @BeforeEach
    void setUp() throws Exception {
        // Create test users
        adminUser = new UserDetailsImpl(1L, "admin", "admin@example.com", "password", Collections.emptyList());
        regularUser = new UserDetailsImpl(42L, "user", "user@example.com", "password", Collections.emptyList());

        // Make auth filter pass-through
        doAnswer((InvocationOnMock inv) -> {
            ServletRequest req = inv.getArgument(0);
            ServletResponse res = inv.getArgument(1);
            FilterChain chain = inv.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(authTokenFilter).doFilter(any(), any(), any());

        // Create sample contest DTOs
        sampleContest1 = createSampleContest(101L, 501L, "CS101 Midterm", 42L, "prof_smith", 
                ContestStatus.UPCOMING, 35L, false);
        sampleContest2 = createSampleContest(102L, 502L, "CS202 Final", 43L, "prof_jones", 
                ContestStatus.LIVE, 50L, false);
    }

    private PrivateContestDTO createSampleContest(Long id, Long contestId, String name, 
            Long hostUserId, String hostUsername, ContestStatus status, Long participantCount, Boolean cancelled) {
        PrivateContestDTO dto = new PrivateContestDTO();
        dto.setId(id);
        dto.setContestId(contestId);
        dto.setName(name);
        dto.setDescription("Test contest description");
        dto.setHostUserId(hostUserId);
        dto.setHostUsername(hostUsername);
        dto.setStatus(status);
        dto.setStartTime(LocalDateTime.now().plusDays(1));
        dto.setEndTime(LocalDateTime.now().plusDays(1).plusHours(3));
        dto.setParticipantCount(participantCount);
        dto.setEnableProctoring(true);
        dto.setCancelled(cancelled);
        dto.setCreatedAt(LocalDateTime.now().minusDays(5));
        return dto;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // List All Private Contests Tests
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/admin/private-contests")
    class ListAllPrivateContests {

        @Test
        @DisplayName("Should list all private contests successfully (admin)")
        @WithMockUser(username = "admin", roles = "ADMIN")
        void listContests_Success() throws Exception {
            List<PrivateContestDTO> contests = Arrays.asList(sampleContest1, sampleContest2);
            Page<PrivateContestDTO> page = new PageImpl<>(contests, PageRequest.of(0, 20), contests.size());

            when(adminService.listAllPrivateContests(any(ContestFilters.class), any(Pageable.class)))
                .thenReturn(page);

            mvc.perform(get("/api/admin/private-contests")
                    .with(user(adminUser))
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id", is(101)))
                .andExpect(jsonPath("$.content[0].name", is("CS101 Midterm")))
                .andExpect(jsonPath("$.content[0].hostUsername", is("prof_smith")))
                .andExpect(jsonPath("$.content[0].status", is("UPCOMING")))
                .andExpect(jsonPath("$.content[0].participantCount", is(35)))
                .andExpect(jsonPath("$.content[1].id", is(102)))
                .andExpect(jsonPath("$.content[1].name", is("CS202 Final")))
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.totalPages", is(1)))
                .andExpect(jsonPath("$.number", is(0)))
                .andExpect(jsonPath("$.size", is(20)));

            verify(adminService, times(1)).listAllPrivateContests(any(ContestFilters.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Should list contests with status filter")
        @WithMockUser(username = "admin", roles = "ADMIN")
        void listContests_WithStatusFilter() throws Exception {
            List<PrivateContestDTO> contests = Arrays.asList(sampleContest2);
            Page<PrivateContestDTO> page = new PageImpl<>(contests, PageRequest.of(0, 20), contests.size());

            when(adminService.listAllPrivateContests(any(ContestFilters.class), any(Pageable.class)))
                .thenReturn(page);

            mvc.perform(get("/api/admin/private-contests")
                    .with(user(adminUser))
                    .param("status", "LIVE")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].status", is("LIVE")));

            verify(adminService, times(1)).listAllPrivateContests(any(ContestFilters.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Should list contests with host filter")
        @WithMockUser(username = "admin", roles = "ADMIN")
        void listContests_WithHostFilter() throws Exception {
            List<PrivateContestDTO> contests = Arrays.asList(sampleContest1);
            Page<PrivateContestDTO> page = new PageImpl<>(contests, PageRequest.of(0, 20), contests.size());

            when(adminService.listAllPrivateContests(any(ContestFilters.class), any(Pageable.class)))
                .thenReturn(page);

            mvc.perform(get("/api/admin/private-contests")
                    .with(user(adminUser))
                    .param("hostUserId", "42")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].hostUserId", is(42)));

            verify(adminService, times(1)).listAllPrivateContests(any(ContestFilters.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Should list contests with pagination")
        @WithMockUser(username = "admin", roles = "ADMIN")
        void listContests_WithPagination() throws Exception {
            List<PrivateContestDTO> contests = Arrays.asList(sampleContest1);
            Page<PrivateContestDTO> page = new PageImpl<>(contests, PageRequest.of(1, 10), 25);

            when(adminService.listAllPrivateContests(any(ContestFilters.class), any(Pageable.class)))
                .thenReturn(page);

            mvc.perform(get("/api/admin/private-contests")
                    .with(user(adminUser))
                    .param("page", "1")
                    .param("size", "10")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements", is(25)))
                .andExpect(jsonPath("$.totalPages", is(3)))
                .andExpect(jsonPath("$.number", is(1)))
                .andExpect(jsonPath("$.size", is(10)));

            verify(adminService, times(1)).listAllPrivateContests(any(ContestFilters.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Should return empty list when no contests exist")
        @WithMockUser(username = "admin", roles = "ADMIN")
        void listContests_EmptyList() throws Exception {
            Page<PrivateContestDTO> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0);

            when(adminService.listAllPrivateContests(any(ContestFilters.class), any(Pageable.class)))
                .thenReturn(emptyPage);

            mvc.perform(get("/api/admin/private-contests")
                    .with(user(adminUser))
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements", is(0)));

            verify(adminService, times(1)).listAllPrivateContests(any(ContestFilters.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Should deny access for non-admin user")
        @WithMockUser(username = "user", roles = "USER")
        void listContests_DeniedForRegularUser() throws Exception {
            mvc.perform(get("/api/admin/private-contests")
                    .with(user(regularUser))
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

            verify(adminService, never()).listAllPrivateContests(any(ContestFilters.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Should deny access for unauthenticated user")
        void listContests_DeniedForUnauthenticated() throws Exception {
            mvc.perform(get("/api/admin/private-contests")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

            verify(adminService, never()).listAllPrivateContests(any(ContestFilters.class), any(Pageable.class));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Get Contest Details Tests
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/admin/private-contests/{contestId}")
    class GetPrivateContestDetails {

        @Test
        @DisplayName("Should get contest details successfully (admin)")
        @WithMockUser(username = "admin", roles = "ADMIN")
        void getDetails_Success() throws Exception {
            when(adminService.getPrivateContestDetails(501L))
                .thenReturn(sampleContest1);

            mvc.perform(get("/api/admin/private-contests/501")
                    .with(user(adminUser))
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(101)))
                .andExpect(jsonPath("$.contestId", is(501)))
                .andExpect(jsonPath("$.name", is("CS101 Midterm")))
                .andExpect(jsonPath("$.hostUserId", is(42)))
                .andExpect(jsonPath("$.hostUsername", is("prof_smith")))
                .andExpect(jsonPath("$.status", is("UPCOMING")))
                .andExpect(jsonPath("$.participantCount", is(35)))
                .andExpect(jsonPath("$.enableProctoring", is(true)))
                .andExpect(jsonPath("$.cancelled", is(false)));

            verify(adminService, times(1)).getPrivateContestDetails(501L);
        }

        @Test
        @DisplayName("Should return 404 when contest not found")
        @WithMockUser(username = "admin", roles = "ADMIN")
        void getDetails_NotFound() throws Exception {
            when(adminService.getPrivateContestDetails(999L))
                .thenThrow(new ResourceNotFoundException("Private contest not found"));

            mvc.perform(get("/api/admin/private-contests/999")
                    .with(user(adminUser))
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

            verify(adminService, times(1)).getPrivateContestDetails(999L);
        }

        @Test
        @DisplayName("Should deny access for non-admin user")
        @WithMockUser(username = "user", roles = "USER")
        void getDetails_DeniedForRegularUser() throws Exception {
            mvc.perform(get("/api/admin/private-contests/501")
                    .with(user(regularUser))
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

            verify(adminService, never()).getPrivateContestDetails(anyLong());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Delete Contest Tests
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("DELETE /api/admin/private-contests/{contestId}")
    class DeletePrivateContest {

        @Test
        @DisplayName("Should delete contest successfully (admin)")
        @WithMockUser(username = "admin", roles = "ADMIN")
        void deleteContest_Success() throws Exception {
            doNothing().when(adminService).deletePrivateContest(501L, 1L);

            mvc.perform(delete("/api/admin/private-contests/501")
                    .with(user(adminUser))
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Private contest deleted successfully")))
                .andExpect(jsonPath("$.contestId", is(501)))
                .andExpect(jsonPath("$.deletedAt").exists());

            verify(adminService, times(1)).deletePrivateContest(501L, 1L);
        }

        @Test
        @DisplayName("Should return 404 when contest to delete not found")
        @WithMockUser(username = "admin", roles = "ADMIN")
        void deleteContest_NotFound() throws Exception {
            doThrow(new ResourceNotFoundException("Private contest not found"))
                .when(adminService).deletePrivateContest(999L, 1L);

            mvc.perform(delete("/api/admin/private-contests/999")
                    .with(user(adminUser))
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

            verify(adminService, times(1)).deletePrivateContest(999L, 1L);
        }

        @Test
        @DisplayName("Should deny access for non-admin user")
        @WithMockUser(username = "user", roles = "USER")
        void deleteContest_DeniedForRegularUser() throws Exception {
            mvc.perform(delete("/api/admin/private-contests/501")
                    .with(user(regularUser))
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

            verify(adminService, never()).deletePrivateContest(anyLong(), anyLong());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Judge Stats Tests
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/admin/private-contests/judge-stats")
    class GetJudgeStats {

        @Test
        @DisplayName("Should get judge stats successfully (admin)")
        @WithMockUser(username = "admin", roles = "ADMIN")
        void getJudgeStats_Success() throws Exception {
            mvc.perform(get("/api/admin/private-contests/judge-stats")
                    .with(user(adminUser))
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicQueueLength").exists())
                .andExpect(jsonPath("$.privateQueueLength").exists())
                .andExpect(jsonPath("$.avgJudgeLatencySeconds").exists())
                .andExpect(jsonPath("$.workersActive").exists())
                .andExpect(jsonPath("$.workersIdle").exists())
                .andExpect(jsonPath("$.totalWorkers").exists())
                .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("Should deny access for non-admin user")
        @WithMockUser(username = "user", roles = "USER")
        void getJudgeStats_DeniedForRegularUser() throws Exception {
            mvc.perform(get("/api/admin/private-contests/judge-stats")
                    .with(user(regularUser))
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Audit Log Query Tests
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/admin/private-contests/audit-logs")
    class QueryAuditLogs {

        @MockBean
        private com.example.codecombat2026.repository.AuditLogRepository auditLogRepository;

        @Test
        @DisplayName("Should query audit logs successfully (admin)")
        @WithMockUser(username = "admin", roles = "ADMIN")
        void queryAuditLogs_Success() throws Exception {
            // Create sample audit logs
            com.example.codecombat2026.entity.AuditLog log1 = new com.example.codecombat2026.entity.AuditLog();
            log1.setId(1L);
            log1.setAction("CONTEST_CREATED");
            log1.setResourceType("PRIVATE_CONTEST");
            log1.setResourceId(501L);
            log1.setTimestamp(LocalDateTime.now());
            log1.setIpAddress("192.168.1.100");
            log1.setUserAgent("Mozilla/5.0");
            log1.setDetailsJson("{\"contestName\":\"CS101 Midterm\"}");
            
            com.example.codecombat2026.entity.User testUser = new com.example.codecombat2026.entity.User();
            testUser.setId(42L);
            testUser.setUsername("prof_smith");
            log1.setUser(testUser);

            List<com.example.codecombat2026.entity.AuditLog> logs = Arrays.asList(log1);
            Page<com.example.codecombat2026.entity.AuditLog> page = new PageImpl<>(logs, PageRequest.of(0, 50), logs.size());

            when(auditLogRepository.findByFilters(isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

            mvc.perform(get("/api/admin/private-contests/audit-logs")
                    .with(user(adminUser))
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(1)))
                .andExpect(jsonPath("$.content[0].action", is("CONTEST_CREATED")))
                .andExpect(jsonPath("$.content[0].resourceType", is("PRIVATE_CONTEST")))
                .andExpect(jsonPath("$.content[0].resourceId", is(501)))
                .andExpect(jsonPath("$.content[0].userId", is(42)))
                .andExpect(jsonPath("$.content[0].username", is("prof_smith")))
                .andExpect(jsonPath("$.content[0].ipAddress", is("192.168.1.100")))
                .andExpect(jsonPath("$.content[0].detailsJson").exists())
                .andExpect(jsonPath("$.totalElements", is(1)));

            verify(auditLogRepository, times(1)).findByFilters(isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class));
        }

        @Test
        @DisplayName("Should query audit logs with userId filter")
        @WithMockUser(username = "admin", roles = "ADMIN")
        void queryAuditLogs_WithUserIdFilter() throws Exception {
            Page<com.example.codecombat2026.entity.AuditLog> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 50), 0);

            when(auditLogRepository.findByFilters(eq(42L), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(emptyPage);

            mvc.perform(get("/api/admin/private-contests/audit-logs")
                    .with(user(adminUser))
                    .param("userId", "42")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));

            verify(auditLogRepository, times(1)).findByFilters(eq(42L), isNull(), isNull(), isNull(), isNull(), any(Pageable.class));
        }

        @Test
        @DisplayName("Should query audit logs with action filter")
        @WithMockUser(username = "admin", roles = "ADMIN")
        void queryAuditLogs_WithActionFilter() throws Exception {
            Page<com.example.codecombat2026.entity.AuditLog> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 50), 0);

            when(auditLogRepository.findByFilters(isNull(), eq("CONTEST_CREATED"), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(emptyPage);

            mvc.perform(get("/api/admin/private-contests/audit-logs")
                    .with(user(adminUser))
                    .param("action", "CONTEST_CREATED")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

            verify(auditLogRepository, times(1)).findByFilters(isNull(), eq("CONTEST_CREATED"), isNull(), isNull(), isNull(), any(Pageable.class));
        }

        @Test
        @DisplayName("Should query audit logs with resourceType filter")
        @WithMockUser(username = "admin", roles = "ADMIN")
        void queryAuditLogs_WithResourceTypeFilter() throws Exception {
            Page<com.example.codecombat2026.entity.AuditLog> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 50), 0);

            when(auditLogRepository.findByFilters(isNull(), isNull(), eq("PRIVATE_CONTEST"), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(emptyPage);

            mvc.perform(get("/api/admin/private-contests/audit-logs")
                    .with(user(adminUser))
                    .param("resourceType", "PRIVATE_CONTEST")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

            verify(auditLogRepository, times(1)).findByFilters(isNull(), isNull(), eq("PRIVATE_CONTEST"), isNull(), isNull(), any(Pageable.class));
        }

        @Test
        @DisplayName("Should query audit logs with date range filter")
        @WithMockUser(username = "admin", roles = "ADMIN")
        void queryAuditLogs_WithDateRangeFilter() throws Exception {
            Page<com.example.codecombat2026.entity.AuditLog> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 50), 0);

            when(auditLogRepository.findByFilters(isNull(), isNull(), isNull(), any(LocalDateTime.class), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(emptyPage);

            mvc.perform(get("/api/admin/private-contests/audit-logs")
                    .with(user(adminUser))
                    .param("startDate", "2026-01-01T00:00:00")
                    .param("endDate", "2026-02-01T00:00:00")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

            verify(auditLogRepository, times(1)).findByFilters(isNull(), isNull(), isNull(), any(LocalDateTime.class), any(LocalDateTime.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Should query audit logs with pagination")
        @WithMockUser(username = "admin", roles = "ADMIN")
        void queryAuditLogs_WithPagination() throws Exception {
            Page<com.example.codecombat2026.entity.AuditLog> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(2, 20), 0);

            when(auditLogRepository.findByFilters(isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(emptyPage);

            mvc.perform(get("/api/admin/private-contests/audit-logs")
                    .with(user(adminUser))
                    .param("page", "2")
                    .param("size", "20")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

            verify(auditLogRepository, times(1)).findByFilters(isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class));
        }

        @Test
        @DisplayName("Should deny access for non-admin user")
        @WithMockUser(username = "user", roles = "USER")
        void queryAuditLogs_DeniedForRegularUser() throws Exception {
            mvc.perform(get("/api/admin/private-contests/audit-logs")
                    .with(user(regularUser))
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

            verify(auditLogRepository, never()).findByFilters(any(), any(), any(), any(), any(), any(Pageable.class));
        }

        @Test
        @DisplayName("Should deny access for unauthenticated user")
        void queryAuditLogs_DeniedForUnauthenticated() throws Exception {
            mvc.perform(get("/api/admin/private-contests/audit-logs")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

            verify(auditLogRepository, never()).findByFilters(any(), any(), any(), any(), any(), any(Pageable.class));
        }
    }
}
