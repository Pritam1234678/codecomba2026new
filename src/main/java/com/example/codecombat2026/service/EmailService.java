package com.example.codecombat2026.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
 * Email sender with bounded exponential backoff for transient SMTP failures.
 * Uses HTML templates from classpath:email-templates/ for all transactional emails.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private static final int MAX_ATTEMPTS = 3;
    private static final long BASE_DELAY_MS = 500;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${APP_URL:https://codecoder.in}")
    private String appUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    // ─── Template loader ──────────────────────────────────────────────────────

    /**
     * Load an HTML template from classpath:email-templates/{name}.html
     * and replace {{PLACEHOLDER}} tokens with provided values.
     * Falls back to a plain-text body if the template file is missing.
     */
    private String loadTemplate(String templateName, String... replacements) {
        try {
            ClassPathResource resource = new ClassPathResource("email-templates/" + templateName + ".html");
            try (InputStream is = resource.getInputStream()) {
                String html = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                // Always inject APP_URL
                html = html.replace("{{APP_URL}}", appUrl);
                // Apply caller-supplied key=value pairs
                for (int i = 0; i + 1 < replacements.length; i += 2) {
                    html = html.replace("{{" + replacements[i] + "}}", replacements[i + 1]);
                }
                return html;
            }
        } catch (IOException e) {
            log.error("Email template '{}' not found, using fallback: {}", templateName, e.getMessage());
            return "<p>Please visit <a href='" + appUrl + "'>" + appUrl + "</a></p>";
        }
    }

    /**
     * Send a MIME message with bounded exponential backoff + jitter.
     * Returns normally on success; throws after final failure.
     */
    private void sendWithRetry(MimeMessage msg) throws MessagingException {
        MailException last = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                mailSender.send(msg);
                if (attempt > 1) log.info("Email delivered on attempt {}", attempt);
                return;
            } catch (MailException e) {
                last = e;
                log.warn("Email attempt {}/{} failed: {}", attempt, MAX_ATTEMPTS, e.getMessage());
                if (attempt < MAX_ATTEMPTS) {
                    long backoff = BASE_DELAY_MS * (1L << (attempt - 1)); // 500, 1000, 2000ms
                    long jitter = ThreadLocalRandom.current().nextLong(0, 200);
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

    private void sendHtml(String to, String subject, String htmlBody) throws MessagingException {
        MimeMessage msg = mailSender.createMimeMessage();
        MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
        h.setFrom("no-reply@codecoder.in");
        h.setTo(to);
        h.setSubject(subject);
        h.setText(htmlBody, true);
        sendWithRetry(msg);
    }

    // ─── Public methods ───────────────────────────────────────────────────────

    public void sendSupportEmail(String senderEmail, String name, String phone, String message) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
            h.setTo("support@codecombat.live");
            // Escape sender-controlled fields used in headers/HTML to prevent injection.
            String safeName    = org.springframework.web.util.HtmlUtils.htmlEscape(name == null ? "" : name);
            String safeEmail   = org.springframework.web.util.HtmlUtils.htmlEscape(senderEmail == null ? "" : senderEmail);
            String safePhone   = org.springframework.web.util.HtmlUtils.htmlEscape(phone == null ? "" : phone);
            String safeMessage = org.springframework.web.util.HtmlUtils.htmlEscape(message == null ? "" : message);
            // Subject is plain text; use the escaped name to avoid exotic header chars.
            h.setSubject("Support Request from " + safeName);
            h.setReplyTo(senderEmail);

            String content = "<!DOCTYPE html><html><head><meta charset='UTF-8'></head>"
                + "<body style='margin:0;padding:0;background:#131313;font-family:Arial,sans-serif;'>"
                + "<div style='max-width:600px;margin:0 auto;background:#1c1b1b;border:1px solid #50453b;padding:32px;'>"
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

            h.setText(content, true);
            sendWithRetry(msg);
        } catch (Exception e) {
            log.error("Failed to send support email after retries: {}", e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        }
    }

    public void sendWelcomeEmail(String userEmail, String fullName) {
        try {
            String safeName = org.springframework.web.util.HtmlUtils.htmlEscape(fullName == null ? "Architect" : fullName);
            String html = loadTemplate("welcome", "FULL_NAME", safeName);
            sendHtml(userEmail, "Welcome to the Arena | CodeCoder", html);
        } catch (Exception e) {
            log.error("Failed to send welcome email to {} after retries: {}", userEmail, e.getMessage());
            // Don't throw — welcome email failure shouldn't block registration
        }
    }

    public void sendPasswordResetEmail(String userEmail, String fullName, String resetToken) {
        try {
            String resetLink = appUrl + "/reset-password?token=" + resetToken;
            String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss z").format(new java.util.Date());
            String html = loadTemplate("password-reset",
                "RESET_LINK", resetLink,
                "IP_ADDRESS", "—",
                "TIMESTAMP", timestamp
            );
            sendHtml(userEmail, "Identity Verification | CodeCoder", html);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {} after retries: {}", userEmail, e.getMessage());
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }

    public void sendUsernameRecoveryEmail(String userEmail, String username) {
        try {
            String safeUsername = org.springframework.web.util.HtmlUtils.htmlEscape(username == null ? "" : username);
            String html = loadTemplate("username-recovery", "USERNAME", safeUsername);
            sendHtml(userEmail, "Access Restored | CodeCoder", html);
            log.info("Username recovery email sent to {}", userEmail);
        } catch (MessagingException e) {
            // HTML send failed — try plain-text fallback
            log.error("HTML email failed for {}, trying plain text fallback: {}", userEmail, e.getMessage());
            try {
                SimpleMailMessage plain = new SimpleMailMessage();
                plain.setFrom("no-reply@codecoder.in");
                plain.setTo(userEmail);
                plain.setSubject("Access Restored | CodeCoder");
                plain.setText("Hello,\n\nYour CodeCoder username is: " + username
                    + "\n\nLogin at: " + appUrl + "/login\n\nIf you didn't request this, ignore this email.\n\nCodeCoder Team");
                mailSender.send(plain);
            } catch (Exception fallback) {
                log.error("Plain text fallback also failed: {}", fallback.getMessage());
                throw new RuntimeException("Failed to send username recovery email", e);
            }
        } catch (Exception e) {
            log.error("Failed to send username recovery email to {}: {}", userEmail, e.getMessage());
            throw new RuntimeException("Failed to send username recovery email", e);
        }
    }

    /**
     * Notify the user that their password just changed. Sent on password
     * reset / password change so an account-takeover attempt is visible
     * even if the attacker controls the next login.
     */
    public void sendPasswordChangedNotification(String userEmail, String username, String ipAddress) {
        try {
            String safeIp = org.springframework.web.util.HtmlUtils.htmlEscape(ipAddress == null || ipAddress.isBlank() ? "unknown" : ipAddress);
            String time = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss z").format(new java.util.Date());
            String html = loadTemplate("password-changed",
                "TIME_STAMP", time,
                "IP_ADDRESS", safeIp
            );
            sendHtml(userEmail, "Security Update | CodeCoder", html);
        } catch (Exception e) {
            // Non-fatal — password change has already succeeded.
            log.error("Failed to send password-changed notification to {}: {}", userEmail, e.getMessage());
        }
    }
}
