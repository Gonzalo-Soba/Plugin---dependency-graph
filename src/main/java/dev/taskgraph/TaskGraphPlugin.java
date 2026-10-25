package dev.taskgraph;

import org.bukkit.plugin.java.JavaPlugin;

public final class TaskGraphPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("TaskGraph enabled");
    }

    @Override
    public void onDisable() {
        getLogger().info("TaskGraph disabled");
    }
}
