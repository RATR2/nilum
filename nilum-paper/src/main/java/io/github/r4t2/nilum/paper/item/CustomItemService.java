package io.github.r4t2.nilum.paper.item;

import io.github.r4t2.nilum.common.model.ModelRegistry;
import io.github.r4t2.nilum.paper.NilumKeys;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;

/**
 * Represents a Nilum custom item as an ItemStack carrying its model id in
 * the item's PersistentDataContainer.
 */
public final class CustomItemService {

    private final ModelRegistry modelRegistry;

    public CustomItemService(ModelRegistry modelRegistry) {
        this.modelRegistry = modelRegistry;
    }

    /**
     * @param modelId      loaded model to represent
     * @param baseMaterial vanilla material to back the item
     * @return the item, or empty if modelId isn't loaded
     */
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

    /**
     * @param item item to inspect
     * @return the Nilum model id it represents, or empty if it isn't a Nilum custom item
     */
    public static Optional<String> modelIdOf(ItemStack item) {
        if (!item.hasItemMeta()) {
            return Optional.empty();
        }
        String modelId = item.getItemMeta().getPersistentDataContainer().get(NilumKeys.MODEL_ID, PersistentDataType.STRING);
        return Optional.ofNullable(modelId);
    }
}
