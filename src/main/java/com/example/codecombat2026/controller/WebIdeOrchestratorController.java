package com.example.codecombat2026.controller;

import com.example.codecombat2026.dto.MessageResponse;
import com.example.codecombat2026.entity.WebContestTemplate;
import com.example.codecombat2026.repository.WebContestTemplateRepository;
import com.example.codecombat2026.security.services.UserDetailsImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Orchestrator controller (runs on VM1).
 *
 * User-facing endpoints that determine which execution VM to route to by language,
 * then forward the request to that VM's {@code /api/web-ide/local/*} endpoints via HTTP.
 * The caller's JWT is forwarded so the execution VM can authenticate the request.
 */
@RestController
@RequestMapping("/api/web-ide")
public class WebIdeOrchestratorController {

    private static final Logger log = LoggerFactory.getLogger(WebIdeOrchestratorController.class);

    // ── VM host mapping per language ──────────────────────────────────────────
    @Value("${WEB_IDE_VM_JAVA:http://10.0.0.228:8080}")
    private String javaVmUrl;

    @Value("${WEB_IDE_VM_PYTHON:http://10.0.0.187:8080}")
    private String pythonVmUrl;

    @Value("${WEB_IDE_VM_NODE:http://20.194.7.164:8080}")
    private String nodeVmUrl;

    // ── Subdomain base for building the IDE URL ──────────────────────────────
    @Value("${WEB_IDE_SUBDOMAIN_BASE:ide.codecoder.in}")
    private String subdomainBase;

    @Autowired
    private RestTemplate webIdeRestTemplate;

    @Autowired
    private WebContestTemplateRepository templateRepository;

    // ═══════════════════════════════════════════════════════════════════════════
    // Endpoints
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * POST /api/web-ide/session/start
     * Body: { problemId: 48, language: "JAVA" }
     *
     * Flow:
     *   1. Generate sessionId = userId + "-" + problemId + "-" + shortUUID
     *   2. Look up template by problemId + language
     *   3. Pick VM by language
     *   4. POST to {vmUrl}/api/web-ide/local/start with { sessionId, templatePath, language }
     *   5. Get back { port }
     *   6. Build URL: https://{vmKey}-{port}.{subdomainBase}/
     *   7. Return to frontend: { sessionId, url, language }
     */
    @PostMapping("/session/start")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> startSession(@RequestBody Map<String, Object> body,
                                          @AuthenticationPrincipal UserDetailsImpl user,
                                          HttpServletRequest request) {
        // Extract inputs
        Object problemIdObj = body.get("problemId");
        if (problemIdObj == null) {
            return ResponseEntity.badRequest().body(new MessageResponse("problemId is required"));
        }
        Long problemId;
        try {
            problemId = Long.parseLong(problemIdObj.toString());
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Invalid problemId"));
        }

        String language = body.getOrDefault("language", "JAVA").toString().toUpperCase();

        // 1. Generate sessionId
        String shortUuid = UUID.randomUUID().toString().substring(0, 8);
        String sessionId = user.getId() + "-" + problemId + "-" + shortUuid;

        // 2. Look up template
        Optional<WebContestTemplate> templateOpt =
                templateRepository.findByProblemIdAndLanguage(problemId, language);
        if (templateOpt.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("No template for problem " + problemId + " language " + language));
        }
        String templatePath = templateOpt.get().getTemplatePath();

        // 3. Pick VM
        String vmUrl = vmUrlFor(language);

        // 4. Forward to execution VM
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("sessionId", sessionId);
        payload.put("templatePath", templatePath);
        payload.put("language", language);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(payload, forwardAuth(request));

