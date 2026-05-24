package com.example.codecombat2026.controller;

import com.example.codecombat2026.entity.Submission;
import com.example.codecombat2026.entity.User;
import com.example.codecombat2026.entity.UserPhoto;
import com.example.codecombat2026.repository.SubmissionRepository;
import com.example.codecombat2026.repository.UserRepository;
import com.example.codecombat2026.repository.UserPhotoRepository;
import com.example.codecombat2026.security.services.UserDetailsImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*", maxAge = 3600)
public class UserController {

    @Autowired private UserRepository userRepository;
    @Autowired private UserPhotoRepository userPhotoRepository;
    @Autowired private SubmissionRepository submissionRepository;
    @Autowired private StringRedisTemplate redis;
    @Autowired private ObjectMapper objectMapper;

    private static final Duration PROFILE_TTL = Duration.ofMinutes(5);

    // ─── GET /api/user/profile ────────────────────────────────────────────────
    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        String cacheKey = "profile:" + username;
        try {
            String cached = redis.opsForValue().get(cacheKey);
            if (cached != null) {
                return ResponseEntity.ok(objectMapper.readValue(cached, UserProfileResponse.class));
            }
        } catch (Exception ignored) {}

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.getEnabled()) {
            return ResponseEntity.status(403)
                    .body(java.util.Collections.singletonMap("message",
                            "ACCOUNT_DISABLED: Your account has been disabled. Please contact support@codecombat.live"));
        }

        String photoUrl = userPhotoRepository.findByUserId(user.getId())
                .map(UserPhoto::getPhotoUrl).orElse(null);

        UserProfileResponse profile = new UserProfileResponse(
                user.getId(), user.getUsername(), user.getEmail(),
                user.getFullName(), photoUrl);

        try {
            redis.opsForValue().set(cacheKey, objectMapper.writeValueAsString(profile), PROFILE_TTL);
        } catch (Exception ignored) {}

        return ResponseEntity.ok(profile);
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboardData(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        Long userId = userDetails.getId();
        String username = userDetails.getUsername();

        CompletableFuture<UserProfileResponse> profileFuture = CompletableFuture.supplyAsync(() -> {
            String cacheKey = "profile:" + username;
            try {
                String cached = redis.opsForValue().get(cacheKey);
                if (cached != null) {
                    return objectMapper.readValue(cached, UserProfileResponse.class);
                }
            } catch (Exception ignored) {}

            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            String photoUrl = userPhotoRepository.findByUserId(user.getId())
                    .map(UserPhoto::getPhotoUrl).orElse(null);
            UserProfileResponse profile = new UserProfileResponse(
                    user.getId(), user.getUsername(), user.getEmail(),
                    user.getFullName(), photoUrl);
            try {
                redis.opsForValue().set(cacheKey, objectMapper.writeValueAsString(profile), PROFILE_TTL);
            } catch (Exception ignored) {}
            return profile;
        });

        // Cache recent submissions per user (30s TTL, invalidated on new verdict)
        CompletableFuture<List<Submission>> submissionsFuture = CompletableFuture.supplyAsync(() -> {
            String subKey = "submissions:user:" + userId;
            try {
                String cached = redis.opsForValue().get(subKey);
                if (cached != null) {
                    return objectMapper.readValue(cached,
                        new com.fasterxml.jackson.core.type.TypeReference<List<Submission>>() {});
                }
            } catch (Exception ignored) {}

            List<Submission> subs = submissionRepository.findByUser_Id(userId);
            try {
                redis.opsForValue().set(subKey, objectMapper.writeValueAsString(subs),
                    java.time.Duration.ofSeconds(30));
            } catch (Exception ignored) {}
            return subs;
        });

        UserProfileResponse profile = profileFuture.join();
        List<Submission> submissions = submissionsFuture.join();

        List<Submission> recent = submissions.size() > 20
                ? submissions.subList(submissions.size() - 20, submissions.size())
                : submissions;

        Map<String, Object> response = new HashMap<>();
        response.put("profile", profile);
        response.put("submissions", recent);
        response.put("totalSubmissions", submissions.size());

        return ResponseEntity.ok(response);
    }

    // ─── PUT /api/user/profile ────────────────────────────────────────────────
    @PutMapping("/profile")
    public ResponseEntity<?> updateUserProfile(@RequestBody UpdateProfileRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            user.setEmail(request.getEmail());
        }
        if (request.getFullName() != null && !request.getFullName().isEmpty()) {
            user.setFullName(request.getFullName());
        }

        userRepository.save(user);

        try { redis.delete("profile:" + username); } catch (Exception ignored) {}

        String photoUrl = userPhotoRepository.findByUserId(user.getId())
                .map(UserPhoto::getPhotoUrl).orElse(null);

        return ResponseEntity.ok(new UserProfileResponse(
                user.getId(), user.getUsername(), user.getEmail(),
                user.getFullName(), photoUrl));
    }

    // ─── POST /api/user/profile/photo ─────────────────────────────────────────
    @PostMapping("/profile/photo")
    public ResponseEntity<?> uploadProfilePhoto(@RequestParam("photo") MultipartFile file) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("Please select a file to upload");
            }
            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest().body("File size must be less than 5MB");
            }
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body("Only image files are allowed");
            }

            String uploadDir = "uploads/profile-photos";
            File directory = new File(uploadDir);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".jpg";
            String filename = user.getId() + "_" + System.currentTimeMillis() + extension;
            Path filepath = Paths.get(uploadDir, filename);
            Files.write(filepath, file.getBytes());

            String photoUrl = "/uploads/profile-photos/" + filename;
            Optional<UserPhoto> existingPhoto = userPhotoRepository.findByUserId(user.getId());
            if (existingPhoto.isPresent()) {
                UserPhoto userPhoto = existingPhoto.get();
                userPhoto.setPhotoUrl(photoUrl);
                userPhotoRepository.save(userPhoto);
            } else {
                userPhotoRepository.save(new UserPhoto(user.getId(), photoUrl));
            }

            try { redis.delete("profile:" + username); } catch (Exception ignored) {}

            return ResponseEntity.ok(new PhotoUploadResponse(photoUrl, "Photo uploaded successfully"));

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Failed to upload photo: " + e.getMessage());
        }
    }

    // ─── Inner DTOs ───────────────────────────────────────────────────────────

    public static class UpdateProfileRequest {
        private String email;
        private String fullName;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
    }

    public static class PhotoUploadResponse {
        private String photoUrl;
        private String message;

        public PhotoUploadResponse(String photoUrl, String message) {
            this.photoUrl = photoUrl;
            this.message = message;
        }

        public String getPhotoUrl() { return photoUrl; }
        public String getMessage() { return message; }
    }

    public static class UserProfileResponse {
        private Long id;
        private String username;
        private String email;
        private String fullName;
        private String photoUrl;

        public UserProfileResponse() {}

        public UserProfileResponse(Long id, String username, String email,
                String fullName, String photoUrl) {
            this.id = id;
            this.username = username;
            this.email = email;
            this.fullName = fullName;
            this.photoUrl = photoUrl;
        }

        public Long getId() { return id; }
        public String getUsername() { return username; }
        public String getEmail() { return email; }
        public String getFullName() { return fullName; }
        public String getPhotoUrl() { return photoUrl; }
        public void setId(Long id) { this.id = id; }
        public void setUsername(String username) { this.username = username; }
        public void setEmail(String email) { this.email = email; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    }
}
