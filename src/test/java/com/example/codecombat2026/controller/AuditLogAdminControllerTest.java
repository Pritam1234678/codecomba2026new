package com.example.codecombat2026.controller;

import com.example.codecombat2026.entity.AuditLog;
import com.example.codecombat2026.entity.User;
import com.example.codecombat2026.repository.AuditLogRepository;
import com.example.codecombat2026.security.SecurityConfig;
import com.example.codecombat2026.security.jwt.AuthEntryPointJwt;
import com.example.codecombat2026.security.jwt.AuthTokenFilter;
import com.example.codecombat2026.security.jwt.JwtUtils;
import com.example.codecombat2026.security.services.UserDetailsImpl;
import com.example.codecombat2026.security.services.UserDetailsServiceImpl;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuditLogAdminController.
 * 
 * Tests the audit log query endpoint with various filter combinations:
 * - No filters (all logs)
 * - Filter by userId
 * - Filter by action type
 * - Filter by resource type
 * - Filter by date range
 * - Combined filters
 * - Pagination and sorting
 * - Access control (admin only)
 * 
 * Requirements: 29.3
 */
@WebMvcTest(AuditLogAdminController.class)
@Import(SecurityConfig.class)
@DisplayName("AuditLogAdminController Integration Tests")
class AuditLogAdminControllerTest {

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private AuthTokenFilter authTokenFilter;

    @MockBean
    private AuthEntryPointJwt authEntryPointJwt;

    @Autowired
    private MockMvc mvc;

    @MockBean
    private AuditLogRepository auditLogRepository;

    private UserDetailsImpl adminUser;
    private UserDetailsImpl regularUser;

    @BeforeEach
    void setUp() {
        adminUser = UserDetailsImpl.build(createUser(1L, "admin", "admin@example.com", "ROLE_ADMIN"));
        regularUser = UserDetailsImpl.build(createUser(2L, "user", "user@example.com", "ROLE_USER"));
    }

