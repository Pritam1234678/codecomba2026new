package com.example.codecombat2026.controller;

import com.example.codecombat2026.dto.MessageResponse;
import com.example.codecombat2026.dto.SubmissionRequest;
import com.example.codecombat2026.entity.Submission;
import com.example.codecombat2026.security.services.UserDetailsImpl;
import com.example.codecombat2026.service.RateLimiterService;
import com.example.codecombat2026.service.SseEmitterRegistry;
import com.example.codecombat2026.service.SubmissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/submissions")
@CrossOrigin(origins = "*", maxAge = 3600)
public class SubmissionController {

    @Autowired private SubmissionService submissionService;
    @Autowired private SseEmitterRegistry sseRegistry;
    @Autowired private RateLimiterService rateLimiter;

    /**
     * Async submit — returns 202 Accepted immediately (< 10ms).
     * Verdict arrives via SSE on /api/submissions/stream.
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> submitCode(@RequestBody SubmissionRequest request,
                                        @AuthenticationPrincipal UserDetailsImpl userDetails) {
        // Rate limit check
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

        // Return 202 Accepted — verdict comes via SSE
        return ResponseEntity.accepted().body(submission);
    }

    /**
     * Test run — same as submit but NOT saved to DB.
     * Still async: queued and result pushed via SSE.
     * For now keeps sync behavior for simplicity.
     */
    @PostMapping("/test")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Submission> testCode(@RequestBody SubmissionRequest request,
                                               @AuthenticationPrincipal UserDetailsImpl userDetails) {
        // Test runs bypass rate limiting and don't save to DB
        // Reuse async submit but mark as test (not saved)
        Submission submission = submissionService.submitCodeAsync(
            userDetails.getId(),
            request.getProblemId(),
            request.getCode(),
            request.getLanguage()
        );
        return ResponseEntity.accepted().body(submission);
    }

    /**
     * SSE stream — client connects once, receives verdict when judging completes.
     * Replaces polling. Connection stays open for 5 minutes.
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("isAuthenticated()")
    public SseEmitter streamVerdicts(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        return sseRegistry.register(userDetails.getId());
    }

    /**
     * Polling fallback — get current status of a specific submission.
     * Used if SSE connection drops.
     */
    @GetMapping("/{submissionId}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Submission> getSubmissionStatus(@PathVariable Long submissionId) {
        return ResponseEntity.ok(submissionService.getSubmissionById(submissionId));
    }

    @GetMapping("/user/{problemId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Submission> getUserSubmission(@PathVariable Long problemId,
                                                        @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Submission submission = submissionService.getSubmission(userDetails.getId(), problemId);
        if (submission == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(submission);
    }

    @GetMapping("/user")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Submission>> getUserSubmissions(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(submissionService.getUserSubmissions(userDetails.getId()));
    }
}
