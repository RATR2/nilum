package io.github.r4t2.nilum.neoforge.hud;

import io.github.r4t2.nilum.common.expr.TextValueSource;
import io.github.r4t2.nilum.common.expr.ValueSource;
import io.github.r4t2.nilum.common.hud.HudAtlasElement;
import io.github.r4t2.nilum.common.logging.NilumLogger;
import io.github.r4t2.nilum.neoforge.font.ClientFontStore;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.neoforged.neoforge.client.gui.GuiLayer;

import java.util.Optional;

/** Draws every loaded HUD atlas's elements at their configured screen position, every frame. */
public final class HudAtlasRenderer implements GuiLayer {

    /** Vanilla's own default drop-shadowed white; matches ordinary in-game text. */
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    /** Safety backstop against a runaway count expression, not a design limit. */
    private static final int MAX_DUPLICATE_COUNT = 256;

    private final ClientHudAtlasStore atlases;
    private final ClientVarStore clientVars;
    private final ClientFontStore fontStore;
    private final NilumLogger logger;

    public HudAtlasRenderer(ClientHudAtlasStore atlases, ClientVarStore clientVars, ClientFontStore fontStore, NilumLogger logger) {
        this.atlases = atlases;
        this.clientVars = clientVars;
        this.fontStore = fontStore;
        this.logger = logger;
    }

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        ValueSource valueSource = new NilumHudValueSource(player, clientVars, logger);
        TextValueSource textSource = new NilumHudTextValueSource(player, logger);
        double timeSeconds = System.nanoTime() / 1_000_000_000.0;

        for (HudAtlas atlas : atlases.all()) {
            if (!atlases.isAtlasVisible(atlas.atlasId())) {
                continue;
            }
            for (var entry : atlas.descriptor().elements().entrySet()) {
                String elementId = entry.getKey();
                if (!atlas.isElementVisible(elementId)) {
                    continue;
                }

                switch (entry.getValue()) {
                    case HudAtlasElement.Sprite sprite -> atlas.textureFor(Optional.empty()).ifPresent(tex -> {
                        int frame = atlas.currentFrame(elementId, valueSource, timeSeconds);
                        int drawWidth = sprite.screenWidth().map(v -> v.resolve(guiGraphics.guiWidth())).orElse(sprite.frameWidth());
                        int drawHeight = sprite.screenHeight().map(v -> v.resolve(guiGraphics.guiHeight())).orElse(sprite.frameHeight());
                        int x = sprite.anchor().resolveX(sprite.screenX(), drawWidth, guiGraphics.guiWidth());
                        int y = sprite.anchor().resolveY(sprite.screenY(), drawHeight, guiGraphics.guiHeight());
                        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, tex.id(),
                                x, y,
                                sprite.frameOriginX(frame), sprite.frameOriginY(frame),
                                drawWidth, drawHeight,
                                sprite.frameWidth(), sprite.frameHeight(),
                                tex.width(), tex.height());
                    });
                    case HudAtlasElement.Image image -> atlas.textureFor(Optional.of(image.textureFile())).ifPresent(tex -> {
                        int frame = atlas.currentFrame(elementId, valueSource, timeSeconds);
                        int drawWidth = image.screenWidth().map(v -> v.resolve(guiGraphics.guiWidth())).orElse(image.frameWidth());
                        int drawHeight = image.screenHeight().map(v -> v.resolve(guiGraphics.guiHeight())).orElse(image.frameHeight());
                        int x = image.anchor().resolveX(image.screenX(), drawWidth, guiGraphics.guiWidth());
                        int y = image.anchor().resolveY(image.screenY(), drawHeight, guiGraphics.guiHeight());
                        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, tex.id(),
                                x, y,
                                image.frameOriginX(frame), image.frameOriginY(frame),
                                drawWidth, drawHeight,
                                image.frameWidth(), image.frameHeight(),
                                tex.width(), tex.height());
                    });
                    case HudAtlasElement.Duplicate duplicate -> atlas.textureFor(Optional.empty()).ifPresent(tex -> {
                        int count = Math.max(0, Math.min(MAX_DUPLICATE_COUNT,
                                atlas.currentCount(elementId, valueSource, timeSeconds)));
                        int originX = duplicate.frameOriginX();
                        int originY = duplicate.frameOriginY();
                        int startX = duplicate.screenX().resolve(guiGraphics.guiWidth());
                        int startY = duplicate.screenY().resolve(guiGraphics.guiHeight());
                        for (int i = 0; i < count; i++) {
                            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, tex.id(),
                                    startX + i * duplicate.offsetX(), startY + i * duplicate.offsetY(),
                                    originX, originY, duplicate.frameWidth(), duplicate.frameHeight(),
                                    tex.width(), tex.height());
                        }
                    });
                    case HudAtlasElement.Text text -> {
                        String value = atlas.currentText(elementId, valueSource, textSource, timeSeconds);
                        Font font = resolveFont(text.font());
                        boolean hasHead = HudHeadText.containsHeadTag(value);
                        int width = hasHead ? font.width(HudHeadText.parse(value)) : font.width(value);
                        int x = text.anchor().resolveX(text.screenX(), width, guiGraphics.guiWidth());
                        int y = text.anchor().resolveY(text.screenY(), font.lineHeight, guiGraphics.guiHeight());
                        if (hasHead) {
                            guiGraphics.drawString(font, HudHeadText.parse(value), x, y, TEXT_COLOR);
                        } else {
                            guiGraphics.drawString(font, value, x, y, TEXT_COLOR);
                        }
                    }
                }
            }
        }
    }

    private Font resolveFont(String fontId) {
        if ("default".equals(fontId)) {
            return Minecraft.getInstance().font;
        }
        return fontStore.get(fontId).orElseGet(() -> Minecraft.getInstance().font);
    }
}
