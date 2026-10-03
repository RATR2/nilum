package io.github.r4t2.nilum.common.worldgen;

import java.util.List;

/**
 * A named, reusable Nilum biome type: biomes/<id>.yml, same convention as blocks/items. Phase 1
 * only covers vanilla-block/vanilla-mob generation inputs (see ROADMAP.md "Custom Biomes");
 * placing Nilum's own custom blocks during generation is a separate follow-up.
 */
public record BiomeDefinition(
        String id,
        boolean hasPrecipitation,
        float temperature,
        float downfall,
        int fogColor,
        int waterColor,
        int waterFogColor,
        int skyColor,
        Integer grassColor,
        Integer foliageColor,
        List<BiomeSpawnEntry> spawners
) {
}
