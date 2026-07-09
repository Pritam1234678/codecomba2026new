package com.example.codecombat2026.service;

import com.example.codecombat2026.dto.SubmissionJob;
import com.example.codecombat2026.entity.Problem;
import com.example.codecombat2026.entity.Submission;
import com.example.codecombat2026.entity.User;
import com.example.codecombat2026.exception.ResourceNotFoundException;
import com.example.codecombat2026.proctoring.entity.ProctoringSession;
import com.example.codecombat2026.proctoring.exception.ProctoringForbiddenException;
import com.example.codecombat2026.proctoring.repository.ProctoringSessionRepository;
import com.example.codecombat2026.proctoring.service.ProctoringSessionService;
import com.example.codecombat2026.repository.ContestRegistrationRepository;
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
    @Autowired private ProctoringSessionService proctoringSessionService;
    @Autowired private ProctoringSessionRepository proctoringSessionRepository;
    @Autowired private ContestRegistrationRepository contestRegistrationRepository;

    /**
     * Async submit — saves PENDING to MySQL, pushes job to Valkey queue,
     * returns immediately (< 10ms). Worker pool handles execution.
     *
     * Upsert logic: if user already has a NON-PENDING submission for this problem,
     * we update it. Otherwise create a new row.
     */
    @Transactional
    public Submission submitCodeAsync(Long userId, Long problemId,
                                       String code, Submission.ProgrammingLanguage language) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        // Warm the uid2uname mapping for profile-cache eviction
        try {
            redis.opsForValue().set("uid2uname:" + userId, user.getUsername(),
                java.time.Duration.ofHours(1));
        } catch (Exception ignored) {}
        Problem problem = problemRepository.findById(problemId)
            .orElseThrow(() -> new ResourceNotFoundException("Problem not found"));

        // Contest liveness + registration gates — checked BEFORE the PENDING
        // row is written so a rejected request doesn't leave an orphan row.
        validateContestAccess(problem, userId, true);

        // Look at the latest submission for this user+problem.
        // If it's a finished real submission → reuse the row.
        // If it's PENDING/JUDGING → leave it (could be in flight) and create new.
        // If it's a test run row → create a new one (don't overwrite tests).
        List<Submission> existing = submissionRepository.findByUser_IdAndProblem_IdOrderBySubmittedAtDesc(
            userId, problemId, org.springframework.data.domain.PageRequest.of(0, 1));

        Submission submission = null;
        if (!existing.isEmpty()) {
            Submission latest = existing.get(0);
            Submission.SubmissionStatus s = latest.getStatus();
            // Reuse only if it's a finished verdict (AC/WA/CE/RE/TLE/MLE)
            if (s == Submission.SubmissionStatus.AC || s == Submission.SubmissionStatus.WA
                || s == Submission.SubmissionStatus.CE || s == Submission.SubmissionStatus.RE
                || s == Submission.SubmissionStatus.TLE || s == Submission.SubmissionStatus.MLE) {
                submission = latest;
            }
        }

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
        submission.setUserRoll(null);
        submission.setProblemName(problem.getTitle());

        submission = submissionRepository.save(submission);

        // Evict user submission caches so the dashboard reflects the new PENDING row.
        try {
            redis.delete("submissions:user:" + userId);
            redis.delete("submission:user:problem:" + userId + ":" + problemId);
        } catch (Exception ignored) {}

        // Push job to Valkey queue — worker picks it up asynchronously
        Long contestId = problem.getContest() != null ? problem.getContest().getId() : null;

        // Req 19.2 / 19.3 — proctoring lockout + session tagging.
        // Contest liveness + registration are validated earlier via
        // validateContestAccess(). Here we only handle proctoring:
        //   - terminal-state lockout from a previous attempt
        //     (SELF_QUIT / ADMIN_FORCED / HEARTBEAT_TIMEOUT)
        //   - tag the job with the candidate's currently-active
        //     proctoring session id for verdict-session correlation.
        Long proctoringSessionId = null;
        if (contestId != null) {
            if (proctoringSessionService.isLocked(contestId, userId)) {
                throw new ProctoringForbiddenException("LOCKED_OUT");
            }
            proctoringSessionId = proctoringSessionRepository
                .findByContestIdAndUserIdAndEndedAtIsNull(contestId, userId)
                .map(ProctoringSession::getId)
                .orElse(null);
        }

        SubmissionJob job = new SubmissionJob(
            submission.getId(), userId, problemId, contestId,
            code, language.name(),
            problem.getTimeLimit() != null ? problem.getTimeLimit() : 5.0,
            problem.getMemoryLimit() != null ? problem.getMemoryLimit() : 256,
            false,  // isTestRun = false
            null,   // duelId — practice/contest submissions are not duel-tagged
            proctoringSessionId,  // null for non-proctored contests; Req 19.2
            false   // practice = false (contest submission)
        );

        try {
            String jobJson = objectMapper.writeValueAsString(job);
            redis.opsForList().leftPush(SubmissionWorkerPool.QUEUE_KEY, jobJson);
            log.debug("Submission {} queued for async judging", submission.getId());
        } catch (Exception e) {
            log.error("Failed to queue submission {}: {}", submission.getId(), e.getMessage());
            submission.setStatus(Submission.SubmissionStatus.RE);
            submission.setErrorMessage("Failed to queue submission. Please try again.");
            submissionRepository.save(submission);
        }

        return submission;
    }

    /**
     * Test run — saves to DB with isTestRun flag, pushes job to queue.
     * Verdict arrives via polling on /submissions/{id}/status.
     */
