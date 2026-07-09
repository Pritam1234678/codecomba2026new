package com.example.codecombat2026.scheduler;

import com.example.codecombat2026.entity.Contest;
import com.example.codecombat2026.entity.PrivateContest;
import com.example.codecombat2026.entity.User;
import com.example.codecombat2026.repository.ContestRepository;
import com.example.codecombat2026.repository.PrivateContestParticipantRepository;
import com.example.codecombat2026.repository.PrivateContestRepository;
import com.example.codecombat2026.repository.SubmissionRepository;
import com.example.codecombat2026.repository.UserRepository;
import com.example.codecombat2026.service.ContestService;
import com.example.codecombat2026.service.PrivateContestCacheService;
import com.example.codecombat2026.service.PrivateContestEmailService;
import com.example.codecombat2026.service.PrivateContestLeaderboardService;
import com.example.codecombat2026.service.SseEmitterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ContestStatusScheduler.
 * 
 * Tests the scheduler's ability to:
 * - Transition contest statuses (UPCOMING → LIVE → ENDED)
 * - Initialize leaderboards for private contests on LIVE transition
 * - Persist leaderboards for private contests on ENDED transition
 * - Send email notifications to hosts on status changes
 * 
 * Requirements: 12.1, 12.2, 12.3, 12.4, 12.5, 17.3, 17.4
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ContestStatusScheduler Tests")
@MockitoSettings(strictness = Strictness.LENIENT)
class ContestStatusSchedulerTest {

    @Mock
    private ContestRepository contestRepository;

    @Mock
    private ContestService contestService;

    @Mock
    private SseEmitterRegistry sseRegistry;

    @Mock
    private PrivateContestRepository privateContestRepository;

    @Mock
    private PrivateContestCacheService privateContestCacheService;

    @Mock
    private PrivateContestLeaderboardService privateContestLeaderboardService;

    @Mock
    private PrivateContestEmailService privateContestEmailService;

    @Mock
    private PrivateContestParticipantRepository privateContestParticipantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @InjectMocks
    private ContestStatusScheduler scheduler;

    private Contest publicContest;
    private Contest privateContestEntity;
    private PrivateContest privateContest;
    private User hostUser;

    @BeforeEach
    void setUp() {
        // Set APP_URL for email templates
        ReflectionTestUtils.setField(scheduler, "appUrl", "https://test.codecoder.in");

        // Create a public contest
        publicContest = new Contest();
        publicContest.setId(1L);
        publicContest.setName("Public Contest");
        publicContest.setStatus(Contest.ContestStatus.UPCOMING);
        publicContest.setStartTime(LocalDateTime.now().minusMinutes(10));
        publicContest.setEndTime(LocalDateTime.now().plusHours(2));
        publicContest.setActive(true);

        // Create a private contest entity
        privateContestEntity = new Contest();
        privateContestEntity.setId(2L);
        privateContestEntity.setName("Private Contest");
        privateContestEntity.setStatus(Contest.ContestStatus.UPCOMING);
        privateContestEntity.setStartTime(LocalDateTime.now().minusMinutes(5));
        privateContestEntity.setEndTime(LocalDateTime.now().plusHours(1));
        privateContestEntity.setActive(true);

        // Create host user
        hostUser = new User();
        hostUser.setId(100L);
        hostUser.setUsername("test_host");
        hostUser.setEmail("host@test.com");
        hostUser.setFullName("Test Host");

        // Create private contest metadata
        privateContest = new PrivateContest();
        privateContest.setId(1L);
        privateContest.setContest(privateContestEntity);
        privateContest.setHostUser(hostUser);
        privateContest.setEnableProctoring(false);
        privateContest.setCancelled(false);
        privateContest.setCreatedAt(LocalDateTime.now().minusDays(1));
    }

    @Test
    @DisplayName("Should transition public contest from UPCOMING to LIVE")
    void shouldTransitionPublicContestToLive() {
        // Arrange
        when(contestRepository.findAll()).thenReturn(Arrays.asList(publicContest));
        when(privateContestRepository.findByContestId(1L)).thenReturn(Optional.empty());

        // Act
        scheduler.updateContestStatuses();

        // Assert
        verify(contestRepository).save(publicContest);
        assertEquals(Contest.ContestStatus.LIVE, publicContest.getStatus());
        verify(contestService).evictContest(1L);
        verify(contestService).evictContestCache();
        
        // Should not call private contest services for public contests
        verify(privateContestLeaderboardService, never()).initializeLeaderboard(anyLong());
        verify(privateContestEmailService, never()).sendContestStartedEmail(any(), anyLong(), anyString(), anyString(), anyString(), anyInt(), anyString());
    }

