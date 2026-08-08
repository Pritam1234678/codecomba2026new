package com.example.codecombat2026.sqljudge.controller;

import com.example.codecombat2026.dto.MessageResponse;
import com.example.codecombat2026.security.services.UserDetailsImpl;
import com.example.codecombat2026.service.RateLimiterService;
import com.example.codecombat2026.service.SseEmitterRegistry;
import com.example.codecombat2026.service.SseTicketService;
import com.example.codecombat2026.sqljudge.config.SqlJudgeProperties;
import com.example.codecombat2026.sqljudge.dto.SqlProblemView;
import com.example.codecombat2026.sqljudge.dto.SqlResult;
import com.example.codecombat2026.sqljudge.dto.SqlSubmissionRequest;
import com.example.codecombat2026.sqljudge.dto.SqlSubmissionStatusResponse;
import com.example.codecombat2026.sqljudge.entity.SqlProblem;
import com.example.codecombat2026.sqljudge.entity.SqlSubmission;
import com.example.codecombat2026.sqljudge.repository.SqlProblemRepository;
import com.example.codecombat2026.sqljudge.repository.SqlSubmissionRepository;
import com.example.codecombat2026.sqljudge.service.SqlSubmissionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * Candidate-facing SQL judge API.
 *
 * <ul>
 *   <li>{@code GET /api/sql/problems} — enabled questions (no internals exposed).</li>
 *   <li>{@code POST /api/sql/problems/{id}/run} — execute + preview, no comparison.</li>
 *   <li>{@code POST /api/sql/problems/{id}/submit} — execute + compare to expected.</li>
 *   <li>{@code GET /api/sql/submissions/...} — poll status / history.</li>
 *   <li>{@code GET /api/sql/stream} — single-use-ticket SSE for live verdicts.</li>
 * </ul>
 *
 * <p>All endpoints are authenticated (SecurityConfig {@code anyRequest().authenticated()});
 * the SSE stream is filter-level {@code permitAll} because {@code @PreAuthorize} does not
 * survive Spring's async dispatch — the ticket consume in {@link #streamVerdicts} is the
 * real auth gate.
 */
@RestController
@RequestMapping("/api/sql")
public class SqlJudgeController {

    @Autowired private SqlProblemRepository problemRepository;
    @Autowired private SqlSubmissionRepository submissionRepository;
    @Autowired private SqlSubmissionService submissionService;
    @Autowired private SqlJudgeProperties properties;
    @Autowired private RateLimiterService rateLimiter;
    @Autowired private SseEmitterRegistry sseRegistry;
    @Autowired private SseTicketService sseTickets;
    @Autowired private ObjectMapper objectMapper;

    // ─── Problems ────────────────────────────────────────────────────────────

    @GetMapping("/problems")
    @PreAuthorize("isAuthenticated()")
    public List<SqlProblemView> listProblems() {
        return SqlProblemView.fromAll(problemRepository.findByEnabledTrueOrderByCreatedAtDesc());
    }

    @GetMapping("/problems/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getProblem(@PathVariable Long id) {
        SqlProblem p = problemRepository.findById(id).orElse(null);
        if (p == null || !p.isEnabled()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse("Problem not found"));
        }
        return ResponseEntity.ok(SqlProblemView.from(p));
    }

    // ─── RUN / SUBMIT ────────────────────────────────────────────────────────

    @PostMapping("/problems/{id}/run")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> run(@PathVariable Long id,
                                 @RequestBody SqlSubmissionRequest req,
                                 @AuthenticationPrincipal UserDetailsImpl user) {
        return queueSubmission(id, req, user, true);
    }

    @PostMapping("/problems/{id}/submit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> submit(@PathVariable Long id,
                                    @RequestBody SqlSubmissionRequest req,
                                    @AuthenticationPrincipal UserDetailsImpl user) {
        return queueSubmission(id, req, user, false);
    }

    private ResponseEntity<?> queueSubmission(Long problemId, SqlSubmissionRequest req,
                                              UserDetailsImpl user, boolean testRun) {
        if (req == null || req.getSql() == null || req.getSql().isBlank()) {
            return ResponseEntity.badRequest().body(new MessageResponse("SQL query is required"));
        }
        if (req.getSql().length() > properties.getMaxSqlLength()) {
            return ResponseEntity.badRequest().body(new MessageResponse(
                "Query too long (max " + properties.getMaxSqlLength() + " chars)"));
        }

        String rateKey = "sqljudge:rate:" + (testRun ? "run" : "submit") + ":" + user.getId();
        int limit = testRun ? properties.getRunRateLimit() : properties.getSubmitRateLimit();
        if (!rateLimiter.allow(rateKey, limit, properties.getRateWindowSeconds())) {
            long retryAfter = rateLimiter.retryAfterSeconds(rateKey, properties.getRateWindowSeconds());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(retryAfter))
                .body(new MessageResponse("Rate limit exceeded. Try again in " + retryAfter + "s."));
        }

        try {
            SqlSubmission submission = submissionService.submit(user.getId(), problemId, req.getSql(), testRun);
            return ResponseEntity.ok(Map.of(
                "submissionId", submission.getId(),
                "status", submission.getStatus().name(),
                "testRun", testRun));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new MessageResponse("Failed to queue submission"));
        }
    }

    // ─── Submission status / history ─────────────────────────────────────────

    @GetMapping("/submissions/{submissionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> submissionStatus(@PathVariable Long submissionId,
                                              @AuthenticationPrincipal UserDetailsImpl user) {
        SqlSubmission s = submissionRepository.findById(submissionId).orElse(null);
        if (s == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse("Submission not found"));
        }
        if (!s.getUserId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new MessageResponse("Not your submission"));
        }
        SqlResult preview = null;
        if (s.getResultPreview() != null && !s.getResultPreview().isBlank()) {
            try {
                preview = objectMapper.readValue(s.getResultPreview(), SqlResult.class);
            } catch (Exception ignored) {}
        }
        return ResponseEntity.ok(SqlSubmissionStatusResponse.from(s, preview));
    }

    @GetMapping("/submissions")
    @PreAuthorize("isAuthenticated()")
    public List<SqlSubmissionStatusResponse> mySubmissions(
            @AuthenticationPrincipal UserDetailsImpl user,
            @RequestParam(defaultValue = "10") int limit) {
        int capped = Math.max(1, Math.min(limit, 50));
        return submissionRepository
            .findByUserIdOrderBySubmittedAtDesc(user.getId(), PageRequest.of(0, capped))
            .getContent().stream()
            .map(s -> SqlSubmissionStatusResponse.from(s, null))
            .toList();
    }

    // ─── SSE verdict stream (single-use ticket, same pattern as code judge) ──

    @PostMapping("/sse-ticket")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> issueSseTicket(@AuthenticationPrincipal UserDetailsImpl user) {
        return ResponseEntity.ok(Map.of("ticket", sseTickets.issue(user.getId())));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamVerdicts(
            @RequestParam(name = "ticket", required = false) String ticket,
            jakarta.servlet.http.HttpServletResponse response) {
        Long userId = sseTickets.consume(ticket);
        if (userId == null) {
            throw new com.example.codecombat2026.controller.SubmissionController.SseAuthException();
        }
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        return sseRegistry.register(userId);
    }
}
