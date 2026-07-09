package com.example.codecombat2026.controller;

import com.example.codecombat2026.dto.LeaderboardEntry;
import com.example.codecombat2026.security.SecurityConfig;
import com.example.codecombat2026.security.jwt.AuthEntryPointJwt;
import com.example.codecombat2026.security.jwt.AuthTokenFilter;
import com.example.codecombat2026.security.jwt.JwtUtils;
import com.example.codecombat2026.security.services.UserDetailsImpl;
import com.example.codecombat2026.security.services.UserDetailsServiceImpl;
import com.example.codecombat2026.service.PrivateContestAccessValidator;
import com.example.codecombat2026.service.PrivateContestLeaderboardService;
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
 * Unit tests for PrivateContestLeaderboardController.
 * 
 * Tests verify:
 * - Access control (host and participant can access, others cannot)
 * - Cache control headers (10-second max-age, must-revalidate)
 * - Leaderboard data retrieval and response format
 * - Error handling for forbidden access
 * 
 * Requirements: 14.3, 14.4
 */
@WebMvcTest(PrivateContestLeaderboardController.class)
@Import(SecurityConfig.class)
@DisplayName("PrivateContestLeaderboardController Tests")
class PrivateContestLeaderboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PrivateContestLeaderboardService leaderboardService;

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
    @DisplayName("GET /api/contests/private/{contestId}/leaderboard")
    class GetLeaderboard {

        @Test
        @DisplayName("Should return leaderboard for contest host with cache headers")
        void testGetLeaderboard_Host_Success() throws Exception {
            // Arrange
            Long contestId = 501L;
            
            // Mock access validation - host can access
            when(accessValidator.canAccess(contestId, hostUser.getId())).thenReturn(true);
            
            // Mock leaderboard data
            List<LeaderboardEntry> leaderboard = Arrays.asList(
                    new LeaderboardEntry(55L, "Alice Johnson", "alice_dev", 300.0, 3, 1),
                    new LeaderboardEntry(56L, "Bob Smith", "bob_smith", 200.0, 2, 2),
                    new LeaderboardEntry(57L, "Charlie Brown", "charlie", 100.0, 1, 3)
            );
            when(leaderboardService.getLeaderboard(contestId)).thenReturn(leaderboard);
            
            // Set authentication context
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(hostUser, null, hostUser.getAuthorities())
            );
            
            // Act & Assert
            mockMvc.perform(get("/api/contests/private/{contestId}/leaderboard", contestId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Cache-Control", "max-age=10, must-revalidate"))
                    .andExpect(jsonPath("$", hasSize(3)))
                    .andExpect(jsonPath("$[0].rank", is(1)))
                    .andExpect(jsonPath("$[0].userId", is(55)))
                    .andExpect(jsonPath("$[0].userName", is("Alice Johnson")))
                    .andExpect(jsonPath("$[0].userRoll", is("alice_dev")))
                    .andExpect(jsonPath("$[0].totalScore", is(300.0)))
                    .andExpect(jsonPath("$[0].problemsSolved", is(3)))
                    .andExpect(jsonPath("$[1].rank", is(2)))
                    .andExpect(jsonPath("$[1].userId", is(56)))
                    .andExpect(jsonPath("$[2].rank", is(3)))
                    .andExpect(jsonPath("$[2].userId", is(57)));
            
            // Verify interactions
            verify(accessValidator).canAccess(contestId, hostUser.getId());
            verify(leaderboardService).getLeaderboard(contestId);
        }

        @Test
        @DisplayName("Should return leaderboard for participant with cache headers")
        void testGetLeaderboard_Participant_Success() throws Exception {
            // Arrange
            Long contestId = 501L;
            
            // Mock access validation - participant can access
            when(accessValidator.canAccess(contestId, participantUser.getId())).thenReturn(true);
            
            // Mock leaderboard data
            List<LeaderboardEntry> leaderboard = Arrays.asList(
                    new LeaderboardEntry(55L, "Alice Johnson", "alice_dev", 300.0, 3, 1),
                    new LeaderboardEntry(56L, "Bob Smith", "bob_smith", 200.0, 2, 2)
            );
            when(leaderboardService.getLeaderboard(contestId)).thenReturn(leaderboard);
            
            // Set authentication context
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(participantUser, null, participantUser.getAuthorities())
            );
            
            // Act & Assert
            mockMvc.perform(get("/api/contests/private/{contestId}/leaderboard", contestId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Cache-Control", "max-age=10, must-revalidate"))
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].rank", is(1)))
                    .andExpect(jsonPath("$[0].userId", is(55)))
                    .andExpect(jsonPath("$[1].rank", is(2)))
                    .andExpect(jsonPath("$[1].userId", is(56)));
            
            // Verify interactions
            verify(accessValidator).canAccess(contestId, participantUser.getId());
            verify(leaderboardService).getLeaderboard(contestId);
        }

        @Test
        @DisplayName("Should return empty leaderboard when no submissions yet")
        void testGetLeaderboard_EmptyLeaderboard() throws Exception {
            // Arrange
            Long contestId = 501L;
            
            // Mock access validation
            when(accessValidator.canAccess(contestId, hostUser.getId())).thenReturn(true);
            
            // Mock empty leaderboard
            when(leaderboardService.getLeaderboard(contestId)).thenReturn(Collections.emptyList());
            
            // Set authentication context
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(hostUser, null, hostUser.getAuthorities())
            );
            
            // Act & Assert
            mockMvc.perform(get("/api/contests/private/{contestId}/leaderboard", contestId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Cache-Control", "max-age=10, must-revalidate"))
                    .andExpect(jsonPath("$", hasSize(0)));
            
            // Verify interactions
            verify(accessValidator).canAccess(contestId, hostUser.getId());
            verify(leaderboardService).getLeaderboard(contestId);
        }

        @Test
        @DisplayName("Should return 403 Forbidden when user is neither host nor participant")
        void testGetLeaderboard_Forbidden_Outsider() throws Exception {
            // Arrange
            Long contestId = 501L;
            
            // Mock access validation - outsider cannot access
            when(accessValidator.canAccess(contestId, outsiderUser.getId())).thenReturn(false);
            
            // Set authentication context
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(outsiderUser, null, outsiderUser.getAuthorities())
            );
            
            // Act & Assert
            mockMvc.perform(get("/api/contests/private/{contestId}/leaderboard", contestId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
            
            // Verify access check was called but leaderboard service was not
            verify(accessValidator).canAccess(contestId, outsiderUser.getId());
            verify(leaderboardService, never()).getLeaderboard(any());
        }

        @Test
        @DisplayName("Should return 401 Unauthorized when user is not authenticated")
        void testGetLeaderboard_Unauthorized() throws Exception {
            // Arrange
            Long contestId = 501L;
            
            // Clear authentication context
            SecurityContextHolder.clearContext();
            
            // Act & Assert
            mockMvc.perform(get("/api/contests/private/{contestId}/leaderboard", contestId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
            
            // Verify services were not called
            verify(accessValidator, never()).canAccess(any(), any());
            verify(leaderboardService, never()).getLeaderboard(any());
        }

        @Test
        @DisplayName("Should handle service exception gracefully")
        void testGetLeaderboard_ServiceException() throws Exception {
            // Arrange
            Long contestId = 501L;
            
            // Mock access validation
            when(accessValidator.canAccess(contestId, hostUser.getId())).thenReturn(true);
            
            // Mock service exception
            when(leaderboardService.getLeaderboard(contestId))
                    .thenThrow(new RuntimeException("Cache unavailable"));
            
            // Set authentication context
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(hostUser, null, hostUser.getAuthorities())
            );
            
            // Act & Assert
            mockMvc.perform(get("/api/contests/private/{contestId}/leaderboard", contestId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError());
            
            // Verify interactions
            verify(accessValidator).canAccess(contestId, hostUser.getId());
            verify(leaderboardService).getLeaderboard(contestId);
        }

        @Test
        @DisplayName("Should verify Cache-Control headers with correct values")
        void testGetLeaderboard_CacheControlHeaders() throws Exception {
            // Arrange
            Long contestId = 501L;
            
            // Mock access validation
            when(accessValidator.canAccess(contestId, hostUser.getId())).thenReturn(true);
            
            // Mock leaderboard data
            List<LeaderboardEntry> leaderboard = Arrays.asList(
                    new LeaderboardEntry(55L, "Alice", "alice", 300.0, 3, 1)
            );
            when(leaderboardService.getLeaderboard(contestId)).thenReturn(leaderboard);
            
            // Set authentication context
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(hostUser, null, hostUser.getAuthorities())
            );
            
            // Act & Assert
            mockMvc.perform(get("/api/contests/private/{contestId}/leaderboard", contestId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    // Verify Cache-Control header contains max-age=10
                    .andExpect(header().string("Cache-Control", containsString("max-age=10")))
                    // Verify Cache-Control header contains must-revalidate
                    .andExpect(header().string("Cache-Control", containsString("must-revalidate")));
        }
    }
}
