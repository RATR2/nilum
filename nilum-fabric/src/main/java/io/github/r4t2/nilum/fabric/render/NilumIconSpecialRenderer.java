package io.github.r4t2.nilum.fabric.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.function.Consumer;

/** Draws a Nilum icon-only item as a flat quad sampling its rect within the shared {@link IconAtlas}. */
public final class NilumIconSpecialRenderer implements SpecialModelRenderer<String> {

    // Every icon shares this exact quad shape (see submit() below) - only the UV rect differs per icon.
    public static final Vector3fc[] EXTENTS = {
            new Vector3f(0f, 0f, 0.5f), new Vector3f(1f, 0f, 0.5f),
            new Vector3f(0f, 1f, 0.5f), new Vector3f(1f, 1f, 0.5f),
    };

    private final IconAtlas iconAtlas;

    public NilumIconSpecialRenderer(IconAtlas iconAtlas) {
        this.iconAtlas = iconAtlas;
    }

    @Override
    public void submit(String iconId, ItemDisplayContext itemDisplayContext, PoseStack poseStack,
                        SubmitNodeCollector collector, int light, int overlay, boolean hasFoil, int seed) {
        if (iconId == null) {
            return;
        }

        IconAtlas.IconUv uv = iconAtlas.uvOf(iconId).orElse(null);
        if (uv == null) {
            return;
        }

        float u0 = (float) uv.x() / iconAtlas.width();
        float v0 = (float) uv.y() / iconAtlas.height();
        float u1 = (float) (uv.x() + uv.width()) / iconAtlas.width();
        float v1 = (float) (uv.y() + uv.height()) / iconAtlas.height();

        RenderType renderType = RenderTypes.entityCutoutNoCull(iconAtlas.textureId());

        // ItemTransform.apply() always ends with an unconditional pose.translate(-0.5, -0.5, -0.5) -
        // vanilla's own generated-item quads are built spanning 0..1 (not centered on 0,0,0) so that
        // shift re-centers them. Match that convention here so our own transform math lines up with it.
        collector.submitCustomGeometry(poseStack, renderType, (pose, vertexConsumer) -> {
            emitVertex(vertexConsumer, pose, 0f, 0f, u0, v1, light, overlay);
            emitVertex(vertexConsumer, pose, 1f, 0f, u1, v1, light, overlay);
            emitVertex(vertexConsumer, pose, 1f, 1f, u1, v0, light, overlay);
            emitVertex(vertexConsumer, pose, 0f, 1f, u0, v0, light, overlay);
        });
    }

    private static void emitVertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y,
                                    float u, float v, int light, int overlay) {
        consumer.addVertex(pose, x, y, 0.5f)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(pose, 0, 0, 1);
    }

    @Override
    public void getExtents(Consumer<Vector3fc> consumer) {
        for (Vector3fc corner : EXTENTS) {
            consumer.accept(corner);
        }
    }

    @Override
    public String extractArgument(ItemStack itemStack) {
        return null;
    }
}
