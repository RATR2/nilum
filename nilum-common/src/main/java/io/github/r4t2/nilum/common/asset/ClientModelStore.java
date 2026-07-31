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


public final class ClientModelStore {

    private final Map<String, BbModel> modelsById = new ConcurrentHashMap<>();
    private final Map<String, Map<Integer, List<BbBakedQuad>>> bakedById = new ConcurrentHashMap<>();

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
    }

    public Optional<BbModel> model(String modelId) {
        return Optional.ofNullable(modelsById.get(modelId));
    }

    public Optional<Map<Integer, List<BbBakedQuad>>> bakedQuadsByTexture(String modelId) {
        return Optional.ofNullable(bakedById.get(modelId));
    }

    public Set<String> modelIds() {
        return Set.copyOf(modelsById.keySet());
    }
}
