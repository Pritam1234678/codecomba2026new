package com.example.codecombat2026.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class WelcomeController {

    @GetMapping("/")
    public Map<String, Object> welcome() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Welcome to Code Combat API");
        response.put("version", "1.0.0");
        response.put("status", "running");
        response.put("endpoints", Map.of(
                "auth", "/api/auth",
                "contests", "/api/contests",
                "problems", "/api/problems",
                "submissions", "/api/submissions",
                "test", "/api/test"));
        return response;
    }

    @GetMapping("/api")
    public Map<String, String> apiInfo() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Code Combat API v1.0");
        response.put("documentation", "Visit / for endpoint information");
        return response;
    }
}
