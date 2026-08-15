package io.github.r4t2.nilum.neoforge.hud;

import io.github.r4t2.nilum.common.logging.NilumLogger;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Owns every currently-loaded HudAtlas, keyed by server-assigned atlas id. */
public final class ClientHudAtlasStore {

    private final Map<String, HudAtlas> atlasesById = new ConcurrentHashMap<>();
    private final NilumLogger logger;

    public ClientHudAtlasStore(NilumLogger logger) {
        this.logger = logger;
    }

    public void add(String atlasId, byte[] assetBytes) {
        Minecraft.getInstance().execute(() -> {
            try {
                atlasesById.put(atlasId, HudAtlas.load(atlasId, assetBytes, logger));
            } catch (IOException | RuntimeException e) {
                logger.warn("Failed to load HUD atlas '" + atlasId + "': " + e);
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

    public void onHudText(String atlasId, String elementId, String text) {
        get(atlasId).ifPresent(atlas -> atlas.setServerText(elementId, text));
    }
}
