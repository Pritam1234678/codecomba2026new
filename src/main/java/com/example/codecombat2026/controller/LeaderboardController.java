package com.example.codecombat2026.controller;

import com.example.codecombat2026.dto.LeaderboardEntry;
import com.example.codecombat2026.service.LeaderboardCacheService;
import com.example.codecombat2026.service.LeaderboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin leaderboard endpoint.
 *
 * Reads from the Valkey ZSET (O(log N)) on every request.
 * Falls back to a full DB scan only on cold start (ZSET empty),
 * then seeds the ZSET so subsequent calls are fast.
 */
@RestController
@RequestMapping("/api/admin/leaderboard")
public class LeaderboardController {

    @Autowired private LeaderboardCacheService leaderboardCache;
    @Autowired private LeaderboardService leaderboardService;

    @GetMapping("/contest/{contestId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<LeaderboardEntry>> getContestLeaderboard(@PathVariable Long contestId) {
        // Fast path: ZSET exists → O(log N) Valkey read
        if (leaderboardCache.exists(contestId)) {
            List<LeaderboardEntry> cached = leaderboardCache.getTopN(contestId, 500);
            if (!cached.isEmpty()) {
                return ResponseEntity.ok(cached);
            }
        }

        // Cold start: ZSET empty → full DB scan, then seed ZSET for next call
        List<LeaderboardEntry> fromDb = leaderboardService.getContestLeaderboard(contestId);
        if (!fromDb.isEmpty()) {
            leaderboardCache.seedFromDatabase(contestId, fromDb);
        }
        return ResponseEntity.ok(fromDb);
    }
}
