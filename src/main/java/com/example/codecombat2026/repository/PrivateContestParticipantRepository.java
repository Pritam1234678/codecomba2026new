package com.example.codecombat2026.repository;

import com.example.codecombat2026.entity.PrivateContestParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for PrivateContestParticipant entity.
 * 
 * Provides data access methods for managing private contest participants including:
 * - Finding all participants for a contest
 * - Finding contests a user has joined
 * - Counting participants (for enforcing 100-participant limit)
 * - Checking if a user is already a participant
 * 
 * Requirements: 3.1, 4.4, 6.5, 7.1
 */
public interface PrivateContestParticipantRepository extends JpaRepository<PrivateContestParticipant, Long> {
    
    /**
     * Find all participants for a specific contest.
     * Used when displaying the participant list to the contest host.
     * 
     * @param contestId The ID of the Contest entity
     * @return List of PrivateContestParticipant entities for the contest
     */
    List<PrivateContestParticipant> findByContestId(Long contestId);
    
    /**
     * Find all contests a user has joined as a participant.
     * Used for displaying "My Joined Contests" to participants.
     * 
     * @param userId The ID of the User entity
     * @return List of PrivateContestParticipant entities for the user
     */
    List<PrivateContestParticipant> findByUserId(Long userId);
    
    /**
     * Count the number of participants in a specific contest.
     * Used for enforcing the 100-participant limit per contest.
     * 
     * @param contestId The ID of the Contest entity
     * @return Count of participants in the contest
     */
    long countByContestId(Long contestId);
    
    /**
     * Check if a user is already a participant in a specific contest.
     * Used to prevent duplicate joins and for access control checks.
     * 
     * @param contestId The ID of the Contest entity
     * @param userId The ID of the User entity
     * @return true if the user is a participant, false otherwise
     */
    boolean existsByContestIdAndUserId(Long contestId, Long userId);
    
    /**
     * Find a specific participant record by contest and user.
     * Used when removing a participant from a contest.
     * 
     * @param contestId The ID of the Contest entity
     * @param userId The ID of the User entity
     * @return Optional containing the PrivateContestParticipant if found
     */
    Optional<PrivateContestParticipant> findByContestIdAndUserId(Long contestId, Long userId);
}
