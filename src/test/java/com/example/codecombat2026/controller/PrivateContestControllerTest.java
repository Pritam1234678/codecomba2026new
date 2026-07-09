package com.example.codecombat2026.controller;

import com.example.codecombat2026.dto.PrivateContestDTO;
import com.example.codecombat2026.entity.Contest.ContestStatus;
import com.example.codecombat2026.exception.ForbiddenException;
import com.example.codecombat2026.exception.ResourceNotFoundException;
import com.example.codecombat2026.service.PrivateContestAccessValidator;
import com.example.codecombat2026.service.PrivateContestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test suite for PrivateContestController.
 * 
 * Tests:
 * - GET /api/contests/private/{id} - Get contest details
 * 
 * Verifies:
 * - Access control (host and participant can access, others cannot)
 * - Proctoring field is included in response (enableProctoring)
 * - Error handling (403 Forbidden, 404 Not Found)
 * 
 * Requirements: 11.4, 15.2 (Task 12.3)
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("PrivateContestController Tests")
class PrivateContestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PrivateContestService privateContestService;

    @MockBean
    private PrivateContestAccessValidator accessValidator;

    private PrivateContestDTO sampleContest;

    @BeforeEach
    void setUp() {
        sampleContest = new PrivateContestDTO();
        sampleContest.setId(1L);
        sampleContest.setContestId(100L);
        sampleContest.setName("Sample Private Contest");
        sampleContest.setDescription("A test contest");
        sampleContest.setStartTime(LocalDateTime.now().plusDays(1));
        sampleContest.setEndTime(LocalDateTime.now().plusDays(1).plusHours(3));
        sampleContest.setStatus(ContestStatus.UPCOMING);
        sampleContest.setHostUserId(42L);
        sampleContest.setHostUsername("contest_host");
        sampleContest.setEnableProctoring(true); // IMPORTANT: proctoring enabled
        sampleContest.setParticipantCount(15L);
        sampleContest.setCancelled(false);
        sampleContest.setCreatedAt(LocalDateTime.now().minusDays(5));
    }

    @Nested
    @DisplayName("GET /api/contests/private/{id}")
    class GetContestDetails {

        @Test
        @WithMockUser(username = "contest_host", roles = "USER")
        @DisplayName("Host can access their own contest - includes enableProctoring field")
        void hostCanAccessOwnContest() throws Exception {
            // Arrange
            Long contestId = 100L;
            doNothing().when(accessValidator).validateAccess(eq(contestId), anyLong());
            when(privateContestService.getPrivateContestById(contestId))
                    .thenReturn(sampleContest);

            // Act & Assert
            mockMvc.perform(get("/api/contests/private/{id}", contestId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.contestId").value(100L))
                    .andExpect(jsonPath("$.name").value("Sample Private Contest"))
                    .andExpect(jsonPath("$.enableProctoring").value(true)) // VERIFY proctoring field
                    .andExpect(jsonPath("$.hostUsername").value("contest_host"))
                    .andExpect(jsonPath("$.participantCount").value(15))
                    .andExpect(jsonPath("$.status").value("UPCOMING"));

            verify(accessValidator, times(1)).validateAccess(eq(contestId), anyLong());
            verify(privateContestService, times(1)).getPrivateContestById(contestId);
        }

        @Test
        @WithMockUser(username = "participant_user", roles = "USER")
        @DisplayName("Participant can access contest they joined - sees proctoring status")
        void participantCanAccessContest() throws Exception {
            // Arrange
            Long contestId = 100L;
            doNothing().when(accessValidator).validateAccess(eq(contestId), anyLong());
            when(privateContestService.getPrivateContestById(contestId))
                    .thenReturn(sampleContest);

            // Act & Assert
            mockMvc.perform(get("/api/contests/private/{id}", contestId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.contestId").value(100L))
                    .andExpect(jsonPath("$.enableProctoring").value(true)) // Participant sees proctoring status
                    .andExpect(jsonPath("$.name").value("Sample Private Contest"));

            verify(accessValidator, times(1)).validateAccess(eq(contestId), anyLong());
            verify(privateContestService, times(1)).getPrivateContestById(contestId);
        }

        @Test
        @WithMockUser(username = "unauthorized_user", roles = "USER")
        @DisplayName("Non-host, non-participant cannot access contest - 403 Forbidden")
        void unauthorizedUserCannotAccess() throws Exception {
            // Arrange
            Long contestId = 100L;
            doThrow(new ForbiddenException("You do not have permission to access this private contest"))
                    .when(accessValidator).validateAccess(eq(contestId), anyLong());

            // Act & Assert
            mockMvc.perform(get("/api/contests/private/{id}", contestId))
                    .andExpect(status().isForbidden());

            verify(accessValidator, times(1)).validateAccess(eq(contestId), anyLong());
            verify(privateContestService, never()).getPrivateContestById(anyLong());
        }

        @Test
        @WithMockUser(username = "user", roles = "USER")
        @DisplayName("Contest not found - 404 Not Found")
        void contestNotFound() throws Exception {
            // Arrange
            Long contestId = 999L;
            doNothing().when(accessValidator).validateAccess(eq(contestId), anyLong());
            when(privateContestService.getPrivateContestById(contestId))
                    .thenThrow(new ResourceNotFoundException("Private contest not found"));

            // Act & Assert
            mockMvc.perform(get("/api/contests/private/{id}", contestId))
                    .andExpect(status().isNotFound());

            verify(accessValidator, times(1)).validateAccess(eq(contestId), anyLong());
            verify(privateContestService, times(1)).getPrivateContestById(contestId);
        }

        @Test
        @DisplayName("Unauthenticated user cannot access - 401 Unauthorized")
        void unauthenticatedUserCannotAccess() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/contests/private/{id}", 100L))
                    .andExpect(status().isUnauthorized());

            verify(accessValidator, never()).validateAccess(anyLong(), anyLong());
            verify(privateContestService, never()).getPrivateContestById(anyLong());
        }

        @Test
        @WithMockUser(username = "host", roles = "USER")
        @DisplayName("Contest with proctoring disabled - enableProctoring is false")
        void contestWithoutProctoring() throws Exception {
            // Arrange
            Long contestId = 100L;
            sampleContest.setEnableProctoring(false); // Proctoring disabled
            
            doNothing().when(accessValidator).validateAccess(eq(contestId), anyLong());
            when(privateContestService.getPrivateContestById(contestId))
                    .thenReturn(sampleContest);

            // Act & Assert
            mockMvc.perform(get("/api/contests/private/{id}", contestId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.contestId").value(100L))
                    .andExpect(jsonPath("$.enableProctoring").value(false)); // Verify false value

            verify(accessValidator, times(1)).validateAccess(eq(contestId), anyLong());
            verify(privateContestService, times(1)).getPrivateContestById(contestId);
        }

        @Test
        @WithMockUser(username = "host", roles = "USER")
        @DisplayName("Live contest with proctoring - participant can see status")
        void liveContestWithProctoring() throws Exception {
            // Arrange
            Long contestId = 100L;
            sampleContest.setStatus(ContestStatus.LIVE);
            sampleContest.setEnableProctoring(true);
            
            doNothing().when(accessValidator).validateAccess(eq(contestId), anyLong());
            when(privateContestService.getPrivateContestById(contestId))
                    .thenReturn(sampleContest);

            // Act & Assert
            mockMvc.perform(get("/api/contests/private/{id}", contestId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("LIVE"))
                    .andExpect(jsonPath("$.enableProctoring").value(true)); // Proctoring visible during LIVE

            verify(accessValidator, times(1)).validateAccess(eq(contestId), anyLong());
            verify(privateContestService, times(1)).getPrivateContestById(contestId);
        }
    }
}
