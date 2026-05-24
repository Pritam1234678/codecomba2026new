package com.example.codecombat2026.controller;

import com.example.codecombat2026.dto.duel.DuelMatchView;
import com.example.codecombat2026.dto.duel.DuelMetrics;
import com.example.codecombat2026.service.DuelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

/**
 * Admin-only observability and intervention surface for Live Duel Mode.
 *
 * <p>Backed by Requirements 11.1 / 11.2 / 11.3 / 11.4. All endpoints under
 * {@code /api/admin/duels/*} are gated by {@code ROLE_ADMIN} both at the
 * security filter ({@code SecurityConfig.requestMatchers("/api/admin/duels/**")
 * .hasRole("ADMIN")}) and re-asserted at class level here as
 * defense-in-depth.
 *
 * <p>Endpoints:
 * <ul>
 *     <li>{@code GET /metrics} — point-in-time {@link DuelMetrics} snapshot
 *         (active match count, queue depth, finished/abandoned today,
 *         live SSE connection count). The dashboard polls every 5 s.</li>
 *     <li>{@code GET ?status=&limit=&offset=} — paged list of matches
 *         filtered by status. {@code limit} clamps to {@code [1, 200]} and
 *         a 400 is returned for out-of-range values.</li>
 *     <li>{@code POST /{matchId}/cancel} — force-finalize a match as
 *         {@code ABANDONED} with no winner. Maps to 404 via
 *         {@code GlobalExceptionHandler} when the match id is unknown.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/admin/duels")
@CrossOrigin(origins = "*", maxAge = 3600)
@PreAuthorize("hasRole('ADMIN')")
public class AdminDuelController {

    private static final Logger log = LoggerFactory.getLogger(AdminDuelController.class);

    private final DuelService duelService;

    public AdminDuelController(DuelService duelService) {
        this.duelService = duelService;
    }

    /**
     * Snapshot of live duel runtime metrics.
     *
     * @return {@link DuelMetrics} computed at request time
     */
    @GetMapping("/metrics")
    public DuelMetrics getMetrics() {
        return duelService.getMetrics();
    }

    /**
     * Paged list of duel matches filtered by status.
     *
     * @param status the {@code DuelMatch.Status} name to filter on
     *               (default {@code IN_PROGRESS})
     * @param limit  page size, must be in {@code [1, 200]} (default 50)
     * @param offset zero-based row offset (default 0)
     * @return paged {@link DuelMatchView} projection
     * @throws ResponseStatusException 400 if {@code limit} is outside
     *                                 {@code [1, 200]}
     */
    @GetMapping
    public Page<DuelMatchView> listMatches(
            @RequestParam(defaultValue = "IN_PROGRESS") String status,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        if (limit < 1 || limit > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "limit must be between 1 and 200");
        }
        return duelService.listMatches(status, limit, offset);
    }

    /**
     * Force-finalize a match as {@code ABANDONED} with no winner. The
     * {@link com.example.codecombat2026.exception.DuelNotFoundException}
     * thrown by {@link DuelService#adminCancel(UUID)} for an unknown match
     * is mapped to 404 by {@code GlobalExceptionHandler}.
     */
    @PostMapping("/{matchId}/cancel")
    public ResponseEntity<Map<String, String>> cancel(@PathVariable UUID matchId) {
        duelService.adminCancel(matchId);
        log.info("Duel admin-cancelled match={}", matchId);
        return ResponseEntity.ok(Map.of(
                "matchId", matchId.toString(),
                "outcome", "ABANDONED"));
    }
}
