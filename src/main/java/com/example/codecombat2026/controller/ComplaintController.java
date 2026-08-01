package com.example.codecombat2026.controller;

import com.example.codecombat2026.entity.ProblemComplaint;
import com.example.codecombat2026.repository.ProblemComplaintRepository;
import com.example.codecombat2026.security.services.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {

    @Autowired
    private ProblemComplaintRepository complaintRepo;

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
        return ResponseEntity.ok(Map.of("message", "Complaint submitted"));
    }

    @GetMapping("/mine")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> myComplaints(@AuthenticationPrincipal UserDetailsImpl user) {
        return ResponseEntity.ok(complaintRepo.findByUserIdOrderByCreatedAtDesc(user.getId()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> all() {
        return ResponseEntity.ok(complaintRepo.findAllByOrderByCreatedAtDesc());
    }

    @PutMapping("/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> resolve(@PathVariable Long id, @RequestBody ResolveRequest req) {
        ProblemComplaint c = complaintRepo.findById(id).orElse(null);
        if (c == null) return ResponseEntity.notFound().build();
        c.setStatus("RESOLVED");
        c.setAdminResponse(req.response);
        complaintRepo.save(c);
        return ResponseEntity.ok(Map.of("message", "Resolved"));
    }

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
