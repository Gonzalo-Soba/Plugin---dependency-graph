package dev.taskgraph.graph;

/**
 * Grid cell of a task in the advancements screen.
 *
 * @param column depth of the task: longest dependency path to a task without dependencies
 * @param row    order inside the column, starting at 0
 */
public record NodePosition(int column, int row) {
}
