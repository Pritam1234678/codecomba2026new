package com.example.codecombat2026.controller;

import com.example.codecombat2026.dto.HostingRequestDTO;
import com.example.codecombat2026.entity.ContestHostingRequest.HostingRequestStatus;
import com.example.codecombat2026.entity.ContestHostingRequest.IntendedUseCase;
import com.example.codecombat2026.security.services.UserDetailsImpl;
import com.example.codecombat2026.service.ContestHostingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Contest Hosting Request management.
 * 
 * Provides endpoints for:
 * - Submitting hosting requests (authenticated users)
 * - Checking request status (authenticated users)
 * - Listing all requests (admin only, filterable by status)
 * - Approving/rejecting requests (admin only)
 * 
 * Security:
 * - User endpoints: Require ROLE_USER authentication
 * - Admin endpoints: Require ROLE_ADMIN authentication
 * 
 * API Paths:
 * - POST   /api/hosting-requests/submit       - Submit hosting request
 * - GET    /api/hosting-requests/my-status    - Get current user's request status
 * - GET    /api/admin/hosting-requests        - List all requests (admin)
 * - POST   /api/admin/hosting-requests/{id}/approve - Approve request (admin)
 * - POST   /api/admin/hosting-requests/{id}/reject  - Reject request (admin)
 * 
 * Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 2.2, 2.3, 2.4, 2.5, 2.6
 */
@RestController
@RequestMapping("/api")
public class ContestHostingController {

    private static final Logger log = LoggerFactory.getLogger(ContestHostingController.class);

    private final ContestHostingService contestHostingService;

    public ContestHostingController(ContestHostingService contestHostingService) {
        this.contestHostingService = contestHostingService;
    }

    // ─── User Endpoints ────────────────────────────────────────────────────────

    /**
     * Submit a new contest hosting request.
     * 
     * Request body:
     * {
     *   "reason": "I teach a university course and want to create assessments",
     *   "intendedUseCase": "EDUCATION"
     * }
     * 
     * Response (201 Created):
     * {
     *   "id": 1,
     *   "userId": 42,
     *   "status": "PENDING",
     *   "submittedAt": "2026-01-15T10:30:00Z",
     *   "reason": "I teach a university course...",
     *   "intendedUseCase": "EDUCATION"
     * }
     * 
     * Errors:
     * - 409 Conflict: Already have a pending or approved request
     * - 400 Bad Request: Invalid intendedUseCase or missing fields
     * 
     * Requirements: 1.1, 1.2, 1.3, 1.4, 1.5
     */
    @PostMapping("/hosting-requests/submit")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<HostingRequestDTO> submitHostingRequest(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody SubmitRequestPayload payload) {
        
        log.info("User {} submitting hosting request", userDetails.getUsername());
        
        HostingRequestDTO createdRequest = contestHostingService.submitRequest(
                userDetails.getId(),
                payload.reason,
                payload.intendedUseCase
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRequest);
    }

    /**
     * Get the current user's hosting request status.
     * 
     * Response (200 OK):
     * {
     *   "hasRequest": true,
     *   "status": "APPROVED",
     *   "submittedAt": "2026-01-15T10:30:00Z",
     *   "reviewedAt": "2026-01-16T14:00:00Z",
     *   "canCreateContests": true,
     *   "request": { ... full HostingRequestDTO ... }
     * }
     * 
     * If no request exists:
     * {
     *   "hasRequest": false,
     *   "canCreateContests": false
     * }
     * 
     * Requirements: 1.1, 1.2
     */
    @GetMapping("/hosting-requests/my-status")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getMyHostingStatus(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        
        log.debug("User {} checking hosting request status", userDetails.getUsername());
        
        HostingRequestDTO request = contestHostingService.getUserRequestStatus(userDetails.getId());
        
        Map<String, Object> response = new HashMap<>();
        response.put("hasRequest", request != null);
        response.put("canCreateContests", request != null && request.getStatus() == HostingRequestStatus.APPROVED);
        
        if (request != null) {
            response.put("status", request.getStatus());
            response.put("submittedAt", request.getSubmittedAt());
            response.put("reviewedAt", request.getReviewedAt());
            response.put("request", request);
        }
        
        return ResponseEntity.ok(response);
    }

