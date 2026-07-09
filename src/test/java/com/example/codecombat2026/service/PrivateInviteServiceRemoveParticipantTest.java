package com.example.codecombat2026.service;

import com.example.codecombat2026.entity.Contest;
import com.example.codecombat2026.entity.PrivateContest;
import com.example.codecombat2026.entity.PrivateContestParticipant;
import com.example.codecombat2026.entity.User;
import com.example.codecombat2026.exception.ConflictException;
import com.example.codecombat2026.exception.ForbiddenException;
import com.example.codecombat2026.exception.ResourceNotFoundException;
import com.example.codecombat2026.repository.PrivateContestParticipantRepository;
import com.example.codecombat2026.repository.PrivateContestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PrivateInviteService - removeParticipant method.
 * 
 * Tests participant removal functionality including:
 * - Successful removal by contest host
 * - Access control validation (host-only operations)
 * - Contest status validation (no removal during LIVE contests)
 * - Error handling for non-existent contests/participants
 * 
 * Requirements: 7.4, 7.5
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PrivateInviteService - Remove Participant Tests")
class PrivateInviteServiceRemoveParticipantTest {

    @Mock
    private PrivateContestRepository privateContestRepository;

    @Mock
    private PrivateContestParticipantRepository participantRepository;

    @InjectMocks
    private PrivateInviteService privateInviteService;

    private User hostUser;
    private User otherUser;
    private User participantUser;
    private Contest contest;
    private PrivateContest privateContest;
    private PrivateContestParticipant participant;

    private static final Long CONTEST_ID = 100L;
    private static final Long HOST_USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long PARTICIPANT_USER_ID = 3L;

    @BeforeEach
    void setUp() {
        // Set up host user
        hostUser = new User();
        hostUser.setId(HOST_USER_ID);
        hostUser.setUsername("host_user");

        // Set up other user (not the host)
        otherUser = new User();
        otherUser.setId(OTHER_USER_ID);
        otherUser.setUsername("other_user");

        // Set up participant user
        participantUser = new User();
        participantUser.setId(PARTICIPANT_USER_ID);
        participantUser.setUsername("participant_user");

        // Set up contest with UPCOMING status
        contest = new Contest();
        contest.setId(CONTEST_ID);
        contest.setName("Test Contest");
        contest.setStatus(Contest.ContestStatus.UPCOMING);
        contest.setStartTime(LocalDateTime.now().plusDays(1));
        contest.setEndTime(LocalDateTime.now().plusDays(1).plusHours(3));

        // Set up private contest
        privateContest = new PrivateContest();
        privateContest.setId(1L);
        privateContest.setContest(contest);
        privateContest.setHostUser(hostUser);
        privateContest.setEnableProctoring(false);
        privateContest.setCancelled(false);

        // Set up participant
        participant = new PrivateContestParticipant();
        participant.setId(1L);
        participant.setContest(contest);
        participant.setUser(participantUser);
        participant.setJoinedAt(LocalDateTime.now().minusDays(5));
    }

    // ─── Successful Removal Tests ──────────────────────────────────────────

    @Test
    @DisplayName("removeParticipant - Successful removal by host")
    void removeParticipant_SuccessfulRemoval() {
        // Given
        when(privateContestRepository.findByContestId(CONTEST_ID))
                .thenReturn(Optional.of(privateContest));
        when(participantRepository.findByContestIdAndUserId(CONTEST_ID, PARTICIPANT_USER_ID))
                .thenReturn(Optional.of(participant));

        // When
        privateInviteService.removeParticipant(CONTEST_ID, PARTICIPANT_USER_ID, HOST_USER_ID);

        // Then
        verify(privateContestRepository).findByContestId(CONTEST_ID);
        verify(participantRepository).findByContestIdAndUserId(CONTEST_ID, PARTICIPANT_USER_ID);
        verify(participantRepository).delete(participant);
    }

    @Test
    @DisplayName("removeParticipant - Successful removal from UPCOMING contest")
    void removeParticipant_UpcomingContest_Success() {
        // Given - contest is UPCOMING (default in setup)
        when(privateContestRepository.findByContestId(CONTEST_ID))
                .thenReturn(Optional.of(privateContest));
        when(participantRepository.findByContestIdAndUserId(CONTEST_ID, PARTICIPANT_USER_ID))
                .thenReturn(Optional.of(participant));

        // When
        privateInviteService.removeParticipant(CONTEST_ID, PARTICIPANT_USER_ID, HOST_USER_ID);

        // Then
        verify(participantRepository).delete(participant);
    }

