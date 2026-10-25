package dev.taskgraph.storage;

import dev.taskgraph.graph.Assignee;
import dev.taskgraph.graph.TaskGraph;
import dev.taskgraph.graph.TaskGraphException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonTaskStoreTest {

    @TempDir
    Path directory;

    private Path file;
    private JsonTaskStore store;

    @BeforeEach
    void setUp() {
        file = directory.resolve("tasks.json");
        store = new JsonTaskStore(file);
    }

    private static TaskGraph sampleGraph() {
        TaskGraph graph = new TaskGraph();
        graph.create("wood", "Juntar madera", "minecraft:oak_log");
        graph.create("stone", "Picar piedra", "minecraft:cobblestone");
        graph.create("furnace", "Armar un horno", "minecraft:furnace");
        graph.addDependency("furnace", "stone");
        graph.addDependency("furnace", "wood");
        graph.setDescription("furnace", "Para cocinar la comida del pueblo: ñandú, ají y más");
        graph.assign("stone", new Assignee(UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e"), "Steve"));
        graph.resolve("wood");
        return graph;
    }

    @Test
    void missingFileLoadsEmptyGraph() {
        assertTrue(store.load().isEmpty());
    }

    @Test
    void emptyFileLoadsEmptyGraph() throws IOException {
        Files.writeString(file, "");

        assertTrue(store.load().isEmpty());
    }

    @Test
    void saveAndLoadRoundTripKeepsEveryField() {
        TaskGraph original = sampleGraph();

        store.save(original);
        TaskGraph loaded = store.load();

        assertEquals(original.tasks(), loaded.tasks());
    }

    @Test
    void roundTripKeepsRemovedIds() {
        TaskGraph original = sampleGraph();
        original.create("old", "Vieja", "minecraft:stone");
        original.remove("old");

        store.save(original);

        assertEquals(Set.of("old"), store.load().removedIds());
    }

    @Test
    void savedFileIsReadableUtf8WithVersion() throws IOException {
        store.save(sampleGraph());

        String json = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(json.contains("\"version\": 1"));
        assertTrue(json.contains("ñandú, ají"));
    }

    @Test
    void saveOverwritesAndLeavesNoTemporaryFile() {
        store.save(sampleGraph());
        TaskGraph smaller = new TaskGraph();
        smaller.create("only", "Una sola", "minecraft:stone");

        store.save(smaller);

        assertEquals(1, store.load().size());
        assertFalse(Files.exists(directory.resolve("tasks.json.tmp")));
    }

    @Test
    void saveCreatesMissingDirectories() {
        JsonTaskStore nested = new JsonTaskStore(directory.resolve("plugins/TaskGraph/tasks.json"));

        nested.save(sampleGraph());

        assertEquals(3, nested.load().size());
    }

    @Test
    void malformedJsonIsRejectedAndFileIsKept() throws IOException {
        Files.writeString(file, "{ not json");

        assertThrows(TaskStoreException.class, store::load);
        assertEquals("{ not json", Files.readString(file));
    }

    @Test
    void graphRuleViolationIsRejected() throws IOException {
        Files.writeString(file, """
                {
                  "version": 1,
                  "tasks": [
                    {"id": "a", "title": "A", "icon": "minecraft:stone", "dependencies": ["b"]},
                    {"id": "b", "title": "B", "icon": "minecraft:stone", "dependencies": ["a"]}
                  ]
                }
                """);

        TaskStoreException exception = assertThrows(TaskStoreException.class, store::load);
        assertInstanceOf(TaskGraphException.class, exception.getCause());
    }

    @Test
    void unsupportedVersionIsRejected() throws IOException {
        Files.writeString(file, "{\"version\": 99, \"tasks\": []}");

        assertThrows(TaskStoreException.class, store::load);
    }

    @Test
    void optionalFieldsDefaultWhenMissing() throws IOException {
        Files.writeString(file, """
                {"version": 1, "tasks": [{"id": "a", "title": "A", "icon": "minecraft:stone"}]}
                """);

        var task = store.load().get("a").orElseThrow();
        assertEquals("", task.description());
        assertTrue(task.dependencies().isEmpty());
        assertTrue(task.assigneeOptional().isEmpty());
        assertFalse(task.resolved());
    }
}
