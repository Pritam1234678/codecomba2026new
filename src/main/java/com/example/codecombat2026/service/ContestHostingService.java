package com.example.codecombat2026.service;

import com.example.codecombat2026.dto.HostingRequestDTO;
import com.example.codecombat2026.entity.ContestHostingRequest;
import com.example.codecombat2026.entity.ContestHostingRequest.HostingRequestStatus;
import com.example.codecombat2026.entity.ContestHostingRequest.IntendedUseCase;
import com.example.codecombat2026.entity.User;
import com.example.codecombat2026.exception.ConflictException;
import com.example.codecombat2026.exception.ResourceNotFoundException;
import com.example.codecombat2026.repository.ContestHostingRequestRepository;
import com.example.codecombat2026.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing contest hosting requests and approvals.
 * 
 * Implements the hosting request workflow:
 * 1. Verified users submit hosting requests
 * 2. Admins review and approve/reject requests
 * 3. Approved users become Contest_Hosts
 * 
 * Business Rules:
 * - Only one PENDING request per user at a time
 * - Cannot submit new request if already APPROVED
 * - Email notifications sent on key events
 * 
 * Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 2.2, 2.3, 2.4, 2.5, 2.6
 */
@Service
public class ContestHostingService {

    private static final Logger log = LoggerFactory.getLogger(ContestHostingService.class);

    @Autowired
    private ContestHostingRequestRepository hostingRequestRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private AuditService auditService;

    /**
     * Submit a new contest hosting request.
     * 
     * Validates that the user doesn't already have a PENDING or APPROVED request.
     * Creates a new request with status PENDING and sends email notification to admins.
     * 
     * @param userId The ID of the user submitting the request
     * @param reason The reason for requesting hosting privileges (max 500 chars)
     * @param intendedUseCase The intended use case for private contests
     * @return HostingRequestDTO containing the created request details
     * @throws ResourceNotFoundException if user doesn't exist
     * @throws ConflictException if user already has a PENDING or APPROVED request
     * 
     * Requirements: 1.1, 1.2, 1.3, 1.4, 1.5
     */
    @Transactional
    public HostingRequestDTO submitRequest(Long userId, String reason, IntendedUseCase intendedUseCase) {
        log.info("Processing hosting request submission for user {}", userId);

        // Validate user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check for existing PENDING request
        if (hostingRequestRepository.existsByUserIdAndStatus(userId, HostingRequestStatus.PENDING)) {
            throw new ConflictException("You already have a pending hosting request");
        }

        // Check for existing APPROVED request
        if (hostingRequestRepository.existsByUserIdAndStatus(userId, HostingRequestStatus.APPROVED)) {
            throw new ConflictException("You are already approved to host contests");
        }

        // Create new hosting request
        ContestHostingRequest request = new ContestHostingRequest();
        request.setUser(user);
        request.setReason(reason);
        request.setIntendedUseCase(intendedUseCase);
        request.setStatus(HostingRequestStatus.PENDING);
        request.setSubmittedAt(LocalDateTime.now());

        ContestHostingRequest savedRequest = hostingRequestRepository.save(request);
        log.info("Created hosting request {} for user {}", savedRequest.getId(), userId);

        // Send email notification to admins
        try {
            sendAdminNotificationEmail(savedRequest);
        } catch (Exception e) {
            log.error("Failed to send admin notification email for request {}: {}", 
                    savedRequest.getId(), e.getMessage());
            // Non-fatal - request was created successfully
        }

        return convertToDTO(savedRequest);
    }

    /**
     * Approve a hosting request.
     * 
     * Updates the request status to APPROVED, records the reviewing admin and timestamp,
     * and sends an approval email to the requesting user.
     * 
     * @param requestId The ID of the hosting request
     * @param adminId The ID of the admin approving the request
     * @param adminNotes Optional notes from the admin
     * @return HostingRequestDTO containing the updated request details
     * @throws ResourceNotFoundException if request doesn't exist
     * @throws ConflictException if request is not in PENDING status
     * 
     * Requirements: 2.2, 2.4
     */
    @Transactional
    public HostingRequestDTO approveRequest(Long requestId, Long adminId, String adminNotes) {
        log.info("Admin {} approving hosting request {}", adminId, requestId);

        ContestHostingRequest request = hostingRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Hosting request not found"));

        if (request.getStatus() != HostingRequestStatus.PENDING) {
            throw new ConflictException("Can only approve requests in PENDING status");
        }

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found"));

        request.setStatus(HostingRequestStatus.APPROVED);
        request.setReviewedBy(admin);
        request.setReviewedAt(LocalDateTime.now());
        request.setAdminNotes(adminNotes);

        ContestHostingRequest updatedRequest = hostingRequestRepository.save(request);
        log.info("Approved hosting request {} by admin {}", requestId, adminId);

        // Log audit event
        auditService.logHostingRequestApproved(adminId, requestId, request.getUser().getId(), adminNotes);

        // Send approval email to user
        try {
            sendApprovalEmail(updatedRequest);
        } catch (Exception e) {
            log.error("Failed to send approval email for request {}: {}", requestId, e.getMessage());
            // Non-fatal - request was approved successfully
        }

        return convertToDTO(updatedRequest);
    }

