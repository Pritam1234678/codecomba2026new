package com.example.codecombat2026.service;

import com.example.codecombat2026.entity.User;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Email notification service for Private Contest Hosting feature.
 * 
 * Provides asynchronous email notifications for:
 * - Contest hosting request workflow (submission, approval, rejection)
 * - Contest lifecycle events (creation, start, end, cancellation)
 * - Participant notifications (reminders, results)
 * - Host notifications (first participant joined, contest updates)
 * 
 * All emails are sent via SendPulse relay using noreply@codecoder.in.
 * Methods are @Async to avoid blocking the main thread during SMTP operations.
 * 
 * Requirements: 17.1, 17.2, 17.3, 17.4, 18.3, 27.1, 27.2, 27.3, 27.4, 31.1, 31.2, 31.3, 31.4
 * 
 * @see EmailService for transactional email patterns
 */
@Service
public class PrivateContestEmailService {

    private static final Logger log = LoggerFactory.getLogger(PrivateContestEmailService.class);

    private static final String NOREPLY_FROM = "noreply@codecoder.in";

    @Autowired
    @Qualifier("noreplyMailSender")
    private JavaMailSender noreplyMailSender;

    @Value("${APP_URL:https://codecoder.in}")
    private String appUrl;

    // ─── Helper Methods ───────────────────────────────────────────────────────

    /**
     * Load {@code classpath:email-templates/{name}.html}, inject {@code APP_URL},
     * then replace every {@code {{KEY}}} token with the corresponding value from
     * the varargs pairs {@code (key0, val0, key1, val1, ...)}.
     */
    private String loadTemplate(String templateName, String... replacements) {
        try {
            ClassPathResource resource = new ClassPathResource("email-templates/" + templateName + ".html");
            try (InputStream is = resource.getInputStream()) {
                String html = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                html = html.replace("{{APP_URL}}", appUrl);
                for (int i = 0; i + 1 < replacements.length; i += 2) {
                    html = html.replace("{{" + replacements[i] + "}}", replacements[i + 1]);
                }
                return html;
            }
        } catch (IOException e) {
            log.error("Email template '{}' not found: {}", templateName, e.getMessage());
            return "<p>Please visit <a href='" + appUrl + "'>" + appUrl + "</a></p>";
        }
    }

    /**
     * Send HTML email with retry logic (inherited from JavaMailSender).
     * Non-blocking when called from @Async methods.
     */
    private void sendEmail(String to, String subject, String htmlBody) {
        // Skip sending if recipient email is null or blank
        if (to == null || to.isBlank()) {
            log.warn("Skipping email send - recipient address is null or blank");
            return;
        }
        
        try {
            MimeMessage msg = noreplyMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(NOREPLY_FROM);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            noreplyMailSender.send(msg);
            log.info("Email sent to {}: {}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            // Non-fatal - don't throw to avoid blocking business logic
        }
    }

    /**
     * Wrap email content in a standard HTML template with consistent styling.
     * 
     * @param title The title/heading for the email
     * @param content The main content HTML (already escaped where necessary)
     * @return Complete HTML email with template structure
     */
    private String buildEmailHtml(String title, String content) {
        return "<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width,initial-scale=1.0'>"
                + "<title>" + HtmlUtils.htmlEscape(title) + "</title></head>"
                + "<body style='margin:0;padding:0;font-family:Arial,sans-serif;background-color:#1c1c1c;'>"
                + "<div style='max-width:600px;margin:40px auto;background:#242424;border:1px solid #3a3a3a;border-radius:8px;padding:32px;'>"
                + "<h1 style='color:#f1bc8b;margin:0 0 24px 0;font-size:24px;'>" + HtmlUtils.htmlEscape(title) + "</h1>"
                + content
                + "<hr style='border:none;border-top:1px solid#3a3a3a;margin:24px 0;'>"
                + "<p style='color:#8a8a8a;font-size:12px;'>This is an automated email from <a href='" + appUrl + "' style='color:#f1bc8b;'>CodeCoder</a>. "
                + "Please do not reply to this email.</p>"
                + "</div></body></html>";
    }

    /**
     * Format timestamp for email display.
     */
    private String formatTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z").format(new Date());
    }

    // ─── Hosting Request Workflow Emails ──────────────────────────────────────

