package com.example.codecombat2026.controller;

import com.example.codecombat2026.dto.MessageResponse;
import com.example.codecombat2026.dto.SubmissionRequest;
import com.example.codecombat2026.entity.Contest;
import com.example.codecombat2026.entity.PrivateContest;
import com.example.codecombat2026.entity.Submission;
import com.example.codecombat2026.exception.ConflictException;
import com.example.codecombat2026.exception.ForbiddenException;
import com.example.codecombat2026.exception.ResourceNotFoundException;
import com.example.codecombat2026.repository.PrivateContestParticipantRepository;
import com.example.codecombat2026.repository.PrivateContestRepository;
import com.example.codecombat2026.repository.SubmissionRepository;
import com.example.codecombat2026.security.services.UserDetailsImpl;
import com.example.codecombat2026.service.PrivateContestSubmissionService;
import com.example.codecombat2026.service.RateLimiterService;
import com.example.codecombat2026.service.SubmissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

/**
 * REST Controller for private contest submission operations.
 * 
 * Provides endpoints for:
 * - Submitting code to private contest problems (participants only)
 * - Listing all submissions for a private contest (host only)
 * - Filtering submissions by user, problem, or status
 * 
 * Integrates with:
 * - SubmissionService: Handles actual submission creation and judging queue
 * - PrivateContestParticipantRepository: Verifies participant access
 * - PrivateContestRepository: Verifies host access
 * - SSE (via SubmissionController): Real-time verdict delivery (reused)
 * 
 * Access Control:
 * - POST /submit: Requires participant status in the contest
 * - GET /submissions: Requires contest host status
 * 
 * Business Rules:
 * - Submissions only allowed when contest status is LIVE
 * - Uses existing submission rate limiting (5 submits per problem)
 * - Submissions go to private:submission:queue (not public queue)
 * - Verdicts delivered via existing SSE endpoint /api/submissions/stream
 * 
 * Requirements: 11.5, 13.1, 13.6
 */
@RestController
@RequestMapping("/api/contests/private")
public class PrivateContestSubmissionController {

    private static final Logger log = LoggerFactory.getLogger(PrivateContestSubmissionController.class);

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private PrivateContestSubmissionService privateContestSubmissionService;

    @Autowired
    private PrivateContestRepository privateContestRepository;

    @Autowired
    private PrivateContestParticipantRepository participantRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private RateLimiterService rateLimiter;

    /**
     * Submit code for a private contest problem.
     * 
     * This endpoint:
     * 1. Verifies the user is a participant in the private contest
     * 2. Verifies the contest is LIVE
     * 3. Applies rate limiting (same as public contests)
     * 4. Creates submission and pushes to private:submission:queue
     * 5. Returns submission ID immediately
     * 6. Verdict will be delivered via SSE to /api/submissions/stream
     * 
     * @param contestId The private contest ID (from private_contests table)
     * @param request SubmissionRequest containing problemId, code, language
     * @param userDetails Authenticated user details
     * @return ResponseEntity with Submission object (202 Accepted)
     * @throws ForbiddenException if user is not a participant
     * @throws ConflictException if contest is not LIVE
     * @throws ResourceNotFoundException if contest doesn't exist
     * 
     * Requirements: 11.5, 13.1, 13.6
     */
    @PostMapping("/{contestId}/submit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> submitCode(
            @PathVariable Long contestId,
            @RequestBody SubmissionRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        log.info("Private contest submission request from user {} for contest {}, problem {}",
                userDetails.getId(), contestId, request.getProblemId());

        // Rate limiting and the 5-submit hard cap are enforced here (controller layer).
        // Participant check, LIVE-status check, problem-in-contest check, private-queue
        // routing and privateContestId tagging are all handled by
        // PrivateContestSubmissionService.submitCode().

        // 1. Apply rate limiting (reuse existing rate limiter)
        if (!rateLimiter.allowSubmission(userDetails.getId())) {
            long retryAfter = rateLimiter.getRetryAfterSeconds(userDetails.getId());
            return ResponseEntity.status(429)
                    .header("Retry-After", String.valueOf(retryAfter))
                    .body(new MessageResponse("Too many submissions. Try again in " + retryAfter + "s"));
        }

        // 2. Hard cap: 5 submits per problem in a contest (no reset)
        long submitCount = submissionService.countContestSubmits(userDetails.getId(), request.getProblemId());
        if (submitCount >= 5) {
            return ResponseEntity.status(429)
                    .body(new MessageResponse("Submit limit reached (5/5). No more submissions allowed for this problem."));
        }

        // 3. Delegate to PrivateContestSubmissionService — it tags the job with
        //    privateContestId and pushes onto private:submission:queue (drained by VM3),
        //    so the private contest leaderboard/points logic fires correctly.
        Submission submission = privateContestSubmissionService.submitCode(
                contestId,
                request.getProblemId(),
                userDetails.getId(),
                request.getCode(),
                request.getLanguage()
        );

        log.info("Private contest submission created with ID {} for user {} in contest {}",
                submission.getId(), userDetails.getId(), contestId);

        return ResponseEntity.accepted().body(submission);
    }

