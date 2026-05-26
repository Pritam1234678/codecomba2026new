package com.example.codecombat2026.controller;

import com.example.codecombat2026.dto.MessageResponse;
import com.example.codecombat2026.entity.Contest;
import com.example.codecombat2026.entity.Problem;
import com.example.codecombat2026.repository.ContestRepository;
import com.example.codecombat2026.repository.ProblemRepository;
import com.example.codecombat2026.service.CacheService;
import com.example.codecombat2026.service.ProblemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin/problems")
@PreAuthorize("hasRole('ADMIN')")
public class AdminProblemController {

    @Autowired private ProblemRepository problemRepository;
    @Autowired private ContestRepository contestRepository;
    @Autowired private ProblemService problemService;   // for cache eviction
    @Autowired private CacheService cacheService;       // for snippet cache eviction

    @GetMapping
    public List<Problem> getAllProblems() {
        return problemRepository.findAll();
    }

    @GetMapping("/contest/{contestId}")
    public List<Problem> getProblemsByContest(@PathVariable Long contestId) {
        return problemRepository.findByContestId(contestId);
    }

    @PostMapping("/contest/{contestId}")
    public Problem createProblem(@PathVariable Long contestId, @RequestBody Problem problem) {
        Contest contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new RuntimeException("Contest not found"));
        problem.setContest(contest);
        if (problem.getActive() == null) {
            problem.setActive(true);
        }
        if (problem.getLevel() == null || problem.getLevel().isBlank()) {
            problem.setLevel("MEDIUM");
        }
        Problem saved = problemRepository.save(problem);
        problemService.evictContestProblems(contestId);
        return saved;
    }

    @PutMapping("/{id}")
    public Problem updateProblem(@PathVariable Long id, @RequestBody Problem problemDetails) {
        Problem problem = problemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Problem not found"));

        problem.setTitle(problemDetails.getTitle());
        problem.setDescription(problemDetails.getDescription());
        problem.setInputFormat(problemDetails.getInputFormat());
        problem.setOutputFormat(problemDetails.getOutputFormat());
        problem.setConstraints(problemDetails.getConstraints());
        problem.setTimeLimit(problemDetails.getTimeLimit());
        problem.setMemoryLimit(problemDetails.getMemoryLimit());
        problem.setExample1(problemDetails.getExample1());
        problem.setExample2(problemDetails.getExample2());
        problem.setExample3(problemDetails.getExample3());
        problem.setImages(problemDetails.getImages());
        if (problemDetails.getActive() != null) {
            problem.setActive(problemDetails.getActive());
        }
        if (problemDetails.getLevel() != null && !problemDetails.getLevel().isBlank()) {
            problem.setLevel(problemDetails.getLevel());
        }

        Problem saved = problemRepository.save(problem);
        problemService.evictProblem(id);
        cacheService.evictProblem(id);
        if (problem.getContestId() != null) {
            problemService.evictContestProblems(problem.getContestId());
        }
        return saved;
    }

    /** Toggle active/inactive for a problem */
    @PatchMapping("/{id}/toggle-active")
    public Problem toggleActive(@PathVariable Long id) {
        Problem problem = problemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Problem not found"));
        problem.setActive(!Boolean.TRUE.equals(problem.getActive()));
        Problem saved = problemRepository.save(problem);
        problemService.evictProblem(id);
        cacheService.evictProblem(id);
        if (problem.getContestId() != null) {
            problemService.evictContestProblems(problem.getContestId());
        }
        return saved;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProblem(@PathVariable Long id) {
        // Get contestId before deleting for cache eviction
        Problem problem = problemRepository.findById(id).orElse(null);
        Long contestId = problem != null ? problem.getContestId() : null;

        problemRepository.deleteById(id);

        // Evict all caches
        problemService.evictProblem(id);
        cacheService.evictProblem(id);
        if (contestId != null) {
            problemService.evictContestProblems(contestId);
        }

        return ResponseEntity.ok(new MessageResponse("Problem deleted successfully"));
    }
}
