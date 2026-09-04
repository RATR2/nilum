package io.github.r4t2.nilum.common.ui;

import java.util.Map;

public record UiDescriptor(UiAnchor anchor, Map<String, UiElement> elements) {
}
