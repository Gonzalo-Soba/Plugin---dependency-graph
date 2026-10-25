package dev.taskgraph.service;

import com.fren_gor.ultimateAdvancementAPI.AdvancementTab;
import dev.taskgraph.advancement.ProgressSynchronizer;
import dev.taskgraph.advancement.TaskTabRenderer;
import dev.taskgraph.graph.Assignee;
import dev.taskgraph.graph.Task;
import dev.taskgraph.graph.TaskGraph;
import dev.taskgraph.storage.JsonTaskStore;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Single entry point for task changes. Each change runs on a copy of the graph and only replaces it
 * once the JSON file is saved, so a rejected or failed change never leaves memory and disk apart.
 * After every change the tab is rebuilt and online players are synced.
 */
public final class TaskService {

    private final JsonTaskStore store;
    private final TaskTabRenderer renderer;
    private final ProgressSynchronizer synchronizer;

    private TaskGraph graph;
    private AdvancementTab tab;

    public TaskService(JsonTaskStore store, TaskTabRenderer renderer, ProgressSynchronizer synchronizer, TaskGraph graph) {
        this.store = store;
        this.renderer = renderer;
        this.synchronizer = synchronizer;
        this.graph = graph;
    }

    /** Builds the tab for the loaded graph and syncs players already online (e.g. after a reload). */
    public void start() {
        refresh();
    }

    public Optional<Task> task(String id) {
        return graph.get(id);
    }

    public Optional<Task> findByTitle(String title) {
        return graph.findByTitle(title);
    }

    public List<Task> tasks() {
        return graph.tasks();
    }

    /** Read-only view for callers that need graph queries; changes to it are not saved. */
    public TaskGraph snapshot() {
        return graph.copy();
    }

    public Task create(String title, String icon) {
        return apply(next -> next.create(title, icon));
    }

    public Task rename(String id, String newTitle) {
        return apply(next -> next.rename(id, newTitle));
    }

    public Task describe(String id, String description) {
        return apply(next -> next.setDescription(id, description));
    }

    public Task addDependency(String id, String dependencyId) {
        return apply(next -> next.addDependency(id, dependencyId));
    }

    public Task assign(String id, Assignee assignee) {
        return apply(next -> next.assign(id, assignee));
    }

    public Task resolve(String id) {
        return apply(next -> next.resolve(id));
    }

    public Task remove(String id) {
        return apply(next -> next.remove(id));
    }

    public void syncPlayer(Player player) {
        if (tab != null) {
            synchronizer.sync(player, graph, tab);
        }
    }

    private Task apply(Function<TaskGraph, Task> change) {
        TaskGraph next = graph.copy();
        Task result = change.apply(next);
        store.save(next);
        graph = next;
        refresh();
        return result;
    }

    private void refresh() {
        tab = renderer.render(graph);
        synchronizer.syncOnlinePlayers(graph, tab);
    }
}
