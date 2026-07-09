package com.example.codecombat2026.controller;

import com.example.codecombat2026.dto.AuditLogDTO;
import com.example.codecombat2026.dto.PrivateContestDTO;
import com.example.codecombat2026.entity.AuditLog;
import com.example.codecombat2026.entity.Contest.ContestStatus;
import com.example.codecombat2026.repository.AuditLogRepository;
import com.example.codecombat2026.security.services.UserDetailsImpl;
import com.example.codecombat2026.service.PrivateContestAdminService;
import com.example.codecombat2026.service.PrivateContestAdminService.ContestFilters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for Admin oversight and moderation of private contests.
 * 
 * Provides admin-only endpoints for:
 * - Listing all private contests across all hosts (with filtering)
 * - Viewing full details of any private contest (bypasses ownership checks)
 * - Deleting private contests with cascade cleanup
 * - Monitoring judge queue statistics
 * 
 * Security:
 * - ALL endpoints require ROLE_ADMIN authentication
 * - Bypasses normal access control checks enforced by PrivateContestAccessValidator
 * 
 * API Paths:
 * - GET    /api/admin/private-contests           - List all private contests with filters
 * - GET    /api/admin/private-contests/{id}      - Get full contest details
 * - DELETE /api/admin/private-contests/{id}      - Delete a private contest
 * - GET    /api/admin/private-contests/judge-stats - Get judge queue statistics
 * - GET    /api/admin/private-contests/audit-logs - Query audit logs with filters
 * 
 * Requirements: 19.1, 19.2, 19.3, 22.3, 29.3
 */
@RestController
@RequestMapping("/api/admin/private-contests")
@PreAuthorize("hasRole('ADMIN')")
public class PrivateContestAdminController {

    private static final Logger log = LoggerFactory.getLogger(PrivateContestAdminController.class);

    private final PrivateContestAdminService adminService;
    private final AuditLogRepository auditLogRepository;

    public PrivateContestAdminController(PrivateContestAdminService adminService, 
                                          AuditLogRepository auditLogRepository) {
        this.adminService = adminService;
        this.auditLogRepository = auditLogRepository;
    }

    // ─── Admin Oversight Endpoints ─────────────────────────────────────────────

