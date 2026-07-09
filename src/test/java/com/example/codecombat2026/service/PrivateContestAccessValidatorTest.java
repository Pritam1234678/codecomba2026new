package com.example.codecombat2026.service;

import com.example.codecombat2026.repository.PrivateContestParticipantRepository;
import com.example.codecombat2026.repository.PrivateContestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PrivateContestAccessValidator service.
 * 
 * Tests access control validation logic for private contests:
 * - Host validation
 * - Participant validation
 * - Combined access validation
 * - Null input handling
 */
@ExtendWith(MockitoExtension.class)
class PrivateContestAccessValidatorTest {

    @Mock
    private PrivateContestRepository privateContestRepository;

    @Mock
    private PrivateContestParticipantRepository participantRepository;

    @InjectMocks
    private PrivateContestAccessValidator validator;

    private Long contestId;
    private Long userId;
    private Long otherUserId;

    @BeforeEach
    void setUp() {
        contestId = 100L;
        userId = 1L;
        otherUserId = 2L;
    }

    @Test
    void isHost_WhenUserIsHost_ReturnsTrue() {
        // Given
        when(privateContestRepository.existsByContestIdAndHostUserId(contestId, userId))
            .thenReturn(true);

        // When
        boolean result = validator.isHost(contestId, userId);

        // Then
        assertTrue(result);
        verify(privateContestRepository).existsByContestIdAndHostUserId(contestId, userId);
    }

    @Test
    void isHost_WhenUserIsNotHost_ReturnsFalse() {
        // Given
        when(privateContestRepository.existsByContestIdAndHostUserId(contestId, userId))
            .thenReturn(false);

        // When
        boolean result = validator.isHost(contestId, userId);

        // Then
        assertFalse(result);
        verify(privateContestRepository).existsByContestIdAndHostUserId(contestId, userId);
    }

    @Test
    void isHost_WhenContestIdIsNull_ReturnsFalse() {
        // When
        boolean result = validator.isHost(null, userId);

        // Then
        assertFalse(result);
        verifyNoInteractions(privateContestRepository);
    }

    @Test
    void isHost_WhenUserIdIsNull_ReturnsFalse() {
        // When
        boolean result = validator.isHost(contestId, null);

        // Then
        assertFalse(result);
        verifyNoInteractions(privateContestRepository);
    }

    @Test
    void isParticipant_WhenUserIsParticipant_ReturnsTrue() {
        // Given
        when(participantRepository.existsByContestIdAndUserId(contestId, userId))
            .thenReturn(true);

        // When
        boolean result = validator.isParticipant(contestId, userId);

        // Then
        assertTrue(result);
        verify(participantRepository).existsByContestIdAndUserId(contestId, userId);
    }

    @Test
    void isParticipant_WhenUserIsNotParticipant_ReturnsFalse() {
        // Given
        when(participantRepository.existsByContestIdAndUserId(contestId, userId))
            .thenReturn(false);

        // When
        boolean result = validator.isParticipant(contestId, userId);

        // Then
        assertFalse(result);
        verify(participantRepository).existsByContestIdAndUserId(contestId, userId);
    }

    @Test
    void isParticipant_WhenContestIdIsNull_ReturnsFalse() {
        // When
        boolean result = validator.isParticipant(null, userId);

        // Then
        assertFalse(result);
        verifyNoInteractions(participantRepository);
    }

    @Test
    void isParticipant_WhenUserIdIsNull_ReturnsFalse() {
        // When
        boolean result = validator.isParticipant(contestId, null);

        // Then
        assertFalse(result);
        verifyNoInteractions(participantRepository);
    }

    @Test
    void canAccess_WhenUserIsHost_ReturnsTrue() {
        // Given
        when(privateContestRepository.existsByContestIdAndHostUserId(contestId, userId))
            .thenReturn(true);
        // Don't stub participant check - short-circuit evaluation means it won't be called

        // When
        boolean result = validator.canAccess(contestId, userId);

        // Then
        assertTrue(result);
        verify(privateContestRepository).existsByContestIdAndHostUserId(contestId, userId);
        // Verify participant check was NOT called due to short-circuit evaluation
        verifyNoInteractions(participantRepository);
    }

    @Test
    void canAccess_WhenUserIsParticipant_ReturnsTrue() {
        // Given
        when(privateContestRepository.existsByContestIdAndHostUserId(contestId, userId))
            .thenReturn(false);
        when(participantRepository.existsByContestIdAndUserId(contestId, userId))
            .thenReturn(true);

        // When
        boolean result = validator.canAccess(contestId, userId);

        // Then
        assertTrue(result);
        verify(privateContestRepository).existsByContestIdAndHostUserId(contestId, userId);
        verify(participantRepository).existsByContestIdAndUserId(contestId, userId);
    }

    @Test
    void canAccess_WhenUserIsNeitherHostNorParticipant_ReturnsFalse() {
        // Given
        when(privateContestRepository.existsByContestIdAndHostUserId(contestId, userId))
            .thenReturn(false);
        when(participantRepository.existsByContestIdAndUserId(contestId, userId))
            .thenReturn(false);

        // When
        boolean result = validator.canAccess(contestId, userId);

        // Then
        assertFalse(result);
        verify(privateContestRepository).existsByContestIdAndHostUserId(contestId, userId);
        verify(participantRepository).existsByContestIdAndUserId(contestId, userId);
    }

    @Test
    void canAccess_WhenContestIdIsNull_ReturnsFalse() {
        // When
        boolean result = validator.canAccess(null, userId);

        // Then
        assertFalse(result);
        verifyNoInteractions(privateContestRepository, participantRepository);
    }

    @Test
    void canAccess_WhenUserIdIsNull_ReturnsFalse() {
        // When
        boolean result = validator.canAccess(contestId, null);

        // Then
        assertFalse(result);
        verifyNoInteractions(privateContestRepository, participantRepository);
    }

    @Test
    void canAccess_WhenBothIdsAreNull_ReturnsFalse() {
        // When
        boolean result = validator.canAccess(null, null);

        // Then
        assertFalse(result);
        verifyNoInteractions(privateContestRepository, participantRepository);
    }
}
