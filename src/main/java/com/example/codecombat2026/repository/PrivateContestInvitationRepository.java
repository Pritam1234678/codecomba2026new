package com.example.codecombat2026.repository;

import com.example.codecombat2026.entity.PrivateContestInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for PrivateContestInvitation entity.
 * 
 * Provides data access methods for managing private contest invitation tokens including:
 * - Finding invitations by token (for invitation acceptance)
 * - Finding all invitations for a specific contest
 * - Deleting expired invitations (for cleanup scheduler)
 * 
 * Requirements: 3.1, 4.4, 6.5, 7.1
 */
@Repository
public interface PrivateContestInvitationRepository extends JpaRepository<PrivateContestInvitation, Long> {
    
    /**
     * Find an invitation by its unique token.
     * Used when a user attempts to join a private contest via invite link.
     * 
     * @param token The unique invitation token
     * @return Optional containing the PrivateContestInvitation if found
     */
    Optional<PrivateContestInvitation> findByToken(String token);
    
    /**
     * Find all invitations for a specific contest.
     * Used when displaying invitation history or regenerating tokens.
     * 
     * @param contestId The ID of the Contest entity
     * @return List of PrivateContestInvitation entities for the contest
     */
    List<PrivateContestInvitation> findByContestId(Long contestId);
    
    /**
     * Delete all invitations that expired before the specified date/time.
     * Used by the scheduler to clean up expired invitation tokens.
     * 
     * @param expirationTime The cutoff time - invitations expiring before this are deleted
     * @return Count of deleted invitations
     */
    long deleteByExpiresAtBefore(LocalDateTime expirationTime);

    /**
     * Find the most recently created, non-invalidated invitation for a contest.
     * Used to locate the currently active invite token for a private contest,
     * e.g. when updating its expiry time.
     * 
     * @param contestId The ID of the Contest entity
     * @return Optional containing the active PrivateContestInvitation if found
     */
    Optional<PrivateContestInvitation> findFirstByContestIdAndInvalidatedFalseOrderByCreatedAtDesc(Long contestId);
}
