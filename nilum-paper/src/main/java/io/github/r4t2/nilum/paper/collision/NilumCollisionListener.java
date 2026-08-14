package io.github.r4t2.nilum.paper.collision;

import io.github.r4t2.nilum.common.model.CollisionResolver;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapts NilumCollisionRegistry's world-space Bukkit boxes to CollisionResolver's pure math, and
 * applies the resolved position back to the player's move. Players only, no other entity types.
 */
public final class NilumCollisionListener implements Listener {

    private static final double PLAYER_HALF_WIDTH = 0.3;
    private static final double PLAYER_HEIGHT = 1.8;

    private final NilumCollisionRegistry registry;

    public NilumCollisionListener(NilumCollisionRegistry registry) {
        this.registry = registry;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null || from.getWorld() == null
                || (from.getX() == to.getX() && from.getY() == to.getY() && from.getZ() == to.getZ())) {
            return;
        }

        List<NilumCollisionRegistry.Entry> entries = registry.entriesInWorld(from.getWorld().getName());
        if (entries.isEmpty()) {
            return;
        }
        List<CollisionResolver.Box> boxes = toBoxes(entries);

        Player player = event.getPlayer();
        double halfWidth = Math.max(PLAYER_HALF_WIDTH, player.getWidth() / 2.0);
        double height = player.getHeight() > 0 ? player.getHeight() : PLAYER_HEIGHT;

        double[] resolved = CollisionResolver.resolveMove(boxes, halfWidth, height,
                from.getX(), from.getY(), from.getZ(), to.getX(), to.getY(), to.getZ());

        double slow = CollisionResolver.partialSlowFactor(boxes, halfWidth, height, resolved[0], resolved[1], resolved[2]);
        if (slow < 1.0) {
            resolved[0] = from.getX() + (resolved[0] - from.getX()) * slow;
            resolved[1] = from.getY() + (resolved[1] - from.getY()) * slow;
            resolved[2] = from.getZ() + (resolved[2] - from.getZ()) * slow;
        }

        if (resolved[0] != to.getX() || resolved[1] != to.getY() || resolved[2] != to.getZ()) {
            event.setTo(new Location(to.getWorld(), resolved[0], resolved[1], resolved[2], to.getYaw(), to.getPitch()));
        }
    }

    private static List<CollisionResolver.Box> toBoxes(List<NilumCollisionRegistry.Entry> entries) {
        List<CollisionResolver.Box> boxes = new ArrayList<>(entries.size());
        for (NilumCollisionRegistry.Entry entry : entries) {
            var box = entry.box();
            boxes.add(new CollisionResolver.Box(
                    box.getMinX(), box.getMinY(), box.getMinZ(), box.getMaxX(), box.getMaxY(), box.getMaxZ(), entry.tier()));
        }
        return boxes;
    }
}
