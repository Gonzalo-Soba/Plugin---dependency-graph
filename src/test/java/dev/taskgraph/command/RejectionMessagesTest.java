package dev.taskgraph.command;

import dev.taskgraph.graph.TaskGraph;
import dev.taskgraph.graph.TaskGraphException;
import dev.taskgraph.graph.TaskGraphException.Reason;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RejectionMessagesTest {

    @ParameterizedTest
    @EnumSource(Reason.class)
    void everyReasonHasAMessage(Reason reason) {
        String message = RejectionMessages.of(new TaskGraphException(reason, "a", List.of("b")));

        assertFalse(message.isBlank());
    }

    @Test
    void cycleShowsThePath() {
        TaskGraph graph = new TaskGraph();
        graph.create("a", "A", "minecraft:stone");
        graph.create("b", "B", "minecraft:stone");
        graph.addDependency("a", "b");

        TaskGraphException exception = assertThrows(TaskGraphException.class, () -> graph.addDependency("b", "a"));

        assertEquals("Esa dependencia crea un ciclo: b → a → b.", RejectionMessages.of(exception));
    }

    @Test
    void unresolvedDependenciesAreListed() {
        String message = RejectionMessages.of(
                new TaskGraphException(Reason.UNRESOLVED_DEPENDENCIES, "horno", List.of("piedra", "madera")));

        assertEquals("No se puede resolver 'horno': faltan 'piedra', 'madera'.", message);
    }
}
