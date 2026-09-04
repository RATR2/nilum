package io.github.r4t2.nilum.fabric.render;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.r4t2.nilum.common.asset.ClientModelStore;
import io.github.r4t2.nilum.common.model.BbBakedQuad;
import io.github.r4t2.nilum.common.model.BbBakedVertex;
import io.github.r4t2.nilum.common.model.BbBonePose;
import io.github.r4t2.nilum.common.model.BbMatrix4;
import io.github.r4t2.nilum.common.model.BbModel;
import io.github.r4t2.nilum.common.model.BbPosedModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Draws a Nilum full-.bbmodel item as its actual baked geometry, the same quads the in-world entity renderer draws. */
public final class NilumModelItemSpecialRenderer implements SpecialModelRenderer<NilumModelItemSpecialRenderer.RenderArgument> {

    /**
     * @param bonePose        null falls back to auto-looping the model's first animation (non-held contexts: gui/ground/etc.)
     * @param hiddenBoneUuids bones to exclude entirely, e.g. a held item at rest with configured hide_groups
     */
    public record RenderArgument(String modelId, Map<String, BbMatrix4> bonePose, Set<String> hiddenBoneUuids) {
    }

    private final ClientModelStore modelStore;
    private final TextureUploader textureUploader;
    private final Map<String, Vector3fc[]> extentsByModelId = new ConcurrentHashMap<>();

    public NilumModelItemSpecialRenderer(ClientModelStore modelStore, TextureUploader textureUploader) {
        this.modelStore = modelStore;
        this.textureUploader = textureUploader;
    }

    @Override
    public void submit(RenderArgument argument, ItemDisplayContext itemDisplayContext, PoseStack poseStack,
                        SubmitNodeCollector collector, int light, int overlay, boolean hasFoil, int seed) {
        if (argument == null) {
            return;
        }
        String modelId = argument.modelId();

        BbModel model = modelStore.model(modelId).orElse(null);
        Map<String, List<BbBakedQuad>> quadsByBone = modelStore.bakedQuadsByBone(modelId).orElse(null);
        if (model == null || quadsByBone == null) {
            return;
        }
        if (!argument.hiddenBoneUuids().isEmpty()) {
            Map<String, List<BbBakedQuad>> filtered = new HashMap<>(quadsByBone);
            argument.hiddenBoneUuids().forEach(filtered::remove);
            quadsByBone = filtered;
        }

        Map<String, BbMatrix4> bonePose = argument.bonePose() != null ? argument.bonePose() : BbBonePose.computeAutoLoop(model);
        List<BbBakedQuad> posed = BbPosedModel.apply(quadsByBone, bonePose);
        Map<Integer, List<BbBakedQuad>> quadsByTexture = NilumModelGeometry.groupByTexture(posed);

        for (Map.Entry<Integer, List<BbBakedQuad>> entry : quadsByTexture.entrySet()) {
            int textureIndex = entry.getKey();
            if (textureIndex >= model.textures().size()) {
                continue;
            }

            Identifier textureId = textureUploader.getOrUpload(modelId, textureIndex, model);
            RenderType renderType = RenderTypes.entityCutoutNoCull(textureId);
            List<BbBakedQuad> quads = entry.getValue();

            collector.submitCustomGeometry(poseStack, renderType, (pose, vertexConsumer) -> {
                for (BbBakedQuad quad : quads) {
                    NilumModelGeometry.emitQuad(vertexConsumer, pose, quad, light, overlay);
                }
            });
        }
    }

    /** Left empty; real per-model extents wiring happens via extentsOf(String), called directly by NilumModelItemModel. */
    @Override
    public void getExtents(Consumer<Vector3fc> consumer) {
    }

    /** Exact bounding-box corners (8) for one model's baked geometry, computed once and cached. */
    public Vector3fc[] extentsOf(String modelId) {
        return extentsByModelId.computeIfAbsent(modelId, this::computeExtents);
    }

    private Vector3fc[] computeExtents(String modelId) {
        Map<Integer, List<BbBakedQuad>> quadsByTexture = modelStore.bakedQuadsByTexture(modelId).orElse(null);
        if (quadsByTexture == null) {
            return new Vector3fc[]{new Vector3f(0, 0, 0), new Vector3f(1, 1, 1)};
        }

        float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY, minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;
        boolean any = false;

        for (List<BbBakedQuad> quads : quadsByTexture.values()) {
            for (BbBakedQuad quad : quads) {
                for (BbBakedVertex v : new BbBakedVertex[]{quad.v0(), quad.v1(), quad.v2(), quad.v3()}) {
                    any = true;
                    minX = Math.min(minX, v.x());
                    minY = Math.min(minY, v.y());
                    minZ = Math.min(minZ, v.z());
                    maxX = Math.max(maxX, v.x());
                    maxY = Math.max(maxY, v.y());
                    maxZ = Math.max(maxZ, v.z());
                }
            }
        }

        if (!any) {
            return new Vector3fc[]{new Vector3f(0, 0, 0), new Vector3f(1, 1, 1)};
        }

        return new Vector3fc[]{
                new Vector3f(minX, minY, minZ), new Vector3f(maxX, minY, minZ),
                new Vector3f(minX, maxY, minZ), new Vector3f(maxX, maxY, minZ),
                new Vector3f(minX, minY, maxZ), new Vector3f(maxX, minY, maxZ),
                new Vector3f(minX, maxY, maxZ), new Vector3f(maxX, maxY, maxZ),
        };
    }

    @Override
    public RenderArgument extractArgument(ItemStack itemStack) {
        return null;
    }
}
