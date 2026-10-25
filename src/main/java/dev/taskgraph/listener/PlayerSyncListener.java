package dev.taskgraph.listener;

import com.fren_gor.ultimateAdvancementAPI.events.PlayerLoadingCompletedEvent;
import dev.taskgraph.service.TaskService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Syncs a joining player's advancements with the graph. UAAPI loads progress asynchronously after
 * PlayerJoinEvent, and this event is the first point where the player's progress can be read.
 */
public final class PlayerSyncListener implements Listener {

    private final TaskService service;

    public PlayerSyncListener(TaskService service) {
        this.service = service;
    }

    @EventHandler
    public void onPlayerLoaded(PlayerLoadingCompletedEvent event) {
        service.syncPlayer(event.getPlayer());
    }
}
