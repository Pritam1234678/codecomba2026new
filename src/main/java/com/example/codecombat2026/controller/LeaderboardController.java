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
        if (leaderboardCache.exists(contestId)) {
            List<LeaderboardEntry> cached = leaderboardCache.getTopN(contestId, 500);
            if (!cached.isEmpty()) {
                // Cache entries have no user data — enrich from DB batch
                List<Long> userIds = cached.stream()
                    .map(LeaderboardEntry::getUserId).collect(Collectors.toList());
                Map<Long, LeaderboardEntry> userInfo = leaderboardService.batchUserInfo(userIds);
                for (LeaderboardEntry e : cached) {
                    LeaderboardEntry info = userInfo.get(e.getUserId());
                    if (info != null) {
                        if (e.getUserName() == null) e.setUserName(info.getUserName());
                        if (e.getUserRoll() == null) e.setUserRoll(info.getUserRoll());
                        if (e.getProblemsSolved() == null || e.getProblemsSolved() == 0)
                            e.setProblemsSolved(info.getProblemsSolved());
                        if (e.getPhotoUrl() == null) e.setPhotoUrl(info.getPhotoUrl());
                    }
                }
                return ResponseEntity.ok(cached);
            }
        }

        List<LeaderboardEntry> fromDb = leaderboardService.getContestLeaderboard(contestId);
        if (!fromDb.isEmpty()) {
            leaderboardCache.seedFromDatabase(contestId, fromDb);
        }
        return ResponseEntity.ok(fromDb);
    }
}
