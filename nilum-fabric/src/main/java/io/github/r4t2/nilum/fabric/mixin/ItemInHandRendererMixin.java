package io.github.r4t2.nilum.fabric.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.r4t2.nilum.common.model.BbElement;
import io.github.r4t2.nilum.common.model.BbMatrix4;
import io.github.r4t2.nilum.common.model.BbModel;
import io.github.r4t2.nilum.common.model.BbOutlinerGroup;
import io.github.r4t2.nilum.common.model.BbVector3;
import io.github.r4t2.nilum.fabric.NilumFabricClient;
import io.github.r4t2.nilum.fabric.debug.HandTuneCorrection;
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
import org.jspecify.annotations.Nullable;
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

    // PlayerModel's own right_arm/left_arm box (width, height, depth), Blockbench-unit scale.
    // Uses the normal (non-slim) 4-wide variant as the reference regardless of the player's
    // actual skin type; slim arms are 3 wide, a difference too small to bother distinguishing.
    private static final float VANILLA_ARM_WIDTH = 4.0F;
    private static final float VANILLA_ARM_HEIGHT = 12.0F;
    private static final float VANILLA_ARM_DEPTH = 4.0F;

    // Where PlayerModel's own pivot sits within its arm box, as a fraction from the box's own
    // "from" corner (right_arm local box is (-3,-2,-2) to (1,10,2), pivot at local (0,0,0), so
    // 3/4 across width, 2/12 down height, 2/4 across depth). Not centered on any axis, closest to
    // the shoulder end along height. Mirrored across width for the left arm.
    private static final float VANILLA_PIVOT_FRACTION_WIDTH_RIGHT = 0.75F;
    private static final float VANILLA_PIVOT_FRACTION_WIDTH_LEFT = 0.25F;
    private static final float VANILLA_PIVOT_FRACTION_HEIGHT = 2.0F / 12.0F;
    private static final float VANILLA_PIVOT_FRACTION_DEPTH = 0.5F;

    // Each arm box's own geometric center relative to its pivot at local (0,0,0) (right_arm box
    // (-3,-2,-2) to (1,10,2); left_arm mirrored across width to (-1,-2,-2)-(3,10,2)): used to
    // re-center the "Left arm" role's 180-degree Z correction below on whichever mesh is actually
    // being drawn, rather than on the pivot, which sits at a box corner.
    private static final float VANILLA_ARM_CENTER_X_RIGHT = -1.0F;
    private static final float VANILLA_ARM_CENTER_X_LEFT = 1.0F;
    private static final float VANILLA_ARM_CENTER_Y = 4.0F;

    // Conjugating a rotation by this (Sx * R * Sx) mirrors its apparent direction while staying a
    // proper rotation (determinant +1), so it reorients rather than flips the chirality of
    // whatever fixed mesh gets drawn through it afterward.
    private static final BbMatrix4 MIRROR_X = BbMatrix4.scale(-1, 1, 1);

    @Inject(method = "renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;"
            + "FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;"
            + "Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V", at = @At("HEAD"), cancellable = true)
    private void nilum$onRenderArmWithItem(AbstractClientPlayer player, float partialTick, float pitch, InteractionHand hand,
                                            float attackAnim, ItemStack itemStack, float swapHeightOffset,
                                            PoseStack poseStack, SubmitNodeCollector collector, int light, CallbackInfo ci) {
        if (player.isScoping()) {
            return;
        }

        // A marker like "Left arm" can represent the player's own arm (or whatever it's holding)
        // moving into frame as part of an animation playing on the OTHER hand's item (e.g. a
        // scanner in the right hand "scanning" the left arm), not just a substitute grip for an
        // item actually held in this hand. Try this hand's own item first; if it isn't a Nilum
        // item, or its model has no marker for this hand's role, fall back to borrowing the other
        // hand's item/animation for the model+pose, but still render only the arm here, never a
        // second copy of that item.
        ItemStack drivingStack = itemStack;
        InteractionHand drivingHand = hand;
        boolean rendersItem = !itemStack.isEmpty();

        BbModel model = resolveModel(drivingStack);
        // Marker choice is a fixed ROLE relative to this item ("Right arm" = the hand actually
        // gripping it, "Left arm" = the other one), not tied to handedness/visual side. A
        // left-handed player's main hand still grips with "Right arm"; it just renders on the
        // visual left.
        String markerGroupName = "Right arm";
        Optional<BbOutlinerGroup> armBone = model == null ? Optional.empty() : model.findGroup(markerGroupName);

        if (armBone.isEmpty()) {
            drivingHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            drivingStack = player.getItemInHand(drivingHand);
            model = resolveModel(drivingStack);
            markerGroupName = "Left arm";
            armBone = model == null ? Optional.empty() : model.findGroup(markerGroupName);
            if (armBone.isEmpty()) {
                return;
            }
        }

        // Visual side (which physical screen-side this hand renders on) drives the vanilla-mesh
        // mirroring math below (anchor, pivot fraction, which ModelPart to draw); it must follow
        // handedness so the correct arm mesh lands on the correct side.
        boolean rightArm = (hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite()) == HumanoidArm.RIGHT;

        List<BbElement> armElements = model.resolveElements(armBone.get());
        if (armElements.isEmpty()) {
            return;
        }

        boolean drivingRightHand = (drivingHand == InteractionHand.MAIN_HAND
                ? player.getMainArm() : player.getMainArm().getOpposite()) == HumanoidArm.RIGHT;
        var animationState = NilumFabricClient.HELD_ITEM_ANIMATIONS.get(player.getUUID(), drivingRightHand, model);
        if (animationState.isIdle(model, System.currentTimeMillis()) && isHiddenGroup(drivingStack, markerGroupName)) {
            return;
        }

        BbElement marker = armElements.get(0);
        BbVector3 markerOrigin = marker.origin();
        BbVector3 markerRotation = marker.rotation();
        BbVector3 markerSize = marker.size();
        float widthFraction = rightArm ? VANILLA_PIVOT_FRACTION_WIDTH_RIGHT : VANILLA_PIVOT_FRACTION_WIDTH_LEFT;
        BbVector3 markerPivotEquivalent = new BbVector3(
                marker.from().x() + widthFraction * markerSize.x(),
                marker.from().y() + VANILLA_PIVOT_FRACTION_HEIGHT * markerSize.y(),
                marker.from().z() + VANILLA_PIVOT_FRACTION_DEPTH * markerSize.z());

        Map<String, BbMatrix4> bonePose = animationState.pose(model, System.currentTimeMillis());
        BbMatrix4 boneWorld = bonePose.get(armBone.get().uuid());
        if (boneWorld == null) {
            return;
        }

        // The marker's own "origin" is a Blockbench rotation pivot, not necessarily where the
        // hand should attach. Use the point within the marker's own box that sits at the same
        // fractional position vanilla's own pivot sits within its arm box instead (not the
        // center; vanilla's pivot is off-center on every axis), rotated around the marker's own
        // pivot by the marker's own rotation, matching how Blockbench itself places the cube.
        BbMatrix4 pivotRotation = BbMatrix4.translation(
                        (float) (markerOrigin.x() / 16.0), (float) (markerOrigin.y() / 16.0), (float) (markerOrigin.z() / 16.0))
                .multiply(BbMatrix4.rotationXYZDegrees(
                        (float) markerRotation.x(), (float) markerRotation.y(), (float) markerRotation.z()))
                .multiply(BbMatrix4.translation(
                        (float) (-markerOrigin.x() / 16.0), (float) (-markerOrigin.y() / 16.0), (float) (-markerOrigin.z() / 16.0)));
        float[] localAttach = pivotRotation.transformPoint(
                (float) (markerPivotEquivalent.x() / 16.0), (float) (markerPivotEquivalent.y() / 16.0), (float) (markerPivotEquivalent.z() / 16.0));

        float[] attachPoint = boneWorld.transformPoint(localAttach[0], localAttach[1], localAttach[2]);
        BbMatrix4 rotationSource = boneWorld;
        if (!drivingRightHand) {
            // A world-space position from an unmirrored transform mirrors correctly by simply
            // negating X afterward. Orientation needs the opposite treatment: negating X here too
            // would double-mirror it back to the original direction, since rotation direction
            // already flips correctly when the transform itself is conjugated (Sx * M * Sx) below.
            attachPoint[0] = -attachPoint[0];
            rotationSource = MIRROR_X.multiply(boneWorld).multiply(MIRROR_X);
        }
        Matrix4f attachRotation = toJoml(rotationSource);
        attachRotation.setTranslation(0, 0, 0);
        Matrix4f markerRotationJoml = toJoml(BbMatrix4.rotationXYZDegrees(
                (float) markerRotation.x(), (float) markerRotation.y(), (float) markerRotation.z()));

        ci.cancel();

        // Anchored on the driving hand's screen side, not this render call's own hand: the arm
        // marker is a bone within the driving item's own model, animated in that model's local
        // space, so it must share the driving item's on-screen anchor. For a hand actually
        // holding its own item, drivingRightHand == rightArm, so this doesn't change that path.
        ItemDisplayContext displayContext = drivingRightHand ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
        float anchorX = drivingRightHand ? ITEM_POS_X : -ITEM_POS_X;
        float sign = drivingRightHand ? 1.0F : -1.0F;

        // Item: same resting anchor vanilla's own applyItemArmTransform uses, plus the same swing
        // vanilla's own swingArm applies on attack. Its own first-person display transform is
        // applied again automatically inside renderItem's normal per-item pipeline, so it must not
        // be applied here too, or the item would be double-transformed. Skipped entirely when this
        // hand is empty and only borrowing the other hand's item for the arm marker below.
        if (rendersItem) {
            poseStack.pushPose();
            poseStack.translate(anchorX, ITEM_POS_Y, ITEM_POS_Z);
            applySwing(poseStack, attackAnim, sign);
            ((ItemInHandRenderer) (Object) this).renderItem(player, itemStack, displayContext, poseStack, collector, light);
            poseStack.popPose();
        }

        // Arm: same anchor and swing, then the item's display transform, then seated at the
        // marker's animated world position, oriented by the bone's rotation composed with the
        // marker's own authored rotation. No hidden vanilla "natural hand" correction.
        poseStack.pushPose();
        poseStack.translate(anchorX, ITEM_POS_Y, ITEM_POS_Z);
        applySwing(poseStack, attackAnim, sign);
        // resolve() already pre-compensates for vanilla's own left-hand auto-mirror, so this is
        // never mirrored again here regardless of which arm is rendering.
        NilumDisplayTransforms.resolve(model, displayContext.getSerializedName()).apply(false, poseStack.last());
        poseStack.translate(attachPoint[0], attachPoint[1], attachPoint[2]);
        poseStack.mulPose(attachRotation);
        poseStack.mulPose(markerRotationJoml);
        if (markerGroupName.equals("Left arm")) {
            // The "Left arm" role's rotation data reads 180 degrees off on Z unless corrected -
            // this is tied to the ROLE, not which visual side/mesh it ends up rendering through:
            // for a left-handed player, "Right arm" (the driving hand) can end up on the left
            // mesh and "Left arm" on the right mesh, but the correction still only ever applies
            // to "Left arm". The pivot point for it does depend on the actual mesh being drawn
            // (each arm box has a different center), so that part still follows rightArm.
            float centerX = rightArm ? VANILLA_ARM_CENTER_X_RIGHT : VANILLA_ARM_CENTER_X_LEFT;
            poseStack.translate(centerX / 16.0F, VANILLA_ARM_CENTER_Y / 16.0F, 0.0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            poseStack.translate(-centerX / 16.0F, -VANILLA_ARM_CENTER_Y / 16.0F, 0.0F);
        }

        // Scales the vanilla hand mesh to match the marker's own bounding size instead of
        // always rendering at vanilla's fixed proportions, so a marker drawn bigger or smaller
        // than a normal arm actually looks bigger or smaller in-game.
        poseStack.scale((float) (markerSize.x() / VANILLA_ARM_WIDTH),
                (float) (markerSize.y() / VANILLA_ARM_HEIGHT),
                (float) (markerSize.z() / VANILLA_ARM_DEPTH));

        // Corrects the marker's own rotation to match vanilla's first-person hand exactly,
        // tuned via NilumHandTuneScreen against a neutral origin-0,0 test rig. Still
        // live-adjustable via HandTuneKeybind if a future model needs a different fit.
        // Rotation MUST come before the position translate below, or it pivots around
        // (attach point + position offset) instead of the attach point itself, which makes the
        // sliders behave completely differently from one model's marker origin to another's.
        poseStack.mulPose(Axis.XP.rotationDegrees(HandTuneCorrection.rotX));
        poseStack.mulPose(Axis.YP.rotationDegrees(HandTuneCorrection.rotY));
        poseStack.mulPose(Axis.ZP.rotationDegrees(HandTuneCorrection.rotZ));
        poseStack.translate(HandTuneCorrection.posX, HandTuneCorrection.posY, HandTuneCorrection.posZ);

        // AvatarRenderer.renderRightHand/renderLeftHand draws playerModel.rightArm/leftArm at
        // its own baked-in vanilla pivot (PartPose.offset(-5, 2, 0) for the right arm, mirrored
        // for the left, in HumanoidModel/PlayerModel), not wherever our poseStack currently is.
        // We've never accounted for that pivot, so the mesh always rendered offset from our
        // attach point by that amount. Counter-translate here so vanilla's own re-application
        // of it cancels out, landing the ModelPart's origin exactly at our attach point instead.
        float vanillaPivotX = rightArm ? -5.0F : 5.0F;
        poseStack.translate(-vanillaPivotX / 16.0F, -2.0F / 16.0F, 0.0F);

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

    private static @Nullable BbModel resolveModel(ItemStack stack) {
        String modelId = NilumItemTags.get(stack, "nilum:model_id");
        return modelId == null ? null : NilumFabricClient.MODEL_STORE.model(modelId).orElse(null);
    }

    private static boolean isHiddenGroup(ItemStack itemStack, String groupName) {
        String raw = NilumItemTags.get(itemStack, "nilum:hide_groups");
        if (raw == null) {
            return false;
        }
        for (String name : raw.split(",")) {
            if (name.trim().equalsIgnoreCase(groupName)) {
                return true;
            }
        }
        return false;
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
