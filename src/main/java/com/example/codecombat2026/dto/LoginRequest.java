package com.example.codecombat2026.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank
    private String username;

    @NotBlank
    private String password;

    /** Honeypot — must remain empty. */
    private String website;

    /** Cloudflare Turnstile response token submitted by the widget. */
    private String turnstileToken;
}
