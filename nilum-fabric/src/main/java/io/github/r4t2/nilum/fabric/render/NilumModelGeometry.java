package io.github.r4t2.nilum.fabric.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.r4t2.nilum.common.model.BbBakedQuad;
import io.github.r4t2.nilum.common.model.BbBakedVertex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Shared baked-quad vertex emission, used by in-world entity rendering, held-item rendering, and block rendering. */
public final class NilumModelGeometry {

    private NilumModelGeometry() {
    }

    /** Regroups a posed mesh (BbPosedModel.apply's flat output) back by texture index for per-texture draw calls. */
    public static Map<Integer, List<BbBakedQuad>> groupByTexture(List<BbBakedQuad> quads) {
        Map<Integer, List<BbBakedQuad>> byTexture = new HashMap<>();
        for (BbBakedQuad quad : quads) {
            if (quad.textureIndex() == null) {
                continue;
            }
            byTexture.computeIfAbsent(quad.textureIndex(), key -> new ArrayList<>()).add(quad);
        }
        return byTexture;
    }

    public static void emitQuad(VertexConsumer consumer, PoseStack.Pose pose, BbBakedQuad quad, int light, int overlay) {
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
