package com.example.codecombat2026.controller;

import com.example.codecombat2026.security.services.UserDetailsImpl;
import com.example.codecombat2026.service.GitHubService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/github")
@PreAuthorize("isAuthenticated()")
public class GitHubController {

    @Autowired
    private GitHubService gitHubService;

    @Autowired
    private com.example.codecombat2026.repository.UserRepository userRepository;

    @PostMapping("/connect")
    public ResponseEntity<?> connect(@RequestBody Map<String, String> body,
                                      @AuthenticationPrincipal UserDetailsImpl user) {
        String code = body.get("code");
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "code is required"));
        }
        try {
            Map<String, String> result = gitHubService.exchangeToken(code);
            var u = userRepository.findById(user.getId()).orElseThrow();
            u.setGithubToken(result.get("token"));
            u.setGithubUsername(result.get("username"));
            userRepository.save(u);
            return ResponseEntity.ok(Map.of("connected", true, "username", result.get("username")));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<?> status(@AuthenticationPrincipal UserDetailsImpl user) {
        boolean connected = gitHubService.isConnected(user.getId());
        return ResponseEntity.ok(Map.of("connected", connected));
    }

    @PostMapping("/disconnect")
    public ResponseEntity<?> disconnect(@AuthenticationPrincipal UserDetailsImpl user) {
        var u = userRepository.findById(user.getId()).orElseThrow();
        u.setGithubToken(null);
        u.setGithubUsername(null);
        userRepository.save(u);
        return ResponseEntity.ok(Map.of("disconnected", true));
    }
}
