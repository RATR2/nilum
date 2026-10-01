package io.github.r4t2.nilum.common.ui;

import java.util.Map;

/** A real-inventory chest UI: row count, title, custom background texture, and its interactive slots. */
public record ChestUiDescriptor(int rows, String title, String background, Map<String, ChestUiElement> elements) {
}
