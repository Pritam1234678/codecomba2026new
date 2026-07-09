package com.example.codecombat2026.service;

import com.example.codecombat2026.entity.Contest;
import com.example.codecombat2026.entity.PrivateContestInvitation;
import com.example.codecombat2026.repository.PrivateContestInvitationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

/**
 * Service for managing invite tokens for private contests.
 * 
 * Provides secure token generation using cryptographically random UUIDs,
 * token validation with expiry checking, and Valkey cache integration
 * for performance optimization.
 * 
 * Token Format:
 * - 32 bytes of cryptographically random data
 * - Base64url-encoded (no padding)
 * - Results in 43-character string (fits in VARCHAR(64))
 * 
 * Cache Strategy:
 * - Valid tokens are cached in Valkey with TTL matching expiry
 * - Cache key format: "invite:token:{token}"
 * - Cache value: contest ID (as string)
 * - On cache miss, falls back to database lookup
 * 
 * Requirements: 4.8, 5.1, 5.2, 5.3, 6.3, 6.4
 */
@Service
public class InviteTokenService {

    private static final Logger log = LoggerFactory.getLogger(InviteTokenService.class);
    
    private static final String CACHE_PREFIX = "invite:token:";
    private static final int TOKEN_BYTES = 32;
    private static final Duration DEFAULT_EXPIRY = Duration.ofDays(30);
    
    @Autowired
    private PrivateContestInvitationRepository invitationRepository;
    
    @Autowired
    private StringRedisTemplate redis;
    
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generate a cryptographically secure random token.
     * 
     * Uses SecureRandom to generate 32 bytes of random data, then encodes
     * with Base64url (URL-safe variant without padding).
     * 
     * @return A 43-character base64url-encoded token string
     */
    public String generateToken() {
        byte[] tokenBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        log.debug("Generated new invite token: {}...", token.substring(0, Math.min(10, token.length())));
        return token;
    }

    /**
     * Create and persist an invitation token for a private contest.
     * 
     * Generates a new token, stores it in the database with the specified
     * expiry time, and caches it in Valkey for fast validation.
     * 
     * @param contest The Contest entity to create an invitation for
     * @param expiresAt The expiry timestamp (null uses default 30 days)
     * @return The created PrivateContestInvitation entity
     */
    @Transactional
    public PrivateContestInvitation createInvitation(Contest contest, LocalDateTime expiresAt) {
        if (contest == null) {
            throw new IllegalArgumentException("Contest cannot be null");
        }
        
        String token = generateToken();
        LocalDateTime expiry = expiresAt != null ? expiresAt : LocalDateTime.now().plus(DEFAULT_EXPIRY);
        
        PrivateContestInvitation invitation = new PrivateContestInvitation();
        invitation.setContest(contest);
        invitation.setToken(token);
        invitation.setCreatedAt(LocalDateTime.now());
        invitation.setExpiresAt(expiry);
        invitation.setInvalidated(false);
        
        invitation = invitationRepository.save(invitation);
        
        // Cache the token for fast validation
        cacheToken(token, contest.getId(), expiry);
        
        log.info("Created invitation token for contest ID {} expiring at {}", contest.getId(), expiry);
        return invitation;
    }

    /**
     * Validate an invite token.
     * 
     * Checks cache first for performance, falls back to database if not cached.
     * Validates that the token exists, is not invalidated, and has not expired.
     * 
     * @param token The token string to validate
     * @return Optional containing the PrivateContestInvitation if valid, empty otherwise
     */
    public Optional<PrivateContestInvitation> validateToken(String token) {
        if (token == null || token.isBlank()) {
            log.debug("Token validation failed: token is null or blank");
            return Optional.empty();
        }
        
        // Check cache first
        String cachedContestId = getCachedToken(token);
        if (cachedContestId != null) {
            log.debug("Token found in cache for contest ID {}", cachedContestId);
            // Still need to fetch from DB to get full invitation details
            Optional<PrivateContestInvitation> invitation = invitationRepository.findByToken(token);
            if (invitation.isPresent() && isTokenValid(invitation.get())) {
                return invitation;
            } else {
                // Cache was stale, evict it
                evictToken(token);
            }
        }
        
        // Cache miss or stale cache - fetch from database
        Optional<PrivateContestInvitation> invitation = invitationRepository.findByToken(token);
        
        if (invitation.isEmpty()) {
            log.debug("Token validation failed: token not found");
            return Optional.empty();
        }
        
        PrivateContestInvitation inv = invitation.get();
        
        if (!isTokenValid(inv)) {
            log.debug("Token validation failed: token is invalid, invalidated={}, expired={}",
                    inv.getInvalidated(), inv.getExpiresAt().isBefore(LocalDateTime.now()));
            return Optional.empty();
        }
        
        // Token is valid, cache it for future lookups
        cacheToken(token, inv.getContest().getId(), inv.getExpiresAt());
        
        log.debug("Token validated successfully for contest ID {}", inv.getContest().getId());
        return invitation;
    }

