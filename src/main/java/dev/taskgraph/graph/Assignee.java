package dev.taskgraph.graph;

import java.util.Objects;
import java.util.UUID;

/**
 * Player a task is assigned to. The name is stored so it can be shown while the player is offline.
 */
public record Assignee(UUID uuid, String name) {

    public Assignee {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(name, "name");
    }
}
