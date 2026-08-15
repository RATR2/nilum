package io.github.r4t2.nilum.neoforge.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.r4t2.nilum.common.asset.ClientModelStore;
import io.github.r4t2.nilum.common.model.BbBakedQuad;
import io.github.r4t2.nilum.common.model.BbBakedVertex;
import io.github.r4t2.nilum.common.model.BbMatrix4;
import io.github.r4t2.nilum.common.model.BbModel;
import io.github.r4t2.nilum.common.model.BbPosedModel;
import io.github.r4t2.nilum.neoforge.render.NilumModelGeometry;
import io.github.r4t2.nilum.neoforge.render.TextureUploader;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.List;
import java.util.Map;

/** Draws every visible Nilum block's real geometry as its own immediate-mode pass. Not chunk-batched, does its own culling and per-quad lighting. */
public final class NilumBlockRenderer {

    private static final double MAX_RENDER_DISTANCE = 64.0;
    private static final double MAX_RENDER_DISTANCE_SQ = MAX_RENDER_DISTANCE * MAX_RENDER_DISTANCE;

    private final ClientModelStore modelStore;
    private final ClientBlockRegistry blockRegistry;
    private final TextureUploader textureUploader;

    public NilumBlockRenderer(ClientModelStore modelStore, ClientBlockRegistry blockRegistry, TextureUploader textureUploader) {
        this.modelStore = modelStore;
        this.blockRegistry = blockRegistry;
        this.textureUploader = textureUploader;
    }

    public void render(RenderLevelStageEvent.AfterEntities event) {
        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        if (level == null || blockRegistry.entries().isEmpty()) {
            return;
        }

        Camera camera = client.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.position();
        Matrix4f viewMatrix = new Matrix4f().rotation(camera.rotation().conjugate(new Quaternionf()));
        float fov = client.options.fov().get() + 10.0f;
        Matrix4f projectionMatrix = client.gameRenderer.getProjectionMatrix(fov);
        Frustum frustum = new Frustum(viewMatrix, projectionMatrix);
        frustum.prepare(cameraPos.x, cameraPos.y, cameraPos.z);

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = client.renderBuffers().bufferSource();

        for (Map.Entry<BlockPos, String> entry : blockRegistry.entries().entrySet()) {
            BlockPos pos = entry.getKey();

            double dx = pos.getX() + 0.5 - cameraPos.x;
            double dy = pos.getY() + 0.5 - cameraPos.y;
            double dz = pos.getZ() + 0.5 - cameraPos.z;
            if (dx * dx + dy * dy + dz * dz > MAX_RENDER_DISTANCE_SQ) {
                continue;
            }

            if (!level.isLoaded(pos)) {
                continue;
            }

            AABB bounds = new AABB(pos);
            if (!frustum.isVisible(bounds)) {
                continue;
            }

            renderBlock(level, pos, entry.getValue(), poseStack, bufferSource, cameraPos);
        }

        bufferSource.endBatch();
    }

    private void renderBlock(ClientLevel level, BlockPos pos, String modelId, PoseStack poseStack,
                              MultiBufferSource.BufferSource bufferSource, Vec3 cameraPos) {
        BbModel model = modelStore.model(modelId).orElse(null);
        Map<String, List<BbBakedQuad>> quadsByBone = modelStore.bakedQuadsByBone(modelId).orElse(null);
        if (model == null || quadsByBone == null) {
            return;
        }

        Map<String, BbMatrix4> bonePose = blockRegistry.animationState(pos).pose(model, System.currentTimeMillis());
        List<BbBakedQuad> posed = BbPosedModel.apply(quadsByBone, bonePose);
        Map<Integer, List<BbBakedQuad>> quadsByTexture = NilumModelGeometry.groupByTexture(posed);

        poseStack.pushPose();
        poseStack.translate(pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z);
        PoseStack.Pose pose = poseStack.last();

        for (Map.Entry<Integer, List<BbBakedQuad>> texEntry : quadsByTexture.entrySet()) {
            int textureIndex = texEntry.getKey();
            if (textureIndex >= model.textures().size()) {
                continue;
            }

            Identifier textureId = textureUploader.getOrUpload(modelId, textureIndex, model);
            RenderType renderType = RenderTypes.entityCutoutNoCull(textureId);
            VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);

            for (BbBakedQuad quad : texEntry.getValue()) {
                Direction face = axisAlignedDirection(quad.v0());
                if (face != null && isFlushWithBoundary(quad, face) && level.getBlockState(pos.relative(face)).isSolidRender()) {
                    continue;
                }
                // Light comes from whatever this face is actually exposed to: the block
                // it's touching in that direction, not one flat value for the whole model.
                // Non-axis-aligned/non-flush faces fall back to the block's own position.
                BlockPos lightPos = face != null ? pos.relative(face) : pos;
                int light = LevelRenderer.getLightColor(level, lightPos);
                NilumModelGeometry.emitQuad(vertexConsumer, pose, quad, light, OverlayTexture.NO_OVERLAY);
            }
        }

        poseStack.popPose();
    }

    /** True only if every vertex of this face sits on the block's boundary plane in that direction, not just facing that way. */
    private static boolean isFlushWithBoundary(BbBakedQuad quad, Direction face) {
        float epsilon = 0.01f;
        for (BbBakedVertex vertex : new BbBakedVertex[]{quad.v0(), quad.v1(), quad.v2(), quad.v3()}) {
            boolean flush = switch (face) {
                case UP -> Math.abs(vertex.y() - 1f) < epsilon;
                case DOWN -> Math.abs(vertex.y()) < epsilon;
                case EAST -> Math.abs(vertex.x() - 1f) < epsilon;
                case WEST -> Math.abs(vertex.x()) < epsilon;
                case SOUTH -> Math.abs(vertex.z() - 1f) < epsilon;
                case NORTH -> Math.abs(vertex.z()) < epsilon;
            };
            if (!flush) {
                return false;
            }
        }
        return true;
    }

    private static Direction axisAlignedDirection(BbBakedVertex vertex) {
        float ax = Math.abs(vertex.nx());
        float ay = Math.abs(vertex.ny());
        float az = Math.abs(vertex.nz());
        float threshold = 0.99f;

        if (ax > threshold) {
            return vertex.nx() > 0 ? Direction.EAST : Direction.WEST;
        }
        if (ay > threshold) {
            return vertex.ny() > 0 ? Direction.UP : Direction.DOWN;
        }
        if (az > threshold) {
            return vertex.nz() > 0 ? Direction.SOUTH : Direction.NORTH;
        }
        return null;
    }
}