    /**
     * Notify admins when a new hosting request is submitted.
     * 
     * Requirement: 1.5 - Admin notification on hosting request submission
     * 
     * @param admins List of admin users to notify
     * @param requestId ID of the hosting request for dashboard link
     * @param username Username of the requester
     * @param useCase Intended use case for hosting
     */
    @Async
    public void sendHostingRequestSubmittedEmail(List<User> admins, Long requestId, String username, String useCase) {
        String subject = "New Contest Hosting Request | CodeCoder";
        String dashboardUrl = appUrl + "/admin/hosting-requests";
        
        for (User admin : admins) {
            String html = loadTemplate("hosting-request-submitted",
                "REQUEST_ID", requestId.toString(),
                "USERNAME", HtmlUtils.htmlEscape(username),
                "TIMESTAMP", formatTimestamp(),
                "USE_CASE", HtmlUtils.htmlEscape(useCase),
                "DASHBOARD_URL", dashboardUrl,
                "ADMIN_EMAIL", admin.getEmail()
            );
            sendEmail(admin.getEmail(), subject, html);
        }
    }

    /**
     * Notify user that their hosting request has been approved.
     * 
     * Requirement: 2.4 - Approval confirmation email with link to create contest
     * 
     * @param user User whose request was approved
     * @param requestId ID of the approved request
     */
    @Async
    public void sendHostingApprovedEmail(User user, Long requestId) {
        String subject = "Hosting Request Approved | CodeCoder";
        String createContestUrl = appUrl + "/contests/private/create";
        
        String safeName = user.getFullName() != null ? user.getFullName() : user.getUsername();
        String html = loadTemplate("hosting-approved",
            "FULL_NAME", HtmlUtils.htmlEscape(safeName),
            "CREATE_CONTEST_URL", createContestUrl,
            "USER_EMAIL", user.getEmail()
        );
        
        sendEmail(user.getEmail(), subject, html);
    }

    /**
     * Notify user that their hosting request has been rejected.
     * 
     * Requirement: 2.3 - Rejection notification with admin notes
     * 
     * @param user User whose request was rejected
     * @param reason Admin's rejection reason (adminNotes)
     */
    @Async
    public void sendHostingRejectedEmail(User user, String reason) {
        String subject = "Hosting Request Update | CodeCoder";
        
        String safeReason = reason != null && !reason.isBlank() 
            ? HtmlUtils.htmlEscape(reason) 
            : "No specific reason provided.";
        
        String safeName = user.getFullName() != null ? user.getFullName() : user.getUsername();
        String html = loadTemplate("hosting-rejected",
            "FULL_NAME", HtmlUtils.htmlEscape(safeName),
            "REJECTION_REASON", safeReason,
            "USER_EMAIL", user.getEmail()
        );
        
        sendEmail(user.getEmail(), subject, html);
    }

    // ─── Contest Lifecycle Emails ─────────────────────────────────────────────

    /**
     * Notify host when their private contest is successfully created.
     * 
     * Requirement: 17.1 - Contest creation confirmation with invite link
     * 
     * @param host Contest host user
     * @param contestId ID of the created contest
     * @param contestName Name of the contest
     * @param startTime Contest start time
     * @param endTime Contest end time
     * @param duration Human-readable duration
     * @param inviteLink Shareable invitation link for participants
     */
    @Async
    public void sendContestCreatedEmail(User host, Long contestId, String contestName, 
                                        String startTime, String endTime, String duration, String inviteLink) {
        String subject = "Contest Created Successfully | CodeCoder";
        String manageUrl = appUrl + "/contests/private/" + contestId + "/manage";
        
        String hostName = host.getFullName() != null ? host.getFullName() : host.getUsername();
        String html = loadTemplate("contest-created",
            "HOST_NAME", HtmlUtils.htmlEscape(hostName),
            "CONTEST_NAME", HtmlUtils.htmlEscape(contestName),
            "CONTEST_ID", contestId.toString(),
            "START_TIME", HtmlUtils.htmlEscape(startTime),
            "END_TIME", HtmlUtils.htmlEscape(endTime),
            "DURATION", HtmlUtils.htmlEscape(duration),
            "INVITE_LINK", HtmlUtils.htmlEscape(inviteLink),
            "MANAGE_URL", manageUrl,
            "HOST_EMAIL", host.getEmail()
        );
        
        sendEmail(host.getEmail(), subject, html);
    }