    @Test
    @DisplayName("removeParticipant - Successful removal from ENDED contest")
    void removeParticipant_EndedContest_Success() {
        // Given - contest is ENDED
        contest.setStatus(Contest.ContestStatus.ENDED);
        when(privateContestRepository.findByContestId(CONTEST_ID))
                .thenReturn(Optional.of(privateContest));
        when(participantRepository.findByContestIdAndUserId(CONTEST_ID, PARTICIPANT_USER_ID))
                .thenReturn(Optional.of(participant));

        // When
        privateInviteService.removeParticipant(CONTEST_ID, PARTICIPANT_USER_ID, HOST_USER_ID);

        // Then
        verify(participantRepository).delete(participant);
    }

    // ─── Access Control Tests ───────────────────────────────────────────────

    @Test
    @DisplayName("removeParticipant - Non-host attempting removal should fail")
    void removeParticipant_NonHost_ThrowsForbiddenException() {
        // Given
        when(privateContestRepository.findByContestId(CONTEST_ID))
                .thenReturn(Optional.of(privateContest));

        // When & Then
        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> {
            privateInviteService.removeParticipant(CONTEST_ID, PARTICIPANT_USER_ID, OTHER_USER_ID);
        });

        assertEquals("Only the contest host can remove participants", exception.getMessage());
        verify(privateContestRepository).findByContestId(CONTEST_ID);
        verify(participantRepository, never()).findByContestIdAndUserId(any(), any());
        verify(participantRepository, never()).delete(any());
    }

    @Test
    @DisplayName("removeParticipant - Participant attempting to remove themselves should fail")
    void removeParticipant_ParticipantRemovingSelf_ThrowsForbiddenException() {
        // Given - participant trying to remove themselves
        when(privateContestRepository.findByContestId(CONTEST_ID))
                .thenReturn(Optional.of(privateContest));

        // When & Then
        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> {
            privateInviteService.removeParticipant(CONTEST_ID, PARTICIPANT_USER_ID, PARTICIPANT_USER_ID);
        });

        assertEquals("Only the contest host can remove participants", exception.getMessage());
        verify(participantRepository, never()).delete(any());
    }

    // ─── Contest Status Validation Tests ────────────────────────────────────

    @Test
    @DisplayName("removeParticipant - Removal during LIVE contest should fail")
    void removeParticipant_LiveContest_ThrowsConflictException() {
        // Given - contest is LIVE
        contest.setStatus(Contest.ContestStatus.LIVE);
        when(privateContestRepository.findByContestId(CONTEST_ID))
                .thenReturn(Optional.of(privateContest));

        // When & Then
        ConflictException exception = assertThrows(ConflictException.class, () -> {
            privateInviteService.removeParticipant(CONTEST_ID, PARTICIPANT_USER_ID, HOST_USER_ID);
        });

        assertEquals("Cannot remove participants from a contest that has already started", 
                exception.getMessage());
        verify(privateContestRepository).findByContestId(CONTEST_ID);
        verify(participantRepository, never()).findByContestIdAndUserId(any(), any());
        verify(participantRepository, never()).delete(any());
    }

    // ─── Resource Not Found Tests ───────────────────────────────────────────

    @Test
    @DisplayName("removeParticipant - Non-existent private contest should fail")
    void removeParticipant_ContestNotFound_ThrowsResourceNotFoundException() {
        // Given
        when(privateContestRepository.findByContestId(CONTEST_ID))
                .thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            privateInviteService.removeParticipant(CONTEST_ID, PARTICIPANT_USER_ID, HOST_USER_ID);
        });

        assertEquals("Private contest not found", exception.getMessage());
        verify(privateContestRepository).findByContestId(CONTEST_ID);
        verify(participantRepository, never()).findByContestIdAndUserId(any(), any());
        verify(participantRepository, never()).delete(any());
    }

    @Test
    @DisplayName("removeParticipant - Non-existent participant should fail")
    void removeParticipant_ParticipantNotFound_ThrowsResourceNotFoundException() {
        // Given
        when(privateContestRepository.findByContestId(CONTEST_ID))
                .thenReturn(Optional.of(privateContest));
        when(participantRepository.findByContestIdAndUserId(CONTEST_ID, PARTICIPANT_USER_ID))
                .thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            privateInviteService.removeParticipant(CONTEST_ID, PARTICIPANT_USER_ID, HOST_USER_ID);
        });

        assertEquals("Participant not found in this contest", exception.getMessage());
        verify(privateContestRepository).findByContestId(CONTEST_ID);
        verify(participantRepository).findByContestIdAndUserId(CONTEST_ID, PARTICIPANT_USER_ID);
        verify(participantRepository, never()).delete(any());
    }

    @Test
    @DisplayName("removeParticipant - Removing non-existent participant from valid contest should fail")
    void removeParticipant_ValidContestInvalidParticipant_ThrowsResourceNotFoundException() {
        // Given
        Long nonExistentUserId = 999L;
        when(privateContestRepository.findByContestId(CONTEST_ID))
                .thenReturn(Optional.of(privateContest));
        when(participantRepository.findByContestIdAndUserId(CONTEST_ID, nonExistentUserId))
                .thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            privateInviteService.removeParticipant(CONTEST_ID, nonExistentUserId, HOST_USER_ID);
        });

        assertEquals("Participant not found in this contest", exception.getMessage());
        verify(participantRepository, never()).delete(any());
    }

    // ─── Edge Case Tests ────────────────────────────────────────────────────

    @Test
    @DisplayName("removeParticipant - Cancelled contest allows removal")
    void removeParticipant_CancelledContest_Success() {
        // Given - contest is cancelled but still UPCOMING
        privateContest.setCancelled(true);
        privateContest.setCancelledAt(LocalDateTime.now().minusHours(2));
        when(privateContestRepository.findByContestId(CONTEST_ID))
                .thenReturn(Optional.of(privateContest));
        when(participantRepository.findByContestIdAndUserId(CONTEST_ID, PARTICIPANT_USER_ID))
                .thenReturn(Optional.of(participant));

        // When
        privateInviteService.removeParticipant(CONTEST_ID, PARTICIPANT_USER_ID, HOST_USER_ID);

        // Then - removal should succeed because status is still UPCOMING
        verify(participantRepository).delete(participant);
    }

    @Test
    @DisplayName("removeParticipant - Contest with proctoring enabled allows removal")
    void removeParticipant_ProctoredContest_Success() {
        // Given - contest has proctoring enabled
        privateContest.setEnableProctoring(true);
        when(privateContestRepository.findByContestId(CONTEST_ID))
                .thenReturn(Optional.of(privateContest));
        when(participantRepository.findByContestIdAndUserId(CONTEST_ID, PARTICIPANT_USER_ID))
                .thenReturn(Optional.of(participant));

        // When
        privateInviteService.removeParticipant(CONTEST_ID, PARTICIPANT_USER_ID, HOST_USER_ID);

        // Then - removal should succeed regardless of proctoring setting
        verify(participantRepository).delete(participant);
    }

    @Test
    @DisplayName("removeParticipant - Verifies correct repository methods are called")
    void removeParticipant_VerifyRepositoryCalls() {
        // Given
        when(privateContestRepository.findByContestId(CONTEST_ID))
                .thenReturn(Optional.of(privateContest));
        when(participantRepository.findByContestIdAndUserId(CONTEST_ID, PARTICIPANT_USER_ID))
                .thenReturn(Optional.of(participant));

        // When
        privateInviteService.removeParticipant(CONTEST_ID, PARTICIPANT_USER_ID, HOST_USER_ID);

        // Then - verify exact sequence of repository calls
        verify(privateContestRepository, times(1)).findByContestId(CONTEST_ID);
        verify(participantRepository, times(1)).findByContestIdAndUserId(CONTEST_ID, PARTICIPANT_USER_ID);
        verify(participantRepository, times(1)).delete(participant);
        verifyNoMoreInteractions(privateContestRepository);
        verifyNoMoreInteractions(participantRepository);
    }
}
