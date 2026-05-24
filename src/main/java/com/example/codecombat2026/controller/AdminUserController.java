package com.example.codecombat2026.controller;

import com.example.codecombat2026.dto.MessageResponse;
import com.example.codecombat2026.entity.User;
import com.example.codecombat2026.repository.UserRepository;
import com.example.codecombat2026.repository.PasswordResetTokenRepository;
import com.example.codecombat2026.security.services.UserDetailsImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
@CrossOrigin(origins = "*", maxAge = 3600)
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private static final String ADMIN_USERS_KEY = "admin:users:all";
    private static final Duration ADMIN_USERS_TTL = Duration.ofSeconds(60);

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordResetTokenRepository passwordResetTokenRepository;
    @Autowired private com.example.codecombat2026.repository.SubmissionRepository submissionRepository;
    @Autowired private AdminDashboardController adminDashboardController;
    @Autowired private StringRedisTemplate redis;
    @Autowired private ObjectMapper objectMapper;

    @GetMapping
    public List<User> getAllUsers() {
        try {
            String cached = redis.opsForValue().get(ADMIN_USERS_KEY);
            if (cached != null) {
                return objectMapper.readValue(cached, new TypeReference<List<User>>() {});
            }
        } catch (Exception ignored) {}

        List<User> users = userRepository.findAll();
        try {
            redis.opsForValue().set(ADMIN_USERS_KEY,
                objectMapper.writeValueAsString(users), ADMIN_USERS_TTL);
        } catch (Exception ignored) {}
        return users;
    }

    @GetMapping("/stats")
    public Map<String, Long> getUserStats() {
        // Reuse the admin dashboard stats cache
        @SuppressWarnings("unchecked")
        Map<String, Object> stats = adminDashboardController.getCachedStats();
        @SuppressWarnings("unchecked")
        Map<String, Long> userStats = (Map<String, Long>) stats.get("userStats");
        if (userStats != null) return userStats;

        // Fallback
        Map<String, Long> result = new HashMap<>();
        result.put("total", userRepository.count());
        result.put("enabled", userRepository.countByEnabled(true));
        result.put("disabled", userRepository.countByEnabled(false));
        return result;
    }

    @PutMapping("/{id}/enable")
    public ResponseEntity<?> enableUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setEnabled(true);
        userRepository.save(user);
        evictAdminUsersCache();
        adminDashboardController.invalidateStatsCache();
        return ResponseEntity.ok(new MessageResponse("User enabled successfully"));
    }

    @PutMapping("/{id}/disable")
    public ResponseEntity<?> disableUser(@PathVariable Long id, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (currentUser.getId().equals(id)) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("You cannot disable your own account"));
        }

        user.setEnabled(false);
        userRepository.save(user);
        evictAdminUsersCache();
        adminDashboardController.invalidateStatsCache();
        return ResponseEntity.ok(new MessageResponse("User disabled successfully"));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteUser(@PathVariable Long id, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (currentUser.getId().equals(id)) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("You cannot delete your own account"));
        }

        passwordResetTokenRepository.deleteByUser(user);
        submissionRepository.deleteByUser_Id(id);
        userRepository.delete(user);
        evictAdminUsersCache();
        adminDashboardController.invalidateStatsCache();
        return ResponseEntity.ok(new MessageResponse("User and all their data deleted successfully"));
    }

    public void evictAdminUsersCache() {
        try { redis.delete(ADMIN_USERS_KEY); } catch (Exception ignored) {}
    }
}
