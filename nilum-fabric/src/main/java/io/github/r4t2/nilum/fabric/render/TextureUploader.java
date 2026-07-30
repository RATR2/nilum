package io.github.r4t2.nilum.fabric.render;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.r4t2.nilum.common.model.BbModel;
import io.github.r4t2.nilum.common.model.BbTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Uploads a model's embedded PNG textures to the GPU once each, memoized by model id and texture index. */
final class TextureUploader {

    private final Map<String, Identifier> uploaded = new ConcurrentHashMap<>();

    Identifier getOrUpload(String modelId, int textureIndex, BbModel model) {
        String key = modelId + "#" + textureIndex;
        return uploaded.computeIfAbsent(key, ignored -> upload(modelId, textureIndex, model));
    }

    private Identifier upload(String modelId, int textureIndex, BbModel model) {
        BbTexture texture = model.textures().get(textureIndex);
        Identifier id = Identifier.fromNamespaceAndPath("nilum", "dynamic/" + modelId + "_" + textureIndex);

        try {
            NativeImage image = NativeImage.read(texture.pngBytes());
            DynamicTexture dynamicTexture = new DynamicTexture(() -> "Nilum model texture " + modelId, image);
            dynamicTexture.upload();

            TextureManager textureManager = Minecraft.getInstance().getTextureManager();
            textureManager.register(id, dynamicTexture);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to decode texture " + textureIndex + " for model '" + modelId + "'", e);
        }

        return id;
    }
}
