package com.example.codecombat2026.service;

import com.example.codecombat2026.dto.LeaderboardEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

/**
 * Real-time leaderboard using Valkey Sorted Sets (ZSET).
 *
 * Key: leaderboard:contest:{contestId}
 * Member: userId (as string)
 * Score: total score (higher = better rank)
 *
 * All operations are O(log N) — instant even with 500 users.
 */
@Service
public class LeaderboardCacheService {

    private static final Logger log = LoggerFactory.getLogger(LeaderboardCacheService.class);

    @Autowired
    private StringRedisTemplate redis;

    private static final String KEY_PREFIX = "leaderboard:contest:";
    private static final Duration TTL = Duration.ofHours(26); // contest + buffer

    /**
     * Update a user's score after a submission.
     * ZINCRBY is atomic — safe for concurrent updates from multiple workers.
     */
    public void updateScore(Long contestId, Long userId, double scoreToAdd) {
        try {
            String key = KEY_PREFIX + contestId;
            redis.opsForZSet().incrementScore(key, userId.toString(), scoreToAdd);
            redis.expire(key, TTL);
        } catch (Exception e) {
            // Log but don't fail — DB is the source of truth
            log.warn("Leaderboard update failed for contest {} user {}: {}", contestId, userId, e.getMessage());
        }
    }

    /**
     * Set a user's absolute score (used when seeding from MySQL).
     */
    public void setScore(Long contestId, Long userId, double score) {
        try {
            String key = KEY_PREFIX + contestId;
            redis.opsForZSet().add(key, userId.toString(), score);
            redis.expire(key, TTL);
        } catch (Exception ignored) {}
    }

    /**
     * Get top N users — O(log N + K).
     * Returns ranked list, highest score first.
     */
    public List<LeaderboardEntry> getTopN(Long contestId, int n) {
        try {
            String key = KEY_PREFIX + contestId;
            Set<ZSetOperations.TypedTuple<String>> top =
                redis.opsForZSet().reverseRangeWithScores(key, 0, n - 1);

            if (top == null || top.isEmpty()) return Collections.emptyList();

            List<LeaderboardEntry> result = new ArrayList<>();
            int rank = 1;
            for (ZSetOperations.TypedTuple<String> entry : top) {
                if (entry.getValue() == null || entry.getScore() == null) continue;
                Long userId = Long.parseLong(entry.getValue());
                result.add(new LeaderboardEntry(userId, null, null,
                    entry.getScore(), 0, rank++));
            }
            return result;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * Get a specific user's rank — O(log N).
     * Returns 1-indexed rank, or null if not on leaderboard.
     */
    public Long getUserRank(Long contestId, Long userId) {
        try {
            String key = KEY_PREFIX + contestId;
            Long rank = redis.opsForZSet().reverseRank(key, userId.toString());
            return rank != null ? rank + 1 : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get a user's current score.
     */
    public Double getUserScore(Long contestId, Long userId) {
        try {
            String key = KEY_PREFIX + contestId;
            return redis.opsForZSet().score(key, userId.toString());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Seed leaderboard from MySQL on first access (cold start).
     */
    public void seedFromDatabase(Long contestId, List<LeaderboardEntry> entries) {
        try {
            String key = KEY_PREFIX + contestId;
            Set<ZSetOperations.TypedTuple<String>> bulk = new HashSet<>();
            for (LeaderboardEntry e : entries) {
                bulk.add(ZSetOperations.TypedTuple.of(
                    e.getUserId().toString(),
                    e.getTotalScore()
                ));
            }
            if (!bulk.isEmpty()) {
                redis.opsForZSet().add(key, bulk);
                redis.expire(key, TTL);
            }
        } catch (Exception ignored) {}
    }

    /**
     * Check if leaderboard exists in cache.
     */
    public boolean exists(Long contestId) {
        try {
            return Boolean.TRUE.equals(redis.hasKey(KEY_PREFIX + contestId));
        } catch (Exception e) {
            return false;
        }
    }
}
