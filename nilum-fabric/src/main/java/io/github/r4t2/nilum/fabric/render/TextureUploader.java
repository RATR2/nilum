package io.github.r4t2.nilum.fabric.render;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.r4t2.nilum.common.model.BbModel;
import io.github.r4t2.nilum.common.model.BbTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Uploads a model's embedded PNG textures to the GPU once each, memoized by model id and texture
 * index. Public/shared (rather than owned by a single renderer) so a model used both in-world
 * (via the entity renderer) and as a held item doesn't upload the same texture twice.
 */
public final class TextureUploader {

    private final Map<String, Identifier> uploaded = new ConcurrentHashMap<>();

    public Identifier getOrUpload(String modelId, int textureIndex, BbModel model) {
        String key = modelId + "#" + textureIndex;
        return uploaded.computeIfAbsent(key, ignored -> upload(modelId, textureIndex, model));
    }

    /** Drops every GPU texture uploaded for this model id, so the next getOrUpload call re-uploads fresh bytes. */
    public void invalidate(String modelId) {
        String prefix = modelId + "#";
        TextureManager textureManager = Minecraft.getInstance().getTextureManager();
        Iterator<Map.Entry<String, Identifier>> it = uploaded.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Identifier> entry = it.next();
            if (entry.getKey().startsWith(prefix)) {
                textureManager.release(entry.getValue());
                it.remove();
            }
        }
    }

    private Identifier upload(String modelId, int textureIndex, BbModel model) {
        BbTexture texture = model.textures().get(textureIndex);
        Identifier id = Identifier.fromNamespaceAndPath("nilum", "dynamic/" + modelId + "_" + textureIndex);

        try {
            NativeImage image = NativeImage.read(texture.pngBytes());
            NilumDynamicTexture dynamicTexture = new NilumDynamicTexture(() -> "Nilum model texture " + modelId, image);
            dynamicTexture.upload();

            TextureManager textureManager = Minecraft.getInstance().getTextureManager();
            textureManager.register(id, dynamicTexture);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to decode texture " + textureIndex + " for model '" + modelId + "'", e);
        }

        return id;
    }
}
