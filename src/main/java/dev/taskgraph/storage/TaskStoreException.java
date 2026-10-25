package dev.taskgraph.storage;

/**
 * The task file could not be read or written. The file on disk is left untouched.
 */
public final class TaskStoreException extends RuntimeException {

    public TaskStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
