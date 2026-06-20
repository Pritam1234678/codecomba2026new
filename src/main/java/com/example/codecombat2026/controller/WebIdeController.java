package com.example.codecombat2026.controller;

import com.example.codecombat2026.dto.MessageResponse;
import com.example.codecombat2026.entity.WebContestTemplate;
import com.example.codecombat2026.repository.WebContestTemplateRepository;
import com.example.codecombat2026.service.WebIdeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * "Into the Web" — local code-server session endpoints.
 *
 * These {@code /local/**} endpoints run on the EXECUTION VM (VM3/VM4/VM5). They
 * spawn and manage code-server processes locally. VM1 (the orchestrator) routes
 * session requests to the correct VM by language and forwards the caller's JWT,
 * which is why every endpoint requires authentication.
 */
@RestController
@RequestMapping("/api/web-ide")
public class WebIdeController {

    private static final Logger log = LoggerFactory.getLogger(WebIdeController.class);

    @Autowired private WebIdeService webIdeService;
    @Autowired private WebContestTemplateRepository templateRepository;

    /**
     * POST /api/web-ide/local/start
     * Body: { sessionId, templatePath, language }
     * If templatePath is omitted, it is resolved from problemId + language.
     * Returns: { sessionId, port }
     */
    @PostMapping("/local/start")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> localStart(@RequestBody Map<String, String> body) {
        String sessionId = body.get("sessionId");
        String language = body.getOrDefault("language", "JAVA");
        String templatePath = body.get("templatePath");

        if (sessionId == null || sessionId.isBlank()) {
            return ResponseEntity.badRequest().body(new MessageResponse("sessionId is required"));
        }

        // Resolve template path from problemId + language if not supplied directly.
        if ((templatePath == null || templatePath.isBlank()) && body.get("problemId") != null) {
            try {
                Long problemId = Long.parseLong(body.get("problemId"));
                Optional<WebContestTemplate> template =
                    templateRepository.findByProblemIdAndLanguage(problemId, language.toUpperCase());
                if (template.isPresent()) {
                    templatePath = template.get().getTemplatePath();
                } else {
                    return ResponseEntity.badRequest()
                        .body(new MessageResponse("No template for problem " + problemId + " language " + language));
                }
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().body(new MessageResponse("Invalid problemId"));
            }
        }

        try {
            WebIdeService.IdeSession session = webIdeService.startSession(sessionId, templatePath, language);
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("sessionId", session.sessionId);
            resp.put("port", session.port);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            log.error("Failed to start Web IDE session {}: {}", sessionId, e.getMessage());
            return ResponseEntity.internalServerError()
                .body(new MessageResponse("Failed to start session: " + e.getMessage()));
        }
    }

    /**
     * POST /api/web-ide/local/stop
     * Body: { sessionId }
     */
    @PostMapping("/local/stop")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> localStop(@RequestBody Map<String, String> body) {
        String sessionId = body.get("sessionId");
        if (sessionId == null || sessionId.isBlank()) {
            return ResponseEntity.badRequest().body(new MessageResponse("sessionId is required"));
        }
        try {
            webIdeService.stopSession(sessionId);
            return ResponseEntity.ok(new MessageResponse("Session stopped"));
        } catch (Exception e) {
            log.error("Failed to stop Web IDE session {}: {}", sessionId, e.getMessage());
            return ResponseEntity.internalServerError()
                .body(new MessageResponse("Failed to stop session: " + e.getMessage()));
        }
    }

    /**
     * POST /api/web-ide/local/test
     * Body: { sessionId, submit: bool }
     * Runs the workspace test suite and returns the verdict.
     */
    @PostMapping("/local/test")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> localTest(@RequestBody Map<String, Object> body) {
        Object sessionIdObj = body.get("sessionId");
        if (sessionIdObj == null || sessionIdObj.toString().isBlank()) {
            return ResponseEntity.badRequest().body(new MessageResponse("sessionId is required"));
        }
        String sessionId = sessionIdObj.toString();
        boolean submit = Boolean.parseBoolean(String.valueOf(body.getOrDefault("submit", false)));

        try {
            WebIdeService.TestResult result = webIdeService.runTest(sessionId, submit);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to run test for Web IDE session {}: {}", sessionId, e.getMessage());
            return ResponseEntity.internalServerError()
                .body(new MessageResponse("Failed to run test: " + e.getMessage()));
        }
    }

    /**
     * POST /api/web-ide/local/heartbeat
     * Body: { sessionId }
     */
    @PostMapping("/local/heartbeat")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> localHeartbeat(@RequestBody Map<String, String> body) {
        String sessionId = body.get("sessionId");
        if (sessionId == null || sessionId.isBlank()) {
            return ResponseEntity.badRequest().body(new MessageResponse("sessionId is required"));
        }
        webIdeService.touchSession(sessionId);
        boolean alive = webIdeService.getSession(sessionId) != null;
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("sessionId", sessionId);
        resp.put("alive", alive);
        return ResponseEntity.ok(resp);
    }

    /**
     * GET /api/web-ide/local/sessions — list active sessions (debug).
     */
    @GetMapping("/local/sessions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> localSessions() {
        return ResponseEntity.ok(webIdeService.listSessions());
    }
}
