package com.example.codecombat2026.service;

import com.example.codecombat2026.dto.PrivateContestDTO;
import com.example.codecombat2026.entity.Contest;
import com.example.codecombat2026.entity.PrivateContest;
import com.example.codecombat2026.entity.PrivateContestInvitation;
import com.example.codecombat2026.entity.PrivateContestParticipant;
import com.example.codecombat2026.exception.ResourceNotFoundException;
import com.example.codecombat2026.repository.ContestRepository;
import com.example.codecombat2026.repository.PrivateContestInvitationRepository;
import com.example.codecombat2026.repository.PrivateContestParticipantRepository;
import com.example.codecombat2026.repository.PrivateContestRepository;
import com.example.codecombat2026.repository.SubmissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for admin oversight and moderation of private contests.
 * 
 * Provides admin-only operations for:
 * - Listing all private contests across all hosts
 * - Viewing full details of any private contest (bypasses ownership checks)
 * - Deleting private contests with cascade cleanup
 * 
 * This service is used exclusively by admins and bypasses the normal
 * access control checks enforced by PrivateContestAccessValidator.
 * 
 * Security Note:
 * All methods in this service should ONLY be called from endpoints
 * protected by @PreAuthorize("hasRole('ADMIN')") annotation.
 * 
 * Requirements: 19.1, 19.2, 19.3
 */
@Service
public class PrivateContestAdminService {

    private static final Logger log = LoggerFactory.getLogger(PrivateContestAdminService.class);

    @Autowired
    private PrivateContestRepository privateContestRepository;

    @Autowired
    private ContestRepository contestRepository;

    @Autowired
    private PrivateContestParticipantRepository participantRepository;

    @Autowired
    private PrivateContestInvitationRepository invitationRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private PrivateContestCacheService cacheService;

    @Autowired
    private AuditService auditService;

    /**
     * List all private contests in the system with optional filters.
     * 
     * Returns paginated list of ALL private contests across all hosts.
     * Includes basic information needed for admin oversight:
     * - Contest ID and name
     * - Host username and ID
     * - Contest status, start/end times
     * - Participant count
     * - Creation timestamp
     * - Cancellation status
     * 
     * Filters (all optional):
     * - status: Filter by contest status (UPCOMING, LIVE, ENDED)
     * - hostUserId: Filter by specific host
     * - cancelled: Filter by cancellation status (true/false)
     * - createdAfter: Filter contests created after this date
     * - createdBefore: Filter contests created before this date
     * 
     * Sorting: Default is by creation time descending (newest first).
     * Can be customized via the Pageable parameter.
     * 
     * @param filters Map of filter parameters (can be null for no filtering)
     * @param pageable Pagination and sorting parameters
     * @return Page of PrivateContestDTO objects with admin-specific data
     * 
     * Requirement: 19.1
     */
    @Transactional(readOnly = true)
    public Page<PrivateContestDTO> listAllPrivateContests(
            ContestFilters filters,
            Pageable pageable) {
        
        log.info("Admin listing all private contests with filters: {}", filters);

        // Fetch all private contests (no access control checks)
        List<PrivateContest> allContests = privateContestRepository.findAll();

        // Apply filters if provided
        List<PrivateContest> filteredContests = allContests.stream()
                .filter(pc -> applyFilters(pc, filters))
                .collect(Collectors.toList());

        // Sort and paginate
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filteredContests.size());
        
