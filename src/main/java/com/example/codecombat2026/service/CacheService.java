package com.example.codecombat2026.service;

import com.example.codecombat2026.entity.CodeSnippet;
import com.example.codecombat2026.entity.Contest;
import com.example.codecombat2026.entity.Problem;
import com.example.codecombat2026.repository.CodeSnippetRepository;
import com.example.codecombat2026.repository.ContestRepository;
import com.example.codecombat2026.repository.ProblemRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Cache-aside pattern using Valkey.
 * Reads hit Valkey first (~0.5ms). On miss, reads MySQL and populates cache.
 * Completely offloads read traffic from Railway MySQL.
 */
@Service
public class CacheService {

    @Autowired private StringRedisTemplate redis;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ProblemRepository problemRepository;
    @Autowired private CodeSnippetRepository snippetRepository;
    @Autowired private ContestRepository contestRepository;

    private static final Duration PROBLEM_TTL  = Duration.ofMinutes(30);
    private static final Duration SNIPPET_TTL  = Duration.ofMinutes(60);
    private static final Duration CONTEST_TTL  = Duration.ofSeconds(30);
    private static final Duration PROFILE_TTL  = Duration.ofMinutes(5);

    // ─── Problem ──────────────────────────────────────────────────────────────

    public Optional<Problem> getProblem(Long problemId) {
        String key = "problem:" + problemId;
        try {
            String cached = redis.opsForValue().get(key);
            if (cached != null) {
                return Optional.of(objectMapper.readValue(cached, Problem.class));
            }
        } catch (Exception ignored) {}

        Optional<Problem> problem = problemRepository.findById(problemId);
        problem.ifPresent(p -> cacheProblem(p));
        return problem;
    }

    public void cacheProblem(Problem problem) {
        try {
            redis.opsForValue().set(
                "problem:" + problem.getId(),
                objectMapper.writeValueAsString(problem),
                PROBLEM_TTL
            );
        } catch (Exception ignored) {}
    }

    public void evictProblem(Long problemId) {
        try { redis.delete("problem:" + problemId); } catch (Exception ignored) {}
        for (String lang : new String[]{"JAVA","CPP","PYTHON","JAVASCRIPT","C"}) {
            try { redis.delete("snippet:" + problemId + ":" + lang); } catch (Exception ignored) {}
        }
    }

    // ─── Code Snippet (harness) ───────────────────────────────────────────────

    public String getSnippetHarness(Long problemId, String language) {
        String key = "snippet:" + problemId + ":" + language;
        try {
            String cached = redis.opsForValue().get(key);
            if (cached != null) return cached;
        } catch (Exception ignored) {}

        CodeSnippet.ProgrammingLanguage lang =
            CodeSnippet.ProgrammingLanguage.valueOf(language.toUpperCase());

        return snippetRepository.findByProblemIdAndLanguage(problemId, lang)
            .map(snippet -> {
                String harness = snippet.getSolutionTemplate();
                try {
                    redis.opsForValue().set(key, harness, SNIPPET_TTL);
                } catch (Exception ignored) {}
                return harness;
            })
            .orElse(null);
    }

    // ─── Contest list ─────────────────────────────────────────────────────────

    public void evictContests() {
        try { redis.delete("contests:active"); } catch (Exception ignored) {}
    }

    // ─── Generic string cache ─────────────────────────────────────────────────

    public void set(String key, String value, Duration ttl) {
        try { redis.opsForValue().set(key, value, ttl); }
        catch (Exception ignored) {}
    }

    public String get(String key) {
        try { return redis.opsForValue().get(key); }
        catch (Exception e) { return null; }
    }

    public void delete(String key) {
        try { redis.delete(key); }
        catch (Exception ignored) {}
    }

    public String getProblemTitle(Long problemId) {
        try {
            String cached = get("problem:" + problemId);
            if (cached != null) {
                var node = objectMapper.readTree(cached);
                var titleNode = node.get("title");
                return titleNode != null ? titleNode.asText() : null;
            }
        } catch (Exception ignored) {}
        try {
            return problemRepository.findById(problemId)
                .map(p -> p.getTitle()).orElse(null);
        } catch (Exception ignored) { return null; }
    }
}