    /**
     * Notify host when the contest transitions to LIVE status.
     * 
     * Requirement: 17.3 - Contest start notification with dashboard link
     * 
     * @param host Contest host user
     * @param contestId ID of the contest
     * @param contestName Name of the contest
     * @param startTime Contest start time
     * @param endTime Contest end time
     * @param participantCount Number of registered participants
     * @param dashboardLink Link to the live contest dashboard
     */
    @Async
    public void sendContestStartedEmail(User host, Long contestId, String contestName,
                                        String startTime, String endTime, int participantCount, String dashboardLink) {
        String subject = "Your Contest Has Started | CodeCoder";
        
        String hostName = host.getFullName() != null ? host.getFullName() : host.getUsername();
        String html = loadTemplate("contest-started",
            "HOST_NAME", HtmlUtils.htmlEscape(hostName),
            "CONTEST_NAME", HtmlUtils.htmlEscape(contestName),
            "CONTEST_ID", contestId.toString(),
            "START_TIME", HtmlUtils.htmlEscape(startTime),
            "END_TIME", HtmlUtils.htmlEscape(endTime),
            "PARTICIPANT_COUNT", String.valueOf(participantCount),
            "DASHBOARD_URL", HtmlUtils.htmlEscape(dashboardLink),
            "HOST_EMAIL", host.getEmail()
        );
        
        sendEmail(host.getEmail(), subject, html);
    }

    /**
     * Notify host when the contest transitions to ENDED status.
     * 
     * Requirement: 17.4 - Contest end summary with analytics link
     * 
     * @param host Contest host user
     * @param contestId ID of the contest
     * @param contestName Name of the contest
     * @param endTime Contest end time
     * @param participantCount Total number of participants
     * @param submissionCount Total number of submissions
     * @param analyticsLink Link to the analytics dashboard
     */
    @Async
    public void sendContestEndedEmail(User host, Long contestId, String contestName, 
                                      String endTime, int participantCount, int submissionCount, String analyticsLink) {
        String subject = "Your Contest Has Ended | CodeCoder";
        
        String hostName = host.getFullName() != null ? host.getFullName() : host.getUsername();
        String html = loadTemplate("contest-ended",
            "HOST_NAME", HtmlUtils.htmlEscape(hostName),
            "CONTEST_NAME", HtmlUtils.htmlEscape(contestName),
            "CONTEST_ID", contestId.toString(),
            "END_TIME", HtmlUtils.htmlEscape(endTime),
            "PARTICIPANT_COUNT", String.valueOf(participantCount),
            "SUBMISSION_COUNT", String.valueOf(submissionCount),
            "ANALYTICS_URL", HtmlUtils.htmlEscape(analyticsLink),
            "HOST_EMAIL", host.getEmail()
        );
        
        sendEmail(host.getEmail(), subject, html);
    }

    /**
     * Notify all participants when a contest is cancelled by the host.
     * 
     * Requirement: 18.3 - Cancellation notification to all participants
     * 
     * @param participants List of participant users to notify
     * @param contestId ID of the cancelled contest
     * @param contestName Name of the contest
     * @param hostName Name of the host who cancelled
     * @param reason Host's cancellation reason (optional)
     */
    @Async
    public void sendContestCancelledEmail(List<User> participants, Long contestId, String contestName, 
                                          String hostName, String reason) {
        String subject = "Contest Cancelled | CodeCoder";
        
        String safeReason = reason != null && !reason.isBlank() 
            ? HtmlUtils.htmlEscape(reason) 
            : "No specific reason provided.";
        
        for (User participant : participants) {
            String participantName = participant.getFullName() != null ? participant.getFullName() : participant.getUsername();
            String html = loadTemplate("contest-cancelled",
                "PARTICIPANT_NAME", HtmlUtils.htmlEscape(participantName),
                "CONTEST_NAME", HtmlUtils.htmlEscape(contestName),
                "CONTEST_ID", contestId.toString(),
                "HOST_NAME", HtmlUtils.htmlEscape(hostName),
                "CANCELLATION_REASON", safeReason,
                "PARTICIPANT_EMAIL", participant.getEmail()
            );
            
            sendEmail(participant.getEmail(), subject, html);
        }
    }

    // ─── Participant Notification Emails ──────────────────────────────────────

