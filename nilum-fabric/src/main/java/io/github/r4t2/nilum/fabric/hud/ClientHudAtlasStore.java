package io.github.r4t2.nilum.fabric.hud;

import io.github.r4t2.nilum.fabric.NilumFabricMod;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Owns every currently-loaded {@link HudAtlas}, keyed by server-assigned atlas id. */
public final class ClientHudAtlasStore {

    private final Map<String, HudAtlas> atlasesById = new ConcurrentHashMap<>();

    public void add(String atlasId, byte[] assetBytes) {
        Minecraft.getInstance().execute(() -> {
            try {
                atlasesById.put(atlasId, HudAtlas.load(atlasId, assetBytes));
            } catch (IOException | RuntimeException e) {
                NilumFabricMod.LOGGER.warn("Failed to load HUD atlas '" + atlasId + "': " + e);
            }
        });
    }

    public Optional<HudAtlas> get(String atlasId) {
        return Optional.ofNullable(atlasesById.get(atlasId));
    }

    public Collection<HudAtlas> all() {
        return atlasesById.values();
    }

    public void onHudFrame(String atlasId, String elementId, int frame) {
        get(atlasId).ifPresent(atlas -> atlas.setServerFrame(elementId, frame));
    }

    public void onAtlasPatch(String atlasId, String elementId, int frame, byte[] png) {
        get(atlasId).ifPresent(atlas -> atlas.applyPatch(elementId, frame, png));
    }

    public void onOverride(String atlasId, String elementId, int frame, int durationTicks) {
        get(atlasId).ifPresent(atlas -> atlas.override(elementId, frame, durationTicks));
    }

    public void onRelease(String atlasId, String elementId) {
        get(atlasId).ifPresent(atlas -> atlas.release(elementId));
    }
}
