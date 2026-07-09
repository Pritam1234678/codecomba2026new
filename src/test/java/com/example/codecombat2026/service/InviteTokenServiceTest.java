package com.example.codecombat2026.service;

import com.example.codecombat2026.entity.Contest;
import com.example.codecombat2026.entity.PrivateContestInvitation;
import com.example.codecombat2026.repository.PrivateContestInvitationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for InviteTokenService.
 * 
 * Tests all token operations including:
 * - Token generation (format, uniqueness)
 * - Token creation and persistence
 * - Token validation with expiry checking
 * - Token regeneration
 * - Token invalidation
 * - Cache integration
 * - Edge cases and error handling
 */
@ExtendWith(MockitoExtension.class)
class InviteTokenServiceTest {

    @Mock
    private PrivateContestInvitationRepository invitationRepository;

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private InviteTokenService inviteTokenService;

    private Contest contest;
    private PrivateContestInvitation invitation;
    private String validToken;
    private LocalDateTime futureExpiry;
    private LocalDateTime pastExpiry;

    @BeforeEach
    void setUp() {
        // Set up mock Redis operations - using lenient() to avoid UnnecessaryStubbingException
        lenient().when(redis.opsForValue()).thenReturn(valueOperations);

        // Set up test data
        contest = new Contest();
        contest.setId(100L);
        contest.setName("Test Contest");

        validToken = "abcdefghijklmnopqrstuvwxyz0123456789ABCDE";
        futureExpiry = LocalDateTime.now().plusDays(30);
        pastExpiry = LocalDateTime.now().minusDays(1);

        invitation = new PrivateContestInvitation();
        invitation.setId(1L);
        invitation.setContest(contest);
        invitation.setToken(validToken);
        invitation.setCreatedAt(LocalDateTime.now());
        invitation.setExpiresAt(futureExpiry);
        invitation.setInvalidated(false);
    }

    // ─── Token Generation Tests ─────────────────────────────────────────────

