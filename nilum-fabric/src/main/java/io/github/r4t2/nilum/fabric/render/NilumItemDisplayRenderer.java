package io.github.r4t2.nilum.fabric.render;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.r4t2.nilum.common.asset.ClientModelStore;
import io.github.r4t2.nilum.common.model.BbBakedQuad;
import io.github.r4t2.nilum.common.model.BbModel;
import io.github.r4t2.nilum.common.model.ClientModelPlacements;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.DisplayRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ItemDisplayEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Display;

import java.util.List;
import java.util.Map;

/**
 * Draws a Nilum model's baked geometry over vanilla's ItemDisplay rendering,
 * for any entity tracked as a Nilum placement. Falls through to vanilla
 * rendering otherwise. Issues one draw call per distinct texture a model uses.
 */
public final class NilumItemDisplayRenderer extends DisplayRenderer.ItemDisplayRenderer {

    private final ClientModelStore modelStore;
    private final ClientModelPlacements placements;
    private final TextureUploader textureUploader;

    public NilumItemDisplayRenderer(EntityRendererProvider.Context context, ClientModelStore modelStore,
                                     ClientModelPlacements placements, TextureUploader textureUploader) {
        super(context);
        this.modelStore = modelStore;
        this.placements = placements;
        this.textureUploader = textureUploader;
    }

    @Override
    public ItemDisplayEntityRenderState createRenderState() {
        return new NilumItemDisplayRenderState();
    }

    @Override
    public void extractRenderState(Display.ItemDisplay entity, ItemDisplayEntityRenderState reuseState, float partialTick) {
        super.extractRenderState(entity, reuseState, partialTick);

        if (!(reuseState instanceof NilumItemDisplayRenderState state)) {
            return;
        }

        String modelId = placements.get(entity.getUUID());
        if (modelId == null) {
            state.nilumModelId = null;
            state.nilumModel = null;
            state.nilumQuadsByTexture = null;
            return;
        }

        state.nilumModelId = modelId;
        state.nilumModel = modelStore.model(modelId).orElse(null);
        state.nilumQuadsByTexture = modelStore.bakedQuadsByTexture(modelId).orElse(null);
    }

    @Override
    public void submitInner(ItemDisplayEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
                             int light, float partialTick) {
        if (!(state instanceof NilumItemDisplayRenderState nilumState)
                || nilumState.nilumModel == null || nilumState.nilumQuadsByTexture == null) {
            super.submitInner(state, poseStack, collector, light, partialTick);
            return;
        }

        BbModel model = nilumState.nilumModel;
        String modelId = nilumState.nilumModelId;

        for (Map.Entry<Integer, List<BbBakedQuad>> entry : nilumState.nilumQuadsByTexture.entrySet()) {
            int textureIndex = entry.getKey();
            if (textureIndex >= model.textures().size()) {
                continue;
            }

            Identifier textureId = textureUploader.getOrUpload(modelId, textureIndex, model);
            RenderType renderType = RenderTypes.entityCutoutNoCull(textureId);
            List<BbBakedQuad> quads = entry.getValue();

            collector.submitCustomGeometry(poseStack, renderType, (pose, vertexConsumer) -> {
                for (BbBakedQuad quad : quads) {
                    NilumModelGeometry.emitQuad(vertexConsumer, pose, quad, light, OverlayTexture.NO_OVERLAY);
                }
            });
        }
    }
}
