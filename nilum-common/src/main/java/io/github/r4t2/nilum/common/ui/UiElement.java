package io.github.r4t2.nilum.common.ui;

import java.util.Optional;

/** One named layer of a custom UI: a static image, a clickable button, or a text label. */
public sealed interface UiElement {

    int x();

    int y();

    int layer();

    /** Visibility condition in Nilum's expression language, evaluated with skriptvar(...) support; visible when absent. */
    Optional<String> requirement();

    record Image(String imageFile, int x, int y, int layer, Optional<String> requirement) implements UiElement {
    }

    /** action is a single Skript effect line, run against the clicking player when present. */
    record Button(String imageFile, String pressedImageFile, int x, int y, int layer,
                  Optional<String> requirement, Optional<String> action) implements UiElement {
    }

    /**
     * Either text or clientConnector is present, never both; a literal string or a Nilum
     * expression evaluated once when the UI opens, not live-updated afterward.
     */
    record Text(String font, Optional<String> text, Optional<String> clientConnector, int color,
                int x, int y, int layer, Optional<String> requirement) implements UiElement {
    }

    /**
     * player is a literal UUID or username, not an expression; renders at the same size as a text
     * glyph, via the same <head:...> mechanism inline text heads use.
     */
    record Head(String player, int x, int y, int layer, Optional<String> requirement) implements UiElement {
    }
}
