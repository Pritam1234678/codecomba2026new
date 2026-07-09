package com.example.codecombat2026.service;

import com.example.codecombat2026.dto.PrivateContestDTO;
import com.example.codecombat2026.entity.Contest;
import com.example.codecombat2026.entity.PrivateContest;
import com.example.codecombat2026.entity.PrivateContestInvitation;
import com.example.codecombat2026.entity.User;
import com.example.codecombat2026.exception.ConflictException;
import com.example.codecombat2026.exception.ForbiddenException;
import com.example.codecombat2026.exception.ResourceNotFoundException;
import com.example.codecombat2026.proctoring.entity.ProctoredContest;
import com.example.codecombat2026.proctoring.repository.ProctoredContestRepository;
import com.example.codecombat2026.repository.ContestRepository;
import com.example.codecombat2026.repository.PrivateContestRepository;
import com.example.codecombat2026.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PrivateContestService.
 * 
 * Tests core business logic methods:
 * - createPrivateContest(): Contest creation with business rule validation
 * - cancelPrivateContest(): Host cancellation flow
 * - getPrivateContestById(): Retrieve contest details
 * - getHostContests(): List all contests for a host
 * - updateContestDetails(): Update contest before it starts
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PrivateContestService Tests")
class PrivateContestServiceTest {

    @Mock
    private PrivateContestRepository privateContestRepository;

    @Mock
    private ContestRepository contestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PrivateContestBusinessRules businessRules;

    @Mock
    private InviteTokenService inviteTokenService;

    @Mock
    private ContestHostingService contestHostingService;

    @Mock
    private PrivateContestCacheService cacheService;

    @Mock
    private ProctoredContestRepository proctoredContestRepository;

    @InjectMocks
    private PrivateContestService privateContestService;

    private User mockHost;
    private Contest mockContest;
    private PrivateContest mockPrivateContest;
    private PrivateContestInvitation mockInvitation;
    private PrivateContestDTO mockDTO;

    @BeforeEach
    void setUp() {
        // Set up mock user (Contest_Host)
        mockHost = new User();
        mockHost.setId(1L);
        mockHost.setUsername("testhost");
        mockHost.setEmail("host@example.com");

        // Set up mock contest
        mockContest = new Contest();
        mockContest.setId(100L);
        mockContest.setName("Test Contest");
        mockContest.setDescription("A test contest");
        mockContest.setStartTime(LocalDateTime.now().plusDays(7));
        mockContest.setEndTime(LocalDateTime.now().plusDays(7).plusHours(3));
        mockContest.setStatus(Contest.ContestStatus.UPCOMING);
        mockContest.setActive(false);

        // Set up mock private contest
        mockPrivateContest = new PrivateContest();
        mockPrivateContest.setId(10L);
        mockPrivateContest.setContest(mockContest);
        mockPrivateContest.setHostUser(mockHost);
        mockPrivateContest.setEnableProctoring(false);
        mockPrivateContest.setCancelled(false);
        mockPrivateContest.setCreatedAt(LocalDateTime.now());

        // Set up mock invitation
        mockInvitation = new PrivateContestInvitation();
        mockInvitation.setId(1L);
        mockInvitation.setContest(mockContest);
        mockInvitation.setToken("testtoken123");
        mockInvitation.setCreatedAt(LocalDateTime.now());
        mockInvitation.setExpiresAt(LocalDateTime.now().plusDays(30));
        mockInvitation.setInvalidated(false);

        // Set up mock DTO
        mockDTO = new PrivateContestDTO();
        mockDTO.setName("Test Contest");
        mockDTO.setDescription("A test contest");
        mockDTO.setStartTime(LocalDateTime.now().plusDays(7));
        mockDTO.setEndTime(LocalDateTime.now().plusDays(7).plusHours(3));
        mockDTO.setEnableProctoring(false);
    }

