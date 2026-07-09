package com.example.codecombat2026.controller;

import com.example.codecombat2026.dto.LeaderboardEntry;
import com.example.codecombat2026.exception.ForbiddenException;
import com.example.codecombat2026.exception.ResourceNotFoundException;
import com.example.codecombat2026.security.services.UserDetailsImpl;
import com.example.codecombat2026.service.PrivateContestAccessValidator;
import com.example.codecombat2026.service.PrivateContestLeaderboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * REST Controller for private contest leaderboard operations.
 * 
 * Provides endpoints for:
 * - Getting real-time leaderboard for private contests (host or participants only)
 * 
 * Access Control:
 * - GET /leaderboard: Requires user to be either the contest host or a participant
 * 
 * Caching Strategy:
 * - Leaderboard data is stored in Valkey sorted set (private:leaderboard:{contestId})
 * - Cache control headers set to 10 seconds to enable client-side refresh rate
 * - Auto-refreshes every 10 seconds in frontend while contest is LIVE
 * 
 * Integration:
 * - PrivateContestLeaderboardService: Handles cache read/write operations
 * - PrivateContestAccessValidator: Verifies user access (host or participant)
 * 
 * Requirements: 14.3, 14.4
 */
@RestController
@RequestMapping("/api/contests/private")
public class PrivateContestLeaderboardController {

    private static final Logger log = LoggerFactory.getLogger(PrivateContestLeaderboardController.class);

    private final PrivateContestLeaderboardService leaderboardService;
    private final PrivateContestAccessValidator accessValidator;

    public PrivateContestLeaderboardController(
            PrivateContestLeaderboardService leaderboardService,
            PrivateContestAccessValidator accessValidator) {
        this.leaderboardService = leaderboardService;
        this.accessValidator = accessValidator;
    }

    /**
     * Get real-time leaderboard for a private contest.
     * 
     * Returns a sorted list of participants with their ranks, scores, and statistics.
     * The leaderboard is read from the Valkey cache (private:leaderboard:{contestId})
     * which is updated in real-time as participants submit solutions.
     * 
     * Access control:
     * - User must be authenticated (ROLE_USER)
     * - User must be either the contest host or a participant in the contest
     * - If neither, throws ForbiddenException (403)
     * 
     * Cache control headers:
     * - max-age=10: Clients can cache for 10 seconds
     * - must-revalidate: Clients must revalidate after expiry
     * - This enables frontend to auto-refresh every 10 seconds while contest is LIVE
     * 
     * Response format:
     * [
     *   {
     *     "rank": 1,
     *     "userId": 55,
     *     "userName": "Alice Johnson",
     *     "userRoll": "alice_dev",
     *     "totalScore": 300.0,
     *     "problemsSolved": 3
     *   },
     *   ...
     * ]
     * 
     * Fallback behavior:
     * - If cache is empty or unavailable, falls back to calculating from database submissions
     * - This ensures leaderboard is always available even during cold starts
     * 
     * @param contestId The private contest ID (references contests.id)
     * @param userDetails Authenticated user details (injected by Spring Security)
     * @return ResponseEntity with List of LeaderboardEntry objects (200 OK)
     * @throws ForbiddenException if user is neither host nor participant
     * @throws ResourceNotFoundException if contest doesn't exist
     * 
     * Requirements: 14.3, 14.4
     */
    @GetMapping("/{contestId}/leaderboard")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<LeaderboardEntry>> getLeaderboard(
            @PathVariable Long contestId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        
        log.debug("User {} requesting leaderboard for private contest {}", 
            userDetails.getUsername(), contestId);
        
        // Validate access: user must be host or participant
        if (!accessValidator.canAccess(contestId, userDetails.getId())) {
            log.warn("User {} denied access to leaderboard for private contest {} (not host or participant)",
                userDetails.getUsername(), contestId);
            throw new ForbiddenException(
                "Access denied. You must be the contest host or a participant to view this leaderboard.");
        }
        
        // Get leaderboard from cache (or database fallback)
        List<LeaderboardEntry> leaderboard = leaderboardService.getLeaderboard(contestId);
        
        log.debug("Retrieved leaderboard for private contest {}: {} entries", 
            contestId, leaderboard.size());
        
        // Set cache control headers for 10-second refresh rate
        // max-age=10: Clients can cache the response for 10 seconds
        // must-revalidate: After expiry, clients must revalidate before using cached data
        // This enables efficient client-side polling without overwhelming the server
        CacheControl cacheControl = CacheControl.maxAge(10, TimeUnit.SECONDS)
                .mustRevalidate();
        
        return ResponseEntity.ok()
                .cacheControl(cacheControl)
                .body(leaderboard);
    }
}
