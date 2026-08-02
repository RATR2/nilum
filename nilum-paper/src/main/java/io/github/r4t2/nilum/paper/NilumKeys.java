package io.github.r4t2.nilum.paper;

import org.bukkit.NamespacedKey;

/** Shared PersistentDataContainer keys, so the same tag name isn't duplicated across placements and items. */
public final class NilumKeys {

    public static final NamespacedKey MODEL_ID = new NamespacedKey("nilum", "model_id");
    public static final NamespacedKey ICON_ID = new NamespacedKey("nilum", "icon_id");
    public static final NamespacedKey GLINT_COLOR = new NamespacedKey("nilum", "glint_color");
    public static final NamespacedKey GLINT_INTENSITY = new NamespacedKey("nilum", "glint_intensity");
    public static final NamespacedKey GLINT_SPEED = new NamespacedKey("nilum", "glint_speed");
    public static final NamespacedKey GLINT_TEXTURE = new NamespacedKey("nilum", "glint_texture");

    private NilumKeys() {
    }
}
