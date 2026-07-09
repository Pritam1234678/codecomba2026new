package com.example.codecombat2026.service;

import com.example.codecombat2026.entity.AuditLog;
import com.example.codecombat2026.entity.User;
import com.example.codecombat2026.repository.AuditLogRepository;
import com.example.codecombat2026.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuditService.
 * 
 * Tests all audit logging methods including:
 * - Core logEvent method with IP and User-Agent extraction
 * - All helper methods for specific events
 * - HTTP request context handling
 * - JSON serialization of details
 * - Error handling and resilience
 */
@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private HttpServletRequest httpRequest;

    @InjectMocks
    private AuditService auditService;

    private User testUser;
    private User adminUser;
    private final Long userId = 42L;
    private final Long adminId = 1L;
    private final Long contestId = 100L;
    private final Long requestId = 50L;

    @BeforeEach
    void setUp() {
        // Set up test users
        testUser = new User();
        testUser.setId(userId);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        adminUser = new User();
        adminUser.setId(adminId);
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@example.com");

        // Clear RequestContextHolder before each test
        RequestContextHolder.resetRequestAttributes();
    }

    // ─── Core logEvent Tests ─────────────────────────────────────────────

    @Test
    void logEvent_WithValidData_CreatesAuditLog() throws Exception {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"key\":\"value\"}");

        Map<String, Object> details = new HashMap<>();
        details.put("key", "value");

        // When
        auditService.logEvent(userId, "TEST_ACTION", "TEST_RESOURCE", 123L, details);

        // Wait briefly for async execution
        Thread.sleep(100);

        // Then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, timeout(500)).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertEquals(testUser, savedLog.getUser());
        assertEquals("TEST_ACTION", savedLog.getAction());
        assertEquals("TEST_RESOURCE", savedLog.getResourceType());
        assertEquals(123L, savedLog.getResourceId());
        assertNotNull(savedLog.getTimestamp());
        assertEquals("{\"key\":\"value\"}", savedLog.getDetailsJson());
    }

    @Test
    void logEvent_WithoutUserId_CreatesAuditLogWithNullUser() throws Exception {
        // Given
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        auditService.logEvent(null, "SYSTEM_ACTION", "SYSTEM", null, null);

        // Wait briefly for async execution
        Thread.sleep(100);

        // Then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, timeout(500)).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertNull(savedLog.getUser());
        assertEquals("SYSTEM_ACTION", savedLog.getAction());
    }

    @Test
    void logEvent_WithHttpContext_ExtractsIpAndUserAgent() throws Exception {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn("192.168.1.100, 10.0.0.1");
        when(httpRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");

        // Set up request context
        ServletRequestAttributes attributes = new ServletRequestAttributes(httpRequest);
        RequestContextHolder.setRequestAttributes(attributes);

        // When
        auditService.logEvent(userId, "TEST_ACTION", "TEST_RESOURCE", null, null);

        // Wait briefly for async execution
        Thread.sleep(100);

        // Then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, timeout(500)).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertEquals("192.168.1.100", savedLog.getIpAddress()); // First IP from X-Forwarded-For
        assertEquals("Mozilla/5.0", savedLog.getUserAgent());

        // Clean up
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void logEvent_WithXRealIpHeader_ExtractsCorrectIp() throws Exception {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(httpRequest.getHeader("X-Real-IP")).thenReturn("192.168.1.200");

        ServletRequestAttributes attributes = new ServletRequestAttributes(httpRequest);
        RequestContextHolder.setRequestAttributes(attributes);

        // When
        auditService.logEvent(userId, "TEST_ACTION", "TEST_RESOURCE", null, null);
        Thread.sleep(100);

        // Then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, timeout(500)).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertEquals("192.168.1.200", savedLog.getIpAddress());

        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void logEvent_WithRemoteAddrOnly_ExtractsCorrectIp() throws Exception {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(httpRequest.getHeader("X-Real-IP")).thenReturn(null);
        when(httpRequest.getRemoteAddr()).thenReturn("10.0.0.50");

        ServletRequestAttributes attributes = new ServletRequestAttributes(httpRequest);
        RequestContextHolder.setRequestAttributes(attributes);

        // When
        auditService.logEvent(userId, "TEST_ACTION", "TEST_RESOURCE", null, null);
        Thread.sleep(100);

        // Then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, timeout(500)).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertEquals("10.0.0.50", savedLog.getIpAddress());

        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void logEvent_WithoutHttpContext_StillCreatesLog() throws Exception {
        // Given - no HTTP context set
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        auditService.logEvent(userId, "TEST_ACTION", "TEST_RESOURCE", null, null);
        Thread.sleep(100);

        // Then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, timeout(500)).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertNull(savedLog.getIpAddress());
        assertNull(savedLog.getUserAgent());
    }

    @Test
    void logEvent_WithJsonSerializationError_CreatesLogWithoutDetails() throws Exception {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(objectMapper.writeValueAsString(any())).thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("JSON error") {});

        Map<String, Object> details = new HashMap<>();
        details.put("key", "value");

        // When
        auditService.logEvent(userId, "TEST_ACTION", "TEST_RESOURCE", null, details);
        Thread.sleep(100);

        // Then - log should still be created, just without details
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, timeout(500)).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertNull(savedLog.getDetailsJson());
        assertEquals("TEST_ACTION", savedLog.getAction());
    }

    @Test
    void logEvent_WithRepositoryError_DoesNotThrowException() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(auditLogRepository.save(any(AuditLog.class))).thenThrow(new RuntimeException("DB error"));

        // When / Then - should not throw exception
        assertDoesNotThrow(() -> {
            auditService.logEvent(userId, "TEST_ACTION", "TEST_RESOURCE", null, null);
            Thread.sleep(100);
        });
    }

    @Test
    void logEvent_WithEmptyDetails_DoesNotSerializeJson() throws Exception {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        auditService.logEvent(userId, "TEST_ACTION", "TEST_RESOURCE", null, new HashMap<>());
        Thread.sleep(100);

        // Then
        verify(objectMapper, never()).writeValueAsString(any());
    }

    // ─── Helper Method Tests ─────────────────────────────────────────────

    @Test
    void logHostingRequestSubmitted_CreatesCorrectAuditLog() throws Exception {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"reason\":\"test\"}");

        // When
        auditService.logHostingRequestSubmitted(userId, requestId, "I want to host", "EDUCATION");
        Thread.sleep(100);

        // Then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, timeout(500)).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertEquals("HOSTING_REQUEST_SUBMITTED", savedLog.getAction());
        assertEquals("HOSTING_REQUEST", savedLog.getResourceType());
        assertEquals(requestId, savedLog.getResourceId());
    }

    @Test
    void logHostingRequestApproved_CreatesCorrectAuditLog() throws Exception {
        // Given
        when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"approvedUserId\":42}");

        // When
        auditService.logHostingRequestApproved(adminId, requestId, userId, "Approved");
        Thread.sleep(100);

        // Then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, timeout(500)).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertEquals("HOSTING_REQUEST_APPROVED", savedLog.getAction());
        assertEquals("HOSTING_REQUEST", savedLog.getResourceType());
        assertEquals(requestId, savedLog.getResourceId());
        assertEquals(adminUser, savedLog.getUser());
    }

    @Test
    void logHostingRequestRejected_CreatesCorrectAuditLog() throws Exception {
        // Given
        when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"rejectedUserId\":42}");

        // When
        auditService.logHostingRequestRejected(adminId, requestId, userId, "Not qualified");
        Thread.sleep(100);

        // Then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, timeout(500)).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertEquals("HOSTING_REQUEST_REJECTED", savedLog.getAction());
        assertEquals("HOSTING_REQUEST", savedLog.getResourceType());
    }

    @Test
    void logContestCreated_CreatesCorrectAuditLog() throws Exception {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"contestName\":\"Test\"}");

        // When
        auditService.logContestCreated(userId, contestId, "Test Contest", 
                                       "2026-02-01T14:00:00Z", "2026-02-01T17:00:00Z", true);
        Thread.sleep(100);

        // Then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, timeout(500)).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertEquals("CONTEST_CREATED", savedLog.getAction());
        assertEquals("PRIVATE_CONTEST", savedLog.getResourceType());
        assertEquals(contestId, savedLog.getResourceId());
    }

    @Test
    void logContestCancelled_CreatesCorrectAuditLog() throws Exception {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"cancellationReason\":\"test\"}");

        // When
        auditService.logContestCancelled(userId, contestId, "Test Contest", "Schedule conflict", 25);
        Thread.sleep(100);

        // Then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, timeout(500)).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertEquals("CONTEST_CANCELLED", savedLog.getAction());
        assertEquals("PRIVATE_CONTEST", savedLog.getResourceType());
        assertEquals(contestId, savedLog.getResourceId());
    }

    @Test
    void logParticipantJoined_CreatesCorrectAuditLog() throws Exception {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"contestName\":\"Test\"}");

        // When
        auditService.logParticipantJoined(userId, contestId, "Test Contest", "token123456");
        Thread.sleep(100);

        // Then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, timeout(500)).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertEquals("PARTICIPANT_JOINED", savedLog.getAction());
        assertEquals("PARTICIPANT", savedLog.getResourceType());
        assertEquals(contestId, savedLog.getResourceId());
    }

    @Test
    void logParticipantJoined_WithNullToken_DoesNotIncludeTokenInDetails() throws Exception {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(objectMapper.writeValueAsString(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> details = invocation.getArgument(0);
            assertFalse(details.containsKey("inviteTokenPrefix"));
            return "{}";
        });

        // When
        auditService.logParticipantJoined(userId, contestId, "Test Contest", null);
        Thread.sleep(100);

        // Then
        verify(objectMapper, timeout(500)).writeValueAsString(any());
    }

    @Test
    void logParticipantRemoved_CreatesCorrectAuditLog() throws Exception {
        // Given
        Long removedUserId = 99L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"removedUserId\":99}");

        // When
        auditService.logParticipantRemoved(userId, removedUserId, contestId, "Test Contest");
        Thread.sleep(100);

        // Then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, timeout(500)).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertEquals("PARTICIPANT_REMOVED", savedLog.getAction());
        assertEquals("PARTICIPANT", savedLog.getResourceType());
        assertEquals(contestId, savedLog.getResourceId());
        assertEquals(testUser, savedLog.getUser());
    }

    @Test
    void logInviteLinkRegenerated_CreatesCorrectAuditLog() throws Exception {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"oldTokenPrefix\":\"abc\"}");

        // When
        auditService.logInviteLinkRegenerated(userId, contestId, "Test Contest", "abc", "xyz");
        Thread.sleep(100);

        // Then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, timeout(500)).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertEquals("INVITE_LINK_REGENERATED", savedLog.getAction());
        assertEquals("INVITATION", savedLog.getResourceType());
        assertEquals(contestId, savedLog.getResourceId());
    }

    // ─── Edge Case Tests ─────────────────────────────────────────────

    @Test
    void logEvent_WithNonExistentUser_StillCreatesLog() throws Exception {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        auditService.logEvent(userId, "TEST_ACTION", "TEST_RESOURCE", null, null);
        Thread.sleep(100);

        // Then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, timeout(500)).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertNull(savedLog.getUser()); // User not set if not found
        assertEquals("TEST_ACTION", savedLog.getAction());
    }

    @Test
    void logEvent_WithBlankXForwardedFor_FallsBackToRealIp() throws Exception {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn("   ");
        when(httpRequest.getHeader("X-Real-IP")).thenReturn("192.168.1.150");

        ServletRequestAttributes attributes = new ServletRequestAttributes(httpRequest);
        RequestContextHolder.setRequestAttributes(attributes);

        // When
        auditService.logEvent(userId, "TEST_ACTION", "TEST_RESOURCE", null, null);
        Thread.sleep(100);

        // Then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, timeout(500)).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertEquals("192.168.1.150", savedLog.getIpAddress());

        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void logEvent_WithMultipleIpsInXForwardedFor_TakesFirst() throws Exception {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1, 192.168.1.1, 10.0.0.1");

        ServletRequestAttributes attributes = new ServletRequestAttributes(httpRequest);
        RequestContextHolder.setRequestAttributes(attributes);

        // When
        auditService.logEvent(userId, "TEST_ACTION", "TEST_RESOURCE", null, null);
        Thread.sleep(100);

        // Then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, timeout(500)).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertEquals("203.0.113.1", savedLog.getIpAddress());

        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void logEvent_WithNullResourceId_CreatesLog() throws Exception {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        auditService.logEvent(userId, "TEST_ACTION", "TEST_RESOURCE", null, null);
        Thread.sleep(100);

        // Then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, timeout(500)).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertNull(savedLog.getResourceId());
        assertEquals("TEST_ACTION", savedLog.getAction());
    }
}
