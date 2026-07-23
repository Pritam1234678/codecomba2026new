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

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body,
                                     @AuthenticationPrincipal UserDetailsImpl user) {
        Long problemId = toLong(body.get("problemId"));
        String language = (String) body.get("language");
        String code = (String) body.get("code");
        String explanation = (String) body.get("explanation");
        String imageUrl = (String) body.get("imageUrl");

        if (problemId == null || language == null || code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "problemId, language, and code are required"));
        }

        ProblemSolution.ProblemLanguages lang;
        try {
            lang = ProblemSolution.ProblemLanguages.valueOf(language.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid language: " + language));
        }

        String userName = user.getUsername();
        ProblemSolution s = service.create(problemId, user.getId(), userName, lang, code, explanation, imageUrl);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("id", s.getId());
        res.put("problemId", s.getProblemId());
        res.put("userId", s.getUserId());
        res.put("userName", s.getUserName());
        res.put("language", s.getLanguage().name());
        res.put("code", s.getCode());
        res.put("explanation", s.getExplanation());
        res.put("imageUrl", s.getImageUrl());
        res.put("createdAt", s.getCreatedAt());
        return ResponseEntity.ok(res);
    }

    @GetMapping("/{problemId}")
    public ResponseEntity<List<Map<String, Object>>> list(@PathVariable Long problemId) {
        List<ProblemSolution> solutions = service.getByProblem(problemId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (ProblemSolution s : solutions) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.getId());
            m.put("problemId", s.getProblemId());
            m.put("userId", s.getUserId());
            m.put("userName", s.getUserName());
            m.put("language", s.getLanguage().name());
            m.put("code", s.getCode());
            m.put("explanation", s.getExplanation());
            m.put("imageUrl", s.getImageUrl());
            m.put("createdAt", s.getCreatedAt());
            result.add(m);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{problemId}/count")
    public ResponseEntity<Map<String, Object>> count(@PathVariable Long problemId) {
        return ResponseEntity.ok(Map.of("count", service.countByProblem(problemId)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                     @RequestBody Map<String, Object> body,
                                     @AuthenticationPrincipal UserDetailsImpl user) {
        String language = (String) body.get("language");
        String code = (String) body.get("code");
        String explanation = (String) body.get("explanation");
        String imageUrl = (String) body.get("imageUrl");

        if (language == null || code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "language and code are required"));
        }
        ProblemSolution.ProblemLanguages lang;
        try {
            lang = ProblemSolution.ProblemLanguages.valueOf(language.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid language: " + language));
        }
        try {
            ProblemSolution s = service.update(id, user.getId(), lang, code, explanation, imageUrl);
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("id", s.getId());
            res.put("language", s.getLanguage().name());
            res.put("code", s.getCode());
            res.put("explanation", s.getExplanation());
            res.put("imageUrl", s.getImageUrl());
            return ResponseEntity.ok(res);
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

    private Long toLong(Object v) {
        if (v instanceof Number) return ((Number) v).longValue();
        if (v instanceof String) try { return Long.parseLong((String) v); } catch (Exception ignored) {}
        return null;
    }
}
