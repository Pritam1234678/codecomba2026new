package com.example.codecombat2026.controller;

import com.example.codecombat2026.entity.ProblemSolution;
import com.example.codecombat2026.security.services.UserDetailsImpl;
import com.example.codecombat2026.service.ProblemSolutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/practice/solutions")
@PreAuthorize("isAuthenticated()")
public class ProblemSolutionController {

    @Autowired
    private ProblemSolutionService service;

    @SuppressWarnings("unchecked")
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body,
                                     @AuthenticationPrincipal UserDetailsImpl user) {
        Long problemId = toLong(body.get("problemId"));
        Map<String, String> codes = (Map<String, String>) body.get("codes");
        String explanation = (String) body.get("explanation");
        String imageUrl = (String) body.get("imageUrl");

        if (problemId == null || codes == null || codes.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "problemId and codes (language→code map) are required"));
        }

        String userName = user.getUsername();
        ProblemSolution s = service.create(problemId, user.getId(), userName, codes, explanation, imageUrl);

        return ResponseEntity.ok(toResponse(s));
    }

    @GetMapping("/{problemId}")
    public ResponseEntity<List<Map<String, Object>>> list(@PathVariable Long problemId) {
        List<ProblemSolution> solutions = service.getByProblem(problemId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (ProblemSolution s : solutions) {
            result.add(toResponse(s));
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{problemId}/count")
    public ResponseEntity<Map<String, Object>> count(@PathVariable Long problemId) {
        return ResponseEntity.ok(Map.of("count", service.countByProblem(problemId)));
    }

    @SuppressWarnings("unchecked")
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                     @RequestBody Map<String, Object> body,
                                     @AuthenticationPrincipal UserDetailsImpl user) {
        Map<String, String> codes = (Map<String, String>) body.get("codes");
        String explanation = (String) body.get("explanation");
        String imageUrl = (String) body.get("imageUrl");

        if (codes == null || codes.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "codes (language→code map) are required"));
        }
        try {
            ProblemSolution s = service.update(id, user.getId(), codes, explanation, imageUrl);
            return ResponseEntity.ok(toResponse(s));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id,
                                     @AuthenticationPrincipal UserDetailsImpl user) {
        try {
            service.delete(id, user.getId());
            return ResponseEntity.ok(Map.of("message", "Solution deleted"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private Map<String, Object> toResponse(ProblemSolution s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("problemId", s.getProblemId());
        m.put("userId", s.getUserId());
        m.put("userName", s.getUserName());
        m.put("codes", s.getCodesMap());
        m.put("explanation", s.getExplanation());
        m.put("imageUrl", s.getImageUrl());
        m.put("createdAt", s.getCreatedAt());
        return m;
    }

    private Long toLong(Object v) {
        if (v instanceof Number) return ((Number) v).longValue();
        if (v instanceof String) try { return Long.parseLong((String) v); } catch (Exception ignored) {}
        return null;
    }
}
