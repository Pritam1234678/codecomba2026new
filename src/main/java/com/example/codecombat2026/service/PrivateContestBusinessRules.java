package com.example.codecombat2026.service;

import com.example.codecombat2026.entity.Contest;
import com.example.codecombat2026.entity.PrivateContest;
import com.example.codecombat2026.repository.PrivateContestParticipantRepository;
import com.example.codecombat2026.repository.PrivateContestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Service for validating business rules for private contest creation and management.
 * 
 * This service enforces the following business rules:
 * - Contest limit: Maximum 2 private contests per host per calendar month
 * - Participant limit: Maximum 100 participants per private contest
 * - Duration limit: Maximum 5 hours (300 minutes) per contest
 * - Overlap detection: No time window overlap for a host's active private contests
 * 
 * Business rules are enforced at the service layer before persistence.
 * All validation methods throw ResponseStatusException with appropriate HTTP status codes
 * and descriptive error messages.
 * 
 * Requirements: 3.1, 3.2, 4.2, 4.3, 4.4, 6.5
 */
@Service
public class PrivateContestBusinessRules {

    private static final int MAX_CONTESTS_PER_MONTH = 2;
    private static final int MAX_PARTICIPANTS = 100;
    private static final int MAX_DURATION_MINUTES = 300; // 5 hours

    @Autowired
    private PrivateContestRepository privateContestRepository;

    @Autowired
    private PrivateContestParticipantRepository participantRepository;

    /**
     * Validate that a Contest_Host has not exceeded their monthly quota of 2 private contests.
     * 
     * Counts all private contests created by the host in the current calendar month (UTC).
     * Includes cancelled contests (those with cancelled=true) in the count.
     * Does not include deleted contests (those removed by admins).
     * 
     * @param hostUserId The ID of the Contest_Host
     * @throws ResponseStatusException with status 429 Too Many Requests if quota exceeded
     * 
     * Requirement: 3.1, 3.2, 3.3, 3.4
     */
    public void validateMonthlyQuota(Long hostUserId) {
        if (hostUserId == null) {
            throw new IllegalArgumentException("Host user ID cannot be null");
        }

        // Calculate current month boundaries (UTC)
        LocalDateTime now = LocalDateTime.now();
        YearMonth currentMonth = YearMonth.from(now);
        LocalDateTime monthStart = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = currentMonth.atEndOfMonth().atTime(23, 59, 59);

        // Count contests created this month
        long contestCount = privateContestRepository.countByHostUserIdAndCreatedAtBetween(
            hostUserId, monthStart, monthEnd
        );

        if (contestCount >= MAX_CONTESTS_PER_MONTH) {
            throw new ResponseStatusException(
                HttpStatus.TOO_MANY_REQUESTS,
                "You have reached your monthly limit of " + MAX_CONTESTS_PER_MONTH + " private contests"
            );
        }
    }

    /**
     * Validate that a private contest does not exceed the maximum duration of 5 hours.
     * 
     * @param startTime Contest start time
     * @param endTime Contest end time
     * @throws ResponseStatusException with status 400 Bad Request if duration exceeds limit
     * @throws IllegalArgumentException if times are null or endTime is before startTime
     * 
     * Requirement: 4.2, 4.3
     */
    public void validateDuration(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Start time and end time cannot be null");
        }

        if (endTime.isBefore(startTime) || endTime.isEqual(startTime)) {
            throw new IllegalArgumentException("End time must be after start time");
        }

        long durationMinutes = ChronoUnit.MINUTES.between(startTime, endTime);
        
