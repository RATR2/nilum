package io.github.r4t2.nilum.paper.model;

import io.github.r4t2.nilum.common.logging.NilumLogger;
import io.github.r4t2.nilum.common.model.BbCollisionParser;
import io.github.r4t2.nilum.common.model.BbCollisionResult;
import io.github.r4t2.nilum.common.model.BbModel;
import io.github.r4t2.nilum.common.model.BbProxyShape;
import io.github.r4t2.nilum.common.model.ModelRegistry;
import io.github.r4t2.nilum.common.model.ProxyMaterialClassifier;
import io.github.r4t2.nilum.common.protocol.ModelSpawnPacket;
import io.github.r4t2.nilum.common.protocol.NilumChannels;
import io.github.r4t2.nilum.paper.NilumPlugin;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BoundingBox;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spawns an invisible anchor entity for a loaded model and tells connected
 * clients which model it represents. Also resolves the model's collision
 * group into world-space bounding boxes at placement time.
 */
public final class ModelDisplayService {

    private static final NamespacedKey MODEL_ID_KEY = new NamespacedKey("nilum", "model_id");

    private final NilumPlugin plugin;
    private final NilumLogger logger;
    private final ModelRegistry modelRegistry;

    private final Map<UUID, String> placements = new ConcurrentHashMap<>();
    private final Map<UUID, List<BoundingBox>> collisionBoxes = new ConcurrentHashMap<>();
    private final Map<UUID, BbProxyShape> proxyShapes = new ConcurrentHashMap<>();

    public ModelDisplayService(NilumPlugin plugin, NilumLogger logger, ModelRegistry modelRegistry) {
        this.plugin = plugin;
        this.logger = logger;
        this.modelRegistry = modelRegistry;
    }

    /** Spawns an anchor entity for {@code modelId} at {@code location}, or empty if that model isn't loaded. */
    public Optional<UUID> place(Location location, String modelId) {
        Optional<BbModel> model = modelRegistry.get(modelId);
        if (model.isEmpty()) {
            return Optional.empty();
        }

        ItemDisplay display = location.getWorld().spawn(location, ItemDisplay.class, entity -> {
            entity.setItemStack(ItemStack.empty());
            entity.getPersistentDataContainer().set(MODEL_ID_KEY, PersistentDataType.STRING, modelId);
        });

        UUID entityId = display.getUniqueId();
        placements.put(entityId, modelId);
        logger.info("Placed model '" + modelId + "' at " + formatLocation(location) + " (entity " + entityId + ").");

        resolveCollision(entityId, modelId, location, model.get());

        broadcast(new ModelSpawnPacket(entityId, modelId));
        return Optional.of(entityId);
    }

    /** Collision boxes resolved for a placement, in world space - empty if the model has no collision group. */
    public List<BoundingBox> collisionBoxesOf(UUID entityId) {
        return collisionBoxes.getOrDefault(entityId, List.of());
    }

    /** The classified proxy shape for a placement, if it has a collision group. */
    public Optional<BbProxyShape> proxyShapeOf(UUID entityId) {
        return Optional.ofNullable(proxyShapes.get(entityId));
    }

    private void resolveCollision(UUID entityId, String modelId, Location location, BbModel model) {
        BbCollisionResult result = BbCollisionParser.resolve(model);

        switch (result) {
            case BbCollisionResult.Found found -> {
                List<BoundingBox> worldBoxes = CollisionShapes.toWorldBoundingBoxes(location, found.boxes());
                collisionBoxes.put(entityId, worldBoxes);

                BbProxyShape shape = ProxyMaterialClassifier.classify(found.boxes());
                proxyShapes.put(entityId, shape);
                logger.info("Model '" + modelId + "' has " + worldBoxes.size()
                        + " collision box(es), classified as " + shape + ".");
            }
            case BbCollisionResult.IntentionallyNonSolid ignored -> {
                // collision_intent: none - deliberate, no warning per the design doc.
            }
            case BbCollisionResult.MissingWarning ignored -> logger.warn("Nilum model '" + modelId
                    + "' has no collision group. It will be non-solid. Add a 'collision' group to define hitboxes.");
        }
    }

    /** Sends every currently-tracked placement to one player - called once they complete the handshake. */
    public void sendAllTo(Player player) {
        for (Map.Entry<UUID, String> placement : placements.entrySet()) {
            player.sendPluginMessage(plugin, NilumChannels.MODEL_SPAWN_QUALIFIED,
                    new ModelSpawnPacket(placement.getKey(), placement.getValue()).encode());
        }
    }

    private void broadcast(ModelSpawnPacket packet) {
        byte[] data = packet.encode();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (plugin.handshakes().hasClient(player.getUniqueId())) {
                player.sendPluginMessage(plugin, NilumChannels.MODEL_SPAWN_QUALIFIED, data);
            }
        }
    }

    private static String formatLocation(Location location) {
        return "(%.1f, %.1f, %.1f)".formatted(location.getX(), location.getY(), location.getZ());
    }
}
