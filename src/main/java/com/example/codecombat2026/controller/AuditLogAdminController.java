package com.example.codecombat2026.controller;

import com.example.codecombat2026.dto.AuditLogDTO;
import com.example.codecombat2026.entity.AuditLog;
import com.example.codecombat2026.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * REST Controller for Admin audit log queries.
 * 
 * Provides admin-only endpoints for querying audit logs with comprehensive filtering:
 * - Filter by user who performed the action
 * - Filter by action type (e.g., CONTEST_CREATED, PARTICIPANT_JOINED)
 * - Filter by resource type (e.g., PRIVATE_CONTEST, HOSTING_REQUEST)
 * - Filter by date range
 * - Paginated results with sorting
 * 
 * Security:
 * - ALL endpoints require ROLE_ADMIN authentication
 * - Used for compliance auditing, debugging, and security investigations
 * 
 * API Paths:
 * - GET /api/admin/audit-logs - Query audit logs with filters
 * 
 * Requirements: 29.3
 */
@RestController
@RequestMapping("/api/admin/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogAdminController {

    private static final Logger log = LoggerFactory.getLogger(AuditLogAdminController.class);

    private final AuditLogRepository auditLogRepository;

    public AuditLogAdminController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Query audit logs with filtering (admin only).
     * 
     * Returns paginated audit log entries with support for multiple filter criteria.
     * Enables admins to track and investigate user actions, contest lifecycle events,
     * and administrative actions for compliance and debugging purposes.
     * 
     * Query Parameters:
     * - userId (optional): Filter by user ID who performed the action
     * - action (optional): Filter by specific action type (e.g., "CONTEST_CREATED", "PARTICIPANT_JOINED")
     * - resourceType (optional): Filter by resource type (e.g., "PRIVATE_CONTEST", "HOSTING_REQUEST")
     * - startDate (optional): Filter logs after this ISO datetime (inclusive)
     * - endDate (optional): Filter logs before this ISO datetime (exclusive)
     * - page (default: 0): Page number (0-indexed)
     * - size (default: 50): Page size
     * - sort (default: "timestamp,desc"): Sort field and direction
     * 
     * Response (200 OK):
     * {
     *   "content": [
     *     {
     *       "id": 1234,
     *       "userId": 42,
     *       "username": "prof_smith",
     *       "action": "CONTEST_CREATED",
     *       "resourceType": "PRIVATE_CONTEST",
     *       "resourceId": 501,
     *       "timestamp": "2026-02-01T14:00:00",
     *       "ipAddress": "192.168.1.100",
     *       "userAgent": "Mozilla/5.0...",
     *       "detailsJson": "{\"contestName\":\"CS101 Midterm\"}"
     *     }
     *   ],
     *   "totalElements": 1500,
     *   "totalPages": 30,
     *   "number": 0,
     *   "size": 50
     * }
     * 
     * Example Queries:
     * - All actions by a specific user:
     *   GET /api/admin/audit-logs?userId=42
     * 
     * - All contest creations:
     *   GET /api/admin/audit-logs?action=CONTEST_CREATED
     * 
     * - All actions on a specific resource type:
     *   GET /api/admin/audit-logs?resourceType=PRIVATE_CONTEST
     * 
     * - Actions within a date range:
     *   GET /api/admin/audit-logs?startDate=2026-01-01T00:00:00&endDate=2026-02-01T00:00:00
     * 
     * - Complex filter (user + action + date range):
     *   GET /api/admin/audit-logs?userId=42&action=PARTICIPANT_JOINED&startDate=2026-01-15T00:00:00
     * 
     * Requirements: 29.3
     */
    @GetMapping
    public ResponseEntity<Page<AuditLogDTO>> queryAuditLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "timestamp,desc") String sort) {
        
        log.info("Admin querying audit logs - filters: userId={}, action={}, resourceType={}, dateRange=[{}, {}]", 
                userId, action, resourceType, startDate, endDate);

        // Parse sort parameter
        String[] sortParts = sort.split(",");
        String sortField = sortParts[0];
        Sort.Direction sortDirection = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc") 
                ? Sort.Direction.ASC 
                : Sort.Direction.DESC;
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortField));

        // Query audit logs with filters
        Page<AuditLog> auditLogs = auditLogRepository.findByFilters(
                userId, action, resourceType, startDate, endDate, pageable);

        // Convert to DTOs
        Page<AuditLogDTO> dtoPage = auditLogs.map(this::convertToDTO);

        log.info("Retrieved {} audit log entries (total: {})", 
                dtoPage.getNumberOfElements(), dtoPage.getTotalElements());
        
        return ResponseEntity.ok(dtoPage);
    }

    /**
     * Convert AuditLog entity to DTO.
     * 
     * Maps entity fields to DTO, handling null User gracefully (for system actions).
     * 
     * @param auditLog The AuditLog entity
     * @return AuditLogDTO with all fields populated
     */
    private AuditLogDTO convertToDTO(AuditLog auditLog) {
        AuditLogDTO dto = new AuditLogDTO();
        dto.setId(auditLog.getId());
        dto.setAction(auditLog.getAction());
        dto.setResourceType(auditLog.getResourceType());
        dto.setResourceId(auditLog.getResourceId());
        dto.setTimestamp(auditLog.getTimestamp());
        dto.setIpAddress(auditLog.getIpAddress());
        dto.setUserAgent(auditLog.getUserAgent());
        dto.setDetailsJson(auditLog.getDetailsJson());
        
        // Handle nullable User (for system-triggered actions)
        if (auditLog.getUser() != null) {
            dto.setUserId(auditLog.getUser().getId());
            dto.setUsername(auditLog.getUser().getUsername());
        }
        
        return dto;
    }
}
