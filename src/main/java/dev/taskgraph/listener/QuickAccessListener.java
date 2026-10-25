package dev.taskgraph.listener;

import dev.taskgraph.dialog.QuickAccess;
import dev.taskgraph.dialog.TaskDialogs;
import io.papermc.paper.connection.PlayerGameConnection;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

/**
 * Opens the live task list when a player presses "Abrir lista" in the quick access dialog
 * (quick actions key or pause menu). Same list as {@code /tareas}.
 */
public final class QuickAccessListener implements Listener {

    private final Plugin plugin;
    private final TaskDialogs dialogs;

    public QuickAccessListener(Plugin plugin, TaskDialogs dialogs) {
        this.plugin = plugin;
        this.dialogs = dialogs;
    }

    @EventHandler
    public void onCustomClick(PlayerCustomClickEvent event) {
        if (!event.getIdentifier().equals(QuickAccess.OPEN_LIST)
                || !(event.getCommonConnection() instanceof PlayerGameConnection connection)) {
            return;
        }
        Player player = connection.getPlayer();
        // The event's thread isn't documented; dialogs and task changes belong on the main thread.
        if (Bukkit.isPrimaryThread()) {
            dialogs.openList(player);
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {
                    dialogs.openList(player);
                }
            });
        }
    }
}
