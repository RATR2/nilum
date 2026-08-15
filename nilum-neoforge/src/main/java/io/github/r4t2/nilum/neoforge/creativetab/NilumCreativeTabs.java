package io.github.r4t2.nilum.neoforge.creativetab;

import io.github.r4t2.nilum.common.asset.ClientModelStore;
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

/** Four fixed Nilum creative tabs. Custom Items stays empty until something calls addCustomItem. */
public final class NilumCreativeTabs {

    private static final List<ItemStack> CUSTOM_ITEMS = new CopyOnWriteArrayList<>();

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
            } else if (event.getTabKey().equals(MODELS.getKey())) {
                for (String modelId : modelStore.modelIds()) {
                    event.accept(NilumTaggedItems.modelItem(Items.PAPER, modelId));
                }
            } else if (event.getTabKey().equals(CUSTOM_ITEMS_TAB.getKey())) {
                for (ItemStack stack : CUSTOM_ITEMS) {
                    event.accept(stack);
                }
            }
        });
    }

    /** Adds an item to the "Nilum Custom Items" tab; empty by default, meant for other code to populate. */
    public static void addCustomItem(ItemStack stack) {
        CUSTOM_ITEMS.add(stack);
    }
}
