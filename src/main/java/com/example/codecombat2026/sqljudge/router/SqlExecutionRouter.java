package com.example.codecombat2026.sqljudge.router;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Lightweight application-level router that picks the best Neon node for the
 * next submission.
 *
 * Simple strategy (kept deliberately dumb):
 * <ol>
 *   <li>Ignore unhealthy nodes.</li>
 *   <li>Ignore nodes that cannot acquire an execution permit right now (saturated).</li>
 *   <li>Prefer the node with the lowest active query count.</li>
 *   <li>Use recent latency as a tie-breaker.</li>
 * </ol>
 *
 * {@link #select()} returns a node with the permit ALREADY acquired — the
 * caller MUST call {@code node.release(latencyNanos)} in a finally block.
 * Returns {@code null} when every node is unhealthy or saturated; the worker
 * then waits and retries within its max-queue-wait window.
 */
@Component
public class SqlExecutionRouter {

    private final NeonNodeRegistry registry;

    @Autowired
    public SqlExecutionRouter(NeonNodeRegistry registry) {
        this.registry = registry;
    }

    /**
     * Pick the best node and acquire its permit. Caller releases afterwards.
     *
     * @param excludedNodeIds nodes to skip (e.g. the one that just failed, for
     *                        the single alternate-node retry)
     */
    public NeonNode select(List<String> excludedNodeIds) {
        List<NeonNode> candidates = registry.getAll().stream()
            .filter(NeonNode::isHealthy)
            .filter(n -> excludedNodeIds == null || !excludedNodeIds.contains(n.getId()))
            .sorted(Comparator
                .comparingInt(NeonNode::getActiveQueries)
                .thenComparingLong(NeonNode::getRecentLatencyNanos))
            .toList();

        for (NeonNode candidate : candidates) {
            if (candidate.tryAcquire()) {
                return candidate;
            }
        }
        return null;
    }

    /** Convenience overload: single excluded node. */
    public NeonNode selectExcluding(String excludedNodeId) {
        return select(excludedNodeId == null ? List.of() : List.of(excludedNodeId));
    }
}
