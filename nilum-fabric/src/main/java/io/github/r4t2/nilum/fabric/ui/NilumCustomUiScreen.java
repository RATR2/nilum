package io.github.r4t2.nilum.fabric.ui;

import io.github.r4t2.nilum.common.expr.ExprEvaluator;
import io.github.r4t2.nilum.common.expr.ExprNode;
import io.github.r4t2.nilum.common.expr.ExprParser;
import io.github.r4t2.nilum.common.expr.TextValueSource;
import io.github.r4t2.nilum.common.expr.ValueSource;
import io.github.r4t2.nilum.common.protocol.UiButtonClickedPacket;
import io.github.r4t2.nilum.common.protocol.UiClosedPacket;
import io.github.r4t2.nilum.common.ui.UiAnchor;
import io.github.r4t2.nilum.common.ui.UiDescriptor;
import io.github.r4t2.nilum.common.ui.UiElement;
import io.github.r4t2.nilum.fabric.NilumFabricClient;
import io.github.r4t2.nilum.fabric.NilumFabricMod;
import io.github.r4t2.nilum.fabric.hud.ClientVarStore;
import io.github.r4t2.nilum.fabric.hud.HudHeadText;
import io.github.r4t2.nilum.fabric.hud.NilumHudTextValueSource;
import io.github.r4t2.nilum.fabric.hud.NilumHudValueSource;
import io.github.r4t2.nilum.fabric.network.NilumUiButtonClickedPayload;
import io.github.r4t2.nilum.fabric.network.NilumUiClosedPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Renders a Custom UI's layers by z-order and handles button clicks. Requirement-based visibility isn't wired up yet. */
public final class NilumCustomUiScreen extends Screen {

    private sealed interface RenderableLayer {
        String elementId();

        int x();

        int y();

        int layer();

        int width();

        int height();

        record Sprite(String elementId, int x, int y, int layer, int width, int height,
                      CustomUi.TextureRef texture, CustomUi.TextureRef pressedTexture) implements RenderableLayer {
            boolean isButton() {
                return pressedTexture != null;
            }
        }

        record TextLayer(String elementId, int x, int y, int layer, int width, int height,
                          Font font, Component value, int color) implements RenderableLayer {
        }

        default boolean contains(double mouseX, double mouseY) {
            return mouseX >= x() && mouseX < x() + width() && mouseY >= y() && mouseY < y() + height();
        }
    }

    private final String uiId;
    private final CustomUi customUi;
    private final List<RenderableLayer> layers = new ArrayList<>();
    private String pressedElementId;

    public NilumCustomUiScreen(String uiId, CustomUi customUi) {
        super(Component.literal("Nilum UI: " + uiId));
        this.uiId = uiId;
        this.customUi = customUi;
    }

    @Override
    protected void init() {
        layers.clear();
        UiDescriptor descriptor = customUi.descriptor();
        LocalPlayer player = Minecraft.getInstance().player;
        ValueSource valueSource = player == null ? null : new NilumHudValueSource(player, new ClientVarStore());
        TextValueSource textSource = player == null ? null : new NilumHudTextValueSource(player);

        for (Map.Entry<String, UiElement> entry : descriptor.elements().entrySet()) {
            UiElement element = entry.getValue();
            if (element instanceof UiElement.Text text) {
                addTextLayer(entry.getKey(), text, valueSource, textSource);
                continue;
            }
            if (element instanceof UiElement.Head head) {
                addHeadLayer(entry.getKey(), head);
                continue;
            }
            String imageFile = switch (element) {
                case UiElement.Image image -> image.imageFile();
                case UiElement.Button button -> button.imageFile();
                case UiElement.Text ignored -> throw new IllegalStateException("handled above");
                case UiElement.Head ignored -> throw new IllegalStateException("handled above");
            };
            CustomUi.TextureRef pressedTexture = element instanceof UiElement.Button button
                    ? customUi.textureFor(button.pressedImageFile()).orElse(null)
                    : null;
            customUi.textureFor(imageFile).ifPresentOrElse(
                    texture -> layers.add(new RenderableLayer.Sprite(entry.getKey(), element.x(), element.y(),
                            element.layer(), texture.width(), texture.height(), texture, pressedTexture)),
                    () -> NilumFabricMod.LOGGER.warn("Custom UI '" + uiId + "' element '" + entry.getKey()
                            + "' references image '" + imageFile + "', which the server never sent; "
                            + "check the server console for a missing-texture warning when it loaded this UI."));
        }

        if (descriptor.anchor() == UiAnchor.CENTER) {
            recenter();
        }

        layers.sort(Comparator.comparingInt(RenderableLayer::layer));
        if (layers.isEmpty()) {
            NilumFabricMod.LOGGER.warn("Custom UI '" + uiId + "' opened with zero renderable layers.");
        }
    }

