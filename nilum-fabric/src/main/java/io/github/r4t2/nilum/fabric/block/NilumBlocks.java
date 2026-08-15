package io.github.r4t2.nilum.fabric.block;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Registered once from the common entrypoint, on every physical side, so client and server registries stay in sync. */
public final class NilumBlocks {

    public static final Identifier BLOCK_ID = Identifier.fromNamespaceAndPath("nilum", "custom_block");

    public static NilumBlock BLOCK;
    public static BlockEntityType<NilumBlockEntity> BLOCK_ENTITY_TYPE;

    private NilumBlocks() {
    }

    public static void register() {
        BLOCK = Registry.register(BuiltInRegistries.BLOCK, BLOCK_ID, new NilumBlock(NilumBlock.defaultProperties()));
        BLOCK_ENTITY_TYPE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, BLOCK_ID,
                FabricBlockEntityTypeBuilder.create(NilumBlockEntity::new, BLOCK).build());
    }
}
