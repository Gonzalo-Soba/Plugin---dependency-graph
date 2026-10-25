package dev.taskgraph.graph;

import dev.taskgraph.graph.TaskGraphException.Reason;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Directed acyclic graph of tasks. An edge {@code a -> b} means "a depends on b".
 * Every mutating method validates first and leaves the graph untouched when it throws.
 * Iteration order is creation order, which keeps topological order and layout stable.
 */
public final class TaskGraph {

    private static final Pattern ID_PATTERN = Pattern.compile("[a-z0-9_]{1,32}");

    private final Map<String, Task> tasks = new LinkedHashMap<>();
    // Ids of deleted tasks. Players who were offline at deletion may still hold progress for them.
    private final Set<String> removedIds = new LinkedHashSet<>();

    /**
     * Rebuilds a graph from stored tasks, rejecting data that breaks any graph rule.
     */
    public static TaskGraph of(Collection<Task> stored) {
        return of(stored, List.of());
    }

    /**
     * Rebuilds a graph from stored tasks and removed ids. Removed ids that belong to an existing task are dropped.
     */
    public static TaskGraph of(Collection<Task> stored, Collection<String> storedRemovedIds) {
        TaskGraph graph = new TaskGraph();
        for (Task task : stored) {
            validateId(task.id());
            validateTitle(task.id(), task.title());
            if (graph.tasks.putIfAbsent(task.id(), task) != null) {
                throw new TaskGraphException(Reason.DUPLICATE_ID, task.id());
            }
        }
        for (Task task : graph.tasks.values()) {
            Set<String> seen = new HashSet<>();
            for (String dependencyId : task.dependencies()) {
                if (dependencyId.equals(task.id())) {
                    throw new TaskGraphException(Reason.SELF_DEPENDENCY, task.id());
                }
                if (!seen.add(dependencyId)) {
                    throw new TaskGraphException(Reason.DUPLICATE_DEPENDENCY, task.id(), List.of(dependencyId));
                }
                Task dependency = graph.tasks.get(dependencyId);
                if (dependency == null) {
                    throw new TaskGraphException(Reason.NOT_FOUND, dependencyId);
                }
                if (task.resolved() && !dependency.resolved()) {
                    throw new TaskGraphException(Reason.RESOLVED_TASK_DEPENDENCY, task.id(), List.of(dependencyId));
                }
            }
        }
        graph.topologicalOrder();
        for (String removedId : storedRemovedIds) {
            if (!graph.tasks.containsKey(removedId)) {
                graph.removedIds.add(removedId);
            }
        }
        return graph;
    }

    /** Independent copy, so a change can be tried and discarded without touching this graph. */
    public TaskGraph copy() {
        TaskGraph copy = new TaskGraph();
        copy.tasks.putAll(tasks);
        copy.removedIds.addAll(removedIds);
        return copy;
    }

    public Task create(String id, String title, String icon) {
        validateId(id);
        validateTitle(id, title);
        if (tasks.containsKey(id)) {
            throw new TaskGraphException(Reason.DUPLICATE_ID, id);
        }
        Task task = Task.create(id, title.strip(), icon);
        tasks.put(id, task);
        removedIds.remove(id);
        return task;
    }

    public Task setDescription(String id, String description) {
        return replace(require(id).withDescription(description.strip()));
    }

    public Task addDependency(String id, String dependencyId) {
        Task task = require(id);
        Task dependency = require(dependencyId);
        if (id.equals(dependencyId)) {
            throw new TaskGraphException(Reason.SELF_DEPENDENCY, id);
        }
        if (task.dependencies().contains(dependencyId)) {
            throw new TaskGraphException(Reason.DUPLICATE_DEPENDENCY, id, List.of(dependencyId));
        }
        List<String> cycle = pathBetween(dependencyId, id);
        if (!cycle.isEmpty()) {
            List<String> path = new ArrayList<>();
            path.add(id);
            path.addAll(cycle);
            throw new TaskGraphException(Reason.CYCLE, id, path);
        }
        if (task.resolved() && !dependency.resolved()) {
            throw new TaskGraphException(Reason.RESOLVED_TASK_DEPENDENCY, id, List.of(dependencyId));
        }
        List<String> dependencies = new ArrayList<>(task.dependencies());
        dependencies.add(dependencyId);
        return replace(task.withDependencies(dependencies));
    }

    public Task assign(String id, Assignee assignee) {
        return replace(require(id).withAssignee(assignee));
    }

    public Task resolve(String id) {
        Task task = require(id);
        if (task.resolved()) {
            throw new TaskGraphException(Reason.ALREADY_RESOLVED, id);
        }
        List<String> unresolved = unresolvedDependencies(id);
        if (!unresolved.isEmpty()) {
            throw new TaskGraphException(Reason.UNRESOLVED_DEPENDENCIES, id, unresolved);
        }
        return replace(task.withResolved(true));
    }

