package io.github.r4t2.nilum.fabric.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.r4t2.nilum.common.model.BbBonePose;
import io.github.r4t2.nilum.common.model.BbElement;
import io.github.r4t2.nilum.common.model.BbMatrix4;
import io.github.r4t2.nilum.common.model.BbModel;
import io.github.r4t2.nilum.common.model.BbOutlinerGroup;
import io.github.r4t2.nilum.common.model.BbVector3;
import io.github.r4t2.nilum.fabric.NilumFabricClient;
import io.github.r4t2.nilum.fabric.render.NilumDisplayTransforms;
import io.github.r4t2.nilum.fabric.render.NilumItemTags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Drives the first-person hand with a held Nilum item's "Left arm"/"Right arm" bone when present; otherwise vanilla rendering. */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {

    private static final float ITEM_POS_X = 0.56F;
    private static final float ITEM_POS_Y = -0.52F;
    private static final float ITEM_POS_Z = -0.72F;

    @Inject(method = "renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;"
            + "FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;"
            + "Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V", at = @At("HEAD"), cancellable = true)
    private void nilum$onRenderArmWithItem(AbstractClientPlayer player, float partialTick, float pitch, InteractionHand hand,
                                            float attackAnim, ItemStack itemStack, float swapHeightOffset,
                                            PoseStack poseStack, SubmitNodeCollector collector, int light, CallbackInfo ci) {
        if (player.isScoping() || itemStack.isEmpty()) {
            return;
        }

        String modelId = NilumItemTags.get(itemStack, "nilum:model_id");
        if (modelId == null) {
            return;
        }
        BbModel model = NilumFabricClient.MODEL_STORE.model(modelId).orElse(null);
        if (model == null) {
            return;
        }

        boolean rightArm = (hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite()) == HumanoidArm.RIGHT;
        Optional<BbOutlinerGroup> armBone = model.findGroup(rightArm ? "Right arm" : "Left arm");
        if (armBone.isEmpty()) {
            return;
        }

        List<BbElement> armElements = model.resolveElements(armBone.get());
        if (armElements.isEmpty()) {
            return;
        }
        BbElement marker = armElements.get(0);
        BbVector3 markerOrigin = marker.origin();
        BbVector3 markerRotation = marker.rotation();

        Map<String, BbMatrix4> bonePose = BbBonePose.computeAutoLoop(model);
        BbMatrix4 boneWorld = bonePose.get(armBone.get().uuid());
        if (boneWorld == null) {
            return;
        }

        float[] attachPoint = boneWorld.transformPoint(
                (float) (markerOrigin.x() / 16.0), (float) (markerOrigin.y() / 16.0), (float) (markerOrigin.z() / 16.0));
        Matrix4f attachRotation = toJoml(boneWorld);
        attachRotation.setTranslation(0, 0, 0);
        Matrix4f markerRotationJoml = toJoml(BbMatrix4.rotationXYZDegrees(
                (float) markerRotation.x(), (float) markerRotation.y(), (float) markerRotation.z()));

        ci.cancel();

        ItemDisplayContext displayContext = rightArm ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
        float anchorX = rightArm ? ITEM_POS_X : -ITEM_POS_X;
        float sign = rightArm ? 1.0F : -1.0F;

        // Item: same resting anchor vanilla's own applyItemArmTransform uses, plus the same swing
        // vanilla's own swingArm applies on attack. Its own first-person display transform is
        // applied again automatically inside renderItem's normal per-item pipeline, so it must not
        // be applied here too, or the item would be double-transformed.
        poseStack.pushPose();
        poseStack.translate(anchorX, ITEM_POS_Y, ITEM_POS_Z);
        applySwing(poseStack, attackAnim, sign);
        ((ItemInHandRenderer) (Object) this).renderItem(player, itemStack, displayContext, poseStack, collector, light);
        poseStack.popPose();

        // Arm: same anchor and swing, then the item's display transform, then seated at the
        // marker's animated world position, oriented by the bone's rotation composed with the
        // marker's own authored rotation. No hidden vanilla "natural hand" correction.
        poseStack.pushPose();
        poseStack.translate(anchorX, ITEM_POS_Y, ITEM_POS_Z);
        applySwing(poseStack, attackAnim, sign);
        NilumDisplayTransforms.resolve(model, displayContext.getSerializedName()).apply(!rightArm, poseStack.last());
        poseStack.translate(attachPoint[0], attachPoint[1], attachPoint[2]);
        poseStack.mulPose(attachRotation);
        poseStack.mulPose(markerRotationJoml);

        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        AvatarRenderer<AbstractClientPlayer> avatarRenderer = dispatcher.getPlayerRenderer(player);
        Identifier skinTexture = player.getSkin().body().texturePath();
        if (rightArm) {
            avatarRenderer.renderRightHand(poseStack, collector, light, skinTexture, player.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE));
        } else {
            avatarRenderer.renderLeftHand(poseStack, collector, light, skinTexture, player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE));
        }
        poseStack.popPose();
    }

    /** Vanilla's own swingArm plus applyItemArmAttackTransform (the WHACK-type attack swing every plain item uses). */
    private static void applySwing(PoseStack poseStack, float attackAnim, float sign) {
        float f = -0.4F * Mth.sin(Mth.sqrt(attackAnim) * (float) Math.PI);
        float f1 = 0.2F * Mth.sin(Mth.sqrt(attackAnim) * (float) (Math.PI * 2));
        float f2 = -0.2F * Mth.sin(attackAnim * (float) Math.PI);
        poseStack.translate(sign * f, f1, f2);

        float f3 = Mth.sin(attackAnim * attackAnim * (float) Math.PI);
        poseStack.mulPose(Axis.YP.rotationDegrees(sign * (45.0F + f3 * -20.0F)));
        float f4 = Mth.sin(Mth.sqrt(attackAnim) * (float) Math.PI);
        poseStack.mulPose(Axis.ZP.rotationDegrees(sign * f4 * -20.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(f4 * -80.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(sign * -45.0F));
    }

    /** BbMatrix4 is row-major (index row*4+col); JOML's Matrix4f constructor takes column-major (col0 first). */
    private static Matrix4f toJoml(BbMatrix4 matrix) {
        float[] m = matrix.toRowMajorArray();
        return new Matrix4f(
                m[0], m[4], m[8], m[12],
                m[1], m[5], m[9], m[13],
                m[2], m[6], m[10], m[14],
                m[3], m[7], m[11], m[15]
        );
    }
}
