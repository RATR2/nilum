package io.github.r4t2.nilum.fabric.mixin;

import io.github.r4t2.nilum.fabric.ui.ClientChestUiState;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Swaps the generic chest/shulker/barrel GUI's background texture when a Nilum chest UI was just opened. */
@Mixin(ContainerScreen.class)
public abstract class ContainerScreenMixin {

    @Shadow
    @Final
    private static Identifier CONTAINER_BACKGROUND;

    @Unique
    private Identifier nilum$backgroundOverride;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void nilum$captureOverride(ChestMenu menu, Inventory inventory, Component title, CallbackInfo ci) {
        this.nilum$backgroundOverride = ClientChestUiState.consumePending();
    }

    @Redirect(method = "renderBg", at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/gui/screens/inventory/ContainerScreen;CONTAINER_BACKGROUND:Lnet/minecraft/resources/Identifier;"))
    private Identifier nilum$redirectBackground() {
        return this.nilum$backgroundOverride != null ? this.nilum$backgroundOverride : CONTAINER_BACKGROUND;
    }
}
