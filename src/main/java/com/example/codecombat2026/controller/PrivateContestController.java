package com.example.codecombat2026.controller;

import com.example.codecombat2026.dto.PrivateContestDTO;
import com.example.codecombat2026.dto.ProblemDTO;
import com.example.codecombat2026.entity.Problem;
import com.example.codecombat2026.repository.ContestProblemRepository;
import com.example.codecombat2026.repository.PrivateContestParticipantRepository;
import com.example.codecombat2026.repository.PrivateContestRepository;
import com.example.codecombat2026.security.services.UserDetailsImpl;
import com.example.codecombat2026.service.ContestProblemService;
import com.example.codecombat2026.service.PrivateContestAccessValidator;
import com.example.codecombat2026.service.PrivateContestService;
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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Controller for Private Contest operations.
 * 
 * Provides endpoints for:
 * - Creating private contests
 * - Retrieving contest details (hosts and participants)
 * - Listing contests created by host
 * - Listing contests joined as participant
 * - Cancelling a contest before it starts
 * - Cloning an ended contest to reuse its problem set and settings
 * 
 * Security:
 * - All endpoints require ROLE_USER authentication
 * - Contest detail access validated: must be host OR participant
 * - Cancel/clone are host-only, enforced in the service layer
 * 
 * API Paths:
 * - POST   /api/contests/private - Create a private contest (Contest_Host only)
 * - GET    /api/contests/private/{id} - Get contest details (host or participant)
 * - GET    /api/contests/private/my-contests - List host's contests
 * - GET    /api/contests/private/joined - List joined contests
 * - PUT    /api/contests/private/{id}/cancel - Cancel a contest (host only)
 * - POST   /api/contests/private/{id}/clone - Clone an ended contest (host only)
 * 
 * Requirements: 4.1, 11.2, 11.3, 11.4, 15.2 (proctoring visibility), 18.1, 33.1
 */
@RestController
@RequestMapping("/api/contests/private")
public class PrivateContestController {

    private static final Logger log = LoggerFactory.getLogger(PrivateContestController.class);

    private final PrivateContestService privateContestService;
    private final PrivateContestAccessValidator accessValidator;
    private final PrivateContestRepository privateContestRepository;
    private final PrivateContestParticipantRepository participantRepository;
    private final ContestProblemRepository contestProblemRepository;
    private final ContestProblemService contestProblemService;

    public PrivateContestController(
            PrivateContestService privateContestService,
            PrivateContestAccessValidator accessValidator,
            PrivateContestRepository privateContestRepository,
            PrivateContestParticipantRepository participantRepository,
            ContestProblemRepository contestProblemRepository,
            ContestProblemService contestProblemService) {
        this.privateContestService = privateContestService;
        this.accessValidator = accessValidator;
        this.privateContestRepository = privateContestRepository;
        this.participantRepository = participantRepository;
        this.contestProblemRepository = contestProblemRepository;
        this.contestProblemService = contestProblemService;
    }

