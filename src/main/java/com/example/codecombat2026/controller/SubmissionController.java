package com.example.codecombat2026.controller;

import com.example.codecombat2026.dto.MessageResponse;
import com.example.codecombat2026.dto.SubmissionRequest;
import com.example.codecombat2026.entity.Submission;
import com.example.codecombat2026.security.services.UserDetailsImpl;
import com.example.codecombat2026.service.RateLimiterService;
import com.example.codecombat2026.service.SseEmitterRegistry;
import com.example.codecombat2026.service.SseTicketService;
import com.example.codecombat2026.service.SubmissionService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    @Autowired private SubmissionService submissionService;
    @Autowired private SseEmitterRegistry sseRegistry;
    @Autowired private RateLimiterService rateLimiter;
    @Autowired private SseTicketService sseTickets;
    @Autowired private StringRedisTemplate redis;
    @Autowired private ObjectMapper objectMapper;

    // Cache keys / TTLs
    // submissions:user:{userId}          — user's full submission list (30s)
    // submission:status:{submissionId}   — single submission status (2s, for polling)
    // submission:user:problem:{userId}:{problemId} — latest per-problem (30s)
    private static final Duration USER_SUBS_TTL    = Duration.ofSeconds(30);
    private static final Duration STATUS_TTL       = Duration.ofSeconds(2);
    private static final Duration PER_PROBLEM_TTL  = Duration.ofSeconds(30);

    /**
     * Async submit — returns 202 Accepted immediately (< 10ms).
     * Verdict arrives via SSE on /api/submissions/stream.
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> submitCode(@RequestBody SubmissionRequest request,
                                        @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (!rateLimiter.allowSubmission(userDetails.getId())) {
            long retryAfter = rateLimiter.getRetryAfterSeconds(userDetails.getId());
            return ResponseEntity.status(429)
                .header("Retry-After", String.valueOf(retryAfter))
                .body(new MessageResponse("Too many submissions. Try again in " + retryAfter + "s"));
        }

        Submission submission = submissionService.submitCodeAsync(
            userDetails.getId(),
            request.getProblemId(),
            request.getCode(),
            request.getLanguage()
        );
        return ResponseEntity.accepted().body(submission);
    }

    /**
     * Test run — executes code but does NOT save to DB, does NOT upsert.
     * Verdict arrives via SSE with isTestRun=true flag.
     */
    @PostMapping("/test")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> testCode(@RequestBody SubmissionRequest request,
                                      @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (!rateLimiter.allowTestRun(userDetails.getId())) {
            long retryAfter = rateLimiter.getRetryAfterSeconds(userDetails.getId());
            return ResponseEntity.status(429)
                .header("Retry-After", String.valueOf(retryAfter))
                .body(new MessageResponse("Too many runs. Try again in " + retryAfter + "s"));
        }
        Submission submission = submissionService.testCodeAsync(
            userDetails.getId(),
            request.getProblemId(),
            request.getCode(),
            request.getLanguage()
        );
        return ResponseEntity.accepted().body(submission);
    }

    /**
     * Issue a single-use SSE ticket. The browser exchanges this for an SSE
     * subscription. Tickets live 60s, are bound to one userId, and are
     * deleted atomically on consumption — so even if the URL appears in a
     * proxy log, the credential is already useless.
     */
    @PostMapping("/sse-ticket")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> issueSseTicket(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        String ticket = sseTickets.issue(userDetails.getId());
        return ResponseEntity.ok(Map.of("ticket", ticket));
    }

    /**
     * SSE stream — client connects once, receives verdict when judging completes.
     *
     * Auth is via a single-use ticket exchanged at /sse-ticket. The endpoint
     * is permitAll() in SecurityConfig so async dispatch doesn't re-trigger
     * AuthorizationFilter.
     *
     * Returns 401 with no body if the ticket is missing/invalid/used; 200 OK
     * with an SseEmitter otherwise. We use a custom exception (handled in
     * GlobalExceptionHandler) to set 401 because returning {@code null} or
     * {@code ResponseEntity.status(401)} from an SSE-producing endpoint is
     * silently rewritten to 200 by Spring's emitter return-value handler.
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamVerdicts(
            @RequestParam(name = "ticket", required = false) String ticket,
            jakarta.servlet.http.HttpServletResponse response) {
        Long userId = sseTickets.consume(ticket);
        if (userId == null) {
            throw new SseAuthException();
        }
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        return sseRegistry.register(userId);
    }

    /** Marker exception → handled in GlobalExceptionHandler with a clean 401. */
    public static class SseAuthException extends RuntimeException {
        public SseAuthException() { super("Invalid or expired SSE ticket"); }
    }

    /**
     * Polling fallback — get current status of a specific submission.
     * Used if SSE connection drops. Cached 2s so rapid polling doesn't hammer DB.
     */
    @GetMapping("/{submissionId}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Submission> getSubmissionStatus(@PathVariable Long submissionId) {
        String key = "submission:status:" + submissionId;
        try {
            String cached = redis.opsForValue().get(key);
            if (cached != null) {
                return ResponseEntity.ok(objectMapper.readValue(cached, Submission.class));
            }
        } catch (Exception ignored) {}

        Submission sub = submissionService.getSubmissionById(submissionId);

        // Only cache terminal states — PENDING/JUDGING change rapidly, no point caching them.
        Submission.SubmissionStatus status = sub.getStatus();
        if (status != null && status != Submission.SubmissionStatus.PENDING
                && status != Submission.SubmissionStatus.JUDGING) {
            try {
                redis.opsForValue().set(key, objectMapper.writeValueAsString(sub), STATUS_TTL);
            } catch (Exception ignored) {}
        }

        return ResponseEntity.ok(sub);
    }

    @GetMapping("/run-count/{problemId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Long>> getRunCount(@PathVariable Long problemId,
                                                          @AuthenticationPrincipal UserDetailsImpl userDetails) {
        long count = submissionService.countContestRuns(userDetails.getId(), problemId);
        return ResponseEntity.ok(Map.of("runCount", count));
    }

    @GetMapping("/user/{problemId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Submission> getUserSubmission(@PathVariable Long problemId,
                                                        @AuthenticationPrincipal UserDetailsImpl userDetails) {
        String key = "submission:user:problem:" + userDetails.getId() + ":" + problemId;
        try {
            String cached = redis.opsForValue().get(key);
            if (cached != null) {
                Submission sub = objectMapper.readValue(cached, Submission.class);
                return ResponseEntity.ok(sub);
            }
        } catch (Exception ignored) {}

        Submission submission = submissionService.getSubmission(userDetails.getId(), problemId);
        if (submission == null) return ResponseEntity.notFound().build();

        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(submission), PER_PROBLEM_TTL);
        } catch (Exception ignored) {}

        return ResponseEntity.ok(submission);
    }

    @GetMapping("/user")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Submission>> getUserSubmissions(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        String key = "submissions:user:" + userDetails.getId();
        try {
            String cached = redis.opsForValue().get(key);
            if (cached != null) {
                List<Submission> subs = objectMapper.readValue(cached,
                    new TypeReference<List<Submission>>() {});
                return ResponseEntity.ok(subs);
            }
        } catch (Exception ignored) {}

        List<Submission> subs = submissionService.getUserSubmissions(userDetails.getId());

        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(subs), USER_SUBS_TTL);
        } catch (Exception ignored) {}

        return ResponseEntity.ok(subs);
    }
}
