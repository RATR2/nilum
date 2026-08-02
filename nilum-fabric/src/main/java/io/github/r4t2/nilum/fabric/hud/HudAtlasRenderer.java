package io.github.r4t2.nilum.fabric.hud;

import io.github.r4t2.nilum.common.expr.TextValueSource;
import io.github.r4t2.nilum.common.expr.ValueSource;
import io.github.r4t2.nilum.common.hud.HudAtlasElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;

/** Draws every loaded HUD atlas's elements at their configured screen position, every frame. */
public final class HudAtlasRenderer implements HudElement {

    /** Vanilla's own default drop-shadowed white; matches ordinary in-game text. */
    private static final int TEXT_COLOR = 0xFFFFFFFF;

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
            for (var entry : atlas.descriptor().elements().entrySet()) {
                String elementId = entry.getKey();

                // Font selection (streamed custom .ttf) isn't wired in yet; vanilla's default
                // font is used for every render_text element regardless of its `font:` value.
                switch (entry.getValue()) {
                    case HudAtlasElement.Sprite sprite -> {
                        int frame = atlas.currentFrame(elementId, valueSource, timeSeconds);
                        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, atlas.textureId(),
                                sprite.screenX(), sprite.screenY(),
                                sprite.frameOriginX(frame), sprite.frameOriginY(frame),
                                sprite.frameWidth(), sprite.frameHeight(),
                                atlas.width(), atlas.height());
                    }
                    case HudAtlasElement.Text text -> {
                        String value = atlas.currentText(elementId, valueSource, textSource, timeSeconds);
                        if (HudHeadText.containsHeadTag(value)) {
                            guiGraphics.drawString(Minecraft.getInstance().font, HudHeadText.parse(value),
                                    text.screenX(), text.screenY(), TEXT_COLOR);
                        } else {
                            guiGraphics.drawString(Minecraft.getInstance().font, value, text.screenX(), text.screenY(), TEXT_COLOR);
                        }
                    }
                }
            }
        }
    }
}
