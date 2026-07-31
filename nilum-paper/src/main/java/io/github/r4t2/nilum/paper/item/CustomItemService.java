package io.github.r4t2.nilum.paper.item;

import io.github.r4t2.nilum.common.model.ModelRegistry;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * Represents a Nilum custom item as a real vanilla ItemStack carrying its
 * model id in the item's {@code custom_model_data} strings list - a real,
 * network-synced Mojang data component, not Bukkit's item PersistentDataContainer
 * (which also reaches the client, but has no stable public format for a client
 * mod to read back without depending on Bukkit-specific internals).
 * <p>
 * Client-side rendering of these items (intercepting item-in-hand/inventory
 * rendering) isn't built yet - that's a different, more invasive rendering
 * subsystem than the world-placed model renderer, and needs its own research pass.
 */
public final class CustomItemService {

    private static final String ID_PREFIX = "nilum:";

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
        item.setData(DataComponentTypes.CUSTOM_MODEL_DATA, CustomModelData.customModelData().addString(ID_PREFIX + modelId));
        return Optional.of(item);
    }

    /** The Nilum model id an item represents, or empty if it isn't a Nilum custom item. */
    public static Optional<String> modelIdOf(ItemStack item) {
        CustomModelData data = item.getData(DataComponentTypes.CUSTOM_MODEL_DATA);
        if (data == null) {
            return Optional.empty();
        }

        List<String> strings = data.strings();
        if (strings.isEmpty() || !strings.get(0).startsWith(ID_PREFIX)) {
            return Optional.empty();
        }

        return Optional.of(strings.get(0).substring(ID_PREFIX.length()));
    }
}
