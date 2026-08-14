package io.github.r4t2.nilum.fabric.font;

import com.mojang.blaze3d.font.GlyphProvider;
import io.github.r4t2.nilum.fabric.NilumFabricMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GlyphSource;
import net.minecraft.client.gui.font.FontOption;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.font.GlyphStitcher;
import net.minecraft.client.gui.font.glyphs.EffectGlyph;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Client-side registry of streamed custom fonts, built into real usable Font instances once their .ttf bytes arrive. */
public final class ClientFontStore {

    private final Map<String, Font> fontsById = new ConcurrentHashMap<>();

    public void install(String fontId, byte[] ttfBytes) {
        Minecraft.getInstance().execute(() -> {
            try {
                fontsById.put(fontId, build(fontId, ttfBytes));
            } catch (RuntimeException e) {
                NilumFabricMod.LOGGER.warn("Failed to load font '" + fontId + "': " + e);
            }
        });
    }

    public Optional<Font> get(String fontId) {
        return Optional.ofNullable(fontsById.get(fontId));
    }

    private static Font build(String fontId, byte[] ttfBytes) {
        GlyphProvider provider = NilumFontLoader.load(ttfBytes);
        Identifier fontSetId = Identifier.fromNamespaceAndPath("nilum", "font/" + fontId);
        GlyphStitcher stitcher = new GlyphStitcher(Minecraft.getInstance().getTextureManager(), fontSetId);
        FontSet fontSet = new FontSet(stitcher);
        fontSet.reload(List.of(new GlyphProvider.Conditional(provider, FontOption.Filter.ALWAYS_PASS)), Set.of());
        return new Font(new NilumFontProvider(fontSet));
    }

    /** Always resolves to this one font set. Inline object components (head-tag glyphs) won't resolve correctly through a custom font. */
    private record NilumFontProvider(FontSet fontSet) implements Font.Provider {
        @Override
        public GlyphSource glyphs(FontDescription description) {
            return fontSet.source(false);
        }

        @Override
        public EffectGlyph effect() {
            return fontSet.whiteGlyph();
        }
    }
}
