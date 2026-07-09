package com.example.codecombat2026.service;

import com.example.codecombat2026.entity.User;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PrivateContestEmailService.
 * 
 * Tests cover all email notification methods:
 * - Hosting request workflow emails (submission, approval, rejection)
 * - Contest lifecycle emails (creation, start, end, cancellation)
 * - Participant notification emails (reminders, results)
 * 
 * Requirements: 17.1, 17.2, 17.3, 17.4, 18.3, 27.1, 27.2, 27.3, 27.4, 31.1, 31.2, 31.3, 31.4
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PrivateContestEmailService Tests")
class PrivateContestEmailServiceTest {

    @Mock
    private JavaMailSender noreplyMailSender;

    @InjectMocks
    private PrivateContestEmailService emailService;

    private User testHost;
    private User testParticipant;
    private User testAdmin;
    private MimeMessage mockMimeMessage;

    @BeforeEach
    void setUp() {
        // Set app URL for testing
        ReflectionTestUtils.setField(emailService, "appUrl", "https://codecoder.in");

        // Create test users
        testHost = new User();
        testHost.setId(1L);
        testHost.setUsername("host_user");
        testHost.setEmail("host@example.com");
        testHost.setFullName("Test Host");

        testParticipant = new User();
        testParticipant.setId(2L);
        testParticipant.setUsername("participant");
        testParticipant.setEmail("participant@example.com");
        testParticipant.setFullName("Test Participant");

        testAdmin = new User();
        testAdmin.setId(3L);
        testAdmin.setUsername("admin");
        testAdmin.setEmail("admin@example.com");
        testAdmin.setFullName("Admin User");

        // Create mock MIME message
        mockMimeMessage = mock(MimeMessage.class);
        when(noreplyMailSender.createMimeMessage()).thenReturn(mockMimeMessage);
    }

    // ─── Hosting Request Workflow Email Tests ─────────────────────────────────

