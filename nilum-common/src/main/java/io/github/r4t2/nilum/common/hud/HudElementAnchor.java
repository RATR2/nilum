package io.github.r4t2.nilum.common.hud;

import io.github.r4t2.nilum.common.util.ScreenValue;

import java.util.Locale;

/** Which corner/edge/center of the screen a HUD element's screen_position is measured from. */
public enum HudElementAnchor {
    TOP_LEFT, TOP_CENTER, TOP_RIGHT,
    MIDDLE_LEFT, MIDDLE_CENTER, MIDDLE_RIGHT,
    BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT;

    private boolean isRight() {
        return this == TOP_RIGHT || this == MIDDLE_RIGHT || this == BOTTOM_RIGHT;
    }

    private boolean isHorizontalCenter() {
        return this == TOP_CENTER || this == MIDDLE_CENTER || this == BOTTOM_CENTER;
    }

    private boolean isBottom() {
        return this == BOTTOM_LEFT || this == BOTTOM_CENTER || this == BOTTOM_RIGHT;
    }

    private boolean isVerticalCenter() {
        return this == MIDDLE_LEFT || this == MIDDLE_CENTER || this == MIDDLE_RIGHT;
    }

    /** Resolves this element's actual draw-space x given its rendered width and the current screen width. */
    public int resolveX(ScreenValue offsetX, int elementWidth, int screenWidth) {
        int offset = offsetX.resolve(screenWidth);
        if (isRight()) {
            return screenWidth - elementWidth - offset;
        }
        if (isHorizontalCenter()) {
            return (screenWidth - elementWidth) / 2 + offset;
        }
        return offset;
    }

    /** Resolves this element's actual draw-space y given its rendered height and the current screen height. */
    public int resolveY(ScreenValue offsetY, int elementHeight, int screenHeight) {
        int offset = offsetY.resolve(screenHeight);
        if (isBottom()) {
            return screenHeight - elementHeight - offset;
        }
        if (isVerticalCenter()) {
            return (screenHeight - elementHeight) / 2 + offset;
        }
        return offset;
    }

    public static HudElementAnchor parse(String raw) {
        return switch (raw.toLowerCase(Locale.ROOT)) {
            case "top-left" -> TOP_LEFT;
            case "top-center" -> TOP_CENTER;
            case "top-right" -> TOP_RIGHT;
            case "middle-left" -> MIDDLE_LEFT;
            case "middle-center" -> MIDDLE_CENTER;
            case "middle-right" -> MIDDLE_RIGHT;
            case "bottom-left" -> BOTTOM_LEFT;
            case "bottom-center" -> BOTTOM_CENTER;
            case "bottom-right" -> BOTTOM_RIGHT;
            default -> throw new HudAtlasParseException("Unknown anchor '" + raw + "'");
        };
    }
}
