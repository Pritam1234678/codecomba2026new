package com.example.codecombat2026.controller;

import com.example.codecombat2026.dto.LeaderboardEntry;
import com.example.codecombat2026.service.LeaderboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/leaderboard")
@CrossOrigin(origins = "*", maxAge = 3600)
public class LeaderboardController {

    @Autowired
    private LeaderboardService leaderboardService;

    @GetMapping("/contest/{contestId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<LeaderboardEntry>> getContestLeaderboard(@PathVariable Long contestId) {
        List<LeaderboardEntry> leaderboard = leaderboardService.getContestLeaderboard(contestId);
        return ResponseEntity.ok(leaderboard);
    }
}
