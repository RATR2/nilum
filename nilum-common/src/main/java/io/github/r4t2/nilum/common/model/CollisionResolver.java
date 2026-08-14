package io.github.r4t2.nilum.common.model;

import java.util.List;

/** Pure AABB-vs-boxes movement resolution: SOLID boxes block (auto-stepping up to STEP_HEIGHT), PARTIAL boxes dampen instead. */
public final class CollisionResolver {

    public record Box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, CollisionTier tier) {

        boolean overlaps(double x0, double y0, double z0, double x1, double y1, double z1) {
            return minX < x1 && maxX > x0 && minY < y1 && maxY > y0 && minZ < z1 && maxZ > z0;
        }
    }

    private static final double STEP_HEIGHT = 0.6;
    private static final double STEP_SCAN_INCREMENT = 0.05;
    public static final double PARTIAL_MOVEMENT_FACTOR = 0.3;

    private CollisionResolver() {
    }

    /** Direct move if clear; else try stepping up onto a low SOLID box; else slide along whichever axes stay open. */
    public static double[] resolveMove(List<Box> boxes, double halfWidth, double height,
                                        double fx, double fy, double fz, double tx, double ty, double tz) {
        if (!collidesSolid(boxes, tx, ty, tz, halfWidth, height)) {
            return new double[]{tx, ty, tz};
        }

        if (ty <= fy) {
            for (double dy = 0; dy <= STEP_HEIGHT + 1e-9; dy += STEP_SCAN_INCREMENT) {
                double candidateY = fy + dy;
                if (!collidesSolid(boxes, tx, candidateY, tz, halfWidth, height)) {
                    return new double[]{tx, Math.max(candidateY, ty), tz};
                }
            }
        }

        double newX = collidesSolid(boxes, tx, fy, fz, halfWidth, height) ? fx : tx;
        double newZ = collidesSolid(boxes, newX, fy, tz, halfWidth, height) ? fz : tz;
        double newY = collidesSolid(boxes, newX, ty, newZ, halfWidth, height) ? fy : ty;
        return new double[]{newX, newY, newZ};
    }

    /** &lt;1 means dampen movement toward the start position by this fraction; 1 means no PARTIAL box in play. */
    public static double partialSlowFactor(List<Box> boxes, double halfWidth, double height, double x, double y, double z) {
        for (Box box : boxes) {
            if (box.tier() == CollisionTier.PARTIAL
                    && box.overlaps(x - halfWidth, y, z - halfWidth, x + halfWidth, y + height, z + halfWidth)) {
                return PARTIAL_MOVEMENT_FACTOR;
            }
        }
        return 1.0;
    }

    private static boolean collidesSolid(List<Box> boxes, double x, double y, double z, double halfWidth, double height) {
        for (Box box : boxes) {
            if (box.tier() == CollisionTier.SOLID
                    && box.overlaps(x - halfWidth, y, z - halfWidth, x + halfWidth, y + height, z + halfWidth)) {
                return true;
            }
        }
        return false;
    }
}
