package io.github.r4t2.nilum.fabric.creativetab;

import io.github.r4t2.nilum.common.asset.ClientModelStore;
import io.github.r4t2.nilum.fabric.render.IconAtlas;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Four fixed Nilum creative tabs, registered once at mod init. Items and Models populate from
 * whatever's currently loaded. Blocks stays empty until Nilum has a custom block system. Custom
 * Items stays empty until something calls addCustomItem, an API surface for other code to feed into.
 */
public final class NilumCreativeTabs {

    private static final List<ItemStack> CUSTOM_ITEMS = new CopyOnWriteArrayList<>();

    private static final ResourceKey<CreativeModeTab> ITEMS = key("items");
    private static final ResourceKey<CreativeModeTab> BLOCKS = key("blocks");
    private static final ResourceKey<CreativeModeTab> MODELS = key("models");
    private static final ResourceKey<CreativeModeTab> CUSTOM_ITEMS_TAB = key("custom_items");

    private NilumCreativeTabs() {
    }

    public static void register(IconAtlas iconAtlas, ClientModelStore modelStore) {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, ITEMS, FabricItemGroup.builder()
                .title(Component.translatable("itemGroup.nilum.items"))
                .icon(() -> new ItemStack(Items.PAPER))
                .displayItems((parameters, output) -> {
                })
                .build());
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, BLOCKS, FabricItemGroup.builder()
                .title(Component.translatable("itemGroup.nilum.blocks"))
                .icon(() -> new ItemStack(Items.BRICKS))
                .displayItems((parameters, output) -> {
                })
                .build());
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, MODELS, FabricItemGroup.builder()
                .title(Component.translatable("itemGroup.nilum.models"))
                .icon(() -> new ItemStack(Items.ARMOR_STAND))
                .displayItems((parameters, output) -> {
                })
                .build());
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, CUSTOM_ITEMS_TAB, FabricItemGroup.builder()
                .title(Component.translatable("itemGroup.nilum.custom_items"))
                .icon(() -> new ItemStack(Items.NAME_TAG))
                .displayItems((parameters, output) -> {
                })
                .build());

        ItemGroupEvents.modifyEntriesEvent(ITEMS).register(entries -> {
            for (String iconId : iconAtlas.iconIds()) {
                entries.accept(NilumTaggedItems.iconItem(Items.PAPER, iconId));
            }
        });
        ItemGroupEvents.modifyEntriesEvent(MODELS).register(entries -> {
            for (String modelId : modelStore.modelIds()) {
                entries.accept(NilumTaggedItems.modelItem(Items.PAPER, modelId));
            }
        });
        ItemGroupEvents.modifyEntriesEvent(CUSTOM_ITEMS_TAB).register(entries -> {
            for (ItemStack stack : CUSTOM_ITEMS) {
                entries.accept(stack);
            }
        });
    }

    /** Adds an item to the "Nilum Custom Items" tab; empty by default, meant for other code to populate. */
    public static void addCustomItem(ItemStack stack) {
        CUSTOM_ITEMS.add(stack);
    }

    private static ResourceKey<CreativeModeTab> key(String path) {
        return ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath("nilum", path));
    }
}
