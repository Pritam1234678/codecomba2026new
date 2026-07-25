package com.example.codecombat2026.controller;

import com.example.codecombat2026.entity.Problem;
import com.example.codecombat2026.repository.ProblemRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.*;

@RestController
@RequestMapping("/api/playlist")
@PreAuthorize("isAuthenticated()")
public class PlaylistController {

    @Autowired private ProblemRepository problemRepository;
    @Autowired private StringRedisTemplate redis;
    @Autowired private ObjectMapper objectMapper;

    @GetMapping("/topics")
    public ResponseEntity<List<Map<String, Object>>> getTopics() {
        String key = "playlist:topics";
        try {
            String cached = redis.opsForValue().get(key);
            if (cached != null) {
                return ResponseEntity.ok(objectMapper.readValue(cached, new TypeReference<>() {}));
            }
        } catch (Exception ignored) {}

        List<Problem> problems;
        try {
            String cached = redis.opsForValue().get("problems:all");
            if (cached != null) problems = objectMapper.readValue(cached, new TypeReference<>() {});
            else problems = problemRepository.findAll();
        } catch (Exception ignored) { problems = problemRepository.findAll(); }

        Map<String, Integer> topicCounts = new LinkedHashMap<>();
        for (Problem p : problems) {
            if (!Boolean.TRUE.equals(p.getActive())) continue;
            String topics = p.getTopics();
            if (topics == null || topics.isBlank()) continue;
            for (String t : topics.split(",")) {
                String clean = t.trim();
                if (!clean.isEmpty()) topicCounts.merge(clean, 1, Integer::sum);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        List<String> sorted = new ArrayList<>(topicCounts.keySet());
        sorted.sort(Comparator.comparingInt(t -> -topicCounts.get(t)));
        for (String topic : sorted) {
            result.add(Map.of("name", topic, "count", topicCounts.get(topic),
                "slug", topic.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "")));
        }

        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(result), Duration.ofMinutes(30));
        } catch (Exception ignored) {}
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{topic}")
    public ResponseEntity<?> getProblems(@PathVariable String topic,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        List<Problem> problems;
        try {
            String cached = redis.opsForValue().get("problems:all");
            if (cached != null) problems = objectMapper.readValue(cached, new TypeReference<>() {});
            else problems = problemRepository.findAll();
        } catch (Exception ignored) { problems = problemRepository.findAll(); }

        String searchTopic = topic.replace('-', ' ').toLowerCase().trim();
        List<Problem> matched = problems.stream()
            .filter(p -> Boolean.TRUE.equals(p.getActive()))
            .filter(p -> {
                String ts = p.getTopics();
                if (ts == null) return false;
                for (String t : ts.split(",")) {
                    if (t.trim().replaceAll("[^a-zA-Z]", " ").toLowerCase().trim().contains(searchTopic)
                        || searchTopic.contains(t.trim().replaceAll("[^a-zA-Z]", " ").toLowerCase().trim())) return true;
                }
                return false;
            }).toList();

        int total = matched.size();
        int from = page * size;
        int to = Math.min(from + size, total);
        List<Problem> pageItems = matched.subList(Math.min(from, total), to);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Problem p : pageItems) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("title", p.getTitle());
            m.put("level", p.getLevel());
            m.put("topics", p.getTopics());
            result.add(m);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("problems", result);
        response.put("total", total);
        response.put("page", page);
        response.put("size", size);
        return ResponseEntity.ok(response);
    }
}
