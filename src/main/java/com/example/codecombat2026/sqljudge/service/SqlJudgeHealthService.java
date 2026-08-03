package com.example.codecombat2026.sqljudge.service;

import com.example.codecombat2026.sqljudge.config.SqlJudgeProperties;
import com.example.codecombat2026.sqljudge.router.NeonNode;
import com.example.codecombat2026.sqljudge.router.NeonNodeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lightweight periodic health probe for every Neon node ({@code SELECT 1}).
 *
 * A node that recovers is flipped back to healthy automatically, so a transient
 * Neon hiccup self-heals without admin intervention. Runs only when the SQL
 * judge is enabled.
 */
@Service
public class SqlJudgeHealthService {

    private static final Logger log = LoggerFactory.getLogger(SqlJudgeHealthService.class);

    private final SqlJudgeProperties properties;
    private final NeonNodeRegistry registry;

    @Autowired
    public SqlJudgeHealthService(SqlJudgeProperties properties, NeonNodeRegistry registry) {
        this.properties = properties;
        this.registry = registry;
    }

    @Scheduled(fixedDelayString = "${SQL_JUDGE_HEALTH_INTERVAL_MS:30000}")
    public void healthSweep() {
        if (!properties.isEnabled()) return;
        for (NeonNode node : registry.getAll()) {
            boolean ok = probe(node);
            if (ok) {
                if (!node.isHealthy()) {
                    node.setHealthy(true);
                    log.info("✅ SQL judge node {} recovered", node.getId());
                }
            } else {
                node.recordFailure(); // may flip unhealthy via circuit breaker
            }
        }
    }

    private boolean probe(NeonNode node) {
        try (Connection conn = node.getRunDataSource().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT 1")) {
            return rs.next();
        } catch (Exception e) {
            log.warn("SQL judge health probe failed for node {}: {}", node.getId(), e.getMessage());
            return false;
        }
    }

    /** Snapshot of node health for the admin status endpoint. */
    public List<Map<String, Object>> nodeStatus() {
        return registry.getAll().stream().map(node -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", node.getId());
            m.put("healthy", node.isHealthy());
            m.put("activeQueries", node.getActiveQueries());
            m.put("availablePermits", node.getAvailablePermits());
            m.put("maxConcurrency", node.getMaxConcurrency());
            m.put("recentLatencyMs", node.getRecentLatencyNanos() / 1_000_000.0);
            m.put("consecutiveFailures", node.getConsecutiveFailures());
            return m;
        }).toList();
    }
}
