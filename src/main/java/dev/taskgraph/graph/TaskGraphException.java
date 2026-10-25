package dev.taskgraph.graph;

import java.util.List;

/**
 * Rejected graph operation. {@link #relatedIds()} carries the tasks that explain the rejection
 * (the cycle path, the unresolved dependencies, the dependents...), so callers can build a message.
 */
public final class TaskGraphException extends RuntimeException {

    public enum Reason {
        INVALID_ID,
        INVALID_TITLE,
        DUPLICATE_ID,
        DUPLICATE_TITLE,
        NOT_FOUND,
        SELF_DEPENDENCY,
        DUPLICATE_DEPENDENCY,
        NOT_A_DEPENDENCY,
        CYCLE,
        RESOLVED_TASK_DEPENDENCY,
        ALREADY_ASSIGNED,
        NOT_ASSIGNED,
        ALREADY_RESOLVED,
        UNRESOLVED_DEPENDENCIES,
        HAS_DEPENDENTS
    }

    private final Reason reason;
    private final String taskId;
    private final List<String> relatedIds;

    public TaskGraphException(Reason reason, String taskId) {
        this(reason, taskId, List.of());
    }

    public TaskGraphException(Reason reason, String taskId, List<String> relatedIds) {
        super(reason + " for task '" + taskId + "'" + (relatedIds.isEmpty() ? "" : ": " + relatedIds));
        this.reason = reason;
        this.taskId = taskId;
        this.relatedIds = List.copyOf(relatedIds);
    }

    public Reason reason() {
        return reason;
    }

    public String taskId() {
        return taskId;
    }

    public List<String> relatedIds() {
        return relatedIds;
    }
}
