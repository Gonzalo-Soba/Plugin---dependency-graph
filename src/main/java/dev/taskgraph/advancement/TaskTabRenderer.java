package dev.taskgraph.advancement;

import com.fren_gor.ultimateAdvancementAPI.AdvancementTab;
import com.fren_gor.ultimateAdvancementAPI.UltimateAdvancementAPI;
import com.fren_gor.ultimateAdvancementAPI.advancement.Advancement;
import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import com.fren_gor.ultimateAdvancementAPI.advancement.RootAdvancement;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplayBuilder;
import com.fren_gor.ultimateAdvancementAPI.util.AdvancementKey;
import dev.taskgraph.graph.GraphLayout;
import dev.taskgraph.graph.NodePosition;
import dev.taskgraph.graph.Task;
import dev.taskgraph.graph.TaskGraph;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Builds the advancement tab from the graph. UAAPI tabs are immutable once registered, so every
 * render unregisters the previous tab and registers a new one under the same namespace.
 */
public final class TaskTabRenderer {

    public static final String NAMESPACE = "taskgraph";

    private static final String ROOT_KEY = "root";
    // Prefix keeps task keys apart from the root key, even for a task with id "root".
    private static final String TASK_KEY_PREFIX = "task/";
    private static final String BACKGROUND_TEXTURE = "textures/block/stone_bricks.png";
    private static final Material ROOT_ICON = Material.WRITABLE_BOOK;
    private static final Material FALLBACK_ICON = Material.PAPER;

    // UAAPI takes bungee-chat components; legacy strings let the rest of the plugin stay on Adventure.
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final UltimateAdvancementAPI api;

    public TaskTabRenderer(UltimateAdvancementAPI api) {
        this.api = api;
    }

    public static AdvancementKey taskKey(String taskId) {
        return new AdvancementKey(NAMESPACE, TASK_KEY_PREFIX + taskId);
    }

    public AdvancementTab render(TaskGraph graph) {
        api.unregisterAdvancementTab(NAMESPACE);
        AdvancementTab tab = api.createAdvancementTab(NAMESPACE, BACKGROUND_TEXTURE);

        Map<String, NodePosition> positions = GraphLayout.compute(graph);
        RootAdvancement root = new RootAdvancement(tab, ROOT_KEY, rootDisplay(graph));
        Map<String, BaseAdvancement> advancements = new HashMap<>();
        // Topological order guarantees the visual parent is built before its children.
        for (Task task : graph.topologicalOrder()) {
            Advancement parent = task.visualParent().<Advancement>map(advancements::get).orElse(root);
            String key = TASK_KEY_PREFIX + task.id();
            advancements.put(task.id(), new BaseAdvancement(parent, key, taskDisplay(graph, task, positions.get(task.id()))));
        }

        tab.registerAdvancements(root, new HashSet<>(advancements.values()));
        tab.automaticallyShowToPlayers();
        tab.automaticallyGrantRootAdvancement();
        return tab;
    }

    private static AdvancementDisplay rootDisplay(TaskGraph graph) {
        Component progress = Component.text(graph.resolvedIds().size() + " de " + graph.size() + " tareas resueltas", NamedTextColor.GRAY);
        return new AdvancementDisplayBuilder(new ItemStack(ROOT_ICON), legacy(Component.text("Tareas del servidor", NamedTextColor.GOLD)))
                .description(legacy(progress))
                .goalFrame()
                .coords(0, 0)
                .build();
    }

    private static AdvancementDisplay taskDisplay(TaskGraph graph, Task task, NodePosition position) {
        List<Component> lines = new ArrayList<>();
        if (!task.description().isEmpty()) {
            lines.add(Component.text(task.description(), NamedTextColor.GRAY));
        }
        task.assigneeOptional().ifPresent(assignee ->
                lines.add(Component.text("Asignada a: " + assignee.name(), NamedTextColor.AQUA)));
        if (!task.extraRequirements().isEmpty()) {
            List<String> titles = task.extraRequirements().stream()
                    .map(id -> graph.get(id).map(Task::title).orElse(id))
                    .toList();
            lines.add(Component.text("También requiere: " + String.join(", ", titles), NamedTextColor.YELLOW));
        }
        lines.add(Component.text("[" + task.id() + "]", NamedTextColor.DARK_GRAY));

        // Column 0 is taken by the root, so tasks start one column to the right.
        return new AdvancementDisplayBuilder(icon(task), legacy(Component.text(task.title(), NamedTextColor.WHITE)))
                .description(lines.stream().map(TaskTabRenderer::legacy).toArray(String[]::new))
                .taskFrame()
                .coords(position.column() + 1, position.row())
                .build();
    }

    private static ItemStack icon(Task task) {
        Material material = Material.matchMaterial(task.icon());
        if (material == null || !material.isItem() || material.isAir()) {
            material = FALLBACK_ICON;
        }
        return new ItemStack(material);
    }

    private static String legacy(Component component) {
        return LEGACY.serialize(component);
    }
}
