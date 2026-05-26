package com.example.codecombat2026.controller;

import com.example.codecombat2026.dto.ProblemDTO;
import com.example.codecombat2026.entity.Contest;
import com.example.codecombat2026.entity.Problem;
import com.example.codecombat2026.service.ContestService;
import com.example.codecombat2026.service.ProblemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/contests")
public class ContestController {
    @Autowired
    private ContestService contestService;

    @Autowired
    private ProblemService problemService;

    @GetMapping
    public ResponseEntity<List<Contest>> getAllContests() {
        List<Contest> contests = contestService.getVisibleContests();
        return ResponseEntity.ok()
                // Cache in browser for 30s, CDN/proxy for 15s
                .header("Cache-Control", "public, max-age=30, s-maxage=15")
                .body(contests);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Contest> getContestById(@PathVariable Long id) {
        return ResponseEntity.ok(contestService.getContestById(id));
    }

    /**
     * Combined endpoint: returns contest + problems in a single request.
     * Eliminates the 2 sequential API calls ContestDetail.jsx was making
     * (GET /contests/:id then GET /problems/contest/:id).
     * Both are served from Valkey cache — typically < 5ms total.
     */
    @GetMapping("/{id}/detail")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getContestDetail(@PathVariable Long id) {
        // Fetch both in parallel — each hits Valkey cache independently
        CompletableFuture<Contest> contestFuture = CompletableFuture.supplyAsync(
                () -> contestService.getContestById(id));
        CompletableFuture<List<Problem>> problemsFuture = CompletableFuture.supplyAsync(
                () -> problemService.getProblemsByContestId(id));

        Contest contest = contestFuture.join();
        List<Problem> problems = problemsFuture.join();

        List<ProblemDTO> problemDTOs = problems.stream()
                .filter(p -> Boolean.TRUE.equals(p.getActive())) // hide disabled problems from users
                .map(p -> new ProblemDTO(
                        p.getId(), p.getTitle(), p.getDescription(),
                        p.getInputFormat(), p.getOutputFormat(), p.getConstraints(),
                        p.getTimeLimit(), p.getMemoryLimit(), p.getActive(),
                        p.getContestId(), p.getExample1(), p.getExample2(),
                        p.getExample3(), p.getImages()))
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("contest", contest);
        response.put("problems", problemDTOs);

        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Contest> createContest(@RequestBody Contest contest) {
        return ResponseEntity.ok(contestService.createContest(contest));
    }
}
