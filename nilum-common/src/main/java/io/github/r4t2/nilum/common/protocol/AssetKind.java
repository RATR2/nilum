package io.github.r4t2.nilum.common.protocol;

/** Which client-side pipeline an asset id's bytes should be routed into. */
public enum AssetKind {
    MODEL,
    ICON,
    HUD_ATLAS,
    SHADER_PACK,
    FONT,
    FONT_ICON,
    CUSTOM_UI,
    CHEST_UI
}
