package io.github.r4t2.nilum.fabric.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.renderer.texture.DynamicTexture;

import java.util.function.Supplier;

/**
 * DynamicTexture defaults to REPEAT addressing, which lets a fragment on a UV=0/1 boundary wrap
 * to the opposite edge instead of clamping, causing a hairline seam between adjacent quads with
 * different textures. Forces CLAMP_TO_EDGE instead.
 */
final class NilumDynamicTexture extends DynamicTexture {

    NilumDynamicTexture(Supplier<String> label, NativeImage image) {
        super(label, image);
        this.sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
    }
}
