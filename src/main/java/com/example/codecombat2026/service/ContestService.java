package com.example.codecombat2026.service;

import com.example.codecombat2026.entity.Contest;
import com.example.codecombat2026.exception.ResourceNotFoundException;
import com.example.codecombat2026.repository.ContestRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class ContestService {

    @Autowired private ContestRepository contestRepository;
    @Autowired private StringRedisTemplate redis;
    @Autowired private ObjectMapper objectMapper;

    private static final String ACTIVE_CONTESTS_KEY = "contests:active";
    private static final Duration CONTESTS_TTL = Duration.ofSeconds(30);
    private static final String CONTEST_KEY_PREFIX = "contest:";
    private static final Duration CONTEST_TTL = Duration.ofMinutes(2);

    public Contest createContest(Contest contest) {
        Contest saved = contestRepository.save(contest);
        evictContestCache();
        return saved;
    }

    public Contest getContestById(Long id) {
        String key = CONTEST_KEY_PREFIX + id;
        try {
            String cached = redis.opsForValue().get(key);
            if (cached != null) {
                return objectMapper.readValue(cached, Contest.class);
            }
        } catch (Exception ignored) {}

        Contest contest = contestRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Contest not found with id: " + id));

        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(contest), CONTEST_TTL);
        } catch (Exception ignored) {}

        return contest;
    }

    public List<Contest> getAllContests() {
        return contestRepository.findAll();
    }

    /**
     * Cached — this is called on every page load by every user.
     * Cache for 30 seconds to absorb burst traffic.
     */
    public List<Contest> getVisibleContests() {
        try {
            String cached = redis.opsForValue().get(ACTIVE_CONTESTS_KEY);
            if (cached != null) {
                return objectMapper.readValue(cached, new TypeReference<List<Contest>>() {});
            }
        } catch (Exception ignored) {}

        List<Contest> contests = contestRepository.findByActiveTrue();

        try {
            redis.opsForValue().set(ACTIVE_CONTESTS_KEY,
                objectMapper.writeValueAsString(contests), CONTESTS_TTL);
        } catch (Exception ignored) {}

        return contests;
    }

    public void evictContestCache() {
        try {
            redis.delete(ACTIVE_CONTESTS_KEY);
        } catch (Exception ignored) {}
    }

    public void evictContest(Long id) {
        try {
            redis.delete(CONTEST_KEY_PREFIX + id);
            redis.delete(ACTIVE_CONTESTS_KEY);
        } catch (Exception ignored) {}
    }
}
