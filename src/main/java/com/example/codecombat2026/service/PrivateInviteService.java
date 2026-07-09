package com.example.codecombat2026.service;

import com.example.codecombat2026.entity.Contest;
import com.example.codecombat2026.entity.PrivateContest;
import com.example.codecombat2026.entity.PrivateContestInvitation;
import com.example.codecombat2026.entity.PrivateContestParticipant;
import com.example.codecombat2026.entity.User;
import com.example.codecombat2026.exception.BadRequestException;
import com.example.codecombat2026.exception.ConflictException;
import com.example.codecombat2026.exception.ForbiddenException;
import com.example.codecombat2026.exception.ResourceNotFoundException;
import com.example.codecombat2026.repository.PrivateContestInvitationRepository;
import com.example.codecombat2026.repository.PrivateContestParticipantRepository;
import com.example.codecombat2026.repository.PrivateContestRepository;
import com.example.codecombat2026.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service for managing private contest invitations and participants.
 * 
 * Provides functionality for:
 * - Accepting invitations and joining private contests
 * - Removing participants from private contests
 * - Validating access control (host-only operations)
 * - Enforcing business rules (no removal during LIVE contests)
 * 
 * Requirements: 6.3, 6.4, 6.5, 6.6, 6.7, 6.8, 7.4, 7.5, 17.2
 */
@Service
public class PrivateInviteService {

    private static final Logger log = LoggerFactory.getLogger(PrivateInviteService.class);

    private static final Duration DEFAULT_TOKEN_VALIDITY = Duration.ofDays(30);

    @Autowired
    private InviteTokenService inviteTokenService;

    @Autowired
    private PrivateContestBusinessRules businessRules;

    @Autowired
    private PrivateContestParticipantRepository participantRepository;

    @Autowired
    private PrivateContestRepository privateContestRepository;

    @Autowired
    private PrivateContestInvitationRepository invitationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private com.example.codecombat2026.config.PrivateContestMetricsConfig metricsConfig;

    /**
     * Accept an invitation and join a private contest.
     * 
     * Validates the invite token, checks business rules (participant limit),
     * creates a participant record, and sends a notification to the host
     * when the first participant joins.
     * 
     * @param userId The ID of the user accepting the invitation
     * @param token The invitation token
     * @return The created PrivateContestParticipant entity
     * @throws ResponseStatusException if validation fails:
     *   - 404 Not Found: Invalid or expired token
     *   - 429 Too Many Requests: Contest has reached participant limit
     *   - 409 Conflict: User has already joined the contest
     *   - 404 Not Found: User not found
     * 
     * Requirements: 6.3, 6.4, 6.5, 6.6, 6.7, 6.8, 17.2
     */
    @Transactional
    public PrivateContestParticipant acceptInvite(Long userId, String token) {
        log.info("User {} attempting to accept invite with token {}", userId, 
                 token != null && token.length() > 10 ? token.substring(0, 10) + "..." : token);

        // Validate token using InviteTokenService
        Optional<PrivateContestInvitation> invitationOpt = inviteTokenService.validateToken(token);
        
        if (invitationOpt.isEmpty()) {
            log.warn("Invalid or expired token for user {}", userId);
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "This invitation link is invalid or has expired"
            );
        }

        PrivateContestInvitation invitation = invitationOpt.get();
        Contest contest = invitation.getContest();
        Long contestId = contest.getId();

        log.debug("Token validated successfully for contest ID {}", contestId);

        // Verify user exists
        User user = userRepository.findById(userId)
            .orElseThrow(() -> {
                log.error("User {} not found", userId);
                return new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
            });

