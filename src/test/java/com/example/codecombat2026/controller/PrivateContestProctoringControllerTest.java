package com.example.codecombat2026.controller;

import com.example.codecombat2026.entity.Submission;
import com.example.codecombat2026.entity.User;
import com.example.codecombat2026.exception.ForbiddenException;
import com.example.codecombat2026.proctoring.entity.EndReason;
import com.example.codecombat2026.proctoring.entity.ProctoringEvent;
import com.example.codecombat2026.proctoring.entity.ProctoringScreenshot;
import com.example.codecombat2026.proctoring.entity.ProctoringSession;
import com.example.codecombat2026.proctoring.entity.RiskBand;
import com.example.codecombat2026.proctoring.exception.ProctoringNotFoundException;
import com.example.codecombat2026.proctoring.repository.ProctoringEventRepository;
import com.example.codecombat2026.proctoring.repository.ProctoringScreenshotRepository;
import com.example.codecombat2026.proctoring.repository.ProctoringSessionRepository;
import com.example.codecombat2026.repository.SubmissionRepository;
import com.example.codecombat2026.repository.UserRepository;
import com.example.codecombat2026.security.services.UserDetailsImpl;
import com.example.codecombat2026.service.PrivateContestAccessValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test suite for PrivateContestProctoringController.
 * 
 * Tests:
 * 1. Host can access proctoring sessions for their contest
 * 2. Non-host users are denied access
 * 3. Admin users can access any contest's proctoring data
 * 4. Session detail endpoint validates session belongs to contest
 * 5. Flagged filter works correctly
 * 
 * Related: Task 12.2, Requirements 15.3, 15.4, 15.5
 */
@ExtendWith(MockitoExtension.class)
class PrivateContestProctoringControllerTest {

    @Mock
    private ProctoringSessionRepository sessionRepo;
    
    @Mock
    private ProctoringEventRepository eventRepo;
    
    @Mock
    private ProctoringScreenshotRepository screenshotRepo;
    
    @Mock
    private UserRepository userRepo;
    
    @Mock
    private SubmissionRepository submissionRepo;
    
    @Mock
    private StringRedisTemplate redis;
    
    @Mock
    private ValueOperations<String, String> valueOperations;
    
    @Mock
    private PrivateContestAccessValidator accessValidator;
    
    @InjectMocks
    private PrivateContestProctoringController controller;
    
    private UserDetailsImpl hostUser;
    private UserDetailsImpl nonHostUser;
    private UserDetailsImpl adminUser;
    
