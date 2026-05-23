package com.example.codecombat2026.controller;

import com.example.codecombat2026.dto.JwtResponse;
import com.example.codecombat2026.dto.LoginRequest;
import com.example.codecombat2026.dto.MessageResponse;
import com.example.codecombat2026.dto.RegisterRequest;
import com.example.codecombat2026.entity.Role;
import com.example.codecombat2026.entity.User;
import com.example.codecombat2026.repository.RoleRepository;
import com.example.codecombat2026.repository.UserRepository;
import com.example.codecombat2026.security.jwt.JwtUtils;
import com.example.codecombat2026.security.services.UserDetailsImpl;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;

    @Autowired
    com.example.codecombat2026.service.EmailService emailService;

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Check if user account is enabled
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.getEnabled()) {
            return ResponseEntity.status(403)
                    .body(new MessageResponse(
                            "ACCOUNT_DISABLED: Your account has been disabled. Please contact support@codecombat.live for assistance."));
        }

        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        return ResponseEntity.ok(new JwtResponse(jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                roles));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest signUpRequest) {
        // 1. Format Validations
        if (signUpRequest.getUsername().contains(" ")) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Username cannot contain spaces."));
        }

        // Password Validation: Min 8 chars, 1 Upper, 1 Lower, 1 Special
        String passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%&*!]).{8,}$";
        if (!signUpRequest.getPassword().matches(passwordPattern)) {
            return ResponseEntity.badRequest().body(new MessageResponse(
                    "Error: Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, and one special character (@ # $ % & * !)."));
        }

        // 2. Uniqueness Validations
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Username is already taken!"));
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Email is already in use!"));
        }

        // Create new user's account
        User user = new User(signUpRequest.getUsername(),
                signUpRequest.getEmail(),
                encoder.encode(signUpRequest.getPassword()));

        user.setFullName(signUpRequest.getFullName());

        Set<String> strRoles = signUpRequest.getRole();
        Set<Role> roles = new HashSet<>();

        if (strRoles == null) {
            Role userRole = roleRepository.findByName(Role.ERole.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(userRole);
        } else {
            strRoles.forEach(role -> {
                switch (role) {
                    case "admin":
                        Role adminRole = roleRepository.findByName(Role.ERole.ROLE_ADMIN)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(adminRole);
                        break;
                    default:
                        Role userRole = roleRepository.findByName(Role.ERole.ROLE_USER)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(userRole);
                }
            });
        }

        user.setRoles(roles);
        userRepository.save(user);

        // Send welcome email to new user
        try {
            emailService.sendWelcomeEmail(user.getEmail(), user.getFullName());
        } catch (Exception e) {
            // Log error but don't fail registration
            log.warn("Failed to send welcome email to {}: {}", user.getEmail(), e.getMessage());
        }

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }

    @Autowired
    private com.example.codecombat2026.repository.PasswordResetTokenRepository passwordResetTokenRepository;

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody java.util.Map<String, String> request) {
        String username = request.get("username");
        String email = request.get("email");

        java.util.Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isEmpty() || !userOpt.get().getEmail().equals(email)) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("The provided details do not match our records."));
        }

        User user = userOpt.get();
        passwordResetTokenRepository.deleteByUser(user);

        String token = java.util.UUID.randomUUID().toString();
        com.example.codecombat2026.entity.PasswordResetToken resetToken = new com.example.codecombat2026.entity.PasswordResetToken(token, user);
        passwordResetTokenRepository.save(resetToken);

        try {
            emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName() != null ? user.getFullName() : "user", token);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new MessageResponse("Failed to send password reset email. Please try again later."));
        }

        return ResponseEntity.ok(new MessageResponse("Password reset link has been sent to your email."));
    }

    @GetMapping("/validate-reset-token/{token}")
    public ResponseEntity<?> validateResetToken(@PathVariable String token) {
        java.util.Optional<com.example.codecombat2026.entity.PasswordResetToken> tokenOpt = passwordResetTokenRepository
                .findByToken(token);

        if (tokenOpt.isEmpty() || tokenOpt.get().getUsed() || tokenOpt.get().isExpired()) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("This password reset link is invalid or has expired."));
        }

        User user = tokenOpt.get().getUser();
        String fullName = user.getFullName() != null ? user.getFullName() : "user";

        return ResponseEntity.ok(java.util.Map.of(
                "valid", true,
                "fullName", fullName));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody java.util.Map<String, String> request) {
        String token = request.get("token");
        String newPassword = request.get("newPassword");

        // Validate password complexity
        if (newPassword == null || newPassword.length() < 8) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Password must be at least 8 characters long."));
        }
        if (!newPassword.matches(".*[A-Z].*")) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Password must contain at least one uppercase letter."));
        }
        if (!newPassword.matches(".*[a-z].*")) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Password must contain at least one lowercase letter."));
        }
        if (!newPassword.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Password must contain at least one special character."));
        }

        java.util.Optional<com.example.codecombat2026.entity.PasswordResetToken> tokenOpt = passwordResetTokenRepository
                .findByToken(token);

        if (tokenOpt.isEmpty() || tokenOpt.get().getUsed() || tokenOpt.get().isExpired()) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("This password reset link is invalid or has expired."));
        }

        com.example.codecombat2026.entity.PasswordResetToken resetToken = tokenOpt.get();
        User user = resetToken.getUser();

        // Update password (BCrypt hashing)
        user.setPassword(encoder.encode(newPassword));
        userRepository.save(user);

        // Mark token as used (single-use security)
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        return ResponseEntity.ok(new MessageResponse("Your password has been updated successfully."));
    }

    @PostMapping("/forgot-username")
    public ResponseEntity<?> forgotUsername(@RequestBody java.util.Map<String, String> request) {
        String email = request.get("email");

        java.util.Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("No account found with this email."));
        }

        User user = userOpt.get();

        try {
            emailService.sendUsernameRecoveryEmail(user.getEmail(), user.getUsername());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new MessageResponse("Failed to send username recovery email. Please try again later."));
        }

        return ResponseEntity.ok(new MessageResponse("Your username has been sent to your email."));
    }
}
