package com.example.codecombat2026.service;

import com.example.codecombat2026.dto.PrivateContestDTO;
import com.example.codecombat2026.entity.Contest;
import com.example.codecombat2026.entity.Contest.ContestStatus;
import com.example.codecombat2026.entity.PrivateContest;
import com.example.codecombat2026.entity.PrivateContestInvitation;
import com.example.codecombat2026.entity.PrivateContestParticipant;
import com.example.codecombat2026.entity.Submission;
import com.example.codecombat2026.entity.User;
import com.example.codecombat2026.exception.ResourceNotFoundException;
import com.example.codecombat2026.repository.ContestRepository;
import com.example.codecombat2026.repository.PrivateContestInvitationRepository;
import com.example.codecombat2026.repository.PrivateContestParticipantRepository;
import com.example.codecombat2026.repository.PrivateContestRepository;
import com.example.codecombat2026.repository.SubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PrivateContestAdminService.
 * 
 * Tests all three main methods:
 * - listAllPrivateContests with filters
 * - getPrivateContestDetails (bypassing access control)
 * - deletePrivateContest with cascade deletes
 * 
 * Requirements: 19.1, 19.2, 19.3
 */
class PrivateContestAdminServiceTest {

    @Mock
    private PrivateContestRepository privateContestRepository;

    @Mock
    private ContestRepository contestRepository;

    @Mock
    private PrivateContestParticipantRepository participantRepository;

    @Mock
    private PrivateContestInvitationRepository invitationRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private PrivateContestCacheService cacheService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private PrivateContestAdminService adminService;

    private User hostUser;
    private Contest contest;
    private PrivateContest privateContest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Create test data
        hostUser = new User();
        hostUser.setId(100L);
        hostUser.setUsername("test_host");
        hostUser.setEmail("host@example.com");

        contest = new Contest();
        contest.setId(1L);
        contest.setName("Test Contest");
        contest.setDescription("A test contest");
        contest.setStartTime(LocalDateTime.now().plusDays(1));
        contest.setEndTime(LocalDateTime.now().plusDays(1).plusHours(3));
        contest.setStatus(ContestStatus.UPCOMING);