@Transactional
    public Submission testCodeAsync(Long userId, Long problemId,
                                     String code, Submission.ProgrammingLanguage language) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        // Warm the uid2uname mapping for profile-cache eviction
        try {
            redis.opsForValue().set("uid2uname:" + userId, user.getUsername(),
                java.time.Duration.ofHours(1));
        } catch (Exception ignored) {}
        Problem problem = problemRepository.findById(problemId)
            .orElseThrow(() -> new ResourceNotFoundException("Problem not found"));

        // Contest liveness gates — test runs must also respect active/start/end
        // (but not registration — a user may test-run before registering).
        validateContestAccess(problem, userId, false);

        // Create a temporary submission for test run (separate from real submission)
        // We use a new submission each time so it doesn't overwrite the real one
        Submission submission = new Submission();
        submission.setUser(user);
        submission.setProblem(problem);
        submission.setContest(problem.getContest());
        submission.setCode(code);
        submission.setLanguage(language);
        submission.setStatus(Submission.SubmissionStatus.PENDING);
        submission.setSubmittedAt(TimeUtil.now());
        submission.setUserName(user.getUsername());
        submission.setUserRoll(null);
        submission.setProblemName(problem.getTitle());
        submission.setTestRun(true);   // a "Run" — never counts as a real submission
        submission = submissionRepository.save(submission);

        Long contestId = problem.getContest() != null ? problem.getContest().getId() : null;
        SubmissionJob job = new SubmissionJob(
            submission.getId(), userId, problemId, contestId,
            code, language.name(),
            problem.getTimeLimit() != null ? problem.getTimeLimit() : 5.0,
            problem.getMemoryLimit() != null ? problem.getMemoryLimit() : 256,
            true,  // isTestRun = true — no leaderboard update
            null,  // duelId — test runs are not duel-tagged
            null,  // proctoringSessionId — test runs are not proctoring-tagged
            false  // practice = false (contest test run)
        );

        try {
            String jobJson = objectMapper.writeValueAsString(job);
            redis.opsForList().leftPush(SubmissionWorkerPool.QUEUE_KEY, jobJson);
            log.debug("Test run {} queued for user {} problem {}", submission.getId(), userId, problemId);
        } catch (Exception e) {
            log.error("Failed to queue test run: {}", e.getMessage());
        }

        return submission;
    }

    public Submission getSubmission(Long userId, Long problemId) {
        List<Submission> results = submissionRepository.findByUser_IdAndProblem_IdOrderBySubmittedAtDesc(
            userId, problemId, org.springframework.data.domain.PageRequest.of(0, 1));
        return results.isEmpty() ? null : results.get(0);
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

    public long countContestRuns(Long userId, Long problemId) {
        return submissionRepository.countContestRunsByUserAndProblem(userId, problemId);
    }

    public long countContestSubmits(Long userId, Long problemId) {
        return submissionRepository.countContestSubmitsByUserAndProblem(userId, problemId);
    }

    /**
     * Validate that the calling user may submit/run against this problem in its
     * contest. Throws ProctoringForbiddenException (→ 403) on violation so the
     * GlobalExceptionHandler surfaces a clean code+message.
     *
     * Gates (only applied when the problem belongs to a contest):
     *   1. Contest must be active (admin has not deactivated it).
     *   2. Contest must have started (startTime not in the future).
     *   3. Contest must not have ended (endTime not in the past).
     *   4. requireRegistration: the calling user must have a row in
     *      contest_registrations for this contest.
     */
    private void validateContestAccess(Problem problem, Long userId, boolean requireRegistration) {
        com.example.codecombat2026.entity.Contest contest = problem.getContest();
        if (contest == null) return; // standalone / practice problem — no gates
        Long contestId = contest.getId();

        if (!Boolean.TRUE.equals(contest.getActive())) {
            throw new ProctoringForbiddenException(
                "CONTEST_INACTIVE",
                "This contest is not currently active.");
        }
        var now = TimeUtil.now();
        if (contest.getStartTime() != null && contest.getStartTime().isAfter(now)) {
            throw new ProctoringForbiddenException(
                "CONTEST_NOT_STARTED",
                "This contest has not started yet.");
        }
        if (contest.getEndTime() != null && contest.getEndTime().isBefore(now)) {
            throw new ProctoringForbiddenException(
                "CONTEST_ENDED",
                "Contest has ended — submissions are no longer accepted");
        }
        if (requireRegistration && contestId != null
                && !contestRegistrationRepository.existsByContestIdAndUserId(contestId, userId)) {
            throw new ProctoringForbiddenException(
                "NOT_REGISTERED",
                "You must register for this contest before submitting.");
        }
    }
}
