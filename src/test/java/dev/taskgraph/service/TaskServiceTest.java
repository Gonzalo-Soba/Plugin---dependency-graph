package dev.taskgraph.service;

import dev.taskgraph.graph.Task;
import dev.taskgraph.graph.TaskGraph;
import dev.taskgraph.graph.TaskGraphException;
import dev.taskgraph.storage.JsonTaskStore;
import dev.taskgraph.storage.TaskStoreException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskServiceTest {

    private static final String ICON = "minecraft:stone";

    @TempDir
    Path directory;

    private final List<TaskGraph> published = new ArrayList<>();

    private TaskService service(JsonTaskStore store, TaskGraph graph) {
        return new TaskService(store, graph, published::add);
    }

    @Test
    void startPublishesLoadedGraph() {
        TaskGraph graph = new TaskGraph();
        TaskService service = service(new JsonTaskStore(directory.resolve("tasks.json")), graph);

        service.start();

        assertEquals(1, published.size());
        assertSame(graph, published.getFirst());
    }

    @Test
    void changeIsSavedAndPublished() {
        JsonTaskStore store = new JsonTaskStore(directory.resolve("tasks.json"));
        TaskService service = service(store, new TaskGraph());

        Task task = service.create("Mesa", ICON, null);

        assertEquals(1, published.size());
        assertTrue(published.getFirst().contains(task.id()));
        assertTrue(store.load().contains(task.id()));
    }

    @Test
    void publishedGraphIsNotModifiedByLaterChanges() {
        TaskService service = service(new JsonTaskStore(directory.resolve("tasks.json")), new TaskGraph());
        service.create("Mesa", ICON, null);
        TaskGraph first = published.getFirst();

        service.create("Silla", ICON, null);

        assertEquals(1, first.size());
        assertEquals(2, published.getLast().size());
    }

    @Test
    void rejectedChangeIsNotSavedNorPublished() {
        JsonTaskStore store = new JsonTaskStore(directory.resolve("tasks.json"));
        TaskService service = service(store, new TaskGraph());
        service.create("Mesa", ICON, null);
        published.clear();

        assertThrows(TaskGraphException.class, () -> service.create("mesa", ICON, null));

        assertTrue(published.isEmpty());
        assertEquals(1, service.tasks().size());
        assertEquals(1, store.load().size());
    }

    @Test
    void failedSaveKeepsPreviousGraph() throws IOException {
        // A non-empty directory where the file should be makes the final move fail.
        Path file = directory.resolve("tasks.json");
        Files.createDirectories(file.resolve("blocker"));
        TaskService service = service(new JsonTaskStore(file), new TaskGraph());

        assertThrows(TaskStoreException.class, () -> service.create("Mesa", ICON, null));

        assertTrue(published.isEmpty());
        assertTrue(service.tasks().isEmpty());
        assertFalse(service.findByTitle("Mesa").isPresent());
    }
}
