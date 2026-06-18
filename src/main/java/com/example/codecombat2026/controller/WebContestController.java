package com.example.codecombat2026.controller;

import com.example.codecombat2026.dto.MessageResponse;
import com.example.codecombat2026.entity.Submission;
import com.example.codecombat2026.security.services.UserDetailsImpl;
import com.example.codecombat2026.service.RateLimiterService;
import com.example.codecombat2026.service.SubmissionService;
import com.example.codecombat2026.service.WebContestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/web-contest")
public class WebContestController {

    @Autowired private WebContestService webContestService;
    @Autowired private SubmissionService submissionService;
    @Autowired private RateLimiterService rateLimiter;

    /**
     * GET /api/web-contest/problems/{problemId}/template
     * Returns visible files (editable + readonly) from manifest. NEVER returns hidden files.
     */
    @GetMapping("/problems/{problemId}/template")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getTemplate(
            @PathVariable Long problemId,
            @RequestParam(defaultValue = "JAVA") String language) {
        Map<String, Object> template = webContestService.getTemplate(problemId, language);
        return ResponseEntity.ok(template);
    }

    /**
     * POST /api/web-contest/run
     * Test run — pushes to web-contest:queue with isTestRun=true.
     * Returns 202 with the submission (contains submissionId for SSE tracking).
     */
    @PostMapping("/run")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> runCode(@RequestBody WebContestRunRequest request,
                                     @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (!rateLimiter.allowTestRun(userDetails.getId())) {
            long retryAfter = rateLimiter.getRetryAfterSeconds(userDetails.getId());
            return ResponseEntity.status(429)
                .header("Retry-After", String.valueOf(retryAfter))
                .body(new MessageResponse("Too many runs. Try again in " + retryAfter + "s"));
        }

        Submission submission = webContestService.runCode(
            userDetails.getId(),
            request.problemId(),
            request.editableFiles(),
            request.language() != null ? request.language() : "JAVA"
        );
        return ResponseEntity.accepted().body(submission);
    }

    /**
     * POST /api/web-contest/submit
     * Real submit — pushes to web-contest:queue with isTestRun=false.
     * 5 submit cap per problem (same as contest).
     */
    @PostMapping("/submit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> submitCode(@RequestBody WebContestRunRequest request,
                                        @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (!rateLimiter.allowSubmission(userDetails.getId())) {
            long retryAfter = rateLimiter.getRetryAfterSeconds(userDetails.getId());
            return ResponseEntity.status(429)
                .header("Retry-After", String.valueOf(retryAfter))
                .body(new MessageResponse("Too many submissions. Try again in " + retryAfter + "s"));
        }

        // Hard cap: 5 submits per problem
        long submitCount = submissionService.countContestSubmits(userDetails.getId(), request.problemId());
        if (submitCount >= 5) {
            return ResponseEntity.status(429)
                .body(new MessageResponse("Submit limit reached (5/5). No more submissions allowed for this problem."));
        }

        Submission submission = webContestService.submitCode(
            userDetails.getId(),
            request.problemId(),
            request.editableFiles(),
            request.language() != null ? request.language() : "JAVA"
        );
        return ResponseEntity.accepted().body(submission);
    }

    /**
     * Request body for /run and /submit.
     */
    public record WebContestRunRequest(
        Long problemId,
        Map<String, String> editableFiles,
        String language
    ) {}
}
