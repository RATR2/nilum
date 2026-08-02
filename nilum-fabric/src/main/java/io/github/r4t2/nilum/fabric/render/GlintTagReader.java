package io.github.r4t2.nilum.fabric.render;

import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Shared by NilumIconItemModel and NilumModelItemModel to build one item's glint data. */
final class GlintTagReader {

    private GlintTagReader() {
    }

    static GlintRenderData read(ItemStack itemStack, List<GlintQuad> quads) {
        String colorHex = NilumItemTags.get(itemStack, "nilum:glint_color");
        if (colorHex == null) {
            return null;
        }

        String textureId = NilumItemTags.get(itemStack, "nilum:glint_texture");
        String intensityStr = NilumItemTags.get(itemStack, "nilum:glint_intensity");
        String speedStr = NilumItemTags.get(itemStack, "nilum:glint_speed");
        try {
            int colorRgb = Integer.parseInt(colorHex, 16);
            float intensity = intensityStr != null ? Float.parseFloat(intensityStr) : 1f;
            float speed = speedStr != null ? Float.parseFloat(speedStr) : 1f;
            return new GlintRenderData(colorRgb, intensity, speed, textureId, quads);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
