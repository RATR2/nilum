package io.github.r4t2.nilum.neoforge.ui;

import io.github.r4t2.nilum.common.logging.NilumLogger;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Owns every currently-loaded CustomUi, keyed by server-assigned ui id. */
public final class ClientCustomUiStore {

    private final Map<String, CustomUi> uisById = new ConcurrentHashMap<>();
    private final NilumLogger logger;

    public ClientCustomUiStore(NilumLogger logger) {
        this.logger = logger;
    }

    public void add(String uiId, byte[] assetBytes) {
        Minecraft.getInstance().execute(() -> {
            try {
                uisById.put(uiId, CustomUi.load(uiId, assetBytes));
            } catch (IOException | RuntimeException e) {
                logger.warn("Failed to load custom UI '" + uiId + "': " + e);
            }
        });
    }

    public Optional<CustomUi> get(String uiId) {
        return Optional.ofNullable(uisById.get(uiId));
    }
}
