package dev.taskgraph.graph;

import java.util.Collection;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Changes needed so a player's advancements match the stored graph, which is the source of truth.
 * Granted ids that are no longer resolved, or that belong to deleted tasks, are revoked.
 */
public record SyncPlan(SortedSet<String> toGrant, SortedSet<String> toRevoke) {

    public SyncPlan {
        toGrant = Collections.unmodifiableSortedSet(new TreeSet<>(toGrant));
        toRevoke = Collections.unmodifiableSortedSet(new TreeSet<>(toRevoke));
    }

    /**
     * @param resolvedIds ids resolved in the graph
     * @param grantedIds  ids the player currently has granted, possibly including deleted tasks
     */
    public static SyncPlan between(Collection<String> resolvedIds, Collection<String> grantedIds) {
        TreeSet<String> toGrant = new TreeSet<>(resolvedIds);
        toGrant.removeAll(grantedIds);
        TreeSet<String> toRevoke = new TreeSet<>(grantedIds);
        toRevoke.removeAll(resolvedIds);
        return new SyncPlan(toGrant, toRevoke);
    }

    public boolean isEmpty() {
        return toGrant.isEmpty() && toRevoke.isEmpty();
    }
}
