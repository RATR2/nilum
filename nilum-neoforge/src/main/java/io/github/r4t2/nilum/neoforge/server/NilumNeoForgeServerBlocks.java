package io.github.r4t2.nilum.neoforge.server;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.r4t2.nilum.common.block.BlockDefinition;
import io.github.r4t2.nilum.common.block.BlockDropEntry;
import io.github.r4t2.nilum.common.hosting.NilumAssetHost;
import io.github.r4t2.nilum.common.logging.NilumLogger;
import io.github.r4t2.nilum.common.model.BbCollisionBox;
import io.github.r4t2.nilum.common.model.BbCollisionParser;
import io.github.r4t2.nilum.common.model.BbCollisionResult;
import io.github.r4t2.nilum.common.model.BbModel;
import io.github.r4t2.nilum.common.model.CollisionTier;
import io.github.r4t2.nilum.neoforge.block.NilumBlockEntity;
import io.github.r4t2.nilum.neoforge.block.NilumBlocks;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * A NeoForge-hosted server's "/nilum placeblock" command and break handling for the real,
 * registered NilumBlock. Placement/removal/model-id all ride the block's own BlockEntity and
 * vanilla's ordinary chunk sync, no separate tracking map or broadcast packet needed.
 */
public final class NilumNeoForgeServerBlocks {

    private NilumNeoForgeServerBlocks() {
    }

    public static void register(NilumAssetHost assetHost, NilumLogger logger) {
        Random random = new Random();

        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, event ->
                event.getDispatcher().register(Commands.literal("nilum")
                        .then(Commands.literal("placeblock")
                                .then(Commands.argument("blockTypeId", StringArgumentType.word())
                                        .executes(context -> placeBlock(context.getSource(),
                                                StringArgumentType.getString(context, "blockTypeId"),
                                                assetHost, logger))))));

        NeoForge.EVENT_BUS.addListener(BlockEvent.BreakEvent.class, event -> onBreak(event, assetHost, random, logger));
    }

    private static int placeBlock(CommandSourceStack source, String blockTypeId, NilumAssetHost assetHost, NilumLogger logger) {
        Optional<BlockDefinition> definition = assetHost.blocks().get(blockTypeId);
        if (definition.isEmpty()) {
            source.sendFailure(Component.literal("No loaded block type named '" + blockTypeId
                    + "' (drop a matching .yml file into the blocks folder)."));
            return 0;
        }

        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());
        var state = NilumBlocks.BLOCK.get().defaultBlockState();
        level.setBlock(pos, state, Block.UPDATE_ALL);

        if (level.getBlockEntity(pos) instanceof NilumBlockEntity blockEntity) {
            List<BbCollisionBox> collisionBoxes = resolveCollisionBoxes(definition.get(), assetHost, blockTypeId, logger);
            blockEntity.setDefinition(blockTypeId, definition.get().hardness(), definition.get().explosionResistance(), collisionBoxes);
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }

        logger.info("Placed block '" + blockTypeId + "' at " + pos + ".");
        source.sendSuccess(() -> Component.literal("Placed '" + blockTypeId + "' at " + pos + "."), true);
        return 1;
    }

    /** Found boxes as parsed; a model with no collision group defaults to a full solid cube; an explicit "none" intent is fully passable. */
    private static List<BbCollisionBox> resolveCollisionBoxes(BlockDefinition definition, NilumAssetHost assetHost,
                                                                String blockTypeId, NilumLogger logger) {
        Optional<BbModel> model = assetHost.models().get(definition.modelId());
        if (model.isEmpty()) {
            return List.of(fullCube());
        }

        BbCollisionResult result = BbCollisionParser.resolve(model.get());
        return switch (result) {
            case BbCollisionResult.Found found -> found.boxes();
            case BbCollisionResult.IntentionallyNonSolid ignored -> List.of();
            case BbCollisionResult.MissingWarning ignored -> {
                logger.warn("Block type '" + blockTypeId + "' has no 'collision' group in its model and no "
                        + "collision_intent set; defaulting to a solid full cube.");
                yield List.of(fullCube());
            }
        };
    }

    private static BbCollisionBox fullCube() {
        return new BbCollisionBox(0, 0, 0, 1, 1, 1, CollisionTier.SOLID);
    }

    /** A definition with configured drops rolls and drops them directly, replacing vanilla's own harvest. */
    private static void onBreak(BlockEvent.BreakEvent event, NilumAssetHost assetHost, Random random, NilumLogger logger) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockPos pos = event.getPos();
        if (!(serverLevel.getBlockEntity(pos) instanceof NilumBlockEntity blockEntity)) {
            return;
        }

        Optional<BlockDefinition> definition = assetHost.blocks().get(blockEntity.definitionId());
        if (definition.isEmpty() || definition.get().drops().isEmpty()) {
            return;
        }

        event.setCanceled(true);
        serverLevel.removeBlock(pos, false);
        for (BlockDropEntry drop : definition.get().drops()) {
            drop.roll(random).ifPresent(count -> dropItem(serverLevel, pos, drop.itemId(), count, logger));
        }
    }

    private static void dropItem(ServerLevel level, BlockPos pos, String itemId, int count, NilumLogger logger) {
        if (itemId.startsWith("nilum:")) {
            logger.warn("Block drop references custom item '" + itemId
                    + "', which isn't supported on hosted servers yet; skipping.");
            return;
        }
        Identifier id = Identifier.tryParse(itemId);
        Item item = id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        if (item == null) {
            logger.warn("Block drop references unknown item '" + itemId + "'.");
            return;
        }
        Block.popResource(level, pos, new ItemStack(item, count));
    }
}
