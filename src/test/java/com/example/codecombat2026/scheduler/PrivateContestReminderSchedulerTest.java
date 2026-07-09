package com.example.codecombat2026.scheduler;

import com.example.codecombat2026.entity.Contest;
import com.example.codecombat2026.entity.PrivateContest;
import com.example.codecombat2026.entity.PrivateContestParticipant;
import com.example.codecombat2026.entity.User;
import com.example.codecombat2026.repository.ContestRepository;
import com.example.codecombat2026.repository.PrivateContestParticipantRepository;
import com.example.codecombat2026.repository.PrivateContestRepository;
import com.example.codecombat2026.repository.UserRepository;
import com.example.codecombat2026.service.PrivateContestEmailService;
import com.example.codecombat2026.util.TimeUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PrivateContestReminderScheduler.
 * 
 * Tests the scheduled reminder notifications including:
 * - 24-hour reminder sending
 * - 1-hour reminder sending
 * - Duplicate reminder prevention via Redis
 * - Cancelled contest handling
 * - Error handling during reminder operations
 * 
 * Requirements: 31.1, 31.2, 31.3, 31.4, 31.5
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PrivateContestReminderScheduler Tests")
class PrivateContestReminderSchedulerTest {

    @Mock
    private ContestRepository contestRepository;

    @Mock
    private PrivateContestRepository privateContestRepository;

    @Mock
    private PrivateContestParticipantRepository participantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PrivateContestEmailService emailService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private PrivateContestReminderScheduler scheduler;

    private Contest contest;
    private PrivateContest privateContest;
    private User participant1;
    private User participant2;
    private PrivateContestParticipant participantRecord1;
    private PrivateContestParticipant participantRecord2;

    @BeforeEach
    void setUp() {
        // Enable reminders by default
        ReflectionTestUtils.setField(scheduler, "reminderEnabled", true);

        // Mock Redis operations
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Setup test data
        LocalDateTime now = TimeUtil.now();
        
        contest = new Contest();
        contest.setId(100L);
        contest.setName("Test Private Contest");
        contest.setStatus(Contest.ContestStatus.UPCOMING);
        contest.setStartTime(now.plusHours(24));
        contest.setEndTime(now.plusHours(27));

        privateContest = new PrivateContest();
        privateContest.setId(1L);
        privateContest.setContest(contest);
        privateContest.setCancelled(false);

        participant1 = new User();
        participant1.setId(1L);
        participant1.setEmail("user1@test.com");
        participant1.setUsername("user1");
        participant1.setFullName("User One");

        participant2 = new User();
        participant2.setId(2L);
        participant2.setEmail("user2@test.com");
        participant2.setUsername("user2");
        participant2.setFullName("User Two");

        participantRecord1 = new PrivateContestParticipant();
        participantRecord1.setId(1L);
        participantRecord1.setContest(contest);
        participantRecord1.setUser(participant1);

        participantRecord2 = new PrivateContestParticipant();
        participantRecord2.setId(2L);
        participantRecord2.setContest(contest);
        participantRecord2.setUser(participant2);
    }

