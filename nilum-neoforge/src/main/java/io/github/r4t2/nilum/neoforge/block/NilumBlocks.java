package io.github.r4t2.nilum.neoforge.block;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registered once from the mod constructor, on every physical side, so client and server registries stay in sync. */
public final class NilumBlocks {

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("nilum");
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "nilum");

    public static final DeferredBlock<NilumBlock> BLOCK =
            BLOCKS.registerBlock("custom_block", NilumBlock::new, NilumBlock::defaultProperties);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NilumBlockEntity>> BLOCK_ENTITY_TYPE =
            BLOCK_ENTITY_TYPES.register("custom_block",
                    () -> new BlockEntityType<>(NilumBlockEntity::new, BLOCK.get()));

    private NilumBlocks() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }
}
