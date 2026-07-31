package io.github.r4t2.nilum.fabric.hud;

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
        double timeSeconds = System.nanoTime() / 1_000_000_000.0;

        for (HudAtlas atlas : atlases.all()) {
            for (var entry : atlas.descriptor().elements().entrySet()) {
                String elementId = entry.getKey();
                HudAtlasElement element = entry.getValue();
                int frame = atlas.currentFrame(elementId, valueSource, timeSeconds);

                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, atlas.textureId(),
                        element.screenX(), element.screenY(),
                        element.frameOriginX(frame), element.frameOriginY(frame),
                        element.frameWidth(), element.frameHeight(),
                        atlas.width(), atlas.height());
            }
        }
    }
}
