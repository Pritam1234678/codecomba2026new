package com.example.codecombat2026.controller;

import com.example.codecombat2026.entity.DuelEligibleProblem;
import com.example.codecombat2026.entity.Problem;
import com.example.codecombat2026.exception.DuelStateConflictException;
import com.example.codecombat2026.exception.ResourceNotFoundException;
import com.example.codecombat2026.repository.DuelEligibleProblemRepository;
import com.example.codecombat2026.repository.ProblemRepository;
import com.example.codecombat2026.security.services.UserDetailsImpl;
import com.example.codecombat2026.service.DuelService;
import com.example.codecombat2026.util.TimeUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Admin CRUD over {@code duel_eligible_problems}: the curated pool of
 * problems that may be served when a Live Duel is started.
 *
 * <p>All endpoints require {@code ROLE_ADMIN} (matched globally in
 * {@code SecurityConfig} for {@code /api/admin/duels/**} and re-asserted at
 * class level here as defense-in-depth).
 *
 * <p>Endpoints:
 * <ul>
 *     <li>{@code GET /} — list eligible problems with their titles.</li>
 *     <li>{@code POST /{problemId}} — add a problem to the pool. 404 if the
 *         problem id does not exist; 409 with {@code error=ALREADY_ELIGIBLE}
 *         if it is already in the pool.</li>
 *     <li>{@code DELETE /{problemId}} — remove from the pool. Idempotent
 *         204 even if the problem was not in the pool.</li>
 * </ul>
 *
 * <p>Backed by Requirements 11.4 and 12.3.
 */
@RestController
@RequestMapping("/api/admin/duels/eligible-problems")
@PreAuthorize("hasRole('ADMIN')")
public class DuelEligibleProblemAdminController {

    private static final Logger log =
            LoggerFactory.getLogger(DuelEligibleProblemAdminController.class);

    /**
     * View row joining a {@link DuelEligibleProblem} with its problem title.
     * Inlined here because it is tightly coupled to this controller's
     * response shape; if other callers ever need the same view, lift to
     * {@code dto.duel} without changing field names.
     */
    public record EligibleProblemListItem(
            Long problemId,
            String title,
            LocalDateTime addedAt,
            Long addedBy) {
    }

    @Autowired
    private DuelEligibleProblemRepository eligibleRepo;

    @Autowired
    private ProblemRepository problemRepo;

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * List every row in {@code duel_eligible_problems} joined with the
     * matching {@code problems.title}. Cached in Valkey under
     * {@link DuelService#ELIGIBLE_LIST_CACHE_KEY} for
     * {@link DuelService#ELIGIBLE_LIST_CACHE_TTL_SEC} seconds; invalidated
     * on every {@code add} / {@code remove} so admins see their own writes
     * immediately. Uses per-row {@code findById} on the cache miss path —
     * the pool is small (curated) so the N+1 is acceptable; a JOIN-fetch
     * query can replace this if the pool ever grows.
     */
    @GetMapping
    public List<EligibleProblemListItem> list() {
        // Cache fast path
        try {
            String cached = redis.opsForValue().get(DuelService.ELIGIBLE_LIST_CACHE_KEY);
            if (cached != null) {
                return objectMapper.readValue(
                        cached, new TypeReference<List<EligibleProblemListItem>>() {});
            }
        } catch (Exception e) {
            log.debug("Eligible-list cache read failed: {}", e.getMessage());
        }

        // Cache miss — build the list from DB.
        List<DuelEligibleProblem> rows = eligibleRepo.findAll();
        List<EligibleProblemListItem> result = new ArrayList<>(rows.size());
        for (DuelEligibleProblem row : rows) {
            Long problemId = row.getProblemId();
            String title = problemRepo.findById(problemId)
                    .map(Problem::getTitle)
                    .orElse(null);
            result.add(new EligibleProblemListItem(
                    problemId, title, row.getAddedAt(), row.getAddedBy()));
        }

        // Best-effort cache write — tolerate any failure silently.
        try {
            redis.opsForValue().set(
                    DuelService.ELIGIBLE_LIST_CACHE_KEY,
                    objectMapper.writeValueAsString(result),
                    Duration.ofSeconds(DuelService.ELIGIBLE_LIST_CACHE_TTL_SEC));
        } catch (Exception e) {
            log.debug("Eligible-list cache write failed: {}", e.getMessage());
        }

        return result;
    }

    /**
     * Add a problem to the duel pool.
     *
     * @throws ResourceNotFoundException   if {@code problemId} is not a row
     *                                     in {@code problems} (mapped to 404)
     * @throws DuelStateConflictException  with code {@code ALREADY_ELIGIBLE}
     *                                     if the problem is already in the
     *                                     pool (mapped to 409)
     */
    @PostMapping("/{problemId}")
    public EligibleProblemListItem add(
            @PathVariable Long problemId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {

        Problem problem = problemRepo.findById(problemId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Problem not found: " + problemId));

        if (eligibleRepo.existsById(problemId)) {
            throw new DuelStateConflictException(
                    "ALREADY_ELIGIBLE", Map.of("problemId", problemId));
        }

        DuelEligibleProblem row = new DuelEligibleProblem();
        row.setProblemId(problemId);
        row.setAddedAt(TimeUtil.now());
        row.setAddedBy(currentUser != null ? currentUser.getId() : null);

        DuelEligibleProblem saved = eligibleRepo.save(row);
        invalidateEligibleListCache();
        log.info("Duel eligible problem added: problemId={} addedBy={}",
                problemId, saved.getAddedBy());

        return new EligibleProblemListItem(
                saved.getProblemId(),
                problem.getTitle(),
                saved.getAddedAt(),
                saved.getAddedBy());
    }

    /**
     * Remove a problem from the duel pool. Idempotent — returns 204 even if
     * the row was not present.
     */
    @DeleteMapping("/{problemId}")
    public ResponseEntity<Void> remove(@PathVariable Long problemId) {
        if (eligibleRepo.existsById(problemId)) {
            eligibleRepo.deleteById(problemId);
            invalidateEligibleListCache();
            log.info("Duel eligible problem removed: problemId={}", problemId);
        } else {
            log.debug("Duel eligible problem remove no-op (not in pool): problemId={}",
                    problemId);
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * Drop the eligible-list cache. Called on every successful add/remove so
     * the admin UI sees its own writes on the very next poll instead of
     * waiting out the TTL.
     */
    private void invalidateEligibleListCache() {
        try {
            redis.delete(DuelService.ELIGIBLE_LIST_CACHE_KEY);
        } catch (Exception e) {
            log.debug("Eligible-list cache invalidate failed: {}", e.getMessage());
        }
    }
}
