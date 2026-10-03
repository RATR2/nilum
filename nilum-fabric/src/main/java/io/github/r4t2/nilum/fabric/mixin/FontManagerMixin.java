package io.github.r4t2.nilum.fabric.mixin;

import com.mojang.blaze3d.font.GlyphProvider;
import io.github.r4t2.nilum.fabric.NilumFabricClient;
import net.minecraft.client.gui.font.FontManager;
import net.minecraft.client.gui.font.FontOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;

/** Appends Nilum's font-provider icons to every FontSet vanilla builds, so <icon:id> works in any font, live, with no reload. */
@Mixin(FontManager.class)
public abstract class FontManagerMixin {

    @ModifyVariable(method = "createFontSet(Lnet/minecraft/resources/Identifier;Ljava/util/List;Ljava/util/Set;)"
            + "Lnet/minecraft/client/gui/font/FontSet;", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private List<GlyphProvider.Conditional> nilum$addIconProvider(List<GlyphProvider.Conditional> providers) {
        List<GlyphProvider.Conditional> combined = new ArrayList<>(providers);
        combined.add(new GlyphProvider.Conditional(NilumFabricClient.FONT_ICON_PROVIDER, FontOption.Filter.ALWAYS_PASS));
        return combined;
    }
}
