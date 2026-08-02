package io.github.r4t2.nilum.common.protocol;

/** One world position within a chunk. modelId null means "this position is no longer a Nilum block." */
public record ChunkBlockEntry(int x, int y, int z, String modelId) {
}
