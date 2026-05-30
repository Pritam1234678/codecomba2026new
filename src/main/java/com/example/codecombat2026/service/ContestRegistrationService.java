package com.example.codecombat2026.service;

import com.example.codecombat2026.entity.ContestRegistration;
import com.example.codecombat2026.repository.ContestRegistrationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class ContestRegistrationService {

    @Autowired private ContestRegistrationRepository repo;
    @Autowired private StringRedisTemplate redis;

    // Cache key: "contest:reg:{contestId}:{userId}" → "1" or "0"
    private static final Duration REG_TTL = Duration.ofMinutes(10);

    private String cacheKey(Long contestId, Long userId) {
        return "contest:reg:" + contestId + ":" + userId;
    }

    /** Register a user for a contest. Idempotent — re-registering is a no-op. */
    public ContestRegistration register(Long contestId, Long userId) {
        if (repo.existsByContestIdAndUserId(contestId, userId)) {
            return repo.findByContestIdAndUserId(contestId, userId).orElseThrow();
        }
        ContestRegistration reg = new ContestRegistration();
        reg.setContestId(contestId);
        reg.setUserId(userId);
        reg.setRegisteredAt(LocalDateTime.now());
        ContestRegistration saved = repo.save(reg);
        // Update cache
        try { redis.opsForValue().set(cacheKey(contestId, userId), "1", REG_TTL); } catch (Exception ignored) {}
        return saved;
    }

    /** Check if a user is registered for a contest (cache-first). */
    public boolean isRegistered(Long contestId, Long userId) {
        try {
            String cached = redis.opsForValue().get(cacheKey(contestId, userId));
            if (cached != null) return "1".equals(cached);
        } catch (Exception ignored) {}

        boolean exists = repo.existsByContestIdAndUserId(contestId, userId);
        try { redis.opsForValue().set(cacheKey(contestId, userId), exists ? "1" : "0", REG_TTL); } catch (Exception ignored) {}
        return exists;
    }

    /** Total registrations for a contest. */
    public long countRegistrations(Long contestId) {
        return repo.countByContestId(contestId);
    }
}
