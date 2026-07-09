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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ContestHostingService.
 * 
 * Tests cover:
 * - Request submission with validations
 * - Approval workflow
 * - Rejection workflow
 * - Status queries
 * - Error handling
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ContestHostingService Tests")
class ContestHostingServiceTest {

    @Mock
    private ContestHostingRequestRepository hostingRequestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private ContestHostingService contestHostingService;

    private User testUser;
    private User adminUser;
    private ContestHostingRequest pendingRequest;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setFullName("Test User");

        // Create admin user
        adminUser = new User();
        adminUser.setId(2L);
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@example.com");
        adminUser.setFullName("Admin User");

        // Create pending request
        pendingRequest = new ContestHostingRequest();
        pendingRequest.setId(1L);
        pendingRequest.setUser(testUser);
        pendingRequest.setReason("I want to host contests for my students");
        pendingRequest.setIntendedUseCase(IntendedUseCase.EDUCATION);
        pendingRequest.setStatus(HostingRequestStatus.PENDING);
        pendingRequest.setSubmittedAt(LocalDateTime.now());
    }

    // ─── Submit Request Tests ─────────────────────────────────────────────────

    @Test
    @DisplayName("Should successfully submit a new hosting request")
    void testSubmitRequest_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(hostingRequestRepository.existsByUserIdAndStatus(1L, HostingRequestStatus.PENDING))
                .thenReturn(false);
        when(hostingRequestRepository.existsByUserIdAndStatus(1L, HostingRequestStatus.APPROVED))
                .thenReturn(false);
        when(hostingRequestRepository.save(any(ContestHostingRequest.class))).thenReturn(pendingRequest);

        // Act
        HostingRequestDTO result = contestHostingService.submitRequest(
                1L, 
                "I want to host contests for my students", 
                IntendedUseCase.EDUCATION
        );

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(1L, result.getUserId());
        assertEquals("I want to host contests for my students", result.getReason());
        assertEquals(IntendedUseCase.EDUCATION, result.getIntendedUseCase());
        assertEquals(HostingRequestStatus.PENDING, result.getStatus());
        assertNotNull(result.getSubmittedAt());

        // Verify repository interactions
        verify(userRepository).findById(1L);
        verify(hostingRequestRepository).existsByUserIdAndStatus(1L, HostingRequestStatus.PENDING);
        verify(hostingRequestRepository).existsByUserIdAndStatus(1L, HostingRequestStatus.APPROVED);
        
        ArgumentCaptor<ContestHostingRequest> requestCaptor = ArgumentCaptor.forClass(ContestHostingRequest.class);
        verify(hostingRequestRepository).save(requestCaptor.capture());
        
        ContestHostingRequest savedRequest = requestCaptor.getValue();
        assertEquals(testUser, savedRequest.getUser());
        assertEquals(HostingRequestStatus.PENDING, savedRequest.getStatus());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when user doesn't exist")
    void testSubmitRequest_UserNotFound() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> contestHostingService.submitRequest(999L, "reason", IntendedUseCase.EDUCATION)
        );

        assertEquals("User not found", exception.getMessage());
        verify(userRepository).findById(999L);
        verify(hostingRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ConflictException when user already has pending request")
    void testSubmitRequest_AlreadyHasPendingRequest() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(hostingRequestRepository.existsByUserIdAndStatus(1L, HostingRequestStatus.PENDING))
                .thenReturn(true);

        // Act & Assert
        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> contestHostingService.submitRequest(1L, "reason", IntendedUseCase.EDUCATION)
        );

        assertEquals("You already have a pending hosting request", exception.getMessage());
        verify(hostingRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ConflictException when user is already approved")
    void testSubmitRequest_AlreadyApproved() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(hostingRequestRepository.existsByUserIdAndStatus(1L, HostingRequestStatus.PENDING))
                .thenReturn(false);
        when(hostingRequestRepository.existsByUserIdAndStatus(1L, HostingRequestStatus.APPROVED))
                .thenReturn(true);

        // Act & Assert
        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> contestHostingService.submitRequest(1L, "reason", IntendedUseCase.EDUCATION)
        );

        assertEquals("You are already approved to host contests", exception.getMessage());
        verify(hostingRequestRepository, never()).save(any());
    }

    // ─── Approve Request Tests ────────────────────────────────────────────────

    @Test
    @DisplayName("Should successfully approve a pending request")
    void testApproveRequest_Success() {
        // Arrange
        when(hostingRequestRepository.findById(1L)).thenReturn(Optional.of(pendingRequest));
        when(userRepository.findById(2L)).thenReturn(Optional.of(adminUser));
        
        ContestHostingRequest approvedRequest = new ContestHostingRequest();
        approvedRequest.setId(1L);
        approvedRequest.setUser(testUser);
        approvedRequest.setStatus(HostingRequestStatus.APPROVED);
        approvedRequest.setReviewedBy(adminUser);
        approvedRequest.setReviewedAt(LocalDateTime.now());
        approvedRequest.setAdminNotes("Approved - verified educational institution");
        
        when(hostingRequestRepository.save(any(ContestHostingRequest.class)))
                .thenReturn(approvedRequest);

        // Act
        HostingRequestDTO result = contestHostingService.approveRequest(
                1L, 
                2L, 
                "Approved - verified educational institution"
        );

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(HostingRequestStatus.APPROVED, result.getStatus());
        assertEquals(2L, result.getReviewedBy());
        assertNotNull(result.getReviewedAt());
        assertEquals("Approved - verified educational institution", result.getAdminNotes());

        // Verify status update
        ArgumentCaptor<ContestHostingRequest> captor = ArgumentCaptor.forClass(ContestHostingRequest.class);
        verify(hostingRequestRepository).save(captor.capture());
        
        ContestHostingRequest saved = captor.getValue();
        assertEquals(HostingRequestStatus.APPROVED, saved.getStatus());
        assertEquals(adminUser, saved.getReviewedBy());
        assertNotNull(saved.getReviewedAt());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when request doesn't exist")
    void testApproveRequest_RequestNotFound() {
        // Arrange
        when(hostingRequestRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> contestHostingService.approveRequest(999L, 2L, "notes")
        );

        assertEquals("Hosting request not found", exception.getMessage());
        verify(hostingRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ConflictException when trying to approve non-pending request")
    void testApproveRequest_NotPending() {
        // Arrange
        ContestHostingRequest approvedRequest = new ContestHostingRequest();
        approvedRequest.setId(1L);
        approvedRequest.setStatus(HostingRequestStatus.APPROVED);
        
        when(hostingRequestRepository.findById(1L)).thenReturn(Optional.of(approvedRequest));

        // Act & Assert
        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> contestHostingService.approveRequest(1L, 2L, "notes")
        );

        assertEquals("Can only approve requests in PENDING status", exception.getMessage());
        verify(hostingRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when admin doesn't exist")
    void testApproveRequest_AdminNotFound() {
        // Arrange
        when(hostingRequestRepository.findById(1L)).thenReturn(Optional.of(pendingRequest));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> contestHostingService.approveRequest(1L, 999L, "notes")
        );

        assertEquals("Admin user not found", exception.getMessage());
        verify(hostingRequestRepository, never()).save(any());
    }

    // ─── Reject Request Tests ─────────────────────────────────────────────────

    @Test
    @DisplayName("Should successfully reject a pending request")
    void testRejectRequest_Success() {
        // Arrange
        when(hostingRequestRepository.findById(1L)).thenReturn(Optional.of(pendingRequest));
        when(userRepository.findById(2L)).thenReturn(Optional.of(adminUser));
        
        ContestHostingRequest rejectedRequest = new ContestHostingRequest();
        rejectedRequest.setId(1L);
        rejectedRequest.setUser(testUser);
        rejectedRequest.setStatus(HostingRequestStatus.REJECTED);
        rejectedRequest.setReviewedBy(adminUser);
        rejectedRequest.setReviewedAt(LocalDateTime.now());
        rejectedRequest.setAdminNotes("Email domain doesn't match organization");
        
        when(hostingRequestRepository.save(any(ContestHostingRequest.class)))
                .thenReturn(rejectedRequest);

        // Act
        HostingRequestDTO result = contestHostingService.rejectRequest(
                1L, 
                2L, 
                "Email domain doesn't match organization"
        );

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(HostingRequestStatus.REJECTED, result.getStatus());
        assertEquals(2L, result.getReviewedBy());
        assertNotNull(result.getReviewedAt());
        assertEquals("Email domain doesn't match organization", result.getAdminNotes());

        // Verify status update
        ArgumentCaptor<ContestHostingRequest> captor = ArgumentCaptor.forClass(ContestHostingRequest.class);
        verify(hostingRequestRepository).save(captor.capture());
        
        ContestHostingRequest saved = captor.getValue();
        assertEquals(HostingRequestStatus.REJECTED, saved.getStatus());
        assertEquals(adminUser, saved.getReviewedBy());
        assertNotNull(saved.getReviewedAt());
    }

    @Test
    @DisplayName("Should throw ConflictException when trying to reject non-pending request")
    void testRejectRequest_NotPending() {
        // Arrange
        ContestHostingRequest rejectedRequest = new ContestHostingRequest();
        rejectedRequest.setId(1L);
        rejectedRequest.setStatus(HostingRequestStatus.REJECTED);
        
        when(hostingRequestRepository.findById(1L)).thenReturn(Optional.of(rejectedRequest));

        // Act & Assert
        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> contestHostingService.rejectRequest(1L, 2L, "notes")
        );

        assertEquals("Can only reject requests in PENDING status", exception.getMessage());
        verify(hostingRequestRepository, never()).save(any());
    }

    // ─── Revoke Approval Tests ────────────────────────────────────────────────

    @Test
    @DisplayName("Should successfully revoke an approved request")
    void testRevokeApproval_Success() {
        // Arrange
        ContestHostingRequest approvedRequest = new ContestHostingRequest();
        approvedRequest.setId(1L);
        approvedRequest.setUser(testUser);
        approvedRequest.setStatus(HostingRequestStatus.APPROVED);
        approvedRequest.setReviewedBy(adminUser);
        approvedRequest.setReviewedAt(LocalDateTime.now().minusDays(10));
        
        when(hostingRequestRepository.findById(1L)).thenReturn(Optional.of(approvedRequest));
        when(userRepository.findById(2L)).thenReturn(Optional.of(adminUser));
        
        ContestHostingRequest revokedRequest = new ContestHostingRequest();
        revokedRequest.setId(1L);
        revokedRequest.setUser(testUser);
        revokedRequest.setStatus(HostingRequestStatus.REVOKED);
        revokedRequest.setReviewedBy(adminUser);
        revokedRequest.setReviewedAt(LocalDateTime.now());
        revokedRequest.setAdminNotes("Policy violation detected");
        
        when(hostingRequestRepository.save(any(ContestHostingRequest.class)))
                .thenReturn(revokedRequest);

        // Act
        HostingRequestDTO result = contestHostingService.revokeApproval(
                1L, 
                2L, 
                "Policy violation detected"
        );

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(HostingRequestStatus.REVOKED, result.getStatus());
        assertEquals(2L, result.getReviewedBy());
        assertNotNull(result.getReviewedAt());
        assertEquals("Policy violation detected", result.getAdminNotes());

        // Verify status update
        ArgumentCaptor<ContestHostingRequest> captor = ArgumentCaptor.forClass(ContestHostingRequest.class);
        verify(hostingRequestRepository).save(captor.capture());
        
        ContestHostingRequest saved = captor.getValue();
        assertEquals(HostingRequestStatus.REVOKED, saved.getStatus());
        assertEquals(adminUser, saved.getReviewedBy());
        assertNotNull(saved.getReviewedAt());
    }

    @Test
    @DisplayName("Should throw ConflictException when trying to revoke non-approved request")
    void testRevokeApproval_NotApproved() {
        // Arrange
        ContestHostingRequest pendingRequest = new ContestHostingRequest();
        pendingRequest.setId(1L);
        pendingRequest.setStatus(HostingRequestStatus.PENDING);
        
        when(hostingRequestRepository.findById(1L)).thenReturn(Optional.of(pendingRequest));

        // Act & Assert
        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> contestHostingService.revokeApproval(1L, 2L, "reason")
        );

        assertEquals("Can only revoke requests in APPROVED status", exception.getMessage());
        verify(hostingRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when trying to revoke non-existent request")
    void testRevokeApproval_RequestNotFound() {
        // Arrange
        when(hostingRequestRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> contestHostingService.revokeApproval(999L, 2L, "reason")
        );

        assertEquals("Hosting request not found", exception.getMessage());
        verify(hostingRequestRepository, never()).save(any());
    }

    // ─── Query Tests ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return all requests by status")
    void testGetRequestsByStatus() {
        // Arrange
        ContestHostingRequest request1 = new ContestHostingRequest();
        request1.setId(1L);
        request1.setUser(testUser);
        request1.setStatus(HostingRequestStatus.PENDING);
        
        ContestHostingRequest request2 = new ContestHostingRequest();
        request2.setId(2L);
        request2.setUser(testUser);
        request2.setStatus(HostingRequestStatus.PENDING);
        
        when(hostingRequestRepository.findByStatus(HostingRequestStatus.PENDING))
                .thenReturn(List.of(request1, request2));

        // Act
        List<HostingRequestDTO> results = contestHostingService.getRequestsByStatus(HostingRequestStatus.PENDING);

        // Assert
        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals(HostingRequestStatus.PENDING, results.get(0).getStatus());
        assertEquals(HostingRequestStatus.PENDING, results.get(1).getStatus());
    }

    @Test
    @DisplayName("Should return user's request status when approved")
    void testGetUserRequestStatus_Approved() {
        // Arrange
        ContestHostingRequest approvedRequest = new ContestHostingRequest();
        approvedRequest.setId(1L);
        approvedRequest.setUser(testUser);
        approvedRequest.setStatus(HostingRequestStatus.APPROVED);
        
        when(hostingRequestRepository.findByUserIdAndStatus(1L, HostingRequestStatus.APPROVED))
                .thenReturn(Optional.of(approvedRequest));

        // Act
        HostingRequestDTO result = contestHostingService.getUserRequestStatus(1L);

        // Assert
        assertNotNull(result);
        assertEquals(HostingRequestStatus.APPROVED, result.getStatus());
    }

    @Test
    @DisplayName("Should return null when user has no request")
    void testGetUserRequestStatus_NoRequest() {
        // Arrange
        when(hostingRequestRepository.findByUserIdAndStatus(anyLong(), any()))
                .thenReturn(Optional.empty());

        // Act
        HostingRequestDTO result = contestHostingService.getUserRequestStatus(1L);

        // Assert
        assertNull(result);
    }

    @Test
    @DisplayName("Should return true when user is approved host")
    void testIsApprovedHost_True() {
        // Arrange
        when(hostingRequestRepository.existsByUserIdAndStatus(1L, HostingRequestStatus.APPROVED))
                .thenReturn(true);

        // Act
        boolean result = contestHostingService.isApprovedHost(1L);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Should return false when user is not approved host")
    void testIsApprovedHost_False() {
        // Arrange
        when(hostingRequestRepository.existsByUserIdAndStatus(1L, HostingRequestStatus.APPROVED))
                .thenReturn(false);

        // Act
        boolean result = contestHostingService.isApprovedHost(1L);

        // Assert
        assertFalse(result);
    }

    // ─── Edge Cases ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should handle null admin notes in approval")
    void testApproveRequest_NullAdminNotes() {
        // Arrange
        when(hostingRequestRepository.findById(1L)).thenReturn(Optional.of(pendingRequest));
        when(userRepository.findById(2L)).thenReturn(Optional.of(adminUser));
        
        ContestHostingRequest approvedRequest = new ContestHostingRequest();
        approvedRequest.setId(1L);
        approvedRequest.setUser(testUser);
        approvedRequest.setStatus(HostingRequestStatus.APPROVED);
        approvedRequest.setReviewedBy(adminUser);
        approvedRequest.setReviewedAt(LocalDateTime.now());
        approvedRequest.setAdminNotes(null);
        
        when(hostingRequestRepository.save(any(ContestHostingRequest.class)))
                .thenReturn(approvedRequest);

        // Act
        HostingRequestDTO result = contestHostingService.approveRequest(1L, 2L, null);

        // Assert
        assertNotNull(result);
        assertNull(result.getAdminNotes());
        verify(hostingRequestRepository).save(any(ContestHostingRequest.class));
    }

    @Test
    @DisplayName("Should handle different intended use cases")
    void testSubmitRequest_DifferentUseCases() {
        // Test each use case
        IntendedUseCase[] useCases = {
            IntendedUseCase.EDUCATION,
            IntendedUseCase.RECRUITMENT,
            IntendedUseCase.COMMUNITY,
            IntendedUseCase.INTERNAL_TRAINING,
            IntendedUseCase.OTHER
        };

        for (IntendedUseCase useCase : useCases) {
            // Arrange
            reset(hostingRequestRepository, userRepository);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(hostingRequestRepository.existsByUserIdAndStatus(anyLong(), any())).thenReturn(false);
            
            ContestHostingRequest mockRequest = new ContestHostingRequest();
            mockRequest.setId(1L);
            mockRequest.setUser(testUser);
            mockRequest.setIntendedUseCase(useCase);
            mockRequest.setStatus(HostingRequestStatus.PENDING);
            
            when(hostingRequestRepository.save(any())).thenReturn(mockRequest);

            // Act
            HostingRequestDTO result = contestHostingService.submitRequest(1L, "reason", useCase);

            // Assert
            assertEquals(useCase, result.getIntendedUseCase());
        }
    }
}
