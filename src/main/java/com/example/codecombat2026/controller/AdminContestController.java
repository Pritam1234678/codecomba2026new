package com.example.codecombat2026.controller;

import com.example.codecombat2026.dto.MessageResponse;
import com.example.codecombat2026.entity.Contest;
import com.example.codecombat2026.repository.ContestRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/contests")
@PreAuthorize("hasRole('ADMIN')")
public class AdminContestController {

    private static final String ADMIN_CONTESTS_KEY = "admin:contests:all";
    private static final Duration ADMIN_CONTESTS_TTL = Duration.ofSeconds(60);

    @Autowired private ContestRepository contestRepository;
    @Autowired private AdminDashboardController adminDashboardController;
    @Autowired private com.example.codecombat2026.service.ContestService contestService;
    @Autowired private StringRedisTemplate redis;
    @Autowired private ObjectMapper objectMapper;

    @GetMapping
    public List<Contest> getAllContests() {
        try {
            String cached = redis.opsForValue().get(ADMIN_CONTESTS_KEY);
            if (cached != null) {
                return objectMapper.readValue(cached, new TypeReference<List<Contest>>() {});
            }
        } catch (Exception ignored) {}

        List<Contest> contests = contestRepository.findAll();
        try {
            redis.opsForValue().set(ADMIN_CONTESTS_KEY,
                objectMapper.writeValueAsString(contests), ADMIN_CONTESTS_TTL);
        } catch (Exception ignored) {}
        return contests;
    }

    @GetMapping("/stats")
    public Map<String, Long> getContestStats() {
        // Reuse the admin dashboard stats cache
        @SuppressWarnings("unchecked")
        Map<String, Object> stats = adminDashboardController.getCachedStats();
        @SuppressWarnings("unchecked")
        Map<String, Long> contestStats = (Map<String, Long>) stats.get("contestStats");
        if (contestStats != null) return contestStats;

        // Fallback if cache miss
        Map<String, Long> result = new HashMap<>();
        result.put("total", contestRepository.count());
        result.put("active", contestRepository.countByActive(true));
        result.put("inactive", contestRepository.countByActive(false));
        return result;
    }

    @PostMapping
    public Contest createContest(@RequestBody Contest contest) {
        if (contest.getActive() == null) {
            contest.setActive(false);
        }
        Contest saved = contestRepository.save(contest);
        evictAdminContestsCache();
        adminDashboardController.invalidateStatsCache();
        return saved;
    }

    @PutMapping("/{id}")
    public Contest updateContest(@PathVariable Long id, @RequestBody Contest contestDetails) {
        Contest contest = contestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contest not found"));

        contest.setName(contestDetails.getName());
        contest.setDescription(contestDetails.getDescription());
        contest.setStartTime(contestDetails.getStartTime());
        contest.setEndTime(contestDetails.getEndTime());
        contest.setActive(contestDetails.getActive());

        Contest saved = contestRepository.save(contest);
        contestService.evictContest(id);
        evictAdminContestsCache();
        adminDashboardController.invalidateStatsCache();
        return saved;
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<?> activateContest(@PathVariable Long id) {
        Contest contest = contestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contest not found"));
        contest.setActive(true);
        contestRepository.save(contest);
        contestService.evictContest(id);
        evictAdminContestsCache();
        adminDashboardController.invalidateStatsCache();
        return ResponseEntity.ok(new MessageResponse("Contest activated successfully"));
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<?> deactivateContest(@PathVariable Long id) {
        Contest contest = contestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contest not found"));
        contest.setActive(false);
        contestRepository.save(contest);
        contestService.evictContest(id);
        evictAdminContestsCache();
        adminDashboardController.invalidateStatsCache();
        return ResponseEntity.ok(new MessageResponse("Contest deactivated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteContest(@PathVariable Long id) {
        contestRepository.deleteById(id);
        contestService.evictContest(id);
        evictAdminContestsCache();
        adminDashboardController.invalidateStatsCache();
        return ResponseEntity.ok(new MessageResponse("Contest deleted successfully"));
    }

    public void evictAdminContestsCache() {
        try { redis.delete(ADMIN_CONTESTS_KEY); } catch (Exception ignored) {}
    }
}
