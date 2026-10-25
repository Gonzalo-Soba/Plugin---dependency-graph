package dev.taskgraph.graph;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable snapshot of a task. {@link TaskGraph} replaces instances on every change.
 *
 * @param id           internal id generated from the title at creation; never shown to players
 * @param title        what players see and type; unique ignoring case, accents and spacing
 * @param icon         namespaced material key, e.g. {@code minecraft:diamond}
 * @param dependencies ids this task depends on, in insertion order; the first one is the visual parent
 * @param assignee     assigned player, or {@code null} when nobody is assigned
 */
public record Task(
        String id,
        String title,
        String description,
        String icon,
        List<String> dependencies,
        Assignee assignee,
        boolean resolved
) {

    public Task {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(icon, "icon");
        description = description == null ? "" : description;
        dependencies = dependencies == null ? List.of() : List.copyOf(dependencies);
    }

    public static Task create(String id, String title, String icon) {
        return new Task(id, title, "", icon, List.of(), null, false);
    }

    public Optional<Assignee> assigneeOptional() {
        return Optional.ofNullable(assignee);
    }

    public Optional<String> visualParent() {
        return dependencies.isEmpty() ? Optional.empty() : Optional.of(dependencies.getFirst());
    }

    /** Dependencies other than the visual parent, listed in the advancement description. */
    public List<String> extraRequirements() {
        return dependencies.isEmpty() ? List.of() : dependencies.subList(1, dependencies.size());
    }

    Task withTitle(String newTitle) {
        return new Task(id, newTitle, description, icon, dependencies, assignee, resolved);
    }

    Task withDescription(String newDescription) {
        return new Task(id, title, newDescription, icon, dependencies, assignee, resolved);
    }

    Task withDependencies(List<String> newDependencies) {
        return new Task(id, title, description, icon, newDependencies, assignee, resolved);
    }

    Task withAssignee(Assignee newAssignee) {
        return new Task(id, title, description, icon, dependencies, newAssignee, resolved);
    }

    Task withResolved(boolean newResolved) {
        return new Task(id, title, description, icon, dependencies, assignee, newResolved);
    }
}
