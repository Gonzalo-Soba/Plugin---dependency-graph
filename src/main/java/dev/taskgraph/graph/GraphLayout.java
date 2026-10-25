package dev.taskgraph.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Grid layout for the advancements screen. The column is the longest dependency path, so every
 * prerequisite is drawn to the left of the tasks that need it. Inside a column, tasks are ordered by
 * the row of their visual parent and then by topological order, which keeps siblings together.
 */
public final class GraphLayout {

    private GraphLayout() {
    }

    /** Positions keyed by task id, iterated in topological order. */
    public static Map<String, NodePosition> compute(TaskGraph graph) {
        List<Task> order = graph.topologicalOrder();
        Map<String, Integer> topologicalIndex = new HashMap<>();
        Map<String, Integer> columns = new HashMap<>();
        Map<Integer, List<Task>> tasksByColumn = new TreeMap<>();
        for (Task task : order) {
            topologicalIndex.put(task.id(), topologicalIndex.size());
            int column = task.dependencies().stream()
                    .mapToInt(dependencyId -> columns.get(dependencyId) + 1)
                    .max()
                    .orElse(0);
            columns.put(task.id(), column);
            tasksByColumn.computeIfAbsent(column, key -> new ArrayList<>()).add(task);
        }

        // Columns are filled left to right, so a visual parent always has its row before its children.
        Map<String, NodePosition> positions = new HashMap<>();
        for (Map.Entry<Integer, List<Task>> entry : tasksByColumn.entrySet()) {
            List<Task> columnTasks = entry.getValue();
            columnTasks.sort(Comparator
                    .comparingInt((Task task) -> task.visualParent().map(parentId -> positions.get(parentId).row()).orElse(-1))
                    .thenComparingInt(task -> topologicalIndex.get(task.id())));
            for (int row = 0; row < columnTasks.size(); row++) {
                positions.put(columnTasks.get(row).id(), new NodePosition(entry.getKey(), row));
            }
        }

        Map<String, NodePosition> ordered = new LinkedHashMap<>();
        for (Task task : order) {
            ordered.put(task.id(), positions.get(task.id()));
        }
        return Collections.unmodifiableMap(ordered);
    }
}
