package com.example.codecombat2026.sqljudge.service;

import com.example.codecombat2026.sqljudge.dto.SqlResult;
import com.example.codecombat2026.sqljudge.entity.SqlProblem;
import com.example.codecombat2026.sqljudge.repository.SqlProblemRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Expected-result cache (cache-aside, Valkey).
 *
 * <p>The application DB is the source of truth; the Valkey entry is a pure
 * performance cache. On miss we load from DB and populate. Used by the worker
 * on every SUBMIT — so the expected result is never recomputed per submission.
 */
@Service
public class SqlExpectedResultCache {

    private static final Logger log = LoggerFactory.getLogger(SqlExpectedResultCache.class);

    private static final String KEY_PREFIX = "sql:problem:";
    private static final String EXPECTED_SUFFIX = ":expected";
    private static final Duration TTL = Duration.ofHours(24);

    @Autowired private StringRedisTemplate redis;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private SqlProblemRepository problemRepository;

    public SqlResult getExpected(Long problemId) {
        String key = KEY_PREFIX + problemId + EXPECTED_SUFFIX;
        try {
            String cached = redis.opsForValue().get(key);
            if (cached != null) {
                return objectMapper.readValue(cached, SqlResult.class);
            }
        } catch (Exception e) {
            log.debug("Expected-result cache read failed for problem {}: {}", problemId, e.getMessage());
        }

        SqlProblem problem = problemRepository.findById(problemId).orElse(null);
        if (problem == null || problem.getExpectedResult() == null || problem.getExpectedResult().isBlank()) {
            return null;
        }
        try {
            SqlResult result = objectMapper.readValue(problem.getExpectedResult(), SqlResult.class);
            try {
                redis.opsForValue().set(key, objectMapper.writeValueAsString(result), TTL);
            } catch (Exception ignored) {}
            return result;
        } catch (Exception e) {
            log.error("Failed to parse expected result for problem {}: {}", problemId, e.getMessage());
            return null;
        }
    }

    /** Store/populate the cache entry for a problem. */
    public void put(Long problemId, SqlResult result) {
        if (result == null) return;
        try {
            redis.opsForValue().set(KEY_PREFIX + problemId + EXPECTED_SUFFIX,
                objectMapper.writeValueAsString(result), TTL);
        } catch (Exception e) {
            log.warn("Failed to cache expected result for problem {}: {}", problemId, e.getMessage());
        }
    }

    public void evict(Long problemId) {
        try {
            redis.delete(KEY_PREFIX + problemId + EXPECTED_SUFFIX);
        } catch (Exception ignored) {}
    }
}
