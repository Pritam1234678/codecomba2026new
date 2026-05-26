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
import com.example.codecombat2026.service.AuthRateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    @Autowired
    AuthRateLimiterService authRateLimiter;

    @Autowired
    com.example.codecombat2026.service.JwtBlacklistService jwtBlacklistService;

    @Autowired
    com.example.codecombat2026.service.CaptchaService captchaService;

    @GetMapping("/captcha")
    public ResponseEntity<?> getCaptcha() {
        return ResponseEntity.ok(captchaService.issue());
    }

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        // Honeypot: real users never fill the hidden 'website' field.
        if (loginRequest.getWebsite() != null && !loginRequest.getWebsite().isBlank()) {
            // Pretend success so bots can't tell they were caught.
            return ResponseEntity.status(401).body(new MessageResponse("Invalid credentials"));
        }

        String ip = getClientIp(request);
        String username = loginRequest.getUsername();

        // Per-IP login attempt rate limit
        if (!authRateLimiter.allowLogin(ip)) {
            long retry = authRateLimiter.loginRetryAfter(ip);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", String.valueOf(retry))
                    .body(new MessageResponse("Too many login attempts. Try again in " + retry + "s."));
        }

        // Per-account lockout (only meaningful if username is non-blank)
        if (username != null && !username.isBlank() && authRateLimiter.isAccountLocked(username)) {
            long lockSec = authRateLimiter.lockedUntilSec(username);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", String.valueOf(lockSec))
                    .body(new MessageResponse(
                            "Account temporarily locked due to too many failed attempts. Try again in " + lockSec + "s."));
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, loginRequest.getPassword()));
        } catch (DisabledException ex) {
            // Disabled account: don't count as a failed credential attempt.
            throw ex;
        } catch (AuthenticationException ex) {
            // Bad credentials, locked, or any other auth failure → record + bubble up.
            authRateLimiter.recordFailedLogin(username);
            throw ex;
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Check if user account is enabled
        User user = userRepository.findByUsername(username)
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

        // Successful auth — clear failed-login counter / lock
        authRateLimiter.clearFailedLogins(username);

        return ResponseEntity.ok(new JwtResponse(jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                roles));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest signUpRequest, HttpServletRequest request) {
        // Honeypot — silently fake-success for bots.
        if (signUpRequest.getWebsite() != null && !signUpRequest.getWebsite().isBlank()) {
            return ResponseEntity.ok(new MessageResponse("Request received."));
        }

        String ip = getClientIp(request);
        if (!authRateLimiter.allowRegister(ip)) {
            long retry = authRateLimiter.registerRetryAfter(ip);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", String.valueOf(retry))
                    .body(new MessageResponse("Too many registration attempts. Try again in " + retry + "s."));
        }

        // CAPTCHA verification — single-use math challenge.
        if (!captchaService.verify(signUpRequest.getCaptchaToken(), signUpRequest.getCaptchaAnswer())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("CAPTCHA verification failed. Please try again."));
        }

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

        // SECURITY: never trust client-provided role. New users always get ROLE_USER.
        // Admin promotion must happen via DB or admin API only.
        Set<Role> roles = new HashSet<>();
        Role userRole = roleRepository.findByName(Role.ERole.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
        roles.add(userRole);

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
    public ResponseEntity<?> forgotPassword(@RequestBody java.util.Map<String, String> request, HttpServletRequest httpRequest) {
        String username = request.get("username");
        String email = request.get("email");
        String ip = getClientIp(httpRequest);

        // Honeypot
        String honeypot = request.get("website");
        if (honeypot != null && !honeypot.isBlank()) {
            return ResponseEntity.ok(new MessageResponse("Password reset link has been sent to your email."));
        }

        if (!authRateLimiter.allowPasswordReset(email == null ? "_" : email, ip)) {
            long retry = email != null
                    ? authRateLimiter.passwordResetRetryAfter(email)
                    : 3600L;
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", String.valueOf(retry))
                    .body(new MessageResponse("Too many password reset attempts. Try again later."));
        }

        // CAPTCHA verification (token + answer come from the same JSON map)
        if (!captchaService.verify(request.get("captchaToken"), request.get("captchaAnswer"))) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("CAPTCHA verification failed. Please try again."));
        }

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
    public ResponseEntity<?> resetPassword(@RequestBody java.util.Map<String, String> request, HttpServletRequest httpRequest) {
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

        // SECURITY: invalidate every JWT issued for this user before now.
        // Anyone who got a token via stolen credentials is now signed out.
        jwtBlacklistService.invalidateAllUserTokens(user.getId());

        // Notify the user that their password just changed (defensive against takeover)
        try {
            String ip = getClientIp(httpRequest);
            emailService.sendPasswordChangedNotification(user.getEmail(), user.getUsername(), ip);
        } catch (Exception e) {
            log.warn("Failed to send password-changed notification to {}: {}", user.getEmail(), e.getMessage());
        }

        return ResponseEntity.ok(new MessageResponse("Your password has been updated successfully."));
    }

    @PostMapping("/forgot-username")
    public ResponseEntity<?> forgotUsername(@RequestBody java.util.Map<String, String> request, HttpServletRequest httpRequest) {
        String email = request.get("email");
        String ip = getClientIp(httpRequest);

        if (!authRateLimiter.allowForgotUsername(ip)) {
            long retry = authRateLimiter.forgotUsernameRetryAfter(ip);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", String.valueOf(retry))
                    .body(new MessageResponse("Too many username recovery attempts. Try again later."));
        }

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

    @PostMapping("/logout")
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String jwt = parseJwt(request);
        if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
            String jti = jwtUtils.getJtiFromJwtToken(jwt);
            long expiry = jwtUtils.getExpiryFromJwtToken(jwt);
            long now = System.currentTimeMillis();
            long ttlSec = Math.max(1L, (expiry - now) / 1000L);
            if (jti != null) {
                jwtBlacklistService.blacklist(jti, ttlSec);
            }
        }
        return ResponseEntity.ok(new MessageResponse("Logged out"));
    }

    /** Extract a Bearer token from the Authorization header, mirroring AuthTokenFilter. */
    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (headerAuth != null && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }

    /** Extract client IP from common proxy headers (X-Forwarded-For wins, then X-Real-IP). */
    private String getClientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String real = req.getHeader("X-Real-IP");
        if (real != null && !real.isBlank()) return real;
        return req.getRemoteAddr();
    }
}
