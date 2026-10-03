package io.github.r4t2.nilum.common.worldgen;

/** One entry of a biome's "spawn:" list: a vanilla mob allowed to spawn here. */
public record BiomeSpawnEntry(String category, String entityId, int weight, int minCount, int maxCount) {
}
