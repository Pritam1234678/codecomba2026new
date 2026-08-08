package com.example.codecombat2026.sqljudge.service;

import com.example.codecombat2026.sqljudge.config.SqlJudgeProperties;
import com.example.codecombat2026.sqljudge.dto.SqlResult;
import com.example.codecombat2026.sqljudge.entity.SqlProblem;
import com.example.codecombat2026.sqljudge.normalizer.SqlResultNormalizer;
import com.example.codecombat2026.sqljudge.repository.SqlProblemRepository;
import com.example.codecombat2026.sqljudge.router.NeonNode;
import com.example.codecombat2026.sqljudge.router.NeonNodeRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Provisions a SQL question on all configured Neon nodes, then computes the
 * expected result from the official solution.
 *
 * <p>Fully data-driven: problem id → schema name ({@code q_&lt;id&gt;}) is read
 * from {@link SqlProblem}, never hardcoded. Adding question #51 (or a 7th Neon
 * node) requires zero judge code changes.
 *
 * <p>Per node, idempotently:
 * <ol>
 *   <li>Recreate schema {@code q_&lt;id&gt;}: {@code DROP SCHEMA IF EXISTS ... CASCADE; CREATE SCHEMA ...}</li>
 *   <li>Apply {@code setupSql} with {@code search_path = q_&lt;id&gt;} (tables land in the right schema).</li>
 *   <li>Create a read-only role {@code q_&lt;id&gt;_ro} (NOLOGIN) with USAGE+SELECT
 *       on that schema only — the cross-question isolation boundary.</li>
 *   <li>Grant membership to the run user so the executor can {@code SET ROLE}.</li>
 *   <li>Verify {@code officialSolutionSql} runs and produces rows.</li>
 * </ol>
 *
 * <p>Expected result is computed ONCE from the first successfully verified node
 * and stored in the app DB (never duplicated per node). Provisioning status is
 * tracked per node as a JSON map; the problem is only marked enabled when ALL
 * configured nodes succeed. Partial failures are retryable by calling
 * {@link #provision(Long)} again (failed nodes are re-run, successful ones
 * skipped).
 */
@Service
public class SqlProblemProvisioningService {

    private static final Logger log = LoggerFactory.getLogger(SqlProblemProvisioningService.class);

    private final SqlProblemRepository problemRepository;
    private final NeonNodeRegistry nodeRegistry;
    private final SqlJudgeProperties properties;
    private final SqlResultNormalizer normalizer;
    private final SqlExpectedResultCache expectedCache;
    private final ObjectMapper objectMapper;

    @Autowired
    public SqlProblemProvisioningService(SqlProblemRepository problemRepository,
                                         NeonNodeRegistry nodeRegistry,
                                         SqlJudgeProperties properties,
                                         SqlResultNormalizer normalizer,
                                         SqlExpectedResultCache expectedCache,
                                         ObjectMapper objectMapper) {
        this.problemRepository = problemRepository;
        this.nodeRegistry = nodeRegistry;
        this.properties = properties;
        this.normalizer = normalizer;
        this.expectedCache = expectedCache;
        this.objectMapper = objectMapper;
    }

    /**
     * Provision (or re-provision / repair) the given problem on all Neon nodes.
     * Only failed or never-attempted nodes are touched on a retry.
     *
     * @return true if the problem is now fully provisioned on all nodes.
     */
    @Transactional
    public boolean provision(Long problemId) {
        SqlProblem problem = problemRepository.findById(problemId).orElse(null);
        if (problem == null) {
            throw new IllegalArgumentException("SQL problem not found: " + problemId);
        }
        if (problem.getSetupSql() == null || problem.getSetupSql().isBlank()) {
            throw new IllegalArgumentException("setupSql is required before provisioning");
        }
        if (problem.getOfficialSolutionSql() == null || problem.getOfficialSolutionSql().isBlank()) {
            throw new IllegalArgumentException("officialSolutionSql is required before provisioning");
        }

        String schemaName = ensureSchemaName(problem);
        Map<String, Map<String, Object>> status = readStatus(problem);

        boolean allOk = true;
        for (NeonNode node : nodeRegistry.getAll()) {
            Map<String, Object> nodeStatus = status.computeIfAbsent(node.getId(), k -> new LinkedHashMap<>());
            String existing = String.valueOf(nodeStatus.getOrDefault("status", ""));
            if ("PROVISIONED".equals(existing)) {
                continue; // retry only failed / pending nodes
            }
            try {
                provisionOnNode(node, schemaName, problem);
                nodeStatus.clear();
                nodeStatus.put("status", "PROVISIONED");
                nodeStatus.put("at", Instant.now().toString());
            } catch (Exception e) {
                allOk = false;
                nodeStatus.clear();
                nodeStatus.put("status", "FAILED");
                nodeStatus.put("error", sanitize(e.getMessage()));
                nodeStatus.put("at", Instant.now().toString());
                log.error("SQL judge: provisioning q_{} on node {} failed: {}", problemId, node.getId(), e.getMessage());
            }
        }

        problem.setProvisioningStatus(writeStatus(status));
        if (allOk) {
            SqlResult expected = computeExpected(problem, schemaName);
            if (expected == null) {
                allOk = false;
            } else {
                problem.setExpectedResult(writeStatusSafe(objectMapper, expected));
                problem.setEnabled(true);
                expectedCache.put(problemId, expected);
            }
        }
        if (!allOk) {
            problem.setEnabled(false);
        }
        problemRepository.save(problem);
        return allOk;
    }

