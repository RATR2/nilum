package io.github.r4t2.nilum.fabric.render;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jspecify.annotations.Nullable;

/**
 * Reads a Nilum tag back off an ItemStack, matching how Paper serializes a
 * PersistentDataContainer entry into the minecraft:custom_data component: a top-level
 * PublicBukkitValues compound, keyed by the full namespace:key string.
 */
final class NilumItemTags {

    private static final String PUBLIC_BUKKIT_VALUES = "PublicBukkitValues";

    private NilumItemTags() {
    }

    static @Nullable String get(ItemStack itemStack, String key) {
        CustomData customData = itemStack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return null;
        }
        return customData.copyTag().getCompound(PUBLIC_BUKKIT_VALUES)
                .flatMap(values -> values.getString(key))
                .orElse(null);
    }
}
