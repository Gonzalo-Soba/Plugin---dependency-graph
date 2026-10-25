package dev.taskgraph.command;

import dev.taskgraph.graph.TaskGraphException;

import java.util.List;
import java.util.function.Function;

/**
 * Player-facing text for rejected graph operations. Exceptions carry internal ids, which players never
 * see, so every id is shown through {@code titleOf}. Kept free of Paper types so it can be unit tested.
 */
public final class RejectionMessages {

    private RejectionMessages() {
    }

    public static String of(TaskGraphException exception, Function<String, String> titleOf) {
        String task = quoted(titleOf.apply(exception.taskId()));
        List<String> related = exception.relatedIds().stream().map(titleOf).toList();
        return switch (exception.reason()) {
            case INVALID_ID -> "Id interno inválido: " + task + ".";
            case INVALID_TITLE -> "El título no puede estar vacío.";
            case DUPLICATE_ID -> "Id interno repetido: " + task + ".";
            case DUPLICATE_TITLE -> "Ya existe una tarea llamada " + task + ".";
            case NOT_FOUND -> "No existe la tarea " + task + ".";
            case SELF_DEPENDENCY -> "Una tarea no puede depender de sí misma.";
            case DUPLICATE_DEPENDENCY -> task + " ya depende de " + list(related) + ".";
            case CYCLE -> "Esa dependencia crea un ciclo: "
                    + String.join(" → ", related.stream().map(RejectionMessages::quoted).toList()) + ".";
            case RESOLVED_TASK_DEPENDENCY -> task + " ya está resuelta y no puede depender de una tarea pendiente ("
                    + list(related) + ").";
            case ALREADY_RESOLVED -> task + " ya está resuelta.";
            case UNRESOLVED_DEPENDENCIES -> "No se puede resolver " + task + ": faltan " + list(related) + ".";
            case HAS_DEPENDENTS -> "No se puede borrar " + task + ": dependen de ella " + list(related) + ".";
        };
    }

    private static String list(List<String> titles) {
        return String.join(", ", titles.stream().map(RejectionMessages::quoted).toList());
    }

    private static String quoted(String title) {
        return "\"" + title + "\"";
    }
}
