package io.github.r4t2.nilum.neoforge.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.renderer.texture.DynamicTexture;

import java.util.function.Supplier;

/** Forces CLAMP_TO_EDGE; DynamicTexture defaults to REPEAT, which causes hairline seams between adjacent quads with different textures. */
final class NilumDynamicTexture extends DynamicTexture {

    NilumDynamicTexture(Supplier<String> label, NativeImage image) {
        super(label, image);
        this.sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
    }
}
