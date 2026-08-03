package com.example.codecombat2026.sqljudge.executor;

import com.example.codecombat2026.sqljudge.dto.SqlExecutionResult;
import com.example.codecombat2026.sqljudge.dto.SqlResult;
import com.example.codecombat2026.sqljudge.entity.SqlProblem;
import com.example.codecombat2026.sqljudge.normalizer.SqlResultNormalizer;
import com.example.codecombat2026.sqljudge.router.NeonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.sql.Statement;

/**
 * Executes a single candidate query against one Neon node.
 *
 * <p>Hot path (as simple as possible):
 * <pre>
 *   acquire connection from node's run pool
 *   SET ROLE NONE                        -- clear any role from a pooled session
 *   SET default_transaction_read_only    -- every tx on this session is read-only
 *   SET ROLE q_&lt;id&gt;_ro               -- PRIMARY boundary: per-problem read-only role
 *   SET search_path = q_&lt;id&gt;
 *   SET statement_timeout = &lt;ms&gt;        -- server-side, mandatory
 *   execute query with JDBC timeout + maxRows
 *   read bounded rows via normalizer
 * </pre>
 *
 * <p>The read-only role grants (per-problem schema) + read-only transaction +
 * server-side statement_timeout + JDBC row cap are all enforced here, so no
 * candidate query can modify data, read another question's schema, or run
 * forever. try-with-resources guarantees every JDBC object is closed.
 *
 * <p>Status semantics: {@code OK} = query ran; {@code TIME_LIMIT_EXCEEDED} /
 * {@code SECURITY_VIOLATION} / {@code RUNTIME_ERROR} = node responded but the
 * query itself failed; {@code NODE_UNAVAILABLE} = we could not even get a
 * connection from the pool (node problem, safe to retry on an alternate node).
 * Only {@code NODE_UNAVAILABLE} marks the node as failing — a user's bad query
 * must never trip the circuit breaker.
 */
@Component
public class SqlQueryExecutor {

    private static final Logger log = LoggerFactory.getLogger(SqlQueryExecutor.class);

    private final SqlResultNormalizer normalizer;

    @Autowired
    public SqlQueryExecutor(SqlResultNormalizer normalizer) {
        this.normalizer = normalizer;
    }

    /**
     * Run {@code candidateSql} for {@code problem} on {@code node}.
     * The caller is responsible for acquiring the node permit and releasing it.
     */
    public SqlExecutionResult execute(NeonNode node, SqlProblem problem, String candidateSql) {
        long start = System.nanoTime();
        int timeoutMs = problem.getTimeLimitMs() > 0 ? problem.getTimeLimitMs() : 2000;
        int maxRows = problem.getMaxResultRows() > 0 ? problem.getMaxResultRows() : 500;
        String schema = problem.getSchemaName();

        try (Connection conn = node.getRunDataSource().getConnection()) {
            conn.setAutoCommit(true);

            try (Statement st = conn.createStatement()) {
                // 0. Reset any role inherited from a previous pooled session
                //    (a connection that last served q_17 may still be q_17_ro).
                st.execute("SET ROLE NONE");
                // 1. Session-level read-only — every future transaction on this
                //    pooled connection is read-only (reliable with autocommit,
                //    unlike SET TRANSACTION READ ONLY which only covers one tx).
                st.execute("SET default_transaction_read_only = on");
                // 2. PRIMARY SECURITY BOUNDARY: drop to the per-problem read-only
                //    role. This role holds ONLY USAGE+SELECT on q_<id>, so even a
                //    fully malicious query cannot touch other schemas/tables. The
                //    run user is granted membership at provisioning time.
                st.execute("SET ROLE " + quoteIdent(schema + "_ro"));
                // 3. Constrain the search path to the problem's schema. Unqualified
                //    names (employees) resolve to q_17 only.
                st.execute("SET search_path TO " + quoteIdent(schema));
                // 4. Server-side timeout — PostgreSQL cancels the query, not just JDBC.
                st.execute("SET statement_timeout = " + timeoutMs);

                // 5. JDBC-side guardrails: wall-clock timeout + bounded fetch.
                st.setQueryTimeout(Math.max(1, (timeoutMs + 999) / 1000));
                st.setMaxRows(maxRows + 1); // +1 so we can flag truncation

                long qStart = System.nanoTime();
                try (ResultSet rs = st.executeQuery(candidateSql)) {
                    SqlResult result = normalizer.normalize(rs, maxRows);
                    long elapsedMs = (System.nanoTime() - qStart) / 1_000_000;
                    node.recordSuccess();
                    return new SqlExecutionResult("OK", result, null, elapsedMs, node.getId());
                }
            }
        } catch (SQLTimeoutException e) {
            return fail(node, "TIME_LIMIT_EXCEEDED",
                "Query timed out after " + timeoutMs + "ms", start, false);
        } catch (SQLException e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            String lower = msg.toLowerCase();
            if (lower.contains("statement timeout") || lower.contains("query timeout")) {
                return fail(node, "TIME_LIMIT_EXCEEDED",
                    "Query timed out after " + timeoutMs + "ms", start, false);
            }
            if (lower.contains("permission denied") || lower.contains("read-only transaction")
                    || lower.contains("cannot execute") || lower.contains("must be owner")) {
                return fail(node, "SECURITY_VIOLATION", "Query rejected: " + msg, start, false);
            }
            if (isConnectionFailure(e)) {
                return fail(node, "NODE_UNAVAILABLE", "Neon node unavailable: " + msg, start, true);
            }
            return fail(node, "RUNTIME_ERROR", "SQL error: " + msg, start, false);
        } catch (Exception e) {
            log.warn("SQL judge: unexpected error on node {} for problem {}: {}",
                node.getId(), problem.getId(), e.getMessage());
            return fail(node, "NODE_UNAVAILABLE",
                e.getMessage() != null ? e.getMessage() : "Internal execution error", start, true);
        }
    }

    private boolean isConnectionFailure(SQLException e) {
        if (e instanceof SQLTimeoutException) return true;
        String sqlState = e.getSQLState();
        if (sqlState == null) return true;
        // Class 08 — connection exception; 57P01 admin shutdown, 57P02 crash,
        // 08P01 protocol violation, etc. Query errors live in class 42 (syntax)
        // or 22 (data) and must NOT count as node failures.
        return sqlState.startsWith("08") || "57P01".equals(sqlState) || "57P02".equals(sqlState)
            || "57P03".equals(sqlState) || "53300".equals(sqlState); // too many connections
    }

    private SqlExecutionResult fail(NeonNode node, String status, String message, long startNanos, boolean nodeFailure) {
        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
        if (nodeFailure) {
            node.recordFailure();
        }
        return new SqlExecutionResult(status, null, message, elapsedMs, node.getId());
    }

    private String quoteIdent(String ident) {
        return "\"" + ident.replace("\"", "\"\"") + "\"";
    }
}