        privateContest = new PrivateContest();
        privateContest.setId(10L);
        privateContest.setContest(contest);
        privateContest.setHostUser(hostUser);
        privateContest.setEnableProctoring(false);
        privateContest.setCancelled(false);
        privateContest.setCreatedAt(LocalDateTime.now());
    }

    // ─── Tests for listAllPrivateContests ────────────────────────────────────

    @Test
    void listAllPrivateContests_noFilters_returnsAllContests() {
        // Arrange
        List<PrivateContest> contests = Arrays.asList(privateContest);
        when(privateContestRepository.findAll()).thenReturn(contests);
        when(participantRepository.countByContestId(anyLong())).thenReturn(5L);

        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<PrivateContestDTO> result = adminService.listAllPrivateContests(null, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Test Contest", result.getContent().get(0).getName());
        assertEquals(5L, result.getContent().get(0).getParticipantCount());
        
        verify(privateContestRepository, times(1)).findAll();
        verify(participantRepository, times(1)).countByContestId(1L);
    }

    @Test
    void listAllPrivateContests_withStatusFilter_returnsFilteredContests() {
        // Arrange
        PrivateContest upcomingContest = createPrivateContest(1L, ContestStatus.UPCOMING);
        PrivateContest liveContest = createPrivateContest(2L, ContestStatus.LIVE);
        PrivateContest endedContest = createPrivateContest(3L, ContestStatus.ENDED);

        when(privateContestRepository.findAll()).thenReturn(Arrays.asList(upcomingContest, liveContest, endedContest));
        when(participantRepository.countByContestId(anyLong())).thenReturn(3L);

        PrivateContestAdminService.ContestFilters filters = new PrivateContestAdminService.ContestFilters();
        filters.setStatus(ContestStatus.LIVE);
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<PrivateContestDTO> result = adminService.listAllPrivateContests(filters, pageable);

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals(ContestStatus.LIVE, result.getContent().get(0).getStatus());
    }

    @Test
    void listAllPrivateContests_withHostUserIdFilter_returnsHostContests() {
        // Arrange
        User host1 = createUser(100L, "host1");
        User host2 = createUser(200L, "host2");

        PrivateContest contest1 = createPrivateContestWithHost(1L, host1);
        PrivateContest contest2 = createPrivateContestWithHost(2L, host2);
        PrivateContest contest3 = createPrivateContestWithHost(3L, host1);

        when(privateContestRepository.findAll()).thenReturn(Arrays.asList(contest1, contest2, contest3));
        when(participantRepository.countByContestId(anyLong())).thenReturn(2L);

        PrivateContestAdminService.ContestFilters filters = new PrivateContestAdminService.ContestFilters();
        filters.setHostUserId(100L);
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<PrivateContestDTO> result = adminService.listAllPrivateContests(filters, pageable);

        // Assert
        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().stream().allMatch(dto -> dto.getHostUserId().equals(100L)));
    }

    @Test
    void listAllPrivateContests_withCancelledFilter_returnsCancelledContests() {
        // Arrange
        PrivateContest activeContest = createPrivateContest(1L, ContestStatus.UPCOMING);
        PrivateContest cancelledContest = createPrivateContest(2L, ContestStatus.UPCOMING);
        cancelledContest.setCancelled(true);
        cancelledContest.setCancelledAt(LocalDateTime.now());

        when(privateContestRepository.findAll()).thenReturn(Arrays.asList(activeContest, cancelledContest));
        when(participantRepository.countByContestId(anyLong())).thenReturn(1L);

        PrivateContestAdminService.ContestFilters filters = new PrivateContestAdminService.ContestFilters();
        filters.setCancelled(true);
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<PrivateContestDTO> result = adminService.listAllPrivateContests(filters, pageable);

        // Assert
        assertEquals(1, result.getTotalElements());
        assertTrue(result.getContent().get(0).getCancelled());
    }

    @Test
    void listAllPrivateContests_withDateRangeFilter_returnsContestsInRange() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        
        PrivateContest oldContest = createPrivateContest(1L, ContestStatus.ENDED);
        oldContest.setCreatedAt(now.minusDays(10));
        
        PrivateContest recentContest = createPrivateContest(2L, ContestStatus.UPCOMING);
        recentContest.setCreatedAt(now.minusDays(2));

        when(privateContestRepository.findAll()).thenReturn(Arrays.asList(oldContest, recentContest));
        when(participantRepository.countByContestId(anyLong())).thenReturn(0L);

        PrivateContestAdminService.ContestFilters filters = new PrivateContestAdminService.ContestFilters();
        filters.setCreatedAfter(now.minusDays(5));
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<PrivateContestDTO> result = adminService.listAllPrivateContests(filters, pageable);

        // Assert
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void listAllPrivateContests_pagination_worksCorrectly() {
        // Arrange
        List<PrivateContest> contests = new ArrayList<>();
        for (int i = 1; i <= 25; i++) {
            contests.add(createPrivateContest((long) i, ContestStatus.UPCOMING));
        }

        when(privateContestRepository.findAll()).thenReturn(contests);
        when(participantRepository.countByContestId(anyLong())).thenReturn(1L);

        Pageable pageable = PageRequest.of(1, 10); // Page 2, size 10

        // Act
        Page<PrivateContestDTO> result = adminService.listAllPrivateContests(null, pageable);

        // Assert
        assertEquals(25, result.getTotalElements());
        assertEquals(3, result.getTotalPages());
        assertEquals(10, result.getContent().size());
    }

    // ─── Tests for getPrivateContestDetails ──────────────────────────────────

    @Test
    void getPrivateContestDetails_existingContest_returnsFullDetails() {
        // Arrange
        when(privateContestRepository.findByContestId(1L)).thenReturn(Optional.of(privateContest));
        when(participantRepository.countByContestId(1L)).thenReturn(10L);

        // Act
        PrivateContestDTO result = adminService.getPrivateContestDetails(1L);

        // Assert
        assertNotNull(result);
        assertEquals("Test Contest", result.getName());
        assertEquals(10L, result.getParticipantCount());
        assertEquals(hostUser.getId(), result.getHostUserId());
        assertEquals(hostUser.getUsername(), result.getHostUsername());
        
        verify(privateContestRepository, times(1)).findByContestId(1L);
        verify(participantRepository, times(1)).countByContestId(1L);
    }

    @Test
    void getPrivateContestDetails_nonExistentContest_throwsResourceNotFoundException() {
        // Arrange
        when(privateContestRepository.findByContestId(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            adminService.getPrivateContestDetails(999L);
        });
        
        verify(privateContestRepository, times(1)).findByContestId(999L);
    }

    @Test
    void getPrivateContestDetails_cancelledContest_includesCancellationDetails() {
        // Arrange
        privateContest.setCancelled(true);
        privateContest.setCancelledAt(LocalDateTime.now());
        privateContest.setCancellationReason("Host unavailable");

        when(privateContestRepository.findByContestId(1L)).thenReturn(Optional.of(privateContest));
        when(participantRepository.countByContestId(1L)).thenReturn(5L);

        // Act
        PrivateContestDTO result = adminService.getPrivateContestDetails(1L);

        // Assert
        assertTrue(result.getCancelled());
        assertNotNull(result.getCancelledAt());
        assertEquals("Host unavailable", result.getCancellationReason());
    }

    // ─── Tests for deletePrivateContest ──────────────────────────────────────

    @Test
    void deletePrivateContest_withoutSubmissions_deletesEverythingIncludingBaseContest() {
        // Arrange
        Long contestId = 1L;
        Long adminId = 500L;

        List<PrivateContestParticipant> participants = Arrays.asList(
            createParticipant(1L, contestId, 101L),
            createParticipant(2L, contestId, 102L)
        );

        List<PrivateContestInvitation> invitations = Arrays.asList(
            createInvitation(1L, contestId, "token1"),
            createInvitation(2L, contestId, "token2")
        );

        when(privateContestRepository.findByContestId(contestId)).thenReturn(Optional.of(privateContest));
        when(participantRepository.findByContestId(contestId)).thenReturn(participants);
        when(invitationRepository.findByContestId(contestId)).thenReturn(invitations);
        when(submissionRepository.findByContest_Id(contestId)).thenReturn(new ArrayList<>()); // No submissions

        // Act
        adminService.deletePrivateContest(contestId, adminId);

        // Assert
        verify(participantRepository, times(1)).deleteAll(participants);
        verify(invitationRepository, times(1)).deleteAll(invitations);
        verify(privateContestRepository, times(1)).delete(privateContest);
        verify(contestRepository, times(1)).delete(contest); // Base contest deleted
        verify(cacheService, times(1)).invalidateContestCache(contestId);
        verify(auditService, times(1)).logEvent(eq(adminId), eq("PRIVATE_CONTEST_DELETED"), 
                eq("PRIVATE_CONTEST"), eq(contestId), anyMap());
    }

    @Test
    void deletePrivateContest_withSubmissions_preservesBaseContest() {
        // Arrange
        Long contestId = 1L;
        Long adminId = 500L;

        List<Submission> submissions = Arrays.asList(
            new Submission(), // Dummy submissions
            new Submission()
        );

        when(privateContestRepository.findByContestId(contestId)).thenReturn(Optional.of(privateContest));
        when(participantRepository.findByContestId(contestId)).thenReturn(new ArrayList<>());
        when(invitationRepository.findByContestId(contestId)).thenReturn(new ArrayList<>());
        when(submissionRepository.findByContest_Id(contestId)).thenReturn(submissions); // Has submissions

        // Act
        adminService.deletePrivateContest(contestId, adminId);

        // Assert
        verify(privateContestRepository, times(1)).delete(privateContest);
        verify(contestRepository, never()).delete(contest); // Base contest preserved
        verify(auditService, times(1)).logEvent(eq(adminId), eq("PRIVATE_CONTEST_DELETED"), 
                eq("PRIVATE_CONTEST"), eq(contestId), anyMap());
    }

    @Test
    void deletePrivateContest_nonExistentContest_throwsResourceNotFoundException() {
        // Arrange
        when(privateContestRepository.findByContestId(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            adminService.deletePrivateContest(999L, 500L);
        });
        
        verify(privateContestRepository, times(1)).findByContestId(999L);
        verify(privateContestRepository, never()).delete(any());
        verify(contestRepository, never()).delete(any());
    }

    @Test
    void deletePrivateContest_logsCorrectAuditDetails() {
        // Arrange
        Long contestId = 1L;
        Long adminId = 500L;

        List<PrivateContestParticipant> participants = Arrays.asList(
            createParticipant(1L, contestId, 101L)
        );
        List<PrivateContestInvitation> invitations = Arrays.asList(
            createInvitation(1L, contestId, "token1")
        );

        when(privateContestRepository.findByContestId(contestId)).thenReturn(Optional.of(privateContest));
        when(participantRepository.findByContestId(contestId)).thenReturn(participants);
        when(invitationRepository.findByContestId(contestId)).thenReturn(invitations);
        when(submissionRepository.findByContest_Id(contestId)).thenReturn(new ArrayList<>());

        // Act
        adminService.deletePrivateContest(contestId, adminId);

        // Assert - Verify audit log details
        verify(auditService, times(1)).logEvent(
            eq(adminId),
            eq("PRIVATE_CONTEST_DELETED"),
            eq("PRIVATE_CONTEST"),
            eq(contestId),
            argThat(details -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) details;
                return map.get("contestName").equals("Test Contest") &&
                       map.get("hostUserId").equals(100L) &&
                       map.get("participantCount").equals(1) &&
                       map.get("invitationCount").equals(1);
            })
        );
    }

    // ─── Helper Methods ──────────────────────────────────────────────────────

    private PrivateContest createPrivateContest(Long contestId, ContestStatus status) {
        User host = createUser(100L, "host");
        Contest contest = new Contest();
        contest.setId(contestId);
        contest.setName("Contest " + contestId);
        contest.setDescription("Description");
        contest.setStartTime(LocalDateTime.now().plusDays(1));
        contest.setEndTime(LocalDateTime.now().plusDays(1).plusHours(3));
        contest.setStatus(status);

        PrivateContest pc = new PrivateContest();
        pc.setId(contestId * 10);
        pc.setContest(contest);
        pc.setHostUser(host);
        pc.setEnableProctoring(false);
        pc.setCancelled(false);
        pc.setCreatedAt(LocalDateTime.now());

        return pc;
    }

    private PrivateContest createPrivateContestWithHost(Long contestId, User host) {
        Contest contest = new Contest();
        contest.setId(contestId);
        contest.setName("Contest " + contestId);
        contest.setStatus(ContestStatus.UPCOMING);
        contest.setStartTime(LocalDateTime.now().plusDays(1));
        contest.setEndTime(LocalDateTime.now().plusDays(1).plusHours(3));

        PrivateContest pc = new PrivateContest();
        pc.setId(contestId * 10);
        pc.setContest(contest);
        pc.setHostUser(host);
        pc.setEnableProctoring(false);
        pc.setCancelled(false);
        pc.setCreatedAt(LocalDateTime.now());

        return pc;
    }

    private User createUser(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        return user;
    }

    private PrivateContestParticipant createParticipant(Long id, Long contestId, Long userId) {
        PrivateContestParticipant participant = new PrivateContestParticipant();
        participant.setId(id);
        
        Contest contest = new Contest();
        contest.setId(contestId);
        participant.setContest(contest);
        
        User user = new User();
        user.setId(userId);
        participant.setUser(user);
        
        participant.setJoinedAt(LocalDateTime.now());
        return participant;
    }

    private PrivateContestInvitation createInvitation(Long id, Long contestId, String token) {
        PrivateContestInvitation invitation = new PrivateContestInvitation();
        invitation.setId(id);
        
        Contest contest = new Contest();
        contest.setId(contestId);
        invitation.setContest(contest);
        
        invitation.setToken(token);
        invitation.setCreatedAt(LocalDateTime.now());
        invitation.setExpiresAt(LocalDateTime.now().plusDays(30));
        invitation.setInvalidated(false);
        
        return invitation;
    }
}