    // ────────────────────────────────────────────────────────────────────────
    // createPrivateContest() Tests
    // ────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createPrivateContest - Success with all validations passing")
    void testCreatePrivateContest_Success() {
        // Arrange
        Long hostUserId = 1L;

        when(contestHostingService.isApprovedHost(hostUserId)).thenReturn(true);
        when(userRepository.findById(hostUserId)).thenReturn(Optional.of(mockHost));
        doNothing().when(businessRules).validateContestCreation(eq(hostUserId), any(), any());
        when(contestRepository.save(any(Contest.class))).thenReturn(mockContest);
        when(privateContestRepository.save(any(PrivateContest.class))).thenReturn(mockPrivateContest);
        when(inviteTokenService.createInvitation(eq(mockContest), any())).thenReturn(mockInvitation);

        // Act
        PrivateContestDTO result = privateContestService.createPrivateContest(mockDTO, hostUserId);

        // Assert
        assertNotNull(result);
        assertEquals("Test Contest", result.getName());
        assertEquals(mockContest.getId(), result.getContestId());
        assertEquals(hostUserId, result.getHostUserId());
        assertEquals("testtoken123", result.getInviteToken());
        assertNotNull(result.getInviteLink());
        assertTrue(result.getInviteLink().contains("testtoken123"));

        // Verify interactions
        verify(contestHostingService).isApprovedHost(hostUserId);
        verify(businessRules).validateContestCreation(eq(hostUserId), any(), any());
        verify(contestRepository).save(any(Contest.class));
        verify(privateContestRepository).save(any(PrivateContest.class));
        verify(inviteTokenService).createInvitation(eq(mockContest), any());
    }

    @Test
    @DisplayName("createPrivateContest - Fails when user is not approved Contest_Host")
    void testCreatePrivateContest_NotApprovedHost() {
        // Arrange
        Long hostUserId = 1L;
        when(contestHostingService.isApprovedHost(hostUserId)).thenReturn(false);

        // Act & Assert
        ForbiddenException exception = assertThrows(ForbiddenException.class,
                () -> privateContestService.createPrivateContest(mockDTO, hostUserId));

        assertEquals("User is not an approved Contest_Host", exception.getMessage());

        // Verify no contest was created
        verify(contestRepository, never()).save(any());
        verify(privateContestRepository, never()).save(any());
    }

