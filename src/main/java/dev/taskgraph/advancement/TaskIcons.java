package dev.taskgraph.advancement;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Item shown for a task, both in the advancement tab and in dialogs.
 */
public final class TaskIcons {

    private static final Material FALLBACK = Material.PAPER;

    private TaskIcons() {
    }

    /** The stored material key, or paper when it is not a valid item (e.g. removed in a game update). */
    public static ItemStack of(String materialKey) {
        Material material = Material.matchMaterial(materialKey);
        if (material == null || !material.isItem() || material.isAir()) {
            material = FALLBACK;
        }
        return new ItemStack(material);
    }
}
