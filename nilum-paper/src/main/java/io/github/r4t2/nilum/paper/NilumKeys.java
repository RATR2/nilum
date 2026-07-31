package io.github.r4t2.nilum.paper;

import org.bukkit.NamespacedKey;

/** Shared PersistentDataContainer keys, so the same tag name isn't duplicated across placements and items. */
public final class NilumKeys {

    public static final NamespacedKey MODEL_ID = new NamespacedKey("nilum", "model_id");
    public static final NamespacedKey ICON_ID = new NamespacedKey("nilum", "icon_id");

    private NilumKeys() {
    }
}
