package io.github.r4t2.nilum.fabric.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.r4t2.nilum.common.model.AnimationPlaybackState;
import io.github.r4t2.nilum.common.model.BbBakedQuad;
import io.github.r4t2.nilum.common.model.BbMatrix4;
import io.github.r4t2.nilum.common.model.BbModel;
import io.github.r4t2.nilum.common.model.BbPosedModel;
import io.github.r4t2.nilum.fabric.NilumFabricClient;
import io.github.r4t2.nilum.fabric.render.NilumModelGeometry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.PlayerModelType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

/** Draws a Nilum skeleton model over a player's avatar. Injects into LivingEntityRenderer.submit() rather than swapping the renderer. */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<S extends LivingEntityRenderState> {

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;"
            + "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;"
            + "Lnet/minecraft/client/renderer/state/CameraRenderState;)V", at = @At("HEAD"), cancellable = true)
    private void nilum$onSubmit(S state, PoseStack poseStack, SubmitNodeCollector collector,
                                 CameraRenderState cameraRenderState, CallbackInfo ci) {
        if (NilumFabricClient.PLACEMENTS == null || Minecraft.getInstance().level == null) {
            return;
        }
        if (!(state instanceof AvatarRenderState avatarState)) {
            return;
        }

        Entity entity = Minecraft.getInstance().level.getEntity(avatarState.id);
        if (!(entity instanceof AbstractClientPlayer player)) {
            return;
        }

        String modelId = NilumFabricClient.PLACEMENTS.get(player.getUUID());
        if (modelId == null) {
            return;
        }
        if (avatarState.skin.model() == PlayerModelType.SLIM) {
            // steve.bbmodel's UVs are authored to vanilla's wide-skin layout; slim skins shift arm
            // UV regions this rig doesn't account for, so slim-skinned players keep the vanilla avatar.
            return;
        }

        BbModel model = NilumFabricClient.MODEL_STORE.model(modelId).orElse(null);
        Map<String, List<BbBakedQuad>> quadsByBone = NilumFabricClient.MODEL_STORE.bakedQuadsByBone(modelId).orElse(null);
        if (model == null || quadsByBone == null) {
            return;
        }

        AnimationPlaybackState animationState = NilumFabricClient.PLACEMENTS.animationState(player.getUUID());

        ci.cancel();
        draw(avatarState, poseStack, collector, modelId, model, quadsByBone, animationState);
    }

    /** Draws with the real player's own skin (AvatarRenderState.skin), not a placeholder texture. */
    private void draw(AvatarRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
                       String modelId, BbModel model, Map<String, List<BbBakedQuad>> quadsByBone,
                       AnimationPlaybackState animationState) {
        poseStack.pushPose();
        float scale = state.scale;
        poseStack.scale(scale, scale, scale);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.bodyRot));
        // Grounds the model at its own lowest rest-pose vertex, regardless of how the model's
        // origin was set up in Blockbench.
        poseStack.translate(0.0F, -NilumFabricClient.MODEL_STORE.groundOffset(modelId), 0.0F);

        Map<String, BbMatrix4> bonePose = animationState.pose(model, System.currentTimeMillis());
        List<BbBakedQuad> posed = BbPosedModel.apply(quadsByBone, bonePose);

        Identifier skinTextureId = state.skin.body().texturePath();
        RenderType renderType = RenderTypes.entityCutoutNoCull(skinTextureId);
        int light = state.lightCoords;

        collector.submitCustomGeometry(poseStack, renderType, (pose, vertexConsumer) -> {
            for (BbBakedQuad quad : posed) {
                NilumModelGeometry.emitQuad(vertexConsumer, pose, quad, light, OverlayTexture.NO_OVERLAY);
            }
        });

        poseStack.popPose();
    }
}