    /**
     * Send reminder to participants before contest starts.
     * 
     * Requirement: 27.1 - Pre-contest reminder notification
     * 
     * @param participants List of participant users to notify
     * @param contestId ID of the upcoming contest
     * @param hoursUntilStart Number of hours remaining until contest starts
     */
    @Async
    public void sendContestReminderEmail(List<User> participants, Long contestId, int hoursUntilStart) {
        String subject = "Contest Starting Soon | CodeCoder";
        String contestUrl = appUrl + "/contests/" + contestId;
        
        for (User participant : participants) {
            String content = "<p style='color:#d4c4b7;'>Hello <strong style='color:#f1bc8b;'>" 
                + HtmlUtils.htmlEscape(participant.getFullName() != null ? participant.getFullName() : participant.getUsername()) + "</strong>,</p>"
                + "<p style='color:#d4c4b7;'>Your registered contest starts in <strong style='color:#f1bc8b;'>" + hoursUntilStart + " hours</strong>!</p>"
                + "<p style='color:#d4c4b7;'><strong style='color:#f1bc8b;'>Contest ID:</strong> #" + contestId + "</p>"
                + "<p style='color:#d4c4b7;'>Make sure you're ready:</p>"
                + "<ul style='color:#d4c4b7;'>"
                + "<li>Test your internet connection</li>"
                + "<li>Prepare your development environment</li>"
                + "<li>Review the contest rules</li>"
                + "</ul>"
                + "<a href='" + contestUrl + "' "
                + "style='display:inline-block;margin-top:16px;padding:12px 24px;background:#f1bc8b;color:#131313;text-decoration:none;font-weight:bold;border-radius:4px;'>View Contest</a>";
            
            String html = buildEmailHtml("Contest Reminder", content);
            sendEmail(participant.getEmail(), subject, html);
        }
    }

    /**
     * Notify participants when contest starts (for participants who may not be online).
     * 
     * Requirement: 27.2 - Contest start notification to participants
     * 
     * @param participants List of participant users to notify
     * @param contestId ID of the contest
     */
    @Async
    public void sendParticipantContestStartedEmail(List<User> participants, Long contestId) {
        String subject = "Contest Is Now Live | CodeCoder";
        String contestUrl = appUrl + "/contests/" + contestId;
        
        for (User participant : participants) {
            String content = "<p style='color:#d4c4b7;'>Hello <strong style='color:#f1bc8b;'>" 
                + HtmlUtils.htmlEscape(participant.getFullName() != null ? participant.getFullName() : participant.getUsername()) + "</strong>,</p>"
                + "<p style='color:#d4c4b7;'>Your contest is now <strong style='color:#f1bc8b;'>LIVE</strong>!</p>"
                + "<p style='color:#d4c4b7;'><strong style='color:#f1bc8b;'>Contest ID:</strong> #" + contestId + "</p>"
                + "<p style='color:#d4c4b7;'>Good luck and happy coding!</p>"
                + "<a href='" + contestUrl + "' "
                + "style='display:inline-block;margin-top:16px;padding:12px 24px;background:#f1bc8b;color:#131313;text-decoration:none;font-weight:bold;border-radius:4px;'>Enter Contest</a>";
            
            String html = buildEmailHtml("Contest Started", content);
            sendEmail(participant.getEmail(), subject, html);
        }
    }

    /**
     * Notify participant of their final results when contest ends.
     * 
     * Requirement: 27.3, 27.4 - Final results notification with rank and score
     * 
     * @param participant Participant user
     * @param contestId ID of the contest
     * @param rank Participant's final rank
     * @param score Participant's total score
     */
    @Async
    public void sendParticipantContestEndedEmail(User participant, Long contestId, int rank, int score) {
        String subject = "Contest Results | CodeCoder";
        String leaderboardUrl = appUrl + "/contests/" + contestId + "/leaderboard";
        
        String content = "<p style='color:#d4c4b7;'>Hello <strong style='color:#f1bc8b;'>" 
            + HtmlUtils.htmlEscape(participant.getFullName() != null ? participant.getFullName() : participant.getUsername()) + "</strong>,</p>"
            + "<p style='color:#d4c4b7;'>The contest has concluded. Here are your results:</p>"
            + "<div style='background:#131313;padding:20px;margin:16px 0;'>"
            + "<p style='color:#d4c4b7;margin:8px 0;'><strong style='color:#f1bc8b;'>Contest ID:</strong> #" + contestId + "</p>"
            + "<p style='color:#d4c4b7;margin:8px 0;'><strong style='color:#f1bc8b;'>Your Rank:</strong> #" + rank + "</p>"
            + "<p style='color:#d4c4b7;margin:8px 0;'><strong style='color:#f1bc8b;'>Your Score:</strong> " + score + " points</p>"
            + "</div>"
            + "<p style='color:#d4c4b7;'>View the full leaderboard and contest details below.</p>"
            + "<a href='" + leaderboardUrl + "' "
            + "style='display:inline-block;margin-top:16px;padding:12px 24px;background:#f1bc8b;color:#131313;text-decoration:none;font-weight:bold;border-radius:4px;'>View Leaderboard</a>";
        
        String html = buildEmailHtml("Contest Results", content);
        sendEmail(participant.getEmail(), subject, html);
    }
}
