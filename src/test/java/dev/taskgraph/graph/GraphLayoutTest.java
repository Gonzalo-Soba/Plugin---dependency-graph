package dev.taskgraph.graph;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphLayoutTest {

    private TaskGraph graph;

    @BeforeEach
    void setUp() {
        graph = new TaskGraph();
    }

    private void createAll(String... ids) {
        for (String id : ids) {
            graph.create(id, "minecraft:stone");
        }
    }

    @Test
    void emptyGraphHasNoPositions() {
        assertTrue(GraphLayout.compute(graph).isEmpty());
    }

    @Test
    void tasksWithoutDependenciesShareFirstColumnInCreationOrder() {
        createAll("a", "b", "c");

        Map<String, NodePosition> positions = GraphLayout.compute(graph);

        assertEquals(new NodePosition(0, 0), positions.get("a"));
        assertEquals(new NodePosition(0, 1), positions.get("b"));
        assertEquals(new NodePosition(0, 2), positions.get("c"));
    }

    @Test
    void columnIsLongestDependencyPath() {
        createAll("base", "middle", "top");
        graph.addDependency("middle", "base");
        // Visual parent is base, but middle forces top into column 2.
        graph.addDependency("top", "base");
        graph.addDependency("top", "middle");

        Map<String, NodePosition> positions = GraphLayout.compute(graph);

        assertEquals(0, positions.get("base").column());
        assertEquals(1, positions.get("middle").column());
        assertEquals(2, positions.get("top").column());
    }

    @Test
    void childrenFollowTheRowOfTheirVisualParent() {
        createAll("wood", "stone", "pickaxe", "furnace", "table");
        graph.addDependency("furnace", "stone");
        graph.addDependency("table", "wood");
        graph.addDependency("pickaxe", "stone");

        Map<String, NodePosition> positions = GraphLayout.compute(graph);

        // Column 1: table (parent wood, row 0) before pickaxe and furnace (parent stone, row 1).
        assertEquals(new NodePosition(1, 0), positions.get("table"));
        assertEquals(new NodePosition(1, 1), positions.get("pickaxe"));
        assertEquals(new NodePosition(1, 2), positions.get("furnace"));
    }

    @Test
    void noTwoTasksShareAPosition() {
        createAll("a", "b", "c", "d", "e", "f");
        graph.addDependency("c", "a");
        graph.addDependency("d", "a");
        graph.addDependency("d", "b");
        graph.addDependency("e", "c");
        graph.addDependency("f", "d");
        graph.addDependency("f", "e");

        Map<String, NodePosition> positions = GraphLayout.compute(graph);

        assertEquals(graph.size(), positions.size());
        assertEquals(graph.size(), new HashSet<>(positions.values()).size());
    }

    @Test
    void everyDependencyIsInAnEarlierColumn() {
        createAll("a", "b", "c", "d");
        graph.addDependency("b", "a");
        graph.addDependency("c", "b");
        graph.addDependency("d", "a");
        graph.addDependency("d", "c");

        Map<String, NodePosition> positions = GraphLayout.compute(graph);

        for (Task task : graph.tasks()) {
            for (String dependencyId : task.dependencies()) {
                assertTrue(positions.get(dependencyId).column() < positions.get(task.id()).column(),
                        dependencyId + " should be left of " + task.id());
            }
        }
    }

    @Test
    void iteratesInTopologicalOrderAndIsStable() {
        createAll("b", "a");
        graph.addDependency("b", "a");

        Map<String, NodePosition> first = GraphLayout.compute(graph);

        assertEquals(List.of("a", "b"), List.copyOf(first.keySet()));
        assertEquals(first, GraphLayout.compute(graph));
        assertEquals(Set.of(new NodePosition(0, 0), new NodePosition(1, 0)), Set.copyOf(first.values()));
    }
}
