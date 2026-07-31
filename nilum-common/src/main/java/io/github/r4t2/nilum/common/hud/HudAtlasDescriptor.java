package io.github.r4t2.nilum.common.hud;

import java.util.Map;

public record HudAtlasDescriptor(int atlasWidth, int atlasHeight, Map<String, HudAtlasElement> elements) {
}
