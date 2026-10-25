package dev.taskgraph;

import com.fren_gor.ultimateAdvancementAPI.UltimateAdvancementAPI;
import dev.taskgraph.advancement.ProgressSynchronizer;
import dev.taskgraph.advancement.TaskTabRenderer;
import dev.taskgraph.graph.TaskGraph;
import dev.taskgraph.listener.PlayerSyncListener;
import dev.taskgraph.service.TaskService;
import dev.taskgraph.storage.JsonTaskStore;
import dev.taskgraph.storage.TaskStoreException;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class TaskGraphPlugin extends JavaPlugin {

    private TaskService service;

    @Override
    public void onEnable() {
        JsonTaskStore store = new JsonTaskStore(getDataPath().resolve("tasks.json"));
        TaskGraph graph;
        try {
            graph = store.load();
        } catch (TaskStoreException exception) {
            // Disabling instead of starting empty keeps the broken file from being overwritten on the next save.
            getLogger().log(Level.SEVERE, "Could not load " + store.file() + ", disabling TaskGraph", exception);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getLogger().info("Loaded " + graph.size() + " tasks");

        UltimateAdvancementAPI api = UltimateAdvancementAPI.getInstance(this);
        service = new TaskService(store, new TaskTabRenderer(api), new ProgressSynchronizer(getLogger()), graph);
        service.start();
        getServer().getPluginManager().registerEvents(new PlayerSyncListener(service), this);
    }

    @Override
    public void onDisable() {
        // UAAPI unregisters this plugin's tabs on PluginDisableEvent.
        getLogger().info("TaskGraph disabled");
    }
}
