package dev.taskgraph.dialog;

import net.kyori.adventure.key.Key;

/**
 * Keys shared by the bootstrap, which registers the quick access dialog, and the listener that reacts to its
 * button. The registered dialog is static (it is built before any task is loaded), so its only job is to send
 * {@link #OPEN_LIST} back to the server, which then opens the live task list.
 */
public final class QuickAccess {

    /** Registry key of the static dialog shown by the quick actions key and the pause menu. */
    public static final Key DIALOG = Key.key("taskgraph", "quick_access");
    /** Custom click sent by the dialog's button. */
    public static final Key OPEN_LIST = Key.key("taskgraph", "open_list");

    private QuickAccess() {
    }
}