    // ─── Admin Endpoints ───────────────────────────────────────────────────────

    /**
     * List all hosting requests (admin dashboard).
     * 
     * Query Parameters:
     * - status (optional): Filter by PENDING, APPROVED, REJECTED, REVOKED
     * 
     * Response (200 OK):
     * [
     *   {
     *     "id": 1,
     *     "userId": 42,
     *     "reason": "I teach a university course...",
     *     "intendedUseCase": "EDUCATION",
     *     "status": "PENDING",
     *     "submittedAt": "2026-01-15T10:30:00Z"
     *   }
     * ]
     * 
     * Requirements: 2.1
     */
    @GetMapping("/admin/hosting-requests")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<HostingRequestDTO>> listHostingRequests(
            @RequestParam(required = false) HostingRequestStatus status) {
        
        log.info("Admin listing hosting requests with status filter: {}", status);
        
        List<HostingRequestDTO> requests;
        if (status != null) {
            requests = contestHostingService.getRequestsByStatus(status);
        } else {
            // Get all pending requests by default
            requests = contestHostingService.getRequestsByStatus(HostingRequestStatus.PENDING);
        }
        
        return ResponseEntity.ok(requests);
    }

    /**
     * Approve a hosting request.
     * 
     * Request body (optional):
     * {
     *   "adminNotes": "Verified educational institution email. Approved."
     * }
     * 
     * Response (200 OK):
     * {
     *   "id": 1,
     *   "status": "APPROVED",
     *   "reviewedBy": 1,
     *   "reviewedAt": "2026-01-16T14:00:00Z",
     *   "adminNotes": "Verified educational institution email. Approved."
     * }
     * 
     * Errors:
     * - 404 Not Found: Request ID doesn't exist
     * - 409 Conflict: Request not in PENDING status
     * 
     * Requirements: 2.2, 2.4, 2.6
     */
    @PostMapping("/admin/hosting-requests/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<HostingRequestDTO> approveHostingRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody(required = false) AdminActionPayload payload) {
        
        log.info("Admin {} approving hosting request {}", userDetails.getUsername(), id);
        
        String adminNotes = (payload != null) ? payload.adminNotes : null;
        
        HostingRequestDTO updatedRequest = contestHostingService.approveRequest(
                id,
                userDetails.getId(),
                adminNotes
        );
        
        return ResponseEntity.ok(updatedRequest);
    }

    /**
     * Reject a hosting request.
     * 
     * Request body:
     * {
     *   "adminNotes": "Email domain does not match stated organization."
     * }
     * 
     * Response (200 OK):
     * {
     *   "id": 1,
     *   "status": "REJECTED",
     *   "reviewedBy": 1,
     *   "reviewedAt": "2026-01-16T14:05:00Z",
     *   "adminNotes": "Email domain does not match stated organization."
     * }
     * 
     * Errors:
     * - 404 Not Found: Request ID doesn't exist
     * - 409 Conflict: Request not in PENDING status
     * 
     * Requirements: 2.3, 2.6
     */
    @PostMapping("/admin/hosting-requests/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<HostingRequestDTO> rejectHostingRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody(required = false) AdminActionPayload payload) {
        
        log.info("Admin {} rejecting hosting request {}", userDetails.getUsername(), id);
        
        String adminNotes = (payload != null) ? payload.adminNotes : null;
        
        HostingRequestDTO updatedRequest = contestHostingService.rejectRequest(
                id,
                userDetails.getId(),
                adminNotes
        );
        
        return ResponseEntity.ok(updatedRequest);
    }

    // ─── Request/Response DTOs ─────────────────────────────────────────────────

    /**
     * Payload for submitting a hosting request.
     */
    public record SubmitRequestPayload(
            @Size(max = 500, message = "Reason cannot exceed 500 characters")
            String reason,
            
            @NotNull(message = "Intended use case is required")
            IntendedUseCase intendedUseCase
    ) {}

    /**
     * Payload for admin approval/rejection actions.
     */
    public record AdminActionPayload(
            String adminNotes
    ) {}
}