        // Check if user already joined (handle duplicate join attempt)
        if (participantRepository.existsByContestIdAndUserId(contestId, userId)) {
            log.warn("User {} has already joined contest {}", userId, contestId);
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "You have already joined this contest"
            );
        }

        // Validate participant limit using businessRules
        businessRules.validateParticipantLimit(contestId);

        // Check if this is the first participant before creating the record
        long currentParticipantCount = participantRepository.countByContestId(contestId);
        boolean isFirstParticipant = (currentParticipantCount == 0);

        // Create PrivateContestParticipant entity
        PrivateContestParticipant participant = new PrivateContestParticipant();
        participant.setContest(contest);
        participant.setUser(user);
        participant.setJoinedAt(LocalDateTime.now());

        try {
            participant = participantRepository.save(participant);
            log.info("User {} successfully joined contest {} as participant", userId, contestId);
        } catch (DataIntegrityViolationException e) {
            // Handle race condition where unique constraint was violated
            // between our check and the save operation
            log.warn("Duplicate join attempt caught by unique constraint for user {} and contest {}", 
                     userId, contestId);
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "You have already joined this contest"
            );
        }

        // Send email to host on first participant join
        if (isFirstParticipant) {
            sendFirstParticipantNotification(contest, user);
        }

        // Increment metrics counter
        metricsConfig.incrementInvitationsAccepted();
        log.debug("Incremented private_contest_invitation_accepted_total metric");

        return participant;
    }

    /**
     * Send an email notification to the contest host when the first participant joins.
     * 
     * This is a non-blocking operation - failures are logged but do not prevent
     * the participant from joining the contest.
     * 
     * @param contest The contest that was joined
     * @param firstParticipant The first user to join
     * 
     * Requirement: 17.2
     */
    private void sendFirstParticipantNotification(Contest contest, User firstParticipant) {
        try {
            // Find the private contest to get the host
            Optional<PrivateContest> privateContestOpt = privateContestRepository.findByContestId(contest.getId());
            
            if (privateContestOpt.isEmpty()) {
                log.error("PrivateContest not found for contest ID {} when sending first participant notification", 
                          contest.getId());
                return;
            }

            PrivateContest privateContest = privateContestOpt.get();
            User host = privateContest.getHostUser();

            log.info("Sending first participant notification to host {} for contest {}", 
                     host.getId(), contest.getId());

            // Send email notification
            emailService.sendFirstParticipantJoinedEmail(
                host.getEmail(),
                host.getFullName() != null ? host.getFullName() : host.getUsername(),
                contest.getName(),
                contest.getId(),
                firstParticipant.getUsername(),
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z").format(new java.util.Date())
            );

            log.info("First participant notification sent successfully to host {}", host.getEmail());
        } catch (Exception e) {
            // Email failures should not prevent participant from joining
            log.error("Failed to send first participant notification for contest {}: {}", 
                      contest.getId(), e.getMessage());
        }
    }

    /**
     * Remove a participant from a private contest.
     * 
     * Business Rules:
     * - Only the contest host can remove participants (Requirement 7.4)
     * - Removal is not allowed once the contest status is LIVE (Requirement 7.5)
     * - Removing the participant deletes the private_contest_participants row,
     *   preventing the user from accessing the contest
     * 
     * @param contestId The ID of the contest
     * @param userId The ID of the user to remove
     * @param hostUserId The ID of the user attempting the removal (must be the host)
     * @throws ForbiddenException if the requesting user is not the contest host
     * @throws ConflictException if the contest has already started (status is LIVE)
     * @throws ResourceNotFoundException if the contest or participant is not found
     */
    @Transactional
    public void removeParticipant(Long contestId, Long userId, Long hostUserId) {
        log.info("Attempting to remove participant {} from contest {} by host {}", 
                userId, contestId, hostUserId);

        // 1. Find the private contest
        Optional<PrivateContest> privateContestOpt = privateContestRepository.findByContestId(contestId);
        if (privateContestOpt.isEmpty()) {
            log.warn("Private contest not found for contest ID {}", contestId);
            throw new ResourceNotFoundException("Private contest not found");
        }

        PrivateContest privateContest = privateContestOpt.get();
        Contest contest = privateContest.getContest();

        // 2. Validate user is the host
        if (!privateContest.getHostUser().getId().equals(hostUserId)) {
            log.warn("User {} attempted to remove participant from contest {} but is not the host", 
                    hostUserId, contestId);
            throw new ForbiddenException("Only the contest host can remove participants");
        }

        // 3. Validate contest status is not LIVE
        if (contest.getStatus() == Contest.ContestStatus.LIVE) {
            log.warn("Attempted to remove participant from LIVE contest {}", contestId);
            throw new ConflictException("Cannot remove participants from a contest that has already started");
        }

        // 4. Find and delete the participant
        Optional<PrivateContestParticipant> participantOpt = 
                participantRepository.findByContestIdAndUserId(contestId, userId);
        
        if (participantOpt.isEmpty()) {
            log.warn("Participant {} not found in contest {}", userId, contestId);
            throw new ResourceNotFoundException("Participant not found in this contest");
        }

        participantRepository.delete(participantOpt.get());
        
        log.info("Successfully removed participant {} from contest {}", userId, contestId);
    }

    /**
     * Regenerate the invite token for a private contest.
     * 
     * Marks the currently active invite token (if any) as invalidated and
     * generates a brand new token with a fresh expiry of 30 days from now.
     * Only the contest host is permitted to regenerate the invite token.
     * 
     * @param contestId The ID of the contest
     * @param hostUserId The ID of the user requesting the regeneration (must be the host)
     * @return The newly created PrivateContestInvitation
     * @throws ResourceNotFoundException if the private contest is not found
     * @throws ForbiddenException if the requesting user is not the contest host
     * 
     * Requirements: 5.2, 5.3
     */
    @Transactional
    public PrivateContestInvitation regenerateInviteToken(Long contestId, Long hostUserId) {
        log.info("Host {} regenerating invite token for contest {}", hostUserId, contestId);

        PrivateContest privateContest = getHostOwnedPrivateContest(contestId, hostUserId);
        Contest contest = privateContest.getContest();

        LocalDateTime newExpiry = LocalDateTime.now().plus(DEFAULT_TOKEN_VALIDITY);

        Optional<PrivateContestInvitation> currentInvitation =
                invitationRepository.findFirstByContestIdAndInvalidatedFalseOrderByCreatedAtDesc(contestId);

        PrivateContestInvitation newInvitation;
        if (currentInvitation.isPresent()) {
            // Invalidates the old token and creates a new one with a fresh expiry
            newInvitation = inviteTokenService.regenerateToken(
                    currentInvitation.get().getToken(), contest, newExpiry);
        } else {
            // No active token exists yet, simply create a new one
            newInvitation = inviteTokenService.createInvitation(contest, newExpiry);
        }

        log.info("Regenerated invite token for contest {} with new expiry {}", contestId, newExpiry);
        return newInvitation;
    }

    /**
     * Update the expiry timestamp of the active invite token for a private contest.
     * 
     * The new expiry must be no earlier than the current time and no later than
     * the contest's end time. Only the contest host is permitted to update the
     * token expiry.
     * 
     * @param contestId The ID of the contest
     * @param hostUserId The ID of the user requesting the update (must be the host)
     * @param expiresAt The desired new expiry timestamp
     * @return The updated (or newly created, if none existed) PrivateContestInvitation
     * @throws ResourceNotFoundException if the private contest is not found
     * @throws ForbiddenException if the requesting user is not the contest host
     * @throws BadRequestException if expiresAt is missing or out of the valid range
     * 
     * Requirements: 5.4
     */
    @Transactional
    public PrivateContestInvitation updateTokenExpiry(Long contestId, Long hostUserId, LocalDateTime expiresAt) {
        log.info("Host {} updating invite token expiry for contest {}", hostUserId, contestId);

        PrivateContest privateContest = getHostOwnedPrivateContest(contestId, hostUserId);
        Contest contest = privateContest.getContest();

        if (expiresAt == null) {
            throw new BadRequestException("Expiry time is required");
        }

        LocalDateTime now = LocalDateTime.now();
        if (expiresAt.isBefore(now)) {
            throw new BadRequestException("Expiry time must be in the future");
        }

        LocalDateTime contestEndTime = contest.getEndTime();
        if (contestEndTime != null && expiresAt.isAfter(contestEndTime)) {
            throw new BadRequestException("Expiry time cannot be after contest end time");
        }

        Optional<PrivateContestInvitation> currentInvitation =
                invitationRepository.findFirstByContestIdAndInvalidatedFalseOrderByCreatedAtDesc(contestId);

        PrivateContestInvitation updatedInvitation;
        if (currentInvitation.isPresent()) {
            updatedInvitation = inviteTokenService.updateExpiry(currentInvitation.get().getToken(), expiresAt);
        } else {
            // No active token exists yet, create one with the requested expiry
            updatedInvitation = inviteTokenService.createInvitation(contest, expiresAt);
        }

        log.info("Updated invite token expiry to {} for contest {}", expiresAt, contestId);
        return updatedInvitation;
    }

    /**
     * Look up the PrivateContest for a given contest ID and verify that the
     * requesting user is the contest host.
     * 
     * @param contestId The ID of the contest
     * @param hostUserId The ID of the user to verify as host
     * @return The PrivateContest entity if verification succeeds
     * @throws ResourceNotFoundException if the private contest is not found
     * @throws ForbiddenException if the requesting user is not the contest host
     */
    private PrivateContest getHostOwnedPrivateContest(Long contestId, Long hostUserId) {
        PrivateContest privateContest = privateContestRepository.findByContestId(contestId)
                .orElseThrow(() -> new ResourceNotFoundException("Private contest not found"));

        if (!privateContest.getHostUser().getId().equals(hostUserId)) {
            throw new ForbiddenException("Only the contest host can perform this action");
        }

        return privateContest;
    }
}
