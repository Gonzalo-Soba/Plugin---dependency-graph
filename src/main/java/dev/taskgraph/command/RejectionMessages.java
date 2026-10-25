package dev.taskgraph.command;

import dev.taskgraph.graph.TaskGraphException;

import java.util.List;

/**
 * Player-facing text for rejected graph operations. Kept free of Paper types so it can be unit tested.
 */
public final class RejectionMessages {

    private RejectionMessages() {
    }

    public static String of(TaskGraphException exception) {
        String id = exception.taskId();
        List<String> related = exception.relatedIds();
        return switch (exception.reason()) {
            case INVALID_ID -> "Id inválido '" + id + "': usá de 1 a 32 caracteres entre a-z, 0-9 y _.";
            case INVALID_TITLE -> "El título no puede estar vacío.";
            case DUPLICATE_ID -> "Ya existe una tarea con id '" + id + "'.";
            case NOT_FOUND -> "No existe la tarea '" + id + "'.";
            case SELF_DEPENDENCY -> "Una tarea no puede depender de sí misma.";
            case DUPLICATE_DEPENDENCY -> "'" + id + "' ya depende de " + list(related) + ".";
            case CYCLE -> "Esa dependencia crea un ciclo: " + String.join(" → ", related) + ".";
            case RESOLVED_TASK_DEPENDENCY -> "'" + id + "' ya está resuelta y no puede depender de una tarea pendiente ("
                    + list(related) + ").";
            case ALREADY_RESOLVED -> "'" + id + "' ya está resuelta.";
            case UNRESOLVED_DEPENDENCIES -> "No se puede resolver '" + id + "': faltan " + list(related) + ".";
            case HAS_DEPENDENTS -> "No se puede borrar '" + id + "': dependen de ella " + list(related) + ".";
        };
    }

    private static String list(List<String> ids) {
        return String.join(", ", ids.stream().map(relatedId -> "'" + relatedId + "'").toList());
    }
}
