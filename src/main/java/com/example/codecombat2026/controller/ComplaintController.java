package com.example.codecombat2026.controller;

import com.example.codecombat2026.entity.ProblemComplaint;
import com.example.codecombat2026.repository.ProblemComplaintRepository;
import com.example.codecombat2026.security.services.UserDetailsImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.*;

@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {

    @Autowired private ProblemComplaintRepository complaintRepo;
    @Autowired private StringRedisTemplate redis;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbc;

    // ── Create ──────────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> create(@RequestBody CreateRequest req, @AuthenticationPrincipal UserDetailsImpl user) {
        if (req.complaintType == null || req.complaintType.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Complaint type is required"));
        }
        if (req.message == null || req.message.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Message is required"));
        }
        ProblemComplaint c = new ProblemComplaint();
        c.setUserId(user.getId());
        c.setProblemId(req.problemId);
        c.setContestId(req.contestId);
        c.setComplaintType(req.complaintType);
        c.setMessage(req.message);
        c.setStatus("PENDING");
        complaintRepo.save(c);
        redis.delete("complaints:admin:*");
        redis.delete("complaints:user:" + user.getId());
        return ResponseEntity.ok(Map.of("message", "Complaint submitted"));
    }

    // ── User: my complaints + responses ─────────────────────────────────────

    @GetMapping("/mine")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> myComplaints(@AuthenticationPrincipal UserDetailsImpl user,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "10") int size) {
        String cacheKey = "complaints:user:" + user.getId() + ":p" + page + "s" + size;
        try {
            String cached = redis.opsForValue().get(cacheKey);
            if (cached != null) return ResponseEntity.ok(objectMapper.readValue(cached, Map.class));
        } catch (Exception ignored) {}

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ProblemComplaint> result = complaintRepo.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);
        Map<String, Object> resp = buildPageResponse(result);
        try { redis.opsForValue().set(cacheKey, objectMapper.writeValueAsString(resp), Duration.ofMinutes(2)); } catch (Exception ignored) {}
        return ResponseEntity.ok(resp);
    }

    // ── Admin: paginated list with user info ────────────────────────────────

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> all(@RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "20") int size,
                                 @RequestParam(required = false) String status) {
        String cacheKey = "complaints:admin:" + page + ":" + size + ":" + (status != null ? status : "ALL");
        try {
            String cached = redis.opsForValue().get(cacheKey);
            if (cached != null) return ResponseEntity.ok(objectMapper.readValue(cached, Map.class));
        } catch (Exception ignored) {}

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ProblemComplaint> result;
        if (status != null && !status.equals("ALL")) {
            result = complaintRepo.findByStatusOrderByCreatedAtDesc(status, pageable);
        } else {
            result = complaintRepo.findAllByOrderByCreatedAtDesc(pageable);
        }

        // Enrich with user info
        List<Map<String, Object>> enriched = new ArrayList<>();
        for (ProblemComplaint c : result.getContent()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("userId", c.getUserId());
            m.put("problemId", c.getProblemId());
            m.put("contestId", c.getContestId());
            m.put("complaintType", c.getComplaintType());
            m.put("message", c.getMessage());
            m.put("status", c.getStatus());
            m.put("adminResponse", c.getAdminResponse());
            m.put("createdAt", c.getCreatedAt() != null ? c.getCreatedAt().toString() : null);
            // Fetch user info
            try {
                Map<String, Object> userRow = jdbc.queryForMap(
                    "SELECT username, email FROM users WHERE id = ?", c.getUserId());
                m.put("username", userRow.get("username"));
                m.put("email", userRow.get("email"));
            } catch (Exception e) {
                m.put("username", "Unknown");
                m.put("email", "");
            }
            enriched.add(m);
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("complaints", enriched);
        resp.put("total", result.getTotalElements());
        resp.put("page", page);
        resp.put("size", size);
        resp.put("totalPages", result.getTotalPages());

        try { redis.opsForValue().set(cacheKey, objectMapper.writeValueAsString(resp), Duration.ofMinutes(1)); } catch (Exception ignored) {}
        return ResponseEntity.ok(resp);
    }

    // ── Resolve ─────────────────────────────────────────────────────────────

    @PutMapping("/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> resolve(@PathVariable Long id, @RequestBody ResolveRequest req) {
        ProblemComplaint c = complaintRepo.findById(id).orElse(null);
        if (c == null) return ResponseEntity.notFound().build();
        c.setStatus("RESOLVED");
        c.setAdminResponse(req.response);
        complaintRepo.save(c);
        redis.delete(redis.keys("complaints:*") != null ? redis.keys("complaints:*") : Set.of());
        return ResponseEntity.ok(Map.of("message", "Resolved"));
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private Map<String, Object> buildPageResponse(Page<ProblemComplaint> page) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("complaints", page.getContent());
        resp.put("total", page.getTotalElements());
        resp.put("page", page.getNumber());
        resp.put("size", page.getSize());
        resp.put("totalPages", page.getTotalPages());
        return resp;
    }

    // ── DTOs ────────────────────────────────────────────────────────────────

    public static class CreateRequest {
        public Long problemId;
        public Long contestId;
        public String complaintType;
        public String message;
    }

    public static class ResolveRequest {
        public String response;
    }
}
