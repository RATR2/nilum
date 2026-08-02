package io.github.r4t2.nilum.paper.model;

import io.github.r4t2.nilum.common.model.BbCollisionBox;
import org.bukkit.Location;
import org.bukkit.util.BoundingBox;

import java.util.ArrayList;
import java.util.List;

/** Converts nilum-common's loader-agnostic BbCollisionBoxes into world-space Bukkit BoundingBoxes. */
final class CollisionShapes {

    private CollisionShapes() {
    }

    static List<BoundingBox> toWorldBoundingBoxes(Location origin, List<BbCollisionBox> boxes) {
        List<BoundingBox> result = new ArrayList<>();
        for (BbCollisionBox box : boxes) {
            result.add(new BoundingBox(
                    origin.getX() + box.minX(), origin.getY() + box.minY(), origin.getZ() + box.minZ(),
                    origin.getX() + box.maxX(), origin.getY() + box.maxY(), origin.getZ() + box.maxZ()));
        }
        return result;
    }
}
