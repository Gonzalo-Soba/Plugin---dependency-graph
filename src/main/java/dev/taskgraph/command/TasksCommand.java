package dev.taskgraph.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.taskgraph.dialog.TaskDialogs;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

/**
 * {@code /tareas}: opens the task list dialog. {@code /tarea} stays as the text alternative.
 */
public final class TasksCommand {

    public static final String NAME = "tareas";
    public static final String DESCRIPTION = "Abre la lista de tareas del servidor";

    private final TaskDialogs dialogs;

    public TasksCommand(TaskDialogs dialogs) {
        this.dialogs = dialogs;
    }

    public LiteralCommandNode<CommandSourceStack> build() {
        return Commands.literal(NAME)
                .executes(context -> {
                    if (!(context.getSource().getSender() instanceof Player player)) {
                        context.getSource().getSender().sendMessage(Component.text(
                                "La lista de tareas es una ventana del juego; desde la consola usá /tarea.", NamedTextColor.RED));
                        return 0;
                    }
                    dialogs.openList(player);
                    return Command.SINGLE_SUCCESS;
                })
                .build();
    }
}
