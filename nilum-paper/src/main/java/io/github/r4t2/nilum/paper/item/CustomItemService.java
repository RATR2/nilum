package io.github.r4t2.nilum.paper.item;

import io.github.r4t2.nilum.common.model.ModelRegistry;
import io.github.r4t2.nilum.paper.NilumKeys;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;

/**
 * Represents a Nilum custom item as a real vanilla ItemStack carrying its
 * model id in the item's PersistentDataContainer - real per-item NBT (backed
 * by the {@code custom_data} component's {@code PublicBukkitValues} compound,
 * confirmed against Paper's own server source), not Minecraft's own item
 * model / CustomModelData systems. Both of those exist to let a resourcepack
 * pick which model to show, which needs a client resourcepack (re)load to
 * update - the opposite of what Nilum is for. This tag is purely our own;
 * updating a model's actual contents never touches it.
 * <p>
 * Client-side rendering of these items isn't built yet - that's a different,
 * more invasive rendering subsystem than the world-placed model renderer,
 * and needs its own research pass.
 */
public final class CustomItemService {

    private final ModelRegistry modelRegistry;

    public CustomItemService(ModelRegistry modelRegistry) {
        this.modelRegistry = modelRegistry;
    }

    /** Creates an item representing {@code modelId} on {@code baseMaterial}, or empty if that model isn't loaded. */
    public Optional<ItemStack> createItem(String modelId, Material baseMaterial) {
        if (modelRegistry.get(modelId).isEmpty()) {
            return Optional.empty();
        }

        ItemStack item = new ItemStack(baseMaterial);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(NilumKeys.MODEL_ID, PersistentDataType.STRING, modelId);
        item.setItemMeta(meta);
        return Optional.of(item);
    }

    /** The Nilum model id an item represents, or empty if it isn't a Nilum custom item. */
    public static Optional<String> modelIdOf(ItemStack item) {
        if (!item.hasItemMeta()) {
            return Optional.empty();
        }
        String modelId = item.getItemMeta().getPersistentDataContainer().get(NilumKeys.MODEL_ID, PersistentDataType.STRING);
        return Optional.ofNullable(modelId);
    }
}
