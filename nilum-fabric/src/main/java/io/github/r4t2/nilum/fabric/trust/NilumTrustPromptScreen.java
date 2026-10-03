package io.github.r4t2.nilum.fabric.trust;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * "This server wants to push custom content to your client" consent prompt, shown the first time
 * a server handshakes with a client that hasn't trusted it before. World/menu stays visible and
 * unpaused behind it, same as NilumHandTuneScreen.
 */
public final class NilumTrustPromptScreen extends Screen {

    private static final int BUTTON_WIDTH = 100;

    private final String serverAddress;
    private final Runnable onAllow;
    private final Runnable onDeny;
    private boolean decided;

    public NilumTrustPromptScreen(String serverAddress, Runnable onAllow, Runnable onDeny) {
        super(Component.literal("Nilum Trust Prompt"));
        this.serverAddress = serverAddress;
        this.onAllow = onAllow;
        this.onDeny = onDeny;
    }

    @Override
    protected void init() {
        int y = height / 2 + 20;
        addRenderableWidget(Button.builder(Component.literal("Allow"), b -> {
                    decided = true;
                    onAllow.run();
                    onClose();
                })
                .bounds(width / 2 - BUTTON_WIDTH - 5, y, BUTTON_WIDTH, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Deny"), b -> {
                    decided = true;
                    onDeny.run();
                    onClose();
                })
                .bounds(width / 2 + 5, y, BUTTON_WIDTH, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(Minecraft.getInstance().font,
                Component.literal("This server wants to push custom content (models, textures, HUD, shaders) to your client."),
                width / 2, height / 2 - 30, 0xFFFFFF);
        graphics.drawCenteredString(Minecraft.getInstance().font,
                Component.literal(serverAddress), width / 2, height / 2 - 15, 0xAAAAAA);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Deliberately no-op: keep the world/join screen visible behind the prompt.
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        super.onClose();
        // Closing without clicking either button (e.g. Escape) counts as Deny: no ack gets sent,
        // same as a player who never responds at all.
        if (!decided) {
            decided = true;
            onDeny.run();
        }
    }
}
