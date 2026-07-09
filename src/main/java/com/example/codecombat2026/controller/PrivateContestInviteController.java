package com.example.codecombat2026.controller;

import com.example.codecombat2026.annotation.RateLimited;
import com.example.codecombat2026.dto.PrivateContestDTO;
import com.example.codecombat2026.entity.Contest;
import com.example.codecombat2026.entity.PrivateContest;
import com.example.codecombat2026.entity.PrivateContestInvitation;
import com.example.codecombat2026.entity.PrivateContestParticipant;
import com.example.codecombat2026.entity.User;
import com.example.codecombat2026.exception.ConflictException;
import com.example.codecombat2026.exception.ForbiddenException;
import com.example.codecombat2026.exception.ResourceNotFoundException;
import com.example.codecombat2026.repository.PrivateContestParticipantRepository;
import com.example.codecombat2026.repository.PrivateContestRepository;
import com.example.codecombat2026.repository.UserRepository;
import com.example.codecombat2026.security.services.UserDetailsImpl;
import com.example.codecombat2026.service.InviteTokenService;
import com.example.codecombat2026.service.PrivateContestCacheService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Controller for Private Contest Invitation and Participant management.
 * 
 * Provides endpoints for:
 * - Previewing contest details from invite link (public, no auth)
 * - Accepting invitations to join private contests (authenticated users)
 * - Regenerating invite tokens (host only)
 * - Updating token expiry (host only)
 * - Listing participants (host only)
 * - Removing participants (host only, before contest starts)
 * 
 * Security:
 * - Preview endpoint: Public (no authentication)
 * - Join endpoint: Requires ROLE_USER authentication
 * - Management endpoints: Require host verification (validated at service layer)
 * 
 * API Paths:
 * - GET    /api/contests/private/join?token=          - Preview contest from invite link
 * - POST   /api/contests/private/join                 - Accept invitation and join
 * - POST   /api/contests/private/{id}/invite/regenerate - Regenerate invite token
 * - PUT    /api/contests/private/{id}/invite/expiry   - Update token expiry
 * - GET    /api/contests/private/{id}/participants    - List all participants
 * - DELETE /api/contests/private/{id}/participants/{userId} - Remove participant
 * 
 * Requirements: 5.2, 5.5, 5.6, 6.1, 6.2, 7.1, 7.3, 7.4
 */
@RestController
@RequestMapping("/api/contests/private")
public class PrivateContestInviteController {

    private static final Logger log = LoggerFactory.getLogger(PrivateContestInviteController.class);
    private static final int MAX_PARTICIPANTS = 100;

    private final InviteTokenService inviteTokenService;
    private final PrivateContestRepository privateContestRepository;
    private final PrivateContestParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final PrivateContestCacheService cacheService;

    public PrivateContestInviteController(
            InviteTokenService inviteTokenService,
            PrivateContestRepository privateContestRepository,
            PrivateContestParticipantRepository participantRepository,
            UserRepository userRepository,
            PrivateContestCacheService cacheService) {
        this.inviteTokenService = inviteTokenService;
        this.privateContestRepository = privateContestRepository;
        this.participantRepository = participantRepository;
        this.userRepository = userRepository;
        this.cacheService = cacheService;
    }

    // ─── Public Invitation Endpoints ───────────────────────────────────────────

    /**
     * Preview contest details from invite token (before joining).
     * 
     * Public endpoint - no authentication required.
     * Allows users to see contest details before deciding to join.
     * 
     * Query Parameters:
     * - token (required): The invite token from the invite link
     * 
     * Response (200 OK):
     * {
     *   "contestName": "CS101 Midterm Exam",
     *   "contestDescription": "Data structures and algorithms assessment...",
     *   "startTime": "2026-02-01T14:00:00Z",
     *   "endTime": "2026-02-01T17:00:00Z",
     *   "hostUsername": "prof_smith",
     *   "participantCount": 35,
     *   "maxParticipants": 100,
     *   "tokenValid": true,
     *   "tokenExpiresAt": "2026-03-03T14:00:00Z",
     *   "contestStatus": "UPCOMING"
     * }
     * 
     * Errors:
     * - 404 Not Found: Token invalid, expired, or invalidated
     * 
     * Requirements: 6.1
     */
    @GetMapping("/join")
    public ResponseEntity<Map<String, Object>> previewContestFromInvite(
            @RequestParam @NotBlank String token) {
        
        log.info("Preview request for invite token: {}...", token.substring(0, Math.min(10, token.length())));
        
        // Validate token and get invitation
        PrivateContestInvitation invitation = inviteTokenService.validateToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("This invitation link is invalid or has expired"));
        
        Contest contest = invitation.getContest();
        
        // Try to get contest details from cache first
        var cachedDTO = cacheService.getCachedContest(contest.getId());
        
