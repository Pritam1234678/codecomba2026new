package com.example.codecombat2026.service;

import com.example.codecombat2026.entity.Contest;
import com.example.codecombat2026.entity.Problem;
import com.example.codecombat2026.exception.ResourceNotFoundException;
import com.example.codecombat2026.repository.ContestRepository;
import com.example.codecombat2026.repository.ProblemRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class ProblemService {

    @Autowired private ProblemRepository problemRepository;
    @Autowired private ContestRepository contestRepository;
    @Autowired private StringRedisTemplate redis;
    @Autowired private ObjectMapper objectMapper;

    private static final Duration PROBLEM_TTL         = Duration.ofMinutes(5);
    private static final Duration CONTEST_PROBLEMS_TTL = Duration.ofSeconds(30);

    public Problem createProblem(Long contestId, Problem problem) {
        Contest contest = contestRepository.findById(contestId)
            .orElseThrow(() -> new ResourceNotFoundException("Contest not found with id: " + contestId));
        problem.setContest(contest);
        Problem saved = problemRepository.save(problem);
        evictContestProblems(contestId);
        return saved;
    }

    /**
     * Cached per problem — problem statements rarely change.
     */
    public Problem getProblemById(Long id) {
        String key = "problem:" + id;
        try {
            String cached = redis.opsForValue().get(key);
            if (cached != null) {
                return objectMapper.readValue(cached, Problem.class);
            }
        } catch (Exception ignored) {}

        Problem problem = problemRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Problem not found with id: " + id));

        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(problem), PROBLEM_TTL);
        } catch (Exception ignored) {}

        return problem;
    }

    public List<Problem> getAllProblems() {
        return problemRepository.findAll();
    }

    /**
     * Cached per contest — called on every ContestDetail page load.
     */
    public List<Problem> getProblemsByContestId(Long contestId) {
        String key = "problems:contest:" + contestId;
        try {
            String cached = redis.opsForValue().get(key);
            if (cached != null) {
                return objectMapper.readValue(cached, new TypeReference<List<Problem>>() {});
            }
        } catch (Exception ignored) {}

        List<Problem> problems = problemRepository.findByContestId(contestId);

        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(problems), CONTEST_PROBLEMS_TTL);
        } catch (Exception ignored) {}

        return problems;
    }

    public void evictProblem(Long problemId) {
        try { redis.delete("problem:" + problemId); } catch (Exception ignored) {}
    }

    public void evictContestProblems(Long contestId) {
        try { redis.delete("problems:contest:" + contestId); } catch (Exception ignored) {}
    }
}
