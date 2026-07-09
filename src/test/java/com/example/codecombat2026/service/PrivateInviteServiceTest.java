package com.example.codecombat2026.service;

import com.example.codecombat2026.entity.Contest;
import com.example.codecombat2026.entity.PrivateContest;
import com.example.codecombat2026.entity.PrivateContestInvitation;
import com.example.codecombat2026.entity.PrivateContestParticipant;
import com.example.codecombat2026.entity.User;
import com.example.codecombat2026.repository.PrivateContestParticipantRepository;
import com.example.codecombat2026.repository.PrivateContestRepository;
import com.example.codecombat2026.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PrivateInviteService.
 * 
 * Tests the participant join flow including:
 * - Token validation via InviteTokenService
 * - Participant limit enforcement
 * - Duplicate join prevention
 * - First participant notification
 * - Error handling for invalid tokens, expired tokens, full contests
 * 
 * Requirements: 6.3, 6.4, 6.5, 6.6, 6.7, 6.8, 17.2
 */
@ExtendWith(MockitoExtension.class)
class PrivateInviteServiceTest {

    @Mock
    private InviteTokenService inviteTokenService;

    @Mock
    private PrivateContestBusinessRules businessRules;

    @Mock
    private PrivateContestParticipantRepository participantRepository;

    @Mock
    private PrivateContestRepository privateContestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private PrivateInviteService privateInviteService;

    private Contest contest;
    private PrivateContest privateContest;
    private User user;
    private User hostUser;
    private PrivateContestInvitation invitation;
    private String validToken;
    private Long userId;
    private Long contestId;

    @BeforeEach
    void setUp() {
        // Set up test data
        contestId = 100L;
        userId = 42L;
        validToken = "abcdefghijklmnopqrstuvwxyz0123456789ABCDE";

        // Contest setup
        contest = new Contest();
        contest.setId(contestId);
        contest.setName("CS101 Midterm Exam");
        contest.setStatus(Contest.ContestStatus.UPCOMING);

        // Host user setup
        hostUser = new User();
        hostUser.setId(1L);
        hostUser.setUsername("prof_smith");
        hostUser.setEmail("prof.smith@university.edu");
        hostUser.setFullName("Professor Smith");

        // Private contest setup
        privateContest = new PrivateContest();
        privateContest.setId(50L);
        privateContest.setContest(contest);
        privateContest.setHostUser(hostUser);

        // Participant user setup
        user = new User();
        user.setId(userId);
        user.setUsername("alice_dev");
        user.setEmail("alice@example.com");
        user.setFullName("Alice Johnson");

        // Invitation setup
        invitation = new PrivateContestInvitation();
        invitation.setId(1L);
        invitation.setContest(contest);
        invitation.setToken(validToken);
        invitation.setCreatedAt(LocalDateTime.now());
        invitation.setExpiresAt(LocalDateTime.now().plusDays(30));
        invitation.setInvalidated(false);
    }

    // ─── Successful Join Flow Tests ─────────────────────────────────────────

    @Test
    void acceptInvite_WithValidToken_SuccessfullyCreatesParticipant() {
        // Given
        when(inviteTokenService.validateToken(validToken))
            .thenReturn(Optional.of(invitation));
        when(userRepository.findById(userId))
            .thenReturn(Optional.of(user));
        when(participantRepository.existsByContestIdAndUserId(contestId, userId))
            .thenReturn(false);
        when(participantRepository.countByContestId(contestId))
            .thenReturn(0L);
        
        PrivateContestParticipant savedParticipant = new PrivateContestParticipant();
        savedParticipant.setId(1L);
        savedParticipant.setContest(contest);
        savedParticipant.setUser(user);
        savedParticipant.setJoinedAt(LocalDateTime.now());
        
        when(participantRepository.save(any(PrivateContestParticipant.class)))
            .thenReturn(savedParticipant);
        when(privateContestRepository.findByContestId(contestId))
            .thenReturn(Optional.of(privateContest));

        // When
        PrivateContestParticipant result = privateInviteService.acceptInvite(userId, validToken);

        // Then
        assertNotNull(result);
        verify(inviteTokenService).validateToken(validToken);
        verify(businessRules).validateParticipantLimit(contestId);
        verify(participantRepository).save(any(PrivateContestParticipant.class));
    }

