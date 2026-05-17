package com.example.codecombat2026.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    // Read from env — no hardcoded URLs
    @Value("${APP_URL:https://codecombat.live}")
    private String appUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    // ─── Email Template ───────────────────────────────────────────────────────

    private String wrap(String content) {
        return "<!DOCTYPE html><html><head>"
            + "<meta charset='UTF-8'>"
            + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
            + "</head><body style='margin:0;padding:0;font-family:Arial,sans-serif;background:#000;'>"
            + "<div style='max-width:600px;margin:40px auto;background:rgba(255,255,255,0.05);border:1px solid rgba(255,255,255,0.1);border-radius:16px;overflow:hidden;'>"
            + "<div style='background:linear-gradient(135deg,#10b981,#059669);padding:30px;text-align:center;'>"
            + "<h1 style='margin:0;color:white;font-size:28px;font-weight:bold;'>CodeCombat</h1>"
            + "<p style='margin:5px 0 0;color:rgba(255,255,255,0.9);font-size:14px;'>Competitive Programming Platform</p>"
            + "</div>"
            + "<div style='padding:40px 30px;color:#e5e7eb;'>" + content + "</div>"
            + "<div style='background:rgba(0,0,0,0.3);padding:20px 30px;text-align:center;border-top:1px solid rgba(255,255,255,0.1);'>"
            + "<p style='margin:0 0 8px;color:#9ca3af;font-size:12px;'>This is an automated email. Please do not reply.</p>"
            + "<p style='margin:0 0 8px;color:#10b981;font-size:12px;font-weight:bold;'>⚠️ Check spam/junk if you don't see this in your inbox.</p>"
            + "<p style='margin:0;color:#6b7280;font-size:11px;'>© 2026 CodeCombat. All rights reserved.</p>"
            + "<p style='margin:5px 0 0;'><a href='" + appUrl + "' style='color:#10b981;text-decoration:none;font-size:12px;'>Visit CodeCombat</a></p>"
            + "</div></div></body></html>";
    }

    private void sendHtml(String to, String subject, String content) throws MessagingException {
        MimeMessage msg = mailSender.createMimeMessage();
        MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
        h.setFrom("no-reply@codecombat.live");
        h.setTo(to);
        h.setSubject(subject);
        h.setText(wrap(content), true);
        mailSender.send(msg);
    }

    // ─── Public methods ───────────────────────────────────────────────────────

    public void sendSupportEmail(String senderEmail, String name, String phone, String message) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
            h.setTo("support@codecombat.live");
            h.setSubject("Support Request from " + name);
            h.setReplyTo(senderEmail);

            String content = "<h2 style='color:#10b981;margin-top:0;'>New Support Request</h2>"
                + "<div style='background:rgba(255,255,255,0.05);padding:20px;border-radius:8px;border-left:4px solid #10b981;'>"
                + "<p><strong style='color:#10b981;'>Name:</strong> " + name + "</p>"
                + "<p><strong style='color:#10b981;'>Email:</strong> " + senderEmail + "</p>"
                + "<p><strong style='color:#10b981;'>Phone:</strong> " + (phone != null && !phone.isEmpty() ? phone : "Not provided") + "</p>"
                + "<p><strong style='color:#10b981;'>Message:</strong></p>"
                + "<p style='padding:15px;background:rgba(0,0,0,0.2);border-radius:8px;'>" + message + "</p>"
                + "</div>";

            h.setText(wrap(content), true);
            mailSender.send(msg);
        } catch (Exception e) {
            log.error("Failed to send support email: {}", e.getMessage());
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }

    public void sendWelcomeEmail(String userEmail, String fullName) {
        try {
            String content = "<h2 style='color:#10b981;margin-top:0;'>Welcome, " + fullName + "! 🚀</h2>"
                + "<p style='font-size:16px;line-height:1.6;'>Thank you for joining CodeCombat!</p>"
                + "<div style='background:rgba(16,185,129,0.1);padding:20px;border-radius:12px;margin:20px 0;border:1px solid rgba(16,185,129,0.3);'>"
                + "<h3 style='color:#10b981;margin-top:0;'>What you can do:</h3>"
                + "<ul style='list-style:none;padding:0;'>"
                + "<li style='padding:8px 0;'>✅ Participate in coding contests</li>"
                + "<li style='padding:8px 0;'>✅ Solve challenging problems</li>"
                + "<li style='padding:8px 0;'>✅ Compete and climb the leaderboard</li>"
                + "</ul></div>"
                + "<div style='text-align:center;margin:30px 0;'>"
                + "<a href='" + appUrl + "/contests' style='background:linear-gradient(135deg,#10b981,#059669);color:white;padding:14px 32px;text-decoration:none;border-radius:8px;font-weight:bold;'>Explore Contests</a>"
                + "</div>"
                + "<p style='color:#9ca3af;font-size:14px;'>Questions? Contact <a href='mailto:support@codecombat.live' style='color:#10b981;'>support@codecombat.live</a></p>"
                + "<p style='font-size:18px;'>Happy Coding! 💻</p>";

            sendHtml(userEmail, "Welcome to CodeCombat! 🎉", content);
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}: {}", userEmail, e.getMessage());
            // Don't throw — welcome email failure shouldn't block registration
        }
    }

    public void sendPasswordResetEmail(String userEmail, String fullName, String resetToken) {
        try {
            String resetLink = appUrl + "/reset-password?token=" + resetToken;

            String content = "<h2 style='color:#10b981;margin-top:0;'>Password Reset Request</h2>"
                + "<p style='font-size:16px;'>Dear " + fullName + ",</p>"
                + "<p style='font-size:16px;line-height:1.6;'>We received a request to reset your CodeCombat password.</p>"
                + "<div style='background:rgba(239,68,68,0.1);padding:20px;border-radius:12px;margin:20px 0;border-left:4px solid #ef4444;'>"
                + "<p style='margin:0;color:#fca5a5;'><strong>⏰ This link expires in 15 minutes.</strong></p>"
                + "</div>"
                + "<div style='text-align:center;margin:30px 0;'>"
                + "<a href='" + resetLink + "' style='background:linear-gradient(135deg,#10b981,#059669);color:white;padding:14px 32px;text-decoration:none;border-radius:8px;font-weight:bold;'>Reset Password</a>"
                + "</div>"
                + "<p style='font-size:12px;color:#9ca3af;word-break:break-all;'>Or copy: <span style='color:#10b981;'>" + resetLink + "</span></p>"
                + "<p style='color:#9ca3af;font-size:14px;'>If you didn't request this, ignore this email.</p>"
                + "<p style='color:#ef4444;font-size:14px;'><strong>⚠️ Never share this link with anyone.</strong></p>";

            sendHtml(userEmail, "Password Reset Request - CodeCombat", content);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", userEmail, e.getMessage());
            throw new RuntimeException("Failed to send password reset email: " + e.getMessage());
        }
    }

    public void sendUsernameRecoveryEmail(String userEmail, String username) {
        try {
            String content = "<h2 style='color:#10b981;margin-top:0;'>Username Recovery</h2>"
                + "<p style='font-size:16px;'>Hello,</p>"
                + "<p style='font-size:16px;line-height:1.6;'>Your CodeCombat username is:</p>"
                + "<div style='background:rgba(16,185,129,0.2);padding:25px;border-radius:12px;margin:25px 0;text-align:center;border:2px solid rgba(16,185,129,0.5);'>"
                + "<p style='margin:0;font-size:28px;font-weight:bold;color:#10b981;'>" + username + "</p>"
                + "</div>"
                + "<div style='text-align:center;margin:30px 0;'>"
                + "<a href='" + appUrl + "/login' style='background:linear-gradient(135deg,#10b981,#059669);color:white;padding:14px 32px;text-decoration:none;border-radius:8px;font-weight:bold;'>Login Now</a>"
                + "</div>"
                + "<p style='color:#9ca3af;font-size:14px;'>If you didn't request this, ignore this email.</p>";

            sendHtml(userEmail, "Username Recovery - CodeCombat", content);
            log.info("Username recovery email sent to {}", userEmail);

        } catch (MessagingException e) {
            log.error("HTML email failed for {}, trying plain text: {}", userEmail, e.getMessage());
            // Fallback to plain text
            try {
                SimpleMailMessage plain = new SimpleMailMessage();
                plain.setFrom("no-reply@codecombat.live");
                plain.setTo(userEmail);
                plain.setSubject("Username Recovery - CodeCombat");
                plain.setText("Hello,\n\nYour CodeCombat username is: " + username
                    + "\n\nLogin at: " + appUrl + "/login\n\nIf you didn't request this, ignore this email.\n\nCodeCombat Team");
                mailSender.send(plain);
            } catch (Exception fallback) {
                log.error("Plain text fallback also failed: {}", fallback.getMessage());
                throw new RuntimeException("Failed to send username recovery email: " + e.getMessage());
            }
        } catch (Exception e) {
            log.error("Failed to send username recovery email to {}: {}", userEmail, e.getMessage());
            throw new RuntimeException("Failed to send username recovery email: " + e.getMessage());
        }
    }
}
