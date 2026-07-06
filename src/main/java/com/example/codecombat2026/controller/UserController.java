package com.example.codecombat2026.controller;

import com.example.codecombat2026.entity.Submission;
import com.example.codecombat2026.entity.User;
import com.example.codecombat2026.entity.UserPhoto;
import com.example.codecombat2026.repository.ContestRegistrationRepository;
import com.example.codecombat2026.repository.SubmissionRepository;
import com.example.codecombat2026.repository.UserPhotoRepository;
import com.example.codecombat2026.repository.UserRepository;
import com.example.codecombat2026.security.services.UserDetailsImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired private UserRepository userRepository;
    @Autowired private UserPhotoRepository userPhotoRepository;
    @Autowired private SubmissionRepository submissionRepository;
    @Autowired private ContestRegistrationRepository contestRegistrationRepository;
    @Autowired private StringRedisTemplate redis;
    @Autowired private ObjectMapper objectMapper;

    /** Resolve a stored photo URL to a full URL if APP_BASE_URL is configured. */
    private String resolvePhotoUrl(String photoUrl) {
        if (photoUrl == null) return null;
        if (photoUrl.startsWith("http")) return photoUrl; // already absolute
        String baseUrl = System.getenv("APP_BASE_URL");
        if (baseUrl != null && !baseUrl.isBlank()) {
            return baseUrl + photoUrl;
        }
        return photoUrl;
    }

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

        String photoUrl = resolvePhotoUrl(userPhotoRepository.findByUserId(user.getId())
                .map(UserPhoto::getPhotoUrl).orElse(null));

        UserProfileResponse profile = new UserProfileResponse(
                user.getId(), user.getUsername(), user.getEmail(),
                user.getFullName(), photoUrl,
                user.getDisplayName(), user.getBio(), user.getTitle(),
                user.getLocation(), user.getCompany(),
                user.getGithubUrl(), user.getLinkedinUrl(), user.getInstagramUrl(),
                user.getTwitterUrl(), user.getWebsiteUrl());

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
            String photoUrl = resolvePhotoUrl(userPhotoRepository.findByUserId(user.getId())
                    .map(UserPhoto::getPhotoUrl).orElse(null));
            UserProfileResponse profile = new UserProfileResponse(
                    user.getId(), user.getUsername(), user.getEmail(),
                    user.getFullName(), photoUrl,
                    user.getDisplayName(), user.getBio(), user.getTitle(),
                    user.getLocation(), user.getCompany(),
                    user.getGithubUrl(), user.getLinkedinUrl(), user.getInstagramUrl(),
                    user.getTwitterUrl(), user.getWebsiteUrl());
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

        // Include total points for the stat card
        User userEntity = userRepository.findById(userId).orElse(null);
        response.put("totalPoints", userEntity != null ? userEntity.getTotalPoints() : 0);

        return ResponseEntity.ok(response);
    }

    // ─── PUT /api/user/profile ────────────────────────────────────────────────
    @PutMapping("/profile")
    public ResponseEntity<?> updateUserProfile(@RequestBody EditProfileRequest request) {
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
        // Social / extended fields — all optional
        if (request.getBio() != null)          user.setBio(request.getBio().orElse(null));
        if (request.getTitle() != null)        user.setTitle(request.getTitle().orElse(null));
        if (request.getLocation() != null)     user.setLocation(request.getLocation().orElse(null));
        if (request.getCompany() != null)      user.setCompany(request.getCompany().orElse(null));
        if (request.getGithubUrl() != null)    user.setGithubUrl(request.getGithubUrl().orElse(null));
        if (request.getLinkedinUrl() != null)  user.setLinkedinUrl(request.getLinkedinUrl().orElse(null));
        if (request.getInstagramUrl() != null) user.setInstagramUrl(request.getInstagramUrl().orElse(null));
        if (request.getTwitterUrl() != null)   user.setTwitterUrl(request.getTwitterUrl().orElse(null));
        if (request.getWebsiteUrl() != null)   user.setWebsiteUrl(request.getWebsiteUrl().orElse(null));

        userRepository.save(user);

        try { redis.delete("profile:" + username); } catch (Exception ignored) {}
        try { redis.delete("public-profile:" + username); } catch (Exception ignored) {}

        String photoUrl = resolvePhotoUrl(userPhotoRepository.findByUserId(user.getId())
                .map(UserPhoto::getPhotoUrl).orElse(null));

        return ResponseEntity.ok(new UserProfileResponse(
                user.getId(), user.getUsername(), user.getEmail(),
                user.getFullName(), photoUrl,
                user.getDisplayName(), user.getBio(), user.getTitle(),
                user.getLocation(), user.getCompany(),
                user.getGithubUrl(), user.getLinkedinUrl(), user.getInstagramUrl(),
                user.getTwitterUrl(), user.getWebsiteUrl()));
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

            // For cross-origin setups (frontend on Vercel, backend on separate domain),
            // store the full URL so the frontend can render it directly.
            String baseUrl = System.getenv("APP_BASE_URL");
            if (baseUrl != null && !baseUrl.isBlank()) {
                photoUrl = baseUrl + photoUrl;
            }
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

    // ─── GET /api/user/search?q=...&page=0&size=10 ───────────────────────────
    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> searchUsers(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        size = Math.min(size, 20); // cap at 20
        Page<User> result = userRepository.searchNonAdminUsers(
                q.trim(), PageRequest.of(page, size));

        List<Map<String, Object>> users = result.getContent().stream().map(u -> {
            String photoUrl = resolvePhotoUrl(userPhotoRepository.findByUserId(u.getId())
                    .map(UserPhoto::getPhotoUrl).orElse(null));
            Map<String, Object> m = new HashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("fullName", u.getFullName());
            m.put("totalPoints", u.getTotalPoints());
            m.put("photoUrl", photoUrl);
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("content", users);
        response.put("totalElements", result.getTotalElements());
        response.put("totalPages", result.getTotalPages());
        response.put("page", page);
        response.put("size", size);
        return ResponseEntity.ok(response);
    }

    // ─── GET /api/user/profile/{username} — public profile ───────────────────
    @GetMapping("/profile/{username}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getPublicProfile(@PathVariable String username) {
        String cacheKey = "public-profile:" + username;
        try {
            String cached = redis.opsForValue().get(cacheKey);
            if (cached != null) {
                return ResponseEntity.ok(objectMapper.readValue(cached, Map.class));
            }
        } catch (Exception ignored) {}

        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null || !user.getEnabled()) {
            return ResponseEntity.notFound().build();
        }
        // Block admin profiles from being viewed
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(r -> r.getName().name().equals("ROLE_ADMIN"));
        if (isAdmin) {
            return ResponseEntity.notFound().build();
        }

        String photoUrl = resolvePhotoUrl(userPhotoRepository.findByUserId(user.getId())
                .map(UserPhoto::getPhotoUrl).orElse(null));

        // Stats — all computed in parallel
        CompletableFuture<List<Submission>> subsFuture = CompletableFuture.supplyAsync(
                () -> submissionRepository.findByUser_Id(user.getId()));
        CompletableFuture<Long> contestRegFuture = CompletableFuture.supplyAsync(
                () -> contestRegistrationRepository.countByUserId(user.getId()));

        List<Submission> subs = subsFuture.join();
        long contestsJoined = contestRegFuture.join();

        long totalSubmissions = subs.size();
        long acceptedSubmissions = subs.stream().filter(s -> "AC".equals(s.getStatus() != null ? s.getStatus().name() : "")).count();
        long problemsSolved = subs.stream().filter(s -> "AC".equals(s.getStatus() != null ? s.getStatus().name() : ""))
                .map(s -> s.getProblem() != null ? s.getProblem().getId() : null)
                .filter(id -> id != null)
                .distinct().count();
        double successRate = totalSubmissions > 0 ? (acceptedSubmissions * 100.0 / totalSubmissions) : 0;

        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("username", user.getUsername());
        response.put("fullName", user.getFullName());
        response.put("photoUrl", photoUrl);
        response.put("totalPoints", user.getTotalPoints());
        response.put("totalSubmissions", totalSubmissions);
        response.put("acceptedSubmissions", acceptedSubmissions);
        response.put("problemsSolved", problemsSolved);
        response.put("successRate", Math.round(successRate * 10.0) / 10.0);
        response.put("contestsJoined", contestsJoined);
        response.put("bio", user.getBio());
        response.put("title", user.getTitle());
        response.put("location", user.getLocation());
        response.put("company", user.getCompany());
        response.put("githubUrl", user.getGithubUrl());
        response.put("linkedinUrl", user.getLinkedinUrl());
        response.put("instagramUrl", user.getInstagramUrl());
        response.put("twitterUrl", user.getTwitterUrl());
        response.put("websiteUrl", user.getWebsiteUrl());

        try {
            redis.opsForValue().set(cacheKey, objectMapper.writeValueAsString(response), PROFILE_TTL);
        } catch (Exception ignored) {}

        return ResponseEntity.ok(response);
    }

    // ─── Inner DTOs ───────────────────────────────────────────────────────────

    public static class EditProfileRequest {
        private String email;
        private String fullName;
        private java.util.Optional<String> bio;
        private java.util.Optional<String> title;
        private java.util.Optional<String> location;
        private java.util.Optional<String> company;
        private java.util.Optional<String> githubUrl;
        private java.util.Optional<String> linkedinUrl;
        private java.util.Optional<String> instagramUrl;
        private java.util.Optional<String> twitterUrl;
        private java.util.Optional<String> websiteUrl;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public java.util.Optional<String> getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = java.util.Optional.ofNullable(displayName); }
        public java.util.Optional<String> getBio() { return bio; }
        public void setBio(String bio) { this.bio = java.util.Optional.ofNullable(bio); }
        public java.util.Optional<String> getTitle() { return title; }
        public void setTitle(String title) { this.title = java.util.Optional.ofNullable(title); }
        public java.util.Optional<String> getLocation() { return location; }
        public void setLocation(String location) { this.location = java.util.Optional.ofNullable(location); }
        public java.util.Optional<String> getCompany() { return company; }
        public void setCompany(String company) { this.company = java.util.Optional.ofNullable(company); }
        public java.util.Optional<String> getGithubUrl() { return githubUrl; }
        public void setGithubUrl(String githubUrl) { this.githubUrl = java.util.Optional.ofNullable(githubUrl); }
        public java.util.Optional<String> getLinkedinUrl() { return linkedinUrl; }
        public void setLinkedinUrl(String linkedinUrl) { this.linkedinUrl = java.util.Optional.ofNullable(linkedinUrl); }
        public java.util.Optional<String> getInstagramUrl() { return instagramUrl; }
        public void setInstagramUrl(String instagramUrl) { this.instagramUrl = java.util.Optional.ofNullable(instagramUrl); }
        public java.util.Optional<String> getTwitterUrl() { return twitterUrl; }
        public void setTwitterUrl(String twitterUrl) { this.twitterUrl = java.util.Optional.ofNullable(twitterUrl); }
        public java.util.Optional<String> getWebsiteUrl() { return websiteUrl; }
        public void setWebsiteUrl(String websiteUrl) { this.websiteUrl = java.util.Optional.ofNullable(websiteUrl); }
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
        // Social + extended fields
        private String bio;
        private String title;
        private String location;
        private String company;
        private String githubUrl;
        private String linkedinUrl;
        private String instagramUrl;
        private String twitterUrl;
        private String websiteUrl;

        public UserProfileResponse() {}

        public UserProfileResponse(Long id, String username, String email,
                String fullName, String photoUrl) {
            this.id = id;
            this.username = username;
            this.email = email;
            this.fullName = fullName;
            this.photoUrl = photoUrl;
        }

        /** Full-constructor with socials. */
        public UserProfileResponse(Long id, String username, String email,
                String fullName, String photoUrl,
                String bio, String title,
                String location, String company,
                String githubUrl, String linkedinUrl, String instagramUrl,
                String twitterUrl, String websiteUrl) {
            this.id = id;
            this.username = username;
            this.email = email;
            this.fullName = fullName;
            this.photoUrl = photoUrl;
            this.bio = bio;
            this.title = title;
            this.location = location;
            this.company = company;
            this.githubUrl = githubUrl;
            this.linkedinUrl = linkedinUrl;
            this.instagramUrl = instagramUrl;
            this.twitterUrl = twitterUrl;
            this.websiteUrl = websiteUrl;
        }

        public Long getId() { return id; }
        public String getUsername() { return username; }
        public String getEmail() { return email; }
        public String getFullName() { return fullName; }
        public String getPhotoUrl() { return photoUrl; }
        public String getDisplayName() { return displayName; }
        public String getBio() { return bio; }
        public String getTitle() { return title; }
        public String getLocation() { return location; }
        public String getCompany() { return company; }
        public String getGithubUrl() { return githubUrl; }
        public String getLinkedinUrl() { return linkedinUrl; }
        public String getInstagramUrl() { return instagramUrl; }
        public String getTwitterUrl() { return twitterUrl; }
        public String getWebsiteUrl() { return websiteUrl; }
        public void setId(Long id) { this.id = id; }
        public void setUsername(String username) { this.username = username; }
        public void setEmail(String email) { this.email = email; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
        public void setBio(String bio) { this.bio = bio; }
        public void setTitle(String title) { this.title = title; }
        public void setLocation(String location) { this.location = location; }
        public void setCompany(String company) { this.company = company; }
        public void setGithubUrl(String githubUrl) { this.githubUrl = githubUrl; }
        public void setLinkedinUrl(String linkedinUrl) { this.linkedinUrl = linkedinUrl; }
        public void setInstagramUrl(String instagramUrl) { this.instagramUrl = instagramUrl; }
        public void setTwitterUrl(String twitterUrl) { this.twitterUrl = twitterUrl; }
        public void setWebsiteUrl(String websiteUrl) { this.websiteUrl = websiteUrl; }
    }
}
rl) { this.websiteUrl = websiteUrl; }
    }
}
}
}
}
    }
}
rl; }
    }
}
}
}
}
    }
}

}
