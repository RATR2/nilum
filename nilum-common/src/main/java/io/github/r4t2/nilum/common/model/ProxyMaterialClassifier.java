package io.github.r4t2.nilum.common.model;

import java.util.List;

/**
 * Classifies a resolved collision shape into a simple vanilla-representable proxy. Only
 * recognizes a single box spanning the full block, or a half-height slab; anything else
 * (multiple boxes, partial footprint, stairs) falls back to BbProxyShape.COMPLEX.
 */
public final class ProxyMaterialClassifier {

    private static final double EPSILON = 1.0 / 32.0;

    private ProxyMaterialClassifier() {
    }

    public static BbProxyShape classify(List<BbCollisionBox> boxes) {
        if (boxes.size() != 1) {
            return BbProxyShape.COMPLEX;
        }

        BbCollisionBox box = boxes.get(0);
        boolean fullFootprint = near(box.minX(), 0) && near(box.maxX(), 1)
                && near(box.minZ(), 0) && near(box.maxZ(), 1);
        if (!fullFootprint) {
            return BbProxyShape.COMPLEX;
        }

        boolean fullHeight = near(box.minY(), 0) && near(box.maxY(), 1);
        if (fullHeight) {
            return BbProxyShape.FULL_CUBE;
        }

        boolean bottomHalf = near(box.minY(), 0) && box.maxY() > EPSILON && box.maxY() <= 0.5 + EPSILON;
        if (bottomHalf) {
            return BbProxyShape.SLAB_BOTTOM;
        }

        boolean topHalf = near(box.maxY(), 1) && box.minY() < 1 - EPSILON && box.minY() >= 0.5 - EPSILON;
        if (topHalf) {
            return BbProxyShape.SLAB_TOP;
        }

        return BbProxyShape.COMPLEX;
    }

    private static boolean near(double value, double target) {
        return Math.abs(value - target) <= EPSILON;
    }
}