        if (durationMinutes > MAX_DURATION_MINUTES) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Contest duration cannot exceed 5 hours"
            );
        }
    }

    /**
     * Validate that a new private contest does not overlap with any existing private contests
     * by the same host.
     * 
     * Two contests overlap if their time windows [start, end) intersect.
     * A time window is considered closed on start time and open on end time.
     * 
     * Overlap detection logic:
     * - Contest A: [startA, endA)
     * - Contest B: [startB, endB)
     * - They overlap if: startA < endB AND startB < endA
     * 
     * @param hostUserId The ID of the Contest_Host
     * @param startTime New contest start time
     * @param endTime New contest end time
     * @throws ResponseStatusException with status 409 Conflict if overlap detected
     * @throws IllegalArgumentException if parameters are null
     * 
     * Requirement: 4.4, 4.5
     */
    public void validateNoOverlap(Long hostUserId, LocalDateTime startTime, LocalDateTime endTime) {
        if (hostUserId == null) {
            throw new IllegalArgumentException("Host user ID cannot be null");
        }
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Start time and end time cannot be null");
        }

        // Get all private contests by this host
        List<PrivateContest> hostContests = privateContestRepository.findByHostUserId(hostUserId);

        // Check for overlaps
        for (PrivateContest existingContest : hostContests) {
            Contest contest = existingContest.getContest();
            
            // Skip cancelled contests
            if (existingContest.getCancelled()) {
                continue;
            }

            // Skip contests that have ended
            if (contest.getStatus() == Contest.ContestStatus.ENDED) {
                continue;
            }

            LocalDateTime existingStart = contest.getStartTime();
            LocalDateTime existingEnd = contest.getEndTime();

            // Check for overlap: startTime < existingEnd AND existingStart < endTime
            if (startTime.isBefore(existingEnd) && existingStart.isBefore(endTime)) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This contest overlaps with your existing contest '" + contest.getName() + "'"
                );
            }
        }
    }

    /**
     * Validate that a private contest has not reached the maximum participant limit of 100.
     * 
     * This method should be called before allowing a new participant to join via invite link.
     * 
     * @param contestId The ID of the contest
     * @throws ResponseStatusException with status 429 Too Many Requests if limit reached
     * @throws IllegalArgumentException if contestId is null
     * 
     * Requirement: 6.5
     */
    public void validateParticipantLimit(Long contestId) {
        if (contestId == null) {
            throw new IllegalArgumentException("Contest ID cannot be null");
        }

        long participantCount = participantRepository.countByContestId(contestId);

        if (participantCount >= MAX_PARTICIPANTS) {
            throw new ResponseStatusException(
                HttpStatus.TOO_MANY_REQUESTS,
                "This contest has reached its maximum capacity of " + MAX_PARTICIPANTS + " participants"
            );
        }
    }

    /**
     * Validate all business rules for creating a new private contest.
     * 
     * This is a convenience method that runs all validations in sequence:
     * 1. Monthly quota check
     * 2. Duration validation
     * 3. Overlap detection
     * 
     * @param hostUserId The ID of the Contest_Host
     * @param startTime Contest start time
     * @param endTime Contest end time
     * @throws ResponseStatusException if any validation fails
     * @throws IllegalArgumentException if any parameter is null or invalid
     * 
     * Requirement: 4.1, 4.2, 4.3, 4.4
     */
    public void validateContestCreation(Long hostUserId, LocalDateTime startTime, LocalDateTime endTime) {
        validateMonthlyQuota(hostUserId);
        validateDuration(startTime, endTime);
        validateNoOverlap(hostUserId, startTime, endTime);
    }

    /**
     * Get the remaining contest quota for a Contest_Host in the current calendar month.
     * 
     * @param hostUserId The ID of the Contest_Host
     * @return The number of contests the host can still create this month (0, 1, or 2)
     * @throws IllegalArgumentException if hostUserId is null
     */
    public int getRemainingMonthlyQuota(Long hostUserId) {
        if (hostUserId == null) {
            throw new IllegalArgumentException("Host user ID cannot be null");
        }

        // Calculate current month boundaries (UTC)
        LocalDateTime now = LocalDateTime.now();
        YearMonth currentMonth = YearMonth.from(now);
        LocalDateTime monthStart = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = currentMonth.atEndOfMonth().atTime(23, 59, 59);

        // Count contests created this month
        long contestCount = privateContestRepository.countByHostUserIdAndCreatedAtBetween(
            hostUserId, monthStart, monthEnd
        );

        return (int) Math.max(0, MAX_CONTESTS_PER_MONTH - contestCount);
    }
}
