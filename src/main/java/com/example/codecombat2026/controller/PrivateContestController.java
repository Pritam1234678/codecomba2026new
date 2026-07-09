package com.example.codecombat2026.controller;

import com.example.codecombat2026.dto.PrivateContestDTO;
import com.example.codecombat2026.security.services.UserDetailsImpl;
import com.example.codecombat2026.service.PrivateContestAccessValidator;
import com.example.codecombat2026.service.PrivateContestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Private Contest operations.
 * 
 * Provides endpoints for:
 * - Retrieving contest details (hosts and participants)
 * - Listing contests created by host
 * - Listing contests joined as participant
 * 
 * Security:
 * - All endpoints require ROLE_USER authentication
 * - Contest detail access validated: must be host OR participant
 * 
 * API Paths:
 * - GET /api/contests/private/{id} - Get contest details
 * - GET /api/contests/private/my-contests - List host's contests
 * - GET /api/contests/private/joined - List joined contests
 * 
 * Requirements: 11.2, 11.3, 11.4, 15.2 (proctoring visibility)
 */
@RestController
@RequestMapping("/api/contests/private")
public class PrivateContestController {

    private static final Logger log = LoggerFactory.getLogger(PrivateContestController.class);

    private final PrivateContestService privateContestService;
    private final PrivateContestAccessValidator accessValidator;

    public PrivateContestController(
            PrivateContestService privateContestService,
            PrivateContestAccessValidator accessValidator) {
        this.privateContestService = privateContestService;
        this.accessValidator = accessValidator;
    }

    /**
     * Get private contest details.
     * 
     * Returns full contest information including:
     * - Contest metadata (name, description, times, status)
     * - Host information
     * - Proctoring status (enableProctoring field)
     * - Participant count
     * - Cancellation status
     * 
     * Access control:
     * - User must be the contest host OR a registered participant
     * - Returns 403 Forbidden if neither condition is met
     * 
     * Response includes enableProctoring field to trigger proctoring
     * consent flow on frontend when true.
     * 
     * @param contestId The contest ID
     * @param userDetails The authenticated user
     * @return PrivateContestDTO with enableProctoring field
     * 
     * Requirements: 11.4, 15.2
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PrivateContestDTO> getContestDetails(
            @PathVariable("id") Long contestId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        
        log.debug("User {} requesting details for private contest {}", 
                userDetails.getUsername(), contestId);

        // Validate access: user must be host or participant
        accessValidator.validateAccess(contestId, userDetails.getId());
        
        // Retrieve contest details
        PrivateContestDTO contest = privateContestService.getPrivateContestById(contestId);
        
        log.debug("Retrieved private contest {} (proctoring: {}) for user {}", 
                contestId, contest.getEnableProctoring(), userDetails.getUsername());
        
        return ResponseEntity.ok(contest);
    }
}
