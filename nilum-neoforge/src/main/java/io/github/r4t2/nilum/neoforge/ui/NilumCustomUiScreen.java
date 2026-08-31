package io.github.r4t2.nilum.neoforge.ui;

import io.github.r4t2.nilum.common.protocol.UiClosedPacket;
import io.github.r4t2.nilum.common.ui.UiDescriptor;
import io.github.r4t2.nilum.common.ui.UiElement;
import io.github.r4t2.nilum.neoforge.network.NilumUiClosedPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Renders a Custom UI's layers by z-order. Buttons don't respond to clicks or requirement conditions yet, layout only. */
public final class NilumCustomUiScreen extends Screen {

    private record RenderableLayer(int x, int y, int layer, CustomUi.TextureRef texture) {
    }

    private final String uiId;
    private final CustomUi customUi;
    private final List<RenderableLayer> layers = new ArrayList<>();

    public NilumCustomUiScreen(String uiId, CustomUi customUi) {
        super(Component.literal("Nilum UI: " + uiId));
        this.uiId = uiId;
        this.customUi = customUi;
    }

    @Override
    protected void init() {
        layers.clear();
        UiDescriptor descriptor = customUi.descriptor();
        for (Map.Entry<String, UiElement> entry : descriptor.elements().entrySet()) {
            UiElement element = entry.getValue();
            String imageFile = switch (element) {
                case UiElement.Image image -> image.imageFile();
                case UiElement.Button button -> button.imageFile();
            };
            customUi.textureFor(imageFile).ifPresent(texture ->
                    layers.add(new RenderableLayer(element.x(), element.y(), element.layer(), texture)));
        }
        layers.sort(Comparator.comparingInt(RenderableLayer::layer));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        for (RenderableLayer layer : layers) {
            CustomUi.TextureRef texture = layer.texture();
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture.id(), layer.x(), layer.y(),
                    0, 0, texture.width(), texture.height(), texture.width(), texture.height());
        }
    }

    @Override
    public void onClose() {
        super.onClose();
        ClientPacketDistributor.sendToServer(new NilumUiClosedPayload(new UiClosedPacket(uiId).encode()));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
