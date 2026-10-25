package dev.taskgraph.action;

import dev.taskgraph.graph.TaskGraph;
import dev.taskgraph.graph.TaskStatus;

import java.util.List;

/**
 * Plain-text descriptions shared by {@code /tarea info} and the task dialogs.
 */
public final class TaskTexts {

    private TaskTexts() {
    }

    /** "✔ Madera, ✘ Piedra", or "nada" when empty. Ids that no longer exist are shown as is. */
    public static String taskList(TaskGraph graph, List<String> ids) {
        if (ids.isEmpty()) {
            return "nada";
        }
        return String.join(", ", ids.stream()
                .map(id -> graph.get(id)
                        .map(task -> (task.resolved() ? "✔ " : "✘ ") + task.title())
                        .orElse(id))
                .toList());
    }

    public static String status(TaskStatus status) {
        return switch (status) {
            case AVAILABLE -> "Disponible";
            case BLOCKED -> "Bloqueada";
            case RESOLVED -> "Resuelta";
        };
    }

    /** Short marker used in the task list: ▶ available, ✘ blocked, ✔ resolved. */
    public static String marker(TaskStatus status) {
        return switch (status) {
            case AVAILABLE -> "▶";
            case BLOCKED -> "✘";
            case RESOLVED -> "✔";
        };
    }
}
