package io.github.r4t2.nilum.common.hosting;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Which entity UUIDs a Fabric/NeoForge-hosted server has placed a model on. In-memory only, doesn't survive a restart. */
public final class ServerModelPlacements {

    private final Map<UUID, String> placements = new ConcurrentHashMap<>();

    public void put(UUID entityId, String modelId) {
        placements.put(entityId, modelId);
    }

    public void remove(UUID entityId) {
        placements.remove(entityId);
    }

    public Map<UUID, String> all() {
        return Map.copyOf(placements);
    }
}
