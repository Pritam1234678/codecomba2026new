package com.example.codecombat2026.service;

import com.example.codecombat2026.dto.ContestAnalyticsDTO;
import com.example.codecombat2026.dto.EngagementTimelineEntryDTO;
import com.example.codecombat2026.dto.ProblemStatDTO;
import com.example.codecombat2026.entity.Contest;
import com.example.codecombat2026.entity.ContestProblem;
import com.example.codecombat2026.entity.PrivateContest;
import com.example.codecombat2026.entity.Submission;
import com.example.codecombat2026.repository.ContestProblemRepository;
import com.example.codecombat2026.repository.ContestRepository;
import com.example.codecombat2026.repository.PrivateContestParticipantRepository;
import com.example.codecombat2026.repository.PrivateContestRepository;
import com.example.codecombat2026.repository.SubmissionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for calculating and caching analytics for private contests.
 * 
 * Provides the Contest_Host with insights about their private contest:
 * - Total and active participant counts
 * - Total submissions
 * - Per-problem stats: submission count, acceptance rate, avg solve time
 * - Engagement timeline: submission count per 15-minute interval
 * 
 * Caches analytics for ENDED contests with 24-hour TTL.
 * 
 * Requirements: 16.1, 16.4
 */
@Service
public class PrivateContestAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(PrivateContestAnalyticsService.class);

    @Autowired
    private PrivateContestRepository privateContestRepository;

    @Autowired
    private ContestRepository contestRepository;

    @Autowired
    private PrivateContestParticipantRepository participantRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private ContestProblemRepository contestProblemRepository;

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PrivateContestAccessValidator accessValidator;

    private static final String ANALYTICS_CACHE_PREFIX = "private:analytics:";
    private static final Duration CACHE_TTL = Duration.ofHours(24);
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    /**
     * Get analytics for a private contest.
     * 
     * Validates that the requesting user is the contest host, then calculates:
     * - Total participants (count of private_contest_participants rows)
     * - Active participants (count of participants with at least one submission)
     * - Total submissions (count of submissions rows for this contest)
     * - Per-problem stats: submission count, acceptance rate, avg solve time
     * - Engagement timeline: submission count per 15-minute interval
     * 
     * For ENDED contests, results are cached in Valkey with 24-hour TTL.
     * For UPCOMING/LIVE contests, results are calculated on every request (no caching).
     * 
     * Requirements: 16.1, 16.4
     * 
     * @param contestId the contest ID
     * @param hostUserId the requesting user ID (must be the contest host)
     * @return ContestAnalyticsDTO with aggregated analytics
     * @throws IllegalArgumentException if contest doesn't exist or user is not the host
     */
    public ContestAnalyticsDTO getAnalytics(Long contestId, Long hostUserId) {
        log.debug("Getting analytics for contestId={}, hostUserId={}", contestId, hostUserId);

        // 1. Validate user is the host
        PrivateContest privateContest = privateContestRepository.findByContestId(contestId)
            .orElseThrow(() -> new IllegalArgumentException("Private contest not found: " + contestId));

        if (!privateContest.getHostUser().getId().equals(hostUserId)) {
            throw new IllegalArgumentException("User " + hostUserId + " is not the host of contest " + contestId);
        }

        Contest contest = privateContest.getContest();

        // 2. Check cache for ENDED contests
        if (contest.getStatus() == Contest.ContestStatus.ENDED) {
            Optional<ContestAnalyticsDTO> cachedAnalytics = getCachedAnalytics(contestId);
            if (cachedAnalytics.isPresent()) {
                log.debug("Returning cached analytics for ended contest: contestId={}", contestId);
                return cachedAnalytics.get();
            }
        }

        // 3. Calculate analytics
        ContestAnalyticsDTO analytics = calculateAnalytics(contestId, contest);

        // 4. Cache results for ENDED contests
        if (contest.getStatus() == Contest.ContestStatus.ENDED) {
            cacheAnalytics(contestId, analytics);
        }

        return analytics;
    }

    /**
     * Calculate analytics from database queries.
     * 
     * @param contestId the contest ID
     * @param contest the Contest entity
     * @return ContestAnalyticsDTO with calculated metrics
     */
    private ContestAnalyticsDTO calculateAnalytics(Long contestId, Contest contest) {
        log.debug("Calculating analytics for contestId={}", contestId);

        // Total participants
        long totalParticipants = participantRepository.countByContestId(contestId);

        // Get all submissions for this contest
        List<Submission> allSubmissions = submissionRepository.findByContest_Id(contestId);

        // Active participants (distinct users with at least one submission)
        long activeParticipants = allSubmissions.stream()
            .map(s -> s.getUser().getId())
            .distinct()
            .count();

        // Total submissions
        int totalSubmissions = allSubmissions.size();

        // Per-problem statistics
        List<ProblemStatDTO> problemStats = calculateProblemStats(contestId, contest, allSubmissions);

        // Engagement timeline (15-minute intervals)
        List<EngagementTimelineEntryDTO> timeline = calculateEngagementTimeline(contest, allSubmissions);

        ContestAnalyticsDTO analytics = new ContestAnalyticsDTO(
            (int) totalParticipants,
            (int) activeParticipants,
            totalSubmissions,
            problemStats,
            timeline
        );

        log.debug("Calculated analytics for contestId={}: totalParticipants={}, activeParticipants={}, totalSubmissions={}",
            contestId, totalParticipants, activeParticipants, totalSubmissions);

        return analytics;
    }

    /**
     * Calculate per-problem statistics.
     * 
     * For each problem in the contest:
     * - Total submission count
     * - Accepted submission count
     * - Acceptance rate (percentage)
     * - Average solve time (minutes from contest start to first ACCEPTED submission per user)
     * 
     * @param contestId the contest ID
     * @param contest the Contest entity
     * @param allSubmissions all submissions for this contest
     * @return List of ProblemStatDTO
     */
    private List<ProblemStatDTO> calculateProblemStats(Long contestId, Contest contest, List<Submission> allSubmissions) {
        // Get all problems for this contest
        List<ContestProblem> contestProblems = contestProblemRepository.findByContestIdOrderByDisplayOrderAscAddedAtAsc(contestId);

        List<ProblemStatDTO> problemStats = new ArrayList<>();

        for (ContestProblem cp : contestProblems) {
            Long problemId = cp.getProblemId();
            String problemTitle = cp.getProblem() != null ? cp.getProblem().getTitle() : "Unknown";

            // Filter submissions for this problem
            List<Submission> problemSubmissions = allSubmissions.stream()
                .filter(s -> s.getProblem().getId().equals(problemId))
                .collect(Collectors.toList());

            int submissionCount = problemSubmissions.size();

            // Count ACCEPTED submissions
            int acceptedCount = (int) problemSubmissions.stream()
                .filter(s -> s.getStatus() == Submission.SubmissionStatus.AC)
                .count();

            // Calculate acceptance rate
            double acceptanceRate = submissionCount > 0 
                ? (acceptedCount * 100.0 / submissionCount) 
                : 0.0;

            // Calculate average solve time
            Double avgSolveTime = calculateAvgSolveTime(contest, problemSubmissions);

            ProblemStatDTO stat = new ProblemStatDTO(
                problemId,
                problemTitle,
                submissionCount,
                acceptedCount,
                acceptanceRate,
                avgSolveTime
            );

            problemStats.add(stat);
        }

        return problemStats;
    }

    /**
     * Calculate average solve time for a problem.
     * 
     * For each user who solved the problem (has at least one ACCEPTED submission):
     * - Find their first ACCEPTED submission timestamp
     * - Calculate time difference from contest start time
     * - Average across all users who solved it
     * 
     * @param contest the Contest entity
     * @param problemSubmissions all submissions for a specific problem
     * @return average solve time in minutes, or null if no one solved it
     */
    private Double calculateAvgSolveTime(Contest contest, List<Submission> problemSubmissions) {
        LocalDateTime contestStart = contest.getStartTime();
        if (contestStart == null) {
            return null;
        }

        // Group submissions by user, find first ACCEPTED submission per user
        Map<Long, LocalDateTime> userFirstSolve = new HashMap<>();

        for (Submission sub : problemSubmissions) {
            if (sub.getStatus() == Submission.SubmissionStatus.AC) {
                Long userId = sub.getUser().getId();
                LocalDateTime subTime = sub.getSubmittedAt();
                
                if (subTime != null) {
                    // Keep only the earliest ACCEPTED submission per user
                    userFirstSolve.merge(userId, subTime, (existing, newTime) -> 
                        newTime.isBefore(existing) ? newTime : existing
                    );
                }
            }
        }

        if (userFirstSolve.isEmpty()) {
            return null; // No one solved this problem
        }

        // Calculate solve times in minutes
        List<Long> solveTimesMinutes = userFirstSolve.values().stream()
            .map(solveTime -> ChronoUnit.MINUTES.between(contestStart, solveTime))
            .collect(Collectors.toList());

        // Average
        double avgMinutes = solveTimesMinutes.stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0.0);

        return avgMinutes;
    }

    /**
     * Calculate engagement timeline with 15-minute intervals.
     * 
     * Creates a list of 15-minute buckets from contest start to end time,
     * counting how many submissions were made in each bucket.
     * 
     * @param contest the Contest entity
     * @param allSubmissions all submissions for this contest
     * @return List of EngagementTimelineEntryDTO
     */
    private List<EngagementTimelineEntryDTO> calculateEngagementTimeline(Contest contest, List<Submission> allSubmissions) {
        LocalDateTime start = contest.getStartTime();
        LocalDateTime end = contest.getEndTime();

        if (start == null || end == null) {
            return Collections.emptyList();
        }

        // Create 15-minute buckets
        List<LocalDateTime> buckets = new ArrayList<>();
        LocalDateTime current = start;
        while (!current.isAfter(end)) {
            buckets.add(current);
            current = current.plusMinutes(15);
        }

        // Count submissions per bucket
        Map<LocalDateTime, Integer> bucketCounts = new HashMap<>();
        for (LocalDateTime bucket : buckets) {
            bucketCounts.put(bucket, 0);
        }

        for (Submission sub : allSubmissions) {
            LocalDateTime subTime = sub.getSubmittedAt();
            if (subTime != null && !subTime.isBefore(start) && !subTime.isAfter(end)) {
                // Find which bucket this submission belongs to
                LocalDateTime bucket = findBucket(start, subTime);
                bucketCounts.merge(bucket, 1, Integer::sum);
            }
        }

        // Convert to DTO list
        List<EngagementTimelineEntryDTO> timeline = buckets.stream()
            .map(bucket -> new EngagementTimelineEntryDTO(
                bucket.format(ISO_FORMATTER),
                bucketCounts.getOrDefault(bucket, 0)
            ))
            .collect(Collectors.toList());

        return timeline;
    }

    /**
     * Find which 15-minute bucket a submission time belongs to.
     * 
     * @param contestStart contest start time
     * @param submissionTime submission timestamp
     * @return the start of the 15-minute bucket
     */
    private LocalDateTime findBucket(LocalDateTime contestStart, LocalDateTime submissionTime) {
        long minutesSinceStart = ChronoUnit.MINUTES.between(contestStart, submissionTime);
        long bucketIndex = minutesSinceStart / 15;
        return contestStart.plusMinutes(bucketIndex * 15);
    }

    /**
     * Retrieve cached analytics from Valkey.
     * 
     * @param contestId the contest ID
     * @return Optional containing cached analytics, or empty if cache miss
     */
    private Optional<ContestAnalyticsDTO> getCachedAnalytics(Long contestId) {
        try {
            String key = ANALYTICS_CACHE_PREFIX + contestId;
            String json = redis.opsForValue().get(key);

            if (json == null) {
                log.debug("Cache miss for analytics: contestId={}", contestId);
                return Optional.empty();
            }

            ContestAnalyticsDTO dto = objectMapper.readValue(json, ContestAnalyticsDTO.class);
            log.debug("Cache hit for analytics: contestId={}", contestId);
            return Optional.of(dto);
        } catch (Exception e) {
            log.warn("Failed to read cached analytics for contestId={}: {}", contestId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Cache analytics in Valkey with 24-hour TTL.
     * 
     * @param contestId the contest ID
     * @param analytics the analytics DTO to cache
     */
    private void cacheAnalytics(Long contestId, ContestAnalyticsDTO analytics) {
        try {
            String key = ANALYTICS_CACHE_PREFIX + contestId;
            String json = objectMapper.writeValueAsString(analytics);
            redis.opsForValue().set(key, json, CACHE_TTL);
            log.debug("Cached analytics for contestId={}", contestId);
        } catch (Exception e) {
            log.warn("Failed to cache analytics for contestId={}: {}", contestId, e.getMessage());
        }
    }

    /**
     * Invalidate cached analytics for a contest.
     * 
     * Should be called when:
     * - New submissions are made
     * - Contest transitions to ENDED status
     * - Manual refresh is requested
     * 
     * @param contestId the contest ID
     */
    public void invalidateCache(Long contestId) {
        try {
            String key = ANALYTICS_CACHE_PREFIX + contestId;
            redis.delete(key);
            log.debug("Invalidated analytics cache for contestId={}", contestId);
        } catch (Exception e) {
            log.warn("Failed to invalidate analytics cache for contestId={}: {}", contestId, e.getMessage());
        }
    }

    /**
     * Export analytics as CSV format.
     * 
     * Generates a CSV file containing contest analytics data:
     * - Contest metadata (name, host, dates)
     * - Summary statistics (participants, submissions)
     * - Per-problem statistics with headers
     * 
     * CSV Format:
     * Row 1: Contest Name, [contest name]
     * Row 2: Host, [host username]
     * Row 3: Start Time, [ISO timestamp]
     * Row 4: End Time, [ISO timestamp]
     * Row 5: Total Participants, [count]
     * Row 6: Active Participants, [count]
     * Row 7: Total Submissions, [count]
     * Row 8: (blank)
     * Row 9: Problem ID, Problem Title, Total Submissions, Accepted Submissions, Acceptance Rate (%), Avg Solve Time (min)
     * Row 10+: [problem data rows]
     * 
     * Requirements: 16.3
     * 
     * @param contestId the contest ID
     * @param hostUserId the requesting user ID (must be the contest host)
     * @return CSV content as String
     * @throws IllegalArgumentException if contest doesn't exist or user is not the host
     */
    public String exportAnalyticsCSV(Long contestId, Long hostUserId) {
        log.debug("Exporting analytics CSV for contestId={}, hostUserId={}", contestId, hostUserId);

        // Get analytics (this validates host ownership)
        ContestAnalyticsDTO analytics = getAnalytics(contestId, hostUserId);

        // Get contest details
        PrivateContest privateContest = privateContestRepository.findByContestId(contestId)
            .orElseThrow(() -> new IllegalArgumentException("Private contest not found: " + contestId));
        Contest contest = privateContest.getContest();
        String hostUsername = privateContest.getHostUser().getUsername();

        // Build CSV
        StringBuilder csv = new StringBuilder();

        // Header section with contest metadata
        csv.append("Contest Name,").append(escapeCsvValue(contest.getName())).append("\n");
        csv.append("Host,").append(escapeCsvValue(hostUsername)).append("\n");
        csv.append("Start Time,").append(contest.getStartTime() != null ? contest.getStartTime().format(ISO_FORMATTER) : "").append("\n");
        csv.append("End Time,").append(contest.getEndTime() != null ? contest.getEndTime().format(ISO_FORMATTER) : "").append("\n");
        csv.append("Total Participants,").append(analytics.getTotalParticipants()).append("\n");
        csv.append("Active Participants,").append(analytics.getActiveParticipants()).append("\n");
        csv.append("Total Submissions,").append(analytics.getTotalSubmissions()).append("\n");
        csv.append("\n"); // Blank line separator

        // Problem statistics table
        csv.append("Problem ID,Problem Title,Total Submissions,Accepted Submissions,Acceptance Rate (%),Avg Solve Time (min)\n");

        for (ProblemStatDTO stat : analytics.getProblemStats()) {
            csv.append(stat.getProblemId()).append(",");
            csv.append(escapeCsvValue(stat.getProblemTitle())).append(",");
            csv.append(stat.getSubmissionCount()).append(",");
            csv.append(stat.getAcceptedSubmissions()).append(",");
            csv.append(String.format("%.2f", stat.getAcceptanceRate())).append(",");
            csv.append(stat.getAvgSolveTimeMinutes() != null ? String.format("%.2f", stat.getAvgSolveTimeMinutes()) : "N/A");
            csv.append("\n");
        }

        log.debug("Generated CSV export for contestId={}, size={} bytes", contestId, csv.length());
        return csv.toString();
    }

    /**
     * Escape CSV value to handle commas, quotes, and newlines.
     * 
     * Rules:
     * - If value contains comma, quote, or newline, wrap in double quotes
     * - Double quotes inside value are escaped as ""
     * 
     * @param value the raw value
     * @return escaped CSV value
     */
    private String escapeCsvValue(String value) {
        if (value == null) {
            return "";
        }

        // Check if value needs escaping
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            // Escape double quotes by doubling them
            String escaped = value.replace("\"", "\"\"");
            // Wrap in double quotes
            return "\"" + escaped + "\"";
        }

        return value;
    }
}
