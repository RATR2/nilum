package io.github.r4t2.nilum.paper.item;

/**
 * A custom per-item enchant glint: "suppress and replace," never a modification of vanilla's
 * glint shaders.
 *
 * @param colorRgb      packed 0xRRGGBB
 * @param textureIconId an already-loaded icon id, reused as the glint's shimmer texture, or null
 *                       to use Minecraft's enchanted_glint_item.png recolored via colorRgb
 */
public record GlintDefinition(int colorRgb, float intensity, float speed, String textureIconId) {

    public static int parseColor(String hex) {
        String value = hex.startsWith("#") ? hex.substring(1) : hex;
        return Integer.parseInt(value, 16);
    }
}