    @Test
    @DisplayName("createPrivateContest - Fails when user not found")
    void testCreatePrivateContest_UserNotFound() {
        // Arrange
        Long hostUserId = 999L;
        when(contestHostingService.isApprovedHost(hostUserId)).thenReturn(true);
        when(userRepository.findById(hostUserId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> privateContestService.createPrivateContest(mockDTO, hostUserId));

        verify(contestRepository, never()).save(any());
    }

    @Test
    @DisplayName("createPrivateContest - Enables proctoring when flag is true")
    void testCreatePrivateContest_WithProctoring() {
        // Arrange
        Long hostUserId = 1L;
        mockDTO.setEnableProctoring(true);

        when(contestHostingService.isApprovedHost(hostUserId)).thenReturn(true);
        when(userRepository.findById(hostUserId)).thenReturn(Optional.of(mockHost));
        doNothing().when(businessRules).validateContestCreation(eq(hostUserId), any(), any());
        when(contestRepository.save(any(Contest.class))).thenReturn(mockContest);
        when(privateContestRepository.save(any(PrivateContest.class))).thenAnswer(invocation -> {
            PrivateContest pc = invocation.getArgument(0);
            pc.setId(10L);
            return pc;
        });
        when(inviteTokenService.createInvitation(eq(mockContest), any())).thenReturn(mockInvitation);

        // Act
        PrivateContestDTO result = privateContestService.createPrivateContest(mockDTO, hostUserId);

        // Assert
        assertNotNull(result);
        assertTrue(result.getEnableProctoring());

        // Verify proctoring was set on saved entity
        verify(privateContestRepository).save(argThat(pc -> pc.getEnableProctoring()));
    }

    @Test
    @DisplayName("createPrivateContest - Creates ProctoredContest entity when proctoring enabled")
    void testCreatePrivateContest_CreatesProctoredContestEntity() {
        // Arrange
        Long hostUserId = 1L;
        mockDTO.setEnableProctoring(true);

        when(contestHostingService.isApprovedHost(hostUserId)).thenReturn(true);
        when(userRepository.findById(hostUserId)).thenReturn(Optional.of(mockHost));
        doNothing().when(businessRules).validateContestCreation(eq(hostUserId), any(), any());
        when(contestRepository.save(any(Contest.class))).thenReturn(mockContest);
        when(privateContestRepository.save(any(PrivateContest.class))).thenAnswer(invocation -> {
            PrivateContest pc = invocation.getArgument(0);
            pc.setId(10L);
            mockPrivateContest.setEnableProctoring(true);
            return mockPrivateContest;
        });
        when(inviteTokenService.createInvitation(eq(mockContest), any())).thenReturn(mockInvitation);
        when(proctoredContestRepository.save(any(ProctoredContest.class))).thenAnswer(invocation -> {
            ProctoredContest proctoredContest = invocation.getArgument(0);
            proctoredContest.setId(1L);
            return proctoredContest;
        });
        doNothing().when(cacheService).cacheContestMetadata(any(), any());

        // Act
        PrivateContestDTO result = privateContestService.createPrivateContest(mockDTO, hostUserId);

        // Assert
        assertNotNull(result);
        assertTrue(result.getEnableProctoring());

        // Verify ProctoredContest entity was created with correct fields
        verify(proctoredContestRepository).save(argThat(proctored -> 
            proctored.getContestId().equals(mockContest.getId()) &&
            proctored.getConsentVersion() == 1 &&
            proctored.getCreatedAt() != null
        ));
    }

    @Test
    @DisplayName("createPrivateContest - Does not create ProctoredContest when proctoring disabled")
    void testCreatePrivateContest_NoProctoredContestWhenDisabled() {
        // Arrange
        Long hostUserId = 1L;
        mockDTO.setEnableProctoring(false);

        when(contestHostingService.isApprovedHost(hostUserId)).thenReturn(true);
        when(userRepository.findById(hostUserId)).thenReturn(Optional.of(mockHost));
        doNothing().when(businessRules).validateContestCreation(eq(hostUserId), any(), any());
        when(contestRepository.save(any(Contest.class))).thenReturn(mockContest);
        when(privateContestRepository.save(any(PrivateContest.class))).thenReturn(mockPrivateContest);
        when(inviteTokenService.createInvitation(eq(mockContest), any())).thenReturn(mockInvitation);
        doNothing().when(cacheService).cacheContestMetadata(any(), any());

        // Act
        PrivateContestDTO result = privateContestService.createPrivateContest(mockDTO, hostUserId);

        // Assert
        assertNotNull(result);
        assertFalse(result.getEnableProctoring());

        // Verify ProctoredContest entity was NOT created
        verify(proctoredContestRepository, never()).save(any(ProctoredContest.class));
    }

    // ────────────────────────────────────────────────────────────────────────
    // cancelPrivateContest() Tests
    // ────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("cancelPrivateContest - Success when contest is UPCOMING")
    void testCancelPrivateContest_Success() {
        // Arrange
        Long contestId = 100L;
        Long hostUserId = 1L;
        String reason = "Unexpected conflict";

        when(privateContestRepository.findByContestId(contestId))
                .thenReturn(Optional.of(mockPrivateContest));
        when(privateContestRepository.save(any(PrivateContest.class)))
                .thenReturn(mockPrivateContest);
        doNothing().when(inviteTokenService).invalidateAllTokensForContest(contestId);

        // Act
        PrivateContestDTO result = privateContestService.cancelPrivateContest(contestId, hostUserId, reason);

        // Assert
        assertNotNull(result);
        assertTrue(result.getCancelled());
        assertNotNull(result.getCancelledAt());
        assertEquals(reason, result.getCancellationReason());

        // Verify interactions
        verify(privateContestRepository).findByContestId(contestId);
        verify(privateContestRepository).save(argThat(pc -> 
            pc.getCancelled() && 
            pc.getCancellationReason().equals(reason) &&
            pc.getCancelledAt() != null
        ));
        verify(inviteTokenService).invalidateAllTokensForContest(contestId);
    }

    @Test
    @DisplayName("cancelPrivateContest - Fails when user is not the host")
    void testCancelPrivateContest_NotHost() {
        // Arrange
        Long contestId = 100L;
        Long wrongUserId = 999L; // Not the host
        String reason = "Test reason";

        when(privateContestRepository.findByContestId(contestId))
                .thenReturn(Optional.of(mockPrivateContest));

        // Act & Assert
        ForbiddenException exception = assertThrows(ForbiddenException.class,
                () -> privateContestService.cancelPrivateContest(contestId, wrongUserId, reason));

        assertEquals("Only the contest host can cancel this contest", exception.getMessage());

        // Verify no changes were made
        verify(privateContestRepository, never()).save(any());
        verify(inviteTokenService, never()).invalidateAllTokensForContest(any());
    }

    @Test
    @DisplayName("cancelPrivateContest - Fails when contest has already started")
    void testCancelPrivateContest_ContestLive() {
        // Arrange
        Long contestId = 100L;
        Long hostUserId = 1L;
        String reason = "Test reason";

        mockContest.setStatus(Contest.ContestStatus.LIVE);
        when(privateContestRepository.findByContestId(contestId))
                .thenReturn(Optional.of(mockPrivateContest));

        // Act & Assert
        ConflictException exception = assertThrows(ConflictException.class,
                () -> privateContestService.cancelPrivateContest(contestId, hostUserId, reason));

        assertEquals("Cannot cancel a contest that has already started", exception.getMessage());

        // Verify no changes were made
        verify(privateContestRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancelPrivateContest - Fails when contest not found")
    void testCancelPrivateContest_NotFound() {
        // Arrange
        Long contestId = 999L;
        Long hostUserId = 1L;
        when(privateContestRepository.findByContestId(contestId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> privateContestService.cancelPrivateContest(contestId, hostUserId, "reason"));
    }

    // ────────────────────────────────────────────────────────────────────────
    // getPrivateContestById() Tests
    // ────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getPrivateContestById - Success when contest exists")
    void testGetPrivateContestById_Success() {
        // Arrange
        Long contestId = 100L;
        when(privateContestRepository.findByContestId(contestId))
                .thenReturn(Optional.of(mockPrivateContest));

        // Act
        PrivateContestDTO result = privateContestService.getPrivateContestById(contestId);

        // Assert
        assertNotNull(result);
        assertEquals(mockContest.getId(), result.getContestId());
        assertEquals("Test Contest", result.getName());
        assertEquals(mockHost.getId(), result.getHostUserId());
        assertEquals(mockHost.getUsername(), result.getHostUsername());

        verify(privateContestRepository).findByContestId(contestId);
    }

    @Test
    @DisplayName("getPrivateContestById - Fails when contest not found")
    void testGetPrivateContestById_NotFound() {
        // Arrange
        Long contestId = 999L;
        when(privateContestRepository.findByContestId(contestId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> privateContestService.getPrivateContestById(contestId));
    }

    // ────────────────────────────────────────────────────────────────────────
    // getHostContests() Tests
    // ────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getHostContests - Returns all contests for a host")
    void testGetHostContests_Success() {
        // Arrange
        Long hostUserId = 1L;

        PrivateContest contest2 = new PrivateContest();
        Contest baseContest2 = new Contest();
        baseContest2.setId(101L);
        baseContest2.setName("Contest 2");
        baseContest2.setStartTime(LocalDateTime.now().plusDays(14));
        baseContest2.setEndTime(LocalDateTime.now().plusDays(14).plusHours(2));
        baseContest2.setStatus(Contest.ContestStatus.UPCOMING);
        contest2.setId(11L);
        contest2.setContest(baseContest2);
        contest2.setHostUser(mockHost);
        contest2.setEnableProctoring(true);
        contest2.setCancelled(false);

        List<PrivateContest> contests = Arrays.asList(mockPrivateContest, contest2);
        when(privateContestRepository.findByHostUserId(hostUserId)).thenReturn(contests);

        // Act
        List<PrivateContestDTO> results = privateContestService.getHostContests(hostUserId);

        // Assert
        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals("Test Contest", results.get(0).getName());
        assertEquals("Contest 2", results.get(1).getName());

        verify(privateContestRepository).findByHostUserId(hostUserId);
    }

    @Test
    @DisplayName("getHostContests - Returns empty list when host has no contests")
    void testGetHostContests_Empty() {
        // Arrange
        Long hostUserId = 1L;
        when(privateContestRepository.findByHostUserId(hostUserId)).thenReturn(List.of());

        // Act
        List<PrivateContestDTO> results = privateContestService.getHostContests(hostUserId);

        // Assert
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    // ────────────────────────────────────────────────────────────────────────
    // updateContestDetails() Tests
    // ────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateContestDetails - Success updating name and description")
    void testUpdateContestDetails_BasicFields() {
        // Arrange
        Long contestId = 100L;
        Long hostUserId = 1L;

        PrivateContestDTO updateDTO = new PrivateContestDTO();
        updateDTO.setName("Updated Contest Name");
        updateDTO.setDescription("Updated description");

        when(privateContestRepository.findByContestId(contestId))
                .thenReturn(Optional.of(mockPrivateContest));
        when(contestRepository.save(any(Contest.class))).thenReturn(mockContest);

        // Act
        PrivateContestDTO result = privateContestService.updateContestDetails(contestId, updateDTO, hostUserId);

        // Assert
        assertNotNull(result);
        verify(contestRepository).save(argThat(contest ->
            contest.getName().equals("Updated Contest Name") &&
            contest.getDescription().equals("Updated description")
        ));
    }

    @Test
    @DisplayName("updateContestDetails - Success updating times with validation")
    void testUpdateContestDetails_Times() {
        // Arrange
        Long contestId = 100L;
        Long hostUserId = 1L;

        LocalDateTime newStartTime = LocalDateTime.now().plusDays(10);
        LocalDateTime newEndTime = LocalDateTime.now().plusDays(10).plusHours(4);

        PrivateContestDTO updateDTO = new PrivateContestDTO();
        updateDTO.setStartTime(newStartTime);
        updateDTO.setEndTime(newEndTime);

        when(privateContestRepository.findByContestId(contestId))
                .thenReturn(Optional.of(mockPrivateContest));
        when(privateContestRepository.findByHostUserId(hostUserId))
                .thenReturn(Arrays.asList(mockPrivateContest));
        doNothing().when(businessRules).validateDuration(newStartTime, newEndTime);
        when(contestRepository.save(any(Contest.class))).thenReturn(mockContest);

        // Act
        PrivateContestDTO result = privateContestService.updateContestDetails(contestId, updateDTO, hostUserId);

        // Assert
        assertNotNull(result);
        verify(businessRules).validateDuration(newStartTime, newEndTime);
        verify(contestRepository).save(any(Contest.class));
    }

    @Test
    @DisplayName("updateContestDetails - Fails when user is not the host")
    void testUpdateContestDetails_NotHost() {
        // Arrange
        Long contestId = 100L;
        Long wrongUserId = 999L;
        PrivateContestDTO updateDTO = new PrivateContestDTO();
        updateDTO.setName("New Name");

        when(privateContestRepository.findByContestId(contestId))
                .thenReturn(Optional.of(mockPrivateContest));

        // Act & Assert
        ForbiddenException exception = assertThrows(ForbiddenException.class,
                () -> privateContestService.updateContestDetails(contestId, updateDTO, wrongUserId));

        assertEquals("Only the contest host can update this contest", exception.getMessage());
        verify(contestRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateContestDetails - Fails when contest has already started")
    void testUpdateContestDetails_ContestLive() {
        // Arrange
        Long contestId = 100L;
        Long hostUserId = 1L;
        mockContest.setStatus(Contest.ContestStatus.LIVE);

        PrivateContestDTO updateDTO = new PrivateContestDTO();
        updateDTO.setName("New Name");

        when(privateContestRepository.findByContestId(contestId))
                .thenReturn(Optional.of(mockPrivateContest));

        // Act & Assert
        ConflictException exception = assertThrows(ConflictException.class,
                () -> privateContestService.updateContestDetails(contestId, updateDTO, hostUserId));

        assertEquals("Cannot update a contest that has already started", exception.getMessage());
        verify(contestRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateContestDetails - Detects overlap when updating times")
    void testUpdateContestDetails_OverlapDetection() {
        // Arrange
        Long contestId = 100L;
        Long hostUserId = 1L;

        // Create another contest that will overlap
        PrivateContest otherContest = new PrivateContest();
        Contest otherBase = new Contest();
        otherBase.setId(101L);
        otherBase.setName("Other Contest");
        otherBase.setStartTime(LocalDateTime.now().plusDays(12));
        otherBase.setEndTime(LocalDateTime.now().plusDays(12).plusHours(3));
        otherBase.setStatus(Contest.ContestStatus.UPCOMING);
        otherContest.setContest(otherBase);
        otherContest.setHostUser(mockHost);
        otherContest.setCancelled(false);

        // Update times that would overlap with the other contest
        LocalDateTime newStartTime = LocalDateTime.now().plusDays(12).plusHours(1);
        LocalDateTime newEndTime = LocalDateTime.now().plusDays(12).plusHours(4);

        PrivateContestDTO updateDTO = new PrivateContestDTO();
        updateDTO.setStartTime(newStartTime);
        updateDTO.setEndTime(newEndTime);

        when(privateContestRepository.findByContestId(contestId))
                .thenReturn(Optional.of(mockPrivateContest));
        when(privateContestRepository.findByHostUserId(hostUserId))
                .thenReturn(Arrays.asList(mockPrivateContest, otherContest));
        doNothing().when(businessRules).validateDuration(newStartTime, newEndTime);

        // Act & Assert
        ConflictException exception = assertThrows(ConflictException.class,
                () -> privateContestService.updateContestDetails(contestId, updateDTO, hostUserId));

        assertTrue(exception.getMessage().contains("overlap"));
        verify(contestRepository, never()).save(any());
    }
}
