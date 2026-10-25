package dev.taskgraph.graph;

import java.util.Optional;
import java.util.UUID;

/**
 * Who may do what with a task. Everything not listed here is open to every player.
 */
public final class TaskPermissions {

    private TaskPermissions() {
    }

    /**
     * Whoever runs a command.
     *
     * @param uuid  the player's id, or {@code null} for the console and other non-player senders
     * @param admin whether the sender has the admin permission (the console always does)
     */
    public record Actor(UUID uuid, boolean admin) {

        public static Actor player(UUID uuid, boolean admin) {
            return new Actor(uuid, admin);
        }

        public static Actor console() {
            return new Actor(null, true);
        }

        boolean is(Optional<PlayerRef> player) {
            return uuid != null && player.filter(ref -> ref.is(uuid)).isPresent();
        }
    }

    /** The creator or an admin. Tasks without a recorded creator can only be removed by admins. */
    public static boolean canRemove(Task task, Actor actor) {
        return actor.admin() || actor.is(task.creatorOptional());
    }

    /** The assignee or an admin. */
    public static boolean canRelease(Task task, Actor actor) {
        return actor.admin() || actor.is(task.assigneeOptional());
    }

    /** Free tasks and your own can be taken by anyone; a task assigned to someone else only by an admin. */
    public static boolean canAssignToSelf(Task task, Actor actor) {
        return actor.admin() || task.assigneeOptional().isEmpty() || actor.is(task.assigneeOptional());
    }
}
