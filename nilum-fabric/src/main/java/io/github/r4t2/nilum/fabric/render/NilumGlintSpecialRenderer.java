package io.github.r4t2.nilum.fabric.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3fc;

import java.util.function.Consumer;

/**
 * Draws a per-item custom glint: the base layer's quads redrawn with a scrolled UV sampling a
 * glint texture, additively blended; "suppress and replace," not a modification of vanilla's
 * glint shaders (Sodium crashes if those are touched). Scroll is computed per-vertex on the
 * CPU, so this works identically on vanilla, Sodium, and Iris.
 */
public final class NilumGlintSpecialRenderer implements SpecialModelRenderer<GlintRenderData> {

    private static final Identifier VANILLA_GLINT_TEXTURE =
            Identifier.fromNamespaceAndPath("minecraft", "textures/misc/enchanted_glint_item.png");

    /** eyes()'s TRANSLUCENT blend makes alpha == opacity; keep well under 255 so the base texture never fully disappears. */
    private static final int MAX_ALPHA = 110;
    private static final double SCROLL_SCALE = 0.15;

    private final IconAtlas iconAtlas;

    public NilumGlintSpecialRenderer(IconAtlas iconAtlas) {
        this.iconAtlas = iconAtlas;
    }

    @Override
    public void submit(GlintRenderData data, ItemDisplayContext itemDisplayContext, PoseStack poseStack,
                        SubmitNodeCollector collector, int light, int overlay, boolean hasFoil, int seed) {
        if (data == null || data.quads().isEmpty()) {
            return;
        }

        Identifier textureId;
        if (data.textureIconId() == null) {
            textureId = VANILLA_GLINT_TEXTURE;
        } else {
            if (iconAtlas.uvOf(data.textureIconId()).isEmpty()) {
                return;
            }
            textureId = iconAtlas.textureId();
        }

        // eyes() blends via BlendFunction.TRANSLUCENT (plain alpha blending), not vanilla's real
        // BlendFunction.GLINT, so alpha directly controls opacity, and full alpha means fully
        // replacing whatever's underneath rather than shimmering over it. Cap it well below 255
        // so the base texture always stays visible; replicating GLINT's exact blend would mean
        // building a custom RenderPipeline from vanilla's private internals.
        double seconds = System.currentTimeMillis() / 1000.0;
        float offset = (float) ((seconds * data.speed() * SCROLL_SCALE) % 1.0);

        int r = (data.colorRgb() >> 16) & 0xFF;
        int g = (data.colorRgb() >> 8) & 0xFF;
        int b = data.colorRgb() & 0xFF;
        int alpha = Math.round(MAX_ALPHA * Math.min(1f, Math.max(0f, data.intensity())));

        RenderType renderType = RenderTypes.eyes(textureId);

        collector.submitCustomGeometry(poseStack, renderType, (pose, vertexConsumer) -> {
            for (GlintQuad quad : data.quads()) {
                emitVertex(vertexConsumer, pose, quad.v0(), offset, r, g, b, alpha, light, overlay);
                emitVertex(vertexConsumer, pose, quad.v1(), offset, r, g, b, alpha, light, overlay);
                emitVertex(vertexConsumer, pose, quad.v2(), offset, r, g, b, alpha, light, overlay);
                emitVertex(vertexConsumer, pose, quad.v3(), offset, r, g, b, alpha, light, overlay);
            }
        });
    }

    private static void emitVertex(VertexConsumer consumer, PoseStack.Pose pose, GlintQuad.GlintVertex vertex,
                                    float offset, int r, int g, int b, int a, int light, int overlay) {
        // Deliberately NOT wrapped into [0,1) here; vanilla scrolls its glint by transforming the
        // whole mesh's UV uniformly and lets the GPU's own texture wrapping handle the tiling.
        // Wrapping each vertex independently on the CPU instead made different quads/vertices
        // cross the 1.0 boundary at different moments (they don't share a base UV), so parts of
        // the mesh would reset out of sync with each other, looking like the shimmer randomly
        // reversing. Letting raw values pass through and relying on the texture's native
        // REPEAT addressing keeps the whole mesh scrolling together, smoothly.
        consumer.addVertex(pose, vertex.x(), vertex.y(), vertex.z())
                .setColor(r, g, b, a)
                .setUv(vertex.u() + offset, vertex.v() + offset)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(pose, 0, 0, 1);
    }

    @Override
    public void getExtents(Consumer<Vector3fc> consumer) {
        // Real per-layer extents come from LayerRenderState.setExtents(...), set by whichever
        // wrapper (icon/model) builds the glint quads, same pattern as
        // NilumModelItemSpecialRenderer for the same reason (no per-argument extents hook exists).
    }

    @Override
    public GlintRenderData extractArgument(ItemStack itemStack) {
        return null;
    }
}
