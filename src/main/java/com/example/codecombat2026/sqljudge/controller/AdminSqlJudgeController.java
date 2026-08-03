package com.example.codecombat2026.sqljudge.controller;

import com.example.codecombat2026.dto.MessageResponse;
import com.example.codecombat2026.sqljudge.config.SqlJudgeProperties;
import com.example.codecombat2026.sqljudge.dto.SqlProblemRequest;
import com.example.codecombat2026.sqljudge.entity.SqlProblem;
import com.example.codecombat2026.sqljudge.repository.SqlProblemRepository;
import com.example.codecombat2026.sqljudge.service.SqlExpectedResultCache;
import com.example.codecombat2026.sqljudge.service.SqlJudgeHealthService;
import com.example.codecombat2026.sqljudge.service.SqlProblemProvisioningService;
import com.example.codecombat2026.sqljudge.worker.SqlJudgeWorkerPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin API for the SQL judge. Mounted under /api/admin/sql, so the existing
 * {@code /api/admin/** hasRole('ADMIN')} rule in SecurityConfig gates every
 * endpoint — no new security wiring needed.
 *
 * <p>Creating a problem is a two-step, data-driven operation:
 * <ol>
 *   <li>{@code POST /api/admin/sql/problems} persists metadata (enabled=false).</li>
 *   <li>The same call provisions it on ALL Neon nodes (idempotent — retryable
 *       on partial failure) and, only when every node is verified, computes the
 *       expected result from the official solution and flips enabled=true.</li>
 * </ol>
 *
 * <p>{@code GET /api/admin/sql/status} surfaces node health, queue depth and
 * active jobs so an operator can watch a load test in real time.
 */
@RestController
@RequestMapping("/api/admin/sql")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSqlJudgeController {

    @Autowired private SqlProblemRepository problemRepository;
    @Autowired private SqlProblemProvisioningService provisioningService;
    @Autowired private SqlExpectedResultCache expectedCache;
    @Autowired private SqlJudgeHealthService healthService;
    @Autowired private SqlJudgeWorkerPool workerPool;
    @Autowired private SqlJudgeProperties properties;

    /** Create a SQL problem and provision it on all Neon nodes. */
    @PostMapping("/problems")
    public ResponseEntity<?> createProblem(@RequestBody SqlProblemRequest req) {
        if (req.getTitle() == null || req.getTitle().isBlank()) {
            return ResponseEntity.badRequest().body(new MessageResponse("title is required"));
        }
        if (req.getSetupSql() == null || req.getSetupSql().isBlank()) {
            return ResponseEntity.badRequest().body(new MessageResponse("setupSql is required"));
        }
        if (req.getOfficialSolutionSql() == null || req.getOfficialSolutionSql().isBlank()) {
            return ResponseEntity.badRequest().body(new MessageResponse("officialSolutionSql is required"));
        }
        String mode = req.getComparisonMode() == null ? "UNORDERED" : req.getComparisonMode().toUpperCase();
        if (!"ORDERED".equals(mode) && !"UNORDERED".equals(mode)) {
            return ResponseEntity.badRequest().body(new MessageResponse("comparisonMode must be ORDERED or UNORDERED"));
        }

        SqlProblem p = new SqlProblem();
        p.setTitle(req.getTitle().trim());
        p.setDescription(req.getDescription());
        p.setSetupSql(req.getSetupSql());
        p.setOfficialSolutionSql(req.getOfficialSolutionSql());
        p.setComparisonMode(mode);
        p.setTimeLimitMs(req.getTimeLimitMs() != null && req.getTimeLimitMs() > 0
            ? req.getTimeLimitMs() : properties.getDefaultTimeoutMs());
        p.setMaxResultRows(req.getMaxResultRows() != null && req.getMaxResultRows() > 0
            ? req.getMaxResultRows() : properties.getMaxResultRows());
        p.setEnabled(false);
        p = problemRepository.save(p);

        boolean provisioned = provisioningService.provision(p.getId());

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("problem", p);
        out.put("provisioned", provisioned);
        out.put("message", provisioned
            ? "Problem provisioned on all Neon nodes and enabled"
            : "Problem saved but provisioning partially failed — check provisioningStatus and re-run POST /problems/{id}/provision");
        return ResponseEntity.status(provisioned ? HttpStatus.CREATED : HttpStatus.INTERNAL_SERVER_ERROR)
            .body(out);
    }

    /** Re-run provisioning (repairs failed nodes). */
    @PostMapping("/problems/{id}/provision")
    public ResponseEntity<?> reprovision(@PathVariable Long id) {
        boolean ok = provisioningService.provision(id);
        SqlProblem p = problemRepository.findById(id).orElse(null);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("problemId", id);
        out.put("provisioned", ok);
        if (p != null) out.put("provisioningStatus", p.getProvisioningStatus());
        return ok
            ? ResponseEntity.ok(out)
            : ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(out);
    }

    /** Toggle a problem on/off without re-provisioning. */
    @PatchMapping("/problems/{id}/enabled")
    public ResponseEntity<?> setEnabled(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        Boolean enabled = body.get("enabled");
        if (enabled == null) {
            return ResponseEntity.badRequest().body(new MessageResponse("enabled is required"));
        }
        problemRepository.updateEnabled(id, enabled);
        if (!enabled) expectedCache.evict(id);
        return ResponseEntity.ok(new MessageResponse(enabled ? "Enabled" : "Disabled"));
    }

    /** Full list — admin view exposes setup/solution/provisioning internals. */
    @GetMapping("/problems")
    public List<SqlProblem> listAll(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "100") int size) {
        return problemRepository
            .findAllByOrderByCreatedAtDesc(PageRequest.of(page, Math.min(size, 500)))
            .getContent();
    }

    @GetMapping("/problems/{id}")
    public ResponseEntity<?> getProblem(@PathVariable Long id) {
        SqlProblem p = problemRepository.findById(id).orElse(null);
        if (p == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MessageResponse("Problem not found"));
        }
        return ResponseEntity.ok(p);
    }

    /** Live judge status: node health, queue depth, active jobs. */
    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("enabled", properties.isEnabled());
        out.put("queueDepth", workerPool.getQueueDepth());
        out.put("activeJobs", workerPool.getActiveJobs());
        out.put("workers", properties.getWorkers());
        out.put("maxInflightQueries", properties.getMaxInflightQueries());
        out.put("nodes", healthService.nodeStatus());
        return out;
    }
}
