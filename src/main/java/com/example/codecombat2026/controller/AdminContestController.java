package com.example.codecombat2026.controller;

import com.example.codecombat2026.dto.MessageResponse;
import com.example.codecombat2026.entity.Contest;
import com.example.codecombat2026.entity.ContestProblem;
import com.example.codecombat2026.entity.Problem;
import com.example.codecombat2026.proctoring.repository.ProctoredContestRepository;
import com.example.codecombat2026.repository.ContestRepository;
import com.example.codecombat2026.service.ContestProblemService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/contests")
@PreAuthorize("hasRole('ADMIN')")
public class AdminContestController {

    private static final String ADMIN_CONTESTS_KEY = "admin:contests:all";
    private static final Duration ADMIN_CONTESTS_TTL = Duration.ofSeconds(60);

    @Autowired private ContestRepository contestRepository;
    @Autowired private AdminDashboardController adminDashboardController;
    @Autowired private com.example.codecombat2026.service.ContestService contestService;
    @Autowired private ContestProblemService contestProblemService;
    @Autowired private ProctoredContestRepository proctoredContestRepository;
    @Autowired private StringRedisTemplate redis;
    @Autowired private ObjectMapper objectMapper;

    @GetMapping
    public List<Map<String, Object>> getAllContests() {
        // Cache contains the bare list of Contest entities for 60s; the
        // proctored flag is an O(1) Set lookup we always resolve fresh
        // so admin's "Toggle Proctored" reflects the change without a
        // cache flush.
        List<Contest> contests = null;
        try {
            String cached = redis.opsForValue().get(ADMIN_CONTESTS_KEY);
            if (cached != null) {
                contests = objectMapper.readValue(cached, new TypeReference<List<Contest>>() {});
            }
        } catch (Exception ignored) {}

        if (contests == null) {
            contests = contestRepository.findAll();
            try {
                redis.opsForValue().set(ADMIN_CONTESTS_KEY,
                    objectMapper.writeValueAsString(contests), ADMIN_CONTESTS_TTL);
            } catch (Exception ignored) {}
        }

        // Resolve the proctored-id set once per call (Req 1.6, 1.7).
        Set<Long> proctoredIds = new HashSet<>(proctoredContestRepository.findAllContestIds());

        List<Map<String, Object>> response = new ArrayList<>(contests.size());
        for (Contest c : contests) {
            Map<String, Object> m = objectMapper.convertValue(
                    c, new TypeReference<Map<String, Object>>() {});
            m.put("proctored", proctoredIds.contains(c.getId()));
            response.add(m);
        }
        return response;
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

    // ── Problem ↔ Contest M:N management ─────────────────────────────────────

    /** Attach an existing problem to this contest. Idempotent. */
    @PostMapping("/{contestId}/problems/{problemId}")
    public ContestProblem attachProblem(@PathVariable Long contestId,
                                        @PathVariable Long problemId) {
        return contestProblemService.attach(contestId, problemId);
    }

    /** Detach a problem from this contest. Does NOT delete the problem. */
    @DeleteMapping("/{contestId}/problems/{problemId}")
    public ResponseEntity<Void> detachProblem(@PathVariable Long contestId,
                                              @PathVariable Long problemId) {
        contestProblemService.detach(contestId, problemId);
        return ResponseEntity.noContent().build();
    }

    /** Pool of problems NOT in this contest, optionally filtered, with pagination. */
    @GetMapping("/{contestId}/available-problems")
    public Map<String, Object> getAvailableProblems(
            @PathVariable Long contestId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String level,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<Problem> all = contestProblemService.findAvailable(contestId, search, level);
        int total = all.size();
        int fromIdx = Math.min(page * size, total);
        int toIdx   = Math.min(fromIdx + size, total);
        List<Problem> pageContent = all.subList(fromIdx, toIdx);

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("content", pageContent);
        response.put("totalElements", total);
        response.put("totalPages", (int) Math.ceil((double) total / size));
        response.put("page", page);
        response.put("size", size);
        return response;
    }

    public void evictAdminContestsCache() {
        try { redis.delete(ADMIN_CONTESTS_KEY); } catch (Exception ignored) {}
    }
}
