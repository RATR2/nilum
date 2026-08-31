package io.github.r4t2.nilum.fabric.debug;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

/**
 * Live-tunable rotation/position correction for the first-person hand-IK offset. Values apply
 * every frame as they're dragged, world stays visible and unpaused behind the sliders. Export
 * prints the final values to chat, ready to hardcode into ItemInHandRendererMixin.
 */
public final class NilumHandTuneScreen extends Screen {

    private static final float ROT_RANGE = 180.0F;
    private static final float POS_RANGE = 1.0F;
    private static final int ROW_HEIGHT = 22;
    private static final int SLIDER_WIDTH = 260;

    public NilumHandTuneScreen() {
        super(Component.literal("Nilum Hand Tuning"));
    }

    @Override
    protected void init() {
        int x = (width - SLIDER_WIDTH) / 2;
        int y = height / 2 - 3 * ROW_HEIGHT;

        addCorrectionSlider(x, y, "Rotation X", ROT_RANGE, () -> HandTuneCorrection.rotX, v -> HandTuneCorrection.rotX = v);
        addCorrectionSlider(x, y + ROW_HEIGHT, "Rotation Y", ROT_RANGE, () -> HandTuneCorrection.rotY, v -> HandTuneCorrection.rotY = v);
        addCorrectionSlider(x, y + 2 * ROW_HEIGHT, "Rotation Z", ROT_RANGE, () -> HandTuneCorrection.rotZ, v -> HandTuneCorrection.rotZ = v);
        addCorrectionSlider(x, y + 3 * ROW_HEIGHT, "Position X", POS_RANGE, () -> HandTuneCorrection.posX, v -> HandTuneCorrection.posX = v);
        addCorrectionSlider(x, y + 4 * ROW_HEIGHT, "Position Y", POS_RANGE, () -> HandTuneCorrection.posY, v -> HandTuneCorrection.posY = v);
        addCorrectionSlider(x, y + 5 * ROW_HEIGHT, "Position Z", POS_RANGE, () -> HandTuneCorrection.posZ, v -> HandTuneCorrection.posZ = v);

        int buttonY = y + 6 * ROW_HEIGHT + 6;
        addRenderableWidget(Button.builder(Component.literal("Export"), b -> export())
                .bounds(x, buttonY, 84, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Reset"), b -> {
                    HandTuneCorrection.reset();
                    clearWidgets();
                    init();
                })
                .bounds(x + 88, buttonY, 84, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Close"), b -> onClose())
                .bounds(x + 176, buttonY, 84, 20).build());
    }

    private void addCorrectionSlider(int x, int y, String label, float range, DoubleSupplier getter, FloatConsumer setter) {
        addRenderableWidget(new CorrectionSlider(x, y, label, range, getter, setter));
    }

    private void export() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        mc.player.displayClientMessage(Component.literal(String.format(
                "rotation=(%.2f, %.2f, %.2f) position=(%.3f, %.3f, %.3f)",
                HandTuneCorrection.rotX, HandTuneCorrection.rotY, HandTuneCorrection.rotZ,
                HandTuneCorrection.posX, HandTuneCorrection.posY, HandTuneCorrection.posZ))
                .withStyle(ChatFormatting.YELLOW), false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Deliberately no-op: the point is to see the actual first-person hand while tuning it,
        // not a blurred/darkened world behind an opaque menu.
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @FunctionalInterface
    private interface FloatConsumer {
        void accept(float value);
    }

    private static final class CorrectionSlider extends AbstractSliderButton {
        private final String label;
        private final float range;
        private final DoubleSupplier getter;
        private final FloatConsumer setter;

        CorrectionSlider(int x, int y, String label, float range, DoubleSupplier getter, FloatConsumer setter) {
            super(x, y, SLIDER_WIDTH, 20, Component.empty(), (getter.getAsDouble() + range) / (2 * range));
            this.label = label;
            this.range = range;
            this.getter = getter;
            this.setter = setter;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            float actual = (float) (value * 2 * range - range);
            setMessage(Component.literal(String.format("%s: %.2f", label, actual)));
        }

        @Override
        protected void applyValue() {
            setter.accept((float) (value * 2 * range - range));
        }
    }
}
