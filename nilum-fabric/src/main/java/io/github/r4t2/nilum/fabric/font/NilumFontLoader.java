package io.github.r4t2.nilum.fabric.font;

import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.TrueTypeGlyphProvider;
import com.mojang.blaze3d.platform.TextureUtil;
import net.minecraft.client.gui.font.providers.FreeTypeUtil;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FreeType;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;

/** Loads a raw .ttf byte array into a real GlyphProvider, mirroring TrueTypeGlyphProviderDefinition's FreeType sequence. */
public final class NilumFontLoader {

    private static final float DEFAULT_SIZE = 11.0F;
    private static final float DEFAULT_OVERSAMPLE = 1.0F;

    private NilumFontLoader() {
    }

    public static GlyphProvider load(byte[] ttfBytes) {
        ByteBuffer buffer;
        try {
            buffer = TextureUtil.readResource(new ByteArrayInputStream(ttfBytes));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        synchronized (FreeTypeUtil.LIBRARY_LOCK) {
            FT_Face face = null;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer pointerBuffer = stack.mallocPointer(1);
                FreeTypeUtil.assertError(FreeType.FT_New_Memory_Face(FreeTypeUtil.getLibrary(), buffer, 0L, pointerBuffer),
                        "Initializing font face");
                face = FT_Face.create(pointerBuffer.get());

                String format = FreeType.FT_Get_Font_Format(face);
                if (!"TrueType".equals(format)) {
                    throw new IllegalArgumentException("Font is not in TTF format, was " + format);
                }

                FreeTypeUtil.assertError(FreeType.FT_Select_Charmap(face, FreeType.FT_ENCODING_UNICODE), "Find unicode charmap");
                return new TrueTypeGlyphProvider(buffer, face, DEFAULT_SIZE, DEFAULT_OVERSAMPLE, 0, 0, "");
            } catch (RuntimeException e) {
                if (face != null) {
                    FreeType.FT_Done_Face(face);
                }
                throw e;
            }
        }
    }
}
