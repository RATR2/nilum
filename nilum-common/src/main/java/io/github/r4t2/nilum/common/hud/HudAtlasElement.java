package io.github.r4t2.nilum.common.hud;

import java.util.Optional;

/**
 * One named region of a HUD atlas: either a spritesheet frame strip (Sprite) or a
 * dynamically-rendered piece of text (Text, type: "render_text").
 */
public sealed interface HudAtlasElement {

    int screenX();

    int screenY();

    /** A horizontal or vertical strip of frameCount equally-sized frames starting at originX,originY. */
    record Sprite(
            int originX,
            int originY,
            int frameWidth,
            int frameHeight,
            int frameCount,
            HudElementLayout layout,
            HudElementType type,
            Optional<String> clientConnector,
            int staticFrame,
            int screenX,
            int screenY
    ) implements HudAtlasElement {

        /** Pixel rect within the atlas spritesheet for one frame index, clamped to frameCount. */
        public int frameOriginX(int frame) {
            int clamped = Math.floorMod(frame, Math.max(1, frameCount));
            return layout == HudElementLayout.HORIZONTAL ? originX + clamped * frameWidth : originX;
        }

        public int frameOriginY(int frame) {
            int clamped = Math.floorMod(frame, Math.max(1, frameCount));
            return layout == HudElementLayout.VERTICAL ? originY + clamped * frameHeight : originY;
        }
    }

    /**
     * Screen text driven by the expression language instead of a spritesheet frame. Always has
     * at least one of clientConnector/serverConnector. serverConnector is parsed but not yet
     * acted on (PlaceholderAPI integration still to come).
     */
    record Text(
            String font,
            Optional<String> clientConnector,
            Optional<String> serverConnector,
            int screenX,
            int screenY
    ) implements HudAtlasElement {
    }
}
