package dev.taskgraph.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.taskgraph.graph.Assignee;
import dev.taskgraph.graph.Task;
import dev.taskgraph.graph.TaskGraph;
import dev.taskgraph.graph.TaskGraphException;
import dev.taskgraph.service.TaskService;
import dev.taskgraph.storage.TaskStoreException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@code /tarea} and its subcommands. Every change goes through {@link TaskService}, which saves the
 * graph and rebuilds the advancement tab.
 */
public final class TaskCommand {

    public static final String NAME = "tarea";
    public static final String DESCRIPTION = "Tareas comunales del servidor";

    private static final String ID = "id";
    private static final String DEPENDENCY_ID = "idDependencia";
    private static final String TITLE = "titulo";
    private static final String TEXT = "texto";
    private static final String FALLBACK_ICON = "minecraft:paper";

    private final TaskService service;
    private final Logger logger;

    public TaskCommand(TaskService service, Logger logger) {
        this.service = service;
        this.logger = logger;
    }

    public LiteralCommandNode<CommandSourceStack> build() {
        return Commands.literal(NAME)
                .executes(this::help)
                .then(Commands.literal("crear")
                        .then(Commands.argument(ID, StringArgumentType.word())
                                .then(Commands.argument(TITLE, StringArgumentType.greedyString())
                                        .executes(this::create))))
                .then(Commands.literal("desc")
                        .then(existingTask(ID)
                                .then(Commands.argument(TEXT, StringArgumentType.greedyString())
                                        .executes(this::describe))))
                .then(Commands.literal("depende")
                        .then(existingTask(ID)
                                .then(existingTask(DEPENDENCY_ID)
                                        .executes(this::addDependency))))
                .then(Commands.literal("asignar")
                        .then(existingTask(ID).executes(this::assign)))
                .then(Commands.literal("resolver")
                        .then(existingTask(ID).executes(this::resolve)))
                .then(Commands.literal("info")
                        .then(existingTask(ID).executes(this::info)))
                .then(Commands.literal("borrar")
                        .then(existingTask(ID).executes(this::remove)))
                .build();
    }

    private RequiredArgumentBuilder<CommandSourceStack, String> existingTask(String name) {
        return Commands.argument(name, StringArgumentType.word()).suggests(taskIds());
    }

    private SuggestionProvider<CommandSourceStack> taskIds() {
        return (context, builder) -> {
            String typed = builder.getRemainingLowerCase();
            for (Task task : service.tasks()) {
                if (task.id().startsWith(typed)) {
                    builder.suggest(task.id());
                }
            }
            return builder.buildFuture();
        };
    }

