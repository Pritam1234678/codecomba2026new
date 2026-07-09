package com.example.codecombat2026.repository;

import com.example.codecombat2026.entity.PrivateContest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for PrivateContest entity.
 * 
 * Provides data access methods for private contest management including:
 * - Finding private contests by contest ID
 * - Finding all private contests hosted by a specific user
 * - Counting private contests by host within a date range (for monthly quota enforcement)
 * 
 * Requirements: 3.1, 4.4, 6.5, 7.1
 */
public interface PrivateContestRepository extends JpaRepository<PrivateContest, Long> {
    
    /**
     * Find a private contest by its associated contest ID.
     * 
     * @param contestId The ID of the Contest entity
     * @return Optional containing the PrivateContest if found
     */
    Optional<PrivateContest> findByContestId(Long contestId);
    
    /**
     * Find all private contests hosted by a specific user.
     * 
     * @param hostUserId The ID of the host user
     * @return List of PrivateContest entities hosted by the user
     */
    List<PrivateContest> findByHostUserId(Long hostUserId);
    
    /**
     * Count the number of private contests created by a host user within a specific date range.
     * Used for enforcing monthly quota (2 contests per month per host).
     * 
     * @param hostUserId The ID of the host user
     * @param startDate Start of the date range (inclusive)
     * @param endDate End of the date range (exclusive)
     * @return Count of private contests created within the date range
     */
    long countByHostUserIdAndCreatedAtBetween(Long hostUserId, LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Check if a user is the host of a specific contest.
     * Used for access validation in PrivateContestAccessValidator.
     * 
     * @param contestId The contest ID
     * @param hostUserId The user ID to check
     * @return true if the user is the host of the contest, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(pc) > 0 THEN true ELSE false END " +
           "FROM PrivateContest pc " +
           "WHERE pc.contest.id = :contestId AND pc.hostUser.id = :hostUserId")
    boolean existsByContestIdAndHostUserId(
        @Param("contestId") Long contestId,
        @Param("hostUserId") Long hostUserId
    );
}
