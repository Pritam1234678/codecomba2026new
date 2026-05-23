package com.example.codecombat2026.controller;

import com.example.codecombat2026.security.services.UserDetailsImpl;
import com.example.codecombat2026.service.CompilerService;
import com.example.codecombat2026.service.CompilerService.CompilerResponse;
import com.example.codecombat2026.service.WsTicketService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

/**
 * Public compiler — REST run is anonymous (like onecompiler/programiz),
 * the interactive WebSocket requires a single-use ticket bound to a logged-in
 * user. Per-IP rate limit (10 runs / 30s) still applies on the public path.
 * Uses a separate thread pool (CompilerService) so it never interferes
 * with the contest judge engine.
 */
@RestController
@RequestMapping("/api/compiler")
@CrossOrigin(origins = "*", maxAge = 3600)
public class CompilerController {

    @Autowired private CompilerService compilerService;
    @Autowired private StringRedisTemplate redis;
    @Autowired private WsTicketService wsTickets;

    private static final int MAX_RUNS_PER_WINDOW = 10;
    private static final int RATE_WINDOW_SECONDS = 30;
    private static final int MAX_CODE_LENGTH = 50_000;
    private static final int MAX_STDIN_LENGTH = 10_000;

    @PostMapping("/run")
    public ResponseEntity<?> runCode(@RequestBody CompileRequest request, HttpServletRequest httpReq) {
        // Validation
        if (request.code == null || request.code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Code cannot be empty"));
        }
        if (request.code.length() > MAX_CODE_LENGTH) {
            return ResponseEntity.badRequest().body(Map.of("error", "Code too long (max " + MAX_CODE_LENGTH + " chars)"));
        }
        if (request.stdin != null && request.stdin.length() > MAX_STDIN_LENGTH) {
            return ResponseEntity.badRequest().body(Map.of("error", "Input too long (max " + MAX_STDIN_LENGTH + " chars)"));
        }
        if (request.language == null || request.language.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Language is required"));
        }

        // Per-IP rate limit
        String ip = getClientIp(httpReq);
        if (!allowRequest(ip)) {
            long retry = getRetryAfter(ip);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(retry))
                .body(Map.of("error", "Too many requests. Try again in " + retry + "s."));
        }

        // Hard timeout 15s (queue wait + 5s execution + buffer)
        CompilerResponse result = compilerService.compile(
            request.code,
            request.language,
            request.stdin,
            15
        );

        // If queue rejected the job, return 503
        if (result.errorMessage != null && result.errorMessage.startsWith("Server busy")) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(result);
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/status")
    public ResponseEntity<?> status() {
        return ResponseEntity.ok(Map.of(
            "activeJobs", compilerService.getActiveJobs(),
            "queueDepth", compilerService.getQueueDepth()
        ));
    }

    /**
     * Issue a single-use ticket for the interactive WebSocket compiler.
     * Anonymous users do NOT need a ticket — the WS handler will accept
     * unauthenticated upgrades subject to the public rate limit. Logged-in
     * users get a higher session quota and a verified userId in audit logs.
     */
    @PostMapping("/ws-ticket")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> issueWsTicket(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        String ticket = wsTickets.issue(userDetails.getId());
        return ResponseEntity.ok(Map.of("ticket", ticket));
    }

    // ─── Rate limiting (per IP) ───────────────────────────────────────────────

    private boolean allowRequest(String ip) {
        String key = "compiler:rl:" + ip;
        try {
            Long count = redis.opsForValue().increment(key);
            if (count != null && count == 1) {
                redis.expire(key, Duration.ofSeconds(RATE_WINDOW_SECONDS));
            }
            return count != null && count <= MAX_RUNS_PER_WINDOW;
        } catch (Exception e) {
            // Redis down — fail open
            return true;
        }
    }

    private long getRetryAfter(String ip) {
        try {
            Long ttl = redis.getExpire("compiler:rl:" + ip);
            return ttl != null && ttl > 0 ? ttl : RATE_WINDOW_SECONDS;
        } catch (Exception e) {
            return RATE_WINDOW_SECONDS;
        }
    }

    private String getClientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String real = req.getHeader("X-Real-IP");
        if (real != null && !real.isBlank()) return real;
        return req.getRemoteAddr();
    }

    // ─── DTO ──────────────────────────────────────────────────────────────────

    public static class CompileRequest {
        public String code;
        public String language; // JAVA / CPP / C / PYTHON / JAVASCRIPT
        public String stdin;
    }
}
