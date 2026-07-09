package com.example.codecombat2026.service;

import com.example.codecombat2026.exception.ForbiddenException;
import com.example.codecombat2026.repository.PrivateContestParticipantRepository;
import com.example.codecombat2026.repository.PrivateContestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for validating access permissions to private contests.
 * 
 * This service provides authorization checks to determine if a user can:
 * - Access a private contest (as host or participant)
 * - View contest details
 * - Submit solutions
 * - View leaderboards
 * 
 * Access rules:
 * - Contest hosts can always access their own contests
 * - Participants can access contests they've joined via invite link
 * - Users who are neither host nor participant are denied access
 * 
 * Cache integration:
 * - Uses PrivateContestCacheService for O(1) participant checks
 * - Falls back to database if cache miss or unavailable
 * - Reduces database load on frequently accessed contests
 * 
 * Requirements: 11.4, 23.2, 23.3, 25.3
 */
@Service
public class PrivateContestAccessValidator {

    @Autowired
    private PrivateContestRepository privateContestRepository;
    
    @Autowired
    private PrivateContestParticipantRepository participantRepository;
    
    @Autowired
    private PrivateContestCacheService cacheService;

    /**
     * Check if a user is the host of a private contest.
     * 
     * @param contestId the contest ID to check
     * @param userId the user ID to validate
     * @return true if the user is the host of the contest, false otherwise
     */
    public boolean isHost(Long contestId, Long userId) {
        if (contestId == null || userId == null) {
            return false;
        }
        return privateContestRepository.existsByContestIdAndHostUserId(contestId, userId);
    }

    /**
     * Check if a user is a participant in a private contest.
     * 
     * Uses cache-aside pattern:
     * 1. Try cache first (O(1) SET membership check)
     * 2. On cache miss or unavailable, query database
     * 3. Cache participant set for future checks (6-hour TTL)
     * 
     * @param contestId the contest ID to check
     * @param userId the user ID to validate
     * @return true if the user is a participant in the contest, false otherwise
     */
    public boolean isParticipant(Long contestId, Long userId) {
        if (contestId == null || userId == null) {
            return false;
        }
        
        // Try cache first
        if (cacheService.isParticipantSetCached(contestId)) {
            boolean isCached = cacheService.isCachedParticipant(contestId, userId);
            return isCached;
        }
        
        // Cache miss - query database
        boolean isParticipant = participantRepository.existsByContestIdAndUserId(contestId, userId);
        
        // If this is the first access, cache the entire participant set for this contest
        if (!cacheService.isParticipantSetCached(contestId)) {
            var participants = participantRepository.findByContestId(contestId);
            var userIds = participants.stream()
                    .map(p -> p.getUser().getId())
                    .collect(java.util.stream.Collectors.toSet());
            cacheService.cacheParticipantSet(contestId, userIds);
        }
        
        return isParticipant;
    }

    /**
     * Check if a user can access a private contest.
     * A user can access a contest if they are either the host or a participant.
     * 
     * This method should be used by controllers to authorize access to:
     * - Contest detail pages
     * - Contest problems
     * - Submission endpoints
     * - Leaderboards
     * - Contest analytics (host only, but access check is first step)
     * 
     * @param contestId the contest ID to check
     * @param userId the user ID to validate
     * @return true if the user is either the host or a participant, false otherwise
     */
    public boolean canAccess(Long contestId, Long userId) {
        if (contestId == null || userId == null) {
            return false;
        }
        return isHost(contestId, userId) || isParticipant(contestId, userId);
    }

    /**
     * Validate that a user can access a private contest.
     * Throws ForbiddenException if the user is neither the host nor a participant.
     * 
     * This is a convenience method for controllers that need to enforce access
     * control and automatically throw an exception on denial.
     * 
     * @param contestId the contest ID to check
     * @param userId the user ID to validate
     * @throws ForbiddenException if the user cannot access the contest
     */
    public void validateAccess(Long contestId, Long userId) {
        if (!canAccess(contestId, userId)) {
            throw new ForbiddenException(
                "You do not have permission to access this private contest. " +
                "You must be either the contest host or a registered participant."
            );
        }
    }
}
