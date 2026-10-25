package dev.taskgraph.graph;

/**
 * Where a task stands, in the order the task list shows them: what can be worked on now comes first.
 */
public enum TaskStatus {
    /** Pending, with every dependency resolved: it can be resolved now. */
    AVAILABLE,
    /** Pending, waiting for at least one unresolved dependency. */
    BLOCKED,
    RESOLVED
}
