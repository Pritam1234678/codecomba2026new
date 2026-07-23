package com.example.codecombat2026.service;

import com.example.codecombat2026.entity.ProblemSolution;
import com.example.codecombat2026.exception.ResourceNotFoundException;
import com.example.codecombat2026.repository.ProblemSolutionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProblemSolutionService {

    private static final String LIST_KEY_PREFIX  = "solutions:list:";
    private static final String COUNT_KEY_PREFIX = "solutions:count:";
    private static final Duration TTL = Duration.ofMinutes(5);

    @Autowired
    private ProblemSolutionRepository repository;

    @Autowired
    private StringRedisTemplate redis;

    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule());

    public ProblemSolution create(Long problemId, Long userId, String userName,
                                   ProblemSolution.ProblemLanguages language,
                                   String code, String explanation, String imageUrl) {
        ProblemSolution s = new ProblemSolution();
        s.setProblemId(problemId);
        s.setUserId(userId);
        s.setUserName(userName);
        s.setLanguage(language);
        s.setCode(code);
        s.setExplanation(explanation);
        s.setImageUrl(imageUrl);
        ProblemSolution saved = repository.save(s);
        evictCache(problemId);
        return saved;
    }

    public ProblemSolution update(Long id, Long userId, ProblemSolution.ProblemLanguages language,
                                   String code, String explanation, String imageUrl) {
        ProblemSolution s = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Solution not found"));
        if (!s.getUserId().equals(userId)) {
            throw new RuntimeException("Not authorized to edit this solution");
        }
        s.setLanguage(language);
        s.setCode(code);
        s.setExplanation(explanation);
        s.setImageUrl(imageUrl);
        ProblemSolution saved = repository.save(s);
        evictCache(s.getProblemId());
        return saved;
    }

    public void delete(Long id, Long userId) {
        ProblemSolution s = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Solution not found"));
        if (!s.getUserId().equals(userId)) {
            throw new RuntimeException("Not authorized to delete this solution");
        }
        Long problemId = s.getProblemId();
        repository.delete(s);
        evictCache(problemId);
    }

    public List<ProblemSolution> getByProblem(Long problemId) {
        String key = LIST_KEY_PREFIX + problemId;
        try {
            String cached = redis.opsForValue().get(key);
            if (cached != null) {
                List<Map<String, Object>> list = objectMapper.readValue(cached,
                    new TypeReference<List<Map<String, Object>>>() {});
                return toEntities(list);
            }
        } catch (Exception ignored) {}

        List<ProblemSolution> solutions = repository.findByProblemIdOrderByCreatedAtDesc(problemId);
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(toMaps(solutions)), TTL);
        } catch (Exception ignored) {}
        return solutions;
    }

    public long countByProblem(Long problemId) {
        String key = COUNT_KEY_PREFIX + problemId;
        try {
            String cached = redis.opsForValue().get(key);
            if (cached != null) return Long.parseLong(cached);
        } catch (Exception ignored) {}

        long count = repository.countByProblemId(problemId);
        try {
            redis.opsForValue().set(key, String.valueOf(count), TTL);
        } catch (Exception ignored) {}
        return count;
    }

    private void evictCache(Long problemId) {
        try { redis.delete(LIST_KEY_PREFIX + problemId); } catch (Exception ignored) {}
        try { redis.delete(COUNT_KEY_PREFIX + problemId); } catch (Exception ignored) {}
    }

    private List<Map<String, Object>> toMaps(List<ProblemSolution> solutions) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (ProblemSolution s : solutions) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.getId());
            m.put("problemId", s.getProblemId());
            m.put("userId", s.getUserId());
            m.put("userName", s.getUserName());
            m.put("language", s.getLanguage().name());
            m.put("code", s.getCode());
            m.put("explanation", s.getExplanation());
            m.put("imageUrl", s.getImageUrl());
            m.put("createdAt", s.getCreatedAt() != null ? s.getCreatedAt().toString() : null);
            result.add(m);
        }
        return result;
    }

    private List<ProblemSolution> toEntities(List<Map<String, Object>> list) {
        List<ProblemSolution> result = new ArrayList<>();
        for (Map<String, Object> m : list) {
            ProblemSolution s = new ProblemSolution();
            s.setId(toLong(m.get("id")));
            s.setProblemId(toLong(m.get("problemId")));
            s.setUserId(toLong(m.get("userId")));
            s.setUserName((String) m.get("userName"));
            String lang = (String) m.get("language");
            if (lang != null) s.setLanguage(ProblemSolution.ProblemLanguages.valueOf(lang));
            s.setCode((String) m.get("code"));
            s.setExplanation((String) m.get("explanation"));
            s.setImageUrl((String) m.get("imageUrl"));
            String createdAt = (String) m.get("createdAt");
            if (createdAt != null) {
                try { s.setCreatedAt(java.time.LocalDateTime.parse(createdAt)); } catch (Exception ignored) {}
            }
            result.add(s);
        }
        return result;
    }

    private Long toLong(Object v) {
        if (v instanceof Number) return ((Number) v).longValue();
        if (v instanceof String) try { return Long.parseLong((String) v); } catch (Exception ignored) {}
        return null;
    }
}