    @Test
    void acceptInvite_SetsCorrectParticipantFields() {
        // Given
        when(inviteTokenService.validateToken(validToken))
            .thenReturn(Optional.of(invitation));
        when(userRepository.findById(userId))
            .thenReturn(Optional.of(user));
        when(participantRepository.existsByContestIdAndUserId(contestId, userId))
            .thenReturn(false);
        when(participantRepository.countByContestId(contestId))
            .thenReturn(5L);
        
        ArgumentCaptor<PrivateContestParticipant> captor = ArgumentCaptor.forClass(PrivateContestParticipant.class);
        when(participantRepository.save(any(PrivateContestParticipant.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        privateInviteService.acceptInvite(userId, validToken);

        // Then
        verify(participantRepository).save(captor.capture());
        PrivateContestParticipant saved = captor.getValue();
        
        assertEquals(contest, saved.getContest());
        assertEquals(user, saved.getUser());
        assertNotNull(saved.getJoinedAt());
    }

    @Test
    void acceptInvite_CallsBusinessRulesValidation() {
        // Given
        when(inviteTokenService.validateToken(validToken))
            .thenReturn(Optional.of(invitation));
        when(userRepository.findById(userId))
            .thenReturn(Optional.of(user));
        when(participantRepository.existsByContestIdAndUserId(contestId, userId))
            .thenReturn(false);
        when(participantRepository.countByContestId(contestId))
            .thenReturn(10L);
        when(participantRepository.save(any(PrivateContestParticipant.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        privateInviteService.acceptInvite(userId, validToken);

        // Then
        verify(businessRules).validateParticipantLimit(contestId);
    }

    // ─── Invalid Token Tests ─────────────────────────────────────────────────

    @Test
    void acceptInvite_WithInvalidToken_ThrowsNotFoundException() {
        // Given
        when(inviteTokenService.validateToken(validToken))
            .thenReturn(Optional.empty());

        // When / Then
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> privateInviteService.acceptInvite(userId, validToken)
        );
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("This invitation link is invalid or has expired", exception.getReason());
        verify(userRepository, never()).findById(any());
        verify(participantRepository, never()).save(any());
    }

    @Test
    void acceptInvite_WithNullToken_ThrowsNotFoundException() {
        // Given
        when(inviteTokenService.validateToken(null))
            .thenReturn(Optional.empty());

        // When / Then
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> privateInviteService.acceptInvite(userId, null)
        );
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void acceptInvite_WithExpiredToken_ThrowsNotFoundException() {
        // Given
        invitation.setExpiresAt(LocalDateTime.now().minusDays(1));
        when(inviteTokenService.validateToken(validToken))
            .thenReturn(Optional.empty()); // validateToken returns empty for expired tokens

        // When / Then
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> privateInviteService.acceptInvite(userId, validToken)
        );
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    // ─── User Not Found Tests ─────────────────────────────────────────────────

    @Test
    void acceptInvite_WithNonExistentUser_ThrowsNotFoundException() {
        // Given
        when(inviteTokenService.validateToken(validToken))
            .thenReturn(Optional.of(invitation));
        when(userRepository.findById(userId))
            .thenReturn(Optional.empty());

        // When / Then
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> privateInviteService.acceptInvite(userId, validToken)
        );
        
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("User not found", exception.getReason());
        verify(participantRepository, never()).save(any());
    }

    // ─── Duplicate Join Tests ─────────────────────────────────────────────────

    @Test
    void acceptInvite_WhenUserAlreadyJoined_ThrowsConflictException() {
        // Given
        when(inviteTokenService.validateToken(validToken))
            .thenReturn(Optional.of(invitation));
        when(userRepository.findById(userId))
            .thenReturn(Optional.of(user));
        when(participantRepository.existsByContestIdAndUserId(contestId, userId))
            .thenReturn(true);

        // When / Then
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> privateInviteService.acceptInvite(userId, validToken)
        );
        
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("You have already joined this contest", exception.getReason());
        verify(businessRules, never()).validateParticipantLimit(any());
        verify(participantRepository, never()).save(any());
    }

    @Test
    void acceptInvite_WhenUniqueConstraintViolatedDuringSave_ThrowsConflictException() {
        // Given - race condition: check passes but save fails due to unique constraint
        when(inviteTokenService.validateToken(validToken))
            .thenReturn(Optional.of(invitation));
        when(userRepository.findById(userId))
            .thenReturn(Optional.of(user));
        when(participantRepository.existsByContestIdAndUserId(contestId, userId))
            .thenReturn(false);
        when(participantRepository.countByContestId(contestId))
            .thenReturn(10L);
        when(participantRepository.save(any(PrivateContestParticipant.class)))
            .thenThrow(new DataIntegrityViolationException("Duplicate key"));

        // When / Then
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> privateInviteService.acceptInvite(userId, validToken)
        );
        
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("You have already joined this contest", exception.getReason());
    }

    // ─── Participant Limit Tests ─────────────────────────────────────────────

    @Test
    void acceptInvite_WhenContestFull_ThrowsTooManyRequestsException() {
        // Given
        when(inviteTokenService.validateToken(validToken))
            .thenReturn(Optional.of(invitation));
        when(userRepository.findById(userId))
            .thenReturn(Optional.of(user));
        when(participantRepository.existsByContestIdAndUserId(contestId, userId))
            .thenReturn(false);
        
        doThrow(new ResponseStatusException(
            HttpStatus.TOO_MANY_REQUESTS,
            "This contest has reached its maximum capacity of 100 participants"
        )).when(businessRules).validateParticipantLimit(contestId);

        // When / Then
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> privateInviteService.acceptInvite(userId, validToken)
        );
        
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getStatusCode());
        assertTrue(exception.getReason().contains("maximum capacity"));
        verify(participantRepository, never()).save(any());
    }

