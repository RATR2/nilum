package io.github.r4t2.nilum.paper.icon;

import io.github.r4t2.nilum.common.icon.IconDisplay;

/** One icon's resolved icons/<id>.yml: which texture file it points at, plus its display transforms. */
public record IconYamlConfig(String textureFileName, IconDisplay display) {
}
