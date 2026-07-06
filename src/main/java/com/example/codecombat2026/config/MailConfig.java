package com.example.codecombat2026.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfig {

    @Value("${SENDPULSE_HOST:smtp-pulse.com}")
    private String sendpulseHost;

    @Value("${SENDPULSE_PORT:587}")
    private int sendpulsePort;

    @Value("${SENDPULSE_USERNAME:mandalpritam765@gmail.com}")
    private String sendpulseUsername;

    @Value("${SENDPULSE_PASSWORD}")
    private String sendpulsePassword;

    @Value("${BREVO_HOST:smtp-relay.brevo.com}")
    private String brevoHost;

    @Value("${BREVO_PORT:587}")
    private int brevoPort;

    @Value("${BREVO_USERNAME:abca1b001@smtp-brevo.com}")
    private String brevoUsername;

    @Value("${BREVO_PASSWORD:}")
    private String brevoPassword;

    @Bean
    @Primary
    public JavaMailSender noreplyMailSender() {
        return buildSender(sendpulseHost, sendpulsePort, sendpulseUsername, sendpulsePassword);
    }

    @Bean
    public JavaMailSender supportMailSender() {
        if (brevoPassword == null || brevoPassword.isBlank()) return null;
        return buildSender(brevoHost, brevoPort, brevoUsername, brevoPassword);
    }

    private JavaMailSender buildSender(String host, int port, String username, String password) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(username);
        sender.setPassword(password);
        sender.setDefaultEncoding("UTF-8");

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.connectiontimeout", "8000");
        props.put("mail.smtp.timeout", "8000");
        props.put("mail.smtp.writetimeout", "8000");
        props.put("mail.smtp.ssl.enable", "false");
        props.put("mail.smtp.ssl.trust", host);

        return sender;
    }
}
