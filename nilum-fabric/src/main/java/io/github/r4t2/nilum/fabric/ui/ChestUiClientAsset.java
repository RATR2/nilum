package io.github.r4t2.nilum.fabric.ui;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.r4t2.nilum.common.ui.ChestUiDescriptor;
import io.github.r4t2.nilum.common.ui.ChestUiParser;
import io.github.r4t2.nilum.common.ui.UiAssetPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** One server-streamed chest UI: its parsed descriptor and its background texture, uploaded as a real GPU texture. */
public final class ChestUiClientAsset {

    private final ChestUiDescriptor descriptor;
    private final Identifier backgroundTextureId;

    private ChestUiClientAsset(ChestUiDescriptor descriptor, Identifier backgroundTextureId) {
        this.descriptor = descriptor;
        this.backgroundTextureId = backgroundTextureId;
    }

    /** Must be called on the render thread; decodes the background PNG and registers a real GPU texture. */
    static ChestUiClientAsset load(String uiId, byte[] assetBytes) throws IOException {
        UiAssetPayload payload = UiAssetPayload.decode(assetBytes);
        ChestUiDescriptor descriptor = ChestUiParser.parse(new String(payload.descriptorBytes(), StandardCharsets.UTF_8));

        byte[] png = payload.images().get(descriptor.background());
        if (png == null) {
            throw new IOException("Chest UI '" + uiId + "' references background '" + descriptor.background()
                    + "', which the server never sent");
        }

        NativeImage canvas = NativeImage.read(png);
        Identifier id = Identifier.fromNamespaceAndPath("nilum", "dynamic/chest_ui/" + sanitize(uiId) + "/background");
        DynamicTexture gpuTexture = new DynamicTexture(() -> "Nilum chest UI '" + uiId + "' background", canvas);
        Minecraft.getInstance().getTextureManager().register(id, gpuTexture);

        return new ChestUiClientAsset(descriptor, id);
    }

    private static String sanitize(String key) {
        return key.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_./-]", "_");
    }

    public ChestUiDescriptor descriptor() {
        return descriptor;
    }

    public Identifier backgroundTextureId() {
        return backgroundTextureId;
    }
}