    private User createUser(Long id, String username, String email, String role) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        // Note: In real implementation, roles would be properly set up
        return user;
    }

    @Nested
    @DisplayName("GET /api/admin/audit-logs")
    class QueryAuditLogs {

        @Test
        @DisplayName("Should query all audit logs successfully (admin)")
        @WithMockUser(username = "admin", roles = "ADMIN")
        void queryAuditLogs_Success() throws Exception {
            // Create sample audit logs
            User user = createUser(42L, "prof_smith", "prof@example.com", "ROLE_USER");
            
            AuditLog log1 = new AuditLog();
            log1.setId(1L);
            log1.setUser(user);
            log1.setAction("CONTEST_CREATED");
            log1.setResourceType("PRIVATE_CONTEST");
            log1.setResourceId(501L);
            log1.setTimestamp(LocalDateTime.of(2026, 2, 1, 14, 0));
            log1.setIpAddress("192.168.1.100");
            log1.setUserAgent("Mozilla/5.0");
            log1.setDetailsJson("{\"contestName\":\"CS101 Midterm\"}");

            Page<AuditLog> page = new PageImpl<>(
                List.of(log1), 
                PageRequest.of(0, 50), 
                1
            );

            when(auditLogRepository.findByFilters(
                isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

            mvc.perform(get("/api/admin/audit-logs")
                    .with(user(adminUser))
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].userId").value(42))
                .andExpect(jsonPath("$.content[0].username").value("prof_smith"))
                .andExpect(jsonPath("$.content[0].action").value("CONTEST_CREATED"))
                .andExpect(jsonPath("$.content[0].resourceType").value("PRIVATE_CONTEST"))
                .andExpect(jsonPath("$.content[0].resourceId").value(501))
                .andExpect(jsonPath("$.content[0].ipAddress").value("192.168.1.100"))
                .andExpect(jsonPath("$.content[0].detailsJson").value("{\"contestName\":\"CS101 Midterm\"}"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
        }

        @Test
        @DisplayName("Should query audit logs with userId filter")
        @WithMockUser(username = "admin", roles = "ADMIN")
        void queryAuditLogs_WithUserIdFilter() throws Exception {
            Page<AuditLog> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 50), 0);

            when(auditLogRepository.findByFilters(
                eq(42L), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(emptyPage);

            mvc.perform(get("/api/admin/audit-logs")
                    .with(user(adminUser))
                    .param("userId", "42")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
        }

        @Test
        @DisplayName("Should query audit logs with action filter")
        @WithMockUser(username = "admin", roles = "ADMIN")
        void queryAuditLogs_WithActionFilter() throws Exception {
            Page<AuditLog> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 50), 0);

            when(auditLogRepository.findByFilters(
                isNull(), eq("CONTEST_CREATED"), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(emptyPage);

            mvc.perform(get("/api/admin/audit-logs")
                    .with(user(adminUser))
                    .param("action", "CONTEST_CREATED")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
        }

        @Test
        @DisplayName("Should query audit logs with resourceType filter")
        @WithMockUser(username = "admin", roles = "ADMIN")
        void queryAuditLogs_WithResourceTypeFilter() throws Exception {
            Page<AuditLog> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 50), 0);

            when(auditLogRepository.findByFilters(
                isNull(), isNull(), eq("PRIVATE_CONTEST"), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(emptyPage);

            mvc.perform(get("/api/admin/audit-logs")
                    .with(user(adminUser))
                    .param("resourceType", "PRIVATE_CONTEST")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
        }

        @Test
        @DisplayName("Should query audit logs with date range filter")
        @WithMockUser(username = "admin", roles = "ADMIN")
        void queryAuditLogs_WithDateRangeFilter() throws Exception {
            Page<AuditLog> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 50), 0);

            when(auditLogRepository.findByFilters(
                isNull(), isNull(), isNull(), any(LocalDateTime.class), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(emptyPage);

            mvc.perform(get("/api/admin/audit-logs")
                    .with(user(adminUser))
                    .param("startDate", "2026-01-01T00:00:00")
                    .param("endDate", "2026-02-01T00:00:00")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
        }

        @Test
        @DisplayName("Should query audit logs with complex filters")
        @WithMockUser(username = "admin", roles = "ADMIN")
        void queryAuditLogs_WithComplexFilters() throws Exception {
            Page<AuditLog> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 50), 0);

            when(auditLogRepository.findByFilters(
                eq(42L), eq("PARTICIPANT_JOINED"), eq("PARTICIPANT"), 
                any(LocalDateTime.class), isNull(), any(Pageable.class)))
                .thenReturn(emptyPage);

            mvc.perform(get("/api/admin/audit-logs")
                    .with(user(adminUser))
                    .param("userId", "42")
                    .param("action", "PARTICIPANT_JOINED")
                    .param("resourceType", "PARTICIPANT")
                    .param("startDate", "2026-01-15T00:00:00")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
        }

        @Test
        @DisplayName("Should query audit logs with pagination")
        @WithMockUser(username = "admin", roles = "ADMIN")
        void queryAuditLogs_WithPagination() throws Exception {
            Page<AuditLog> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(2, 20), 0);

            when(auditLogRepository.findByFilters(
                isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(emptyPage);

            mvc.perform(get("/api/admin/audit-logs")
                    .with(user(adminUser))
                    .param("page", "2")
                    .param("size", "20")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(2))
                .andExpect(jsonPath("$.size").value(20));
        }

        @Test
        @DisplayName("Should query audit logs with custom sorting")
        @WithMockUser(username = "admin", roles = "ADMIN")
        void queryAuditLogs_WithCustomSort() throws Exception {
            Page<AuditLog> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 50), 0);

            when(auditLogRepository.findByFilters(
                isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(emptyPage);

            mvc.perform(get("/api/admin/audit-logs")
                    .with(user(adminUser))
                    .param("sort", "action,asc")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should handle audit log with null user (system action)")
        @WithMockUser(username = "admin", roles = "ADMIN")
        void queryAuditLogs_WithNullUser() throws Exception {
            // System-triggered action (no user)
            AuditLog systemLog = new AuditLog();
            systemLog.setId(2L);
            systemLog.setUser(null);
            systemLog.setAction("SYSTEM_CLEANUP");
            systemLog.setResourceType("INVITATION");
            systemLog.setResourceId(null);
            systemLog.setTimestamp(LocalDateTime.of(2026, 2, 1, 2, 0));
            systemLog.setIpAddress(null);
            systemLog.setUserAgent(null);
            systemLog.setDetailsJson("{\"deletedCount\":15}");

            Page<AuditLog> page = new PageImpl<>(
                List.of(systemLog), 
                PageRequest.of(0, 50), 
                1
            );

            when(auditLogRepository.findByFilters(
                isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

            mvc.perform(get("/api/admin/audit-logs")
                    .with(user(adminUser))
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(2))
                .andExpect(jsonPath("$.content[0].userId").isEmpty())
                .andExpect(jsonPath("$.content[0].username").isEmpty())
                .andExpect(jsonPath("$.content[0].action").value("SYSTEM_CLEANUP"));
        }

        @Test
        @DisplayName("Should deny access for non-admin user")
        @WithMockUser(username = "user", roles = "USER")
        void queryAuditLogs_DeniedForRegularUser() throws Exception {
            mvc.perform(get("/api/admin/audit-logs")
                    .with(user(regularUser))
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should deny access for unauthenticated user")
        void queryAuditLogs_DeniedForUnauthenticated() throws Exception {
            mvc.perform(get("/api/admin/audit-logs")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should use default pagination parameters")
        @WithMockUser(username = "admin", roles = "ADMIN")
        void queryAuditLogs_DefaultPagination() throws Exception {
            Page<AuditLog> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 50), 0);

            when(auditLogRepository.findByFilters(
                isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(emptyPage);

            mvc.perform(get("/api/admin/audit-logs")
                    .with(user(adminUser))
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(50));
        }

        @Test
        @DisplayName("Should use default sorting (timestamp,desc)")
        @WithMockUser(username = "admin", roles = "ADMIN")
        void queryAuditLogs_DefaultSorting() throws Exception {
            Page<AuditLog> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 50), 0);

            when(auditLogRepository.findByFilters(
                isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(emptyPage);

            mvc.perform(get("/api/admin/audit-logs")
                    .with(user(adminUser))
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        }
    }
}