    @Test
    void generateToken_ReturnsNonNullToken() {
        // When
        String token = inviteTokenService.generateToken();

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void generateToken_ReturnsBase64UrlEncodedString() {
        // When
        String token = inviteTokenService.generateToken();

        // Then
        // Base64url-encoded 32 bytes should be 43 characters (no padding)
        assertEquals(43, token.length());
        
        // Verify it can be decoded
        assertDoesNotThrow(() -> Base64.getUrlDecoder().decode(token));
    }

    @Test
    void generateToken_ReturnsUniqueTokens() {
        // When
        String token1 = inviteTokenService.generateToken();
        String token2 = inviteTokenService.generateToken();
        String token3 = inviteTokenService.generateToken();

        // Then
        assertNotEquals(token1, token2);
        assertNotEquals(token2, token3);
        assertNotEquals(token1, token3);
    }

    @Test
    void generateToken_DoesNotContainPadding() {
        // When
        String token = inviteTokenService.generateToken();

        // Then
        assertFalse(token.contains("="), "Token should not contain padding characters");
    }

    // ─── Create Invitation Tests ─────────────────────────────────────────────

    @Test
    void createInvitation_WithValidContest_CreatesAndReturnsInvitation() {
        // Given
        when(invitationRepository.save(any(PrivateContestInvitation.class)))
            .thenReturn(invitation);

        // When
        PrivateContestInvitation result = inviteTokenService.createInvitation(contest, futureExpiry);

        // Then
        assertNotNull(result);
        verify(invitationRepository).save(any(PrivateContestInvitation.class));
    }

    @Test
    void createInvitation_SetsCorrectFields() {
        // Given
        ArgumentCaptor<PrivateContestInvitation> captor = ArgumentCaptor.forClass(PrivateContestInvitation.class);
        when(invitationRepository.save(any(PrivateContestInvitation.class)))
            .thenReturn(invitation);

        // When
        inviteTokenService.createInvitation(contest, futureExpiry);

        // Then
        verify(invitationRepository).save(captor.capture());
        PrivateContestInvitation saved = captor.getValue();
        
        assertEquals(contest, saved.getContest());
        assertNotNull(saved.getToken());
        assertNotNull(saved.getCreatedAt());
        assertEquals(futureExpiry, saved.getExpiresAt());
        assertFalse(saved.getInvalidated());
    }

    @Test
    void createInvitation_WithNullExpiry_UsesDefaultExpiry() {
        // Given
        ArgumentCaptor<PrivateContestInvitation> captor = ArgumentCaptor.forClass(PrivateContestInvitation.class);
        when(invitationRepository.save(any(PrivateContestInvitation.class)))
            .thenReturn(invitation);

        // When
        inviteTokenService.createInvitation(contest, null);

        // Then
        verify(invitationRepository).save(captor.capture());
        PrivateContestInvitation saved = captor.getValue();
        
        // Default is 30 days - check it's approximately correct (within 1 minute tolerance)
        LocalDateTime expectedExpiry = LocalDateTime.now().plusDays(30);
        assertTrue(saved.getExpiresAt().isAfter(expectedExpiry.minusMinutes(1)));
        assertTrue(saved.getExpiresAt().isBefore(expectedExpiry.plusMinutes(1)));
    }

    @Test
    void createInvitation_CachesToken() {
        // Given
        when(invitationRepository.save(any(PrivateContestInvitation.class)))
            .thenReturn(invitation);

        // When
        inviteTokenService.createInvitation(contest, futureExpiry);

        // Then
        verify(valueOperations).set(
            anyString(),
            eq(String.valueOf(contest.getId())),
            any(Duration.class)
        );
    }

    @Test
    void createInvitation_WithNullContest_ThrowsException() {
        // When / Then
        assertThrows(IllegalArgumentException.class, () -> {
            inviteTokenService.createInvitation(null, futureExpiry);
        });
    }

    // ─── Validate Token Tests ─────────────────────────────────────────────

    @Test
    void validateToken_WithValidToken_ReturnsInvitation() {
        // Given
        when(invitationRepository.findByToken(validToken))
            .thenReturn(Optional.of(invitation));

        // When
        Optional<PrivateContestInvitation> result = inviteTokenService.validateToken(validToken);

        // Then
        assertTrue(result.isPresent());
        assertEquals(invitation, result.get());
    }

    @Test
    void validateToken_WithNullToken_ReturnsEmpty() {
        // When
        Optional<PrivateContestInvitation> result = inviteTokenService.validateToken(null);

        // Then
        assertTrue(result.isEmpty());
        verifyNoInteractions(invitationRepository);
    }

    @Test
    void validateToken_WithBlankToken_ReturnsEmpty() {
        // When
        Optional<PrivateContestInvitation> result = inviteTokenService.validateToken("   ");

        // Then
        assertTrue(result.isEmpty());
        verifyNoInteractions(invitationRepository);
    }

    @Test
    void validateToken_WithNonExistentToken_ReturnsEmpty() {
        // Given
        when(invitationRepository.findByToken(validToken))
            .thenReturn(Optional.empty());

        // When
        Optional<PrivateContestInvitation> result = inviteTokenService.validateToken(validToken);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void validateToken_WithExpiredToken_ReturnsEmpty() {
        // Given
        invitation.setExpiresAt(pastExpiry);
        when(invitationRepository.findByToken(validToken))
            .thenReturn(Optional.of(invitation));

        // When
        Optional<PrivateContestInvitation> result = inviteTokenService.validateToken(validToken);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void validateToken_WithInvalidatedToken_ReturnsEmpty() {
        // Given
        invitation.setInvalidated(true);
        when(invitationRepository.findByToken(validToken))
            .thenReturn(Optional.of(invitation));

        // When
        Optional<PrivateContestInvitation> result = inviteTokenService.validateToken(validToken);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void validateToken_ChecksCacheFirst() {
        // Given
        when(valueOperations.get("invite:token:" + validToken))
            .thenReturn(String.valueOf(contest.getId()));
        when(invitationRepository.findByToken(validToken))
            .thenReturn(Optional.of(invitation));

        // When
        inviteTokenService.validateToken(validToken);

        // Then
        verify(valueOperations).get("invite:token:" + validToken);
        verify(invitationRepository).findByToken(validToken);
    }

    @Test
    void validateToken_WithCacheMiss_FetchesFromDatabase() {
        // Given
        when(valueOperations.get("invite:token:" + validToken))
            .thenReturn(null);
        when(invitationRepository.findByToken(validToken))
            .thenReturn(Optional.of(invitation));

        // When
        Optional<PrivateContestInvitation> result = inviteTokenService.validateToken(validToken);

        // Then
        assertTrue(result.isPresent());
        verify(invitationRepository).findByToken(validToken);
        // Should cache the result
        verify(valueOperations).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void validateToken_WithStaleCacheAndInvalidToken_EvictsCache() {
        // Given - cache says token is valid, but DB shows it's invalidated
        when(valueOperations.get("invite:token:" + validToken))
            .thenReturn(String.valueOf(contest.getId()));
        invitation.setInvalidated(true);
        when(invitationRepository.findByToken(validToken))
            .thenReturn(Optional.of(invitation));

        // When
        Optional<PrivateContestInvitation> result = inviteTokenService.validateToken(validToken);

        // Then
        assertTrue(result.isEmpty());
        verify(redis).delete("invite:token:" + validToken);
    }

    // ─── Regenerate Token Tests ─────────────────────────────────────────────

    @Test
    void regenerateToken_WithValidOldToken_CreatesNewToken() {
        // Given
        PrivateContestInvitation newInvitation = new PrivateContestInvitation();
        newInvitation.setToken("newToken123456789");
        newInvitation.setContest(contest);
        
        when(invitationRepository.findByToken(validToken))
            .thenReturn(Optional.of(invitation));
        when(invitationRepository.save(any(PrivateContestInvitation.class)))
            .thenReturn(invitation, newInvitation);

        // When
        PrivateContestInvitation result = inviteTokenService.regenerateToken(validToken, contest, futureExpiry);

        // Then
        assertNotNull(result);
        verify(invitationRepository, times(2)).save(any(PrivateContestInvitation.class));
    }

    @Test
    void regenerateToken_InvalidatesOldToken() {
        // Given
        when(invitationRepository.findByToken(validToken))
            .thenReturn(Optional.of(invitation));
        when(invitationRepository.save(any(PrivateContestInvitation.class)))
            .thenReturn(invitation);

        // When
        inviteTokenService.regenerateToken(validToken, contest, futureExpiry);

        // Then
        ArgumentCaptor<PrivateContestInvitation> captor = ArgumentCaptor.forClass(PrivateContestInvitation.class);
        verify(invitationRepository, atLeastOnce()).save(captor.capture());
        
        // First save should invalidate old token
        PrivateContestInvitation invalidated = captor.getAllValues().get(0);
        assertTrue(invalidated.getInvalidated());
    }

    @Test
    void regenerateToken_EvictsOldTokenFromCache() {
        // Given
        when(invitationRepository.findByToken(validToken))
            .thenReturn(Optional.of(invitation));
        when(invitationRepository.save(any(PrivateContestInvitation.class)))
            .thenReturn(invitation);

        // When
        inviteTokenService.regenerateToken(validToken, contest, futureExpiry);

        // Then
        verify(redis).delete("invite:token:" + validToken);
    }

    @Test
    void regenerateToken_WithNonExistentToken_ThrowsException() {
        // Given
        when(invitationRepository.findByToken(validToken))
            .thenReturn(Optional.empty());

        // When / Then
        assertThrows(IllegalArgumentException.class, () -> {
            inviteTokenService.regenerateToken(validToken, contest, futureExpiry);
        });
    }

    // ─── Invalidate All Tokens Tests ─────────────────────────────────────────

    @Test
    void invalidateAllTokensForContest_InvalidatesAllTokens() {
        // Given
        PrivateContestInvitation invitation1 = new PrivateContestInvitation();
        invitation1.setToken("token1");
        invitation1.setInvalidated(false);
        
        PrivateContestInvitation invitation2 = new PrivateContestInvitation();
        invitation2.setToken("token2");
        invitation2.setInvalidated(false);
        
        List<PrivateContestInvitation> invitations = Arrays.asList(invitation1, invitation2);
        
        when(invitationRepository.findByContestId(contest.getId()))
            .thenReturn(invitations);
        when(invitationRepository.save(any(PrivateContestInvitation.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        inviteTokenService.invalidateAllTokensForContest(contest.getId());

        // Then
        verify(invitationRepository, times(2)).save(any(PrivateContestInvitation.class));
        assertTrue(invitation1.getInvalidated());
        assertTrue(invitation2.getInvalidated());
    }

    @Test
    void invalidateAllTokensForContest_EvictsAllTokensFromCache() {
        // Given
        PrivateContestInvitation invitation1 = new PrivateContestInvitation();
        invitation1.setToken("token1");
        
        PrivateContestInvitation invitation2 = new PrivateContestInvitation();
        invitation2.setToken("token2");
        
        List<PrivateContestInvitation> invitations = Arrays.asList(invitation1, invitation2);
        
        when(invitationRepository.findByContestId(contest.getId()))
            .thenReturn(invitations);
        when(invitationRepository.save(any(PrivateContestInvitation.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        inviteTokenService.invalidateAllTokensForContest(contest.getId());

        // Then
        verify(redis).delete("invite:token:token1");
        verify(redis).delete("invite:token:token2");
    }

    @Test
    void invalidateAllTokensForContest_WithNoTokens_DoesNothing() {
        // Given
        when(invitationRepository.findByContestId(contest.getId()))
            .thenReturn(List.of());

        // When
        inviteTokenService.invalidateAllTokensForContest(contest.getId());

        // Then
        verify(invitationRepository, never()).save(any(PrivateContestInvitation.class));
        verify(redis, never()).delete(anyString());
    }

    // ─── Update Expiry Tests ─────────────────────────────────────────────

    @Test
    void updateExpiry_WithValidToken_UpdatesExpiry() {
        // Given
        LocalDateTime newExpiry = LocalDateTime.now().plusDays(60);
        when(invitationRepository.findByToken(validToken))
            .thenReturn(Optional.of(invitation));
        when(invitationRepository.save(any(PrivateContestInvitation.class)))
            .thenReturn(invitation);

        // When
        PrivateContestInvitation result = inviteTokenService.updateExpiry(validToken, newExpiry);

        // Then
        assertNotNull(result);
        verify(invitationRepository).save(any(PrivateContestInvitation.class));
    }

    @Test
    void updateExpiry_UpdatesCache() {
        // Given
        LocalDateTime newExpiry = LocalDateTime.now().plusDays(60);
        when(invitationRepository.findByToken(validToken))
            .thenReturn(Optional.of(invitation));
        when(invitationRepository.save(any(PrivateContestInvitation.class)))
            .thenReturn(invitation);

        // When
        inviteTokenService.updateExpiry(validToken, newExpiry);

        // Then
        verify(valueOperations).set(
            eq("invite:token:" + validToken),
            anyString(),
            any(Duration.class)
        );
    }

    @Test
    void updateExpiry_WithNonExistentToken_ThrowsException() {
        // Given
        when(invitationRepository.findByToken(validToken))
            .thenReturn(Optional.empty());

        // When / Then
        assertThrows(IllegalArgumentException.class, () -> {
            inviteTokenService.updateExpiry(validToken, futureExpiry);
        });
    }

    // ─── Cache Error Handling Tests ─────────────────────────────────────────

    @Test
    void createInvitation_WithCacheFailure_StillCreatesInvitation() {
        // Given
        when(invitationRepository.save(any(PrivateContestInvitation.class)))
            .thenReturn(invitation);
        doThrow(new RuntimeException("Redis down"))
            .when(valueOperations).set(anyString(), anyString(), any(Duration.class));

        // When
        PrivateContestInvitation result = inviteTokenService.createInvitation(contest, futureExpiry);

        // Then
        assertNotNull(result);
        verify(invitationRepository).save(any(PrivateContestInvitation.class));
    }

    @Test
    void validateToken_WithCacheReadFailure_FallsBackToDatabase() {
        // Given
        when(valueOperations.get(anyString()))
            .thenThrow(new RuntimeException("Redis down"));
        when(invitationRepository.findByToken(validToken))
            .thenReturn(Optional.of(invitation));

        // When
        Optional<PrivateContestInvitation> result = inviteTokenService.validateToken(validToken);

        // Then
        assertTrue(result.isPresent());
        verify(invitationRepository).findByToken(validToken);
    }

    @Test
    void invalidateAllTokensForContest_WithCacheEvictionFailure_StillInvalidatesInDatabase() {
        // Given
        PrivateContestInvitation invitation1 = new PrivateContestInvitation();
        invitation1.setToken("token1");
        invitation1.setInvalidated(false);
        
        when(invitationRepository.findByContestId(contest.getId()))
            .thenReturn(List.of(invitation1));
        when(invitationRepository.save(any(PrivateContestInvitation.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("Redis down"))
            .when(redis).delete(anyString());

        // When
        inviteTokenService.invalidateAllTokensForContest(contest.getId());

        // Then
        assertTrue(invitation1.getInvalidated());
        verify(invitationRepository).save(invitation1);
    }
}