    /**
     * List all private contests in the system (admin dashboard).
     * 
     * Returns paginated list of ALL private contests across all hosts.
     * Supports filtering by status, host, cancellation status, and date range.
     * 
     * Query Parameters:
     * - status (optional): Filter by contest status (UPCOMING, LIVE, ENDED)
     * - hostUserId (optional): Filter by specific host user ID
     * - cancelled (optional): Filter by cancellation status (true/false)
     * - createdAfter (optional): Filter contests created after this ISO datetime
     * - createdBefore (optional): Filter contests created before this ISO datetime
     * - page (default: 0): Page number (0-indexed)
     * - size (default: 20): Page size
     * - sort (default: "createdAt,desc"): Sort field and direction
     * 
     * Response (200 OK):
     * {
     *   "content": [
     *     {
     *       "id": 101,
     *       "contestId": 501,
     *       "name": "CS101 Midterm Exam",
     *       "hostUserId": 42,
     *       "hostUsername": "prof_smith",
     *       "status": "UPCOMING",
     *       "startTime": "2026-02-01T14:00:00",
     *       "endTime": "2026-02-01T17:00:00",
     *       "participantCount": 35,
     *       "cancelled": false,
     *       "createdAt": "2026-01-15T10:00:00"
     *     }
     *   ],
     *   "totalElements": 150,
     *   "totalPages": 8,
     *   "number": 0,
     *   "size": 20
     * }
     * 
     * Requirements: 19.1
     */
    @GetMapping
    public ResponseEntity<Page<PrivateContestDTO>> listAllPrivateContests(
            @RequestParam(required = false) ContestStatus status,
            @RequestParam(required = false) Long hostUserId,
            @RequestParam(required = false) Boolean cancelled,
            @RequestParam(required = false) LocalDateTime createdAfter,
            @RequestParam(required = false) LocalDateTime createdBefore,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        
        log.info("Admin listing all private contests - page: {}, size: {}, filters: status={}, hostUserId={}, cancelled={}", 
                page, size, status, hostUserId, cancelled);

        // Build filters
        ContestFilters filters = new ContestFilters();
        filters.setStatus(status);
        filters.setHostUserId(hostUserId);
        filters.setCancelled(cancelled);
        filters.setCreatedAfter(createdAfter);
        filters.setCreatedBefore(createdBefore);

        // Parse sort parameter
        String[] sortParts = sort.split(",");
        String sortField = sortParts[0];
        Sort.Direction sortDirection = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc") 
                ? Sort.Direction.ASC 
                : Sort.Direction.DESC;
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortField));

        // Fetch paginated results
        Page<PrivateContestDTO> contests = adminService.listAllPrivateContests(filters, pageable);

        log.info("Retrieved {} private contests (total: {})", contests.getNumberOfElements(), contests.getTotalElements());
        
        return ResponseEntity.ok(contests);
    }

    /**
     * Get full details of a specific private contest (admin only).
     * 
     * Returns complete contest information including:
     * - All contest metadata (name, description, times, status)
     * - Host information (ID, username)
     * - Participant count
     * - Proctoring settings
     * - Cancellation details
     * 
     * Bypasses the normal access control that restricts contest details
     * to the host and participants only.
     * 
     * Response (200 OK):
     * {
     *   "id": 101,
     *   "contestId": 501,
     *   "name": "CS101 Midterm Exam",
     *   "description": "Data structures and algorithms assessment...",
     *   "hostUserId": 42,
     *   "hostUsername": "prof_smith",
     *   "startTime": "2026-02-01T14:00:00",
     *   "endTime": "2026-02-01T17:00:00",
     *   "status": "UPCOMING",
     *   "enableProctoring": true,
     *   "participantCount": 35,
     *   "cancelled": false,
     *   "createdAt": "2026-01-15T10:00:00"
     * }
     * 
     * Errors:
     * - 404 Not Found: Contest doesn't exist
     * 
     * Requirements: 19.2
     */
    @GetMapping("/{contestId}")
    public ResponseEntity<PrivateContestDTO> getPrivateContestDetails(
            @PathVariable Long contestId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        
        log.info("Admin {} retrieving details for private contest {}", 
                userDetails.getUsername(), contestId);

        PrivateContestDTO contest = adminService.getPrivateContestDetails(contestId);

        log.debug("Retrieved details for contest: {} (host: {})", 
                contest.getName(), contest.getHostUsername());
        
        return ResponseEntity.ok(contest);
    }

    /**
     * Delete a private contest (admin only).
     * 
     * This is a destructive operation that:
     * 1. Deletes all private_contest_participants rows
     * 2. Deletes all private_contest_invitations rows
     * 3. Deletes the private_contests row
     * 4. Optionally deletes the contests row (if no submissions exist)
     * 5. Invalidates all related caches
     * 6. Logs the deletion in the audit log
     * 
     * Business Rules:
     * - If the contest has submissions, the base Contest entity is preserved
     *   (to maintain submission history integrity)
     * - If the contest has no submissions, the base Contest entity is deleted
     * - All private-contest-specific data is always deleted
     * 
     * Response (200 OK):
     * {
     *   "success": true,
     *   "message": "Private contest deleted successfully",
     *   "contestId": 501,
     *   "baseContestDeleted": false,
     *   "submissionCount": 25
     * }
     * 
     * Errors:
     * - 404 Not Found: Contest doesn't exist
     * 
     * Warning: This operation cannot be undone. Use with caution.
     * 
     * Requirements: 19.3
     */
    @DeleteMapping("/{contestId}")
    public ResponseEntity<Map<String, Object>> deletePrivateContest(
            @PathVariable Long contestId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        
        log.warn("Admin {} requesting deletion of private contest {}", 
                userDetails.getUsername(), contestId);

        // Perform the deletion (service handles all cascade logic and audit logging)
        adminService.deletePrivateContest(contestId, userDetails.getId());

        // Build success response
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Private contest deleted successfully");
        response.put("contestId", contestId);
        response.put("deletedAt", LocalDateTime.now());

        log.info("Successfully deleted private contest {} by admin {}", 
                contestId, userDetails.getUsername());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get judge queue statistics for monitoring.
     * 
     * Returns current statistics about the judge queue processing:
     * - Public queue length
     * - Private queue length
     * - Average latency (if available)
     * - Worker utilization metrics
     * 
     * This endpoint provides operational visibility into the judge system
     * to help admins monitor performance and identify bottlenecks.
     * 
     * Response (200 OK):
     * {
     *   "publicQueueLength": 5,
     *   "privateQueueLength": 3,
     *   "avgJudgeLatencySeconds": 2.5,
     *   "workersActive": 4,
     *   "workersIdle": 2,
     *   "totalWorkers": 6,
     *   "timestamp": "2026-02-01T15:30:00"
     * }
     * 
     * Note: This endpoint is a placeholder for future implementation.
     * Currently returns mock/unavailable data pending integration with
     * the judge worker monitoring system.
     * 
     * Requirements: 22.3
     */
    @GetMapping("/judge-stats")
    public ResponseEntity<Map<String, Object>> getJudgeStats() {
        
        log.debug("Admin requesting judge queue statistics");

        // TODO: Implement actual judge queue monitoring
        // This would require integration with:
        // - Redis/Valkey queue length monitoring (LLEN submission:queue, private:submission:queue)
        // - Judge worker metrics collection (via shared metrics or monitoring API)
        // - Latency tracking (submission created_at vs verdict_at timestamps)

        Map<String, Object> stats = new HashMap<>();
        stats.put("publicQueueLength", 0);
        stats.put("privateQueueLength", 0);
        stats.put("avgJudgeLatencySeconds", 0.0);
        stats.put("workersActive", 0);
        stats.put("workersIdle", 0);
        stats.put("totalWorkers", 6);
        stats.put("timestamp", LocalDateTime.now());
        stats.put("note", "Judge statistics monitoring not yet implemented");

        return ResponseEntity.ok(stats);
    }

    // ─── Audit Log Query Endpoint ─────────────────────────────────────────────

    /**
     * Query audit logs with filtering (admin only).
     * 
     * Returns paginated audit log entries with support for multiple filter criteria.
     * Enables admins to track and investigate user actions, contest lifecycle events,
     * and administrative actions for compliance and debugging purposes.
     * 
     * Query Parameters:
     * - userId (optional): Filter by user ID who performed the action
     * - action (optional): Filter by specific action type (e.g., "CONTEST_CREATED")
     * - resourceType (optional): Filter by resource type (e.g., "PRIVATE_CONTEST")
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
    @GetMapping("/audit-logs")
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
