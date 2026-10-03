package io.github.r4t2.nilum.common.ui;

import io.github.r4t2.nilum.common.util.ScreenValue;

import java.util.Optional;

/** One named layer of a custom UI: a static image, a clickable button, or a text label. */
public sealed interface UiElement {

    ScreenValue x();

    ScreenValue y();

    int layer();

    /** Visibility condition in Nilum's expression language, evaluated with skriptvar(...) support; visible when absent. */
    Optional<String> requirement();

    /** width/height override the image's native texture size when present; drawn at native size otherwise. */
    record Image(String imageFile, ScreenValue x, ScreenValue y, int layer, Optional<String> requirement,
                 Optional<ScreenValue> width, Optional<ScreenValue> height) implements UiElement {
    }

    /** action is a single Skript effect line, run against the clicking player when present. */
    record Button(String imageFile, String pressedImageFile, ScreenValue x, ScreenValue y, int layer,
                  Optional<String> requirement, Optional<String> action,
                  Optional<ScreenValue> width, Optional<ScreenValue> height) implements UiElement {
    }

    /**
     * Exactly one of text/clientConnector/serverConnector is present. text is a literal string.
     * clientConnector is evaluated once when the UI opens, not live-updated afterward.
     * serverConnector is evaluated server-side (skriptvar/placeholderapi/java, same as HUD
     * render_text) and live-pushed to the client whenever its value changes while the UI is open.
     */
    record Text(String font, Optional<String> text, Optional<String> clientConnector, Optional<String> serverConnector,
                int color, ScreenValue x, ScreenValue y, int layer, Optional<String> requirement) implements UiElement {
    }

    /**
     * player is a literal UUID or username, not an expression; renders at the same size as a text
     * glyph, via the same <head:...> mechanism inline text heads use.
     */
    record Head(String player, ScreenValue x, ScreenValue y, int layer, Optional<String> requirement) implements UiElement {
    }
}
