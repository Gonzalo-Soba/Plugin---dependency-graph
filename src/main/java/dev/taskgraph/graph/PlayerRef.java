package dev.taskgraph.graph;

import java.util.Objects;
import java.util.UUID;

/**
 * Player linked to a task (creator or assignee). The name is stored so it can be shown while the player is offline.
 */
public record PlayerRef(UUID uuid, String name) {

    public PlayerRef {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(name, "name");
    }

    public boolean is(UUID other) {
        return uuid.equals(other);
    }
}
