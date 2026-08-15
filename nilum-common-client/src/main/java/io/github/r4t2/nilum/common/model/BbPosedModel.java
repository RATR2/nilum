package io.github.r4t2.nilum.common.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Combines BbModelBaker.bakeByBone's per-bone quads with a BbBonePose's per-bone world matrices into one posed mesh. */
public final class BbPosedModel {

    private BbPosedModel() {
    }

    public static List<BbBakedQuad> apply(Map<String, List<BbBakedQuad>> quadsByBone, Map<String, BbMatrix4> poseByBone) {
        List<BbBakedQuad> result = new ArrayList<>();
        quadsByBone.forEach((boneUuid, quads) -> {
            BbMatrix4 transform = poseByBone.getOrDefault(boneUuid, BbMatrix4.identity());
            for (BbBakedQuad quad : quads) {
                result.add(transformQuad(quad, transform));
            }
        });
        return result;
    }

    private static BbBakedQuad transformQuad(BbBakedQuad quad, BbMatrix4 transform) {
        return new BbBakedQuad(
                transformVertex(quad.v0(), transform),
                transformVertex(quad.v1(), transform),
                transformVertex(quad.v2(), transform),
                transformVertex(quad.v3(), transform),
                quad.textureIndex());
    }

    private static BbBakedVertex transformVertex(BbBakedVertex v, BbMatrix4 transform) {
        float[] position = transform.transformPoint(v.x(), v.y(), v.z());
        float[] normal = transform.transformDirection(v.nx(), v.ny(), v.nz());
        return new BbBakedVertex(position[0], position[1], position[2], v.u(), v.v(), normal[0], normal[1], normal[2]);
    }
}
