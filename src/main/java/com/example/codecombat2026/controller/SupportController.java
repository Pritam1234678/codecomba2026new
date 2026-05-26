package com.example.codecombat2026.controller;

import com.example.codecombat2026.dto.SupportRequest;
import com.example.codecombat2026.service.EmailService;
import com.example.codecombat2026.service.TurnstileService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/support")
public class SupportController {

    @Autowired
    private EmailService emailService;

    @Autowired
    private TurnstileService turnstileService;

    @PostMapping("/send")
    public ResponseEntity<?> sendSupportEmail(@RequestBody SupportRequest request, HttpServletRequest httpRequest) {
        // Honeypot — bots typically fill the hidden 'website' field.
        if (request.getWebsite() != null && !request.getWebsite().isBlank()) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Support request sent successfully! We'll get back to you soon.");
            return ResponseEntity.ok(response);
        }

        // Cloudflare Turnstile verification.
        String clientIp = getClientIp(httpRequest);
        if (!turnstileService.verify(request.getTurnstileToken(), clientIp)) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Captcha verification failed. Please try again.");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            emailService.sendSupportEmail(
                    request.getEmail(),
                    request.getFullName(),
                    request.getPhone(),
                    request.getMessage());

            Map<String, String> response = new HashMap<>();
            response.put("message", "Support request sent successfully! We'll get back to you soon.");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to send support request. Please try again later.");
            return ResponseEntity.status(500).body(error);
        }
    }

    /** Extract client IP from common proxy headers (X-Forwarded-For wins, then X-Real-IP). */
    private String getClientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String real = req.getHeader("X-Real-IP");
        if (real != null && !real.isBlank()) return real;
        return req.getRemoteAddr();
    }
}
