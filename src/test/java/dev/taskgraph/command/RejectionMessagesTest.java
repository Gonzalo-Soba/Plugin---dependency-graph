package dev.taskgraph.command;

import dev.taskgraph.graph.Task;
import dev.taskgraph.graph.TaskGraph;
import dev.taskgraph.graph.TaskGraphException;
import dev.taskgraph.graph.TaskGraphException.Reason;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RejectionMessagesTest {

    private static Function<String, String> titlesOf(TaskGraph graph) {
        return id -> graph.get(id).map(Task::title).orElse(id);
    }

    @ParameterizedTest
    @EnumSource(Reason.class)
    void everyReasonHasAMessage(Reason reason) {
        String message = RejectionMessages.of(new TaskGraphException(reason, "a", List.of("b")), Function.identity());

        assertFalse(message.isBlank());
    }

    @Test
    void cycleShowsThePathByTitle() {
        TaskGraph graph = new TaskGraph();
        Task table = graph.create("Mesa de trabajo", "minecraft:crafting_table");
        Task wood = graph.create("Madera", "minecraft:oak_log");
        graph.addDependency(table.id(), wood.id());

        TaskGraphException exception = assertThrows(TaskGraphException.class,
                () -> graph.addDependency(wood.id(), table.id()));

        assertEquals("Esa dependencia crea un ciclo: \"Madera\" → \"Mesa de trabajo\" → \"Madera\".",
                RejectionMessages.of(exception, titlesOf(graph)));
    }

    @Test
    void unresolvedDependenciesAreListedByTitle() {
        TaskGraph graph = new TaskGraph();
        Task furnace = graph.create("Horno", "minecraft:furnace");
        Task stone = graph.create("Piedra", "minecraft:cobblestone");
        Task wood = graph.create("Madera", "minecraft:oak_log");
        graph.addDependency(furnace.id(), stone.id());
        graph.addDependency(furnace.id(), wood.id());

        TaskGraphException exception = assertThrows(TaskGraphException.class, () -> graph.resolve(furnace.id()));

        assertEquals("No se puede resolver \"Horno\": faltan \"Piedra\", \"Madera\".",
                RejectionMessages.of(exception, titlesOf(graph)));
    }

    @Test
    void duplicateTitleNamesTheExistingTask() {
        TaskGraph graph = new TaskGraph();
        graph.create("Jardín", "minecraft:rose_bush");

        TaskGraphException exception = assertThrows(TaskGraphException.class,
                () -> graph.create("jardin", "minecraft:stone"));

        assertEquals("Ya existe una tarea llamada \"Jardín\".", RejectionMessages.of(exception, titlesOf(graph)));
    }
}
