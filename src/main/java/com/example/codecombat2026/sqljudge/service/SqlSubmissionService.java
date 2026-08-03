package com.example.codecombat2026.sqljudge.service;

import com.example.codecombat2026.sqljudge.dto.SqlJob;
import com.example.codecombat2026.sqljudge.entity.SqlProblem;
import com.example.codecombat2026.sqljudge.entity.SqlSubmission;
import com.example.codecombat2026.sqljudge.repository.SqlProblemRepository;
import com.example.codecombat2026.sqljudge.repository.SqlSubmissionRepository;
import com.example.codecombat2026.sqljudge.worker.SqlJudgeWorkerPool;
import com.example.codecombat2026.util.TimeUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Producer side of the SQL judge. Accepts a submission, persists it as QUEUED,
 * pushes a tiny job onto the Valkey queue, and returns immediately — the API
 * never blocks on Neon.
 */
@Service
public class SqlSubmissionService {

    private static final Logger log = LoggerFactory.getLogger(SqlSubmissionService.class);

    @Autowired private SqlSubmissionRepository submissionRepository;
    @Autowired private SqlProblemRepository problemRepository;
    @Autowired private StringRedisTemplate redis;
    @Autowired private ObjectMapper objectMapper;

    /**
     * Create a QUEUED submission and enqueue it.
     *
     * @throws IllegalArgumentException if the problem is missing or not enabled
     */
    public SqlSubmission submit(Long userId, Long problemId, String sql, boolean testRun) {
        SqlProblem problem = problemRepository.findById(problemId).orElse(null);
        if (problem == null) {
            throw new IllegalArgumentException("SQL problem not found");
        }
        if (!problem.isEnabled()) {
            throw new IllegalArgumentException("SQL problem is not active");
        }

        SqlSubmission submission = new SqlSubmission();
        submission.setUserId(userId);
        submission.setProblemId(problemId);
        submission.setSubmittedSql(sql);
        submission.setStatus(SqlSubmission.Status.QUEUED);
        submission.setTestRun(testRun);
        submission.setSubmittedAt(TimeUtil.now());
        submission = submissionRepository.save(submission);

        SqlJob job = new SqlJob(submission.getId(), userId, problemId, testRun);
        try {
            String jobJson = objectMapper.writeValueAsString(job);
            redis.opsForList().leftPush(SqlJudgeWorkerPool.QUEUE_KEY, jobJson);
        } catch (Exception e) {
            log.error("Failed to enqueue SQL submission {}: {}", submission.getId(), e.getMessage());
            submission.setStatus(SqlSubmission.Status.INTERNAL_ERROR);
            submission.setErrorMessage("Failed to queue submission. Please try again.");
            submissionRepository.save(submission);
        }
        return submission;
    }
}
