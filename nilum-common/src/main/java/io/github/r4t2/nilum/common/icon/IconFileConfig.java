package io.github.r4t2.nilum.common.icon;

/** One icon's resolved icons/<id>.yml: which texture file it points at, plus its display transforms. */
public record IconFileConfig(String textureFileName, IconDisplay display) {
}
