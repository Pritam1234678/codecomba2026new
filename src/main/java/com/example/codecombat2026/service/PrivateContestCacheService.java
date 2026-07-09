package com.example.codecombat2026.service;

import com.example.codecombat2026.dto.PrivateContestDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

/**
 * Cache service for private contest metadata and participant sets.
 * 
 * Cache key patterns:
 * - private:contest:{contestId} — HASH storing contest metadata (6-hour TTL)
 * - private:participants:{contestId} — SET storing participant user IDs (6-hour TTL)
 * 
 * Used to reduce database load for frequently accessed private contest data:
 * - Contest metadata (name, description, times, host info)
 * - Participant membership checks (fast O(1) lookup)
 * 
 * Requirements: 21.1, 25.3
 */
@Service
public class PrivateContestCacheService {

    private static final Logger log = LoggerFactory.getLogger(PrivateContestCacheService.class);

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.example.codecombat2026.config.PrivateContestMetricsConfig metricsConfig;

    private static final String CONTEST_KEY_PREFIX = "private:contest:";
    private static final String PARTICIPANTS_KEY_PREFIX = "private:participants:";
    private static final Duration CACHE_TTL = Duration.ofHours(6);

    /**
     * Cache private contest metadata with 6-hour TTL.
     * 
     * Stores the PrivateContestDTO as JSON in Valkey under key:
     *   private:contest:{contestId}
     * 
     * If caching fails (Valkey unavailable), logs a warning but does not throw.
     * Database remains source of truth.
     * 
     * @param contestId the contest ID
     * @param dto the PrivateContestDTO to cache
     */
    public void cacheContestMetadata(Long contestId, PrivateContestDTO dto) {
        if (contestId == null || dto == null) {
            log.warn("Cannot cache null contestId or dto");
            return;
        }

        try {
            String key = CONTEST_KEY_PREFIX + contestId;
            String json = objectMapper.writeValueAsString(dto);
            redis.opsForValue().set(key, json, CACHE_TTL);
            log.debug("Cached private contest metadata: contestId={}", contestId);
        } catch (Exception e) {
            log.warn("Failed to cache private contest metadata for contestId={}: {}", 
                contestId, e.getMessage());
        }
    }

