package io.github.r4t2.nilum.fabric.ui;

import io.github.r4t2.nilum.fabric.NilumFabricMod;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Owns every currently-loaded chest UI asset, keyed by server-assigned ui id. */
public final class ClientChestUiStore {

    private final Map<String, ChestUiClientAsset> assetsById = new ConcurrentHashMap<>();

    public void add(String uiId, byte[] assetBytes) {
        Minecraft.getInstance().execute(() -> {
            try {
                assetsById.put(uiId, ChestUiClientAsset.load(uiId, assetBytes));
            } catch (IOException | RuntimeException e) {
                NilumFabricMod.LOGGER.warn("Failed to load chest UI '" + uiId + "': " + e);
            }
        });
    }

    public Optional<ChestUiClientAsset> get(String uiId) {
        return Optional.ofNullable(assetsById.get(uiId));
    }
}
