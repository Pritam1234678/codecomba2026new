package com.example.codecombat2026.controller;

import com.example.codecombat2026.dto.MessageResponse;
import com.example.codecombat2026.entity.Submission;
import com.example.codecombat2026.entity.WebContestTemplate;
import com.example.codecombat2026.security.services.UserDetailsImpl;
import com.example.codecombat2026.service.RateLimiterService;
import com.example.codecombat2026.service.SubmissionService;
import com.example.codecombat2026.service.WebContestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
    @PreAuthorize("hasRole('ADMIN')")
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
    @PreAuthorize("hasRole('ADMIN')")
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

    // ── User: list all challenges ─────────────────────────────────────────────

    /**
     * GET /api/web-contest/list
     * Returns all problems that have web contest templates.
     */
    @GetMapping("/list")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> listChallenges() {
        return ResponseEntity.ok(webContestService.listChallenges());
    }

    // ── Admin: template management ────────────────────────────────────────────

    /**
     * GET /api/admin/web-contest/templates — list all templates (admin)
     */
    @GetMapping("/admin/templates")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> adminListTemplates() {
        return ResponseEntity.ok(webContestService.adminListTemplates());
    }

    /**
     * POST /api/admin/web-contest/templates — create a new template (admin)
     * Body: { problemId, language, templatePath }
     */
    @PostMapping("/admin/templates")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> adminCreateTemplate(@RequestBody AdminCreateTemplateRequest req) {
        try {
            WebContestTemplate t = webContestService.adminCreateTemplate(req.problemId(), req.language(), req.templatePath());
            return ResponseEntity.ok(t);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new MessageResponse("Failed to create template: " + e.getMessage()));
        }
    }

    /**
     * GET /api/admin/web-contest/templates/{id}/files — get all files including hidden (admin)
     */
    @GetMapping("/admin/templates/{id}/files")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> adminGetFiles(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(webContestService.adminGetAllFiles(id));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * POST /api/admin/web-contest/templates/{id}/files — save a file (admin)
     * Body: { path: "relative/path/file.java", content: "..." }
     */
    @PostMapping("/admin/templates/{id}/files")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> adminSaveFile(@PathVariable Long id, @RequestBody AdminFileRequest req) {
        try {
            webContestService.adminSaveFile(id, req.path(), req.content());
            return ResponseEntity.ok(new MessageResponse("File saved"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * DELETE /api/admin/web-contest/templates/{id}/files?path=... — delete a file (admin)
     */
    @DeleteMapping("/admin/templates/{id}/files")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> adminDeleteFile(@PathVariable Long id, @RequestParam String path) {
        try {
            webContestService.adminDeleteFile(id, path);
            return ResponseEntity.ok(new MessageResponse("File deleted"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * GET /api/admin/web-contest/templates/{id}/manifest — get manifest (admin)
     */
    @GetMapping("/admin/templates/{id}/manifest")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> adminGetManifest(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(webContestService.adminGetManifest(id));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * PUT /api/admin/web-contest/templates/{id}/manifest — update manifest (admin)
     */
    @PutMapping("/admin/templates/{id}/manifest")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> adminUpdateManifest(@PathVariable Long id, @RequestBody Map<String, Object> manifest) {
        try {
            webContestService.adminUpdateManifest(id, manifest);
            return ResponseEntity.ok(new MessageResponse("Manifest updated"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    // ── Request bodies ─────────────────────────────────────────────────────────

    public record AdminCreateTemplateRequest(
        Long problemId,
        String language,
        String templatePath
    ) {}

    public record AdminFileRequest(
        String path,
        String content
    ) {}
}