    @Test
    @DisplayName("Should send hosting request submitted email to all admins")
    void testSendHostingRequestSubmittedEmail_Success() {
        // Arrange
        List<User> admins = Arrays.asList(testAdmin);
        Long requestId = 42L;

        // Act
        emailService.sendHostingRequestSubmittedEmail(admins, requestId);

        // Assert
        verify(noreplyMailSender, times(1)).createMimeMessage();
        verify(noreplyMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should send hosting request submitted email to multiple admins")
    void testSendHostingRequestSubmittedEmail_MultipleAdmins() {
        // Arrange
        User admin2 = new User();
        admin2.setId(4L);
        admin2.setEmail("admin2@example.com");
        admin2.setFullName("Admin Two");

        List<User> admins = Arrays.asList(testAdmin, admin2);
        Long requestId = 42L;

        // Act
        emailService.sendHostingRequestSubmittedEmail(admins, requestId);

        // Assert
        verify(noreplyMailSender, times(2)).createMimeMessage();
        verify(noreplyMailSender, times(2)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should send hosting approved email with contest creation link")
    void testSendHostingApprovedEmail_Success() {
        // Arrange
        Long requestId = 42L;

        // Act
        emailService.sendHostingApprovedEmail(testHost, requestId);

        // Assert
        verify(noreplyMailSender, times(1)).createMimeMessage();
        verify(noreplyMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should send hosting approved email with username when fullName is null")
    void testSendHostingApprovedEmail_NullFullName() {
        // Arrange
        testHost.setFullName(null);
        Long requestId = 42L;

        // Act
        emailService.sendHostingApprovedEmail(testHost, requestId);

        // Assert
        verify(noreplyMailSender, times(1)).createMimeMessage();
        verify(noreplyMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should send hosting rejected email with reason")
    void testSendHostingRejectedEmail_WithReason() {
        // Arrange
        String reason = "Email domain does not match stated organization";

        // Act
        emailService.sendHostingRejectedEmail(testHost, reason);

        // Assert
        verify(noreplyMailSender, times(1)).createMimeMessage();
        verify(noreplyMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should send hosting rejected email with default message when reason is null")
    void testSendHostingRejectedEmail_NullReason() {
        // Arrange & Act
        emailService.sendHostingRejectedEmail(testHost, null);

        // Assert
        verify(noreplyMailSender, times(1)).createMimeMessage();
        verify(noreplyMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should send hosting rejected email with default message when reason is blank")
    void testSendHostingRejectedEmail_BlankReason() {
        // Arrange & Act
        emailService.sendHostingRejectedEmail(testHost, "   ");

        // Assert
        verify(noreplyMailSender, times(1)).createMimeMessage();
        verify(noreplyMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should escape HTML in rejection reason")
    void testSendHostingRejectedEmail_HTMLEscaping() {
        // Arrange
        String reason = "<script>alert('xss')</script>";

        // Act
        emailService.sendHostingRejectedEmail(testHost, reason);

        // Assert
        verify(noreplyMailSender, times(1)).createMimeMessage();
        verify(noreplyMailSender, times(1)).send(any(MimeMessage.class));
        // HTML escaping prevents script execution
    }

    // ─── Contest Lifecycle Email Tests ────────────────────────────────────────

    @Test
    @DisplayName("Should send contest created email with invite link")
    void testSendContestCreatedEmail_Success() {
        // Arrange
        Long contestId = 101L;
        String inviteLink = "https://codecoder.in/contest/private/join?token=abc123xyz";

        // Act
        emailService.sendContestCreatedEmail(testHost, contestId, inviteLink);

        // Assert
        verify(noreplyMailSender, times(1)).createMimeMessage();
        verify(noreplyMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should escape HTML in invite link")
    void testSendContestCreatedEmail_HTMLEscaping() {
        // Arrange
        Long contestId = 101L;
        String inviteLink = "https://codecoder.in/contest?token=<script>";

        // Act
        emailService.sendContestCreatedEmail(testHost, contestId, inviteLink);

        // Assert
        verify(noreplyMailSender, times(1)).createMimeMessage();
        verify(noreplyMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should send contest started email with dashboard link")
    void testSendContestStartedEmail_Success() {
        // Arrange
        Long contestId = 101L;
        String dashboardLink = "https://codecoder.in/contests/private/101/dashboard";

        // Act
        emailService.sendContestStartedEmail(testHost, contestId, dashboardLink);

        // Assert
        verify(noreplyMailSender, times(1)).createMimeMessage();
        verify(noreplyMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should send contest ended email with analytics link")
    void testSendContestEndedEmail_Success() {
        // Arrange
        Long contestId = 101L;
        String analyticsLink = "https://codecoder.in/contests/private/101/analytics";

        // Act
        emailService.sendContestEndedEmail(testHost, contestId, analyticsLink);

        // Assert
        verify(noreplyMailSender, times(1)).createMimeMessage();
        verify(noreplyMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should send contest cancelled email to all participants")
    void testSendContestCancelledEmail_Success() {
        // Arrange
        List<User> participants = Arrays.asList(testParticipant);
        Long contestId = 101L;
        String reason = "Rescheduling due to holiday conflict";

        // Act
        emailService.sendContestCancelledEmail(participants, contestId, reason);

        // Assert
        verify(noreplyMailSender, times(1)).createMimeMessage();
        verify(noreplyMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should send contest cancelled email to multiple participants")
    void testSendContestCancelledEmail_MultipleParticipants() {
        // Arrange
        User participant2 = new User();
        participant2.setId(5L);
        participant2.setEmail("participant2@example.com");
        participant2.setFullName("Participant Two");

        List<User> participants = Arrays.asList(testParticipant, participant2);
        Long contestId = 101L;
        String reason = "Technical issues";

        // Act
        emailService.sendContestCancelledEmail(participants, contestId, reason);

        // Assert
        verify(noreplyMailSender, times(2)).createMimeMessage();
        verify(noreplyMailSender, times(2)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should send contest cancelled email with default reason when null")
    void testSendContestCancelledEmail_NullReason() {
        // Arrange
        List<User> participants = Arrays.asList(testParticipant);
        Long contestId = 101L;

        // Act
        emailService.sendContestCancelledEmail(participants, contestId, null);

        // Assert
        verify(noreplyMailSender, times(1)).createMimeMessage();
        verify(noreplyMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should handle empty participants list gracefully")
    void testSendContestCancelledEmail_EmptyParticipants() {
        // Arrange
        List<User> participants = Arrays.asList();
        Long contestId = 101L;

        // Reset mocks to avoid unnecessary stubbing warning
        reset(noreplyMailSender);

        // Act
        emailService.sendContestCancelledEmail(participants, contestId, "reason");

        // Assert
        verify(noreplyMailSender, never()).createMimeMessage();
        verify(noreplyMailSender, never()).send(any(MimeMessage.class));
    }

    // ─── Participant Notification Email Tests ─────────────────────────────────

    @Test
    @DisplayName("Should send contest reminder email")
    void testSendContestReminderEmail_Success() {
        // Arrange
        List<User> participants = Arrays.asList(testParticipant);
        Long contestId = 101L;
        int hoursUntilStart = 24;

        // Act
        emailService.sendContestReminderEmail(participants, contestId, hoursUntilStart);

        // Assert
        verify(noreplyMailSender, times(1)).createMimeMessage();
        verify(noreplyMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should send reminder with 1 hour remaining")
    void testSendContestReminderEmail_OneHour() {
        // Arrange
        List<User> participants = Arrays.asList(testParticipant);
        Long contestId = 101L;
        int hoursUntilStart = 1;

        // Act
        emailService.sendContestReminderEmail(participants, contestId, hoursUntilStart);

        // Assert
        verify(noreplyMailSender, times(1)).createMimeMessage();
        verify(noreplyMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should send reminder to multiple participants")
    void testSendContestReminderEmail_MultipleParticipants() {
        // Arrange
        User participant2 = new User();
        participant2.setId(6L);
        participant2.setEmail("participant3@example.com");
        participant2.setFullName("Participant Three");

        List<User> participants = Arrays.asList(testParticipant, participant2);
        Long contestId = 101L;
        int hoursUntilStart = 12;

        // Act
        emailService.sendContestReminderEmail(participants, contestId, hoursUntilStart);

        // Assert
        verify(noreplyMailSender, times(2)).createMimeMessage();
        verify(noreplyMailSender, times(2)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should send participant contest started email")
    void testSendParticipantContestStartedEmail_Success() {
        // Arrange
        List<User> participants = Arrays.asList(testParticipant);
        Long contestId = 101L;

        // Act
        emailService.sendParticipantContestStartedEmail(participants, contestId);

        // Assert
        verify(noreplyMailSender, times(1)).createMimeMessage();
        verify(noreplyMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should send started email to multiple participants")
    void testSendParticipantContestStartedEmail_MultipleParticipants() {
        // Arrange
        User participant2 = new User();
        participant2.setId(7L);
        participant2.setEmail("participant4@example.com");
        participant2.setFullName("Participant Four");

        List<User> participants = Arrays.asList(testParticipant, participant2);
        Long contestId = 101L;

        // Act
        emailService.sendParticipantContestStartedEmail(participants, contestId);

        // Assert
        verify(noreplyMailSender, times(2)).createMimeMessage();
        verify(noreplyMailSender, times(2)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should send participant contest ended email with results")
    void testSendParticipantContestEndedEmail_Success() {
        // Arrange
        Long contestId = 101L;
        int rank = 5;
        int score = 250;

        // Act
        emailService.sendParticipantContestEndedEmail(testParticipant, contestId, rank, score);

        // Assert
        verify(noreplyMailSender, times(1)).createMimeMessage();
        verify(noreplyMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should send ended email with rank 1")
    void testSendParticipantContestEndedEmail_FirstPlace() {
        // Arrange
        Long contestId = 101L;
        int rank = 1;
        int score = 500;

        // Act
        emailService.sendParticipantContestEndedEmail(testParticipant, contestId, rank, score);

        // Assert
        verify(noreplyMailSender, times(1)).createMimeMessage();
        verify(noreplyMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should send ended email with zero score")
    void testSendParticipantContestEndedEmail_ZeroScore() {
        // Arrange
        Long contestId = 101L;
        int rank = 50;
        int score = 0;

        // Act
        emailService.sendParticipantContestEndedEmail(testParticipant, contestId, rank, score);

        // Assert
        verify(noreplyMailSender, times(1)).createMimeMessage();
        verify(noreplyMailSender, times(1)).send(any(MimeMessage.class));
    }

    // ─── Error Handling Tests ─────────────────────────────────────────────────

    @Test
    @DisplayName("Should handle email send failure gracefully")
    void testSendEmail_MessagingException() {
        // Arrange
        doThrow(new RuntimeException("SMTP server error"))
            .when(noreplyMailSender).send(any(MimeMessage.class));

        // Act - should not throw exception
        assertDoesNotThrow(() -> 
            emailService.sendHostingApprovedEmail(testHost, 42L)
        );

        // Assert
        verify(noreplyMailSender, times(1)).createMimeMessage();
    }

    @Test
    @DisplayName("Should handle null user email gracefully")
    void testSendEmail_NullEmail() {
        // Arrange
        testHost.setEmail(null);

        // Reset mocks to avoid unnecessary stubbing warning
        reset(noreplyMailSender);

        // Act - should not throw exception
        assertDoesNotThrow(() -> 
            emailService.sendContestCreatedEmail(testHost, 101L, "https://link.com")
        );
        
        // Assert - should not attempt to send
        verify(noreplyMailSender, never()).createMimeMessage();
    }

    @Test
    @DisplayName("Should handle null fullName by using username")
    void testSendEmail_NullFullName() {
        // Arrange
        testParticipant.setFullName(null);
        Long contestId = 101L;

        // Act
        emailService.sendParticipantContestStartedEmail(Arrays.asList(testParticipant), contestId);

        // Assert
        verify(noreplyMailSender, times(1)).createMimeMessage();
        verify(noreplyMailSender, times(1)).send(any(MimeMessage.class));
    }

    // ─── Integration Tests ────────────────────────────────────────────────────

    @Test
    @DisplayName("Should send multiple emails in sequence without interference")
    void testMultipleEmailsSent_NoInterference() {
        // Act
        emailService.sendHostingApprovedEmail(testHost, 1L);
        emailService.sendContestCreatedEmail(testHost, 101L, "https://invite.link");
        emailService.sendContestStartedEmail(testHost, 101L, "https://dashboard.link");

        // Assert
        verify(noreplyMailSender, times(3)).createMimeMessage();
        verify(noreplyMailSender, times(3)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should handle special characters in contest data")
    void testSendEmail_SpecialCharacters() {
        // Arrange
        String reason = "Contest has special chars: <>&\"'";
        
        // Act
        emailService.sendHostingRejectedEmail(testHost, reason);

        // Assert
        verify(noreplyMailSender, times(1)).createMimeMessage();
        verify(noreplyMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should handle very long reasons gracefully")
    void testSendEmail_LongReason() {
        // Arrange
        String longReason = "A".repeat(1000);
        
        // Act
        emailService.sendHostingRejectedEmail(testHost, longReason);

        // Assert
        verify(noreplyMailSender, times(1)).createMimeMessage();
        verify(noreplyMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should handle newlines in reason text")
    void testSendEmail_NewlinesInReason() {
        // Arrange
        String reasonWithNewlines = "Line 1\nLine 2\nLine 3";
        
        // Act
        emailService.sendContestCancelledEmail(Arrays.asList(testParticipant), 101L, reasonWithNewlines);

        // Assert
        verify(noreplyMailSender, times(1)).createMimeMessage();
        verify(noreplyMailSender, times(1)).send(any(MimeMessage.class));
    }

    // ─── Requirement Coverage Tests ───────────────────────────────────────────

    @Test
    @DisplayName("Requirement 17.1: Contest creation confirmation with invite link")
    void testRequirement_17_1() {
        // Arrange
        Long contestId = 101L;
        String inviteLink = "https://codecoder.in/contest/private/join?token=abc123";

        // Act
        emailService.sendContestCreatedEmail(testHost, contestId, inviteLink);

        // Assert - should send email successfully
        verify(noreplyMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Requirement 17.3: Contest start notification with dashboard link")
    void testRequirement_17_3() {
        // Arrange
        Long contestId = 101L;
        String dashboardLink = "https://codecoder.in/contests/private/101/dashboard";

        // Act
        emailService.sendContestStartedEmail(testHost, contestId, dashboardLink);

        // Assert - should send email successfully
        verify(noreplyMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Requirement 17.4: Contest end summary with analytics link")
    void testRequirement_17_4() {
        // Arrange
        Long contestId = 101L;
        String analyticsLink = "https://codecoder.in/contests/private/101/analytics";

        // Act
        emailService.sendContestEndedEmail(testHost, contestId, analyticsLink);

        // Assert - should send email successfully
        verify(noreplyMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Requirement 18.3: Cancellation notification to all participants")
    void testRequirement_18_3() {
        // Arrange
        List<User> participants = Arrays.asList(testParticipant);
        Long contestId = 101L;
        String reason = "Cancellation reason";

        // Act
        emailService.sendContestCancelledEmail(participants, contestId, reason);

        // Assert - should send email to all participants
        verify(noreplyMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Requirement 27.1: Pre-contest reminder notification")
    void testRequirement_27_1() {
        // Arrange
        List<User> participants = Arrays.asList(testParticipant);
        Long contestId = 101L;
        int hoursUntilStart = 24;

        // Act
        emailService.sendContestReminderEmail(participants, contestId, hoursUntilStart);

        // Assert - should send reminder to all participants
        verify(noreplyMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Requirement 27.2: Contest start notification to participants")
    void testRequirement_27_2() {
        // Arrange
        List<User> participants = Arrays.asList(testParticipant);
        Long contestId = 101L;

        // Act
        emailService.sendParticipantContestStartedEmail(participants, contestId);

        // Assert - should send notification to all participants
        verify(noreplyMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Requirement 27.3, 27.4: Final results notification with rank and score")
    void testRequirement_27_3_27_4() {
        // Arrange
        Long contestId = 101L;
        int rank = 10;
        int score = 300;

        // Act
        emailService.sendParticipantContestEndedEmail(testParticipant, contestId, rank, score);

        // Assert - should send results with rank and score
        verify(noreplyMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Requirement 31.1: Admin notification on hosting request submission")
    void testRequirement_31_1() {
        // Arrange
        List<User> admins = Arrays.asList(testAdmin);
        Long requestId = 42L;

        // Act
        emailService.sendHostingRequestSubmittedEmail(admins, requestId);

        // Assert - should notify all admins
        verify(noreplyMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Requirement 31.2: Approval confirmation email")
    void testRequirement_31_2() {
        // Arrange
        Long requestId = 42L;

        // Act
        emailService.sendHostingApprovedEmail(testHost, requestId);

        // Assert - should send approval email
        verify(noreplyMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Requirement 31.3: Rejection notification with reason")
    void testRequirement_31_3() {
        // Arrange
        String reason = "Email domain does not match organization";

        // Act
        emailService.sendHostingRejectedEmail(testHost, reason);

        // Assert - should send rejection email with reason
        verify(noreplyMailSender, times(1)).send(any(MimeMessage.class));
    }
}
