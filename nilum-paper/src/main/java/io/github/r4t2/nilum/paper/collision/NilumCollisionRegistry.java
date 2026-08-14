package io.github.r4t2.nilum.paper.collision;

import io.github.r4t2.nilum.common.model.CollisionTier;
import org.bukkit.util.BoundingBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Manual, world-space collision boxes for placed models and PARTIAL-collision blocks, keyed by owner id. */
public final class NilumCollisionRegistry {

    public record Entry(String world, BoundingBox box, CollisionTier tier) {
    }

    private final Map<String, List<Entry>> entriesByOwner = new ConcurrentHashMap<>();

    public void set(String ownerId, List<Entry> entries) {
        if (entries.isEmpty()) {
            entriesByOwner.remove(ownerId);
        } else {
            entriesByOwner.put(ownerId, entries);
        }
    }

    public void remove(String ownerId) {
        entriesByOwner.remove(ownerId);
    }

    /** Every registered entry in the given world; the listener filters/tests these itself per move. */
    public List<Entry> entriesInWorld(String world) {
        List<Entry> result = new ArrayList<>();
        for (List<Entry> owned : entriesByOwner.values()) {
            for (Entry entry : owned) {
                if (entry.world().equals(world)) {
                    result.add(entry);
                }
            }
        }
        return result;
    }
}
