package io.github.r4t2.nilum.common.model;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientModelPlacements {

    private final Map<UUID, String> modelIdsByEntity = new ConcurrentHashMap<>();

    public void put(UUID entityId, String modelId) {
        modelIdsByEntity.put(entityId, modelId);
    }

    public void remove(UUID entityId) {
        modelIdsByEntity.remove(entityId);
    }

    public String get(UUID entityId) {
        return modelIdsByEntity.get(entityId);
    }
}
