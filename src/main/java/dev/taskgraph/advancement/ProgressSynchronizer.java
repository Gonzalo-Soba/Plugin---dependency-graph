package dev.taskgraph.advancement;

import com.fren_gor.ultimateAdvancementAPI.AdvancementTab;
import com.fren_gor.ultimateAdvancementAPI.advancement.Advancement;
import com.fren_gor.ultimateAdvancementAPI.database.DatabaseManager;
import com.fren_gor.ultimateAdvancementAPI.database.TeamProgression;
import com.fren_gor.ultimateAdvancementAPI.util.AdvancementKey;
import dev.taskgraph.graph.SyncPlan;
import dev.taskgraph.graph.Task;
import dev.taskgraph.graph.TaskGraph;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Makes each player's UAAPI progress match the graph: resolved tasks are granted, everything else
 * (unresolved or deleted tasks) is revoked.
 */
public final class ProgressSynchronizer {

    private final Logger logger;

    public ProgressSynchronizer(Logger logger) {
        this.logger = logger;
    }

    public void syncOnlinePlayers(TaskGraph graph, AdvancementTab tab) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            sync(player, graph, tab);
        }
    }

    public void sync(Player player, TaskGraph graph, AdvancementTab tab) {
        DatabaseManager database = tab.getDatabaseManager();
        // Players still loading are synced again by PlayerLoadingCompletedEvent.
        if (!database.isLoadedAndOnline(player)) {
            return;
        }
        TeamProgression progression = database.getTeamProgression(player);

        Set<String> knownIds = new LinkedHashSet<>();
        graph.tasks().stream().map(Task::id).forEach(knownIds::add);
        knownIds.addAll(graph.removedIds());

        Set<String> grantedIds = new LinkedHashSet<>();
        for (String id : knownIds) {
            if (progression.getRawProgression(TaskTabRenderer.taskKey(id)) > 0) {
                grantedIds.add(id);
            }
        }

        SyncPlan plan = SyncPlan.between(graph.resolvedIds(), grantedIds);
        for (String id : plan.toGrant()) {
            tab.getAdvancement(TaskTabRenderer.taskKey(id)).grant(player, false);
        }
        for (String id : plan.toRevoke()) {
            AdvancementKey key = TaskTabRenderer.taskKey(id);
            Advancement advancement = tab.getAdvancement(key);
            if (advancement != null) {
                advancement.revoke(player);
            } else {
                // Deleted task: not in the tab anymore, so clear it straight in the database.
                database.setProgression(key, player, 0).exceptionally(error -> {
                    logger.log(Level.WARNING, "Could not clear progress of deleted task " + id + " for " + player.getName(), error);
                    return null;
                });
            }
        }
    }
}
