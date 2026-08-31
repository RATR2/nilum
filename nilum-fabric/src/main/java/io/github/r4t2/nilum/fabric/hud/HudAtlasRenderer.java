package io.github.r4t2.nilum.fabric.hud;

import io.github.r4t2.nilum.common.expr.TextValueSource;
import io.github.r4t2.nilum.common.expr.ValueSource;
import io.github.r4t2.nilum.common.hud.HudAtlasElement;
import io.github.r4t2.nilum.fabric.NilumFabricClient;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;

import java.util.Optional;

/** Draws every loaded HUD atlas's elements at their configured screen position, every frame. */
public final class HudAtlasRenderer implements HudElement {

    /** Vanilla's own default drop-shadowed white; matches ordinary in-game text. */
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    /** Safety backstop against a runaway count expression, not a design limit. */
    private static final int MAX_DUPLICATE_COUNT = 256;

    private final ClientHudAtlasStore atlases;
    private final ClientVarStore clientVars;

    public HudAtlasRenderer(ClientHudAtlasStore atlases, ClientVarStore clientVars) {
        this.atlases = atlases;
        this.clientVars = clientVars;
    }

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        ValueSource valueSource = new NilumHudValueSource(player, clientVars);
        TextValueSource textSource = new NilumHudTextValueSource(player);
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
                        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, tex.id(),
                                sprite.screenX(), sprite.screenY(),
                                sprite.frameOriginX(frame), sprite.frameOriginY(frame),
                                sprite.frameWidth(), sprite.frameHeight(),
                                tex.width(), tex.height());
                    });
                    case HudAtlasElement.Image image -> atlas.textureFor(Optional.of(image.textureFile())).ifPresent(tex -> {
                        int frame = atlas.currentFrame(elementId, valueSource, timeSeconds);
                        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, tex.id(),
                                image.screenX(), image.screenY(),
                                image.frameOriginX(frame), image.frameOriginY(frame),
                                image.frameWidth(), image.frameHeight(),
                                tex.width(), tex.height());
                    });
                    case HudAtlasElement.Duplicate duplicate -> atlas.textureFor(Optional.empty()).ifPresent(tex -> {
                        int count = Math.max(0, Math.min(MAX_DUPLICATE_COUNT,
                                atlas.currentCount(elementId, valueSource, timeSeconds)));
                        int originX = duplicate.frameOriginX();
                        int originY = duplicate.frameOriginY();
                        for (int i = 0; i < count; i++) {
                            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, tex.id(),
                                    duplicate.screenX() + i * duplicate.offsetX(), duplicate.screenY() + i * duplicate.offsetY(),
                                    originX, originY, duplicate.frameWidth(), duplicate.frameHeight(),
                                    tex.width(), tex.height());
                        }
                    });
                    case HudAtlasElement.Text text -> {
                        String value = atlas.currentText(elementId, valueSource, textSource, timeSeconds);
                        Font font = resolveFont(text.font());
                        if (HudHeadText.containsHeadTag(value)) {
                            guiGraphics.drawString(font, HudHeadText.parse(value), text.screenX(), text.screenY(), TEXT_COLOR);
                        } else {
                            guiGraphics.drawString(font, value, text.screenX(), text.screenY(), TEXT_COLOR);
                        }
                    }
                }
            }
        }
    }

    private static Font resolveFont(String fontId) {
        if ("default".equals(fontId)) {
            return Minecraft.getInstance().font;
        }
        return NilumFabricClient.FONT_STORE.get(fontId).orElseGet(() -> Minecraft.getInstance().font);
    }
}
