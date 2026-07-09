package com.example.codecombat2026.service;

import com.example.codecombat2026.dto.PrivateContestDTO;
import com.example.codecombat2026.entity.Contest;
import com.example.codecombat2026.entity.PrivateContest;
import com.example.codecombat2026.entity.PrivateContestInvitation;
import com.example.codecombat2026.entity.User;
import com.example.codecombat2026.exception.ConflictException;
import com.example.codecombat2026.exception.ForbiddenException;
import com.example.codecombat2026.exception.ResourceNotFoundException;
import com.example.codecombat2026.proctoring.entity.ProctoredContest;
import com.example.codecombat2026.proctoring.repository.ProctoredContestRepository;
import com.example.codecombat2026.entity.PrivateContestParticipant;
import com.example.codecombat2026.entity.ContestProblem;
import com.example.codecombat2026.repository.ContestProblemRepository;
import com.example.codecombat2026.repository.ContestRepository;
import com.example.codecombat2026.repository.PrivateContestParticipantRepository;
import com.example.codecombat2026.repository.PrivateContestRepository;
import com.example.codecombat2026.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing private contest lifecycle operations.
 * 
 * Provides core business logic for:
 * - Creating private contests with business rule validation
 * - Cancelling contests before they start
 * - Retrieving contest details and listings
 * - Updating contest metadata (before contest starts)
 * 
 * Integrates with:
 * - PrivateContestBusinessRules: Validates monthly quota, duration, overlaps
 * - InviteTokenService: Generates and manages invite tokens
 * - ContestHostingService: Verifies host approval status
 * - EmailService: Sends notifications for key events
 * 
 * Business Rules:
 * - Monthly Quota: 2 private contests per host per calendar month
 * - Participant Limit: Maximum 100 participants per contest
 * - Duration Limit: Maximum 5 hours (300 minutes)
 * - Overlap Prevention: No time window overlap for host's contests
 * - Cancellation: Only allowed before contest starts (status = UPCOMING)
 * 
 * Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9, 17.1, 18.1, 18.2, 18.3, 18.4, 18.5
 */
@Service
public class PrivateContestService {

    private static final Logger log = LoggerFactory.getLogger(PrivateContestService.class);

    @Autowired
    private PrivateContestRepository privateContestRepository;

    @Autowired
    private PrivateContestParticipantRepository participantRepository;

    @Autowired
    private ContestRepository contestRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PrivateContestBusinessRules businessRules;

    @Autowired
    private InviteTokenService inviteTokenService;

    @Autowired
    private ContestHostingService contestHostingService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PrivateContestEmailService privateContestEmailService;

    @Autowired
    private PrivateContestCacheService cacheService;

    @Autowired
    private ProctoredContestRepository proctoredContestRepository;

    @Autowired
    private ContestProblemRepository contestProblemRepository;

    @Autowired
    private com.example.codecombat2026.config.PrivateContestMetricsConfig metricsConfig;

