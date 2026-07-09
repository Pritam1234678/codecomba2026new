package com.example.codecombat2026.scheduler;

import com.example.codecombat2026.repository.PrivateContestInvitationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for InviteTokenCleanupScheduler.
 * 
 * Tests the scheduled cleanup of expired invite tokens including:
 * - Successful cleanup with expired tokens
 * - No-op when no expired tokens exist
 * - Error handling during cleanup operations
 * 
 * Requirements: 26.1, 26.2
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InviteTokenCleanupScheduler Tests")
class InviteTokenCleanupSchedulerTest {

    @Mock
    private PrivateContestInvitationRepository invitationRepository;

    @InjectMocks
    private InviteTokenCleanupScheduler scheduler;

    @BeforeEach
    void setUp() {
        // Reset mocks before each test
        reset(invitationRepository);
    }

    @Test
    @DisplayName("Should delete expired tokens and log count")
    void testCleanupExpiredInvitations_WithExpiredTokens() {
        // Arrange
        long expectedDeletedCount = 5L;
        when(invitationRepository.deleteByExpiresAtBefore(any(LocalDateTime.class)))
            .thenReturn(expectedDeletedCount);

        // Act
        scheduler.cleanupExpiredInvitations();

        // Assert
        ArgumentCaptor<LocalDateTime> dateCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(invitationRepository, times(1)).deleteByExpiresAtBefore(dateCaptor.capture());
        
        // Verify that the captured date is approximately now (within 1 second)
        LocalDateTime capturedDate = dateCaptor.getValue();
        LocalDateTime now = LocalDateTime.now();
        assertThat(capturedDate).isBetween(now.minusSeconds(1), now.plusSeconds(1));
    }

    @Test
    @DisplayName("Should handle cleanup when no expired tokens exist")
    void testCleanupExpiredInvitations_NoExpiredTokens() {
        // Arrange
        when(invitationRepository.deleteByExpiresAtBefore(any(LocalDateTime.class)))
            .thenReturn(0L);

        // Act
        scheduler.cleanupExpiredInvitations();

        // Assert
        verify(invitationRepository, times(1)).deleteByExpiresAtBefore(any(LocalDateTime.class));
        // No exception should be thrown - this is a valid scenario
    }

    @Test
    @DisplayName("Should handle repository errors gracefully without propagating exception")
    void testCleanupExpiredInvitations_RepositoryError() {
        // Arrange
        when(invitationRepository.deleteByExpiresAtBefore(any(LocalDateTime.class)))
            .thenThrow(new RuntimeException("Database connection error"));

        // Act - should not throw exception
        scheduler.cleanupExpiredInvitations();

        // Assert
        verify(invitationRepository, times(1)).deleteByExpiresAtBefore(any(LocalDateTime.class));
        // Error should be logged but not propagated, allowing scheduler to continue
    }

    @Test
    @DisplayName("Should call repository with current timestamp")
    void testCleanupExpiredInvitations_UsesCurrentTimestamp() {
        // Arrange
        when(invitationRepository.deleteByExpiresAtBefore(any(LocalDateTime.class)))
            .thenReturn(3L);

        LocalDateTime beforeExecution = LocalDateTime.now();

        // Act
        scheduler.cleanupExpiredInvitations();

        LocalDateTime afterExecution = LocalDateTime.now();

        // Assert
        ArgumentCaptor<LocalDateTime> dateCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(invitationRepository, times(1)).deleteByExpiresAtBefore(dateCaptor.capture());
        
        LocalDateTime capturedDate = dateCaptor.getValue();
        
        // Verify the timestamp is between before and after execution (within a reasonable window)
        assertThat(capturedDate)
            .isAfterOrEqualTo(beforeExecution.minusSeconds(1))
            .isBeforeOrEqualTo(afterExecution.plusSeconds(1));
    }

    @Test
    @DisplayName("Should handle large number of expired tokens")
    void testCleanupExpiredInvitations_LargeCount() {
        // Arrange
        long largeCount = 10000L;
        when(invitationRepository.deleteByExpiresAtBefore(any(LocalDateTime.class)))
            .thenReturn(largeCount);

        // Act
        scheduler.cleanupExpiredInvitations();

        // Assert
        verify(invitationRepository, times(1)).deleteByExpiresAtBefore(any(LocalDateTime.class));
        // Should handle large numbers without issues
    }
}
