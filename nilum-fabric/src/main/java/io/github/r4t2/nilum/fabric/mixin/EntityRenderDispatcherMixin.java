package io.github.r4t2.nilum.fabric.mixin;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * getPlayerRenderer(AbstractClientPlayer) is the single method every other system calls to pick
 * a player's renderer; there's no registration API for it.
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {

    @Inject(method = "getPlayerRenderer", at = @At("HEAD"))
    private void nilum$onGetPlayerRenderer(AbstractClientPlayer player, CallbackInfoReturnable<AvatarRenderer> cir) {
        // Real skeleton swap wires in here next.
    }
}
