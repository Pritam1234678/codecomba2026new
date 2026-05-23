package com.example.codecombat2026.controller;

import com.example.codecombat2026.dto.ContestStatusDTO;
import com.example.codecombat2026.dto.ProblemDTO;
import com.example.codecombat2026.entity.Problem;
import com.example.codecombat2026.service.ProblemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/problems")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ProblemController {
    @Autowired
    private ProblemService problemService;

    @Autowired
    private com.example.codecombat2026.service.ContestService contestService;

    @Autowired
    private com.example.codecombat2026.repository.ProblemRepository problemRepository;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<ProblemDTO> getAllProblems() {
        List<Problem> problems = problemService.getAllProblems();
        return problems.stream()
                .filter(p -> Boolean.TRUE.equals(p.getActive()))
                .map(p -> new ProblemDTO(
                        p.getId(),
                        p.getTitle(),
                        p.getDescription(),
                        p.getInputFormat(),
                        p.getOutputFormat(),
                        p.getConstraints(),
                        p.getTimeLimit(),
                        p.getMemoryLimit(),
                        p.getActive(),
                        p.getContestId(),
                        p.getExample1(),
                        p.getExample2(),
                        p.getExample3(),
                        p.getImages()))
                .collect(java.util.stream.Collectors.toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProblemDTO> getProblemById(@PathVariable Long id) {
        Problem p = problemService.getProblemById(id);
        // Return 404 if problem is disabled — user should not access it
        if (!Boolean.TRUE.equals(p.getActive())) {
            return ResponseEntity.notFound().build();
        }
        ProblemDTO dto = new ProblemDTO(
                p.getId(),
                p.getTitle(),
                p.getDescription(),
                p.getInputFormat(),
                p.getOutputFormat(),
                p.getConstraints(),
                p.getTimeLimit(),
                p.getMemoryLimit(),
                p.getActive(),
                p.getContestId(),
                p.getExample1(),
                p.getExample2(),
                p.getExample3(),
                p.getImages());
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/contest/{contestId}")
    @PreAuthorize("isAuthenticated()")
    public List<ProblemDTO> getProblemsByContest(@PathVariable Long contestId) {
        List<Problem> problems = problemService.getProblemsByContestId(contestId);
        return problems.stream()
                .filter(p -> Boolean.TRUE.equals(p.getActive()))
                .map(p -> new ProblemDTO(
                        p.getId(),
                        p.getTitle(),
                        p.getDescription(),
                        p.getInputFormat(),
                        p.getOutputFormat(),
                        p.getConstraints(),
                        p.getTimeLimit(),
                        p.getMemoryLimit(),
                        p.getActive(),
                        p.getContestId(),
                        p.getExample1(),
                        p.getExample2(),
                        p.getExample3(),
                        p.getImages()))
                .collect(java.util.stream.Collectors.toList());
    }

    @PostMapping("/contest/{contestId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Problem> createProblem(@PathVariable Long contestId, @RequestBody Problem problem) {
        return ResponseEntity.ok(problemService.createProblem(contestId, problem));
    }

    @GetMapping("/{problemId}/contest-status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ContestStatusDTO> getContestStatusByProblem(@PathVariable Long problemId) {
        try {
            // Use direct DB query (not cache) so disable takes effect instantly
            Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new com.example.codecombat2026.exception.ResourceNotFoundException("Problem not found"));
            boolean problemActive = Boolean.TRUE.equals(problem.getActive());
            Long contestId = problem.getContestId();

            if (contestId == null) {
                return ResponseEntity.ok(new ContestStatusDTO(false, false, null, null, null, problemActive));
            }

            try {
                com.example.codecombat2026.entity.Contest contest = contestService.getContestById(contestId);
                return ResponseEntity.ok(new ContestStatusDTO(
                        contest.getActive(), true,
                        contest.getName(), contest.getStartTime(), contest.getEndTime(),
                        problemActive));
            } catch (Exception e) {
                return ResponseEntity.ok(new ContestStatusDTO(false, false, null, null, null, problemActive));
            }
        } catch (Exception e) {
            return ResponseEntity.ok(new ContestStatusDTO(false, false, null, null, null, false));
        }
    }
}