    public Task remove(String id) {
        require(id);
        List<String> dependents = dependents(id);
        if (!dependents.isEmpty()) {
            throw new TaskGraphException(Reason.HAS_DEPENDENTS, id, dependents);
        }
        removedIds.add(id);
        return tasks.remove(id);
    }

    public Optional<Task> get(String id) {
        return Optional.ofNullable(tasks.get(id));
    }

    public boolean contains(String id) {
        return tasks.containsKey(id);
    }

    public int size() {
        return tasks.size();
    }

    public boolean isEmpty() {
        return tasks.isEmpty();
    }

    /** All tasks in creation order. */
    public List<Task> tasks() {
        return List.copyOf(tasks.values());
    }

    /** Ids of the tasks that depend directly on {@code id}, in creation order. */
    public List<String> dependents(String id) {
        require(id);
        return tasks.values().stream()
                .filter(task -> task.dependencies().contains(id))
                .map(Task::id)
                .toList();
    }

    public List<String> unresolvedDependencies(String id) {
        return require(id).dependencies().stream()
                .filter(dependencyId -> !tasks.get(dependencyId).resolved())
                .toList();
    }

    /** Ids of deleted tasks that have not been created again, in deletion order. */
    public Set<String> removedIds() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(removedIds));
    }

    public Set<String> resolvedIds() {
        Set<String> resolved = new LinkedHashSet<>();
        for (Task task : tasks.values()) {
            if (task.resolved()) {
                resolved.add(task.id());
            }
        }
        return resolved;
    }

    /**
     * Dependencies before dependents. Ties are broken by creation order, so the result is stable.
     */
    public List<Task> topologicalOrder() {
        Map<String, Integer> creationIndex = new HashMap<>();
        Map<String, Integer> pendingDependencies = new HashMap<>();
        Map<String, List<String>> dependentsById = new HashMap<>();
        for (Task task : tasks.values()) {
            creationIndex.put(task.id(), creationIndex.size());
            pendingDependencies.put(task.id(), task.dependencies().size());
            for (String dependencyId : task.dependencies()) {
                dependentsById.computeIfAbsent(dependencyId, key -> new ArrayList<>()).add(task.id());
            }
        }

        PriorityQueue<String> ready = new PriorityQueue<>(Comparator.comparingInt(creationIndex::get));
        pendingDependencies.forEach((id, pending) -> {
            if (pending == 0) {
                ready.add(id);
            }
        });

        List<Task> order = new ArrayList<>(tasks.size());
        while (!ready.isEmpty()) {
            String id = ready.poll();
            order.add(tasks.get(id));
            for (String dependentId : dependentsById.getOrDefault(id, List.of())) {
                if (pendingDependencies.merge(dependentId, -1, Integer::sum) == 0) {
                    ready.add(dependentId);
                }
            }
        }

        if (order.size() != tasks.size()) {
            List<String> inCycle = tasks.keySet().stream()
                    .filter(id -> pendingDependencies.get(id) > 0)
                    .toList();
            throw new TaskGraphException(Reason.CYCLE, inCycle.getFirst(), inCycle);
        }
        return order;
    }

    /**
     * Dependency path from {@code fromId} to {@code toId} (both included), or empty if {@code toId}
     * is not reachable by following dependencies.
     */
    private List<String> pathBetween(String fromId, String toId) {
        Map<String, String> reachedFrom = new HashMap<>();
        Deque<String> queue = new ArrayDeque<>();
        queue.add(fromId);
        reachedFrom.put(fromId, null);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (current.equals(toId)) {
                List<String> path = new ArrayList<>();
                for (String step = current; step != null; step = reachedFrom.get(step)) {
                    path.addFirst(step);
                }
                return path;
            }
            for (String next : tasks.get(current).dependencies()) {
                if (!reachedFrom.containsKey(next)) {
                    reachedFrom.put(next, current);
                    queue.add(next);
                }
            }
        }
        return List.of();
    }

    private Task require(String id) {
        Task task = tasks.get(id);
        if (task == null) {
            throw new TaskGraphException(Reason.NOT_FOUND, id);
        }
        return task;
    }

    private Task replace(Task task) {
        tasks.put(task.id(), task);
        return task;
    }

    private static void validateId(String id) {
        if (id == null || !ID_PATTERN.matcher(id).matches()) {
            throw new TaskGraphException(Reason.INVALID_ID, String.valueOf(id));
        }
    }

    private static void validateTitle(String id, String title) {
        if (title == null || title.isBlank()) {
            throw new TaskGraphException(Reason.INVALID_TITLE, id);
        }
    }
}
