package io.github.r4t2.nilum.fabric.block;

import io.github.r4t2.nilum.common.protocol.ChunkBlockEntry;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Which world positions are currently known to be Nilum custom blocks, and which model each one
 * renders. Populated by ChunkBlockEntry batches (a null modelId means removal), cleared
 * per-chunk on ClientChunkEvents.CHUNK_UNLOAD so it can't grow forever.
 */
public final class ClientBlockRegistry {

    private final ConcurrentMap<BlockPos, String> modelIdByPosition = new ConcurrentHashMap<>();

    public void apply(List<ChunkBlockEntry> entries) {
        for (ChunkBlockEntry entry : entries) {
            BlockPos pos = new BlockPos(entry.x(), entry.y(), entry.z());
            if (entry.modelId() == null) {
                modelIdByPosition.remove(pos);
            } else {
                modelIdByPosition.put(pos, entry.modelId());
            }
        }
    }

    public Optional<String> modelIdAt(BlockPos pos) {
        return Optional.ofNullable(modelIdByPosition.get(pos));
    }

    /** Every currently-known Nilum block position and its model id; the renderer filters this down itself each frame. */
    public Map<BlockPos, String> entries() {
        return modelIdByPosition;
    }

    public void onChunkUnload(int chunkX, int chunkZ) {
        modelIdByPosition.keySet().removeIf(pos -> (pos.getX() >> 4) == chunkX && (pos.getZ() >> 4) == chunkZ);
    }
}
