package com.example.codecombat2026.service;

import com.example.codecombat2026.dto.SubmissionJob;
import com.example.codecombat2026.entity.Contest;
import com.example.codecombat2026.entity.Contest.ContestStatus;
import com.example.codecombat2026.entity.PrivateContest;
import com.example.codecombat2026.entity.Problem;
import com.example.codecombat2026.entity.Submission;
import com.example.codecombat2026.entity.Submission.ProgrammingLanguage;
import com.example.codecombat2026.entity.User;
import com.example.codecombat2026.exception.ResourceNotFoundException;
import com.example.codecombat2026.repository.ContestRepository;
import com.example.codecombat2026.repository.PrivateContestRepository;
import com.example.codecombat2026.repository.ProblemRepository;
import com.example.codecombat2026.repository.SubmissionRepository;
import com.example.codecombat2026.repository.UserRepository;
import com.example.codecombat2026.util.TimeUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service for handling code submissions in private contests.
 *
 * This service extends the existing submission flow to support private contests
 * with participant validation and dedicated queue routing.
 *
 * Key responsibilities:
 * - Validate user is a participant using PrivateContestAccessValidator
 * - Validate contest status is LIVE
 * - Create Submission entity with status PENDING
 * - Create SubmissionJob with privateContest flag
 * - Push job to private:submission:queue in Valkey
 * - Return submissionId for SSE tracking
 *
 * Requirements: 11.5, 13.1, 13.2
 */
@Service
public class PrivateContestSubmissionService {

    private static final Logger log = LoggerFactory.getLogger(PrivateContestSubmissionService.class);

    /**
     * Valkey queue key for private contest submissions.
     * Separate from public contest queue (submission:queue).
     */
    private static final String PRIVATE_SUBMISSION_QUEUE = "private:submission:queue";

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProblemRepository problemRepository;

    @Autowired
    private ContestRepository contestRepository;

    @Autowired
    private PrivateContestRepository privateContestRepository;

    @Autowired
    private PrivateContestAccessValidator accessValidator;

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.example.codecombat2026.config.PrivateContestMetricsConfig metricsConfig;

    /**
     * Submit code for a private contest problem.
     *
     * @param contestId The private contest ID
     * @param problemId The problem ID within the contest
     * @param userId The user ID submitting the code
     * @param code The source code to submit
     * @param language The programming language
     * @return The created Submission entity with PENDING status
     * @throws ResponseStatusException if validation fails (403 FORBIDDEN, 409 CONFLICT)
     * @throws ResourceNotFoundException if contest, problem, or user not found
     */
    @Transactional
    public Submission submitCode(Long contestId, Long problemId, Long userId,
                                 String code, ProgrammingLanguage language) {
        // Validate user exists
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Validate contest exists
        Contest contest = contestRepository.findById(contestId)
            .orElseThrow(() -> new ResourceNotFoundException("Contest not found"));

        // Validate this is a private contest
        PrivateContest privateContest = privateContestRepository.findByContestId(contestId)
            .orElseThrow(() -> new ResourceNotFoundException("Private contest not found"));

        // Validate user is a participant (Requirement 11.5)
        if (!accessValidator.isParticipant(contestId, userId)) {
            log.warn("Non-participant user {} attempted submission to private contest {}", userId, contestId);
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You must be a participant to submit to this private contest"
            );
        }

        // Validate contest status is LIVE (Requirement 13.1)
        if (contest.getStatus() != ContestStatus.LIVE) {
            log.warn("User {} attempted submission to private contest {} with status {}",
                userId, contestId, contest.getStatus());
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Submissions are only allowed when the contest is LIVE"
            );
        }

        // Validate problem exists and belongs to this contest
        Problem problem = problemRepository.findById(problemId)
            .orElseThrow(() -> new ResourceNotFoundException("Problem not found"));

        // Verify problem is attached to this contest
        boolean isProblemInContest = problem.getContests().stream()
            .anyMatch(c -> c.getId().equals(contestId));
        if (!isProblemInContest) {
            log.warn("User {} attempted submission for problem {} not in contest {}",
                userId, problemId, contestId);
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Problem does not belong to this contest"
            );
        }

        // Create Submission entity with status PENDING (Requirement 13.1)
        Submission submission = new Submission();
        submission.setUser(user);
        submission.setProblem(problem);
        submission.setContest(contest);
        submission.setCode(code);
        submission.setLanguage(language);
        submission.setStatus(Submission.SubmissionStatus.PENDING);
        submission.setSubmittedAt(TimeUtil.now());
        submission.setUserName(user.getUsername());
        submission.setProblemName(problem.getTitle());
        submission.setScore(null);
        submission.setTestCasesPassed(null);
        submission.setTotalTestCases(null);
        submission.setErrorMessage(null);
        submission.setTestCaseDetails(null);
        submission.setTimeConsumed(null);
        submission.setTestRun(false);

        // Persist submission
        submission = submissionRepository.save(submission);

        log.info("Created submission {} for user {} in private contest {} problem {}",
            submission.getId(), userId, contestId, problemId);

        // Evict user submission caches
        try {
            redis.delete("submissions:user:" + userId);
            redis.delete("submission:user:problem:" + userId + ":" + problemId);
        } catch (Exception e) {
            log.warn("Failed to evict submission cache for user {}: {}", userId, e.getMessage());
        }

        // Create SubmissionJob with privateContest flag (Requirement 13.2)
        SubmissionJob job = new SubmissionJob(
            submission.getId(),
            userId,
            problemId,
            contestId,
            code,
            language.name(),
            problem.getTimeLimit() != null ? problem.getTimeLimit() : 5.0,
            problem.getMemoryLimit() != null ? problem.getMemoryLimit() : 256,
            false,  // isTestRun = false
            null,   // duelId = null (not a duel submission)
            null,   // proctoringSessionId = null (proctoring handled separately if enabled)
            privateContest.getId()  // privateContestId for routing to private queue
        );

        // Push job to private:submission:queue in Valkey (Requirement 13.2)
        try {
            String jobJson = objectMapper.writeValueAsString(job);
            redis.opsForList().leftPush(PRIVATE_SUBMISSION_QUEUE, jobJson);
            log.info("Pushed submission job {} to private queue for contest {}", submission.getId(), contestId);
            
            // Increment metrics counter
            metricsConfig.incrementSubmissions();
            log.debug("Incremented private_contest_submission_total metric");
        } catch (Exception e) {
            log.error("Failed to queue private contest submission {}: {}", submission.getId(), e.getMessage());
            // Mark submission as failed
            submission.setStatus(Submission.SubmissionStatus.RE);
            submission.setErrorMessage("Failed to queue submission. Please try again.");
            submissionRepository.save(submission);
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to queue submission for judging"
            );
        }

        return submission;
    }
}