    @Test
    @DisplayName("Should initialize leaderboard when private contest transitions to LIVE")
    void shouldInitializeLeaderboardOnPrivateContestStart() {
        // Arrange
        when(contestRepository.findAll()).thenReturn(Arrays.asList(privateContestEntity));
        when(privateContestRepository.findByContestId(2L)).thenReturn(Optional.of(privateContest));
        when(userRepository.findById(100L)).thenReturn(Optional.of(hostUser));
        when(privateContestParticipantRepository.countByContestId(2L)).thenReturn(25L);

        // Act
        scheduler.updateContestStatuses();

        // Assert
        // Verify contest status changed
        verify(contestRepository).save(privateContestEntity);
        assertEquals(Contest.ContestStatus.LIVE, privateContestEntity.getStatus());

        // Verify private contest cache invalidated
        verify(privateContestCacheService).invalidateContestCache(2L);

        // Verify leaderboard initialized (Requirement 12.4)
        verify(privateContestLeaderboardService).initializeLeaderboard(2L);

        // Verify email sent to host (Requirement 17.3)
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        ArgumentCaptor<Long> contestIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> contestNameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> participantCountCaptor = ArgumentCaptor.forClass(Integer.class);

        verify(privateContestEmailService).sendContestStartedEmail(
            userCaptor.capture(),
            contestIdCaptor.capture(),
            contestNameCaptor.capture(),
            anyString(),
            anyString(),
            participantCountCaptor.capture(),
            anyString()
        );

        assertEquals(hostUser.getId(), userCaptor.getValue().getId());
        assertEquals(2L, contestIdCaptor.getValue());
        assertEquals("Private Contest", contestNameCaptor.getValue());
        assertEquals(25, participantCountCaptor.getValue());
    }

    @Test
    @DisplayName("Should persist leaderboard when private contest transitions to ENDED")
    void shouldPersistLeaderboardOnPrivateContestEnd() {
        // Arrange
        privateContestEntity.setStatus(Contest.ContestStatus.LIVE);
        privateContestEntity.setEndTime(LocalDateTime.now().minusMinutes(5));

        when(contestRepository.findAll()).thenReturn(Arrays.asList(privateContestEntity));
        when(privateContestRepository.findByContestId(2L)).thenReturn(Optional.of(privateContest));
        when(userRepository.findById(100L)).thenReturn(Optional.of(hostUser));
        when(privateContestParticipantRepository.countByContestId(2L)).thenReturn(30L);
        when(submissionRepository.countByContestId(2L)).thenReturn(150L);

        // Act
        scheduler.updateContestStatuses();

        // Assert
        // Verify contest status changed
        verify(contestRepository).save(privateContestEntity);
        assertEquals(Contest.ContestStatus.ENDED, privateContestEntity.getStatus());
        assertFalse(privateContestEntity.getActive());

        // Verify private contest cache invalidated
        verify(privateContestCacheService).invalidateContestCache(2L);

        // Verify leaderboard persisted (Requirement 12.5)
        verify(privateContestLeaderboardService).persistLeaderboard(2L);

        // Verify email sent to host (Requirement 17.4)
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        ArgumentCaptor<Long> contestIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> contestNameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> participantCountCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> submissionCountCaptor = ArgumentCaptor.forClass(Integer.class);

        verify(privateContestEmailService).sendContestEndedEmail(
            userCaptor.capture(),
            contestIdCaptor.capture(),
            contestNameCaptor.capture(),
            anyString(),
            participantCountCaptor.capture(),
            submissionCountCaptor.capture(),
            anyString()
        );

        assertEquals(hostUser.getId(), userCaptor.getValue().getId());
        assertEquals(2L, contestIdCaptor.getValue());
        assertEquals("Private Contest", contestNameCaptor.getValue());
        assertEquals(30, participantCountCaptor.getValue());
        assertEquals(150, submissionCountCaptor.getValue());
    }

    @Test
    @DisplayName("Should handle leaderboard initialization failure gracefully")
    void shouldHandleLeaderboardInitializationFailure() {
        // Arrange
        when(contestRepository.findAll()).thenReturn(Arrays.asList(privateContestEntity));
        when(privateContestRepository.findByContestId(2L)).thenReturn(Optional.of(privateContest));
        when(userRepository.findById(100L)).thenReturn(Optional.of(hostUser));
        when(privateContestParticipantRepository.countByContestId(2L)).thenReturn(25L);
        
        // Simulate leaderboard initialization failure
        doThrow(new RuntimeException("Redis connection failed")).when(privateContestLeaderboardService).initializeLeaderboard(2L);

        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> scheduler.updateContestStatuses());

