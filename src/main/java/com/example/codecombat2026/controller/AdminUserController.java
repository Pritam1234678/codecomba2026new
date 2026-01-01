package com.example.codecombat2026.controller;

import com.example.codecombat2026.dto.MessageResponse;
import com.example.codecombat2026.entity.User;
import com.example.codecombat2026.repository.UserRepository;
import com.example.codecombat2026.security.services.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
@CrossOrigin(origins = "*", maxAge = 3600)
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.example.codecombat2026.repository.SubmissionRepository submissionRepository;

    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @GetMapping("/stats")
    public Map<String, Long> getUserStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("total", userRepository.count());
        stats.put("enabled", userRepository.countByEnabled(true));
        stats.put("disabled", userRepository.countByEnabled(false));
        return stats;
    }

    @PutMapping("/{id}/enable")
    public ResponseEntity<?> enableUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setEnabled(true);
        userRepository.save(user);
        return ResponseEntity.ok(new MessageResponse("User enabled successfully"));
    }

    @PutMapping("/{id}/disable")
    public ResponseEntity<?> disableUser(@PathVariable Long id, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Prevent admin from disabling themselves
        if (currentUser.getId().equals(id)) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("You cannot disable your own account"));
        }

        user.setEnabled(false);
        userRepository.save(user);
        return ResponseEntity.ok(new MessageResponse("User disabled successfully"));
    }

    @DeleteMapping("/{id}")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> deleteUser(@PathVariable Long id, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Prevent admin from deleting themselves
        if (currentUser.getId().equals(id)) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("You cannot delete your own account"));
        }

        // Delete all submissions by this user first
        submissionRepository.deleteByUser_Id(id);

        // Then delete the user
        userRepository.delete(user);

        return ResponseEntity.ok(new MessageResponse("User and all their submissions deleted successfully"));
    }
}