    /**
     * Create a new private contest.
     * 
     * Request body:
     * {
     *   "name": "CS101 Midterm Exam",
     *   "description": "Data structures and algorithms assessment",
     *   "startTime": "2026-02-01T14:00:00",
     *   "endTime": "2026-02-01T17:00:00",
     *   "enableProctoring": false
     * }
     * 
     * Response (201 Created): PrivateContestDTO including inviteLink.
     * 
     * Errors:
     * - 403 Forbidden: user is not an approved Contest_Host
     * - 429 Too Many Requests: monthly quota (2/month) exceeded
     * - 400 Bad Request: duration exceeds 5 hours
     * - 409 Conflict: time window overlaps another of the host's contests
     * 
     * Requirements: 4.1
     */
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PrivateContestDTO> createPrivateContest(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody CreateContestRequest request) {

        log.info("User {} creating private contest: {}", userDetails.getUsername(), request.name());

        PrivateContestDTO dto = new PrivateContestDTO();
        dto.setName(request.name());
        dto.setDescription(request.description());
        dto.setStartTime(request.startTime());
        dto.setEndTime(request.endTime());
        dto.setEnableProctoring(request.enableProctoring());

        PrivateContestDTO created = privateContestService.createPrivateContest(dto, userDetails.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Get private contest details.
     * 
     * Returns full contest information including:
     * - Contest metadata (name, description, times, status)
     * - Host information
     * - Proctoring status (enableProctoring field)
     * - Participant count and problem count
     * - Attached problems (for participants to render the contest arena)
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
     * @return Contest details merged with participantCount, problemCount, and problems
     * 
     * Requirements: 11.4, 15.2
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getContestDetails(
            @PathVariable("id") Long contestId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        
        log.debug("User {} requesting details for private contest {}", 
                userDetails.getUsername(), contestId);

        // Validate access: user must be host or participant
        accessValidator.validateAccess(contestId, userDetails.getId());
        
        // Retrieve contest details
        PrivateContestDTO contest = privateContestService.getPrivateContestById(contestId);

        Map<String, Object> response = toEnrichedMap(contest, contestId);

        // Attach problem list — needed by the participant-facing arena page.
        List<Problem> problems = contestProblemService.listProblemsForContest(contestId);
        response.put("problems", problems.stream().map(this::toProblemDTO).collect(Collectors.toList()));
        
        log.debug("Retrieved private contest {} (proctoring: {}) for user {}", 
                contestId, contest.getEnableProctoring(), userDetails.getUsername());
        
        return ResponseEntity.ok(response);
    }

    /**
     * List private contests hosted by the current user.
     * 
     * Response (200 OK): array of contest objects, each including
     * participantCount and problemCount.
     * 
     * Requirements: 11.2
     */
    @GetMapping("/my-contests")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<Map<String, Object>>> getMyContests(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        log.debug("User {} listing hosted private contests", userDetails.getUsername());

        List<PrivateContestDTO> contests = privateContestService.getHostContests(userDetails.getId());

        List<Map<String, Object>> response = contests.stream()
                .map(c -> toEnrichedMap(c, c.getContestId()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * List private contests the current user has joined as a participant.
     * 
     * Response (200 OK): array of contest objects, each including
     * participantCount and problemCount.
     * 
     * Requirements: 11.3
     */
    @GetMapping("/joined")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<Map<String, Object>>> getJoinedContests(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        log.debug("User {} listing joined private contests", userDetails.getUsername());

        List<Long> contestIds = participantRepository.findByUserId(userDetails.getId())
                .stream()
                .map(p -> p.getContest().getId())
                .collect(Collectors.toList());

        List<Map<String, Object>> response = contestIds.stream()
                .map(cid -> {
                    try {
                        PrivateContestDTO dto = privateContestService.getPrivateContestById(cid);
                        return toEnrichedMap(dto, cid);
                    } catch (Exception e) {
                        log.warn("Skipping joined contest {} — failed to load: {}", cid, e.getMessage());
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Update a private contest's details (name, description, start/end times).
     * Only allowed while the contest is still UPCOMING.
     * 
     * Requirements: 4.2, 4.4
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PrivateContestDTO> updateContest(
            @PathVariable("id") Long contestId,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody UpdateContestRequest request) {

        log.info("User {} updating private contest {}", userDetails.getUsername(), contestId);

        PrivateContestDTO dto = new PrivateContestDTO();
        dto.setName(request.name());
        dto.setDescription(request.description());
        dto.setStartTime(request.startTime());
        dto.setEndTime(request.endTime());

        PrivateContestDTO updated = privateContestService.updateContestDetails(contestId, dto, userDetails.getId());

        return ResponseEntity.ok(updated);
    }

    /**
     * Cancel a private contest before it starts.
     * 
     * Validates that the requesting user is the contest host and that the
     * contest status is UPCOMING. Invalidates all invite tokens and notifies
     * all participants of the cancellation.
     * 
     * Errors:
     * - 403 Forbidden: user is not the contest host
     * - 404 Not Found: contest doesn't exist
     * - 409 Conflict: contest has already started (LIVE or ENDED)
     * 
     * Requirements: 18.1
     */
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PrivateContestDTO> cancelContest(
            @PathVariable("id") Long contestId,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody(required = false) CancelContestRequest request) {

        log.info("User {} cancelling private contest {}", userDetails.getUsername(), contestId);

        String reason = request != null ? request.reason() : null;
        PrivateContestDTO cancelled = privateContestService.cancelPrivateContest(contestId, userDetails.getId(), reason);

        return ResponseEntity.ok(cancelled);
    }

    /**
     * Clone an ended private contest to reuse its problem set and settings.
     * 
     * Creates a new UPCOMING contest with the same name (suffixed " (Copy)"),
     * description, problem list, and proctoring settings as the source contest.
     * Does NOT copy participants, submissions, or the leaderboard. The host
     * must set new start/end times afterward.
     * 
     * Errors:
     * - 403 Forbidden: user is not the host of the source contest
     * - 404 Not Found: source contest doesn't exist
     * - 409 Conflict: source contest is not ENDED
     * 
     * Requirements: 33.1
     */
    @PostMapping("/{id}/clone")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PrivateContestDTO> cloneContest(
            @PathVariable("id") Long contestId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        log.info("User {} cloning private contest {}", userDetails.getUsername(), contestId);

        PrivateContestDTO cloned = privateContestService.cloneContest(contestId, userDetails.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(cloned);
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    /**
     * Flatten a PrivateContestDTO into a Map and fill in participantCount
     * and problemCount, which the DTO/service layer leaves as TODOs.
     */
    private Map<String, Object> toEnrichedMap(PrivateContestDTO dto, Long contestId) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", dto.getId());
        map.put("contestId", dto.getContestId());
        map.put("hostUserId", dto.getHostUserId());
        map.put("hostUsername", dto.getHostUsername());
        map.put("enableProctoring", dto.getEnableProctoring());
        map.put("cancelled", dto.getCancelled());
        map.put("cancelledAt", dto.getCancelledAt());
        map.put("cancellationReason", dto.getCancellationReason());
        map.put("createdAt", dto.getCreatedAt());
        map.put("name", dto.getName());
        map.put("description", dto.getDescription());
        map.put("startTime", dto.getStartTime());
        map.put("endTime", dto.getEndTime());
        map.put("status", dto.getStatus());
        map.put("inviteToken", dto.getInviteToken());
        map.put("inviteLink", dto.getInviteLink());
        map.put("inviteLinkExpiresAt", dto.getInviteLinkExpiresAt());
        map.put("participantCount", participantRepository.countByContestId(contestId));
        map.put("problemCount", contestProblemRepository.countByContestId(contestId));
        return map;
    }

    private ProblemDTO toProblemDTO(Problem p) {
        ProblemDTO dto = new ProblemDTO();
        dto.setId(p.getId());
        dto.setTitle(p.getTitle());
        dto.setDescription(p.getDescription());
        dto.setInputFormat(p.getInputFormat());
        dto.setOutputFormat(p.getOutputFormat());
        dto.setConstraints(p.getConstraints());
        dto.setTimeLimit(p.getTimeLimit());
        dto.setMemoryLimit(p.getMemoryLimit());
        dto.setLevel(p.getLevel());
        dto.setExample1(p.getExample1());
        dto.setExample2(p.getExample2());
        dto.setExample3(p.getExample3());
        return dto;
    }

    // ─── Request DTOs ───────────────────────────────────────────────────────

    public record CreateContestRequest(
            @NotBlank(message = "Contest name is required") String name,
            String description,
            @NotNull(message = "Start time is required") LocalDateTime startTime,
            @NotNull(message = "End time is required") LocalDateTime endTime,
            Boolean enableProctoring
    ) {}

    public record UpdateContestRequest(
            String name,
            String description,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {}

    public record CancelContestRequest(
            String reason
    ) {}
}