    /**
     * Reject a hosting request.
     * 
     * Updates the request status to REJECTED, records the reviewing admin and timestamp,
     * and sends a rejection email to the requesting user with the reason.
     * 
     * @param requestId The ID of the hosting request
     * @param adminId The ID of the admin rejecting the request
     * @param adminNotes Required notes explaining the rejection reason
     * @return HostingRequestDTO containing the updated request details
     * @throws ResourceNotFoundException if request doesn't exist
     * @throws ConflictException if request is not in PENDING status
     * 
     * Requirements: 2.3
     */
    @Transactional
    public HostingRequestDTO rejectRequest(Long requestId, Long adminId, String adminNotes) {
        log.info("Admin {} rejecting hosting request {}", adminId, requestId);

        ContestHostingRequest request = hostingRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Hosting request not found"));

        if (request.getStatus() != HostingRequestStatus.PENDING) {
            throw new ConflictException("Can only reject requests in PENDING status");
        }

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found"));

        request.setStatus(HostingRequestStatus.REJECTED);
        request.setReviewedBy(admin);
        request.setReviewedAt(LocalDateTime.now());
        request.setAdminNotes(adminNotes);

        ContestHostingRequest updatedRequest = hostingRequestRepository.save(request);
        log.info("Rejected hosting request {} by admin {}", requestId, adminId);

        // Log audit event
        auditService.logHostingRequestRejected(adminId, requestId, request.getUser().getId(), adminNotes);

        // Send rejection email to user
        try {
            sendRejectionEmail(updatedRequest);
        } catch (Exception e) {
            log.error("Failed to send rejection email for request {}: {}", requestId, e.getMessage());
            // Non-fatal - request was rejected successfully
        }

        return convertToDTO(updatedRequest);
    }

    /**
     * Revoke an approved Contest_Host's hosting privileges.
     * 
     * Updates the request status from APPROVED to REVOKED, preventing the user from
     * creating new private contests. Existing contests remain active but the user
     * cannot create new ones until re-approved.
     * 
     * @param requestId The ID of the hosting request
     * @param adminId The ID of the admin revoking the privileges
     * @param reason Required explanation for the revocation
     * @return HostingRequestDTO containing the updated request details
     * @throws ResourceNotFoundException if request doesn't exist
     * @throws ConflictException if request is not in APPROVED status
     * 
     * Requirements: 19.4, 19.5
     */
    @Transactional
    public HostingRequestDTO revokeApproval(Long requestId, Long adminId, String reason) {
        log.info("Admin {} revoking hosting privileges for request {}", adminId, requestId);

        ContestHostingRequest request = hostingRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Hosting request not found"));

        if (request.getStatus() != HostingRequestStatus.APPROVED) {
            throw new ConflictException("Can only revoke requests in APPROVED status");
        }

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found"));

        request.setStatus(HostingRequestStatus.REVOKED);
        request.setReviewedBy(admin);
        request.setReviewedAt(LocalDateTime.now());
        request.setAdminNotes(reason);

        ContestHostingRequest updatedRequest = hostingRequestRepository.save(request);
        log.info("Revoked hosting privileges for request {} by admin {}", requestId, adminId);

        // Log audit event
        auditService.logHostingRequestRevoked(adminId, requestId, request.getUser().getId(), reason);

        // Send revocation email to user
        try {
            sendRevocationEmail(updatedRequest);
        } catch (Exception e) {
            log.error("Failed to send revocation email for request {}: {}", requestId, e.getMessage());
            // Non-fatal - request was revoked successfully
        }

        return convertToDTO(updatedRequest);
    }

