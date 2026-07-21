package com.example.codecombat2026.controller;

import com.example.codecombat2026.dto.ProblemDTO;
import com.example.codecombat2026.entity.Problem;
import com.example.codecombat2026.entity.User;
import com.example.codecombat2026.entity.PracticeSubmission;
import com.example.codecombat2026.entity.UserProblemSolved;
import com.example.codecombat2026.repository.ProblemRepository;
import com.example.codecombat2026.repository.UserProblemSolvedRepository;
import com.example.codecombat2026.repository.UserRepository;
import com.example.codecombat2026.security.services.UserDetailsImpl;
import com.example.codecombat2026.service.PracticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Practice mode controller — separate from contest submission flow.
 * GET /api/practice/problems  — all active problems with user's solved flag
 * POST /api/practice/run      — judge user's code against the problem harness
 * GET /api/practice/stats     — user's points + solved count
 */
@RestController
@RequestMapping("/api/practice")
public class PracticeController {

    @Autowired private ProblemRepository problemRepository;
    @Autowired private UserProblemSolvedRepository solvedRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PracticeService practiceService;
    @Autowired private org.springframework.data.redis.core.StringRedisTemplate redis;
    @Autowired private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    private static final java.time.Duration SOLVED_TTL = java.time.Duration.ofSeconds(30);