    /**
     * Retrieve cached private contest metadata.
     * 
     * Returns Optional.empty() if:
     * - Cache miss (key not found)
     * - Valkey unavailable
     * - Deserialization fails
     * 
     * Caller should fallback to database on cache miss.
     * 
     * @param contestId the contest ID
     * @return Optional containing PrivateContestDTO if cache hit, empty otherwise
     */
    public Optional<PrivateContestDTO> getCachedContest(Long contestId) {
        if (contestId == null) {
            return Optional.empty();
        }

        try {
            String key = CONTEST_KEY_PREFIX + contestId;
            String json = redis.opsForValue().get(key);
            
            if (json == null) {
                log.debug("Cache miss for private contest: contestId={}", contestId);
                metricsConfig.incrementCacheMisses();
                return Optional.empty();
            }

            PrivateContestDTO dto = objectMapper.readValue(json, PrivateContestDTO.class);
            log.debug("Cache hit for private contest: contestId={}", contestId);
            metricsConfig.incrementCacheHits();
            return Optional.of(dto);
        } catch (Exception e) {
            log.warn("Failed to read cached private contest for contestId={}: {}", 
                contestId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Cache the participant set for a private contest with 6-hour TTL.
     * 
     * Stores participant user IDs in a Valkey SET under key:
     *   private:participants:{contestId}
     * 
     * Enables O(1) membership checks via isCachedParticipant().
     * 
     * @param contestId the contest ID
     * @param userIds set of participant user IDs
     */
    public void cacheParticipantSet(Long contestId, Set<Long> userIds) {
        if (contestId == null) {
            log.warn("Cannot cache participant set for null contestId");
            return;
        }

        if (userIds == null || userIds.isEmpty()) {
            log.debug("Caching empty participant set for contestId={}", contestId);
            // Still cache an empty set to indicate "no participants" vs "not cached"
            // We'll use a special marker value
            try {
                String key = PARTICIPANTS_KEY_PREFIX + contestId;
                redis.delete(key); // Clear any existing set
                // Set TTL on the key even if empty - use a dummy value
                redis.opsForValue().set(key + ":empty", "1", CACHE_TTL);
                log.debug("Cached empty participant set marker for contestId={}", contestId);
            } catch (Exception e) {
                log.warn("Failed to cache empty participant set for contestId={}: {}", 
                    contestId, e.getMessage());
            }
            return;
        }

        try {
            String key = PARTICIPANTS_KEY_PREFIX + contestId;
            
            // Convert Long IDs to String for Redis SET
            String[] userIdStrings = userIds.stream()
                .map(String::valueOf)
                .toArray(String[]::new);
            
            // Clear any previous empty marker
            redis.delete(key + ":empty");
            
            // Add all user IDs to the SET
            redis.opsForSet().add(key, userIdStrings);
            redis.expire(key, CACHE_TTL);
            
            log.debug("Cached participant set for contestId={}: {} participants", 
                contestId, userIds.size());
        } catch (Exception e) {
            log.warn("Failed to cache participant set for contestId={}: {}", 
                contestId, e.getMessage());
        }
    }

    /**
     * Fast O(1) membership check for whether a user is a participant.
     * 
     * Checks if userId exists in the cached SET:
     *   private:participants:{contestId}
     * 
     * Returns false if:
     * - Cache miss (key not found or expired)
     * - User not in the set
     * - Valkey unavailable
     * 
     * Caller should fallback to database if caching is unavailable or on cache miss.
     * 
     * @param contestId the contest ID
     * @param userId the user ID to check
     * @return true if user is in cached participant set, false otherwise
     */
    public boolean isCachedParticipant(Long contestId, Long userId) {
        if (contestId == null || userId == null) {
            return false;
        }

        try {
            String key = PARTICIPANTS_KEY_PREFIX + contestId;
            
            // Check if empty marker exists
            Boolean hasEmptyMarker = redis.hasKey(key + ":empty");
            if (Boolean.TRUE.equals(hasEmptyMarker)) {
                log.debug("Found empty participant set marker for contestId={}", contestId);
                return false;
            }
            
            // Check if key exists
            Boolean keyExists = redis.hasKey(key);
            if (!Boolean.TRUE.equals(keyExists)) {
                log.debug("Cache miss: participant set not found for contestId={}", contestId);
                return false;
            }
            
            // Check SET membership
            Boolean isMember = redis.opsForSet().isMember(key, userId.toString());
            boolean result = Boolean.TRUE.equals(isMember);
            
            log.debug("Participant check for contestId={}, userId={}: {}", 
                contestId, userId, result);
            return result;
        } catch (Exception e) {
            log.warn("Failed to check cached participant for contestId={}, userId={}: {}", 
                contestId, userId, e.getMessage());
            return false;
        }
    }

    /**
     * Invalidate all cached data for a private contest.
     * 
     * Deletes both:
     * - private:contest:{contestId} (metadata)
     * - private:participants:{contestId} (participant set)
     * - private:participants:{contestId}:empty (empty marker)
     * 
     * Should be called when:
     * - Participant joins or is removed
     * - Contest metadata changes
     * - Contest is cancelled
     * - Problems are attached/removed
     * 
     * @param contestId the contest ID
     */
    public void invalidateContestCache(Long contestId) {
        if (contestId == null) {
            log.warn("Cannot invalidate cache for null contestId");
            return;
        }

        try {
            String contestKey = CONTEST_KEY_PREFIX + contestId;
            String participantsKey = PARTICIPANTS_KEY_PREFIX + contestId;
            String emptyMarkerKey = participantsKey + ":empty";
            
            redis.delete(contestKey);
            redis.delete(participantsKey);
            redis.delete(emptyMarkerKey);
            
            log.debug("Invalidated cache for private contest: contestId={}", contestId);
        } catch (Exception e) {
            log.warn("Failed to invalidate cache for contestId={}: {}", 
                contestId, e.getMessage());
        }
    }

    /**
     * Check if contest metadata exists in cache.
     * 
     * Useful for conditional caching logic.
     * 
     * @param contestId the contest ID
     * @return true if contest metadata is cached, false otherwise
     */
    public boolean isContestCached(Long contestId) {
        if (contestId == null) {
            return false;
        }

        try {
            String key = CONTEST_KEY_PREFIX + contestId;
            Boolean exists = redis.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.warn("Failed to check if contest is cached for contestId={}: {}", 
                contestId, e.getMessage());
            return false;
        }
    }

    /**
     * Check if participant set exists in cache.
     * 
     * @param contestId the contest ID
     * @return true if participant set is cached (even if empty), false otherwise
     */
    public boolean isParticipantSetCached(Long contestId) {
        if (contestId == null) {
            return false;
        }

        try {
            String key = PARTICIPANTS_KEY_PREFIX + contestId;
            String emptyMarkerKey = key + ":empty";
            
            Boolean hasSet = redis.hasKey(key);
            Boolean hasEmptyMarker = redis.hasKey(emptyMarkerKey);
            
            return Boolean.TRUE.equals(hasSet) || Boolean.TRUE.equals(hasEmptyMarker);
        } catch (Exception e) {
            log.warn("Failed to check if participant set is cached for contestId={}: {}", 
                contestId, e.getMessage());
            return false;
        }
    }
}
