package io.github.r4t2.nilum.common.hud;

import java.util.Optional;

/**
 * One named region of a HUD atlas spritesheet - a horizontal or vertical strip of
 * {@code frameCount} equally-sized frames starting at {@code originX,originY}.
 *
 * <p>{@code screenX,screenY} is Nilum's own addition, not part of the design doc's original
 * {@code .atlas} example - rendering a HUD element has to be anchored somewhere on screen, and
 * the doc's format never specified where. Chosen as the simplest reasonable field rather than
 * leaving it unresolved.
 */
public record HudAtlasElement(
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
) {

    /** Pixel rect within the atlas spritesheet for one frame index, clamped to {@link #frameCount()}. */
    public int frameOriginX(int frame) {
        int clamped = Math.floorMod(frame, Math.max(1, frameCount));
        return layout == HudElementLayout.HORIZONTAL ? originX + clamped * frameWidth : originX;
    }

    public int frameOriginY(int frame) {
        int clamped = Math.floorMod(frame, Math.max(1, frameCount));
        return layout == HudElementLayout.VERTICAL ? originY + clamped * frameHeight : originY;
    }
}
