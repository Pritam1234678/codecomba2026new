package com.example.codecombat2026.service;

import com.example.codecombat2026.entity.AuditLog;
import com.example.codecombat2026.repository.AuditLogRepository;
import com.example.codecombat2026.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for logging critical audit events in the Private Contest Hosting system.
 * 
 * Provides centralized audit logging for compliance, debugging, and security monitoring.
 * All critical actions (hosting requests, contest lifecycle, participant management) are logged
 * with full context including user, timestamp, IP address, user agent, and optional details.
 * 
 * Business Rules:
 * - Logs are append-only (no updates or deletes via application layer)
 * - Logging is asynchronous to avoid blocking API responses
 * - Minimum retention: 90 days (enforced by scheduled cleanup job)
 * - All critical private contest actions must be logged
 * 
 * Logged Events:
 * - Hosting request submitted/approved/rejected/revoked
 * - Private contest created/cancelled/deleted
 * - Participant joined/removed
 * - Problem added/removed
 * - Invite link regenerated
 * 
 * Requirements: 2.6, 29.1, 29.2, 29.3
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Core method to create audit log entries.
     * 
     * This is the primary method used by all other audit logging methods.
     * It extracts IP address and User-Agent from the current HTTP request context
     * and persists the audit log asynchronously.
     * 
     * @param userId The ID of the user performing the action (nullable for system actions)
     * @param action The action performed (e.g., "HOSTING_REQUEST_SUBMITTED")
     * @param resourceType The type of resource affected (e.g., "PRIVATE_CONTEST")
     * @param resourceId The ID of the specific resource (nullable)
     * @param details Optional map of additional details (will be serialized to JSON)
     * 
     * Requirements: 29.1
     */
    @Async
    @Transactional
    public void logEvent(Long userId, String action, String resourceType, Long resourceId, Map<String, Object> details) {
        try {
            AuditLog auditLog = new AuditLog();
            
            // Set user if provided
            if (userId != null) {
                userRepository.findById(userId).ifPresent(auditLog::setUser);
            }
            
            auditLog.setAction(action);
            auditLog.setResourceType(resourceType);
            auditLog.setResourceId(resourceId);
            auditLog.setTimestamp(LocalDateTime.now());
            
            // Extract IP address and User-Agent from current request
            HttpServletRequest request = getCurrentRequest();
            if (request != null) {
                auditLog.setIpAddress(extractClientIp(request));
                auditLog.setUserAgent(request.getHeader("User-Agent"));
            }
            
            // Serialize details to JSON
            if (details != null && !details.isEmpty()) {
                try {
                    auditLog.setDetailsJson(objectMapper.writeValueAsString(details));
                } catch (JsonProcessingException e) {
                    log.error("Failed to serialize audit log details to JSON: {}", e.getMessage());
                    // Continue without details rather than failing the entire log
                }
            }
            
            auditLogRepository.save(auditLog);
            log.debug("Audit log created: action={}, resourceType={}, resourceId={}, userId={}", 
                    action, resourceType, resourceId, userId);
            
        } catch (Exception e) {
            // Log errors but don't throw - audit logging should never break business logic
            log.error("Failed to create audit log: action={}, resourceType={}, resourceId={}, error={}", 
                    action, resourceType, resourceId, e.getMessage(), e);
        }
    }

    /**
     * Log hosting request submission.
     * 
     * @param userId The user submitting the request
     * @param requestId The hosting request ID
     * @param reason The reason provided by the user
     * @param intendedUseCase The intended use case
     * 
     * Requirements: 29.1
     */
    public void logHostingRequestSubmitted(Long userId, Long requestId, String reason, String intendedUseCase) {
        Map<String, Object> details = new HashMap<>();
        details.put("reason", reason);
        details.put("intendedUseCase", intendedUseCase);
        
        logEvent(userId, "HOSTING_REQUEST_SUBMITTED", "HOSTING_REQUEST", requestId, details);
    }

    /**
     * Log hosting request approval by admin.
     * 
     * @param adminId The admin who approved the request
     * @param requestId The hosting request ID
     * @param userId The user whose request was approved
     * @param adminNotes Optional notes from the admin
     * 
     * Requirements: 2.6, 29.1
     */
    public void logHostingRequestApproved(Long adminId, Long requestId, Long userId, String adminNotes) {
        Map<String, Object> details = new HashMap<>();
        details.put("approvedUserId", userId);
        details.put("previousStatus", "PENDING");
        details.put("newStatus", "APPROVED");
        if (adminNotes != null) {
            details.put("adminNotes", adminNotes);
        }
        
        logEvent(adminId, "HOSTING_REQUEST_APPROVED", "HOSTING_REQUEST", requestId, details);
    }

    /**
     * Log hosting request rejection by admin.
     * 
     * @param adminId The admin who rejected the request
     * @param requestId The hosting request ID
     * @param userId The user whose request was rejected
     * @param adminNotes Optional notes from the admin
     * 
     * Requirements: 2.6, 29.1
     */
    public void logHostingRequestRejected(Long adminId, Long requestId, Long userId, String adminNotes) {
        Map<String, Object> details = new HashMap<>();
        details.put("rejectedUserId", userId);
        details.put("previousStatus", "PENDING");
        details.put("newStatus", "REJECTED");
        if (adminNotes != null) {
            details.put("adminNotes", adminNotes);
        }
        
        logEvent(adminId, "HOSTING_REQUEST_REJECTED", "HOSTING_REQUEST", requestId, details);
    }

    /**
     * Log hosting request revocation by admin.
     * 
     * @param adminId The admin who revoked the approval
     * @param requestId The hosting request ID
     * @param userId The user whose approval was revoked
     * @param reason The reason for revocation
     * 
     * Requirements: 19.4, 19.5, 29.1
     */
    public void logHostingRequestRevoked(Long adminId, Long requestId, Long userId, String reason) {
        Map<String, Object> details = new HashMap<>();
        details.put("revokedUserId", userId);
        details.put("previousStatus", "APPROVED");
        details.put("newStatus", "REVOKED");
        if (reason != null) {
            details.put("revocationReason", reason);
        }
        
        logEvent(adminId, "HOSTING_REQUEST_REVOKED", "HOSTING_REQUEST", requestId, details);
    }

    /**
     * Log private contest creation.
     * 
     * @param hostUserId The Contest_Host who created the contest
     * @param contestId The contest ID
     * @param contestName The name of the contest
     * @param startTime The contest start time
     * @param endTime The contest end time
     * @param enableProctoring Whether proctoring is enabled
     * 
     * Requirements: 29.1
     */
    public void logContestCreated(Long hostUserId, Long contestId, String contestName, 
                                   String startTime, String endTime, boolean enableProctoring) {
        Map<String, Object> details = new HashMap<>();
        details.put("contestName", contestName);
        details.put("startTime", startTime);
        details.put("endTime", endTime);
        details.put("enableProctoring", enableProctoring);
        
        logEvent(hostUserId, "CONTEST_CREATED", "PRIVATE_CONTEST", contestId, details);
    }

    /**
     * Log private contest cancellation.
     * 
     * @param hostUserId The Contest_Host who cancelled the contest
     * @param contestId The contest ID
     * @param contestName The name of the contest
     * @param cancellationReason The reason for cancellation
     * @param participantCount Number of participants at time of cancellation
     * 
     * Requirements: 29.1
     */
    public void logContestCancelled(Long hostUserId, Long contestId, String contestName, 
                                     String cancellationReason, int participantCount) {
        Map<String, Object> details = new HashMap<>();
        details.put("contestName", contestName);
        details.put("cancellationReason", cancellationReason);
        details.put("participantCount", participantCount);
        
        logEvent(hostUserId, "CONTEST_CANCELLED", "PRIVATE_CONTEST", contestId, details);
    }

    /**
     * Log participant joining a private contest.
     * 
     * @param userId The user who joined
     * @param contestId The contest ID
     * @param contestName The name of the contest
     * @param inviteToken The invite token used (optional, for tracking)
     * 
     * Requirements: 29.1
     */
    public void logParticipantJoined(Long userId, Long contestId, String contestName, String inviteToken) {
        Map<String, Object> details = new HashMap<>();
        details.put("contestName", contestName);
        if (inviteToken != null) {
            // Store only a hash prefix for security
            details.put("inviteTokenPrefix", inviteToken.substring(0, Math.min(8, inviteToken.length())));
        }
        
        logEvent(userId, "PARTICIPANT_JOINED", "PARTICIPANT", contestId, details);
    }

    /**
     * Log participant removal from a private contest.
     * 
     * @param hostUserId The Contest_Host who removed the participant
     * @param removedUserId The user who was removed
     * @param contestId The contest ID
     * @param contestName The name of the contest
     * 
     * Requirements: 29.1
     */
    public void logParticipantRemoved(Long hostUserId, Long removedUserId, Long contestId, String contestName) {
        Map<String, Object> details = new HashMap<>();
        details.put("removedUserId", removedUserId);
        details.put("contestName", contestName);
        
        logEvent(hostUserId, "PARTICIPANT_REMOVED", "PARTICIPANT", contestId, details);
    }

    /**
     * Log invite link regeneration.
     * 
     * @param hostUserId The Contest_Host who regenerated the link
     * @param contestId The contest ID
     * @param contestName The name of the contest
     * @param oldTokenPrefix Prefix of the invalidated token (for tracking)
     * @param newTokenPrefix Prefix of the new token (for tracking)
     * 
     * Requirements: 29.1
     */
    public void logInviteLinkRegenerated(Long hostUserId, Long contestId, String contestName, 
                                         String oldTokenPrefix, String newTokenPrefix) {
        Map<String, Object> details = new HashMap<>();
        details.put("contestName", contestName);
        details.put("oldTokenPrefix", oldTokenPrefix);
        details.put("newTokenPrefix", newTokenPrefix);
        
        logEvent(hostUserId, "INVITE_LINK_REGENERATED", "INVITATION", contestId, details);
    }

    /**
     * Extract client IP address from HTTP request.
     * 
     * Follows the same pattern used throughout the codebase:
     * 1. Check X-Forwarded-For header (first hop, not the proxy chain)
     * 2. Fall back to X-Real-IP header
     * 3. Fall back to RemoteAddr from servlet
     * 
     * This handles proxy/load balancer scenarios where the direct remote address
     * would be the proxy rather than the actual client.
     * 
     * @param request The HTTP servlet request
     * @return The client IP address, or null if unavailable
     */
    private String extractClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        
        // Check X-Forwarded-For (used by nginx and most proxies)
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // Take first IP in the chain (original client)
            return xff.split(",")[0].trim();
        }
        
        // Fall back to X-Real-IP
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }
        
        // Fall back to direct remote address
        return request.getRemoteAddr();
    }

    /**
     * Get the current HTTP request from Spring's RequestContextHolder.
     * 
     * This allows us to access the HTTP request from service layer code without
     * explicitly passing it through method parameters.
     * 
     * Returns null if called outside of a web request context (e.g., scheduled jobs).
     * 
     * @return The current HTTP request, or null if not in a web request context
     */
    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            // Not in a request context (e.g., scheduled job, async task without context propagation)
            return null;
        }
    }
}
