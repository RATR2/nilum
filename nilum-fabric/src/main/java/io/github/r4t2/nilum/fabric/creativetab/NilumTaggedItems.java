package io.github.r4t2.nilum.fabric.creativetab;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Builds representative ItemStacks for Nilum's creative tabs, tagged the exact same way Paper's
 * PersistentDataContainer serializes onto the wire ({@code custom_data -> PublicBukkitValues ->
 * <namespace>:<key>}) - so picking one up via vanilla's own creative-tab click handling round-trips
 * correctly with no Nilum-specific packet involved at all.
 */
final class NilumTaggedItems {

    private static final String PUBLIC_BUKKIT_VALUES = "PublicBukkitValues";

    private NilumTaggedItems() {
    }

    static ItemStack iconItem(Item baseMaterial, String iconId) {
        return tagged(baseMaterial, "nilum:icon_id", iconId);
    }

    static ItemStack modelItem(Item baseMaterial, String modelId) {
        return tagged(baseMaterial, "nilum:model_id", modelId);
    }

    private static ItemStack tagged(Item baseMaterial, String key, String value) {
        ItemStack stack = new ItemStack(baseMaterial);
        CompoundTag publicValues = new CompoundTag();
        publicValues.putString(key, value);
        CompoundTag customData = new CompoundTag();
        customData.put(PUBLIC_BUKKIT_VALUES, publicValues);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(customData));
        return stack;
    }
}
