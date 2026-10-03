package io.github.r4t2.nilum.fabric.font;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.UnbakedGlyph;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;

final class NilumIconUnbakedGlyph implements UnbakedGlyph {

    private final NativeImage image;
    private final float advance;

    NilumIconUnbakedGlyph(NativeImage image, float advance) {
        this.image = image;
        this.advance = advance;
    }

    @Override
    public GlyphInfo info() {
        return GlyphInfo.simple(advance);
    }

    @Override
    public BakedGlyph bake(Stitcher stitcher) {
        return stitcher.stitch(info(), new NilumIconGlyphBitmap(image));
    }
}
