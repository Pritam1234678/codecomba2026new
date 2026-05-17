package com.example.codecombat2026.service;

import com.example.codecombat2026.dto.SubmissionJob;
import com.example.codecombat2026.entity.Problem;
import com.example.codecombat2026.entity.Submission;
import com.example.codecombat2026.entity.User;
import com.example.codecombat2026.exception.ResourceNotFoundException;
import com.example.codecombat2026.repository.ProblemRepository;
import com.example.codecombat2026.repository.SubmissionRepository;
import com.example.codecombat2026.repository.UserRepository;
import com.example.codecombat2026.util.TimeUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SubmissionService {

    private static final Logger log = LoggerFactory.getLogger(SubmissionService.class);

    @Autowired private SubmissionRepository submissionRepository;
    @Autowired private ProblemRepository problemRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private StringRedisTemplate redis;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private RateLimiterService rateLimiter;

    /**
     * Async submit — saves PENDING to MySQL, pushes job to Valkey queue,
     * returns immediately (< 10ms). Worker pool handles execution.
     */
    @Transactional
    public Submission submitCodeAsync(Long userId, Long problemId,
                                      String code, Submission.ProgrammingLanguage language) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Problem problem = problemRepository.findById(problemId)
            .orElseThrow(() -> new ResourceNotFoundException("Problem not found"));

        // Upsert: update existing submission or create new one
        Submission submission = submissionRepository.findByUser_IdAndProblem_Id(userId, problemId);
        if (submission == null) {
            submission = new Submission();
            submission.setUser(user);
            submission.setProblem(problem);
            submission.setContest(problem.getContest());
        }

        submission.setCode(code);
        submission.setLanguage(language);
        submission.setStatus(Submission.SubmissionStatus.PENDING);
        submission.setSubmittedAt(TimeUtil.now());
        submission.setScore(null);
        submission.setTestCasesPassed(null);
        submission.setTotalTestCases(null);
        submission.setErrorMessage(null);
        submission.setTestCaseDetails(null);
        submission.setTimeConsumed(null);
        submission.setUserName(user.getUsername());
        submission.setUserRoll(user.getRollNumber());
        submission.setProblemName(problem.getTitle());

        submission = submissionRepository.save(submission);

        // Push job to Valkey queue — worker picks it up asynchronously
        Long contestId = problem.getContest() != null ? problem.getContest().getId() : null;
        SubmissionJob job = new SubmissionJob(
            submission.getId(), userId, problemId, contestId,
            code, language.name(),
            problem.getTimeLimit() != null ? problem.getTimeLimit() : 5.0
        );

        try {
            String jobJson = objectMapper.writeValueAsString(job);
            redis.opsForList().leftPush(SubmissionWorkerPool.QUEUE_KEY, jobJson);
            log.debug("Submission {} queued for async judging", submission.getId());
        } catch (Exception e) {
            log.error("Failed to queue submission {}: {}", submission.getId(), e.getMessage());
            // Fallback: mark as error so user knows something went wrong
            submission.setStatus(Submission.SubmissionStatus.RE);
            submission.setErrorMessage("Failed to queue submission. Please try again.");
            submissionRepository.save(submission);
        }

        return submission;
    }

    public Submission getSubmission(Long userId, Long problemId) {
        return submissionRepository.findByUser_IdAndProblem_Id(userId, problemId);
    }

    public List<Submission> getUserSubmissions(Long userId) {
        return submissionRepository.findByUser_Id(userId);
    }

    /**
     * Get current status of a submission (for polling fallback).
     */
    public Submission getSubmissionById(Long submissionId) {
        return submissionRepository.findById(submissionId)
            .orElseThrow(() -> new ResourceNotFoundException("Submission not found"));
    }
}
