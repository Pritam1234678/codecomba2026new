package com.example.codecombat2026.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Dual-relay email service for CodeCoder.
 *
 * <p>Routing:
 * <ul>
 *   <li>Transactional emails (welcome, password-reset, password-changed,
 *       username-recovery) → {@code noreply@codecoder.in} via SendPulse.</li>
 *   <li>Support tickets → {@code support@codecoder.in} via Brevo.</li>
 * </ul>
 *
 * <p>All HTML bodies are loaded from {@code classpath:email-templates/} so
 * designers can edit them without touching Java. Placeholder tokens use the
 * {@code {{KEY}}} convention and are replaced at send time.
 *
 * <p>Every send path uses bounded exponential backoff (3 attempts, 500 ms
 * base, ±200 ms jitter) to survive transient SMTP relay hiccups.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private static final int MAX_ATTEMPTS = 3;
    private static final long BASE_DELAY_MS = 500;

    /** SendPulse relay — used for all noreply@codecoder.in transactional mail. */
    @Autowired
    @Qualifier("noreplyMailSender")
    private JavaMailSender noreplyMailSender;

    /** Brevo relay — used for support@codecoder.in inbound tickets. */
    @Autowired(required = false)
    @Qualifier("supportMailSender")
    private JavaMailSender supportMailSender;

    private static final String NOREPLY_FROM  = "noreply@codecoder.in";
    private static final String SUPPORT_FROM  = "support@codecoder.in";
    private static final String SUPPORT_TO    = "support@codecoder.in";

    @Value("${APP_URL:https://codecoder.in}")
    private String appUrl;

    // ─── Template loader ──────────────────────────────────────────────────────

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

    // ─── Low-level send helpers ───────────────────────────────────────────────

    /**
     * Send a MIME message with bounded exponential backoff + jitter.
     * Throws {@link MessagingException} after all retries are exhausted.
     */
    private void sendWithRetry(JavaMailSender sender, MimeMessage msg) throws MessagingException {
        MailException last = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                sender.send(msg);
                if (attempt > 1) log.info("Email delivered on attempt {}", attempt);
                return;
            } catch (MailException e) {
                last = e;
                log.warn("Email attempt {}/{} failed: {}", attempt, MAX_ATTEMPTS, e.getMessage());
                if (attempt < MAX_ATTEMPTS) {
                    long backoff = BASE_DELAY_MS * (1L << (attempt - 1));
                    long jitter  = ThreadLocalRandom.current().nextLong(0, 200);
                    try { Thread.sleep(backoff + jitter); }
                    catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new MessagingException("Retry interrupted", ie);
                    }
                }
            }
        }
        throw new MessagingException("Email send failed after " + MAX_ATTEMPTS + " attempts", last);
    }

    /**
     * Build and send an HTML email via the noreply (SendPulse) relay.
     */
    private void sendNoreplyHtml(String to, String subject, String htmlBody) throws MessagingException {
        MimeMessage msg = noreplyMailSender.createMimeMessage();
        MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
        h.setFrom(NOREPLY_FROM);
        h.setTo(to);
        h.setSubject(subject);
        h.setText(htmlBody, true);
        sendWithRetry(noreplyMailSender, msg);
    }

    /**
     * Build and send an HTML email via the support (Brevo) relay.
     */
    private void sendSupportHtml(String to, String from, String replyTo,
                                  String subject, String htmlBody) throws MessagingException {
        MimeMessage msg = supportMailSender.createMimeMessage();
        MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
        h.setFrom(from);
        h.setTo(to);
        if (replyTo != null && !replyTo.isBlank()) h.setReplyTo(replyTo);
        h.setSubject(subject);
        h.setText(htmlBody, true);
        sendWithRetry(supportMailSender, msg);
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Forward a user support request to the admin inbox via Brevo.
     * Sender is {@code support@codecoder.in}; Reply-To is the user's email.
     */
    public void sendSupportEmail(String senderEmail, String name, String phone, String message) {
        try {
            String safeName    = org.springframework.web.util.HtmlUtils.htmlEscape(name    == null ? "" : name);
            String safeEmail   = org.springframework.web.util.HtmlUtils.htmlEscape(senderEmail == null ? "" : senderEmail);
            String safePhone   = org.springframework.web.util.HtmlUtils.htmlEscape(phone   == null ? "" : phone);
            String safeMessage = org.springframework.web.util.HtmlUtils.htmlEscape(message == null ? "" : message);

            String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'></head>"
                + "<body style='margin:0;padding:0;background:#131313;font-family:Arial,sans-serif;'>"
                + "<div style='max-width:600px;margin:0 auto;background:#1c1b1b;border:1px solid #50453b;padding:32px;'>"
                + "<img src='https://codecoder.in/logo.png' alt='CodeCoder' height='40' style='height:40px;width:auto;margin-bottom:24px;'/>"
                + "<h2 style='color:#f1bc8b;margin-top:0;font-family:Georgia,serif;'>New Support Request</h2>"
                + "<div style='background:#0e0e0e;padding:20px;border-left:2px solid #f1bc8b;'>"
                + "<p style='color:#d4c4b7;'><strong style='color:#f1bc8b;'>Name:</strong> " + safeName + "</p>"
                + "<p style='color:#d4c4b7;'><strong style='color:#f1bc8b;'>Email:</strong> " + safeEmail + "</p>"
                + "<p style='color:#d4c4b7;'><strong style='color:#f1bc8b;'>Phone:</strong> " + (!safePhone.isEmpty() ? safePhone : "Not provided") + "</p>"
                + "<p style='color:#d4c4b7;'><strong style='color:#f1bc8b;'>Message:</strong></p>"
                + "<p style='color:#d4c4b7;padding:12px;background:#131313;white-space:pre-wrap;'>" + safeMessage + "</p>"
                + "</div>"
                + "<p style='color:#9d8e83;font-size:11px;margin-top:24px;'>Sent via codecoder.in support form</p>"
                + "</div></body></html>";

            sendSupportHtml(SUPPORT_TO, SUPPORT_FROM, senderEmail,
                "Support Request from " + safeName, html);
        } catch (Exception e) {
            log.error("Failed to send support email: {}", e.getMessage());
            throw new RuntimeException("Failed to send support email", e);
        }
    }

    /**
     * Welcome email sent on successful registration.
     * Uses SendPulse via {@code noreply@codecoder.in}.
     */
    public void sendWelcomeEmail(String userEmail, String fullName) {
        try {
            String safeName = org.springframework.web.util.HtmlUtils.htmlEscape(
                fullName == null ? "Architect" : fullName);
            String html = loadTemplate("welcome", "FULL_NAME", safeName);
            sendNoreplyHtml(userEmail, "Welcome to the Arena | CodeCoder", html);
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}: {}", userEmail, e.getMessage());
            // Non-fatal — don't block registration
        }
    }

    /**
     * Password-reset link email.
     * Uses SendPulse via {@code noreply@codecoder.in}.
     */
    public void sendPasswordResetEmail(String userEmail, String fullName, String resetToken) {
        try {
            String resetLink  = appUrl + "/reset-password?token=" + resetToken;
            String timestamp  = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss z")
                .format(new java.util.Date());
            String html = loadTemplate("password-reset",
                "RESET_LINK", resetLink,
                "IP_ADDRESS", "—",
                "TIMESTAMP",  timestamp
            );
            sendNoreplyHtml(userEmail, "Identity Verification | CodeCoder", html);
        } catch (Exception e) {
            log.error("Failed to send password-reset email to {}: {}", userEmail, e.getMessage());
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }

    /**
     * Username recovery email.
     * Uses SendPulse via {@code noreply@codecoder.in}.
     * Falls back to plain text if HTML send fails.
     */
    public void sendUsernameRecoveryEmail(String userEmail, String username) {
        try {
            String safeUsername = org.springframework.web.util.HtmlUtils.htmlEscape(
                username == null ? "" : username);
            String html = loadTemplate("username-recovery", "USERNAME", safeUsername);
            sendNoreplyHtml(userEmail, "Access Restored | CodeCoder", html);
            log.info("Username recovery email sent to {}", userEmail);
        } catch (MessagingException e) {
            log.error("HTML email failed for {}, trying plain-text fallback: {}", userEmail, e.getMessage());
            try {
                SimpleMailMessage plain = new SimpleMailMessage();
                plain.setFrom(NOREPLY_FROM);
                plain.setTo(userEmail);
                plain.setSubject("Access Restored | CodeCoder");
                plain.setText("Hello,\n\nYour CodeCoder username is: " + username
                    + "\n\nLogin at: " + appUrl + "/login"
                    + "\n\nIf you didn't request this, ignore this email.\n\nCodeCoder Team");
                noreplyMailSender.send(plain);
            } catch (Exception fallback) {
                log.error("Plain-text fallback also failed: {}", fallback.getMessage());
                throw new RuntimeException("Failed to send username recovery email", e);
            }
        } catch (Exception e) {
            log.error("Failed to send username recovery email to {}: {}", userEmail, e.getMessage());
            throw new RuntimeException("Failed to send username recovery email", e);
        }
    }

    /**
     * Security notification sent after a password change.
     * Uses SendPulse via {@code noreply@codecoder.in}.
     * Non-fatal — password change has already succeeded before this is called.
     */
    public void sendPasswordChangedNotification(String userEmail, String username, String ipAddress) {
        try {
            String safeIp = org.springframework.web.util.HtmlUtils.htmlEscape(
                ipAddress == null || ipAddress.isBlank() ? "unknown" : ipAddress);
            String time = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss z")
                .format(new java.util.Date());
            String html = loadTemplate("password-changed",
                "TIME_STAMP",  time,
                "IP_ADDRESS",  safeIp
            );
            sendNoreplyHtml(userEmail, "Security Update | CodeCoder", html);
        } catch (Exception e) {
            log.error("Failed to send password-changed notification to {}: {}", userEmail, e.getMessage());
        }
    }

    /**
     * Notification sent to contest host when the first participant joins their private contest.
     * Uses SendPulse via {@code noreply@codecoder.in}.
     * Non-fatal — participant join has already succeeded before this is called.
     * 
     * @param hostEmail Email address of the contest host
     * @param hostName Full name or username of the contest host
     * @param contestName Name of the private contest
     * @param contestId ID of the contest
     * @param participantUsername Username of the first participant who joined
     * @param joinedAt Timestamp when participant joined
     * 
     * Requirement: 17.2
     */
    public void sendFirstParticipantJoinedEmail(String hostEmail, String hostName, 
                                                 String contestName, Long contestId,
                                                 String participantUsername, String joinedAt) {
        try {
            String safeHostName = org.springframework.web.util.HtmlUtils.htmlEscape(
                hostName == null ? "Host" : hostName);
            String safeContestName = org.springframework.web.util.HtmlUtils.htmlEscape(
                contestName == null ? "Your Contest" : contestName);
            String safeParticipantUsername = org.springframework.web.util.HtmlUtils.htmlEscape(
                participantUsername == null ? "A participant" : participantUsername);
            String manageUrl = appUrl + "/contests/private/" + contestId + "/manage";

            String html = loadTemplate("first-participant-joined",
                "HOST_NAME", safeHostName,
                "CONTEST_NAME", safeContestName,
                "PARTICIPANT_USERNAME", safeParticipantUsername,
                "JOINED_AT", joinedAt,
                "MANAGE_URL", manageUrl,
                "HOST_EMAIL", hostEmail
            );

            sendNoreplyHtml(hostEmail, "First Registration - " + safeContestName + " | CodeCoder", html);
            log.info("First participant notification sent to host {}", hostEmail);
        } catch (Exception e) {
            log.error("Failed to send first participant notification to {}: {}", hostEmail, e.getMessage());
            // Non-fatal — don't throw exception
        }
    }
}
