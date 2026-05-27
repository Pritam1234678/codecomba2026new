package com.example.codecombat2026.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * Dual SMTP configuration for CodeCoder.
 *
 * <p>Two senders are wired:
 * <ul>
 *   <li><b>noreplyMailSender</b> (primary) — SendPulse relay, used for all
 *       transactional emails (welcome, password-reset, password-changed,
 *       username-recovery). Sender address: {@code noreply@codecoder.in}.</li>
 *   <li><b>supportMailSender</b> — Brevo relay, used for inbound support
 *       tickets forwarded to the admin inbox. Sender address:
 *       {@code support@codecoder.in}.</li>
 * </ul>
 *
 * <p>Both relays use STARTTLS on port 587 — the only outbound mail port
 * available on Oracle Cloud A1 Flex (port 25 is blocked by OCI policy).
 *
 * <p>Credentials are injected from environment variables so they never
 * appear in committed source. See {@code .env.example} for the required keys.
 */
@Configuration
public class MailConfig {

    // ── SendPulse (noreply) ───────────────────────────────────────────────────

    @Value("${SENDPULSE_HOST:smtp-pulse.com}")
    private String sendpulseHost;

    @Value("${SENDPULSE_PORT:587}")
    private int sendpulsePort;

    /** SendPulse SMTP login (your Gmail address registered with SendPulse). */
    @Value("${SENDPULSE_USERNAME:mandalpritam765@gmail.com}")
    private String sendpulseUsername;

    /** SendPulse SMTP password. */
    @Value("${SENDPULSE_PASSWORD}")
    private String sendpulsePassword;

    // ── Brevo (support) ───────────────────────────────────────────────────────

    @Value("${BREVO_HOST:smtp-relay.brevo.com}")
    private String brevoHost;

    @Value("${BREVO_PORT:587}")
    private int brevoPort;

    /** Brevo SMTP login (the @smtp-brevo.com address shown in the dashboard). */
    @Value("${BREVO_USERNAME:abca1b001@smtp-brevo.com}")
    private String brevoUsername;

    /** Brevo SMTP API key (used as the SMTP password). */
    @Value("${BREVO_PASSWORD}")
    private String brevoPassword;

    // ── Bean definitions ──────────────────────────────────────────────────────

    /**
     * Primary mail sender — SendPulse relay for {@code noreply@codecoder.in}.
     * Marked {@code @Primary} so Spring auto-wires this wherever a single
     * {@link JavaMailSender} is injected without a qualifier.
     */
    @Bean
    @Primary
    public JavaMailSender noreplyMailSender() {
        return buildSender(sendpulseHost, sendpulsePort, sendpulseUsername, sendpulsePassword);
    }

    /**
     * Secondary mail sender — Brevo relay for {@code support@codecoder.in}.
     * Inject with {@code @Qualifier("supportMailSender")}.
     */
    @Bean
    public JavaMailSender supportMailSender() {
        return buildSender(brevoHost, brevoPort, brevoUsername, brevoPassword);
    }

    // ── Shared builder ────────────────────────────────────────────────────────

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
        // Timeouts — prevent hanging on slow relay connections
        props.put("mail.smtp.connectiontimeout", "8000");
        props.put("mail.smtp.timeout", "8000");
        props.put("mail.smtp.writetimeout", "8000");
        // Disable SSL on the socket level — STARTTLS upgrades the plain connection
        props.put("mail.smtp.ssl.enable", "false");
        // Trust the relay's certificate (avoids SNI issues on some JDK versions)
        props.put("mail.smtp.ssl.trust", host);

        return sender;
    }
}
