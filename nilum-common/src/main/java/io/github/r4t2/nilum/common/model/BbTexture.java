package io.github.r4t2.nilum.common.model;

/**
 * uvWidth/uvHeight are the resolution face UVs are authored against, which Blockbench lets
 * differ per-texture from the texture's own pixel size and the model's global "resolution."
 * Faces must be scaled by their own texture's uv size, not the model's.
 */
public record BbTexture(String id, String name, int width, int height, int uvWidth, int uvHeight, byte[] pngBytes) {
}
