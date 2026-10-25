package dev.taskgraph.graph;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SyncPlanTest {

    @Test
    void grantsResolvedTasksThePlayerIsMissing() {
        SyncPlan plan = SyncPlan.between(Set.of("a", "b"), Set.of("a"));

        assertEquals(Set.of("b"), plan.toGrant());
        assertTrue(plan.toRevoke().isEmpty());
    }

    @Test
    void revokesTasksThatAreNoLongerResolvedOrWereDeleted() {
        SyncPlan plan = SyncPlan.between(Set.of("a"), Set.of("a", "unresolved", "deleted"));

        assertTrue(plan.toGrant().isEmpty());
        assertEquals(Set.of("deleted", "unresolved"), plan.toRevoke());
    }

    @Test
    void isEmptyWhenPlayerIsInSync() {
        SyncPlan plan = SyncPlan.between(Set.of("a", "b"), Set.of("b", "a"));

        assertTrue(plan.isEmpty());
    }

    @Test
    void newPlayerGetsEveryResolvedTask() {
        SyncPlan plan = SyncPlan.between(Set.of("c", "a", "b"), Set.of());

        assertFalse(plan.isEmpty());
        assertEquals(List.of("a", "b", "c"), List.copyOf(plan.toGrant()));
    }

    @Test
    void worksWithGraphResolvedIds() {
        TaskGraph graph = new TaskGraph();
        graph.create("a", "A", "minecraft:stone");
        graph.create("b", "B", "minecraft:stone");
        graph.resolve("a");

        SyncPlan plan = SyncPlan.between(graph.resolvedIds(), Set.of("b"));

        assertEquals(Set.of("a"), plan.toGrant());
        assertEquals(Set.of("b"), plan.toRevoke());
    }
}
