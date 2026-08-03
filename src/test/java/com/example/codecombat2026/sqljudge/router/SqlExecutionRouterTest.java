package com.example.codecombat2026.sqljudge.router;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlExecutionRouterTest {

    private NeonNodeRegistry registry;
    private SqlExecutionRouter router;

    private NeonNode node(String id, int maxConcurrency) {
        // Datasources are null — the router only touches id/health/permits/latency.
        return new NeonNode(id, null, null, maxConcurrency, 3);
    }

    @BeforeEach
    void setUp() {
        registry = new NeonNodeRegistry();
        router = new SqlExecutionRouter(registry);
    }

    @Test
    void picksHealthyNode() {
        NeonNode a = node("a", 10);
        NeonNode b = node("b", 10);
        registry.register(a);
        registry.register(b);

        NeonNode chosen = router.select(List.of());
        assertNotNull(chosen);
        assertTrue(chosen.isHealthy());
        chosen.release(1_000_000);
    }

    @Test
    void skipsUnhealthyNodes() {
        NeonNode bad = node("bad", 10);
        bad.setHealthy(false);
        registry.register(bad);

        assertNull(router.select(List.of()));
    }

    @Test
    void returnsNullWhenAllSaturated() {
        NeonNode busy = node("busy", 1);
        registry.register(busy);
        // Take the only permit.
        assertTrue(busy.tryAcquire());

        assertNull(router.select(List.of()));
    }

    @Test
    void selectExcludingSkipsGivenNode() {
        NeonNode a = node("a", 10);
        NeonNode b = node("b", 10);
        registry.register(a);
        registry.register(b);

        NeonNode chosen = router.selectExcluding("a");
        assertNotNull(chosen);
        assertEquals("b", chosen.getId());
        chosen.release(0);
    }

    @Test
    void picksLowestActiveQueryCount() {
        NeonNode light = node("light", 10);
        NeonNode heavy = node("heavy", 10);
        // heavy has an in-flight query already
        assertTrue(heavy.tryAcquire());
        registry.register(light);
        registry.register(heavy);

        NeonNode chosen = router.select(List.of());
        assertNotNull(chosen);
        assertEquals("light", chosen.getId());
        chosen.release(0);
    }

    @Test
    void acquireConsumesAndReleaseRestoresPermit() {
        NeonNode n = node("n", 2);
        registry.register(n);
        assertEquals(2, n.getAvailablePermits());
        NeonNode chosen = router.select(List.of());
        assertEquals(1, n.getAvailablePermits());
        chosen.release(5_000_000);
        assertEquals(2, n.getAvailablePermits());
    }
}
