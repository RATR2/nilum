package io.github.r4t2.nilum.common.asset;

import io.github.r4t2.nilum.common.model.BbBakedQuad;
import io.github.r4t2.nilum.common.model.BbModel;
import io.github.r4t2.nilum.common.model.BbModelBaker;
import io.github.r4t2.nilum.common.model.BbModelParser;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;


public final class ClientModelStore {

    private final Map<String, BbModel> modelsById = new ConcurrentHashMap<>();
    private final Map<String, Map<Integer, List<BbBakedQuad>>> bakedById = new ConcurrentHashMap<>();
    private final Map<String, Map<String, List<BbBakedQuad>>> bakedByBoneById = new ConcurrentHashMap<>();
    private final Map<String, Float> groundOffsetById = new ConcurrentHashMap<>();
    private final List<Consumer<String>> reloadListeners = new CopyOnWriteArrayList<>();

    public void onReload(Consumer<String> listener) {
        reloadListeners.add(listener);
    }

    public void load(String modelId, byte[] rawBytes) {
        BbModel model = BbModelParser.parse(new String(rawBytes, StandardCharsets.UTF_8));
        List<BbBakedQuad> quads = BbModelBaker.bake(model, List.copyOf(model.elementsByUuid().values()));

        Map<Integer, List<BbBakedQuad>> byTexture = new HashMap<>();
        for (BbBakedQuad quad : quads) {
            if (quad.textureIndex() == null) {
                continue;
            }
            byTexture.computeIfAbsent(quad.textureIndex(), key -> new ArrayList<>()).add(quad);
        }

        modelsById.put(modelId, model);
        bakedById.put(modelId, Map.copyOf(byTexture));
        bakedByBoneById.put(modelId, BbModelBaker.bakeByBone(model));
        groundOffsetById.put(modelId, lowestY(quads));
        for (Consumer<String> listener : reloadListeners) {
            listener.accept(modelId);
        }
    }

    /** The model's lowest vertex Y in its own authored coordinates, used to ground it at rest. */
    public float groundOffset(String modelId) {
        Float offset = groundOffsetById.get(modelId);
        return offset == null ? 0f : offset;
    }

    private static float lowestY(List<BbBakedQuad> quads) {
        float minY = Float.MAX_VALUE;
        for (BbBakedQuad quad : quads) {
            minY = Math.min(minY, Math.min(Math.min(quad.v0().y(), quad.v1().y()), Math.min(quad.v2().y(), quad.v3().y())));
        }
        return minY == Float.MAX_VALUE ? 0f : minY;
    }

    public Optional<BbModel> model(String modelId) {
        return Optional.ofNullable(modelsById.get(modelId));
    }

    public Optional<Map<Integer, List<BbBakedQuad>>> bakedQuadsByTexture(String modelId) {
        return Optional.ofNullable(bakedById.get(modelId));
    }

    /** Baked quads grouped by the outliner group (bone) that owns them, re-posed per frame for animation. */
    public Optional<Map<String, List<BbBakedQuad>>> bakedQuadsByBone(String modelId) {
        return Optional.ofNullable(bakedByBoneById.get(modelId));
    }

    public Set<String> modelIds() {
        return Set.copyOf(modelsById.keySet());
    }
}
