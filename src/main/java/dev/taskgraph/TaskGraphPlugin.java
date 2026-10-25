package dev.taskgraph;

import dev.taskgraph.graph.TaskGraph;
import dev.taskgraph.storage.JsonTaskStore;
import dev.taskgraph.storage.TaskStoreException;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class TaskGraphPlugin extends JavaPlugin {

    private JsonTaskStore store;
    private TaskGraph graph;

    @Override
    public void onEnable() {
        store = new JsonTaskStore(getDataPath().resolve("tasks.json"));
        try {
            graph = store.load();
        } catch (TaskStoreException exception) {
            // Disabling instead of starting empty keeps the broken file from being overwritten on the next save.
            getLogger().log(Level.SEVERE, "Could not load " + store.file() + ", disabling TaskGraph", exception);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getLogger().info("Loaded " + graph.size() + " tasks");
    }

    @Override
    public void onDisable() {
        getLogger().info("TaskGraph disabled");
    }
}
