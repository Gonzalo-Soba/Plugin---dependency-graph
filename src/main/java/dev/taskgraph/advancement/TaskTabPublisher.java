package dev.taskgraph.advancement;

import com.fren_gor.ultimateAdvancementAPI.AdvancementTab;
import dev.taskgraph.graph.TaskGraph;
import org.bukkit.entity.Player;

/**
 * Keeps the advancement tab in step with the graph: every published graph rebuilds the tab and syncs
 * online players; players who finish loading later are synced against the last published graph.
 */
public final class TaskTabPublisher {

    private final TaskTabRenderer renderer;
    private final ProgressSynchronizer synchronizer;

    private TaskGraph graph;
    private AdvancementTab tab;

    public TaskTabPublisher(TaskTabRenderer renderer, ProgressSynchronizer synchronizer) {
        this.renderer = renderer;
        this.synchronizer = synchronizer;
    }

    public void publish(TaskGraph graph) {
        this.graph = graph;
        tab = renderer.render(graph);
        synchronizer.syncOnlinePlayers(graph, tab);
    }

    public void syncPlayer(Player player) {
        if (tab != null) {
            synchronizer.sync(player, graph, tab);
        }
    }
}
