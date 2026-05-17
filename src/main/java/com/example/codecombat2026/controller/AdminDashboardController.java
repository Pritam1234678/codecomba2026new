package com.example.codecombat2026.controller;

import com.example.codecombat2026.entity.User;
import com.example.codecombat2026.entity.UserPhoto;
import com.example.codecombat2026.repository.ContestRepository;
import com.example.codecombat2026.repository.UserPhotoRepository;
import com.example.codecombat2026.repository.UserRepository;
import com.example.codecombat2026.security.services.UserDetailsImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Combined admin dashboard endpoint.
 * Returns profile + userStats + contestStats in ONE request.
 * Stats cached in Valkey for 2 minutes.
 * Profile uses same cache key/format as UserController (no type mismatch).
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*", maxAge = 3600)
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private static final Logger log = LoggerFactory.getLogger(AdminDashboardController.class);

    // Public so ConnectionWarmupConfig can reference it
    public static final String STATS_CACHE_KEY = "admin:dashboard:stats";
    private static final Duration STATS_TTL    = Duration.ofMinutes(2);
    private static final Duration PROFILE_TTL  = Duration.ofMinutes(5);

    @Autowired private UserRepository userRepository;
    @Autowired private ContestRepository contestRepository;
    @Autowired private UserPhotoRepository userPhotoRepository;
    @Autowired private StringRedisTemplate redis;
    @Autowired private ObjectMapper objectMapper;

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboardData(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        String username = userDetails.getUsername();

        // Fetch stats (Valkey cached) and profile (Valkey cached) in parallel
        CompletableFuture<Map<String, Object>> statsFuture =
                CompletableFuture.supplyAsync(this::getCachedStats);

        CompletableFuture<UserController.UserProfileResponse> profileFuture =
                CompletableFuture.supplyAsync(() -> getProfile(username));

        Map<String, Object> stats   = statsFuture.join();
        UserController.UserProfileResponse profile = profileFuture.join();

        Map<String, Object> response = new HashMap<>();
        response.put("userStats",    stats.get("userStats"));
        response.put("contestStats", stats.get("contestStats"));
        response.put("profile",      profile);

        return ResponseEntity.ok(response);
    }

    // ─── Stats cache ──────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public Map<String, Object> getCachedStats() {
        try {
            String cached = redis.opsForValue().get(STATS_CACHE_KEY);
            if (cached != null) {
                return objectMapper.readValue(cached, Map.class);
            }
        } catch (Exception ignored) {}

        // Cache miss — fetch both counts in parallel
        CompletableFuture<Map<String, Long>> userFuture = CompletableFuture.supplyAsync(() -> {
            Map<String, Long> s = new HashMap<>();
            s.put("total",    userRepository.count());
            s.put("enabled",  userRepository.countByEnabled(true));
            s.put("disabled", userRepository.countByEnabled(false));
            return s;
        });

        CompletableFuture<Map<String, Long>> contestFuture = CompletableFuture.supplyAsync(() -> {
            Map<String, Long> s = new HashMap<>();
            s.put("total",    contestRepository.count());
            s.put("active",   contestRepository.countByActive(true));
            s.put("inactive", contestRepository.countByActive(false));
            return s;
        });

        Map<String, Object> result = new HashMap<>();
        result.put("userStats",    userFuture.join());
        result.put("contestStats", contestFuture.join());

        try {
            redis.opsForValue().set(STATS_CACHE_KEY,
                objectMapper.writeValueAsString(result), STATS_TTL);
        } catch (Exception ignored) {}

        return result;
    }

    /**
     * Get profile using the SAME format as UserController.UserProfileResponse.
     * This ensures both controllers share the same Valkey cache key without
     * type mismatch (previously AdminDashboardController wrote Map, UserController
     * read UserProfileResponse → Jackson deserialization always failed silently).
     */
    private UserController.UserProfileResponse getProfile(String username) {
        String cacheKey = "profile:" + username;

        // Try Valkey cache — same key/type as UserController
        try {
            String cached = redis.opsForValue().get(cacheKey);
            if (cached != null) {
                return objectMapper.readValue(cached, UserController.UserProfileResponse.class);
            }
        } catch (Exception ignored) {}

        // Cache miss — fetch from MySQL
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String photoUrl = userPhotoRepository.findByUserId(user.getId())
                .map(UserPhoto::getPhotoUrl).orElse(null);

        UserController.UserProfileResponse profile = new UserController.UserProfileResponse(
                user.getId(), user.getUsername(), user.getEmail(),
                user.getFullName(), user.getRollNumber(), user.getBranch(),
                user.getPhoneNumber(), photoUrl);

        // Cache using same format as UserController
        try {
            redis.opsForValue().set(cacheKey,
                objectMapper.writeValueAsString(profile), PROFILE_TTL);
        } catch (Exception ignored) {}

        return profile;
    }

    /** Invalidate stats cache — called after user/contest mutations */
    public void invalidateStatsCache() {
        try { redis.delete(STATS_CACHE_KEY); } catch (Exception ignored) {}
    }
}
