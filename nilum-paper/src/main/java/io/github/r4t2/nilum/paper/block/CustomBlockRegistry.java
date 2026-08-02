package io.github.r4t2.nilum.paper.block;

import io.github.r4t2.nilum.common.protocol.ChunkBlockEntry;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which world positions are Nilum custom blocks and which BlockDefinition each one is.
 * In-memory only for now, doesn't survive a server restart yet. The wire block is whatever
 * vanilla material the block type's BlockProxy specifies; the client renders the real .bbmodel
 * geometry over that position via a dynamic block model (see nilum-fabric's NilumBlockStateModel).
 */
public final class CustomBlockRegistry {

    private final BlockDefinitionRegistry blockDefinitions;
    private final Map<BlockKey, String> blockTypeIdByPosition = new ConcurrentHashMap<>();

    public CustomBlockRegistry(BlockDefinitionRegistry blockDefinitions) {
        this.blockDefinitions = blockDefinitions;
    }

    /** @return the placed definition, or empty if blockTypeId isn't a loaded block type. */
    public Optional<BlockDefinition> place(Location location, String blockTypeId) {
        Optional<BlockDefinition> definition = blockDefinitions.get(blockTypeId);
        if (definition.isEmpty()) {
            return Optional.empty();
        }

        location.getBlock().setType(definition.get().proxy().wireMaterial());
        blockTypeIdByPosition.put(BlockKey.of(location), blockTypeId);
        return definition;
    }

    /** @return the block type id that was there, if any. Always leaves the actual block as AIR. */
    public Optional<String> remove(Location location) {
        BlockKey key = BlockKey.of(location);
        String blockTypeId = blockTypeIdByPosition.remove(key);
        if (blockTypeId != null) {
            location.getBlock().setType(Material.AIR);
        }
        return Optional.ofNullable(blockTypeId);
    }

    /** Like remove(Location), but doesn't touch the actual block; for when the caller already will. */
    public Optional<String> forget(Location location) {
        return Optional.ofNullable(blockTypeIdByPosition.remove(BlockKey.of(location)));
    }

    public Optional<String> blockTypeIdAt(Location location) {
        return Optional.ofNullable(blockTypeIdByPosition.get(BlockKey.of(location)));
    }

    public Optional<BlockDefinition> definitionAt(Location location) {
        return blockTypeIdAt(location).flatMap(blockDefinitions::get);
    }

    public List<ChunkBlockEntry> entriesInChunk(String world, int chunkX, int chunkZ) {
        List<ChunkBlockEntry> entries = new ArrayList<>();
        for (Map.Entry<BlockKey, String> entry : blockTypeIdByPosition.entrySet()) {
            BlockKey key = entry.getKey();
            if (!key.world().equals(world) || key.chunkX() != chunkX || key.chunkZ() != chunkZ) {
                continue;
            }
            blockDefinitions.get(entry.getValue()).ifPresent(definition ->
                    entries.add(new ChunkBlockEntry(key.x(), key.y(), key.z(), definition.modelId())));
        }
        return entries;
    }
}