    private int help(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        sender.sendMessage(Component.text("Comandos de tareas:", NamedTextColor.GOLD));
        for (String usage : List.of(
                "crear <id> <título> — el ícono es el ítem en tu mano",
                "desc <id> <texto>",
                "depende <id> <idDependencia>",
                "asignar <id>",
                "resolver <id>",
                "info <id>",
                "borrar <id>")) {
            sender.sendMessage(Component.text("/" + NAME + " " + usage, NamedTextColor.GRAY));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int create(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        if (!(sender instanceof Player player)) {
            return fail(sender, "Solo un jugador puede crear tareas: el ícono es el ítem que tiene en la mano.");
        }
        String id = StringArgumentType.getString(context, ID);
        String title = StringArgumentType.getString(context, TITLE);
        ItemStack hand = player.getInventory().getItemInMainHand();
        String icon = hand.isEmpty() ? FALLBACK_ICON : hand.getType().getKey().asString();
        return run(sender, () -> {
            Task task = service.create(id, title, icon);
            succeed(sender, "Tarea '" + task.id() + "' creada: " + task.title() + ".");
            if (hand.isEmpty()) {
                sender.sendMessage(Component.text("No tenías nada en la mano, así que el ícono es papel.", NamedTextColor.GRAY));
            }
        });
    }

    private int describe(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        String id = StringArgumentType.getString(context, ID);
        String text = StringArgumentType.getString(context, TEXT);
        return run(sender, () -> {
            service.describe(id, text);
            succeed(sender, "Descripción de '" + id + "' actualizada.");
        });
    }

    private int addDependency(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        String id = StringArgumentType.getString(context, ID);
        String dependencyId = StringArgumentType.getString(context, DEPENDENCY_ID);
        return run(sender, () -> {
            Task task = service.addDependency(id, dependencyId);
            boolean visualParent = task.dependencies().size() == 1;
            succeed(sender, "'" + id + "' ahora depende de '" + dependencyId + "'"
                    + (visualParent ? "." : " (se muestra en \"También requiere\")."));
        });
    }

    private int assign(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        if (!(sender instanceof Player player)) {
            return fail(sender, "Solo un jugador puede asignarse una tarea.");
        }
        String id = StringArgumentType.getString(context, ID);
        return run(sender, () -> {
            service.assign(id, new Assignee(player.getUniqueId(), player.getName()));
            succeed(sender, "Te asignaste la tarea '" + id + "'.");
        });
    }

    private int resolve(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        String id = StringArgumentType.getString(context, ID);
        return run(sender, () -> {
            Task task = service.resolve(id);
            Bukkit.getServer().broadcast(Component.text(
                    "✔ " + sender.getName() + " resolvió la tarea \"" + task.title() + "\"", NamedTextColor.GREEN));
        });
    }

    private int info(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        String id = StringArgumentType.getString(context, ID);
        TaskGraph graph = service.snapshot();
        return run(sender, () -> {
            Task task = graph.get(id).orElseThrow(() -> new TaskGraphException(TaskGraphException.Reason.NOT_FOUND, id));
            sender.sendMessage(Component.text(task.title() + " [" + task.id() + "]", NamedTextColor.GOLD));
            if (!task.description().isEmpty()) {
                sender.sendMessage(Component.text(task.description(), NamedTextColor.GRAY));
            }
            sender.sendMessage(Component.text("Estado: " + (task.resolved() ? "resuelta" : "pendiente"),
                    task.resolved() ? NamedTextColor.GREEN : NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("Asignada a: "
                    + task.assigneeOptional().map(Assignee::name).orElse("nadie"), NamedTextColor.AQUA));
            sender.sendMessage(Component.text("Depende de: " + describeIds(graph, task.dependencies()), NamedTextColor.GRAY));
            sender.sendMessage(Component.text("La necesitan: " + describeIds(graph, graph.dependents(id)), NamedTextColor.GRAY));
            sender.sendMessage(Component.text("Ícono: " + task.icon(), NamedTextColor.DARK_GRAY));
        });
    }

    private int remove(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        String id = StringArgumentType.getString(context, ID);
        return run(sender, () -> {
            Task task = service.remove(id);
            succeed(sender, "Tarea '" + task.id() + "' borrada.");
        });
    }

    private static String describeIds(TaskGraph graph, List<String> ids) {
        if (ids.isEmpty()) {
            return "nada";
        }
        return String.join(", ", ids.stream()
                .map(id -> graph.get(id)
                        .map(task -> (task.resolved() ? "✔ " : "✘ ") + task.title() + " [" + id + "]")
                        .orElse(id))
                .toList());
    }

    private int run(CommandSender sender, Runnable action) {
        try {
            action.run();
            return Command.SINGLE_SUCCESS;
        } catch (TaskGraphException exception) {
            return fail(sender, RejectionMessages.of(exception));
        } catch (TaskStoreException exception) {
            logger.log(Level.SEVERE, "Could not save tasks", exception);
            return fail(sender, "No se pudo guardar el cambio. Revisá el log del servidor.");
        }
    }

    private static void succeed(CommandSender sender, String message) {
        sender.sendMessage(Component.text(message, NamedTextColor.GREEN));
    }

    private static int fail(CommandSender sender, String message) {
        sender.sendMessage(Component.text(message, NamedTextColor.RED));
        return 0;
    }
}
