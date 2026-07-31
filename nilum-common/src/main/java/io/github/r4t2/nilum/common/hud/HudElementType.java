package io.github.r4t2.nilum.common.hud;

/** How a {@link HudAtlasElement}'s current frame is decided. */
public enum HudElementType {
    /** Server pushes HUD_FRAME/HUD_FRAME_OVERRIDE packets to drive the frame. */
    SERVER,
    /** Client evaluates {@link HudAtlasElement#clientConnector()} every render tick, zero packets. */
    AUTO,
    /** Frozen on a specific frame, never changes. */
    STATIC
}
