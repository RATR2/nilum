package io.github.r4t2.nilum.paper.model;

import io.github.r4t2.nilum.common.model.BbCollisionBox;
import io.github.r4t2.nilum.paper.collision.NilumCollisionRegistry;
import org.bukkit.Location;
import org.bukkit.util.BoundingBox;

import java.util.ArrayList;
import java.util.List;

/** Converts nilum-common's loader-agnostic BbCollisionBoxes into world-space Bukkit BoundingBoxes. */
public final class CollisionShapes {

    private CollisionShapes() {
    }

    public static List<BoundingBox> toWorldBoundingBoxes(Location origin, List<BbCollisionBox> boxes) {
        List<BoundingBox> result = new ArrayList<>();
        for (BbCollisionBox box : boxes) {
            result.add(toWorldBoundingBox(origin, box));
        }
        return result;
    }

    public static List<NilumCollisionRegistry.Entry> toWorldEntries(Location origin, List<BbCollisionBox> boxes) {
        List<NilumCollisionRegistry.Entry> result = new ArrayList<>();
        for (BbCollisionBox box : boxes) {
            result.add(new NilumCollisionRegistry.Entry(origin.getWorld().getName(), toWorldBoundingBox(origin, box), box.tier()));
        }
        return result;
    }

    private static BoundingBox toWorldBoundingBox(Location origin, BbCollisionBox box) {
        return new BoundingBox(
                origin.getX() + box.minX(), origin.getY() + box.minY(), origin.getZ() + box.minZ(),
                origin.getX() + box.maxX(), origin.getY() + box.maxY(), origin.getZ() + box.maxZ());
    }
}