        Map<String, Object> vmResponse;
        try {
            ResponseEntity<Map> resp = webIdeRestTemplate.exchange(
                    vmUrl + "/api/web-ide/local/start",
                    HttpMethod.POST, entity, Map.class);
            vmResponse = resp.getBody();
        } catch (ResourceAccessException e) {
            log.error("Execution VM unreachable for language {}: {}", language, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new MessageResponse("Execution VM unreachable for " + language + ". Please try again later."));
        } catch (HttpStatusCodeException e) {
            log.error("Execution VM returned error for start: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode())
                    .body(new MessageResponse("VM error: " + e.getResponseBodyAsString()));
        }

        // 5. Extract port from VM response
        Object portObj = vmResponse != null ? vmResponse.get("port") : null;
        if (portObj == null) {
            return ResponseEntity.internalServerError()
                    .body(new MessageResponse("VM did not return a port"));
        }

        // 6. Build URL
        String vmKey = vmKeyFor(language);
        String ideUrl = "https://" + vmKey + "-" + portObj + "." + subdomainBase + "/";

        // 7. Return to frontend
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", sessionId);
        result.put("url", ideUrl);
        result.put("language", language);
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/web-ide/session/stop — { sessionId, language }
     * Routes to correct VM's /local/stop.
     */
    @PostMapping("/session/stop")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> stopSession(@RequestBody Map<String, String> body,
                                         HttpServletRequest request) {
        String sessionId = body.get("sessionId");
        String language = body.getOrDefault("language", "JAVA").toUpperCase();

        if (sessionId == null || sessionId.isBlank()) {
            return ResponseEntity.badRequest().body(new MessageResponse("sessionId is required"));
        }

        String vmUrl = vmUrlFor(language);
        Map<String, String> payload = Map.of("sessionId", sessionId);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(payload, forwardAuth(request));

        try {
            ResponseEntity<Map> resp = webIdeRestTemplate.exchange(
                    vmUrl + "/api/web-ide/local/stop",
                    HttpMethod.POST, entity, Map.class);
            return ResponseEntity.ok(resp.getBody());
        } catch (ResourceAccessException e) {
            log.error("Execution VM unreachable for stop (language={}): {}", language, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new MessageResponse("Execution VM unreachable. Please try again later."));
        } catch (HttpStatusCodeException e) {
            log.error("Execution VM error on stop: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode())
                    .body(new MessageResponse("VM error: " + e.getResponseBodyAsString()));
        }
    }

    /**
     * POST /api/web-ide/session/test — { sessionId, language, submit: bool }
     * Routes to correct VM's /local/test. Returns the test verdict.
     */
    @PostMapping("/session/test")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> testSession(@RequestBody Map<String, Object> body,
                                         HttpServletRequest request) {
        Object sessionIdObj = body.get("sessionId");
        if (sessionIdObj == null || sessionIdObj.toString().isBlank()) {
            return ResponseEntity.badRequest().body(new MessageResponse("sessionId is required"));
        }
        String sessionId = sessionIdObj.toString();
        String language = body.getOrDefault("language", "JAVA").toString().toUpperCase();
        boolean submit = Boolean.parseBoolean(String.valueOf(body.getOrDefault("submit", false)));

        String vmUrl = vmUrlFor(language);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sessionId", sessionId);
        payload.put("submit", submit);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, forwardAuth(request));

        try {
            ResponseEntity<Map> resp = webIdeRestTemplate.exchange(
                    vmUrl + "/api/web-ide/local/test",
                    HttpMethod.POST, entity, Map.class);
            return ResponseEntity.ok(resp.getBody());
        } catch (ResourceAccessException e) {
            log.error("Execution VM unreachable for test (language={}): {}", language, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new MessageResponse("Execution VM unreachable. Please try again later."));
        } catch (HttpStatusCodeException e) {
            log.error("Execution VM error on test: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode())
                    .body(new MessageResponse("VM error: " + e.getResponseBodyAsString()));
        }
    }

    /**
     * POST /api/web-ide/session/heartbeat — { sessionId, language }
     * Keeps session alive (called by frontend every 60s).
     */
    @PostMapping("/session/heartbeat")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> heartbeat(@RequestBody Map<String, String> body,
                                       HttpServletRequest request) {
        String sessionId = body.get("sessionId");
        String language = body.getOrDefault("language", "JAVA").toUpperCase();

        if (sessionId == null || sessionId.isBlank()) {
            return ResponseEntity.badRequest().body(new MessageResponse("sessionId is required"));
        }

        String vmUrl = vmUrlFor(language);
        Map<String, String> payload = Map.of("sessionId", sessionId);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(payload, forwardAuth(request));

        try {
            ResponseEntity<Map> resp = webIdeRestTemplate.exchange(
                    vmUrl + "/api/web-ide/local/heartbeat",
                    HttpMethod.POST, entity, Map.class);
            return ResponseEntity.ok(resp.getBody());
        } catch (ResourceAccessException e) {
            log.error("Execution VM unreachable for heartbeat (language={}): {}", language, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new MessageResponse("Execution VM unreachable."));
        } catch (HttpStatusCodeException e) {
            log.error("Execution VM error on heartbeat: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode())
                    .body(new MessageResponse("VM error: " + e.getResponseBodyAsString()));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════════════════

    /** Pick VM URL by language. */
    private String vmUrlFor(String language) {
        return switch (language.toUpperCase()) {
            case "PYTHON" -> pythonVmUrl;
            case "NODEJS", "NODE", "JAVASCRIPT" -> nodeVmUrl;
            default -> javaVmUrl; // JAVA and anything else
        };
    }

    /** Pick vmKey for subdomain construction. */
    private String vmKeyFor(String language) {
        return switch (language.toUpperCase()) {
            case "PYTHON" -> "python";
            case "NODEJS", "NODE", "JAVASCRIPT" -> "node";
            default -> "java";
        };
    }

    /** Forward JWT from incoming request to outgoing VM request. */
    private HttpHeaders forwardAuth(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String auth = request.getHeader("Authorization");
        if (auth != null) {
            headers.set("Authorization", auth);
        }
        return headers;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // RestTemplate Bean configuration
    // ═══════════════════════════════════════════════════════════════════════════

    @Configuration
    static class WebIdeRestTemplateConfig {

        @Bean
        public RestTemplate webIdeRestTemplate() {
            return new RestTemplate();
        }
    }
}
