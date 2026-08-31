package io.github.r4t2.nilum.paper.block;

import io.github.r4t2.nilum.common.logging.NilumLogger;
import io.github.r4t2.nilum.common.model.BbCollisionParser;
import io.github.r4t2.nilum.common.model.BbCollisionResult;
import io.github.r4t2.nilum.common.model.BbModel;
import io.github.r4t2.nilum.common.model.CollisionTier;
import io.github.r4t2.nilum.common.model.ModelRegistry;
import io.github.r4t2.nilum.common.protocol.ChunkBlockEntry;
import io.github.r4t2.nilum.paper.collision.NilumCollisionRegistry;
import io.github.r4t2.nilum.paper.model.CollisionShapes;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which world positions are Nilum custom blocks and which BlockDefinition each one is.
 * In-memory only for now, doesn't survive a server restart yet.
 */
public final class CustomBlockRegistry {

    private final BlockDefinitionRegistry blockDefinitions;
    private final ModelRegistry modelRegistry;
    private final NilumCollisionRegistry collisionRegistry;
    private final NilumLogger logger;
    private final Map<BlockKey, String> blockTypeIdByPosition = new ConcurrentHashMap<>();

    public CustomBlockRegistry(BlockDefinitionRegistry blockDefinitions, ModelRegistry modelRegistry,
                                NilumCollisionRegistry collisionRegistry, NilumLogger logger) {
        this.blockDefinitions = blockDefinitions;
        this.modelRegistry = modelRegistry;
        this.collisionRegistry = collisionRegistry;
        this.logger = logger;
    }

    /** @return the placed definition, or empty if blockTypeId isn't a loaded block type. */
    public Optional<BlockDefinition> place(Location location, String blockTypeId) {
        Optional<BlockDefinition> definition = blockDefinitions.get(blockTypeId);
        if (definition.isEmpty()) {
            return Optional.empty();
        }

        location.getBlock().setType(definition.get().proxy().wireMaterial());
        BlockKey key = BlockKey.of(location);
        blockTypeIdByPosition.put(key, blockTypeId);
        registerCollision(key, location, definition.get());
        return definition;
    }

    /**
     * Only registers manual collision for a block whose collision group has a PARTIAL box; a
     * uniformly SOLID shape is already handled by the wire block's own vanilla hitbox.
     */
    private void registerCollision(BlockKey key, Location location, BlockDefinition definition) {
        Optional<BbModel> model = modelRegistry.get(definition.modelId());
        if (model.isEmpty()) {
            return;
        }

        if (!(BbCollisionParser.resolve(model.get()) instanceof BbCollisionResult.Found found)) {
            return;
        }
        boolean hasPartial = found.boxes().stream().anyMatch(box -> box.tier() == CollisionTier.PARTIAL);
        if (!hasPartial) {
            return;
        }

        if (definition.proxy().wireMaterial().isSolid()) {
            logger.warn("Block definition '" + definition.id() + "' has a 'partial' collision box but its wire "
                    + "block (" + definition.proxy().wireMaterial() + ") is solid; pick a non-solid material "
                    + "(e.g. tripwire, a carpet) so Nilum's manual collision isn't fighting the wire block's own.");
        }

        Location origin = location.getBlock().getLocation();
        collisionRegistry.set("block:" + key, CollisionShapes.toWorldEntries(origin, found.boxes()));
    }

    /** @return the block type id that was there, if any. Always leaves the actual block as AIR. */
    public Optional<String> remove(Location location) {
        BlockKey key = BlockKey.of(location);
        String blockTypeId = blockTypeIdByPosition.remove(key);
        if (blockTypeId != null) {
            location.getBlock().setType(Material.AIR);
            collisionRegistry.remove("block:" + key);
        }
        return Optional.ofNullable(blockTypeId);
    }

    /** Like remove(Location), but doesn't touch the actual block; for when the caller already will. */
    public Optional<String> forget(Location location) {
        BlockKey key = BlockKey.of(location);
        collisionRegistry.remove("block:" + key);
        return Optional.ofNullable(blockTypeIdByPosition.remove(key));
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
