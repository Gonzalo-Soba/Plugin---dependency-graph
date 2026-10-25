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
import dev.taskgraph.graph.Titles;
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
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@code /tarea} and its subcommands. Players refer to tasks by title; internal ids never show up here.
 * The last argument of every subcommand is greedy, so it needs no quotes. Earlier arguments use
 * Brigadier quoted strings, which suggestions quote automatically when the title has spaces or accents.
 * Every change goes through {@link TaskService}, which saves the graph and rebuilds the advancement tab.
 */
public final class TaskCommand {

    public static final String NAME = "tarea";
    public static final String DESCRIPTION = "Tareas comunales del servidor";

    private static final String TASK = "tarea";
    private static final String DEPENDENCY = "dependencia";
    private static final String TITLE = "titulo";
    private static final String NEW_TITLE = "nuevoTitulo";
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
                        .then(Commands.argument(TITLE, StringArgumentType.greedyString())
                                .executes(this::create)))
                .then(Commands.literal("renombrar")
                        .then(quotedTask(TASK)
                                .then(Commands.argument(NEW_TITLE, StringArgumentType.greedyString())
                                        .executes(this::rename))))
                .then(Commands.literal("desc")
                        .then(quotedTask(TASK)
                                .then(Commands.argument(TEXT, StringArgumentType.greedyString())
                                        .executes(this::describe))))
                .then(Commands.literal("depende")
                        .then(quotedTask(TASK)
                                .then(lastTask(DEPENDENCY).executes(this::addDependency))))
                .then(Commands.literal("asignar")
                        .then(lastTask(TASK).executes(this::assign)))
                .then(Commands.literal("resolver")
                        .then(lastTask(TASK).executes(this::resolve)))
                .then(Commands.literal("info")
                        .then(lastTask(TASK).executes(this::info)))
                .then(Commands.literal("borrar")
                        .then(lastTask(TASK).executes(this::remove)))
                .build();
    }

    /** Task title that is followed by more arguments, so titles with spaces or accents need quotes. */
    private RequiredArgumentBuilder<CommandSourceStack, String> quotedTask(String name) {
        return Commands.argument(name, StringArgumentType.string()).suggests(taskTitles(StringArgumentType::escapeIfRequired));
    }

    /** Task title as the last argument: takes the rest of the line, no quotes needed. */
    private RequiredArgumentBuilder<CommandSourceStack, String> lastTask(String name) {
        return Commands.argument(name, StringArgumentType.greedyString()).suggests(taskTitles(Function.identity()));
    }

    private SuggestionProvider<CommandSourceStack> taskTitles(Function<String, String> format) {
        return (context, builder) -> {
            String typed = Titles.key(unquote(builder.getRemaining()));
            for (Task task : service.tasks()) {
                if (Titles.key(task.title()).startsWith(typed)) {
                    builder.suggest(format.apply(task.title()));
                }
            }
            return builder.buildFuture();
        };
    }

    private int help(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        sender.sendMessage(Component.text("Comandos de tareas:", NamedTextColor.GOLD));
        for (String usage : List.of(
                "crear <título> — el ícono es el ítem en tu mano",
                "renombrar \"<título>\" <nuevo título>",
                "desc \"<título>\" <texto>",
                "depende \"<título>\" <dependencia>",
                "asignar <título>",
                "resolver <título>",
                "info <título>",
                "borrar <título>")) {
            sender.sendMessage(Component.text("/" + NAME + " " + usage, NamedTextColor.GRAY));
        }
        sender.sendMessage(Component.text("Usá comillas cuando el título tenga espacios o tildes y no sea lo último del comando. Tab las pone por vos.",
                NamedTextColor.DARK_GRAY));
        return Command.SINGLE_SUCCESS;
    }

    private int create(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        if (!(sender instanceof Player player)) {
            return fail(sender, "Solo un jugador puede crear tareas: el ícono es el ítem que tiene en la mano.");
        }
        String title = unquote(StringArgumentType.getString(context, TITLE));
        ItemStack hand = player.getInventory().getItemInMainHand();
        String icon = hand.isEmpty() ? FALLBACK_ICON : hand.getType().getKey().asString();
        return run(sender, () -> {
            Task task = service.create(title, icon);
            succeed(sender, "Tarea " + quoted(task.title()) + " creada.");
            if (hand.isEmpty()) {
                sender.sendMessage(Component.text("No tenías nada en la mano, así que el ícono es papel.", NamedTextColor.GRAY));
            }
        });
    }

    private int rename(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        String newTitle = unquote(StringArgumentType.getString(context, NEW_TITLE));
        return withTask(context, TASK, task -> {
            Task renamed = service.rename(task.id(), newTitle);
            succeed(sender, quoted(task.title()) + " ahora se llama " + quoted(renamed.title()) + ".");
        });
    }

    private int describe(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        String text = StringArgumentType.getString(context, TEXT);
        return withTask(context, TASK, task -> {
            service.describe(task.id(), text);
            succeed(sender, "Descripción de " + quoted(task.title()) + " actualizada.");
        });
    }

    private int addDependency(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        return withTask(context, TASK, task -> withTaskOrFail(context, DEPENDENCY, dependency -> {
            Task updated = service.addDependency(task.id(), dependency.id());
            boolean visualParent = updated.dependencies().size() == 1;
            succeed(sender, quoted(task.title()) + " ahora depende de " + quoted(dependency.title())
                    + (visualParent ? "." : " (se muestra en \"También requiere\")."));
        }));
    }

    private int assign(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        if (!(sender instanceof Player player)) {
            return fail(sender, "Solo un jugador puede asignarse una tarea.");
        }
        return withTask(context, TASK, task -> {
            service.assign(task.id(), new Assignee(player.getUniqueId(), player.getName()));
            succeed(sender, "Te asignaste la tarea " + quoted(task.title()) + ".");
        });
    }

    private int resolve(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        return withTask(context, TASK, task -> {
            service.resolve(task.id());
            Bukkit.getServer().broadcast(Component.text(
                    "✔ " + sender.getName() + " resolvió la tarea " + quoted(task.title()), NamedTextColor.GREEN));
        });
    }

    private int info(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        TaskGraph graph = service.snapshot();
        return withTask(context, TASK, task -> {
            sender.sendMessage(Component.text(task.title(), NamedTextColor.GOLD));
            if (!task.description().isEmpty()) {
                sender.sendMessage(Component.text(task.description(), NamedTextColor.GRAY));
            }
            sender.sendMessage(Component.text("Estado: " + (task.resolved() ? "resuelta" : "pendiente"),
                    task.resolved() ? NamedTextColor.GREEN : NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("Asignada a: "
                    + task.assigneeOptional().map(Assignee::name).orElse("nadie"), NamedTextColor.AQUA));
            sender.sendMessage(Component.text("Depende de: " + describeTasks(graph, task.dependencies()), NamedTextColor.GRAY));
            sender.sendMessage(Component.text("La necesitan: " + describeTasks(graph, graph.dependents(task.id())), NamedTextColor.GRAY));
            sender.sendMessage(Component.text("Ícono: " + task.icon(), NamedTextColor.DARK_GRAY));
        });
    }

    private int remove(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        return withTask(context, TASK, task -> {
            service.remove(task.id());
            succeed(sender, "Tarea " + quoted(task.title()) + " borrada.");
        });
    }

    /** Looks up the task named by the argument and runs the action, reporting unknown titles and rejections. */
    private int withTask(CommandContext<CommandSourceStack> context, String argument, Consumer<Task> action) {
        CommandSender sender = context.getSource().getSender();
        return run(sender, () -> withTaskOrFail(context, argument, action));
    }

    private void withTaskOrFail(CommandContext<CommandSourceStack> context, String argument, Consumer<Task> action) {
        String title = unquote(StringArgumentType.getString(context, argument));
        Optional<Task> task = service.findByTitle(title);
        if (task.isEmpty()) {
            throw new UnknownTitleException(title);
        }
        action.accept(task.get());
    }

    private static String describeTasks(TaskGraph graph, List<String> ids) {
        if (ids.isEmpty()) {
            return "nada";
        }
        return String.join(", ", ids.stream()
                .map(id -> graph.get(id)
                        .map(task -> (task.resolved() ? "✔ " : "✘ ") + task.title())
                        .orElse(id))
                .toList());
    }

    private int run(CommandSender sender, Runnable action) {
        try {
            action.run();
            return Command.SINGLE_SUCCESS;
        } catch (UnknownTitleException exception) {
            return fail(sender, "No existe la tarea " + quoted(exception.title) + ".");
        } catch (TaskGraphException exception) {
            return fail(sender, RejectionMessages.of(exception, this::titleOf));
        } catch (TaskStoreException exception) {
            logger.log(Level.SEVERE, "Could not save tasks", exception);
            return fail(sender, "No se pudo guardar el cambio. Revisá el log del servidor.");
        }
    }

    private String titleOf(String id) {
        return service.task(id).map(Task::title).orElse(id);
    }

    /** Greedy arguments keep quotes literally; drop them when the player typed a quoted title anyway. */
    private static String unquote(String value) {
        String stripped = value.strip();
        if (stripped.length() >= 2 && stripped.startsWith("\"") && stripped.endsWith("\"")) {
            return stripped.substring(1, stripped.length() - 1);
        }
        return stripped.startsWith("\"") ? stripped.substring(1) : stripped;
    }

    private static String quoted(String title) {
        return "\"" + title + "\"";
    }

    private static void succeed(CommandSender sender, String message) {
        sender.sendMessage(Component.text(message, NamedTextColor.GREEN));
    }

    private static int fail(CommandSender sender, String message) {
        sender.sendMessage(Component.text(message, NamedTextColor.RED));
        return 0;
    }

    /** A typed title that matches no task. Kept apart from graph rejections, which carry internal ids. */
    private static final class UnknownTitleException extends RuntimeException {

        private final String title;

        private UnknownTitleException(String title) {
            super(null, null, false, false);
            this.title = title;
        }
    }
}
