package io.github.r4t2.nilum.neoforge.block;

import io.github.r4t2.nilum.common.model.AnimationPlaybackState;
import io.github.r4t2.nilum.common.protocol.ChunkBlockEntry;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Which world positions are Nilum custom blocks and which model each renders; cleared per-chunk on CHUNK_UNLOAD. */
public final class ClientBlockRegistry {

    private final ConcurrentMap<BlockPos, String> modelIdByPosition = new ConcurrentHashMap<>();
    private final ConcurrentMap<BlockPos, AnimationPlaybackState> animationByPosition = new ConcurrentHashMap<>();

    public void apply(List<ChunkBlockEntry> entries) {
        for (ChunkBlockEntry entry : entries) {
            BlockPos pos = new BlockPos(entry.x(), entry.y(), entry.z());
            if (entry.modelId() == null) {
                modelIdByPosition.remove(pos);
                animationByPosition.remove(pos);
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

    /** This block's current animation playback state, created on first access (auto-loop until a play/stop trigger arrives). */
    public AnimationPlaybackState animationState(BlockPos pos) {
        return animationByPosition.computeIfAbsent(pos, ignored -> new AnimationPlaybackState());
    }

    public void onChunkUnload(int chunkX, int chunkZ) {
        modelIdByPosition.keySet().removeIf(pos -> (pos.getX() >> 4) == chunkX && (pos.getZ() >> 4) == chunkZ);
        animationByPosition.keySet().removeIf(pos -> (pos.getX() >> 4) == chunkX && (pos.getZ() >> 4) == chunkZ);
    }
}
