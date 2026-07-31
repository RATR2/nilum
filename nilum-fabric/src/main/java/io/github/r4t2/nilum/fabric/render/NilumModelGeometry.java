package io.github.r4t2.nilum.fabric.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.r4t2.nilum.common.model.BbBakedQuad;
import io.github.r4t2.nilum.common.model.BbBakedVertex;

/** Shared baked-quad vertex emission, used by both in-world entity rendering and held-item rendering. */
final class NilumModelGeometry {

    private NilumModelGeometry() {
    }

    static void emitQuad(VertexConsumer consumer, PoseStack.Pose pose, BbBakedQuad quad, int light, int overlay) {
        emitVertex(consumer, pose, quad.v0(), light, overlay);
        emitVertex(consumer, pose, quad.v1(), light, overlay);
        emitVertex(consumer, pose, quad.v2(), light, overlay);
        emitVertex(consumer, pose, quad.v3(), light, overlay);
    }

    private static void emitVertex(VertexConsumer consumer, PoseStack.Pose pose, BbBakedVertex vertex, int light, int overlay) {
        consumer.addVertex(pose, vertex.x(), vertex.y(), vertex.z())
                .setColor(255, 255, 255, 255)
                .setUv(vertex.u(), vertex.v())
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(pose, vertex.nx(), vertex.ny(), vertex.nz());
    }
}
