package com.example.codecombat2026.dto;

import com.example.codecombat2026.entity.Contest.ContestStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for PrivateContest entity with related Contest details.
 * 
 * Used for API request/response payloads for private contest operations.
 * Combines fields from both PrivateContest and Contest entities for convenience.
 * 
 * Includes:
 * - Basic contest info (name, description, times, status)
 * - Host information
 * - Proctoring settings
 * - Cancellation status
 * - Invite link details
 * - Participant count
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrivateContestDTO {
    
    // PrivateContest fields
    private Long id;                      // PrivateContest ID
    private Long contestId;               // Associated Contest ID
    private Long hostUserId;              // Host user ID
    private String hostUsername;          // Host username (for display)
    private Boolean enableProctoring;     // Proctoring enabled flag
    private Boolean cancelled;            // Cancellation flag
    private LocalDateTime cancelledAt;    // Cancellation timestamp
    private String cancellationReason;    // Reason for cancellation
    private LocalDateTime createdAt;      // Creation timestamp
    
    // Contest fields
    private String name;                  // Contest name
    private String description;           // Contest description
    private LocalDateTime startTime;      // Contest start time
    private LocalDateTime endTime;        // Contest end time
    private ContestStatus status;         // Contest status (UPCOMING, LIVE, ENDED)
    
    // Invite link fields
    private String inviteToken;           // Invite token string
    private String inviteLink;            // Full invite link URL
    private LocalDateTime inviteLinkExpiresAt;  // Token expiry timestamp
    
    // Computed fields
    private Long participantCount;        // Number of joined participants
}
