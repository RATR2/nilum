package io.github.r4t2.nilum.common.model;

import java.util.ArrayList;
import java.util.List;

/** Compact text encoding for a list of collision boxes, for baking onto a placed block's own NBT. */
public final class CollisionBoxSerializer {

    private CollisionBoxSerializer() {
    }

    public static String encode(List<BbCollisionBox> boxes) {
        StringBuilder out = new StringBuilder();
        for (BbCollisionBox box : boxes) {
            if (!out.isEmpty()) {
                out.append(';');
            }
            out.append(box.minX()).append(',').append(box.minY()).append(',').append(box.minZ()).append(',')
                    .append(box.maxX()).append(',').append(box.maxY()).append(',').append(box.maxZ()).append(',')
                    .append(box.tier() == CollisionTier.PARTIAL ? 'P' : 'S');
        }
        return out.toString();
    }

    public static List<BbCollisionBox> decode(String encoded) {
        List<BbCollisionBox> boxes = new ArrayList<>();
        if (encoded == null || encoded.isBlank()) {
            return boxes;
        }
        for (String entry : encoded.split(";")) {
            String[] parts = entry.split(",");
            if (parts.length != 7) {
                continue;
            }
            CollisionTier tier = parts[6].equals("P") ? CollisionTier.PARTIAL : CollisionTier.SOLID;
            boxes.add(new BbCollisionBox(
                    Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]),
                    Double.parseDouble(parts[3]), Double.parseDouble(parts[4]), Double.parseDouble(parts[5]), tier));
        }
        return boxes;
    }
}
