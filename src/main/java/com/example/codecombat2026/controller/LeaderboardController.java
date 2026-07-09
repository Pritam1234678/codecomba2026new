package com.example.codecombat2026.controller;

import com.example.codecombat2026.dto.LeaderboardEntry;
import com.example.codecombat2026.service.LeaderboardCacheService;
import com.example.codecombat2026.service.LeaderboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/leaderboard")
public class LeaderboardController {

    @Autowired private LeaderboardCacheService leaderboardCache;
    @Autowired private LeaderboardService leaderboardService;

    @GetMapping("/contest/{contestId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<LeaderboardEntry>> getContestLeaderboard(@PathVariable Long contestId) {
        // Always read from DB — the service queries the submissions table and
        // computes totalScore + problemsSolved correctly. The ZSET cache is
        // only useful for real-time display; for the admin page, consistency
        // matters more than a few ms of latency.
        List<LeaderboardEntry> fromDb = leaderboardService.getContestLeaderboard(contestId);
        if (!fromDb.isEmpty()) {
            leaderboardCache.seedFromDatabase(contestId, fromDb);
        }
        return ResponseEntity.ok(fromDb);
    }
}
