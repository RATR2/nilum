package io.github.r4t2.nilum.common.block;

import java.util.List;

/** One entry from the blocks folder, backing a real registered block on a Fabric/NeoForge-hosted server. */
public record BlockDefinition(String id, String modelId, float hardness, float explosionResistance, List<BlockDropEntry> drops) {
}