    /**
     * Get all submissions for a private contest (host only).
     * 
     * This endpoint:
     * 1. Verifies the requesting user is the contest host
     * 2. Returns paginated list of all submissions
     * 3. Supports filtering by userId, problemId, and status
     * 
     * Query Parameters:
     * - userId (optional): Filter by participant user ID
     * - problemId (optional): Filter by problem ID
     * - status (optional): Filter by submission status (AC, WA, TLE, etc.)
     * - page (default: 0): Page number
     * - size (default: 20): Page size
     * 
     * @param contestId The private contest ID (from private_contests table)
     * @param userId Optional filter by participant user ID
     * @param problemId Optional filter by problem ID
     * @param status Optional filter by submission status
     * @param page Page number (default 0)
     * @param size Page size (default 20)
     * @param userDetails Authenticated user details
     * @return ResponseEntity with Page<Submission>
     * @throws ForbiddenException if user is not the contest host
     * @throws ResourceNotFoundException if contest doesn't exist
     * 
     * Requirements: 13.1
     */
    @GetMapping("/{contestId}/submissions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<Submission>> getContestSubmissions(
            @PathVariable Long contestId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long problemId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        log.info("Fetching submissions for private contest {} by user {} (filters: userId={}, problemId={}, status={})",
                contestId, userDetails.getId(), userId, problemId, status);

        // 1. Verify private contest exists
        PrivateContest privateContest = privateContestRepository.findById(contestId)
                .orElseThrow(() -> new ResourceNotFoundException("Private contest not found"));

        Contest contest = privateContest.getContest();
        Long actualContestId = contest.getId();

        // 2. Verify user is the contest host
        if (!privateContest.getHostUser().getId().equals(userDetails.getId())) {
            log.warn("User {} attempted to view submissions for private contest {} without being the host",
                    userDetails.getId(), contestId);
            throw new ForbiddenException("Only the contest host can view all submissions");
        }

        // 3. Build dynamic query with filters
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "submittedAt"));

        Specification<Submission> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always filter by contest ID
            predicates.add(criteriaBuilder.equal(root.get("contest").get("id"), actualContestId));

            // Filter out test runs (only show real submissions)
            predicates.add(criteriaBuilder.equal(root.get("testRun"), false));

            // Optional: Filter by user ID
            if (userId != null) {
                predicates.add(criteriaBuilder.equal(root.get("user").get("id"), userId));
            }

            // Optional: Filter by problem ID
            if (problemId != null) {
                predicates.add(criteriaBuilder.equal(root.get("problem").get("id"), problemId));
            }

            // Optional: Filter by status
            if (status != null && !status.isEmpty()) {
                try {
                    Submission.SubmissionStatus statusEnum = Submission.SubmissionStatus.valueOf(status.toUpperCase());
                    predicates.add(criteriaBuilder.equal(root.get("status"), statusEnum));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid status filter provided: {}", status);
                    // Invalid status, ignore filter
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<Submission> submissions = submissionRepository.findAll(spec, pageable);

        log.info("Returning {} submissions for private contest {}", submissions.getTotalElements(), contestId);

        return ResponseEntity.ok(submissions);
    }
}
