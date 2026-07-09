package com.example.codecombat2026.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for AuditLog entity.
 * 
 * Used to return audit log information to admin endpoints without
 * exposing the full entity structure or lazy-loaded relationships.
 * 
 * Requirements: 29.3
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDTO {
    
    /**
     * Unique identifier of the audit log entry.
     */
    private Long id;
    
    /**
     * ID of the user who performed the action.
     * Null for system-triggered actions.
     */
    private Long userId;
    
    /**
     * Username of the user who performed the action.
     * Null for system-triggered actions.
     */
    private String username;
    
    /**
     * The action performed (e.g., "CONTEST_CREATED", "PARTICIPANT_JOINED").
     */
    private String action;
    
    /**
     * The type of resource affected (e.g., "PRIVATE_CONTEST", "PARTICIPANT").
     */
    private String resourceType;
    
    /**
     * The ID of the specific resource affected.
     */
    private Long resourceId;
    
    /**
     * Timestamp when the action was performed.
     */
    private LocalDateTime timestamp;
    
    /**
     * IP address of the user who performed the action.
     */
    private String ipAddress;
    
    /**
     * User agent string from the HTTP request.
     */
    private String userAgent;
    
    /**
     * Additional details about the action in JSON format.
     */
    private String detailsJson;
}
