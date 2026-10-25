package dev.taskgraph.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import dev.taskgraph.graph.Task;
import dev.taskgraph.graph.TaskGraph;
import dev.taskgraph.graph.TaskGraphException;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Stores the task graph as a JSON file. This file is the source of truth for every task and its state.
 */
public final class JsonTaskStore {

    static final int FORMAT_VERSION = 1;

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private final Path file;

    public JsonTaskStore(Path file) {
        this.file = file;
    }

    public Path file() {
        return file;
    }

    /** Loads the graph, or returns an empty one if the file does not exist yet. */
    public TaskGraph load() {
        if (Files.notExists(file)) {
            return new TaskGraph();
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            StoredGraph stored = GSON.fromJson(reader, StoredGraph.class);
            if (stored == null) {
                return new TaskGraph();
            }
            if (stored.version() != FORMAT_VERSION) {
                throw new TaskStoreException("Unsupported format version " + stored.version() + " in " + file, null);
            }
            return TaskGraph.of(
                    stored.tasks() == null ? List.of() : stored.tasks(),
                    stored.removedIds() == null ? List.of() : stored.removedIds());
        } catch (IOException | JsonParseException | TaskGraphException exception) {
            throw new TaskStoreException("Could not load tasks from " + file, exception);
        }
    }

    /**
     * Writes to a temporary file first and then moves it over the old one, so a crash mid-write
     * never leaves a truncated task file.
     */
    public void save(TaskGraph graph) {
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                GSON.toJson(new StoredGraph(FORMAT_VERSION, graph.tasks(), List.copyOf(graph.removedIds())), writer);
            }
            try {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new TaskStoreException("Could not save tasks to " + file, exception);
        }
    }

    private record StoredGraph(int version, List<Task> tasks, List<String> removedIds) {
    }
}
