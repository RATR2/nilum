package io.github.r4t2.nilum.paper.item;

import io.github.r4t2.nilum.common.icon.IconRegistry;
import io.github.r4t2.nilum.paper.NilumKeys;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;

/** Represents a Nilum icon-only custom item as an ItemStack carrying its icon id in the PersistentDataContainer. */
public final class IconItemService {

    private final IconRegistry iconRegistry;

    public IconItemService(IconRegistry iconRegistry) {
        this.iconRegistry = iconRegistry;
    }

    /**
     * @param iconId       loaded icon to represent
     * @param baseMaterial vanilla material to back the item
     * @return the item, or empty if iconId isn't loaded
     */
    public Optional<ItemStack> createItem(String iconId, Material baseMaterial) {
        if (!iconRegistry.iconIds().contains(iconId)) {
            return Optional.empty();
        }

        ItemStack item = new ItemStack(baseMaterial);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(NilumKeys.ICON_ID, PersistentDataType.STRING, iconId);
        item.setItemMeta(meta);
        return Optional.of(item);
    }

    /**
     * @param item item to inspect
     * @return the Nilum icon id it represents, or empty if it isn't a Nilum icon item
     */
    public static Optional<String> iconIdOf(ItemStack item) {
        if (!item.hasItemMeta()) {
            return Optional.empty();
        }
        String iconId = item.getItemMeta().getPersistentDataContainer().get(NilumKeys.ICON_ID, PersistentDataType.STRING);
        return Optional.ofNullable(iconId);
    }
}
