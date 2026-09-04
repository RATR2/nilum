package io.github.r4t2.nilum.neoforge.creativetab;

import io.github.r4t2.nilum.common.protocol.ItemPreviewEntry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

/** Builds representative ItemStacks for Nilum's creative tabs, tagged the same way Paper serializes a PersistentDataContainer. */
final class NilumTaggedItems {

    private static final String PUBLIC_BUKKIT_VALUES = "PublicBukkitValues";

    private NilumTaggedItems() {
    }

    /** Bare tag only, no name/lore/hide_groups; used for the generic "Items" icon-atlas tab, which isn't backed by a real item definition. */
    static ItemStack iconItem(Item baseMaterial, String iconId) {
        return tagged(baseMaterial, "nilum:icon_id", iconId, null);
    }

    /** Full preview (name, lore, hide_groups) from a real item definition; used for the "Nilum Custom Items" tab. */
    static ItemStack modelItem(Item baseMaterial, ItemPreviewEntry preview) {
        return tagged(baseMaterial, "nilum:model_id", preview.assetId(), preview);
    }

    static ItemStack iconItem(Item baseMaterial, ItemPreviewEntry preview) {
        return tagged(baseMaterial, "nilum:icon_id", preview.assetId(), preview);
    }

    private static ItemStack tagged(Item baseMaterial, String key, String value, ItemPreviewEntry preview) {
        ItemStack stack = new ItemStack(baseMaterial);
        CompoundTag publicValues = new CompoundTag();
        publicValues.putString(key, value);
        if (preview != null && !preview.hideGroups().isEmpty()) {
            publicValues.putString("nilum:hide_groups", String.join(",", preview.hideGroups()));
        }
        CompoundTag customData = new CompoundTag();
        customData.put(PUBLIC_BUKKIT_VALUES, publicValues);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(customData));

        if (preview != null && !preview.displayName().isBlank()) {
            stack.set(DataComponents.CUSTOM_NAME, legacy(preview.displayName()));
        }
        if (preview != null && !preview.lore().isEmpty()) {
            stack.set(DataComponents.LORE, new ItemLore(preview.lore().stream().map(NilumTaggedItems::legacy).toList()));
        }
        return stack;
    }

    /** item.yml text uses '&' legacy color codes (matching the server's LegacyComponentSerializer); vanilla's own text layout already re-parses embedded section-sign codes. */
    private static Component legacy(String text) {
        return Component.literal(text.replace('&', '§'));
    }
}
