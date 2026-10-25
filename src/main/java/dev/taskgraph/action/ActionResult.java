package dev.taskgraph.action;

import dev.taskgraph.graph.Task;

import java.util.List;
import java.util.Optional;

/**
 * Outcome of a {@link TaskActions} call, as plain text so commands and dialogs can style it their own way.
 *
 * @param success whether the change was applied
 * @param task    the task after the change, or before it when the action failed; {@code null} if unknown
 * @param message main line for the actor (success or error), or {@code null} when there is nothing to say
 * @param details extra informational lines
 */
public record ActionResult(boolean success, Task task, String message, List<String> details) {

    public ActionResult {
        details = List.copyOf(details);
    }

    static ActionResult success(Task task, String message, String... details) {
        return new ActionResult(true, task, message, List.of(details));
    }

    static ActionResult failure(Task task, String message) {
        return new ActionResult(false, task, message, List.of());
    }

    public Optional<Task> taskOptional() {
        return Optional.ofNullable(task);
    }

    public Optional<String> messageOptional() {
        return Optional.ofNullable(message);
    }
}