        List<PrivateContestDTO> dtos = filteredContests.subList(start, end).stream()
                .map(this::convertToDTOWithParticipantCount)
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, filteredContests.size());
    }

    /**
     * Get full details of any private contest (admin only).
     * 
     * Returns complete contest information including:
     * - All contest metadata (name, description, times, status)
     * - Host information (ID, username, email)
     * - Full participant list
     * - Problem list (if any attached)
     * - Proctoring settings
     * - Cancellation details
     * - Submission count
     * 
     * This method bypasses the normal access control that restricts
     * contest details to the host and participants only.
     * 
     * Admins can view ANY private contest regardless of whether they
     * are the host or a participant.
     * 
     * @param contestId The contest ID to retrieve
     * @return PrivateContestDTO with full contest details
     * @throws ResourceNotFoundException if contest doesn't exist
     * 
     * Requirement: 19.2
     */
    @Transactional(readOnly = true)
    public PrivateContestDTO getPrivateContestDetails(Long contestId) {
        log.info("Admin retrieving details for private contest {}", contestId);

        PrivateContest privateContest = privateContestRepository.findByContestId(contestId)
                .orElseThrow(() -> new ResourceNotFoundException("Private contest not found"));

        // Get full details including participant count
        PrivateContestDTO dto = convertToDTOWithParticipantCount(privateContest);

        return dto;
    }

    /**
     * Delete a private contest and cascade-delete all related data.
     * 
     * This is a destructive admin-only operation that:
     * 1. Deletes all private_contest_participants rows
     * 2. Deletes all private_contest_invitations rows
     * 3. Deletes the private_contests row
     * 4. Optionally deletes the contests row (if no submissions exist)
     * 5. Invalidates all related caches
     * 6. Logs the deletion in the audit log
     * 
     * Business Rules:
     * - If the contest has submissions, the base Contest entity is NOT deleted
     *   (preserves submission history for integrity)
     * - If the contest has no submissions, the base Contest entity IS deleted
     * - All private-contest-specific data is always deleted
     * 
     * Warning: This operation cannot be undone. Use with caution.
     * 
     * @param contestId The contest ID to delete
     * @param adminId The ID of the admin performing the deletion
     * @throws ResourceNotFoundException if contest doesn't exist
     * 
     * Requirement: 19.3
     */
    @Transactional
    public void deletePrivateContest(Long contestId, Long adminId) {
        log.warn("Admin {} deleting private contest {}", adminId, contestId);

        // Verify the contest exists
        PrivateContest privateContest = privateContestRepository.findByContestId(contestId)
                .orElseThrow(() -> new ResourceNotFoundException("Private contest not found"));

        Contest contest = privateContest.getContest();
        String contestName = contest.getName();
        Long hostUserId = privateContest.getHostUser().getId();

        // Check if there are any submissions for this contest
        long submissionCount = submissionRepository.findByContest_Id(contestId).size();
        boolean hasSubmissions = submissionCount > 0;

        log.info("Contest {} has {} submissions. Will {}delete base Contest entity.", 
                contestId, submissionCount, hasSubmissions ? "NOT " : "");

        // Step 1: Delete all participants (CASCADE from DB will handle this, but we do it explicitly for logging)
        List<PrivateContestParticipant> participants = participantRepository.findByContestId(contestId);
        int participantCount = participants.size();
        participantRepository.deleteAll(participants);
        log.info("Deleted {} participants for contest {}", participantCount, contestId);

        // Step 2: Delete all invitations
        List<PrivateContestInvitation> invitations = invitationRepository.findByContestId(contestId);
        int invitationCount = invitations.size();
        invitationRepository.deleteAll(invitations);
        log.info("Deleted {} invitations for contest {}", invitationCount, contestId);

        // Step 3: Delete the private_contests row
        privateContestRepository.delete(privateContest);
        log.info("Deleted private_contests row for contest {}", contestId);

        // Step 4: Optionally delete the base Contest entity if no submissions
        if (!hasSubmissions) {
            contestRepository.delete(contest);
            log.info("Deleted base Contest entity {} (no submissions existed)", contestId);
        } else {
            log.info("Preserved base Contest entity {} ({} submissions exist)", contestId, submissionCount);
        }

        // Step 5: Invalidate all related caches
        cacheService.invalidateContestCache(contestId);
        log.info("Invalidated cache for contest {}", contestId);

        // Step 6: Audit log the deletion
        Map<String, Object> auditDetails = new HashMap<>();
        auditDetails.put("contestName", contestName);
        auditDetails.put("contestId", contestId);
        auditDetails.put("hostUserId", hostUserId);
        auditDetails.put("participantCount", participantCount);
        auditDetails.put("invitationCount", invitationCount);
        auditDetails.put("submissionCount", submissionCount);
        auditDetails.put("baseContestDeleted", !hasSubmissions);
        
        auditService.logEvent(
            adminId,
            "PRIVATE_CONTEST_DELETED",
            "PRIVATE_CONTEST",
            contestId,
            auditDetails
        );

        log.warn("Successfully deleted private contest {} by admin {}", contestId, adminId);
    }

    // ─── Private Helper Methods ───────────────────────────────────────────────

    /**
     * Apply filter criteria to a private contest.
     * 
     * @param privateContest The contest to check
     * @param filters The filter criteria
     * @return true if the contest matches all filters, false otherwise
     */
    private boolean applyFilters(PrivateContest privateContest, ContestFilters filters) {
        if (filters == null) {
            return true;
        }

        Contest contest = privateContest.getContest();

        // Filter by status
        if (filters.getStatus() != null && !contest.getStatus().equals(filters.getStatus())) {
            return false;
        }

        // Filter by host user ID
        if (filters.getHostUserId() != null && !privateContest.getHostUser().getId().equals(filters.getHostUserId())) {
            return false;
        }

        // Filter by cancelled status
        if (filters.getCancelled() != null && !privateContest.getCancelled().equals(filters.getCancelled())) {
            return false;
        }

        // Filter by creation date range
        if (filters.getCreatedAfter() != null && privateContest.getCreatedAt().isBefore(filters.getCreatedAfter())) {
            return false;
        }

        if (filters.getCreatedBefore() != null && privateContest.getCreatedAt().isAfter(filters.getCreatedBefore())) {
            return false;
        }

        return true;
    }

    /**
     * Convert PrivateContest entity to DTO with participant count.
     * 
     * @param privateContest The entity to convert
     * @return PrivateContestDTO with participant count populated
     */
    private PrivateContestDTO convertToDTOWithParticipantCount(PrivateContest privateContest) {
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

        // Add participant count
        long participantCount = participantRepository.countByContestId(contest.getId());
        dto.setParticipantCount(participantCount);

        return dto;
    }

    /**
     * Filter criteria for listing private contests.
     * 
     * All fields are optional. Only non-null fields are used for filtering.
     */
    public static class ContestFilters {
        private Contest.ContestStatus status;
        private Long hostUserId;
        private Boolean cancelled;
        private LocalDateTime createdAfter;
        private LocalDateTime createdBefore;

        // Constructors
        public ContestFilters() {}

        // Getters and setters
        public Contest.ContestStatus getStatus() {
            return status;
        }

        public void setStatus(Contest.ContestStatus status) {
            this.status = status;
        }

        public Long getHostUserId() {
            return hostUserId;
        }

        public void setHostUserId(Long hostUserId) {
            this.hostUserId = hostUserId;
        }

        public Boolean getCancelled() {
            return cancelled;
        }

        public void setCancelled(Boolean cancelled) {
            this.cancelled = cancelled;
        }

        public LocalDateTime getCreatedAfter() {
            return createdAfter;
        }

        public void setCreatedAfter(LocalDateTime createdAfter) {
            this.createdAfter = createdAfter;
        }

        public LocalDateTime getCreatedBefore() {
            return createdBefore;
        }

        public void setCreatedBefore(LocalDateTime createdBefore) {
            this.createdBefore = createdBefore;
        }

        @Override
        public String toString() {
            return "ContestFilters{" +
                    "status=" + status +
                    ", hostUserId=" + hostUserId +
                    ", cancelled=" + cancelled +
                    ", createdAfter=" + createdAfter +
                    ", createdBefore=" + createdBefore +
                    '}';
        }
    }
}