    /**
     * Get all hosting requests by status.
     * 
     * @param status The status to filter by (PENDING, APPROVED, REJECTED, REVOKED)
     * @return List of HostingRequestDTOs matching the status
     */
    @Transactional(readOnly = true)
    public List<HostingRequestDTO> getRequestsByStatus(HostingRequestStatus status) {
        return hostingRequestRepository.findByStatus(status).stream()
                .map(this::convertToDTO)
                .toList();
    }

    /**
     * Get a user's hosting request status.
     * 
     * @param userId The user ID
     * @return HostingRequestDTO if found, null otherwise
     */
    @Transactional(readOnly = true)
    public HostingRequestDTO getUserRequestStatus(Long userId) {
        // Check for APPROVED first, then PENDING, then REJECTED
        return hostingRequestRepository.findByUserIdAndStatus(userId, HostingRequestStatus.APPROVED)
                .or(() -> hostingRequestRepository.findByUserIdAndStatus(userId, HostingRequestStatus.PENDING))
                .or(() -> hostingRequestRepository.findByUserIdAndStatus(userId, HostingRequestStatus.REJECTED))
                .map(this::convertToDTO)
                .orElse(null);
    }

    /**
     * Check if a user is an approved contest host.
     * 
     * @param userId The user ID
     * @return true if the user has an APPROVED hosting request
     */
    @Transactional(readOnly = true)
    public boolean isApprovedHost(Long userId) {
        return hostingRequestRepository.existsByUserIdAndStatus(userId, HostingRequestStatus.APPROVED);
    }

    // ─── Private Helper Methods ───────────────────────────────────────────────

    /**
     * Send email notification to all admins when a new hosting request is submitted.
     * 
     * Requirements: 1.5
     */
    private void sendAdminNotificationEmail(ContestHostingRequest request) {
        // TODO: Implement when admin email list is available
        // For now, log the notification
        log.info("Admin notification email would be sent for request {}", request.getId());
        
        // Placeholder for future implementation:
        // List<User> admins = userRepository.findByRole(RoleType.ADMIN);
        // for (User admin : admins) {
        //     emailService.sendHostingRequestNotification(admin.getEmail(), request);
        // }
    }

    /**
     * Send approval email to the user who submitted the hosting request.
     * 
     * Requirements: 2.4
     */
    private void sendApprovalEmail(ContestHostingRequest request) {
        // TODO: Implement when email template is available
        log.info("Approval email would be sent to user {} for request {}", 
                request.getUser().getId(), request.getId());
        
        // Placeholder for future implementation:
        // String email = request.getUser().getEmail();
        // String link = appUrl + "/private-contests/create";
        // emailService.sendHostingApprovalEmail(email, request.getUser().getFullName(), link);
    }

    /**
     * Send rejection email to the user who submitted the hosting request.
     * 
     * Requirements: 2.3
     */
    private void sendRejectionEmail(ContestHostingRequest request) {
        // TODO: Implement when email template is available
        log.info("Rejection email would be sent to user {} for request {}", 
                request.getUser().getId(), request.getId());
        
        // Placeholder for future implementation:
        // String email = request.getUser().getEmail();
        // emailService.sendHostingRejectionEmail(email, request.getUser().getFullName(), 
        //                                        request.getAdminNotes());
    }

    /**
     * Send revocation email to the user whose hosting privileges were revoked.
     * 
     * Requirements: 19.5
     */
    private void sendRevocationEmail(ContestHostingRequest request) {
        // TODO: Implement when email template is available
        log.info("Revocation email would be sent to user {} for request {}", 
                request.getUser().getId(), request.getId());
        
        // Placeholder for future implementation:
        // String email = request.getUser().getEmail();
        // emailService.sendHostingRevocationEmail(email, request.getUser().getFullName(), 
        //                                         request.getAdminNotes());
    }

    /**
     * Convert entity to DTO.
     */
    private HostingRequestDTO convertToDTO(ContestHostingRequest request) {
        HostingRequestDTO dto = new HostingRequestDTO();
        dto.setId(request.getId());
        dto.setUserId(request.getUser().getId());
        dto.setReason(request.getReason());
        dto.setIntendedUseCase(request.getIntendedUseCase());
        dto.setStatus(request.getStatus());
        dto.setSubmittedAt(request.getSubmittedAt());
        dto.setReviewedBy(request.getReviewedBy() != null ? request.getReviewedBy().getId() : null);
        dto.setReviewedAt(request.getReviewedAt());
        dto.setAdminNotes(request.getAdminNotes());
        return dto;
    }
}