    // ─── First Participant Notification Tests ─────────────────────────────────

    @Test
    void acceptInvite_WhenFirstParticipant_SendsEmailToHost() {
        // Given
        when(inviteTokenService.validateToken(validToken))
            .thenReturn(Optional.of(invitation));
        when(userRepository.findById(userId))
            .thenReturn(Optional.of(user));
        when(participantRepository.existsByContestIdAndUserId(contestId, userId))
            .thenReturn(false);
        when(participantRepository.countByContestId(contestId))
            .thenReturn(0L); // First participant
        when(participantRepository.save(any(PrivateContestParticipant.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(privateContestRepository.findByContestId(contestId))
            .thenReturn(Optional.of(privateContest));

        // When
        privateInviteService.acceptInvite(userId, validToken);

        // Then
        verify(emailService).sendFirstParticipantJoinedEmail(
            eq(hostUser.getEmail()),
            eq(hostUser.getFullName()),
            eq(contest.getName()),
            contestId, eq(user.getUsername()), validToken
        );
    }

    @Test
    void acceptInvite_WhenNotFirstParticipant_DoesNotSendEmail() {
        // Given
        when(inviteTokenService.validateToken(validToken))
            .thenReturn(Optional.of(invitation));
        when(userRepository.findById(userId))
            .thenReturn(Optional.of(user));
        when(participantRepository.existsByContestIdAndUserId(contestId, userId))
            .thenReturn(false);
        when(participantRepository.countByContestId(contestId))
            .thenReturn(35L); // Not the first participant
        when(participantRepository.save(any(PrivateContestParticipant.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        privateInviteService.acceptInvite(userId, validToken);

        // Then
        verify(emailService, never()).sendFirstParticipantJoinedEmail(
            anyString(), anyString(), anyString(), anyString()
        );
    }

    @Test
    void acceptInvite_WhenEmailServiceFails_StillCreatesParticipant() {
        // Given - email service throws exception
        when(inviteTokenService.validateToken(validToken))
            .thenReturn(Optional.of(invitation));
        when(userRepository.findById(userId))
            .thenReturn(Optional.of(user));
        when(participantRepository.existsByContestIdAndUserId(contestId, userId))
            .thenReturn(false);
        when(participantRepository.countByContestId(contestId))
            .thenReturn(0L);
        
        PrivateContestParticipant savedParticipant = new PrivateContestParticipant();
        savedParticipant.setId(1L);
        savedParticipant.setContest(contest);
        savedParticipant.setUser(user);
        
        when(participantRepository.save(any(PrivateContestParticipant.class)))
            .thenReturn(savedParticipant);
        when(privateContestRepository.findByContestId(contestId))
            .thenReturn(Optional.of(privateContest));
        
        doThrow(new RuntimeException("Email service unavailable"))
            .when(emailService).sendFirstParticipantJoinedEmail(
                anyString(), anyString(), anyString(), anyString()
            );

        // When
        PrivateContestParticipant result = privateInviteService.acceptInvite(userId, validToken);

        // Then - participant should still be created
        assertNotNull(result);
        verify(participantRepository).save(any(PrivateContestParticipant.class));
    }
}
