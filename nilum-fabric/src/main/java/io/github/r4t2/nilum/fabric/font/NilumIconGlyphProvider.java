package io.github.r4t2.nilum.fabric.font;

import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.UnbakedGlyph;
import com.mojang.blaze3d.platform.NativeImage;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.jspecify.annotations.Nullable;

/** Exposes every registered font-provider icon as a glyph at its auto-assigned codepoint; backed live by ClientFontIconStore. */
public final class NilumIconGlyphProvider implements GlyphProvider {

    private static final float TARGET_HEIGHT = 8.0F;

    private final ClientFontIconStore store;

    public NilumIconGlyphProvider(ClientFontIconStore store) {
        this.store = store;
    }

    @Override
    public @Nullable UnbakedGlyph getGlyph(int codepoint) {
        NativeImage image = store.imageFor(codepoint).orElse(null);
        if (image == null) {
            return null;
        }
        float advance = image.getWidth() * (TARGET_HEIGHT / image.getHeight());
        return new NilumIconUnbakedGlyph(image, advance);
    }

    @Override
    public IntSet getSupportedGlyphs() {
        return new IntOpenHashSet(store.codepoints());
    }
}