    /**
     * Check if a token is valid based on business rules.
     * 
     * @param invitation The invitation to check
     * @return true if the token is not invalidated and has not expired
     */
    private boolean isTokenValid(PrivateContestInvitation invitation) {
        return !invitation.getInvalidated() && 
               invitation.getExpiresAt().isAfter(LocalDateTime.now());
    }

    /**
     * Invalidate an existing token and generate a new one.
     * 
     * Used when a Contest_Host regenerates their invite link.
     * Marks the old token as invalidated (preserves audit trail)
     * and creates a new invitation.
     * 
     * @param oldToken The existing token to invalidate
     * @param contest The Contest entity
     * @param newExpiresAt Expiry for the new token (null uses default)
     * @return The newly created PrivateContestInvitation
     * @throws IllegalArgumentException if the old token doesn't exist
     */
    @Transactional
    public PrivateContestInvitation regenerateToken(String oldToken, Contest contest, LocalDateTime newExpiresAt) {
        Optional<PrivateContestInvitation> oldInvitation = invitationRepository.findByToken(oldToken);
        
        if (oldInvitation.isEmpty()) {
            throw new IllegalArgumentException("Token not found: " + oldToken);
        }
        
        // Invalidate old token
        PrivateContestInvitation old = oldInvitation.get();
        old.setInvalidated(true);
        invitationRepository.save(old);
        
        // Evict from cache
        evictToken(oldToken);
        
        log.info("Invalidated old token for contest ID {}", contest.getId());
        
        // Create new token
        return createInvitation(contest, newExpiresAt);
    }

    /**
     * Invalidate all tokens for a specific contest.
     * 
     * Used when a contest is cancelled or deleted.
     * 
     * @param contestId The ID of the contest
     */
    @Transactional
    public void invalidateAllTokensForContest(Long contestId) {
        var invitations = invitationRepository.findByContestId(contestId);
        
        for (PrivateContestInvitation invitation : invitations) {
            invitation.setInvalidated(true);
            invitationRepository.save(invitation);
            evictToken(invitation.getToken());
        }
        
        log.info("Invalidated {} tokens for contest ID {}", invitations.size(), contestId);
    }

    /**
     * Cache a token in Valkey for fast validation.
     * 
     * @param token The token string
     * @param contestId The contest ID
     * @param expiresAt The expiry timestamp
     */
    private void cacheToken(String token, Long contestId, LocalDateTime expiresAt) {
        try {
            String key = CACHE_PREFIX + token;
            String value = String.valueOf(contestId);
            
            // Calculate TTL based on expiry time
            Duration ttl = Duration.between(LocalDateTime.now(), expiresAt);
            
            if (ttl.isNegative() || ttl.isZero()) {
                log.warn("Not caching token with expired or zero TTL");
                return;
            }
            
            redis.opsForValue().set(key, value, ttl);
            log.debug("Cached token with TTL of {} seconds", ttl.getSeconds());
        } catch (Exception e) {
            // Cache failures should not break token operations
            log.warn("Failed to cache token: {}", e.getMessage());
        }
    }

    /**
     * Retrieve a cached token from Valkey.
     * 
     * @param token The token string
     * @return The contest ID as a string, or null if not cached
     */
    private String getCachedToken(String token) {
        try {
            String key = CACHE_PREFIX + token;
            return redis.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("Failed to retrieve cached token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Evict a token from the cache.
     * 
     * @param token The token string to evict
     */
    private void evictToken(String token) {
        try {
            String key = CACHE_PREFIX + token;
            redis.delete(key);
            log.debug("Evicted token from cache");
        } catch (Exception e) {
            log.warn("Failed to evict token from cache: {}", e.getMessage());
        }
    }

    /**
     * Update the expiry time for an existing invitation.
     * 
     * @param token The token to update
     * @param newExpiresAt The new expiry timestamp
     * @return The updated PrivateContestInvitation
     * @throws IllegalArgumentException if the token doesn't exist
     */
    @Transactional
    public PrivateContestInvitation updateExpiry(String token, LocalDateTime newExpiresAt) {
        Optional<PrivateContestInvitation> invitation = invitationRepository.findByToken(token);
        
        if (invitation.isEmpty()) {
            throw new IllegalArgumentException("Token not found: " + token);
        }
        
        PrivateContestInvitation inv = invitation.get();
        inv.setExpiresAt(newExpiresAt);
        inv = invitationRepository.save(inv);
        
        // Update cache with new TTL
        cacheToken(token, inv.getContest().getId(), newExpiresAt);
        
        log.info("Updated token expiry to {} for contest ID {}", newExpiresAt, inv.getContest().getId());
        return inv;
    }
}
