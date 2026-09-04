package io.github.r4t2.nilum.common.ui;

/** How a UI's elements, positioned relative to each other as authored, get placed on screen. */
public enum UiAnchor {
    /** Element positions are used as literal absolute screen pixels, the current default. */
    TOP_LEFT,
    /** The bounding box of every element (by its own texture size) is centered on screen, elements keep their positions relative to each other. */
    CENTER
}