    /** Returns all active problems with a "solved" flag for the current user. */
    @GetMapping("/problems")
    @PreAuthorize("isAuthenticated()")
    public List<PracticeProblemDTO> listProblems(@AuthenticationPrincipal UserDetailsImpl user) {
        // Cache solved IDs per user (30s TTL, invalidated on AC verdict)
        Set<Long> solvedIds = getSolvedIds(user.getId());

        // Reuse the problems:all cache from ProblemService (60s TTL)
        List<Problem> allProblems;
        try {
            String cached = redis.opsForValue().get("problems:all");
            if (cached != null) {
                allProblems = objectMapper.readValue(cached,
                    new com.fasterxml.jackson.core.type.TypeReference<List<Problem>>() {});
            } else {
                allProblems = problemRepository.findAll();
                try {
                    redis.opsForValue().set("problems:all",
                        objectMapper.writeValueAsString(allProblems),
                        java.time.Duration.ofSeconds(60));
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {
            allProblems = problemRepository.findAll();
        }

        return allProblems.stream()
                .filter(p -> Boolean.TRUE.equals(p.getActive()))
                .map(p -> new PracticeProblemDTO(
                        p.getId(),
                        p.getTitle(),
                        p.getDescription(),
                        p.getLevel(),
                        p.getTimeLimit(),
                        p.getMemoryLimit(),
                        solvedIds.contains(p.getId()),
                        practiceService.pointsForLevel(p.getLevel())
                ))
                .collect(Collectors.toList());
    }

    /** Cache-aside for solved IDs — short TTL, invalidated on AC. */
    @SuppressWarnings("unchecked")
    private Set<Long> getSolvedIds(Long userId) {
        String key = "solved:" + userId;
        try {
            String cached = redis.opsForValue().get(key);
            if (cached != null) {
                List<Long> ids = objectMapper.readValue(cached,
                    new com.fasterxml.jackson.core.type.TypeReference<List<Long>>() {});
                return new HashSet<>(ids);
            }
        } catch (Exception ignored) {}

        Set<Long> ids = solvedRepository.findByUserId(userId).stream()
                .map(UserProblemSolved::getProblemId)
                .collect(Collectors.toCollection(HashSet::new));

        try {
            redis.opsForValue().set(key,
                objectMapper.writeValueAsString(new java.util.ArrayList<>(ids)),
                SOLVED_TTL);
        } catch (Exception ignored) {}

        return ids;
    }

    /** Evict solved cache for a user — call after AC verdict. */
    public void evictSolvedCache(Long userId) {
        try { redis.delete("solved:" + userId); } catch (Exception ignored) {}
    }

    /** Returns a single problem (active only) with full details. */
    @GetMapping("/problems/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getProblem(@PathVariable Long id, @AuthenticationPrincipal UserDetailsImpl user) {
        Problem p = problemRepository.findById(id).orElse(null);
        if (p == null || !Boolean.TRUE.equals(p.getActive())) {
            return ResponseEntity.notFound().build();
        }
        boolean solved = solvedRepository.existsByUserIdAndProblemId(user.getId(), id);
        Map<String, Object> resp = new HashMap<>();
        resp.put("problem", new ProblemDTO(
                p.getId(), p.getTitle(), p.getDescription(),
                p.getInputFormat(), p.getOutputFormat(), p.getConstraints(),
                p.getTimeLimit(), p.getMemoryLimit(), p.getActive(),
                p.getContestId(), p.getExample1(), p.getExample2(),
                p.getExample3(), p.getImages()));
        resp.put("solved", solved);
        resp.put("pointsAvailable", practiceService.pointsForLevel(p.getLevel()));
        return ResponseEntity.ok(resp);
    }

    /** Enqueues user's code for async judging — verdict delivered via SSE. */
    @PostMapping("/run")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> runPractice(
            @RequestBody PracticeRunRequest req,
            @AuthenticationPrincipal UserDetailsImpl user) {
        if (req.code == null || req.code.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        if (req.code.length() > 50_000) {
            return ResponseEntity.badRequest().build();
        }
        try {
            Long submissionId = practiceService.enqueuePractice(user.getId(), req.problemId, req.code, req.language);
            Map<String, Object> resp = new HashMap<>();
            resp.put("submissionId", submissionId);
            resp.put("message", "Accepted — verdict will be delivered via SSE.");
            return ResponseEntity.accepted().body(resp);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private static final java.time.Duration SUBMISSION_CACHE_TTL = java.time.Duration.ofMinutes(5);

    /** Fetch all practice submissions for a user on a specific problem. */
    @GetMapping("/submissions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getSubmissions(
            @RequestParam Long problemId,
            @AuthenticationPrincipal UserDetailsImpl user) {

        String cacheKey = "practice:submissions:" + user.getId() + ":" + problemId;
        try {
            String cached = redis.opsForValue().get(cacheKey);
            if (cached != null) {
                java.util.List<Map<String, Object>> cachedList =
                    objectMapper.readValue(cached,
                        new com.fasterxml.jackson.core.type.TypeReference<java.util.List<Map<String, Object>>>() {});
                return ResponseEntity.ok(cachedList);
            }
        } catch (Exception ignored) {}

        List<PracticeSubmission> subs = practiceService.getPracticeSubmissions(user.getId(), problemId);
        List<Map<String, Object>> result = subs.stream().map(s -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", s.getId());
            m.put("status", s.getStatus() != null ? s.getStatus().name() : null);
            m.put("language", s.getLanguage() != null ? s.getLanguage().name() : null);
            m.put("submittedAt", s.getSubmittedAt() != null ? s.getSubmittedAt().toString() : null);
            m.put("timeConsumed", s.getTimeConsumed());
            m.put("testCasesPassed", s.getTestCasesPassed());
            m.put("totalTestCases", s.getTotalTestCases());
            m.put("errorMessage", s.getErrorMessage());
            m.put("testCaseDetails", s.getTestCaseDetails());
            m.put("score", s.getScore());
            m.put("code", s.getCode());
            return m;
        }).collect(Collectors.toList());

        try {
            redis.opsForValue().set(cacheKey, objectMapper.writeValueAsString(result), SUBMISSION_CACHE_TTL);
        } catch (Exception ignored) {}

        return ResponseEntity.ok(result);
    }

    /** Invalidate submission history cache for a user+problem. */
    public void evictSubmissionCache(Long userId, Long problemId) {
        try { redis.delete("practice:submissions:" + userId + ":" + problemId); } catch (Exception ignored) {}
    }

    /** User's overall practice stats. */
    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getStats(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        User u = userRepository.findById(userDetails.getId()).orElse(null);
        long solvedCount = solvedRepository.countByUserId(userDetails.getId());
        Map<String, Object> resp = new HashMap<>();
        resp.put("totalPoints", u != null && u.getTotalPoints() != null ? u.getTotalPoints() : 0);
        resp.put("solvedCount", solvedCount);
        return ResponseEntity.ok(resp);
    }

    // ─── DTOs ─────────────────────────────────────────────────────────────────

    public static class PracticeProblemDTO {
        public Long id;
        public String title;
        public String description;
        public String level;
        public Double timeLimit;
        public Integer memoryLimit;
        public boolean solved;
        public int pointsAvailable;

        public PracticeProblemDTO(Long id, String title, String description, String level,
                                   Double timeLimit, Integer memoryLimit, boolean solved, int pointsAvailable) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.level = level;
            this.timeLimit = timeLimit;
            this.memoryLimit = memoryLimit;
            this.solved = solved;
            this.pointsAvailable = pointsAvailable;
        }
    }

    public static class PracticeRunRequest {
        public Long problemId;
        public String code;
        public String language;
    }
}