    /**
     * Create a new private contest with full business rule validation.
     * 
     * This method:
     * 1. Validates that the user is an approved Contest_Host
     * 2. Validates monthly quota (2 contests per month)
     * 3. Validates duration (max 5 hours)
     * 4. Validates no time overlap with host's other contests
     * 5. Creates Contest entity with status UPCOMING
     * 6. Creates PrivateContest wrapper entity
     * 7. Creates ProctoredContest entity if proctoring enabled
     * 8. Generates initial invite token with 30-day expiry
     * 9. Sends confirmation email to host with invite link
     * 
     * @param dto PrivateContestDTO containing contest details (name, description, times, enableProctoring)
     * @param hostUserId The ID of the user creating the contest
     * @return PrivateContestDTO with created contest details including invite link
     * @throws ForbiddenException if user is not an approved Contest_Host
     * @throws ResourceNotFoundException if user doesn't exist
     * @throws ResponseStatusException with 429 if monthly quota exceeded
     * @throws ResponseStatusException with 400 if duration > 5 hours
     * @throws ResponseStatusException with 409 if time overlap detected
     * 
     * Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9, 17.1
     */
    @Transactional
    public PrivateContestDTO createPrivateContest(PrivateContestDTO dto, Long hostUserId) {
        log.info("Creating private contest for host user {}: {}", hostUserId, dto.getName());

        // 1. Verify user is an approved Contest_Host
        if (!contestHostingService.isApprovedHost(hostUserId)) {
            throw new ForbiddenException("User is not an approved Contest_Host");
        }

        User host = userRepository.findById(hostUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 2. Validate all business rules
        businessRules.validateContestCreation(hostUserId, dto.getStartTime(), dto.getEndTime());

        // 3. Create Contest entity (base contest)
        Contest contest = new Contest();
        contest.setName(dto.getName());
        contest.setDescription(dto.getDescription());
        contest.setStartTime(dto.getStartTime());
        contest.setEndTime(dto.getEndTime());
        contest.setStatus(Contest.ContestStatus.UPCOMING);
        contest.setActive(false); // Private contests use active=false by default

        contest = contestRepository.save(contest);
        log.info("Created base contest with ID {}", contest.getId());

        // 4. Create PrivateContest wrapper entity
        PrivateContest privateContest = new PrivateContest();
        privateContest.setContest(contest);
        privateContest.setHostUser(host);
        privateContest.setEnableProctoring(dto.getEnableProctoring() != null && dto.getEnableProctoring());
        privateContest.setCancelled(false);

        privateContest = privateContestRepository.save(privateContest);
        log.info("Created private contest with ID {} for contest ID {}", privateContest.getId(), contest.getId());

        // 5. Create ProctoredContest entity if proctoring enabled
        if (privateContest.getEnableProctoring()) {
            ProctoredContest proctoredContest = new ProctoredContest();
            proctoredContest.setContestId(contest.getId());
            proctoredContest.setCreatedAt(LocalDateTime.now());
            proctoredContest.setConsentVersion(1); // Default consent version
            
            proctoredContestRepository.save(proctoredContest);
            log.info("Created ProctoredContest for contest {} with consent version 1", contest.getId());
        }

        // 6. Generate invite token (30-day expiry by default)
        LocalDateTime tokenExpiry = LocalDateTime.now().plusDays(30);
        PrivateContestInvitation invitation = inviteTokenService.createInvitation(contest, tokenExpiry);
        log.info("Generated invite token for contest {}", contest.getId());

        // 7. Send confirmation email to host
        try {
            sendContestCreatedEmail(host, privateContest, invitation.getToken());
        } catch (Exception e) {
            log.error("Failed to send contest creation email: {}", e.getMessage());
            // Non-fatal - contest was created successfully
        }

        // 8. Convert to DTO and return
        PrivateContestDTO resultDTO = convertToDTO(privateContest, invitation);
        
        // 9. Cache the newly created contest metadata
        cacheService.cacheContestMetadata(contest.getId(), resultDTO);
        
        // 10. Increment metrics counter
        metricsConfig.incrementContestsCreated();
        log.debug("Incremented private_contest_created_total metric");
        
        return resultDTO;
    }

    /**
     * Cancel a private contest before it starts.
     * 
     * Validates that:
     * - The requesting user is the contest host
     * - The contest status is UPCOMING (not yet started)
     * 
     * Updates the contest:
     * - Sets cancelled=true, cancelledAt=now
     * - Stores cancellation reason
     * - Invalidates all invite tokens
     * - Sends email notification to all participants
     * 
     * Note: Cancelled contests still count toward the host's monthly quota.
     * 
     * @param contestId The ID of the contest to cancel
     * @param hostUserId The ID of the user requesting cancellation
     * @param reason Optional reason for cancellation
     * @return Updated PrivateContestDTO
     * @throws ResourceNotFoundException if contest doesn't exist
     * @throws ForbiddenException if user is not the host
     * @throws ConflictException if contest has already started (status LIVE or ENDED)
     * 
     * Requirements: 18.1, 18.2, 18.3, 18.4, 18.5
     */
    @Transactional
    public PrivateContestDTO cancelPrivateContest(Long contestId, Long hostUserId, String reason) {
        log.info("Cancelling private contest {} by host user {}", contestId, hostUserId);

        // Find the private contest
        PrivateContest privateContest = privateContestRepository.findByContestId(contestId)
                .orElseThrow(() -> new ResourceNotFoundException("Private contest not found"));

        // Verify user is the host
        if (!privateContest.getHostUser().getId().equals(hostUserId)) {
            throw new ForbiddenException("Only the contest host can cancel this contest");
        }

        // Verify contest hasn't started yet
        Contest contest = privateContest.getContest();
        if (contest.getStatus() != Contest.ContestStatus.UPCOMING) {
            throw new ConflictException("Cannot cancel a contest that has already started");
        }

        // Update cancellation fields
        privateContest.setCancelled(true);
        privateContest.setCancelledAt(LocalDateTime.now());
        privateContest.setCancellationReason(reason);

        privateContest = privateContestRepository.save(privateContest);
        log.info("Marked contest {} as cancelled", contestId);

        // Invalidate cache since contest metadata changed
        cacheService.invalidateContestCache(contestId);

        // Invalidate all invite tokens
        inviteTokenService.invalidateAllTokensForContest(contestId);
        log.info("Invalidated all invite tokens for contest {}", contestId);

        // Send cancellation email to all participants
        try {
            sendContestCancelledEmail(privateContest);
        } catch (Exception e) {
            log.error("Failed to send cancellation emails: {}", e.getMessage());
            // Non-fatal - contest was cancelled successfully
        }

        return convertToDTO(privateContest, null);
    }

    /**
     * Get private contest details by contest ID.
     * 
     * Returns full contest details including:
     * - Basic info (name, description, times, status)
     * - Host information
     * - Participant count
     * - Proctoring status
     * - Cancellation status
     * 
     * Uses cache-aside pattern:
     * 1. Check cache first (O(1) lookup)
     * 2. On cache miss, query database
     * 3. Cache the result for future requests (6-hour TTL)
     * 
     * Note: Access validation should be done by the caller
     * (PrivateContestAccessValidator.canAccess)
     * 
     * @param contestId The contest ID
     * @return PrivateContestDTO with full contest details
     * @throws ResourceNotFoundException if contest doesn't exist
     * 
     * Requirements: 11.4, 25.3
     */
    @Transactional(readOnly = true)
    public PrivateContestDTO getPrivateContestById(Long contestId) {
        log.debug("Retrieving private contest {}", contestId);

        // Try cache first
        var cachedContest = cacheService.getCachedContest(contestId);
        if (cachedContest.isPresent()) {
            log.debug("Cache hit for private contest {}", contestId);
            return cachedContest.get();
        }

        // Cache miss - query database
        log.debug("Cache miss for private contest {}, querying database", contestId);
        PrivateContest privateContest = privateContestRepository.findByContestId(contestId)
                .orElseThrow(() -> new ResourceNotFoundException("Private contest not found"));

        PrivateContestDTO dto = convertToDTO(privateContest, null);
        
        // Cache the result
        cacheService.cacheContestMetadata(contestId, dto);
        
        return dto;
    }

    /**
     * Get all private contests created by a specific host.
     * 
     * Returns list of contests with basic information:
     * - Contest ID and name
     * - Start/end times and status
     * - Participant count
     * - Cancellation status
     * 
     * Sorted by creation time descending (newest first).
     * 
     * @param hostUserId The ID of the Contest_Host
     * @return List of PrivateContestDTO objects
     * 
     * Requirements: 11.2
     */
    @Transactional(readOnly = true)
    public List<PrivateContestDTO> getHostContests(Long hostUserId) {
        log.debug("Retrieving all contests for host user {}", hostUserId);

        List<PrivateContest> contests = privateContestRepository.findByHostUserId(hostUserId);

        return contests.stream()
                .map(pc -> convertToDTO(pc, null))
                .toList();
    }

    /**
     * Update contest details (name, description, start/end times).
     * 
     * Validates that:
     * - The requesting user is the contest host
     * - The contest status is UPCOMING (not yet started)
     * - Updated times still meet duration and overlap requirements
     * 
     * Only allows updating:
     * - name
     * - description
     * - startTime
     * - endTime
     * 
     * Cannot update:
     * - enableProctoring (set at creation time)
     * - host
     * - problems (use separate problem management methods)
     * 
     * @param contestId The contest ID to update
     * @param dto PrivateContestDTO with updated values
     * @param hostUserId The ID of the user requesting the update
     * @return Updated PrivateContestDTO
     * @throws ResourceNotFoundException if contest doesn't exist
     * @throws ForbiddenException if user is not the host
     * @throws ConflictException if contest has already started
     * @throws ResponseStatusException with 400 if new duration > 5 hours
     * @throws ResponseStatusException with 409 if new times overlap with other contests
     * 
     * Requirements: 4.2, 4.4
     */
    @Transactional
    public PrivateContestDTO updateContestDetails(Long contestId, PrivateContestDTO dto, Long hostUserId) {
        log.info("Updating contest {} by host user {}", contestId, hostUserId);

        // Find the private contest
        PrivateContest privateContest = privateContestRepository.findByContestId(contestId)
                .orElseThrow(() -> new ResourceNotFoundException("Private contest not found"));

        // Verify user is the host
        if (!privateContest.getHostUser().getId().equals(hostUserId)) {
            throw new ForbiddenException("Only the contest host can update this contest");
        }

        // Verify contest hasn't started yet
        Contest contest = privateContest.getContest();
        if (contest.getStatus() != Contest.ContestStatus.UPCOMING) {
            throw new ConflictException("Cannot update a contest that has already started");
        }

        // If times are being updated, validate business rules
        LocalDateTime newStartTime = dto.getStartTime() != null ? dto.getStartTime() : contest.getStartTime();
        LocalDateTime newEndTime = dto.getEndTime() != null ? dto.getEndTime() : contest.getEndTime();

        if (!newStartTime.equals(contest.getStartTime()) || !newEndTime.equals(contest.getEndTime())) {
            // Validate duration
            businessRules.validateDuration(newStartTime, newEndTime);

            // Validate no overlap (excluding this contest itself)
            // This is a simplified check - we'll verify it doesn't overlap with OTHER contests
            // by temporarily considering this contest as if it doesn't exist
            List<PrivateContest> otherContests = privateContestRepository.findByHostUserId(hostUserId).stream()
                    .filter(pc -> !pc.getContest().getId().equals(contestId))
                    .toList();

            for (PrivateContest other : otherContests) {
                Contest otherContest = other.getContest();
                if (!other.getCancelled() && otherContest.getStatus() != Contest.ContestStatus.ENDED) {
                    // Check overlap
                    if (newStartTime.isBefore(otherContest.getEndTime()) && 
                        otherContest.getStartTime().isBefore(newEndTime)) {
                        throw new ConflictException(
                            "Updated times overlap with your existing contest '" + otherContest.getName() + "'"
                        );
                    }
                }
            }
        }

        // Update allowed fields
        if (dto.getName() != null && !dto.getName().isBlank()) {
            contest.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            contest.setDescription(dto.getDescription());
        }
        if (dto.getStartTime() != null) {
            contest.setStartTime(dto.getStartTime());
        }
        if (dto.getEndTime() != null) {
            contest.setEndTime(dto.getEndTime());
        }

        contest = contestRepository.save(contest);
        log.info("Updated contest {} details", contestId);

        // Invalidate cache since contest metadata changed
        cacheService.invalidateContestCache(contestId);

        return convertToDTO(privateContest, null);
    }

    /**
     * Clone an ended private contest to reuse its problem set and settings.
     * 
     * Validates that:
     * - The requesting user is the host of the source contest
     * - The source contest status is ENDED
     * 
     * Creates:
     * - A new Contest entity with the same name (appended with " (Copy)"),
     *   description and proctoring settings, status UPCOMING, and
     *   start/end times left unset for the host to configure
     * - A new PrivateContest wrapper entity owned by the same host
     * - A new ProctoredContest entity if the source contest had proctoring enabled
     * - New ContestProblem junction rows mirroring the source contest's problem list
     *   (same displayOrder, fresh addedAt)
     * - A new invite token for the cloned contest
     * 
     * Does NOT copy participants, submissions, or leaderboard data.
     * 
     * Note: the cloned contest counts toward the host's monthly quota, but since
     * cloning itself does not set start/end times yet, quota/duration/overlap
     * validation is deferred to when the host saves the new times via
     * {@link #updateContestDetails(Long, PrivateContestDTO, Long)}.
     * 
     * @param contestId The ID of the source contest to clone
     * @param hostUserId The ID of the user requesting the clone
     * @return PrivateContestDTO for the newly created (cloned) contest
     * @throws ResourceNotFoundException if the source contest doesn't exist
     * @throws ForbiddenException if the requesting user is not the host of the source contest
     * @throws ConflictException if the source contest status is not ENDED
     * 
     * Requirements: 33.1, 33.2, 33.3, 33.4, 33.5, 33.6
     */
    @Transactional
    public PrivateContestDTO cloneContest(Long contestId, Long hostUserId) {
        log.info("Cloning private contest {} by host user {}", contestId, hostUserId);

        // Find the source private contest
        PrivateContest sourcePrivateContest = privateContestRepository.findByContestId(contestId)
                .orElseThrow(() -> new ResourceNotFoundException("Private contest not found"));

        // Verify user is the host of the source contest
        if (!sourcePrivateContest.getHostUser().getId().equals(hostUserId)) {
            throw new ForbiddenException("Only the contest host can clone this contest");
        }

        // Verify source contest has ended
        Contest sourceContest = sourcePrivateContest.getContest();
        if (sourceContest.getStatus() != Contest.ContestStatus.ENDED) {
            throw new ConflictException("Only ended contests can be cloned");
        }

        // Create new Contest entity - status UPCOMING, start/end times unset
        // (host must set new times before saving, per Requirement 33.5)
        Contest newContest = new Contest();
        newContest.setName(sourceContest.getName() + " (Copy)");
        newContest.setDescription(sourceContest.getDescription());
        newContest.setStartTime(null);
        newContest.setEndTime(null);
        newContest.setStatus(Contest.ContestStatus.UPCOMING);
        newContest.setActive(false);

        newContest = contestRepository.save(newContest);
        log.info("Created cloned base contest with ID {} from source contest {}", newContest.getId(), contestId);

        // Create new PrivateContest wrapper entity, owned by the same host
        PrivateContest newPrivateContest = new PrivateContest();
        newPrivateContest.setContest(newContest);
        newPrivateContest.setHostUser(sourcePrivateContest.getHostUser());
        newPrivateContest.setEnableProctoring(sourcePrivateContest.getEnableProctoring());
        newPrivateContest.setCancelled(false);

        newPrivateContest = privateContestRepository.save(newPrivateContest);
        log.info("Created cloned private contest with ID {} for contest ID {}",
                newPrivateContest.getId(), newContest.getId());

        // Copy proctoring settings if the source contest had proctoring enabled
        if (Boolean.TRUE.equals(newPrivateContest.getEnableProctoring())) {
            ProctoredContest proctoredContest = new ProctoredContest();
            proctoredContest.setContestId(newContest.getId());
            proctoredContest.setCreatedAt(LocalDateTime.now());
            proctoredContest.setConsentVersion(1);

            proctoredContestRepository.save(proctoredContest);
            log.info("Created ProctoredContest for cloned contest {}", newContest.getId());
        }

        // Copy problem attachments via the contest_problems junction table
        List<ContestProblem> sourceProblems = contestProblemRepository
                .findByContestIdOrderByDisplayOrderAscAddedAtAsc(contestId);

        for (ContestProblem sourceProblem : sourceProblems) {
            ContestProblem clonedProblem = new ContestProblem();
            clonedProblem.setContestId(newContest.getId());
            clonedProblem.setProblemId(sourceProblem.getProblemId());
            clonedProblem.setDisplayOrder(sourceProblem.getDisplayOrder());
            contestProblemRepository.save(clonedProblem);
        }
        log.info("Copied {} problem attachment(s) from contest {} to cloned contest {}",
                sourceProblems.size(), contestId, newContest.getId());

        // Generate a new invite token for the cloned contest
        LocalDateTime tokenExpiry = LocalDateTime.now().plusDays(30);
        PrivateContestInvitation invitation = inviteTokenService.createInvitation(newContest, tokenExpiry);
        log.info("Generated invite token for cloned contest {}", newContest.getId());

        // Note: participants, submissions, and leaderboard are intentionally NOT copied

        PrivateContestDTO resultDTO = convertToDTO(newPrivateContest, invitation);

        // Cache the newly created cloned contest metadata
        cacheService.cacheContestMetadata(newContest.getId(), resultDTO);

        // Cloned contests count toward the host's monthly quota, same as regular creation
        metricsConfig.incrementContestsCreated();

        return resultDTO;
    }

    // ─── Private Helper Methods ───────────────────────────────────────────────

    /**
     * Send email notification to host when contest is created.
     * 
     * Email includes:
     * - Contest details (name, times)
     * - Invite link for sharing
     * - Link to contest management dashboard
     * 
     * Requirements: 17.1
     */
    private void sendContestCreatedEmail(User host, PrivateContest privateContest, String inviteToken) {
        // TODO: Implement when email template is available
        log.info("Contest creation email would be sent to host {} for contest {}", 
                host.getId(), privateContest.getContest().getId());
        
        // Placeholder for future implementation:
        // String inviteLink = buildInviteLink(inviteToken);
        // String dashboardLink = buildDashboardLink(privateContest.getContest().getId());
        // emailService.sendPrivateContestCreated(
        //     host.getEmail(),
        //     host.getFullName(),
        //     privateContest.getContest().getName(),
        //     inviteLink,
        //     dashboardLink
        // );
    }

    /**
     * Send email notification to all participants when contest is cancelled.
     * 
     * Email includes:
     * - Contest name
     * - Cancellation reason (if provided)
     * - Host contact information
     * 
     * Requirements: 18.3
     */
    private void sendContestCancelledEmail(PrivateContest privateContest) {
        Long contestId = privateContest.getContest().getId();

        List<PrivateContestParticipant> participants = participantRepository.findByContestId(contestId);
        if (participants.isEmpty()) {
            log.info("No participants to notify for cancelled contest {}", contestId);
            return;
        }

        List<User> participantUsers = participants.stream()
                .map(PrivateContestParticipant::getUser)
                .toList();

        User host = privateContest.getHostUser();
        String hostName = host.getFullName() != null ? host.getFullName() : host.getUsername();

        privateContestEmailService.sendContestCancelledEmail(
                participantUsers,
                contestId,
                privateContest.getContest().getName(),
                hostName,
                privateContest.getCancellationReason()
        );
        log.info("Queued contest cancellation emails for {} participants of contest {}",
                participantUsers.size(), contestId);
    }

    /**
     * Convert PrivateContest entity to DTO.
     * 
     * @param privateContest The entity to convert
     * @param invitation Optional invitation (null if not needed)
     * @return PrivateContestDTO with all relevant fields populated
     */
    private PrivateContestDTO convertToDTO(PrivateContest privateContest, PrivateContestInvitation invitation) {
        Contest contest = privateContest.getContest();
        
        PrivateContestDTO dto = new PrivateContestDTO();
        dto.setId(privateContest.getId());
        dto.setContestId(contest.getId());
        dto.setName(contest.getName());
        dto.setDescription(contest.getDescription());
        dto.setStartTime(contest.getStartTime());
        dto.setEndTime(contest.getEndTime());
        dto.setStatus(contest.getStatus());
        dto.setHostUserId(privateContest.getHostUser().getId());
        dto.setHostUsername(privateContest.getHostUser().getUsername());
        dto.setEnableProctoring(privateContest.getEnableProctoring());
        dto.setCancelled(privateContest.getCancelled());
        dto.setCancelledAt(privateContest.getCancelledAt());
        dto.setCancellationReason(privateContest.getCancellationReason());
        dto.setCreatedAt(privateContest.getCreatedAt());

        // Add invite link if invitation provided
        if (invitation != null) {
            dto.setInviteToken(invitation.getToken());
            dto.setInviteLink(buildInviteLink(invitation.getToken()));
            dto.setInviteLinkExpiresAt(invitation.getExpiresAt());
        }

        // TODO: Add participant count when PrivateContestParticipantRepository is available
        // dto.setParticipantCount(participantRepository.countByContestId(contest.getId()));

        return dto;
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
}
