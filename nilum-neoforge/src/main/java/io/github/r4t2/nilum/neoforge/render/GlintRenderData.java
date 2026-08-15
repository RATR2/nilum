package io.github.r4t2.nilum.neoforge.render;

import java.util.List;

/**
 * Everything NilumGlintSpecialRenderer needs to draw one item's custom glint.
 *
 * @param textureIconId an icon-atlas id for a custom glint texture, or null to use Minecraft's
 *                       enchanted_glint_item.png recolored via colorRgb/intensity
 * @param quads          the base layer's own geometry, so the glint is masked to the item's
 *                        actual shape and tracks its real transform
 */
record GlintRenderData(int colorRgb, float intensity, float speed, String textureIconId, List<GlintQuad> quads) {
}