    private void addTextLayer(String elementId, UiElement.Text text, ValueSource valueSource, TextValueSource textSource) {
        String raw;
        if (text.text().isPresent()) {
            raw = text.text().get();
        } else if (valueSource == null || textSource == null) {
            raw = "";
        } else {
            ExprNode node = ExprParser.parse(text.clientConnector().orElseThrow());
            raw = ExprEvaluator.evaluateText(node, valueSource, textSource, 0);
        }
        Component value = HudHeadText.containsHeadTag(raw) ? HudHeadText.parse(raw) : Component.literal(raw);

        Font font = resolveFont(text.font());
        layers.add(new RenderableLayer.TextLayer(elementId, text.x(), text.y(), text.layer(),
                font.width(value), font.lineHeight, font, value, text.color()));
    }

    private void addHeadLayer(String elementId, UiElement.Head head) {
        Component value = HudHeadText.parse("<head:" + head.player() + ">");
        Font font = resolveFont("default");
        layers.add(new RenderableLayer.TextLayer(elementId, head.x(), head.y(), head.layer(),
                font.width(value), font.lineHeight, font, value, 0xFFFFFFFF));
    }

    private static Font resolveFont(String fontId) {
        if ("default".equals(fontId)) {
            return Minecraft.getInstance().font;
        }
        return NilumFabricClient.FONT_STORE.get(fontId).orElseGet(() -> Minecraft.getInstance().font);
    }

    /** Shifts every layer by the same offset so the bounding box of all of them, keeping their authored
     *  positions relative to each other, lands centered on the current screen size. */
    private void recenter() {
        if (layers.isEmpty()) {
            return;
        }
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (RenderableLayer layer : layers) {
            minX = Math.min(minX, layer.x());
            minY = Math.min(minY, layer.y());
            maxX = Math.max(maxX, layer.x() + layer.width());
            maxY = Math.max(maxY, layer.y() + layer.height());
        }

        int offsetX = (width - (maxX - minX)) / 2 - minX;
        int offsetY = (height - (maxY - minY)) / 2 - minY;

        List<RenderableLayer> recentered = new ArrayList<>(layers.size());
        for (RenderableLayer layer : layers) {
            recentered.add(switch (layer) {
                case RenderableLayer.Sprite sprite -> new RenderableLayer.Sprite(sprite.elementId(),
                        sprite.x() + offsetX, sprite.y() + offsetY, sprite.layer(), sprite.width(), sprite.height(),
                        sprite.texture(), sprite.pressedTexture());
                case RenderableLayer.TextLayer text -> new RenderableLayer.TextLayer(text.elementId(),
                        text.x() + offsetX, text.y() + offsetY, text.layer(), text.width(), text.height(),
                        text.font(), text.value(), text.color());
            });
        }
        layers.clear();
        layers.addAll(recentered);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        for (RenderableLayer layer : layers) {
            switch (layer) {
                case RenderableLayer.Sprite sprite -> {
                    CustomUi.TextureRef texture = sprite.isButton() && sprite.elementId().equals(pressedElementId)
                            ? sprite.pressedTexture() : sprite.texture();
                    graphics.blit(RenderPipelines.GUI_TEXTURED, texture.id(), sprite.x(), sprite.y(),
                            0, 0, texture.width(), texture.height(), texture.width(), texture.height());
                }
                case RenderableLayer.TextLayer text ->
                        graphics.drawString(text.font(), text.value(), text.x(), text.y(), text.color());
            }
        }
    }

    private RenderableLayer.Sprite topButtonAt(double mouseX, double mouseY) {
        for (int i = layers.size() - 1; i >= 0; i--) {
            if (layers.get(i) instanceof RenderableLayer.Sprite sprite && sprite.isButton() && sprite.contains(mouseX, mouseY)) {
                return sprite;
            }
        }
        return null;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            RenderableLayer.Sprite hit = topButtonAt(event.x(), event.y());
            if (hit != null) {
                pressedElementId = hit.elementId();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && pressedElementId != null) {
            RenderableLayer.Sprite released = topButtonAt(event.x(), event.y());
            if (released != null && released.elementId().equals(pressedElementId)) {
                ClientPlayNetworking.send(new NilumUiButtonClickedPayload(
                        new UiButtonClickedPacket(uiId, pressedElementId).encode()));
            }
            pressedElementId = null;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public void onClose() {
        super.onClose();
        if (ClientPlayNetworking.canSend(NilumUiClosedPayload.TYPE)) {
            ClientPlayNetworking.send(new NilumUiClosedPayload(new UiClosedPacket(uiId).encode()));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