    @BeforeEach
    void setUp() {
        // Setup host user
        hostUser = UserDetailsImpl.build(createUser(1L, "host@test.com", "host"));
        
        // Setup non-host user
        nonHostUser = UserDetailsImpl.build(createUser(2L, "user@test.com", "user"));
        
        // Setup admin user
        User admin = createUser(3L, "admin@test.com", "admin");
        adminUser = new UserDetailsImpl(
                admin.getId(),
                admin.getUsername(),
                admin.getEmail(),
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        
        when(redis.opsForValue()).thenReturn(valueOperations);
    }
    
    @Test
    void testHostCanAccessProctoringSessionsForTheirContest() {
        // Given
        Long contestId = 100L;
        Long hostUserId = 1L;
        
        when(accessValidator.isHost(contestId, hostUserId)).thenReturn(true);
        
        ProctoringSession session = createSession(1L, contestId, 10L, RiskBand.LOW, 0, false);
        when(sessionRepo.findByContestIdOrderByIdAsc(contestId)).thenReturn(List.of(session));
        
        User participant = createUser(10L, "participant@test.com", "participant");
        when(userRepo.findAllById(anyList())).thenReturn(List.of(participant));
        
        // When
        ResponseEntity<List<PrivateContestProctoringController.LiveSessionRow>> response = 
                controller.listSessions(contestId, null, hostUser);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(session.getId(), response.getBody().get(0).sessionId());
        assertEquals("participant", response.getBody().get(0).username());
        
        verify(accessValidator).isHost(contestId, hostUserId);
        verify(sessionRepo).findByContestIdOrderByIdAsc(contestId);
    }
    
    @Test
    void testNonHostCannotAccessProctoringData() {
        // Given
        Long contestId = 100L;
        Long nonHostUserId = 2L;
        
        when(accessValidator.isHost(contestId, nonHostUserId)).thenReturn(false);
        
        // When/Then
        assertThrows(ForbiddenException.class, () -> {
            controller.listSessions(contestId, null, nonHostUser);
        });
        
        verify(accessValidator).isHost(contestId, nonHostUserId);
        verifyNoInteractions(sessionRepo);
    }
    
    @Test
    void testAdminCanAccessAnyContestProctoringData() {
        // Given
        Long contestId = 100L;
        Long adminUserId = 3L;
        
        // Admin should bypass host check
        ProctoringSession session = createSession(1L, contestId, 10L, RiskBand.HIGH, 85, true);
        when(sessionRepo.findByContestIdOrderByIdAsc(contestId)).thenReturn(List.of(session));
        
        User participant = createUser(10L, "participant@test.com", "participant");
        when(userRepo.findAllById(anyList())).thenReturn(List.of(participant));
        
        // When
        ResponseEntity<List<PrivateContestProctoringController.LiveSessionRow>> response = 
                controller.listSessions(contestId, null, adminUser);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(session.getId(), response.getBody().get(0).sessionId());
        assertTrue(response.getBody().get(0).flagged());
        
        // Admin doesn't need host validation
        verify(accessValidator, never()).isHost(any(), any());
        verify(sessionRepo).findByContestIdOrderByIdAsc(contestId);
    }
    
    @Test
    void testFlaggedFilterWorks() {
        // Given
        Long contestId = 100L;
        Long hostUserId = 1L;
        
        when(accessValidator.isHost(contestId, hostUserId)).thenReturn(true);
        
        // Create two sessions - one flagged, one not
        ProctoringSession flaggedSession = createSession(1L, contestId, 10L, RiskBand.HIGH, 90, true);
        ProctoringSession normalSession = createSession(2L, contestId, 11L, RiskBand.LOW, 10, false);
        when(sessionRepo.findByContestIdOrderByIdAsc(contestId))
                .thenReturn(List.of(flaggedSession, normalSession));
        
        User p1 = createUser(10L, "p1@test.com", "p1");
        User p2 = createUser(11L, "p2@test.com", "p2");
        when(userRepo.findAllById(anyList())).thenReturn(List.of(p1, p2));
        
        // When - filter for flagged only
        ResponseEntity<List<PrivateContestProctoringController.LiveSessionRow>> response = 
                controller.listSessions(contestId, true, hostUser);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(flaggedSession.getId(), response.getBody().get(0).sessionId());
        assertTrue(response.getBody().get(0).flagged());
    }
    
    @Test
    void testSessionDetailEndpoint() {
        // Given
        Long contestId = 100L;
        Long sessionId = 1L;
        Long hostUserId = 1L;
        Long participantUserId = 10L;
        
        when(accessValidator.isHost(contestId, hostUserId)).thenReturn(true);
        
        ProctoringSession session = createSession(sessionId, contestId, participantUserId, 
                RiskBand.MEDIUM, 50, false);
        session.setEndedAt(LocalDateTime.now());
        session.setEndReason(EndReason.CONTEST_ENDED);
        when(sessionRepo.findById(sessionId)).thenReturn(Optional.of(session));
        
        User participant = createUser(participantUserId, "participant@test.com", "participant");
        when(userRepo.findById(participantUserId)).thenReturn(Optional.of(participant));
        
        // Mock events, screenshots, submissions
        when(eventRepo.findBySessionIdOrderByServerTimestampAsc(sessionId))
                .thenReturn(Collections.emptyList());
        when(screenshotRepo.findBySessionIdOrderByCapturedAtAsc(sessionId))
                .thenReturn(Collections.emptyList());
        when(submissionRepo.findByUser_IdAndContest_IdAndSubmittedAtBetween(
                eq(participantUserId), eq(contestId), any(), any()))
                .thenReturn(Collections.emptyList());
        
        // When
        ResponseEntity<PrivateContestProctoringController.SessionDetail> response = 
                controller.sessionDetail(contestId, sessionId, hostUser);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(sessionId, response.getBody().session().id());
        assertEquals(contestId, response.getBody().session().contestId());
        assertEquals("participant", response.getBody().session().username());
        assertEquals("CONTEST_ENDED", response.getBody().session().endReason());
        
        verify(accessValidator).isHost(contestId, hostUserId);
        verify(sessionRepo).findById(sessionId);
    }
    
    @Test
    void testSessionDetailRejectsSessionFromDifferentContest() {
        // Given
        Long contestId = 100L;
        Long sessionId = 1L;
        Long hostUserId = 1L;
        Long differentContestId = 999L;
        
        when(accessValidator.isHost(contestId, hostUserId)).thenReturn(true);
        
        // Session belongs to a different contest
        ProctoringSession session = createSession(sessionId, differentContestId, 10L, 
                RiskBand.LOW, 0, false);
        when(sessionRepo.findById(sessionId)).thenReturn(Optional.of(session));
        
        // When/Then
        assertThrows(ForbiddenException.class, () -> {
            controller.sessionDetail(contestId, sessionId, hostUser);
        });
    }
    
    @Test
    void testSessionDetailThrowsNotFoundForInvalidSession() {
        // Given
        Long contestId = 100L;
        Long sessionId = 999L;
        Long hostUserId = 1L;
        
        when(accessValidator.isHost(contestId, hostUserId)).thenReturn(true);
        when(sessionRepo.findById(sessionId)).thenReturn(Optional.empty());
        
        // When/Then
        assertThrows(ProctoringNotFoundException.class, () -> {
            controller.sessionDetail(contestId, sessionId, hostUser);
        });
    }
    
    @Test
    void testEmptySessionListReturnsOk() {
        // Given
        Long contestId = 100L;
        Long hostUserId = 1L;
        
        when(accessValidator.isHost(contestId, hostUserId)).thenReturn(true);
        when(sessionRepo.findByContestIdOrderByIdAsc(contestId)).thenReturn(Collections.emptyList());
        
        // When
        ResponseEntity<List<PrivateContestProctoringController.LiveSessionRow>> response = 
                controller.listSessions(contestId, null, hostUser);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }
    
    // Helper methods
    
    private User createUser(Long id, String email, String username) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setUsername(username);
        user.setPassword("password");
        user.setEnabled(true);
        return user;
    }
    
    private ProctoringSession createSession(Long id, Long contestId, Long userId, 
                                           RiskBand band, int score, boolean flagged) {
        ProctoringSession session = new ProctoringSession();
        session.setId(id);
        session.setContestId(contestId);
        session.setUserId(userId);
        session.setStartedAt(LocalDateTime.now().minusHours(1));
        session.setRiskScore(score);
        session.setRiskBand(band);
        session.setFlagged(flagged);
        session.setClientIp("127.0.0.1");
        session.setConsentVersion(1);
        session.setResumeCount(0);
        return session;
    }
}
