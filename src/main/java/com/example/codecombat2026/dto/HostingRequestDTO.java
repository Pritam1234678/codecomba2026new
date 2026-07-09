package com.example.codecombat2026.dto;

import com.example.codecombat2026.entity.ContestHostingRequest.HostingRequestStatus;
import com.example.codecombat2026.entity.ContestHostingRequest.IntendedUseCase;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for ContestHostingRequest entity.
 * Used for API request/response payloads.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HostingRequestDTO {
    private Long id;
    private Long userId;
    private String reason;
    private IntendedUseCase intendedUseCase;
    private HostingRequestStatus status;
    private LocalDateTime submittedAt;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    private String adminNotes;
}
