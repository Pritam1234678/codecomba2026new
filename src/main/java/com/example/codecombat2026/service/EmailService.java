package com.example.codecombat2026.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendSupportEmail(String fromEmail, String name, String phone, String message) {
        try {
            SimpleMailMessage email = new SimpleMailMessage();
            email.setTo("mandalpritam756@gmail.com");
            email.setSubject("CodeCombat Support Request from " + name);

            String emailBody = "New Support Request\n\n" +
                    "Name: " + name + "\n" +
                    "Email: " + fromEmail + "\n" +
                    "Phone: " + (phone != null && !phone.isEmpty() ? phone : "Not provided") + "\n\n" +
                    "Message:\n" + message + "\n\n" +
                    "---\n" +
                    "This email was sent from CodeCombat Support Form";

            email.setText(emailBody);
            email.setReplyTo(fromEmail);

            mailSender.send(email);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }

    public void sendWelcomeEmail(String userEmail, String fullName) {
        try {
            SimpleMailMessage email = new SimpleMailMessage();
            email.setFrom("no-reply@codecombat.live");
            email.setTo(userEmail);
            email.setSubject("Welcome to CodeCombat! 🎉");

            String emailBody = "Dear " + fullName + ",\n\n" +
                    "Welcome to CodeCombat! 🚀\n\n" +
                    "Thank you for registering with us. We're excited to have you join our competitive programming community!\n\n"
                    +
                    "Here's what you can do now:\n" +
                    "✅ Participate in coding contests\n" +
                    "✅ Solve challenging problems\n" +
                    "✅ Compete with other programmers\n" +
                    "✅ Track your progress on the leaderboard\n\n" +
                    "Get started by exploring our active contests and problems!\n\n" +
                    "If you have any questions or need help, feel free to contact us at support@codecombat.live\n\n" +
                    "Happy Coding! 💻\n\n" +
                    "Best regards,\n" +
                    "The CodeCombat Team\n\n" +
                    "---\n" +
                    "Visit us at: http://codecombat.live";

            email.setText(emailBody);
            mailSender.send(email);
        } catch (Exception e) {
            // Log error but don't throw - registration should succeed even if email fails
            System.err.println("Failed to send welcome email: " + e.getMessage());
        }
    }
}
