package io.github.r4t2.nilum.neoforge.render;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.r4t2.nilum.common.model.BbModel;
import io.github.r4t2.nilum.common.model.BbTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Uploads a model's embedded PNG textures to the GPU once each, memoized by model id and texture
 * index. Bounded by VRAM_BUDGET_BYTES; least-recently-used textures are evicted first once exceeded.
 */
public final class TextureUploader {

    /** Rough budget, not an exact VRAM accounting (doesn't factor in mipmaps or GPU-side padding). */
    private static final long VRAM_BUDGET_BYTES = 256L * 1024 * 1024;

    private record Upload(Identifier id, long byteSize) {
    }

    /** Access-order so eviction can walk from least- to most-recently-used. */
    private final Map<String, Upload> uploaded = new LinkedHashMap<>(16, 0.75f, true);
    private long totalBytes = 0;

    public synchronized Identifier getOrUpload(String modelId, int textureIndex, BbModel model) {
        String key = modelId + "#" + textureIndex;
        Upload existing = uploaded.get(key);
        if (existing != null) {
            return existing.id();
        }

        Upload upload = upload(modelId, textureIndex, model);
        uploaded.put(key, upload);
        totalBytes += upload.byteSize();
        evictLeastRecentlyUsedOverBudget();
        return upload.id();
    }

    /** Drops every GPU texture uploaded for this model id, so the next getOrUpload call re-uploads fresh bytes. */
    public synchronized void invalidate(String modelId) {
        String prefix = modelId + "#";
        TextureManager textureManager = Minecraft.getInstance().getTextureManager();
        Iterator<Map.Entry<String, Upload>> it = uploaded.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Upload> entry = it.next();
            if (entry.getKey().startsWith(prefix)) {
                textureManager.release(entry.getValue().id());
                totalBytes -= entry.getValue().byteSize();
                it.remove();
            }
        }
    }

    private void evictLeastRecentlyUsedOverBudget() {
        if (totalBytes <= VRAM_BUDGET_BYTES) {
            return;
        }

        TextureManager textureManager = Minecraft.getInstance().getTextureManager();
        Iterator<Map.Entry<String, Upload>> it = uploaded.entrySet().iterator();
        // it.next() walks least-recently-used first under access-order iteration.
        while (totalBytes > VRAM_BUDGET_BYTES && it.hasNext()) {
            Upload evicted = it.next().getValue();
            textureManager.release(evicted.id());
            totalBytes -= evicted.byteSize();
            it.remove();
        }
    }

    private Upload upload(String modelId, int textureIndex, BbModel model) {
        BbTexture texture = model.textures().get(textureIndex);
        Identifier id = Identifier.fromNamespaceAndPath("nilum", "dynamic/" + modelId + "_" + textureIndex);

        try {
            NativeImage image = NativeImage.read(texture.pngBytes());
            long byteSize = 4L * image.getWidth() * image.getHeight();
            NilumDynamicTexture dynamicTexture = new NilumDynamicTexture(() -> "Nilum model texture " + modelId, image);
            dynamicTexture.upload();

            TextureManager textureManager = Minecraft.getInstance().getTextureManager();
            textureManager.register(id, dynamicTexture);
            return new Upload(id, byteSize);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to decode texture " + textureIndex + " for model '" + modelId + "'", e);
        }
    }
}
