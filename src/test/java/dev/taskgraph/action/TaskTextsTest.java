package dev.taskgraph.action;

import dev.taskgraph.graph.Task;
import dev.taskgraph.graph.TaskGraph;
import dev.taskgraph.graph.TaskStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TaskTextsTest {

    @Test
    void taskListMarksResolvedTasks() {
        TaskGraph graph = new TaskGraph();
        Task wood = graph.create("Madera", "minecraft:oak_log");
        Task stone = graph.create("Piedra", "minecraft:cobblestone");
        graph.resolve(wood.id());

        assertEquals("✔ Madera, ✘ Piedra", TaskTexts.taskList(graph, List.of(wood.id(), stone.id())));
    }

    @Test
    void emptyTaskListSaysNothing() {
        assertEquals("nada", TaskTexts.taskList(new TaskGraph(), List.of()));
    }

    @ParameterizedTest
    @EnumSource(TaskStatus.class)
    void everyStatusHasLabelAndMarker(TaskStatus status) {
        assertFalse(TaskTexts.status(status).isBlank());
        assertFalse(TaskTexts.marker(status).isBlank());
    }
}
