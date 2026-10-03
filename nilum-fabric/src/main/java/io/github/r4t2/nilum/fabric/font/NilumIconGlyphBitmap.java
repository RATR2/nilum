package io.github.r4t2.nilum.fabric.font;

import com.mojang.blaze3d.font.GlyphBitmap;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;

/** A whole streamed PNG as one glyph cell, mirroring vanilla's own BitmapProvider.Glyph upload path. */
final class NilumIconGlyphBitmap implements GlyphBitmap {

    private final NativeImage image;

    NilumIconGlyphBitmap(NativeImage image) {
        this.image = image;
    }

    @Override
    public int getPixelWidth() {
        return image.getWidth();
    }

    @Override
    public int getPixelHeight() {
        return image.getHeight();
    }

    @Override
    public void upload(int x, int y, GpuTexture texture) {
        RenderSystem.getDevice().createCommandEncoder()
                .writeToTexture(texture, image, 0, 0, x, y, image.getWidth(), image.getHeight(), 0, 0);
    }

    @Override
    public boolean isColored() {
        return true;
    }

    @Override
    public float getOversample() {
        return 1.0F;
    }
}
