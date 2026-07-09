package com.example.codecombat2026.service;

import com.example.codecombat2026.dto.ProblemDTO;
import com.example.codecombat2026.entity.Contest;
import com.example.codecombat2026.entity.ContestProblem;
import com.example.codecombat2026.entity.PrivateContest;
import com.example.codecombat2026.entity.Problem;
import com.example.codecombat2026.exception.ConflictException;
import com.example.codecombat2026.exception.ForbiddenException;
import com.example.codecombat2026.exception.ResourceNotFoundException;
import com.example.codecombat2026.repository.ContestProblemRepository;
import com.example.codecombat2026.repository.PrivateContestRepository;
import com.example.codecombat2026.repository.ProblemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Service for managing problem selection on a Private_Contest.
 *
 * <p>Provides:
 * <ul>
 *   <li>{@link #browseProblems} — paginated browse of the shared problem
 *       bank, restricted to problems a Contest_Host is allowed to reuse
 *       ({@code visibility} of {@code PUBLIC} or {@code PRIVATE_AVAILABLE}).
 *       {@code ADMIN_ONLY} and {@code PRIVATE_OWNED} (other hosts') problems
 *       are never returned.</li>
 *   <li>{@link #attachProblems} — persists the Contest_Host's selection into
 *       the existing {@code contest_problems} junction table with a
 *       sequential {@code displayOrder}.</li>
 *   <li>{@link #removeProblems} — detaches problems from the contest.</li>
 * </ul>
 *
 * <p>Both mutating operations require the caller to be the contest's host
 * and the contest to still be in {@code UPCOMING} status — once a contest
 * goes {@code LIVE} the problem set is frozen (Requirement 8.7).
 *
 * Requirements: 8.1, 8.2, 8.4, 8.5, 8.6, 8.7, 30.2, 30.3
 */
@Service
public class PrivateContestProblemService {

    private static final Logger log = LoggerFactory.getLogger(PrivateContestProblemService.class);

    /**
     * Problem visibility values a Contest_Host is allowed to attach to a
     * Private_Contest. {@code ADMIN_ONLY} is always excluded; {@code
     * PRIVATE_OWNED} problems are only attachable by their creator, which is
     * out of scope for the shared browse/attach flow (Requirement 30.5
     * covers editing/deleting one's own PRIVATE_OWNED problems separately).
     */
    private static final Set<String> ATTACHABLE_VISIBILITIES = Set.of("PUBLIC", "PRIVATE_AVAILABLE");

    @Autowired
    private PrivateContestRepository privateContestRepository;

    @Autowired
    private ContestProblemRepository contestProblemRepository;

    @Autowired
    private ProblemRepository problemRepository;

    @Autowired
    private PrivateContestCacheService cacheService;

    /**
     * Browse problems available for attachment to a Private_Contest.
     *
     * <p>Only returns problems with {@code visibility} of {@code PUBLIC} or
     * {@code PRIVATE_AVAILABLE} — {@code ADMIN_ONLY} problems are never
     * returned, satisfying Requirements 8.1, 30.2, 30.3.
     *
     * @param difficulty optional exact-match filter on problem level (EASY/MEDIUM/HARD), null/blank = no filter
     * @param search     optional case-insensitive title substring filter, null/blank = no filter
     * @param page       zero-based page index
     * @param size       page size
     * @return a page of {@link ProblemDTO}
     */
    @Transactional(readOnly = true)
    public Page<ProblemDTO> browseProblems(String difficulty, String search, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        Page<Problem> problems = problemRepository.browseByVisibility(
                blankToNull(difficulty), blankToNull(search), pageable);
        return problems.map(this::toProblemDTO);
    }

    /**
     * Attach a batch of existing problems to a Private_Contest, assigning
     * each a sequential {@code displayOrder} continuing after the highest
     * order already present (Requirement 8.6).
     *
     * <p>Idempotent per problem: a problem already attached to the contest
     * is silently skipped rather than duplicated or re-ordered.
     *
     * @param contestId  the Contest ID (not the private_contests.id) to attach to
     * @param problemIds problems to attach
     * @param hostUserId the user performing the action — must be the contest host
     * @return the {@link ContestProblem} rows created by this call (excludes skipped duplicates)
     * @throws ResourceNotFoundException if the private contest or a problem does not exist
     * @throws ForbiddenException        if the caller is not the host, or a problem's visibility disallows attachment
     * @throws ConflictException         if the contest is no longer {@code UPCOMING}
     */
    @Transactional
    public List<ContestProblem> attachProblems(Long contestId, List<Long> problemIds, Long hostUserId) {
        validateHostAndUpcoming(contestId, hostUserId);

        if (problemIds == null || problemIds.isEmpty()) {
            return Collections.emptyList();
        }

        Integer maxOrder = contestProblemRepository.findMaxDisplayOrderByContestId(contestId);
        int nextOrder = (maxOrder == null ? 0 : maxOrder) + 1;

        List<ContestProblem> created = new ArrayList<>();
        for (Long problemId : problemIds) {
            Problem problem = problemRepository.findById(problemId)
                    .orElseThrow(() -> new ResourceNotFoundException("Problem not found: " + problemId));

            if (!ATTACHABLE_VISIBILITIES.contains(problem.getVisibility())) {
                throw new ForbiddenException(
                        "Problem " + problemId + " is not available for private contests");
            }

            if (contestProblemRepository.existsByContestIdAndProblemId(contestId, problemId)) {
                log.debug("Problem {} already attached to contest {}, skipping", problemId, contestId);
                continue;
            }

            ContestProblem cp = new ContestProblem();
            cp.setContestId(contestId);
            cp.setProblemId(problemId);
            cp.setDisplayOrder(nextOrder++);
            created.add(contestProblemRepository.save(cp));
        }

        cacheService.invalidateContestCache(contestId);
        log.info("Attached {} problem(s) to private contest {}", created.size(), contestId);

        return created;
    }

    /**
     * Remove a batch of problems from a Private_Contest.
     *
     * <p>Idempotent: problem IDs that are not currently attached are
     * silently ignored.
     *
     * @param contestId  the Contest ID to remove problems from
     * @param problemIds problems to remove
     * @param hostUserId the user performing the action — must be the contest host
     * @return the number of {@code contest_problems} rows actually deleted
     * @throws ResourceNotFoundException if the private contest does not exist
     * @throws ForbiddenException        if the caller is not the host
     * @throws ConflictException         if the contest is no longer {@code UPCOMING}
     */
    @Transactional
    public int removeProblems(Long contestId, List<Long> problemIds, Long hostUserId) {
        validateHostAndUpcoming(contestId, hostUserId);

        if (problemIds == null || problemIds.isEmpty()) {
            return 0;
        }

        int removed = 0;
        for (Long problemId : problemIds) {
            removed += contestProblemRepository.deleteByContestIdAndProblemId(contestId, problemId);
        }

        cacheService.invalidateContestCache(contestId);
        log.info("Removed {} problem(s) from private contest {}", removed, contestId);

        return removed;
    }

    // ─── Private Helper Methods ───────────────────────────────────────────────

    /**
     * Validate that {@code hostUserId} is the host of the private contest
     * backing {@code contestId} and that the contest has not yet started.
     *
     * @return the resolved {@link PrivateContest} entity, for callers that need it
     */
    private PrivateContest validateHostAndUpcoming(Long contestId, Long hostUserId) {
        PrivateContest privateContest = privateContestRepository.findByContestId(contestId)
                .orElseThrow(() -> new ResourceNotFoundException("Private contest not found"));

        if (!privateContest.getHostUser().getId().equals(hostUserId)) {
            throw new ForbiddenException("Only the contest host can manage problems for this contest");
        }

        Contest contest = privateContest.getContest();
        if (contest.getStatus() != Contest.ContestStatus.UPCOMING) {
            throw new ConflictException("Cannot modify problems after the contest has started");
        }

        return privateContest;
    }

    private ProblemDTO toProblemDTO(Problem p) {
        ProblemDTO dto = new ProblemDTO();
        dto.setId(p.getId());
        dto.setTitle(p.getTitle());
        dto.setDescription(p.getDescription());
        dto.setInputFormat(p.getInputFormat());
        dto.setOutputFormat(p.getOutputFormat());
        dto.setConstraints(p.getConstraints());
        dto.setTimeLimit(p.getTimeLimit());
        dto.setMemoryLimit(p.getMemoryLimit());
        dto.setActive(p.getActive());
        dto.setExample1(p.getExample1());
        dto.setExample2(p.getExample2());
        dto.setExample3(p.getExample3());
        dto.setImages(p.getImages());
        dto.setLevel(p.getLevel());
        dto.setVisibility(p.getVisibility());
        dto.setCreatedBy(p.getCreatedBy());
        return dto;
    }

    private static String blankToNull(String s) {
        if (s == null) {
            return null;
        }
        return s.trim().isEmpty() ? null : s;
    }
}
