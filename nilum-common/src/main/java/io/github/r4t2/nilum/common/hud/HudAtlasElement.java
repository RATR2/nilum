package io.github.r4t2.nilum.common.hud;

import io.github.r4t2.nilum.common.util.ScreenValue;

import java.util.Optional;

/** One named region of a HUD atlas: a spritesheet frame strip (Sprite), its own texture (Image), or rendered text (Text). */
public sealed interface HudAtlasElement {

    ScreenValue screenX();

    ScreenValue screenY();

    /**
     * Which screen corner/edge/center screenX/screenY are measured from. Defaults to top-left
     * (screenX/screenY as literal pixels, matching this format's original behavior) for every
     * type except Duplicate, which always stays top-left: its bounding box grows with a
     * server-driven count, so anchoring it from the right/bottom/center would shift the whole
     * strip every time that count changes.
     */
    default HudElementAnchor anchor() {
        return HudElementAnchor.TOP_LEFT;
    }

    /** Pixel rect within a frame strip for one frame index, clamped to frameCount. Shared by Sprite/Image/Duplicate. */
    static int frameOrigin(HudElementLayout activeLayout, HudElementLayout layout, int origin, int frameSize, int frameCount, int frame) {
        int clamped = Math.floorMod(frame, Math.max(1, frameCount));
        return layout == activeLayout ? origin + clamped * frameSize : origin;
    }

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
            ScreenValue screenX,
            ScreenValue screenY,
            HudElementAnchor anchor,
            Optional<ScreenValue> screenWidth,
            Optional<ScreenValue> screenHeight
    ) implements HudAtlasElement {

        public int frameOriginX(int frame) {
            return frameOrigin(HudElementLayout.HORIZONTAL, layout, originX, frameWidth, frameCount, frame);
        }

        public int frameOriginY(int frame) {
            return frameOrigin(HudElementLayout.VERTICAL, layout, originY, frameHeight, frameCount, frame);
        }
    }

    /** Like Sprite, but its frame strip lives in its own dedicated PNG (textureFile) instead of the shared spritesheet. */
    record Image(
            String textureFile,
            int originX,
            int originY,
            int frameWidth,
            int frameHeight,
            int frameCount,
            HudElementLayout layout,
            HudElementType type,
            Optional<String> clientConnector,
            int staticFrame,
            ScreenValue screenX,
            ScreenValue screenY,
            HudElementAnchor anchor,
            Optional<ScreenValue> screenWidth,
            Optional<ScreenValue> screenHeight
    ) implements HudAtlasElement {

        public int frameOriginX(int frame) {
            return frameOrigin(HudElementLayout.HORIZONTAL, layout, originX, frameWidth, frameCount, frame);
        }

        public int frameOriginY(int frame) {
            return frameOrigin(HudElementLayout.VERTICAL, layout, originY, frameHeight, frameCount, frame);
        }
    }

    /**
     * Screen text driven by the expression language. With format, both connectors combine via
     * %client%/%server%; without it, a pushed server value wins over the client one.
     */
    record Text(
            String font,
            Optional<String> clientConnector,
            Optional<String> serverConnector,
            Optional<String> format,
            ScreenValue screenX,
            ScreenValue screenY,
            HudElementAnchor anchor
    ) implements HudAtlasElement {
    }

    /** Stamps one fixed frame repeatedly, offset by offsetX/offsetY per copy. count resolves like a Sprite's frame index. */
    record Duplicate(
            int originX,
            int originY,
            int frameWidth,
            int frameHeight,
            int frameCount,
            HudElementLayout layout,
            HudElementType type,
            Optional<String> clientConnector,
            int staticFrame,
            int imageFrame,
            int offsetX,
            int offsetY,
            ScreenValue screenX,
            ScreenValue screenY
    ) implements HudAtlasElement {

        public int frameOriginX() {
            return frameOrigin(HudElementLayout.HORIZONTAL, layout, originX, frameWidth, frameCount, imageFrame);
        }

        public int frameOriginY() {
            return frameOrigin(HudElementLayout.VERTICAL, layout, originY, frameHeight, frameCount, imageFrame);
        }
    }
}
