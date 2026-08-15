package io.github.r4t2.nilum.fabric.block;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.r4t2.nilum.common.asset.ClientModelStore;
import io.github.r4t2.nilum.common.model.BbBakedQuad;
import io.github.r4t2.nilum.common.model.BbMatrix4;
import io.github.r4t2.nilum.common.model.BbModel;
import io.github.r4t2.nilum.common.model.BbPosedModel;
import io.github.r4t2.nilum.fabric.render.NilumModelGeometry;
import io.github.r4t2.nilum.fabric.render.TextureUploader;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;

/** Draws a real Nilum block's baked geometry, one draw call per distinct texture. Every real Nilum block is fully invisible otherwise. */
public final class NilumBlockEntityRenderer implements BlockEntityRenderer<NilumBlockEntity, NilumBlockEntityRenderState> {

    private final ClientModelStore modelStore;
    private final ClientBlockRegistry blockRegistry;
    private final TextureUploader textureUploader;

    public NilumBlockEntityRenderer(ClientModelStore modelStore, ClientBlockRegistry blockRegistry, TextureUploader textureUploader) {
        this.modelStore = modelStore;
        this.blockRegistry = blockRegistry;
        this.textureUploader = textureUploader;
    }

    @Override
    public NilumBlockEntityRenderState createRenderState() {
        return new NilumBlockEntityRenderState();
    }

    @Override
    public void extractRenderState(NilumBlockEntity blockEntity, NilumBlockEntityRenderState state, float partialTick,
                                    Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderState.extractBase(blockEntity, state, crumblingOverlay);

        String modelId = blockEntity.definitionId();
        state.nilumModelId = modelId;
        state.nilumModel = modelStore.model(modelId).orElse(null);
        state.nilumQuadsByBone = modelStore.bakedQuadsByBone(modelId).orElse(null);
        state.nilumAnimationState = blockRegistry.animationState(blockEntity.getBlockPos());
    }

    @Override
    public void submit(NilumBlockEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraRenderState) {
        BbModel model = state.nilumModel;
        if (model == null || state.nilumQuadsByBone == null) {
            return;
        }

        Map<String, BbMatrix4> bonePose = state.nilumAnimationState.pose(model, System.currentTimeMillis());
        List<BbBakedQuad> posed = BbPosedModel.apply(state.nilumQuadsByBone, bonePose);
        Map<Integer, List<BbBakedQuad>> quadsByTexture = NilumModelGeometry.groupByTexture(posed);

        for (Map.Entry<Integer, List<BbBakedQuad>> entry : quadsByTexture.entrySet()) {
            int textureIndex = entry.getKey();
            if (textureIndex >= model.textures().size()) {
                continue;
            }

            Identifier textureId = textureUploader.getOrUpload(state.nilumModelId, textureIndex, model);
            RenderType renderType = RenderTypes.entityCutoutNoCull(textureId);
            List<BbBakedQuad> quads = entry.getValue();
            int light = state.lightCoords;

            collector.submitCustomGeometry(poseStack, renderType, (pose, vertexConsumer) -> {
                for (BbBakedQuad quad : quads) {
                    NilumModelGeometry.emitQuad(vertexConsumer, pose, quad, light, OverlayTexture.NO_OVERLAY);
                }
            });
        }
    }
}
