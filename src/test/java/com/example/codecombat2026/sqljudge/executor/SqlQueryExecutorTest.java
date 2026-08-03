package com.example.codecombat2026.sqljudge.executor;

import com.example.codecombat2026.sqljudge.dto.SqlExecutionResult;
import com.example.codecombat2026.sqljudge.entity.SqlProblem;
import com.example.codecombat2026.sqljudge.normalizer.SqlResultNormalizer;
import com.example.codecombat2026.sqljudge.router.NeonNode;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SqlQueryExecutorTest {

    @Mock private HikariDataSource runDs;
    @Mock private Connection connection;
    @Mock private Statement statement;
    @Mock private ResultSet rs;
    @Mock private ResultSetMetaData md;

    private SqlQueryExecutor executor;
    private NeonNode node;
    private SqlProblem problem;

    @BeforeEach
    void setUp() throws SQLException {
        executor = new SqlQueryExecutor(new SqlResultNormalizer());
        node = new NeonNode("neon-1", null, runDs, 5, 3);
        problem = new SqlProblem();
        problem.setId(1L);
        problem.setSchemaName("q_1");
        problem.setTimeLimitMs(2000);
        problem.setMaxResultRows(100);

        when(runDs.getConnection()).thenReturn(connection);
        lenient().when(connection.createStatement()).thenReturn(statement);
    }

    @Test
    void successfulQueryReturnsOkWithRows() throws SQLException {
        when(statement.executeQuery(anyString())).thenReturn(rs);
        when(rs.getMetaData()).thenReturn(md);
        when(md.getColumnCount()).thenReturn(1);
        when(md.getColumnLabel(1)).thenReturn("name");
        when(rs.next()).thenReturn(true, false);
        when(rs.getObject(1)).thenReturn("alice");

        SqlExecutionResult r = executor.execute(node, problem, "SELECT name FROM employees");

        assertEquals("OK", r.getStatus());
        assertEquals("neon-1", r.getSelectedNode());
        assertEquals(1, r.getResult().getRows().size());
        assertTrue(node.isHealthy());
        assertEquals(0, node.getConsecutiveFailures());
    }

    @Test
    void statementTimeoutReturnsTimeLimitExceeded() throws SQLException {
        when(statement.executeQuery(anyString()))
            .thenThrow(new java.sql.SQLTimeoutException("timeout"));
        when(connection.createStatement()).thenReturn(statement);

        SqlExecutionResult r = executor.execute(node, problem, "SELECT pg_sleep(10)");

        assertEquals("TIME_LIMIT_EXCEEDED", r.getStatus());
        // A user's slow query must NOT trip the circuit breaker.
        assertTrue(node.isHealthy());
    }

    @Test
    void connectionFailureMarksNodeUnavailable() throws SQLException {
        when(runDs.getConnection()).thenThrow(new SQLException("Connection refused", "08001"));

        SqlExecutionResult r = executor.execute(node, problem, "SELECT 1");

        assertEquals("NODE_UNAVAILABLE", r.getStatus());
        assertEquals(1, node.getConsecutiveFailures());
    }

    @Test
    void syntaxErrorIsRuntimeErrorNotNodeFailure() throws SQLException {
        when(statement.executeQuery(anyString()))
            .thenThrow(new SQLException("syntax error at or near \"FROMM\"", "42601"));

        SqlExecutionResult r = executor.execute(node, problem, "SELECT FROMM employees");

        assertEquals("RUNTIME_ERROR", r.getStatus());
        assertTrue(node.isHealthy());
    }

    @Test
    void permissionDeniedIsSecurityViolation() throws SQLException {
        when(statement.executeQuery(anyString()))
            .thenThrow(new SQLException("permission denied for table secret", "42501"));

        SqlExecutionResult r = executor.execute(node, problem, "SELECT * FROM secret");

        assertEquals("SECURITY_VIOLATION", r.getStatus());
        assertTrue(node.isHealthy());
    }
}
