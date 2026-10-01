package io.github.r4t2.nilum.common.worldgen;

import java.util.List;

/**
 * A named, reusable Nilum dimension: dimensions/<id>.yml, same convention as blocks/items. Phase 1
 * generates with a flat generator over the referenced biome(s) (first biome id is the fixed
 * biome source) rather than noise-based terrain, to avoid needing vanilla's surface-rule/
 * density-function machinery; see ROADMAP.md "Dimensions" and DatapackWriter.
 */
public record DimensionDefinition(
        String id,
        List<String> biomeIds,
        boolean hasSkylight,
        boolean hasCeiling,
        boolean ultrawarm,
        boolean natural,
        boolean bedWorks,
        boolean respawnAnchorWorks,
        boolean piglinSafe,
        double ambientLight,
        int minY,
        int height,
        int logicalHeight,
        String infiniburn,
        String effects,
        List<FlatLayer> layers
) {
}
