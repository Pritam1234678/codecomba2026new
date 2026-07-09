package com.example.codecombat2026.controller;

import com.example.codecombat2026.dto.ContestAnalyticsDTO;
import com.example.codecombat2026.exception.ForbiddenException;
import com.example.codecombat2026.exception.ResourceNotFoundException;
import com.example.codecombat2026.security.services.UserDetailsImpl;
import com.example.codecombat2026.service.PrivateContestAccessValidator;
import com.example.codecombat2026.service.PrivateContestAnalyticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for private contest analytics operations.
 * 
 * Provides endpoints for:
 * - Getting analytics data in JSON format (GET /analytics)
 * - Exporting analytics as CSV file (GET /analytics/export)
 * 
 * Access Control:
 * - Both endpoints require user to be the contest host
 * - Uses PrivateContestAccessValidator to verify ownership
 * - Returns 403 Forbidden if user is not the host
 * 
 * Analytics Data Includes:
 * - Total invited participants
 * - Active participants (with at least one submission)
 * - Total submissions
 * - Per-problem statistics (submission count, acceptance rate, avg solve time)
 * - Engagement timeline (submission count per 15-minute interval)
 * 
 * Caching Strategy:
 * - Analytics for ENDED contests are cached in Valkey with 24-hour TTL
 * - UPCOMING/LIVE contests calculate analytics on every request
 * - Cache managed by PrivateContestAnalyticsService
 * 
 * CSV Export Format:
 * - Contest metadata header (name, host, dates, summary stats)
 * - Per-problem data table with headers
 * - RFC 4180 compliant CSV format
 * - Content-Disposition header for file download
 * 
 * Requirements: 16.1, 16.3 (Task 11.3)
 */
@RestController
@RequestMapping("/api/contests/private")
public class PrivateContestAnalyticsController {

    private static final Logger log = LoggerFactory.getLogger(PrivateContestAnalyticsController.class);

    private final PrivateContestAnalyticsService analyticsService;
    private final PrivateContestAccessValidator accessValidator;

    public PrivateContestAnalyticsController(
            PrivateContestAnalyticsService analyticsService,
            PrivateContestAccessValidator accessValidator) {
        this.analyticsService = analyticsService;
        this.accessValidator = accessValidator;
    }

    /**
     * Get analytics data for a private contest in JSON format.
     * 
     * Returns comprehensive analytics including:
     * - Total and active participant counts
     * - Total submission count
     * - Per-problem statistics (submission count, acceptance rate, avg solve time)
     * - Engagement timeline (submissions per 15-minute interval)
     * 
     * Access Control:
     * - User must be authenticated (ROLE_USER)
     * - User must be the contest host
     * - Throws ForbiddenException (403) if user is not the host
     * 
     * Caching:
     * - ENDED contests: Results cached in Valkey for 24 hours
     * - UPCOMING/LIVE contests: Calculated on every request (no caching)
     * 
     * Response Format:
     * {
     *   "totalParticipants": 35,
     *   "activeParticipants": 32,
     *   "totalSubmissions": 120,
     *   "problemStats": [
     *     {
     *       "problemId": 10,
     *       "problemTitle": "Two Sum",
     *       "submissionCount": 35,
     *       "acceptedSubmissions": 28,
     *       "acceptanceRate": 80.0,
     *       "avgSolveTimeMinutes": 12.5
     *     }
     *   ],
     *   "engagementTimeline": [
     *     {"timestamp": "2026-02-01T14:00:00Z", "submissionCount": 15}
     *   ]
     * }
     * 
     * @param contestId The private contest ID (references contests.id)
     * @param userDetails Authenticated user details (injected by Spring Security)
     * @return ResponseEntity with ContestAnalyticsDTO (200 OK)
     * @throws ForbiddenException if user is not the contest host
     * @throws ResourceNotFoundException if contest doesn't exist
     * 
     * Requirements: 16.1 (Task 11.3)
     */
    @GetMapping("/{contestId}/analytics")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ContestAnalyticsDTO> getAnalytics(
            @PathVariable Long contestId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        
        log.debug("User {} requesting analytics for private contest {}", 
            userDetails.getUsername(), contestId);
        
        // Validate user is the contest host
        if (!accessValidator.isHost(contestId, userDetails.getId())) {
            log.warn("User {} denied access to analytics for private contest {} (not the host)",
                userDetails.getUsername(), contestId);
            throw new ForbiddenException(
                "Access denied. Only the contest host can view analytics.");
        }
        
        // Get analytics (validates host ownership internally as well)
        ContestAnalyticsDTO analytics = analyticsService.getAnalytics(contestId, userDetails.getId());
        
        log.debug("Retrieved analytics for private contest {}: {} participants, {} submissions", 
            contestId, analytics.getTotalParticipants(), analytics.getTotalSubmissions());
        
        return ResponseEntity.ok(analytics);
    }

    /**
     * Export analytics as CSV file download.
     * 
     * Generates a CSV file containing:
     * - Contest metadata (name, host, start/end times)
     * - Summary statistics (participants, submissions)
     * - Per-problem data table with headers:
     *   - Problem ID, Problem Title, Total Submissions, Accepted Submissions, Acceptance Rate (%), Avg Solve Time (min)
     * 
     * CSV Format:
     * - RFC 4180 compliant
     * - UTF-8 encoding
     * - Fields containing commas, quotes, or newlines are wrapped in double quotes
     * - Double quotes inside fields are escaped as ""
     * 
     * Access Control:
     * - User must be authenticated (ROLE_USER)
     * - User must be the contest host
     * - Throws ForbiddenException (403) if user is not the host
     * 
     * Response Headers:
     * - Content-Type: text/csv; charset=UTF-8
     * - Content-Disposition: attachment; filename="contest_{contestId}_analytics.csv"
     * 
     * Example CSV Output:
     * ```
     * Contest Name,CS101 Midterm Exam
     * Host,prof_smith
     * Start Time,2026-02-01T14:00:00Z
     * End Time,2026-02-01T17:00:00Z
     * Total Participants,35
     * Active Participants,32
     * Total Submissions,120
     * 
     * Problem ID,Problem Title,Total Submissions,Accepted Submissions,Acceptance Rate (%),Avg Solve Time (min)
     * 10,Two Sum,35,28,80.00,12.50
     * 25,Binary Search Tree,40,20,50.00,25.30
     * ```
     * 
     * @param contestId The private contest ID (references contests.id)
     * @param userDetails Authenticated user details (injected by Spring Security)
     * @return ResponseEntity with CSV content and download headers (200 OK)
     * @throws ForbiddenException if user is not the contest host
     * @throws ResourceNotFoundException if contest doesn't exist
     * 
     * Requirements: 16.3 (Task 11.3)
     */
    @GetMapping("/{contestId}/analytics/export")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> exportAnalyticsCSV(
            @PathVariable Long contestId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        
        log.debug("User {} requesting CSV export for private contest {}", 
            userDetails.getUsername(), contestId);
        
        // Validate user is the contest host
        if (!accessValidator.isHost(contestId, userDetails.getId())) {
            log.warn("User {} denied access to CSV export for private contest {} (not the host)",
                userDetails.getUsername(), contestId);
            throw new ForbiddenException(
                "Access denied. Only the contest host can export analytics.");
        }
        
        // Generate CSV (validates host ownership internally as well)
        String csvContent = analyticsService.exportAnalyticsCSV(contestId, userDetails.getId());
        
        log.debug("Generated CSV export for private contest {}: {} bytes", contestId, csvContent.length());
        
        // Set headers for file download
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", "contest_" + contestId + "_analytics.csv");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(csvContent);
    }
}