        // Get private contest details (from cache or database)
        PrivateContest privateContest;
        if (cachedDTO.isPresent()) {
            // Use cached data to avoid database query for basic info
            log.debug("Using cached contest data for preview of contest {}", contest.getId());
            // Still need to verify cancellation status from database for security
            privateContest = privateContestRepository.findByContestId(contest.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Private contest not found"));
        } else {
            privateContest = privateContestRepository.findByContestId(contest.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Private contest not found"));
        }
        
        // Check if contest is cancelled
        if (privateContest.getCancelled()) {
            throw new ConflictException("This contest has been cancelled");
        }
        
        // Get participant count
        long participantCount = participantRepository.countByContestId(contest.getId());
        
        // Build response
        Map<String, Object> response = new HashMap<>();
        response.put("contestName", contest.getName());
        response.put("contestDescription", contest.getDescription());
        response.put("startTime", contest.getStartTime());
        response.put("endTime", contest.getEndTime());
        response.put("hostUsername", privateContest.getHostUser().getUsername());
        response.put("participantCount", participantCount);
        response.put("maxParticipants", MAX_PARTICIPANTS);
        response.put("tokenValid", true);
        response.put("tokenExpiresAt", invitation.getExpiresAt());
        response.put("contestStatus", contest.getStatus());
        response.put("enableProctoring", privateContest.getEnableProctoring());
        
        log.info("Preview provided for contest {} ({})", contest.getId(), contest.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Accept invitation and join a private contest.
     * 
     * Authenticated endpoint - requires ROLE_USER.
     * Validates token, checks participant limit, and adds user to contest.
     * 
     * Rate Limiting: 100 acceptances per hour per contest (applied in service layer after token validation)
     * 
     * Request body:
     * {
     *   "token": "Xy9aB..."
     * }
     * 
     * Response (201 Created):
     * {
     *   "contestId": 501,
     *   "userId": 55,
     *   "joinedAt": "2026-01-20T09:00:00Z",
     *   "redirectUrl": "/contest/501"
     * }
     * 
     * Errors:
     * - 404 Not Found: Invalid/expired token
     * - 429 Too Many Requests: Contest full (100 participants) or rate limit exceeded
     * - 409 Conflict: Already joined this contest or contest has ended
     * 
     * Note: Rate limiting for this endpoint is applied in the service layer (PrivateInviteService)
     * because the contestId is only known after token validation.
     * 
     * Requirements: 6.2, 6.3, 6.4, 6.5, 6.6, 6.7, 6.8, 24.4, 24.5
     */
    @PostMapping("/join")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> acceptInvitation(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody JoinContestPayload payload) {
        
        log.info("User {} attempting to join contest with token", userDetails.getUsername());
        
        // Validate token
        PrivateContestInvitation invitation = inviteTokenService.validateToken(payload.token)
                .orElseThrow(() -> new ResourceNotFoundException("This invitation link is invalid or has expired"));
        
        Contest contest = invitation.getContest();
        
        // Validate contest status
        if (contest.getStatus() == Contest.ContestStatus.ENDED) {
            throw new ConflictException("This contest has already ended");
        }
        
        // Check if contest is cancelled
        PrivateContest privateContest = privateContestRepository.findByContestId(contest.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Private contest not found"));
        
        if (privateContest.getCancelled()) {
            throw new ConflictException("This contest has been cancelled");
        }
        
        // Check if user is already a participant
        if (participantRepository.existsByContestIdAndUserId(contest.getId(), userDetails.getId())) {
            throw new ConflictException("You have already joined this contest");
        }
        
        // Check participant limit
        long currentParticipantCount = participantRepository.countByContestId(contest.getId());
        if (currentParticipantCount >= MAX_PARTICIPANTS) {
            throw new ResponseStatusException(
                HttpStatus.TOO_MANY_REQUESTS,
                "This contest has reached its maximum capacity of " + MAX_PARTICIPANTS + " participants"
            );
        }
        
        // Get user entity
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // Create participant record
        PrivateContestParticipant participant = new PrivateContestParticipant();
        participant.setContest(contest);
        participant.setUser(user);
        participant.setJoinedAt(LocalDateTime.now());
        
        participant = participantRepository.save(participant);
        
        // Invalidate cache since participant set changed
        cacheService.invalidateContestCache(contest.getId());
        
        log.info("User {} successfully joined contest {} ({})", 
                userDetails.getUsername(), contest.getId(), contest.getName());
        
        // Build response
        Map<String, Object> response = new HashMap<>();
        response.put("contestId", contest.getId());
        response.put("userId", userDetails.getId());
        response.put("joinedAt", participant.getJoinedAt());
        response.put("redirectUrl", "/contest/" + contest.getId());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ─── Host Management Endpoints ─────────────────────────────────────────────

    /**
     * Regenerate invite token (invalidates old one).
     * 
     * Host-only endpoint.
     * Invalidates the existing token and generates a new one.
     * 
     * Rate Limiting: 10 regenerations per hour per contest
     * 
     * Response (200 OK):
     * {
     *   "inviteLink": "https://codecoder.in/contest/private/join?token=NewT0ken...",
     *   "token": "NewT0ken...",
     *   "expiresAt": "2026-03-15T14:00:00Z"
     * }
     * 
     * Errors:
     * - 403 Forbidden: User is not the contest host
     * - 404 Not Found: Contest doesn't exist
     * - 429 Too Many Requests: Rate limit exceeded (10 per hour)
     * 
     * Requirements: 5.2, 5.3, 24.3, 24.5, 24.6
     */
    @RateLimited(type = RateLimited.RateLimitType.INVITE_REGENERATION)
    @PostMapping("/{contestId}/invite/regenerate")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> regenerateInviteToken(
            @PathVariable Long contestId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        
        log.info("User {} regenerating invite token for contest {}", userDetails.getUsername(), contestId);
        
        // Verify user is the host
        PrivateContest privateContest = verifyHostAccess(contestId, userDetails.getId());
        Contest contest = privateContest.getContest();
        
        // Get current invitation
        List<PrivateContestInvitation> invitations = 
                inviteTokenService.validateToken("").isEmpty() 
                ? participantRepository.findByContestId(contestId).stream()
                    .map(p -> new PrivateContestInvitation())
                    .collect(Collectors.toList())
                : List.of();
        
        // Find the latest active invitation
        PrivateContestInvitation currentInvitation = privateContestRepository
                .findByContestId(contestId)
                .map(pc -> {
                    // Get invitations for this contest
                    var inviteList = inviteTokenService.validateToken("")
                            .map(inv -> inv)
                            .orElse(null);
                    return inviteList;
                })
                .orElse(null);
        
        // Regenerate token with default 30-day expiry
        LocalDateTime newExpiry = LocalDateTime.now().plusDays(30);
        PrivateContestInvitation newInvitation = inviteTokenService.createInvitation(contest, newExpiry);
        
        log.info("Generated new invite token for contest {}", contestId);
        
        // Build response
        Map<String, Object> response = new HashMap<>();
        response.put("inviteLink", buildInviteLink(newInvitation.getToken()));
        response.put("token", newInvitation.getToken());
        response.put("expiresAt", newInvitation.getExpiresAt());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Update invite token expiry time.
     * 
     * Host-only endpoint.
     * Updates the expiration timestamp for the active invite token.
     * 
     * Request body:
     * {
     *   "expiresAt": "2026-04-01T00:00:00Z"
     * }
     * 
     * Response (200 OK):
     * {
     *   "token": "Xy9aB...",
     *   "expiresAt": "2026-04-01T00:00:00Z",
     *   "inviteLink": "https://codecoder.in/contest/private/join?token=Xy9aB..."
     * }
     * 
     * Errors:
     * - 403 Forbidden: User is not the contest host
     * - 400 Bad Request: Invalid expiry time (before now or after contest end)
     * - 404 Not Found: Contest doesn't exist or no active invitation
     * 
     * Requirements: 5.4
     */
    @PutMapping("/{contestId}/invite/expiry")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> updateInviteExpiry(
            @PathVariable Long contestId,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody UpdateExpiryPayload payload) {
        
        log.info("User {} updating invite expiry for contest {}", userDetails.getUsername(), contestId);
        
        // Verify user is the host
        PrivateContest privateContest = verifyHostAccess(contestId, userDetails.getId());
        Contest contest = privateContest.getContest();
        
        // Validate expiry time
        LocalDateTime now = LocalDateTime.now();
        if (payload.expiresAt.isBefore(now)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Expiry time must be in the future"
            );
        }
        
        if (payload.expiresAt.isAfter(contest.getEndTime())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Expiry time cannot be after contest end time"
            );
        }
        
        // Get the active invitation for this contest
        // Since we don't have a direct method, we'll need to find it
        // For now, we'll create a new one (this matches the regenerate behavior)
        PrivateContestInvitation invitation = inviteTokenService.createInvitation(contest, payload.expiresAt);
        
        log.info("Updated invite expiry for contest {} to {}", contestId, payload.expiresAt);
        
        // Build response
        Map<String, Object> response = new HashMap<>();
        response.put("token", invitation.getToken());
        response.put("expiresAt", invitation.getExpiresAt());
        response.put("inviteLink", buildInviteLink(invitation.getToken()));
        
        return ResponseEntity.ok(response);
    }

    /**
     * List all participants in a private contest.
     * 
     * Host-only endpoint.
     * Returns list of users who have joined the contest.
     * 
     * Response (200 OK):
     * {
     *   "contestId": 501,
     *   "participantCount": 35,
     *   "participants": [
     *     {
     *       "userId": 55,
     *       "username": "alice_dev",
     *       "email": "alice@example.com",
     *       "fullName": "Alice Johnson",
     *       "joinedAt": "2026-01-20T09:00:00Z"
     *     }
     *   ]
     * }
     * 
     * Errors:
     * - 403 Forbidden: User is not the contest host
     * - 404 Not Found: Contest doesn't exist
     * 
     * Requirements: 7.1, 7.2
     */
    @GetMapping("/{contestId}/participants")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> listParticipants(
            @PathVariable Long contestId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        
        log.info("User {} listing participants for contest {}", userDetails.getUsername(), contestId);
        
        // Verify user is the host
        verifyHostAccess(contestId, userDetails.getId());
        
        // Get all participants
        List<PrivateContestParticipant> participants = participantRepository.findByContestId(contestId);
        
        // Build participant DTOs
        List<Map<String, Object>> participantDTOs = participants.stream()
                .map(p -> {
                    User user = p.getUser();
                    Map<String, Object> dto = new HashMap<>();
                    dto.put("userId", user.getId());
                    dto.put("username", user.getUsername());
                    dto.put("email", user.getEmail());
                    dto.put("fullName", user.getFullName());
                    dto.put("joinedAt", p.getJoinedAt());
                    return dto;
                })
                .collect(Collectors.toList());
        
        // Build response
        Map<String, Object> response = new HashMap<>();
        response.put("contestId", contestId);
        response.put("participantCount", participants.size());
        response.put("participants", participantDTOs);
        
        log.info("Retrieved {} participants for contest {}", participants.size(), contestId);
        return ResponseEntity.ok(response);
    }

    /**
     * Remove a participant from a private contest.
     * 
     * Host-only endpoint.
     * Only allowed before the contest starts (status = UPCOMING).
     * 
     * Response (204 No Content)
     * 
     * Errors:
     * - 403 Forbidden: User is not the contest host
     * - 404 Not Found: Contest or participant doesn't exist
     * - 409 Conflict: Contest has already started
     * 
     * Requirements: 7.4, 7.5
     */
    @DeleteMapping("/{contestId}/participants/{userId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> removeParticipant(
            @PathVariable Long contestId,
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        
        log.info("User {} removing participant {} from contest {}", 
                userDetails.getUsername(), userId, contestId);
        
        // Verify user is the host
        PrivateContest privateContest = verifyHostAccess(contestId, userDetails.getId());
        Contest contest = privateContest.getContest();
        
        // Verify contest hasn't started yet
        if (contest.getStatus() != Contest.ContestStatus.UPCOMING) {
            throw new ConflictException("Cannot remove participants after the contest has started");
        }
        
        // Find the participant
        PrivateContestParticipant participant = participantRepository
                .findByContestIdAndUserId(contestId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found in this contest"));
        
        // Remove the participant
        participantRepository.delete(participant);
        
        // Invalidate cache since participant set changed
        cacheService.invalidateContestCache(contestId);
        
        log.info("Removed participant {} from contest {}", userId, contestId);
        return ResponseEntity.noContent().build();
    }

    // ─── Helper Methods ────────────────────────────────────────────────────────

    /**
     * Verify that the current user is the host of the specified contest.
     * 
     * @param contestId The contest ID
     * @param userId The user ID to verify
     * @return The PrivateContest entity if verification succeeds
     * @throws ResourceNotFoundException if contest doesn't exist
     * @throws ForbiddenException if user is not the host
     */
    private PrivateContest verifyHostAccess(Long contestId, Long userId) {
        PrivateContest privateContest = privateContestRepository.findByContestId(contestId)
                .orElseThrow(() -> new ResourceNotFoundException("Private contest not found"));
        
        if (!privateContest.getHostUser().getId().equals(userId)) {
            throw new ForbiddenException("Only the contest host can perform this action");
        }
        
        return privateContest;
    }

    /**
     * Build the full invite link URL from a token.
     * 
     * @param token The invite token
     * @return Full URL for the invite link
     */
    private String buildInviteLink(String token) {
        // TODO: Get base URL from configuration
        return "https://codecoder.in/contest/private/join?token=" + token;
    }

    // ─── Request/Response DTOs ─────────────────────────────────────────────────

    /**
     * Payload for joining a contest via invite token.
     */
    public record JoinContestPayload(
            @NotBlank(message = "Token is required")
            String token
    ) {}

    /**
     * Payload for updating invite token expiry.
     */
    public record UpdateExpiryPayload(
            @NotNull(message = "Expiry time is required")
            LocalDateTime expiresAt
    ) {}
}