    @Test
    @DisplayName("Should send 24-hour reminder when contest starts in 24 hours")
    void testSend24HourReminder() {
        // Arrange
        LocalDateTime now = TimeUtil.now();
        contest.setStartTime(now.plusHours(24));
        
        when(contestRepository.findByStatus(Contest.ContestStatus.UPCOMING))
            .thenReturn(Collections.singletonList(contest));
        when(privateContestRepository.findByContestId(100L))
            .thenReturn(Optional.of(privateContest));
        when(redisTemplate.hasKey("private:contest:reminder:24h:100"))
            .thenReturn(false);
        when(participantRepository.findByContestId(100L))
            .thenReturn(Arrays.asList(participantRecord1, participantRecord2));
        when(userRepository.findById(1L)).thenReturn(Optional.of(participant1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(participant2));

        // Act
        scheduler.sendContestReminders();

        // Assert
        ArgumentCaptor<List<User>> participantsCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Long> contestIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Integer> hoursCaptor = ArgumentCaptor.forClass(Integer.class);
        
        verify(emailService, times(1)).sendContestReminderEmail(
            participantsCaptor.capture(),
            contestIdCaptor.capture(),
            hoursCaptor.capture()
        );
        
        assertThat(participantsCaptor.getValue()).hasSize(2);
        assertThat(contestIdCaptor.getValue()).isEqualTo(100L);
        assertThat(hoursCaptor.getValue()).isEqualTo(24);
        
        // Verify Redis key was set
        verify(valueOperations, times(1)).set(eq("private:contest:reminder:24h:100"), eq("sent"));
        verify(redisTemplate, times(1)).expire(eq("private:contest:reminder:24h:100"), anyLong(), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Should send 1-hour reminder when contest starts in 1 hour")
    void testSend1HourReminder() {
        // Arrange
        LocalDateTime now = TimeUtil.now();
        contest.setStartTime(now.plusMinutes(45)); // Within 0.5-1.5 hour window
        
        when(contestRepository.findByStatus(Contest.ContestStatus.UPCOMING))
            .thenReturn(Collections.singletonList(contest));
        when(privateContestRepository.findByContestId(100L))
            .thenReturn(Optional.of(privateContest));
        when(redisTemplate.hasKey("private:contest:reminder:1h:100"))
            .thenReturn(false);
        when(participantRepository.findByContestId(100L))
            .thenReturn(Arrays.asList(participantRecord1, participantRecord2));
        when(userRepository.findById(1L)).thenReturn(Optional.of(participant1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(participant2));

        // Act
        scheduler.sendContestReminders();

        // Assert
        ArgumentCaptor<Integer> hoursCaptor = ArgumentCaptor.forClass(Integer.class);
        
        verify(emailService, times(1)).sendContestReminderEmail(
            anyList(),
            eq(100L),
            hoursCaptor.capture()
        );
        
        assertThat(hoursCaptor.getValue()).isEqualTo(1);
        
        // Verify Redis key was set
        verify(valueOperations, times(1)).set(eq("private:contest:reminder:1h:100"), eq("sent"));
    }

    @Test
    @DisplayName("Should not send reminder if already sent (Redis key exists)")
    void testDoNotSendDuplicateReminder() {
        // Arrange
        LocalDateTime now = TimeUtil.now();
        contest.setStartTime(now.plusHours(24));
        
        when(contestRepository.findByStatus(Contest.ContestStatus.UPCOMING))
            .thenReturn(Collections.singletonList(contest));
        when(privateContestRepository.findByContestId(100L))
            .thenReturn(Optional.of(privateContest));
        when(redisTemplate.hasKey("private:contest:reminder:24h:100"))
            .thenReturn(true); // Already sent

        // Act
        scheduler.sendContestReminders();

        // Assert
        verify(emailService, never()).sendContestReminderEmail(anyList(), anyLong(), anyInt());
        verify(valueOperations, never()).set(anyString(), anyString());
    }

    @Test
    @DisplayName("Should skip cancelled contests")
    void testSkipCancelledContest() {
        // Arrange
        LocalDateTime now = TimeUtil.now();
        contest.setStartTime(now.plusHours(24));
        privateContest.setCancelled(true); // Contest is cancelled
        
        when(contestRepository.findByStatus(Contest.ContestStatus.UPCOMING))
            .thenReturn(Collections.singletonList(contest));
        when(privateContestRepository.findByContestId(100L))
            .thenReturn(Optional.of(privateContest));

        // Act
        scheduler.sendContestReminders();

        // Assert
        verify(emailService, never()).sendContestReminderEmail(anyList(), anyLong(), anyInt());
        verify(redisTemplate, never()).hasKey(anyString());
    }

    @Test
    @DisplayName("Should skip public contests (no PrivateContest record)")
    void testSkipPublicContest() {
        // Arrange
        LocalDateTime now = TimeUtil.now();
        contest.setStartTime(now.plusHours(24));
        
        when(contestRepository.findByStatus(Contest.ContestStatus.UPCOMING))
            .thenReturn(Collections.singletonList(contest));
        when(privateContestRepository.findByContestId(100L))
            .thenReturn(Optional.empty()); // Not a private contest

        // Act
        scheduler.sendContestReminders();

        // Assert
        verify(emailService, never()).sendContestReminderEmail(anyList(), anyLong(), anyInt());
    }

    @Test
    @DisplayName("Should handle contest with no participants gracefully")
    void testHandleNoParticipants() {
        // Arrange
        LocalDateTime now = TimeUtil.now();
        contest.setStartTime(now.plusHours(24));
        
        when(contestRepository.findByStatus(Contest.ContestStatus.UPCOMING))
            .thenReturn(Collections.singletonList(contest));
        when(privateContestRepository.findByContestId(100L))
            .thenReturn(Optional.of(privateContest));
        when(redisTemplate.hasKey("private:contest:reminder:24h:100"))
            .thenReturn(false);
        when(participantRepository.findByContestId(100L))
            .thenReturn(Collections.emptyList());

        // Act
        scheduler.sendContestReminders();

        // Assert
        verify(emailService, never()).sendContestReminderEmail(anyList(), anyLong(), anyInt());
        // Should still mark as sent to avoid repeated checks
        verify(valueOperations, times(1)).set(eq("private:contest:reminder:24h:100"), eq("sent"));
    }

    @Test
    @DisplayName("Should skip participants without valid email")
    void testSkipParticipantsWithoutEmail() {
        // Arrange
        LocalDateTime now = TimeUtil.now();
        contest.setStartTime(now.plusHours(24));
        
        participant1.setEmail(null); // No email
        participant2.setEmail("   "); // Blank email
        
        when(contestRepository.findByStatus(Contest.ContestStatus.UPCOMING))
            .thenReturn(Collections.singletonList(contest));
        when(privateContestRepository.findByContestId(100L))
            .thenReturn(Optional.of(privateContest));
        when(redisTemplate.hasKey("private:contest:reminder:24h:100"))
            .thenReturn(false);
        when(participantRepository.findByContestId(100L))
            .thenReturn(Arrays.asList(participantRecord1, participantRecord2));
        when(userRepository.findById(1L)).thenReturn(Optional.of(participant1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(participant2));

        // Act
        scheduler.sendContestReminders();

        // Assert
        // Should not send email if no valid participants
        verify(emailService, never()).sendContestReminderEmail(anyList(), anyLong(), anyInt());
    }

    @Test
    @DisplayName("Should handle repository errors gracefully")
    void testHandleRepositoryError() {
        // Arrange
        when(contestRepository.findByStatus(Contest.ContestStatus.UPCOMING))
            .thenThrow(new RuntimeException("Database error"));

        // Act - should not throw exception
        scheduler.sendContestReminders();

        // Assert
        verify(emailService, never()).sendContestReminderEmail(anyList(), anyLong(), anyInt());
        // Error should be logged but not propagated
    }

    @Test
    @DisplayName("Should not run when reminders are disabled")
    void testSkipWhenRemindersDisabled() {
        // Arrange
        ReflectionTestUtils.setField(scheduler, "reminderEnabled", false);

        // Act
        scheduler.sendContestReminders();

        // Assert
        verify(contestRepository, never()).findByStatus(any());
        verify(emailService, never()).sendContestReminderEmail(anyList(), anyLong(), anyInt());
    }

    @Test
    @DisplayName("Should skip contests outside reminder windows")
    void testSkipContestsOutsideReminderWindows() {
        // Arrange
        LocalDateTime now = TimeUtil.now();
        contest.setStartTime(now.plusHours(5)); // 5 hours away - not in any window
        
        when(contestRepository.findByStatus(Contest.ContestStatus.UPCOMING))
            .thenReturn(Collections.singletonList(contest));
        when(privateContestRepository.findByContestId(100L))
            .thenReturn(Optional.of(privateContest));

        // Act
        scheduler.sendContestReminders();

        // Assert
        verify(redisTemplate, never()).hasKey(anyString());
        verify(emailService, never()).sendContestReminderEmail(anyList(), anyLong(), anyInt());
    }

    @Test
    @DisplayName("Should handle lazy-loaded user proxies")
    void testHandleLazyLoadedUserProxies() {
        // Arrange
        LocalDateTime now = TimeUtil.now();
        contest.setStartTime(now.plusHours(24));
        
        // Simulate lazy-loaded user without email loaded
        User lazyUser = new User();
        lazyUser.setId(1L);
        lazyUser.setEmail(null); // Email not loaded
        lazyUser.setUsername(null); // Username not loaded
        
        PrivateContestParticipant participantWithLazyUser = new PrivateContestParticipant();
        participantWithLazyUser.setId(1L);
        participantWithLazyUser.setContest(contest);
        participantWithLazyUser.setUser(lazyUser);
        
        when(contestRepository.findByStatus(Contest.ContestStatus.UPCOMING))
            .thenReturn(Collections.singletonList(contest));
        when(privateContestRepository.findByContestId(100L))
            .thenReturn(Optional.of(privateContest));
        when(redisTemplate.hasKey("private:contest:reminder:24h:100"))
            .thenReturn(false);
        when(participantRepository.findByContestId(100L))
            .thenReturn(Collections.singletonList(participantWithLazyUser));
        when(userRepository.findById(1L)).thenReturn(Optional.of(participant1)); // Return fully loaded user

        // Act
        scheduler.sendContestReminders();

        // Assert
        ArgumentCaptor<List<User>> participantsCaptor = ArgumentCaptor.forClass(List.class);
        verify(emailService, times(1)).sendContestReminderEmail(
            participantsCaptor.capture(),
            eq(100L),
            eq(24)
        );
        
        assertThat(participantsCaptor.getValue()).hasSize(1);
        assertThat(participantsCaptor.getValue().get(0).getEmail()).isEqualTo("user1@test.com");
    }
}
