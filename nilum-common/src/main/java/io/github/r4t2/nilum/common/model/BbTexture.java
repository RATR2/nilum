package io.github.r4t2.nilum.common.model;

/** uvWidth/uvHeight are the resolution face UVs are authored against; faces scale by their own texture's uv size, not the model's. */
public record BbTexture(String id, String name, int width, int height, int uvWidth, int uvHeight, byte[] pngBytes) {
}
