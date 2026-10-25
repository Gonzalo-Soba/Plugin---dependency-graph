package dev.taskgraph.command;

import dev.taskgraph.graph.TaskPermissions.Actor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Turns Paper senders into the plain values {@link dev.taskgraph.action.TaskActions} works with.
 * Shared by commands and dialogs so both build actors and icons the same way.
 */
public final class Senders {

    /** Declared in paper-plugin.yml; granted to operators by default. The console always has it. */
    public static final String ADMIN_PERMISSION = "taskgraph.admin";
    public static final String FALLBACK_ICON = "minecraft:paper";

    private Senders() {
    }

    public static Actor actor(CommandSender sender) {
        boolean admin = sender.hasPermission(ADMIN_PERMISSION);
        return sender instanceof Player player
                ? Actor.player(player.getUniqueId(), player.getName(), admin)
                : new Actor(null, sender.getName(), admin);
    }

    /** Key of the item in the main hand, or {@link #FALLBACK_ICON} when the hand is empty. */
    public static String iconInHand(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        return hand.isEmpty() ? FALLBACK_ICON : hand.getType().getKey().asString();
    }

    public static boolean handIsEmpty(Player player) {
        return player.getInventory().getItemInMainHand().isEmpty();
    }
}
