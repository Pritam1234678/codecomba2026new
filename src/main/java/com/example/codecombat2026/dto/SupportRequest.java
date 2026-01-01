package com.example.codecombat2026.dto;

import lombok.Data;

@Data
public class SupportRequest {
    private String fullName;
    private String email;
    private String phone;
    private String message;
}
