package com.example.codecombat2026.controller;

import com.example.codecombat2026.dto.MessageResponse;
import com.example.codecombat2026.entity.Contest;
import com.example.codecombat2026.repository.ContestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/contests")
@CrossOrigin(origins = "*", maxAge = 3600)
@PreAuthorize("hasRole('ADMIN')")
public class AdminContestController {

    @Autowired
    private ContestRepository contestRepository;

    @GetMapping
    public List<Contest> getAllContests() {
        return contestRepository.findAll();
    }

    @GetMapping("/stats")
    public Map<String, Long> getContestStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("total", contestRepository.count());
        stats.put("active", contestRepository.countByActive(true));
        stats.put("inactive", contestRepository.countByActive(false));
        return stats;
    }

    @PostMapping
    public Contest createContest(@RequestBody Contest contest) {
        if (contest.getActive() == null) {
            contest.setActive(false);
        }
        return contestRepository.save(contest);
    }

    @PutMapping("/{id}")
    public Contest updateContest(@PathVariable Long id, @RequestBody Contest contestDetails) {
        Contest contest = contestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contest not found"));

        contest.setName(contestDetails.getName());
        contest.setDescription(contestDetails.getDescription());
        contest.setStartTime(contestDetails.getStartTime());
        contest.setEndTime(contestDetails.getEndTime());
        // Don't set status manually - let @PreUpdate calculate it automatically
        contest.setActive(contestDetails.getActive());

        return contestRepository.save(contest);
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<?> activateContest(@PathVariable Long id) {
        Contest contest = contestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contest not found"));
        contest.setActive(true);
        contestRepository.save(contest);
        return ResponseEntity.ok(new MessageResponse("Contest activated successfully"));
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<?> deactivateContest(@PathVariable Long id) {
        Contest contest = contestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contest not found"));
        contest.setActive(false);
        contestRepository.save(contest);
        return ResponseEntity.ok(new MessageResponse("Contest deactivated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteContest(@PathVariable Long id) {
        contestRepository.deleteById(id);
        return ResponseEntity.ok(new MessageResponse("Contest deleted successfully"));
    }
}
