package io.github.r4t2.nilum.neoforge.creativetab;

import io.github.r4t2.nilum.common.asset.ClientModelStore;
import io.github.r4t2.nilum.common.protocol.ItemPreviewEntry;
import io.github.r4t2.nilum.neoforge.render.IconAtlas;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Four fixed Nilum creative tabs. Custom Items lists addCustomItem() entries plus every model/icon the server says has a real item definition. */
public final class NilumCreativeTabs {

    private static final List<ItemStack> CUSTOM_ITEMS = new CopyOnWriteArrayList<>();
    private static volatile List<ItemPreviewEntry> itemDefinedModels = List.of();
    private static volatile List<ItemPreviewEntry> itemDefinedIcons = List.of();

    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "nilum");

    private static final DeferredHolder<CreativeModeTab, CreativeModeTab> ITEMS = TABS.register("items", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.nilum.items"))
                    .icon(() -> new ItemStack(Items.PAPER))
                    .displayItems((parameters, output) -> {
                    })
                    .build());
    private static final DeferredHolder<CreativeModeTab, CreativeModeTab> BLOCKS = TABS.register("blocks", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.nilum.blocks"))
                    .icon(() -> new ItemStack(Items.BRICKS))
                    .displayItems((parameters, output) -> {
                    })
                    .build());
    private static final DeferredHolder<CreativeModeTab, CreativeModeTab> MODELS = TABS.register("models", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.nilum.models"))
                    .icon(() -> new ItemStack(Items.ARMOR_STAND))
                    .displayItems((parameters, output) -> {
                    })
                    .build());
    private static final DeferredHolder<CreativeModeTab, CreativeModeTab> CUSTOM_ITEMS_TAB = TABS.register("custom_items", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.nilum.custom_items"))
                    .icon(() -> new ItemStack(Items.NAME_TAG))
                    .displayItems((parameters, output) -> {
                    })
                    .build());

    private NilumCreativeTabs() {
    }

    public static void register(IEventBus modEventBus, IconAtlas iconAtlas, ClientModelStore modelStore) {
        TABS.register(modEventBus);

        modEventBus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey().equals(ITEMS.getKey())) {
                for (String iconId : iconAtlas.iconIds()) {
                    event.accept(NilumTaggedItems.iconItem(Items.PAPER, iconId));
                }
            } else if (event.getTabKey().equals(CUSTOM_ITEMS_TAB.getKey())) {
                // Deliberately not auto-populated from modelStore.modelIds()/iconAtlas.iconIds() for
                // MODELS/ITEMS: a loaded model or icon isn't necessarily meant to be a standalone item
                // (block-only models, skeleton overrides, hand-IK calibration rigs). Only ids the
                // server confirms have a real items/*.yml definition (see setItemDefinedAssets) show
                // up here.
                for (ItemStack stack : CUSTOM_ITEMS) {
                    event.accept(stack);
                }
                for (ItemPreviewEntry preview : itemDefinedModels) {
                    event.accept(NilumTaggedItems.modelItem(Items.PAPER, preview));
                }
                for (ItemPreviewEntry preview : itemDefinedIcons) {
                    event.accept(NilumTaggedItems.iconItem(Items.PAPER, preview));
                }
            }
        });
    }

    /** Adds an item to the "Nilum Custom Items" tab; empty by default, meant for other code to populate. */
    public static void addCustomItem(ItemStack stack) {
        CUSTOM_ITEMS.add(stack);
    }

    /** Which loaded models/icons the server says have a real item definition, with their name/lore/hide_groups; drives the "Nilum Custom Items" tab. */
    public static void setItemDefinedAssets(List<ItemPreviewEntry> models, List<ItemPreviewEntry> icons) {
        itemDefinedModels = List.copyOf(models);
        itemDefinedIcons = List.copyOf(icons);
    }
}
