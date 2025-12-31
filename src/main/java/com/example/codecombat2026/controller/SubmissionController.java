package com.example.codecombat2026.controller;

import com.example.codecombat2026.dto.SubmissionRequest;
import com.example.codecombat2026.entity.Submission;
import com.example.codecombat2026.security.services.UserDetailsImpl;
import com.example.codecombat2026.service.SubmissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/submissions")
@CrossOrigin(origins = "*", maxAge = 3600)
public class SubmissionController {
    @Autowired
    private SubmissionService submissionService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Submission> submitCode(@RequestBody SubmissionRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Submission submission = submissionService.submitCode(
                userDetails.getId(),
                request.getProblemId(),
                request.getCode(),
                request.getLanguage());
        return ResponseEntity.ok(submission);
    }

    @PostMapping("/test")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Submission> testCode(@RequestBody SubmissionRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Submission testResult = submissionService.testCode(
                userDetails.getId(),
                request.getProblemId(),
                request.getCode(),
                request.getLanguage());
        return ResponseEntity.ok(testResult);
    }

    @GetMapping("/user/{problemId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Submission> getUserSubmission(@PathVariable Long problemId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Submission submission = submissionService.getSubmission(userDetails.getId(), problemId);
        if (submission == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(submission);
    }

    @GetMapping("/user")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Submission>> getUserSubmissions(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        List<Submission> submissions = submissionService.getUserSubmissions(userDetails.getId());
        return ResponseEntity.ok(submissions);
    }
}
