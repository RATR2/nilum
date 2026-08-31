package io.github.r4t2.nilum.common.ui;

import java.util.Optional;

/** One named layer of a custom UI: a static image, or a clickable button. */
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
}
