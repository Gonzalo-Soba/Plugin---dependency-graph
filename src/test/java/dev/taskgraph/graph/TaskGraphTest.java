package dev.taskgraph.graph;

import dev.taskgraph.graph.TaskGraphException.Reason;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskGraphTest {

    private static final String ICON = "minecraft:stone";

    private TaskGraph graph;

    @BeforeEach
    void setUp() {
        graph = new TaskGraph();
    }

    private void createAll(String... ids) {
        for (String id : ids) {
            graph.create(id, "Title " + id, ICON);
        }
    }

    private static TaskGraphException assertRejected(Reason reason, Executable action) {
        TaskGraphException exception = assertThrows(TaskGraphException.class, action);
        assertEquals(reason, exception.reason());
        return exception;
    }

    @Nested
    class Create {

        @Test
        void storesTaskWithDefaults() {
            Task task = graph.create("farm", "  Build a farm ", "minecraft:wheat");

            assertEquals("Build a farm", task.title());
            assertEquals("", task.description());
            assertEquals("minecraft:wheat", task.icon());
            assertTrue(task.dependencies().isEmpty());
            assertTrue(task.assigneeOptional().isEmpty());
            assertFalse(task.resolved());
            assertEquals(task, graph.get("farm").orElseThrow());
        }

        @Test
        void rejectsDuplicateId() {
            createAll("farm");

            assertRejected(Reason.DUPLICATE_ID, () -> graph.create("farm", "Other", ICON));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "Farm", "my-farm", "a.b", "with space", "abcdefghijklmnopqrstuvwxyz0123456"})
        void rejectsInvalidId(String id) {
            assertRejected(Reason.INVALID_ID, () -> graph.create(id, "Title", ICON));
            assertTrue(graph.isEmpty());
        }

        @Test
        void rejectsBlankTitle() {
            assertRejected(Reason.INVALID_TITLE, () -> graph.create("farm", "   ", ICON));
        }
    }

    @Nested
    class Dependencies {

        @Test
        void firstDependencyIsVisualParent() {
            createAll("a", "b", "c");
            graph.addDependency("c", "b");
            Task task = graph.addDependency("c", "a");

            assertEquals(List.of("b", "a"), task.dependencies());
            assertEquals("b", task.visualParent().orElseThrow());
            assertEquals(List.of("a"), task.extraRequirements());
        }

        @Test
        void taskWithoutDependenciesHasNoVisualParent() {
            createAll("a");

            Task task = graph.get("a").orElseThrow();
            assertTrue(task.visualParent().isEmpty());
            assertTrue(task.extraRequirements().isEmpty());
        }

        @Test
        void rejectsSelfDependency() {
            createAll("a");

            assertRejected(Reason.SELF_DEPENDENCY, () -> graph.addDependency("a", "a"));
        }

        @Test
        void rejectsTwoNodeCycle() {
            createAll("a", "b");
            graph.addDependency("a", "b");

            TaskGraphException exception = assertRejected(Reason.CYCLE, () -> graph.addDependency("b", "a"));
            assertEquals(List.of("b", "a", "b"), exception.relatedIds());
            assertTrue(graph.get("b").orElseThrow().dependencies().isEmpty());
        }

        @Test
        void rejectsLongCycle() {
            createAll("a", "b", "c", "d");
            graph.addDependency("a", "b");
            graph.addDependency("b", "c");
            graph.addDependency("c", "d");

            TaskGraphException exception = assertRejected(Reason.CYCLE, () -> graph.addDependency("d", "a"));
            assertEquals(List.of("d", "a", "b", "c", "d"), exception.relatedIds());
            assertTrue(graph.get("d").orElseThrow().dependencies().isEmpty());
        }

        @Test
        void allowsDiamondShape() {
            createAll("base", "left", "right", "top");
            graph.addDependency("left", "base");
            graph.addDependency("right", "base");
            graph.addDependency("top", "left");

            Task top = graph.addDependency("top", "right");
            assertEquals(List.of("left", "right"), top.dependencies());
        }

        @Test
        void rejectsDuplicateDependency() {
            createAll("a", "b");
            graph.addDependency("a", "b");

            assertRejected(Reason.DUPLICATE_DEPENDENCY, () -> graph.addDependency("a", "b"));
        }

        @Test
        void rejectsMissingTasks() {
            createAll("a");

            assertRejected(Reason.NOT_FOUND, () -> graph.addDependency("a", "missing"));
            assertRejected(Reason.NOT_FOUND, () -> graph.addDependency("missing", "a"));
        }

        @Test
        void rejectsUnresolvedDependencyOnResolvedTask() {
            createAll("a", "b");
            graph.resolve("a");

            assertRejected(Reason.RESOLVED_TASK_DEPENDENCY, () -> graph.addDependency("a", "b"));
            assertTrue(graph.get("a").orElseThrow().dependencies().isEmpty());
        }

        @Test
        void allowsResolvedDependencyOnResolvedTask() {
            createAll("a", "b");
            graph.resolve("a");
            graph.resolve("b");

            assertEquals(List.of("b"), graph.addDependency("a", "b").dependencies());
        }
    }

    @Nested
    class Resolve {

        @Test
        void rejectsWhenDependenciesAreUnresolved() {
            createAll("a", "b", "c");
            graph.addDependency("c", "a");
            graph.addDependency("c", "b");
            graph.resolve("a");

            TaskGraphException exception = assertRejected(Reason.UNRESOLVED_DEPENDENCIES, () -> graph.resolve("c"));
            assertEquals(List.of("b"), exception.relatedIds());
            assertFalse(graph.get("c").orElseThrow().resolved());
        }

        @Test
        void succeedsWhenAllDependenciesAreResolved() {
            createAll("a", "b");
            graph.addDependency("b", "a");
            graph.resolve("a");

            assertTrue(graph.resolve("b").resolved());
            assertEquals(Set.of("a", "b"), graph.resolvedIds());
        }

        @Test
        void rejectsAlreadyResolved() {
            createAll("a");
            graph.resolve("a");

            assertRejected(Reason.ALREADY_RESOLVED, () -> graph.resolve("a"));
        }
    }

    @Nested
    class Remove {

        @Test
        void rejectsWhenOtherTasksDependOnIt() {
            createAll("a", "b", "c");
            graph.addDependency("b", "a");
            graph.addDependency("c", "a");

            TaskGraphException exception = assertRejected(Reason.HAS_DEPENDENTS, () -> graph.remove("a"));
            assertEquals(List.of("b", "c"), exception.relatedIds());
            assertTrue(graph.contains("a"));
        }

        @Test
        void removesLeafTask() {
            createAll("a", "b");
            graph.addDependency("b", "a");

            graph.remove("b");

            assertFalse(graph.contains("b"));
            assertTrue(graph.dependents("a").isEmpty());
        }

        @Test
        void rejectsMissingTask() {
            assertRejected(Reason.NOT_FOUND, () -> graph.remove("missing"));
        }

        @Test
        void remembersRemovedIdsUntilCreatedAgain() {
            createAll("a", "b");
            graph.remove("b");
            graph.remove("a");

            assertEquals(List.of("b", "a"), List.copyOf(graph.removedIds()));

            graph.create("a", "Again", ICON);
            assertEquals(Set.of("b"), graph.removedIds());
        }

        @Test
        void rejectedRemovalDoesNotRecordId() {
            createAll("a", "b");
            graph.addDependency("b", "a");

            assertThrows(TaskGraphException.class, () -> graph.remove("a"));
            assertTrue(graph.removedIds().isEmpty());
        }
    }

    @Nested
    class Copy {

        @Test
        void changesToCopyDoNotAffectOriginal() {
            createAll("a", "b");
            graph.remove("b");
            TaskGraph copy = graph.copy();

            copy.create("c", "C", ICON);
            copy.resolve("a");
            copy.remove("c");

            assertEquals(List.of("a"), graph.tasks().stream().map(Task::id).toList());
            assertFalse(graph.get("a").orElseThrow().resolved());
            assertEquals(Set.of("b"), graph.removedIds());
            assertEquals(Set.of("b", "c"), copy.removedIds());
        }
    }

    @Nested
    class Edit {

        @Test
        void setsDescription() {
            createAll("a");

            assertEquals("Needs lots of wood", graph.setDescription("a", " Needs lots of wood ").description());
        }

        @Test
        void assignReplacesPreviousAssignee() {
            createAll("a");
            Assignee first = new Assignee(UUID.randomUUID(), "Alex");
            Assignee second = new Assignee(UUID.randomUUID(), "Steve");

            graph.assign("a", first);
            Task task = graph.assign("a", second);

            assertEquals(second, task.assigneeOptional().orElseThrow());
        }
    }

    @Nested
    class TopologicalOrder {

        @Test
        void placesDependenciesFirstAndKeepsCreationOrderOnTies() {
            createAll("top", "left", "right", "base");
            graph.addDependency("top", "left");
            graph.addDependency("top", "right");
            graph.addDependency("left", "base");
            graph.addDependency("right", "base");

            List<String> order = graph.topologicalOrder().stream().map(Task::id).toList();

            assertEquals(List.of("base", "left", "right", "top"), order);
        }

        @Test
        void isStableAcrossCalls() {
            createAll("c", "b", "a");
            graph.addDependency("a", "c");

            assertEquals(graph.topologicalOrder(), graph.topologicalOrder());
            assertEquals(List.of("c", "b", "a"), graph.topologicalOrder().stream().map(Task::id).toList());
        }
    }

    @Nested
    class Load {

        private static Task task(String id, boolean resolved, String... dependencies) {
            return new Task(id, "Title " + id, "", ICON, List.of(dependencies), null, resolved);
        }

        @Test
        void rebuildsValidGraph() {
            TaskGraph loaded = TaskGraph.of(List.of(task("a", true), task("b", false, "a")));

            assertEquals(List.of("a", "b"), loaded.tasks().stream().map(Task::id).toList());
            assertEquals(List.of("b"), loaded.dependents("a"));
        }

        @Test
        void keepsRemovedIdsThatAreNotExistingTasks() {
            TaskGraph loaded = TaskGraph.of(List.of(task("a", false)), List.of("old", "a"));

            assertEquals(Set.of("old"), loaded.removedIds());
        }

        @Test
        void rejectsCycle() {
            assertRejected(Reason.CYCLE, () -> TaskGraph.of(List.of(task("a", false, "b"), task("b", false, "a"))));
        }

        @Test
        void rejectsUnknownDependency() {
            assertRejected(Reason.NOT_FOUND, () -> TaskGraph.of(List.of(task("a", false, "ghost"))));
        }

        @Test
        void rejectsDuplicateIds() {
            assertRejected(Reason.DUPLICATE_ID, () -> TaskGraph.of(List.of(task("a", false), task("a", false))));
        }

        @Test
        void rejectsResolvedTaskWithUnresolvedDependency() {
            assertRejected(Reason.RESOLVED_TASK_DEPENDENCY,
                    () -> TaskGraph.of(List.of(task("a", false), task("b", true, "a"))));
        }
    }
}
