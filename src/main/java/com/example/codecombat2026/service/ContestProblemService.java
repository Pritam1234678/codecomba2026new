package com.example.codecombat2026.service;

import com.example.codecombat2026.entity.Contest;
import com.example.codecombat2026.entity.ContestProblem;
import com.example.codecombat2026.entity.ContestProblemId;
import com.example.codecombat2026.entity.Problem;
import com.example.codecombat2026.exception.ResourceNotFoundException;
import com.example.codecombat2026.repository.ContestProblemRepository;
import com.example.codecombat2026.repository.ContestRepository;
import com.example.codecombat2026.repository.ProblemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Owns attach / detach / list operations against the {@code contest_problems}
 * junction. This service is the single place that mutates the M:N
 * relationship between contests and problems and is responsible for keeping
 * the legacy {@code problems.contest_id} column dual-written during the
 * transition window.
 *
 * <p>Per the design, callers must NOT mutate the relationship through
 * {@code Contest#getProblems()} or {@code Problem#getContests()} directly —
 * those collections bypass dual-write and cache eviction.
 *
 * <p>Method bodies for {@link #attach}, {@link #attachMany}, and
 * {@link #detach} are filled in by subsequent tasks (6.2 – 6.5). The simple
 * delegating reads ({@link #listProblemsForContest},
 * {@link #listContestsForProblem}, {@link #findAvailable}) are implemented
 * here as one-liners since they involve no business logic beyond delegation.
 */
@Service
public class ContestProblemService {

    @Autowired private ContestProblemRepository contestProblemRepository;
    @Autowired private ProblemRepository problemRepository;
    @Autowired private ContestRepository contestRepository;
    @Autowired private ProblemService problemService;

    /**
     * Attach an existing problem to a contest.
     *
     * <p>Idempotent: if the {@code (contestId, problemId)} pair already
     * exists in {@code contest_problems}, the existing row is returned and
     * no duplicate is inserted.
     *
     * <p>Dual-writes the legacy {@code problems.contest_id} column ONLY when
     * it is currently {@code NULL}; never overwrites an existing legacy
     * pointer (that would silently re-home a problem).
     *
     * <p>Evicts the {@code problems:contest:{contestId}} and
     * {@code problem:{problemId}} cache keys.
     *
     * @throws com.example.codecombat2026.exception.ResourceNotFoundException
     *         if either id does not exist.
     */
    @Transactional
    public ContestProblem attach(Long contestId, Long problemId) {
        if (!contestRepository.existsById(contestId)) {
            throw new ResourceNotFoundException("Contest not found with id: " + contestId);
        }
        if (!problemRepository.existsById(problemId)) {
            throw new ResourceNotFoundException("Problem not found with id: " + problemId);
        }

        // Idempotent: short-circuit if already attached.
        if (contestProblemRepository.existsByContestIdAndProblemId(contestId, problemId)) {
            ContestProblem existing = contestProblemRepository
                    .findById(new ContestProblemId(contestId, problemId))
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "ContestProblem disappeared between exists and findById: ("
                                    + contestId + ", " + problemId + ")"));
            // Defensive cache eviction so callers always observe a fresh roster.
            problemService.evictContestProblems(contestId);
            problemService.evictProblem(problemId);
            return existing;
        }

        // Insert junction row. addedAt is filled in by @PrePersist on the entity.
        ContestProblem cp = new ContestProblem();
        cp.setContestId(contestId);
        cp.setProblemId(problemId);
        cp.setDisplayOrder(0);
        ContestProblem saved = contestProblemRepository.save(cp);

        // Dual-write to legacy problems.contest_id ONLY when it is currently NULL.
        // Never overwrite an existing legacy pointer — that would silently re-home
        // a problem and violate Property 10.
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Problem not found with id: " + problemId));
        if (problem.getContestId() == null) {
            problem.setContest(contestRepository.getReferenceById(contestId));
            problemRepository.save(problem);
        }

        // Cache eviction so the contest detail page and per-problem caches
        // reflect the new attachment immediately.
        problemService.evictContestProblems(contestId);
        problemService.evictProblem(problemId);

        return saved;
    }

    /**
     * Bulk-attach a list of problems to a contest atomically.
     *
     * <p>The whole batch runs inside a single transaction so either every
     * row in {@code problemIds} is attached or none of them are — partial
     * attachments are not observable on rollback.
     *
     * <p>Each individual attach follows the same idempotence and dual-write
     * rules as {@link #attach(Long, Long)}.
     */
    @Transactional
    public List<ContestProblem> attachMany(Long contestId, List<Long> problemIds) {
        if (problemIds == null || problemIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<ContestProblem> results = new ArrayList<>(problemIds.size());
        for (Long pid : problemIds) {
            // Any exception (e.g. ResourceNotFoundException for an unknown
            // problemId) propagates and rolls back the entire transaction,
            // so partial attachments are never observable.
            results.add(this.attach(contestId, pid));
        }
        return results;
    }

    /**
     * Detach a problem from a contest. The problem itself, its
     * {@code code_snippets}, and its test cases are NOT deleted.
     *
     * <p>Idempotent: if no junction row exists for the pair the call is a
     * no-op (caches are still evicted defensively).
     *
     * <p>If the legacy {@code problems.contest_id} pointed at the contest
     * being detached, it is repointed to the smallest remaining
     * {@code contest_id} in the junction for that problem, or set to
     * {@code NULL} if no attachments remain.
     *
     * <p>Evicts the {@code problems:contest:{contestId}} and
     * {@code problem:{problemId}} cache keys.
     */
    @Transactional
    public void detach(Long contestId, Long problemId) {
        int rowsDeleted = contestProblemRepository
                .deleteByContestIdAndProblemId(contestId, problemId);

        if (rowsDeleted == 0) {
            // Idempotent no-op: nothing to detach. Evict caches defensively
            // so callers always observe a fresh roster regardless of state.
            problemService.evictContestProblems(contestId);
            problemService.evictProblem(problemId);
            return;
        }

        // Defensive guard: if the problem somehow no longer exists (e.g. it
        // was deleted concurrently and the FK cascade already cleared the
        // junction row that we just tried to delete), the junction is
        // already consistent — skip the legacy-column repoint and just
        // evict caches.
        Problem problem = problemRepository.findById(problemId).orElse(null);
        if (problem == null) {
            problemService.evictContestProblems(contestId);
            problemService.evictProblem(problemId);
            return;
        }

        // Repoint the legacy problems.contest_id column only if it was
        // pointing at the contest we just detached from. Otherwise leave it
        // alone — it already references a contest the problem still belongs
        // to (or NULL), and we must not silently re-home it (Property 10).
        if (problem.getContestId() != null && problem.getContestId().equals(contestId)) {
            List<ContestProblem> remaining =
                    contestProblemRepository.findByProblemId(problemId);
            if (remaining.isEmpty()) {
                problem.setContest(null);
            } else {
                // Deterministic survivor: smallest remaining contestId.
                Long survivor = remaining.stream()
                        .map(ContestProblem::getContestId)
                        .min(Long::compare)
                        .get();
                problem.setContest(contestRepository.getReferenceById(survivor));
            }
            problemRepository.save(problem);
        }

        problemService.evictContestProblems(contestId);
        problemService.evictProblem(problemId);
    }

    /**
     * Return every problem currently attached to {@code contestId}, ordered
     * by {@code (display_order ASC, added_at ASC)}.
     *
     * <p>Reads strictly through the junction; the legacy
     * {@code problems.contest_id} column is not consulted.
     */
    public List<Problem> listProblemsForContest(Long contestId) {
        return problemRepository.findByContestId(contestId);
    }

    /**
     * Return every contest the given problem is currently attached to.
     *
     * <p>Reads junction rows for {@code problemId}, then resolves the
     * corresponding {@link Contest} entities in a single
     * {@code findAllById} call.
     *
     * @throws com.example.codecombat2026.exception.ResourceNotFoundException
     *         if the problem does not exist (raised in task 6.4).
     */
    public List<Contest> listContestsForProblem(Long problemId) {
        if (!problemRepository.existsById(problemId)) {
            throw new ResourceNotFoundException("Problem not found with id: " + problemId);
        }
        List<ContestProblem> rows = contestProblemRepository.findByProblemId(problemId);
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> contestIds = rows.stream()
                .map(ContestProblem::getContestId)
                .toList();
        return contestRepository.findAllById(contestIds);
    }

    /**
     * Return problems NOT currently attached to {@code contestId}, optionally
     * narrowed by a case-insensitive title substring ({@code search}) and an
     * exact level match ({@code level}). Blank or {@code null} filter values
     * are normalised to {@code null} so the underlying query treats them as
     * "not provided".
     */
    public List<Problem> findAvailable(Long contestId, String search, String level) {
        return problemRepository.findAvailableForContest(
                contestId,
                blankToNull(search),
                blankToNull(level));
    }

    /**
     * Returns {@code null} if {@code s} is {@code null} or contains only
     * whitespace; otherwise returns {@code s} unchanged. Used to coerce
     * empty query parameters into JPQL {@code IS NULL} branches.
     */
    private static String blankToNull(String s) {
        if (s == null) {
            return null;
        }
        return s.trim().isEmpty() ? null : s;
    }
}
