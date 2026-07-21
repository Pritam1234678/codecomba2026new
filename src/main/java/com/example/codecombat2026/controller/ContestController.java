package com.example.codecombat2026.controller;

import com.example.codecombat2026.dto.ProblemDTO;
import com.example.codecombat2026.entity.Contest;
import com.example.codecombat2026.entity.ContestRegistration;
import com.example.codecombat2026.entity.Problem;
import com.example.codecombat2026.proctoring.repository.ProctoredContestRepository;
import com.example.codecombat2026.security.services.UserDetailsImpl;
import com.example.codecombat2026.service.ContestRegistrationService;
import com.example.codecombat2026.service.ContestService;
import com.example.codecombat2026.service.ProblemService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/contests")
public class ContestController {
    @Autowired
    private ContestService contestService;

    @Autowired
    private ProblemService problemService;

    @Autowired
    private ContestRegistrationService registrationService;

    @Autowired
    private ProctoredContestRepository proctoredContestRepo;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.example.codecombat2026.repository.SubmissionRepository submissionRepository;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllContests() {
        List<Contest> contests = contestService.getVisibleContests();

        Set<Long> proctoredIds = new HashSet<>(proctoredContestRepo.findAllContestIds());

        // Batch-fetch participant counts — one query for all contests
        List<Long> contestIds = contests.stream().map(Contest::getId).collect(Collectors.toList());
        Map<Long, Long> participantCounts = new HashMap<>();
        if (!contestIds.isEmpty()) {
            for (Object[] row : submissionRepository.countParticipantsByContestIds(contestIds)) {
                participantCounts.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
            }
        }

        List<Map<String, Object>> response = contests.stream()
                .map(c -> {
                    Map<String, Object> m = objectMapper.convertValue(
                            c, new TypeReference<Map<String, Object>>() {});
                    m.put("proctored", proctoredIds.contains(c.getId()));
                    m.put("participantCount", participantCounts.getOrDefault(c.getId(), 0L));
                    return m;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok()
                .header("Cache-Control", "public, max-age=30, s-maxage=15")
                .body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getContestById(@PathVariable Long id) {
        Contest contest = contestService.getContestById(id);
        Map<String, Object> response = objectMapper.convertValue(
                contest, new TypeReference<Map<String, Object>>() {});
        response.put("proctored", proctoredContestRepo.existsByContestId(id));
        return ResponseEntity.ok(response);
    }

    /**
     * Combined endpoint: returns contest + problems in a single request.
     * Eliminates the 2 sequential API calls ContestDetail.jsx was making
     * (GET /contests/:id then GET /problems/contest/:id).
     * Both are served from Valkey cache — typically < 5ms total.
     */
    @GetMapping("/{id}/detail")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getContestDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        // Fetch contest + problems in parallel — each hits Valkey cache independently
        CompletableFuture<Contest> contestFuture = CompletableFuture.supplyAsync(
                () -> contestService.getContestById(id));
        CompletableFuture<List<Problem>> problemsFuture = CompletableFuture.supplyAsync(
                () -> problemService.getProblemsByContestId(id));

        Contest contest = contestFuture.join();
        List<Problem> problems = problemsFuture.join();

        List<ProblemDTO> problemDTOs = problems.stream()
                .filter(p -> Boolean.TRUE.equals(p.getActive()))
                .map(p -> new ProblemDTO(
                        p.getId(), p.getTitle(), p.getDescription(),
                        p.getInputFormat(), p.getOutputFormat(), p.getConstraints(),
                        p.getTimeLimit(), p.getMemoryLimit(), p.getActive(),
                        p.getContestId(), p.getExample1(), p.getExample2(),
                        p.getExample3(), p.getImages(), p.getTopics()))
                .collect(Collectors.toList());

        boolean registered = registrationService.isRegistered(id, userDetails.getId());
        long registrationCount = registrationService.countRegistrations(id);

        Map<String, Object> response = new HashMap<>();
        response.put("contest", contest);
        response.put("problems", problemDTOs);
        response.put("registered", registered);
        response.put("registrationCount", registrationCount);
        response.put("proctored", proctoredContestRepo.existsByContestId(id));

        return ResponseEntity.ok(response);
    }

    /** Register the current user for a contest. Idempotent. */
    @PostMapping("/{id}/register")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> registerForContest(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        ContestRegistration reg = registrationService.register(id, userDetails.getId());
        long count = registrationService.countRegistrations(id);

        Map<String, Object> response = new HashMap<>();
        response.put("registered", true);
        response.put("registrationCount", count);
        response.put("registeredAt", reg.getRegisteredAt());
        return ResponseEntity.ok(response);
    }

    /** Check registration status for the current user. */
    @GetMapping("/{id}/registration-status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> registrationStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        boolean registered = registrationService.isRegistered(id, userDetails.getId());
        long count = registrationService.countRegistrations(id);

        Map<String, Object> response = new HashMap<>();
        response.put("registered", registered);
        response.put("registrationCount", count);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Contest> createContest(@RequestBody Contest contest) {
        return ResponseEntity.ok(contestService.createContest(contest));
    }
}