        // Verify contest still transitioned
        assertEquals(Contest.ContestStatus.LIVE, privateContestEntity.getStatus());

        // Verify email still sent even if leaderboard init failed
        verify(privateContestEmailService).sendContestStartedEmail(any(), eq(2L), anyString(), anyString(), anyString(), anyInt(), anyString());
    }

    @Test
    @DisplayName("Should handle email notification failure gracefully")
    void shouldHandleEmailNotificationFailure() {
        // Arrange
        when(contestRepository.findAll()).thenReturn(Arrays.asList(privateContestEntity));
        when(privateContestRepository.findByContestId(2L)).thenReturn(Optional.of(privateContest));
        when(userRepository.findById(100L)).thenReturn(Optional.of(hostUser));
        when(privateContestParticipantRepository.countByContestId(2L)).thenReturn(25L);
        
        // Simulate email sending failure
        doThrow(new RuntimeException("SMTP connection failed")).when(privateContestEmailService)
            .sendContestStartedEmail(any(), anyLong(), anyString(), anyString(), anyString(), anyInt(), anyString());

        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> scheduler.updateContestStatuses());

        // Verify contest still transitioned
        assertEquals(Contest.ContestStatus.LIVE, privateContestEntity.getStatus());

        // Verify leaderboard still initialized even if email failed
        verify(privateContestLeaderboardService).initializeLeaderboard(2L);
    }

    @Test
    @DisplayName("Should handle missing host user gracefully")
    void shouldHandleMissingHostUser() {
        // Arrange
        // Create a private contest with a null hostUser reference to simulate missing host
        PrivateContest privateContestWithoutHost = new PrivateContest();
        privateContestWithoutHost.setId(1L);
        privateContestWithoutHost.setContest(privateContestEntity);
        // Deliberately set hostUser to a user with just an ID (simulating lazy loading scenario)
        User lazyHost = new User();
        lazyHost.setId(100L);
        // Email and username are null (not loaded)
        privateContestWithoutHost.setHostUser(lazyHost);
        
        when(contestRepository.findAll()).thenReturn(Arrays.asList(privateContestEntity));
        when(privateContestRepository.findByContestId(2L)).thenReturn(Optional.of(privateContestWithoutHost));
        // Repository returns empty when trying to fetch the host
        when(userRepository.findById(100L)).thenReturn(Optional.empty());

        // Act
        assertDoesNotThrow(() -> scheduler.updateContestStatuses());

        // Assert
        // Verify contest still transitioned
        assertEquals(Contest.ContestStatus.LIVE, privateContestEntity.getStatus());

        // Verify leaderboard still initialized
        verify(privateContestLeaderboardService).initializeLeaderboard(2L);

        // Verify email not sent (no host found in repository)
        verify(privateContestEmailService, never()).sendContestStartedEmail(any(), anyLong(), anyString(), anyString(), anyString(), anyInt(), anyString());
    }

    @Test
    @DisplayName("Should not transition contest if times are null")
    void shouldNotTransitionContestWithNullTimes() {
        // Arrange
        publicContest.setStartTime(null);
        publicContest.setEndTime(null);
        when(contestRepository.findAll()).thenReturn(Arrays.asList(publicContest));

        // Act
        scheduler.updateContestStatuses();

        // Assert
        verify(contestRepository, never()).save(any());
        verify(contestService, never()).evictContest(anyLong());
    }

    @Test
    @DisplayName("Should process multiple contests in single scheduler run")
    void shouldProcessMultipleContests() {
        // Arrange
        when(contestRepository.findAll()).thenReturn(Arrays.asList(publicContest, privateContestEntity));
        when(privateContestRepository.findByContestId(1L)).thenReturn(Optional.empty());
        when(privateContestRepository.findByContestId(2L)).thenReturn(Optional.of(privateContest));
        when(userRepository.findById(100L)).thenReturn(Optional.of(hostUser));
        when(privateContestParticipantRepository.countByContestId(2L)).thenReturn(25L);

        // Act
        scheduler.updateContestStatuses();

        // Assert
        verify(contestRepository, times(2)).save(any());
        assertEquals(Contest.ContestStatus.LIVE, publicContest.getStatus());
        assertEquals(Contest.ContestStatus.LIVE, privateContestEntity.getStatus());
        
        // Verify private contest specific actions only called once
        verify(privateContestLeaderboardService, times(1)).initializeLeaderboard(2L);
        verify(privateContestEmailService, times(1)).sendContestStartedEmail(any(), eq(2L), anyString(), anyString(), anyString(), anyInt(), anyString());
    }
}
