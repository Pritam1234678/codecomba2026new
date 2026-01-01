package com.example.codecombat2026.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    private String getEmailTemplate(String content) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "</head>" +
                "<body style='margin: 0; padding: 0; font-family: Arial, sans-serif; background: linear-gradient(135deg, #000000 0%, #0a4d2e 100%);'>"
                +
                "<div style='max-width: 600px; margin: 40px auto; background: rgba(255, 255, 255, 0.05); backdrop-filter: blur(10px); border: 1px solid rgba(255, 255, 255, 0.1); border-radius: 16px; overflow: hidden; box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);'>"
                +

                "<!-- Header -->" +
                "<div style='background: linear-gradient(135deg, #10b981 0%, #059669 100%); padding: 30px; text-align: center;'>"
                +
                "<h1 style='margin: 0; color: white; font-size: 28px; font-weight: bold; text-shadow: 0 2px 4px rgba(0,0,0,0.2);'>CodeCombat</h1>"
                +
                "<p style='margin: 5px 0 0 0; color: rgba(255,255,255,0.9); font-size: 14px;'>Competitive Programming Platform</p>"
                +
                "</div>" +

                "<!-- Content -->" +
                "<div style='padding: 40px 30px; color: #e5e7eb;'>" +
                content +
                "</div>" +

                "<!-- Footer -->" +
                "<div style='background: rgba(0, 0, 0, 0.3); padding: 20px 30px; text-align: center; border-top: 1px solid rgba(255, 255, 255, 0.1);'>"
                +
                "<p style='margin: 0 0 10px 0; color: #9ca3af; font-size: 12px;'>This is an automated email. Please do not reply to this message.</p>"
                +
                "<p style='margin: 0 0 10px 0; color: #10b981; font-size: 12px; font-weight: bold;'>⚠️ If you don't see this email in your inbox, please check your spam/junk folder.</p>"
                +
                "<p style='margin: 0; color: #6b7280; font-size: 11px;'>© 2026 CodeCombat. All rights reserved.</p>" +
                "<p style='margin: 5px 0 0 0;'><a href='https://codecombat.live' style='color: #10b981; text-decoration: none; font-size: 12px;'>Visit CodeCombat</a></p>"
                +
                "</div>" +

                "</div>" +
                "</body>" +
                "</html>";
    }

    public void sendSupportEmail(String fromEmail, String name, String phone, String message) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo("support@codecombat.live");
            helper.setSubject("CodeCombat Support Request from " + name);
            helper.setReplyTo(fromEmail);

            String content = "<h2 style='color: #10b981; margin-top: 0;'>New Support Request</h2>" +
                    "<div style='background: rgba(255, 255, 255, 0.05); padding: 20px; border-radius: 8px; border-left: 4px solid #10b981;'>"
                    +
                    "<p style='margin: 10px 0;'><strong style='color: #10b981;'>Name:</strong> " + name + "</p>" +
                    "<p style='margin: 10px 0;'><strong style='color: #10b981;'>Email:</strong> " + fromEmail + "</p>" +
                    "<p style='margin: 10px 0;'><strong style='color: #10b981;'>Phone:</strong> "
                    + (phone != null && !phone.isEmpty() ? phone : "Not provided") + "</p>" +
                    "<p style='margin: 10px 0;'><strong style='color: #10b981;'>Message:</strong></p>" +
                    "<p style='margin: 10px 0; padding: 15px; background: rgba(0, 0, 0, 0.2); border-radius: 8px;'>"
                    + message + "</p>" +
                    "</div>";

            helper.setText(getEmailTemplate(content), true);
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            System.err.println("Failed to send HTML support email, error: " + e.getMessage());
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }

    public void sendWelcomeEmail(String userEmail, String fullName) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom("no-reply@codecombat.live");
            helper.setTo(userEmail);
            helper.setSubject("Welcome to CodeCombat! 🎉");

            String content = "<h2 style='color: #10b981; margin-top: 0;'>Welcome, " + fullName + "! 🚀</h2>" +
                    "<p style='font-size: 16px; line-height: 1.6;'>Thank you for registering with us. We're excited to have you join our competitive programming community!</p>"
                    +

                    "<div style='background: linear-gradient(135deg, rgba(16, 185, 129, 0.1) 0%, rgba(5, 150, 105, 0.1) 100%); padding: 20px; border-radius: 12px; margin: 20px 0; border: 1px solid rgba(16, 185, 129, 0.3);'>"
                    +
                    "<h3 style='color: #10b981; margin-top: 0;'>Here's what you can do now:</h3>" +
                    "<ul style='list-style: none; padding: 0;'>" +
                    "<li style='padding: 8px 0;'>✅ Participate in coding contests</li>" +
                    "<li style='padding: 8px 0;'>✅ Solve challenging problems</li>" +
                    "<li style='padding: 8px 0;'>✅ Compete with other programmers</li>" +
                    "<li style='padding: 8px 0;'>✅ Track your progress on the leaderboard</li>" +
                    "</ul>" +
                    "</div>" +

                    "<p style='font-size: 16px;'>Get started by exploring our active contests and problems!</p>" +
                    "<div style='text-align: center; margin: 30px 0;'>" +
                    "<a href='https://codecombat.live/contests' style='display: inline-block; background: linear-gradient(135deg, #10b981 0%, #059669 100%); color: white; padding: 14px 32px; text-decoration: none; border-radius: 8px; font-weight: bold; box-shadow: 0 4px 12px rgba(16, 185, 129, 0.3);'>Explore Contests</a>"
                    +
                    "</div>" +

                    "<p style='color: #9ca3af; font-size: 14px; margin-top: 30px;'>If you have any questions or need help, feel free to contact us at <a href='mailto:support@codecombat.live' style='color: #10b981; text-decoration: none;'>support@codecombat.live</a></p>"
                    +
                    "<p style='font-size: 18px; margin-top: 20px;'>Happy Coding! 💻</p>";

            helper.setText(getEmailTemplate(content), true);
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            System.err.println("Failed to send welcome email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendPasswordResetEmail(String userEmail, String fullName, String resetToken) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom("no-reply@codecombat.live");
            helper.setTo(userEmail);
            helper.setSubject("Password Reset Request - CodeCombat");

            String resetLink = "https://codecombat.live/reset-password?token=" + resetToken;

            String content = "<h2 style='color: #10b981; margin-top: 0;'>Password Reset Request</h2>" +
                    "<p style='font-size: 16px;'>Dear " + fullName + ",</p>" +
                    "<p style='font-size: 16px; line-height: 1.6;'>We received a request to reset your password for your CodeCombat account.</p>"
                    +

                    "<div style='background: rgba(239, 68, 68, 0.1); padding: 20px; border-radius: 12px; margin: 20px 0; border-left: 4px solid #ef4444;'>"
                    +
                    "<p style='margin: 0; color: #fca5a5;'><strong>⏰ This link will expire in 15 minutes for security reasons.</strong></p>"
                    +
                    "</div>" +

                    "<div style='text-align: center; margin: 30px 0;'>" +
                    "<a href='" + resetLink
                    + "' style='display: inline-block; background: linear-gradient(135deg, #10b981 0%, #059669 100%); color: white; padding: 14px 32px; text-decoration: none; border-radius: 8px; font-weight: bold; box-shadow: 0 4px 12px rgba(16, 185, 129, 0.3);'>Reset Password</a>"
                    +
                    "</div>" +

                    "<div style='background: rgba(255, 255, 255, 0.05); padding: 15px; border-radius: 8px; margin: 20px 0;'>"
                    +
                    "<p style='margin: 0; font-size: 12px; color: #9ca3af; word-break: break-all;'>Or copy this link: <span style='color: #10b981;'>"
                    + resetLink + "</span></p>" +
                    "</div>" +

                    "<p style='color: #9ca3af; font-size: 14px; margin-top: 30px;'>If you didn't request this password reset, please ignore this email. Your password will remain unchanged.</p>"
                    +
                    "<p style='color: #ef4444; font-size: 14px;'><strong>⚠️ For security reasons, never share this link with anyone.</strong></p>";

            helper.setText(getEmailTemplate(content), true);
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            System.err.println("Failed to send password reset email: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to send password reset email: " + e.getMessage());
        }
    }

    public void sendUsernameRecoveryEmail(String userEmail, String username) {
        try {
            System.out.println("Attempting to send username recovery email to: " + userEmail);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom("no-reply@codecombat.live");
            helper.setTo(userEmail);
            helper.setSubject("Username Recovery - CodeCombat");

            String content = "<h2 style='color: #10b981; margin-top: 0;'>Username Recovery</h2>" +
                    "<p style='font-size: 16px;'>Hello,</p>" +
                    "<p style='font-size: 16px; line-height: 1.6;'>We received a request to recover your username for CodeCombat.</p>"
                    +

                    "<div style='background: linear-gradient(135deg, rgba(16, 185, 129, 0.2) 0%, rgba(5, 150, 105, 0.2) 100%); padding: 25px; border-radius: 12px; margin: 25px 0; text-align: center; border: 2px solid rgba(16, 185, 129, 0.5);'>"
                    +
                    "<p style='margin: 0 0 10px 0; color: #9ca3af; font-size: 14px;'>Your username is:</p>" +
                    "<p style='margin: 0; font-size: 28px; font-weight: bold; color: #10b981; text-shadow: 0 2px 4px rgba(0,0,0,0.3);'>"
                    + username + "</p>" +
                    "</div>" +

                    "<p style='font-size: 16px; line-height: 1.6;'>You can now use this username to log in to your account.</p>"
                    +

                    "<div style='text-align: center; margin: 30px 0;'>" +
                    "<a href='https://codecombat.live/login' style='display: inline-block; background: linear-gradient(135deg, #10b981 0%, #059669 100%); color: white; padding: 14px 32px; text-decoration: none; border-radius: 8px; font-weight: bold; box-shadow: 0 4px 12px rgba(16, 185, 129, 0.3);'>Login Now</a>"
                    +
                    "</div>" +

                    "<p style='color: #9ca3af; font-size: 14px; margin-top: 30px;'>If you didn't request this information, please ignore this email.</p>";

            helper.setText(getEmailTemplate(content), true);

            System.out.println("Sending email...");
            mailSender.send(mimeMessage);
            System.out.println("Username recovery email sent successfully to: " + userEmail);

        } catch (MessagingException e) {
            System.err.println("MessagingException while sending username recovery email: " + e.getMessage());
            e.printStackTrace();

            // Fallback to plain text email
            try {
                System.out.println("Attempting fallback to plain text email...");
                SimpleMailMessage plainEmail = new SimpleMailMessage();
                plainEmail.setFrom("no-reply@codecombat.live");
                plainEmail.setTo(userEmail);
                plainEmail.setSubject("Username Recovery - CodeCombat");
                plainEmail.setText("Hello,\n\n" +
                        "We received a request to recover your username for CodeCombat.\n\n" +
                        "Your username is: " + username + "\n\n" +
                        "You can now use this username to log in at https://codecombat.live/login\n\n" +
                        "If you didn't request this, please ignore this email.\n\n" +
                        "Best regards,\nThe CodeCombat Team");

                mailSender.send(plainEmail);
                System.out.println("Plain text email sent successfully");
            } catch (Exception fallbackError) {
                System.err.println("Fallback email also failed: " + fallbackError.getMessage());
                fallbackError.printStackTrace();
                throw new RuntimeException("Failed to send username recovery email: " + e.getMessage());
            }
        } catch (Exception e) {
            System.err.println("Unexpected error sending username recovery email: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to send username recovery email: " + e.getMessage());
        }
    }
}