    private void provisionOnNode(NeonNode node, String schemaName, SqlProblem problem) throws SQLException {
        String runUser = runUserFor(node);
        try (Connection conn = node.getAdminDataSource().getConnection()) {
            conn.setAutoCommit(true);
            try (Statement st = conn.createStatement()) {
                // 1. Clean, isolated schema for this problem.
                st.execute("DROP SCHEMA IF EXISTS " + quoteIdent(schemaName) + " CASCADE");
                st.execute("CREATE SCHEMA " + quoteIdent(schemaName));
                // 2. Apply setup SQL with the schema on the search path.
                st.execute("SET search_path TO " + quoteIdent(schemaName));
                st.execute(problem.getSetupSql());
                st.execute("SET search_path TO pg_catalog"); // don't leak into the shared session

                // 3. Read-only role scoped to THIS schema only (idempotent).
                st.execute("DO $$ BEGIN "
                    + "IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = " + quoteLit(readerRole(schemaName)) + ") THEN "
                    + "CREATE ROLE " + quoteIdent(readerRole(schemaName)) + " NOLOGIN; "
                    + "END IF; "
                    + "END $$;");
                st.execute("GRANT USAGE ON SCHEMA " + quoteIdent(schemaName)
                    + " TO " + quoteIdent(readerRole(schemaName)));
                st.execute("GRANT SELECT ON ALL TABLES IN SCHEMA " + quoteIdent(schemaName)
                    + " TO " + quoteIdent(readerRole(schemaName)));
                st.execute("ALTER DEFAULT PRIVILEGES IN SCHEMA " + quoteIdent(schemaName)
                    + " GRANT SELECT ON TABLES TO " + quoteIdent(readerRole(schemaName)));

                // 4. Grant membership so the run user can SET ROLE (session isolation).
                if (runUser != null && !runUser.isBlank()) {
                    st.execute("GRANT " + quoteIdent(readerRole(schemaName)) + " TO " + quoteIdent(runUser));
                }
            }
        }
    }

    /**
     * Compute the normalized expected result by running the official solution
     * against the first verified node. Called once at publish time.
     */
    private SqlResult computeExpected(SqlProblem problem, String schemaName) {
        for (NeonNode node : nodeRegistry.getAll()) {
            if (!node.isHealthy()) continue;
            try (Connection conn = node.getRunDataSource().getConnection()) {
                conn.setAutoCommit(true);
                try (Statement st = conn.createStatement()) {
                    st.execute("SET ROLE NONE");
                    st.execute("SET search_path TO " + quoteIdent(schemaName));
                    st.setMaxRows(10_000);
                    try (ResultSet rs = st.executeQuery(problem.getOfficialSolutionSql())) {
                        SqlResult result = normalizer.normalize(rs, 10_000);
                        log.info("✅ SQL judge: expected result computed for problem {} ({} cols, {} rows) on node {}",
                            problem.getId(), result.getColumns().size(), result.getRows().size(), node.getId());
                        return result;
                    }
                }
            } catch (Exception e) {
                log.error("SQL judge: failed to compute expected result for problem {} on node {}: {}",
                    problem.getId(), node.getId(), e.getMessage());
            }
        }
        return null;
    }

    private String runUserFor(NeonNode node) {
        SqlJudgeProperties.NodeConfig cfg = nodeConfig(node.getId());
        if (cfg != null && cfg.getUsername() != null && !cfg.getUsername().isBlank()) {
            return cfg.getUsername();
        }
        // Fall back to parsing user= from the JDBC URL.
        return parseUserFromUrl(node.getRunDataSource().getJdbcUrl());
    }

    private SqlJudgeProperties.NodeConfig nodeConfig(String nodeId) {
        for (SqlJudgeProperties.NodeConfig cfg : properties.getNodes()) {
            if (nodeId.equals(cfg.getId())) return cfg;
        }
        return null;
    }

    private String parseUserFromUrl(String url) {
        if (url == null) return null;
        int q = url.indexOf('?');
        if (q < 0) return null;
        for (String pair : url.substring(q + 1).split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0 && "user".equalsIgnoreCase(pair.substring(0, eq))) {
                return pair.substring(eq + 1);
            }
        }
        return null;
    }

    private String ensureSchemaName(SqlProblem problem) {
        if (problem.getSchemaName() == null || problem.getSchemaName().isBlank()) {
            problem.setSchemaName("q_" + problem.getId());
        }
        return problem.getSchemaName();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Map<String, Object>> readStatus(SqlProblem problem) {
        if (problem.getProvisioningStatus() == null || problem.getProvisioningStatus().isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(problem.getProvisioningStatus(), LinkedHashMap.class);
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private String writeStatus(Map<String, Map<String, Object>> status) {
        return writeStatusSafe(objectMapper, status);
    }

    private static String writeStatusSafe(ObjectMapper mapper, Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String readerRole(String schemaName) {
        return schemaName + "_ro";
    }

    private String quoteIdent(String ident) {
        return "\"" + ident.replace("\"", "\"\"") + "\"";
    }

    private String quoteLit(String s) {
        return "'" + s.replace("'", "''") + "'";
    }

    private String sanitize(String msg) {
        if (msg == null) return "Unknown provisioning error";
        // Never leak connection strings / credentials in errors.
        return msg.replaceAll("(jdbc:postgresql://)[^\\s]+", "$1***").substring(0, Math.min(500, msg.length()));
    }
}
