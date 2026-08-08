package com.example.codecombat2026.sqljudge.router;

import com.example.codecombat2026.sqljudge.config.SqlJudgeProperties;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Builds and holds the {@link NeonNode} instances, one per configured Neon DB.
 *
 * Each node gets a small bounded HikariCP pool sized to its concurrency cap
 * (plus headroom for pooled endpoint retries), so we never open an unbounded
 * number of physical connections. Credentials come exclusively from env vars
 * via {@code application.properties} placeholders — never hardcoded.
 */
@Component
public class NeonNodeRegistry {

    private static final Logger log = LoggerFactory.getLogger(NeonNodeRegistry.class);

    private final Map<String, NeonNode> nodes = new ConcurrentHashMap<>();

    /** No-arg path for unit tests; production uses the properties constructor. */
    public NeonNodeRegistry() {
    }

    @Autowired
    public NeonNodeRegistry(SqlJudgeProperties properties) {
        for (SqlJudgeProperties.NodeConfig cfg : properties.getNodes()) {
            if (cfg.getId() == null || cfg.getId().isBlank()) {
                log.warn("SQL judge: skipping node with empty id");
                continue;
            }
            if (cfg.getUrl() == null || cfg.getUrl().isBlank()) {
                log.warn("SQL judge: node '{}' has no url — skipped. Set sql.judge.nodes[].url", cfg.getId());
                continue;
            }
            try {
                HikariDataSource adminDs = buildDataSource(cfg.getUrl(), cfg.getUsername(), cfg.getPassword(),
                    cfg.getId() + "-admin", cfg.getMaxConcurrency() + 5);
                HikariDataSource runDs = buildDataSource(cfg.getUrl(), cfg.getUsername(), cfg.getPassword(),
                    cfg.getId() + "-run", cfg.getMaxConcurrency());
                NeonNode node = new NeonNode(cfg.getId(), adminDs, runDs,
                    cfg.getMaxConcurrency(), properties.getFailureThreshold());
                nodes.put(node.getId(), node);
                log.info("✅ SQL judge node '{}' registered (maxConcurrency={})", node.getId(), cfg.getMaxConcurrency());
            } catch (Exception e) {
                log.error("❌ Failed to build SQL judge node '{}': {}", cfg.getId(), e.getMessage());
            }
        }
        if (nodes.isEmpty()) {
            log.warn("⚠️  SQL judge: no Neon nodes configured — submissions will fail with INTERNAL_ERROR.");
        }
    }

    private HikariDataSource buildDataSource(String url, String username, String password,
                                             String poolName, int maxPoolSize) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setPoolName(poolName);
        config.setMaximumPoolSize(Math.max(2, maxPoolSize));
        config.setMinimumIdle(0);
        config.setIdleTimeout(120_000);
        config.setMaxLifetime(1_200_000);
        config.setConnectionTimeout(5_000);
        config.setValidationTimeout(3_000);
        config.setConnectionTestQuery("SELECT 1");
        // Fresh physical connections always start as the connection user — a
        // pooled session that last SET ROLE to a problem's read-only role must
        // never leak that role into a different problem's execution.
        config.setConnectionInitSql("SET ROLE NONE");
        // Neon pooled endpoint may reject transactions spanning multiple
        // statements; default autocommit on is fine since each candidate query
        // is a single statement run in its own transaction.
        config.setAutoCommit(true);
        config.setInitializationFailTimeout(0);
        if (username != null && !username.isBlank()) {
            config.setUsername(username);
        }
        if (password != null) {
            config.setPassword(password);
        }
        return new HikariDataSource(config);
    }

    public List<NeonNode> getAll() {
        return List.copyOf(nodes.values());
    }

    /** Register a node directly (unit tests). */
    public void register(NeonNode node) {
        if (node != null) {
            nodes.put(node.getId(), node);
        }
    }

    public List<NeonNode> getHealthy() {
        return nodes.values().stream().filter(NeonNode::isHealthy).collect(Collectors.toList());
    }

    public NeonNode getById(String id) {
        return nodes.get(id);
    }

    @PreDestroy
    public void shutdown() {
        nodes.values().forEach(NeonNode::close);
    }
}
