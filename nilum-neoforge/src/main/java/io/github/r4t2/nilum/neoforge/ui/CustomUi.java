package io.github.r4t2.nilum.neoforge.ui;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.r4t2.nilum.common.ui.UiAssetPayload;
import io.github.r4t2.nilum.common.ui.UiDescriptor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** One server-streamed custom UI: its parsed .ui descriptor and every referenced image, uploaded as a real GPU texture. */
public final class CustomUi {

    public record TextureRef(Identifier id, int width, int height) {
    }

    private final UiDescriptor descriptor;
    private final Map<String, TextureRef> texturesByFile;

    private CustomUi(UiDescriptor descriptor, Map<String, TextureRef> texturesByFile) {
        this.descriptor = descriptor;
        this.texturesByFile = texturesByFile;
    }

    /** Must be called on the render thread; decodes every PNG in the payload and registers real GPU textures. */
    static CustomUi load(String uiId, byte[] assetBytes) throws IOException {
        UiAssetPayload payload = UiAssetPayload.decode(assetBytes);
        UiDescriptor descriptor = payload.decodeDescriptor();

        Map<String, TextureRef> textures = new HashMap<>();
        for (Map.Entry<String, byte[]> entry : payload.images().entrySet()) {
            textures.put(entry.getKey(), loadTexture(uiId, entry.getKey(), entry.getValue()));
        }

        return new CustomUi(descriptor, textures);
    }

    private static TextureRef loadTexture(String uiId, String fileName, byte[] pngBytes) throws IOException {
        NativeImage canvas = NativeImage.read(pngBytes);
        Identifier id = Identifier.fromNamespaceAndPath("nilum", "dynamic/custom_ui/" + uiId + "/" + sanitize(fileName));
        DynamicTexture gpuTexture = new DynamicTexture(() -> "Nilum custom UI '" + uiId + "' texture '" + fileName + "'", canvas);
        Minecraft.getInstance().getTextureManager().register(id, gpuTexture);
        return new TextureRef(id, canvas.getWidth(), canvas.getHeight());
    }

    private static String sanitize(String key) {
        return key.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_./-]", "_");
    }

    public UiDescriptor descriptor() {
        return descriptor;
    }

    public Optional<TextureRef> textureFor(String imageFile) {
        return Optional.ofNullable(texturesByFile.get(imageFile));
    }
}
