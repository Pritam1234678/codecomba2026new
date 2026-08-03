package com.example.codecombat2026.sqljudge.router;

import com.zaxxer.hikari.HikariDataSource;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * One Neon execution node.
 *
 * <ul>
 *   <li>{@code adminDataSource} — owner/pooled creds used by provisioning to
 *       create schemas, seed data, and the per-schema read-only role.</li>
 *   <li>{@code runDataSource} — read-only creds used for candidate queries
 *       (falls back to adminDataSource when not separately configured).</li>
 * </ul>
 *
 * Concurrency is bounded by a {@link Semaphore} of {@code maxConcurrency}
 * permits — never more than that many in-flight queries on this node. Health
 * tracking powers the circuit breaker: after {@code failureThreshold}
 * consecutive failures the node is marked unhealthy and skipped by the router
 * until a {@code SELECT 1} probe succeeds.
 */
@Data
public class NeonNode {

    private static final Logger log = LoggerFactory.getLogger(NeonNode.class);

    private final String id;
    private final HikariDataSource adminDataSource;
    private final HikariDataSource runDataSource;
    private final int maxConcurrency;

    private final Semaphore permits;
    private final AtomicInteger activeQueries = new AtomicInteger(0);
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicLong recentLatencyNanos = new AtomicLong(0);
    private final int failureThreshold;

    private volatile boolean healthy = true;

    public NeonNode(String id, HikariDataSource adminDataSource, HikariDataSource runDataSource,
                    int maxConcurrency, int failureThreshold) {
        this.id = id;
        this.adminDataSource = adminDataSource;
        this.runDataSource = runDataSource;
        this.maxConcurrency = maxConcurrency;
        this.failureThreshold = failureThreshold;
        this.permits = new Semaphore(maxConcurrency, true);
    }

    /** Try to take one execution slot. Returns false if the node is saturated. */
    public boolean tryAcquire() {
        boolean acquired = permits.tryAcquire();
        if (acquired) {
            activeQueries.incrementAndGet();
        }
        return acquired;
    }

    /** Release one execution slot and record observed latency. */
    public void release(long latencyNanos) {
        permits.release();
        activeQueries.decrementAndGet();
        recordLatency(latencyNanos);
    }

    /** Call when a query against this node failed. Marks unhealthy past the threshold. */
    public void recordFailure() {
        int failures = consecutiveFailures.incrementAndGet();
        if (failures >= failureThreshold) {
            setHealthy(false);
            log.warn("SQL judge node {} marked unhealthy after {} consecutive failures", id, failures);
        }
    }

    /** Call on any successful execution — resets the failure streak. */
    public void recordSuccess() {
        consecutiveFailures.set(0);
    }

    private void recordLatency(long latencyNanos) {
        // Simple exponential moving average (alpha 0.2) — cheap, deterministic.
        long prev = recentLatencyNanos.get();
        if (prev == 0) {
            recentLatencyNanos.set(latencyNanos);
        } else {
            long updated = (long) (prev * 0.8 + latencyNanos * 0.2);
            recentLatencyNanos.compareAndSet(prev, updated);
        }
    }

    public int getActiveQueries() {
        return activeQueries.get();
    }

    public long getRecentLatencyNanos() {
        return recentLatencyNanos.get();
    }

    public int getAvailablePermits() {
        return permits.availablePermits();
    }

    public int getConsecutiveFailures() {
        return consecutiveFailures.get();
    }

    /** Closes both datasources. Called on graceful shutdown. */
    public void close() {
        if (adminDataSource != null && adminDataSource != runDataSource) {
            adminDataSource.close();
        }
        if (runDataSource != null) {
            runDataSource.close();
        }
    }
}
