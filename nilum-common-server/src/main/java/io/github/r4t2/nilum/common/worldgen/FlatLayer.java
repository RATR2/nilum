package io.github.r4t2.nilum.common.worldgen;

/** One layer of a dimension's flat-generator "layers:" stack, e.g. "minecraft:stone:60". */
public record FlatLayer(String block, int height) {
}
