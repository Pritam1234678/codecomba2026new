package com.example.codecombat2026.controller;

import com.example.codecombat2026.dto.SupportRequest;
import com.example.codecombat2026.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/support")
@CrossOrigin(origins = "*")
public class SupportController {

    @Autowired
    private EmailService emailService;

    @PostMapping("/send")
    public ResponseEntity<?> sendSupportEmail(@RequestBody SupportRequest request) {
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
}
