package com.example.codecombat2026.controller;

import com.example.codecombat2026.dto.ContestAnalyticsDTO;
import com.example.codecombat2026.dto.EngagementTimelineEntryDTO;
import com.example.codecombat2026.dto.ProblemStatDTO;
import com.example.codecombat2026.security.SecurityConfig;
import com.example.codecombat2026.security.jwt.AuthEntryPointJwt;
import com.example.codecombat2026.security.jwt.AuthTokenFilter;
import com.example.codecombat2026.security.jwt.JwtUtils;
import com.example.codecombat2026.security.services.UserDetailsImpl;
import com.example.codecombat2026.security.services.UserDetailsServiceImpl;
import com.example.codecombat2026.service.PrivateContestAccessValidator;
import com.example.codecombat2026.service.PrivateContestAnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for PrivateContestAnalyticsController.
 * 
 * Tests verify:
 * - Access control (only host can access analytics)
 * - JSON analytics endpoint returns correct data structure
 * - CSV export endpoint returns correct format and headers
 * - Error handling for forbidden access and missing contests
 * 
 * Requirements: 16.1, 16.3 (Task 11.3)
 */
@WebMvcTest(PrivateContestAnalyticsController.class)
@Import(SecurityConfig.class)
@DisplayName("PrivateContestAnalyticsController Tests")
class PrivateContestAnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PrivateContestAnalyticsService analyticsService;

    @MockBean
    private PrivateContestAccessValidator accessValidator;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private AuthEntryPointJwt authEntryPointJwt;

    @MockBean
    private AuthTokenFilter authTokenFilter;

    @MockBean
    private JwtUtils jwtUtils;

    private UserDetailsImpl hostUser;
    private UserDetailsImpl participantUser;
    private UserDetailsImpl outsiderUser;

    @BeforeEach
    void setUp() throws Exception {
        // Create test users
        hostUser = new UserDetailsImpl(
                42L, "host", "host@example.com", "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );

        participantUser = new UserDetailsImpl(
                55L, "participant", "participant@example.com", "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );

        outsiderUser = new UserDetailsImpl(
                99L, "outsider", "outsider@example.com", "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );

        // Make auth filter pass-through
        doAnswer(invocation -> {
            invocation.getArgument(2, jakarta.servlet.FilterChain.class)
                    .doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(authTokenFilter).doFilter(any(), any(), any());
    }

    @Nested
    @DisplayName("GET /api/contests/private/{contestId}/analytics")
    class GetAnalytics {

        @Test
        @DisplayName("Should return analytics JSON for contest host")
        void testGetAnalytics_Host_Success() throws Exception {
            // Arrange
            Long contestId = 501L;
            
            // Mock access validation - host can access
            when(accessValidator.isHost(contestId, hostUser.getId())).thenReturn(true);
            
            // Mock analytics data
            List<ProblemStatDTO> problemStats = Arrays.asList(
                    new ProblemStatDTO(10L, "Two Sum", 35, 28, 80.0, 12.5),
                    new ProblemStatDTO(25L, "Binary Search Tree", 40, 20, 50.0, 25.3)
            );
            List<EngagementTimelineEntryDTO> timeline = Arrays.asList(
                    new EngagementTimelineEntryDTO("2026-02-01T14:00:00Z", 15),
                    new EngagementTimelineEntryDTO("2026-02-01T14:15:00Z", 22)
            );
            ContestAnalyticsDTO analytics = new ContestAnalyticsDTO(35, 32, 120, problemStats, timeline);
            
            when(analyticsService.getAnalytics(contestId, hostUser.getId())).thenReturn(analytics);
            
            // Set authentication context
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(hostUser, null, hostUser.getAuthorities())
            );
            
            // Act & Assert
            mockMvc.perform(get("/api/contests/private/{contestId}/analytics", contestId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.totalParticipants", is(35)))
                    .andExpect(jsonPath("$.activeParticipants", is(32)))
                    .andExpect(jsonPath("$.totalSubmissions", is(120)))
                    .andExpect(jsonPath("$.problemStats", hasSize(2)))
                    .andExpect(jsonPath("$.problemStats[0].problemId", is(10)))
                    .andExpect(jsonPath("$.problemStats[0].problemTitle", is("Two Sum")))
                    .andExpect(jsonPath("$.problemStats[0].submissionCount", is(35)))
                    .andExpect(jsonPath("$.problemStats[0].acceptedSubmissions", is(28)))
                    .andExpect(jsonPath("$.problemStats[0].acceptanceRate", is(80.0)))
                    .andExpect(jsonPath("$.problemStats[0].avgSolveTimeMinutes", is(12.5)))
                    .andExpect(jsonPath("$.problemStats[1].problemId", is(25)))
                    .andExpect(jsonPath("$.problemStats[1].problemTitle", is("Binary Search Tree")))
                    .andExpect(jsonPath("$.engagementTimeline", hasSize(2)))
                    .andExpect(jsonPath("$.engagementTimeline[0].timestamp", is("2026-02-01T14:00:00Z")))
                    .andExpect(jsonPath("$.engagementTimeline[0].submissionCount", is(15)));
            
            // Verify interactions
            verify(accessValidator).isHost(contestId, hostUser.getId());
            verify(analyticsService).getAnalytics(contestId, hostUser.getId());
        }

        @Test
        @DisplayName("Should return 403 Forbidden when user is not the host")
        void testGetAnalytics_Forbidden_NotHost() throws Exception {
            // Arrange
            Long contestId = 501L;
            
            // Mock access validation - participant is not the host
            when(accessValidator.isHost(contestId, participantUser.getId())).thenReturn(false);
            
            // Set authentication context
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(participantUser, null, participantUser.getAuthorities())
            );
            
            // Act & Assert
            mockMvc.perform(get("/api/contests/private/{contestId}/analytics", contestId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
            
            // Verify access check was called but service was not
            verify(accessValidator).isHost(contestId, participantUser.getId());
            verify(analyticsService, never()).getAnalytics(any(), any());
        }

        @Test
        @DisplayName("Should return 401 Unauthorized when user is not authenticated")
        void testGetAnalytics_Unauthorized() throws Exception {
            // Arrange
            Long contestId = 501L;
            
            // Clear authentication context
            SecurityContextHolder.clearContext();
            
            // Act & Assert
            mockMvc.perform(get("/api/contests/private/{contestId}/analytics", contestId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
            
            // Verify services were not called
            verify(accessValidator, never()).isHost(any(), any());
            verify(analyticsService, never()).getAnalytics(any(), any());
        }

        @Test
        @DisplayName("Should handle service exception gracefully")
        void testGetAnalytics_ServiceException() throws Exception {
            // Arrange
            Long contestId = 501L;
            
            // Mock access validation
            when(accessValidator.isHost(contestId, hostUser.getId())).thenReturn(true);
            
            // Mock service exception
            when(analyticsService.getAnalytics(contestId, hostUser.getId()))
                    .thenThrow(new IllegalArgumentException("Contest not found"));
            
            // Set authentication context
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(hostUser, null, hostUser.getAuthorities())
            );
            
            // Act & Assert
            mockMvc.perform(get("/api/contests/private/{contestId}/analytics", contestId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
            
            // Verify interactions
            verify(accessValidator).isHost(contestId, hostUser.getId());
            verify(analyticsService).getAnalytics(contestId, hostUser.getId());
        }
    }

    @Nested
    @DisplayName("GET /api/contests/private/{contestId}/analytics/export")
    class ExportAnalyticsCSV {

        @Test
        @DisplayName("Should return CSV file with correct headers for contest host")
        void testExportCSV_Host_Success() throws Exception {
            // Arrange
            Long contestId = 501L;
            
            // Mock access validation - host can access
            when(accessValidator.isHost(contestId, hostUser.getId())).thenReturn(true);
            
            // Mock CSV export
            String csvContent = "Contest Name,CS101 Midterm Exam\n" +
                    "Host,host\n" +
                    "Start Time,2026-02-01T14:00:00Z\n" +
                    "End Time,2026-02-01T17:00:00Z\n" +
                    "Total Participants,35\n" +
                    "Active Participants,32\n" +
                    "Total Submissions,120\n" +
                    "\n" +
                    "Problem ID,Problem Title,Total Submissions,Accepted Submissions,Acceptance Rate (%),Avg Solve Time (min)\n" +
                    "10,Two Sum,35,28,80.00,12.50\n" +
                    "25,Binary Search Tree,40,20,50.00,25.30\n";
            
            when(analyticsService.exportAnalyticsCSV(contestId, hostUser.getId())).thenReturn(csvContent);
            
            // Set authentication context
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(hostUser, null, hostUser.getAuthorities())
            );
            
            // Act & Assert
            mockMvc.perform(get("/api/contests/private/{contestId}/analytics/export", contestId))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", "text/csv; charset=UTF-8"))
                    .andExpect(header().string("Content-Disposition", 
                            "form-data; name=\"attachment\"; filename=\"contest_" + contestId + "_analytics.csv\""))
                    .andExpect(content().string(csvContent))
                    .andExpect(content().string(containsString("Contest Name,CS101 Midterm Exam")))
                    .andExpect(content().string(containsString("Problem ID,Problem Title")))
                    .andExpect(content().string(containsString("10,Two Sum,35,28,80.00,12.50")));
            
            // Verify interactions
            verify(accessValidator).isHost(contestId, hostUser.getId());
            verify(analyticsService).exportAnalyticsCSV(contestId, hostUser.getId());
        }

        @Test
        @DisplayName("Should handle CSV fields with commas properly escaped")
        void testExportCSV_EscapedFields() throws Exception {
            // Arrange
            Long contestId = 501L;
            
            // Mock access validation
            when(accessValidator.isHost(contestId, hostUser.getId())).thenReturn(true);
            
            // Mock CSV with escaped field (problem title contains comma)
            String csvContent = "Contest Name,\"Contest, Special Edition\"\n" +
                    "Host,host\n" +
                    "Start Time,2026-02-01T14:00:00Z\n" +
                    "End Time,2026-02-01T17:00:00Z\n" +
                    "Total Participants,10\n" +
                    "Active Participants,8\n" +
                    "Total Submissions,30\n" +
                    "\n" +
                    "Problem ID,Problem Title,Total Submissions,Accepted Submissions,Acceptance Rate (%),Avg Solve Time (min)\n" +
                    "10,\"Two Sum, Redux\",15,10,66.67,15.00\n";
            
            when(analyticsService.exportAnalyticsCSV(contestId, hostUser.getId())).thenReturn(csvContent);
            
            // Set authentication context
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(hostUser, null, hostUser.getAuthorities())
            );
            
            // Act & Assert
            mockMvc.perform(get("/api/contests/private/{contestId}/analytics/export", contestId))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", "text/csv; charset=UTF-8"))
                    .andExpect(content().string(containsString("\"Contest, Special Edition\"")))
                    .andExpect(content().string(containsString("\"Two Sum, Redux\"")));
            
            // Verify interactions
            verify(accessValidator).isHost(contestId, hostUser.getId());
            verify(analyticsService).exportAnalyticsCSV(contestId, hostUser.getId());
        }

        @Test
        @DisplayName("Should return 403 Forbidden when user is not the host")
        void testExportCSV_Forbidden_NotHost() throws Exception {
            // Arrange
            Long contestId = 501L;
            
            // Mock access validation - outsider is not the host
            when(accessValidator.isHost(contestId, outsiderUser.getId())).thenReturn(false);
            
            // Set authentication context
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(outsiderUser, null, outsiderUser.getAuthorities())
            );
            
            // Act & Assert
            mockMvc.perform(get("/api/contests/private/{contestId}/analytics/export", contestId))
                    .andExpect(status().isForbidden());
            
            // Verify access check was called but service was not
            verify(accessValidator).isHost(contestId, outsiderUser.getId());
            verify(analyticsService, never()).exportAnalyticsCSV(any(), any());
        }

        @Test
        @DisplayName("Should return 401 Unauthorized when user is not authenticated")
        void testExportCSV_Unauthorized() throws Exception {
            // Arrange
            Long contestId = 501L;
            
            // Clear authentication context
            SecurityContextHolder.clearContext();
            
            // Act & Assert
            mockMvc.perform(get("/api/contests/private/{contestId}/analytics/export", contestId))
                    .andExpect(status().isUnauthorized());
            
            // Verify services were not called
            verify(accessValidator, never()).isHost(any(), any());
            verify(analyticsService, never()).exportAnalyticsCSV(any(), any());
        }

        @Test
        @DisplayName("Should verify filename contains contestId")
        void testExportCSV_FilenameFormat() throws Exception {
            // Arrange
            Long contestId = 999L;
            
            // Mock access validation
            when(accessValidator.isHost(contestId, hostUser.getId())).thenReturn(true);
            
            // Mock CSV export
            String csvContent = "Contest Name,Test Contest\n";
            when(analyticsService.exportAnalyticsCSV(contestId, hostUser.getId())).thenReturn(csvContent);
            
            // Set authentication context
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(hostUser, null, hostUser.getAuthorities())
            );
            
            // Act & Assert
            mockMvc.perform(get("/api/contests/private/{contestId}/analytics/export", contestId))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Disposition", 
                            containsString("contest_999_analytics.csv")));
            
            // Verify interactions
            verify(accessValidator).isHost(contestId, hostUser.getId());
            verify(analyticsService).exportAnalyticsCSV(contestId, hostUser.getId());
        }

        @Test
        @DisplayName("Should handle service exception gracefully")
        void testExportCSV_ServiceException() throws Exception {
            // Arrange
            Long contestId = 501L;
            
            // Mock access validation
            when(accessValidator.isHost(contestId, hostUser.getId())).thenReturn(true);
            
            // Mock service exception
            when(analyticsService.exportAnalyticsCSV(contestId, hostUser.getId()))
                    .thenThrow(new IllegalArgumentException("Contest not found"));
            
            // Set authentication context
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(hostUser, null, hostUser.getAuthorities())
            );
            
            // Act & Assert
            mockMvc.perform(get("/api/contests/private/{contestId}/analytics/export", contestId))
                    .andExpect(status().isBadRequest());
            
            // Verify interactions
            verify(accessValidator).isHost(contestId, hostUser.getId());
            verify(analyticsService).exportAnalyticsCSV(contestId, hostUser.getId());
        }
    }
}
