package com.example.codecombat2026.controller;

import com.example.codecombat2026.entity.User;
import com.example.codecombat2026.entity.UserPhoto;
import com.example.codecombat2026.repository.UserRepository;
import com.example.codecombat2026.repository.UserPhotoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*", maxAge = 3600)
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserPhotoRepository userPhotoRepository;

    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Get photo URL if exists
        String photoUrl = userPhotoRepository.findByUserId(user.getId())
                .map(UserPhoto::getPhotoUrl)
                .orElse(null);

        // Return user profile data
        return ResponseEntity.ok(new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRollNumber(),
                user.getBranch(),
                user.getPhoneNumber(),
                photoUrl));
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateUserProfile(@RequestBody UpdateProfileRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update user fields
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            user.setEmail(request.getEmail());
        }
        if (request.getFullName() != null && !request.getFullName().isEmpty()) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getRollNumber() != null) {
            user.setRollNumber(request.getRollNumber());
        }
        if (request.getBranch() != null) {
            user.setBranch(request.getBranch());
        }

        userRepository.save(user);

        // Get photo URL
        String photoUrl = userPhotoRepository.findByUserId(user.getId())
                .map(UserPhoto::getPhotoUrl)
                .orElse(null);

        return ResponseEntity.ok(new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRollNumber(),
                user.getBranch(),
                user.getPhoneNumber(),
                photoUrl));
    }

    @PostMapping("/profile/photo")
    public ResponseEntity<?> uploadProfilePhoto(@RequestParam("photo") MultipartFile file) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("Please select a file to upload");
            }

            // Check file size (max 5MB)
            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest().body("File size must be less than 5MB");
            }

            // Check file type
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body("Only image files are allowed");
            }

            // Create uploads directory if not exists
            String uploadDir = "uploads/profile-photos";
            File directory = new File(uploadDir);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".jpg";
            String filename = user.getId() + "_" + System.currentTimeMillis() + extension;
            Path filepath = Paths.get(uploadDir, filename);

            // Save file
            Files.write(filepath, file.getBytes());

            // Save or update photo URL in database
            String photoUrl = "/uploads/profile-photos/" + filename;
            Optional<UserPhoto> existingPhoto = userPhotoRepository.findByUserId(user.getId());

            if (existingPhoto.isPresent()) {
                // Update existing photo
                UserPhoto userPhoto = existingPhoto.get();
                userPhoto.setPhotoUrl(photoUrl);
                userPhotoRepository.save(userPhoto);
            } else {
                // Create new photo record
                UserPhoto userPhoto = new UserPhoto(user.getId(), photoUrl);
                userPhotoRepository.save(userPhoto);
            }

            return ResponseEntity.ok(new PhotoUploadResponse(photoUrl, "Photo uploaded successfully"));

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Failed to upload photo: " + e.getMessage());
        }
    }

    // Request DTO for profile update
    public static class UpdateProfileRequest {
        private String email;
        private String fullName;
        private String phoneNumber;
        private String rollNumber;
        private String branch;

        // Getters and Setters
        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public void setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }

        public String getRollNumber() {
            return rollNumber;
        }

        public void setRollNumber(String rollNumber) {
            this.rollNumber = rollNumber;
        }

        public String getBranch() {
            return branch;
        }

        public void setBranch(String branch) {
            this.branch = branch;
        }
    }

    // Response DTO for photo upload
    public static class PhotoUploadResponse {
        private String photoUrl;
        private String message;

        public PhotoUploadResponse(String photoUrl, String message) {
            this.photoUrl = photoUrl;
            this.message = message;
        }

        public String getPhotoUrl() {
            return photoUrl;
        }

        public String getMessage() {
            return message;
        }
    }

    // Inner class for response
    public static class UserProfileResponse {
        private Long id;
        private String username;
        private String email;
        private String fullName;
        private String rollNumber;
        private String branch;
        private String phoneNumber;
        private String photoUrl;

        public UserProfileResponse(Long id, String username, String email, String fullName,
                String rollNumber, String branch, String phoneNumber, String photoUrl) {
            this.id = id;
            this.username = username;
            this.email = email;
            this.fullName = fullName;
            this.rollNumber = rollNumber;
            this.branch = branch;
            this.phoneNumber = phoneNumber;
            this.photoUrl = photoUrl;
        }

        // Getters
        public Long getId() {
            return id;
        }

        public String getUsername() {
            return username;
        }

        public String getEmail() {
            return email;
        }

        public String getFullName() {
            return fullName;
        }

        public String getRollNumber() {
            return rollNumber;
        }

        public String getBranch() {
            return branch;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public String getPhotoUrl() {
            return photoUrl;
        }
    }
}
