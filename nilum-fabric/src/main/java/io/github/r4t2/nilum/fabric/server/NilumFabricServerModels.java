package io.github.r4t2.nilum.fabric.server;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.r4t2.nilum.common.hosting.NilumAssetHost;
import io.github.r4t2.nilum.common.hosting.ServerModelPlacements;
import io.github.r4t2.nilum.common.logging.NilumLogger;
import io.github.r4t2.nilum.common.protocol.ModelSpawnPacket;
import io.github.r4t2.nilum.fabric.network.NilumModelSpawnPayload;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

/** A Fabric-hosted server's "/nilum placemodel" command. Mirrors nilum-paper's ModelDisplayService minus collision resolution. */
public final class NilumFabricServerModels {

    private NilumFabricServerModels() {
    }

    public static void register(NilumAssetHost assetHost, NilumLogger logger) {
        ServerModelPlacements placements = new ServerModelPlacements();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal("nilum")
                        .then(Commands.literal("placemodel")
                                .then(Commands.argument("modelId", StringArgumentType.word())
                                        .executes(context -> {
                                            String modelId = StringArgumentType.getString(context, "modelId");
                                            return placeModel(context.getSource(), modelId, assetHost, placements, logger);
                                        })))));

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sendAllTo(handler.getPlayer(), placements));
    }

    private static int placeModel(CommandSourceStack source, String modelId, NilumAssetHost assetHost,
                                   ServerModelPlacements placements, NilumLogger logger) {
        if (assetHost.models().get(modelId).isEmpty()) {
            source.sendFailure(Component.literal("No loaded model named '" + modelId
                    + "' (drop a matching .bbmodel file into the models folder)."));
            return 0;
        }

        ServerLevel level = source.getLevel();
        Vec3 pos = source.getPosition();

        Display.ItemDisplay display = EntityType.ITEM_DISPLAY.create(level, EntitySpawnReason.COMMAND);
        if (display == null) {
            source.sendFailure(Component.literal("Failed to create the model's anchor entity."));
            return 0;
        }
        display.setPos(pos.x, pos.y, pos.z);
        forceRenderStateRecompute(display);
        level.addFreshEntity(display);

        UUID entityId = display.getUUID();
        placements.put(entityId, modelId);
        logger.info("Placed model '" + modelId + "' at " + pos + " (entity " + entityId + ").");

        byte[] data = new ModelSpawnPacket(entityId, modelId).encode();
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(player, new NilumModelSpawnPayload(data));
        }

        source.sendSuccess(() -> Component.literal("Placed '" + modelId + "' (entity " + entityId + ")."), true);
        return 1;
    }

    /** ItemStack.EMPTY never marks synced data dirty, so forces a shadow-strength write (vanilla's setter is private) to kick rendering off. */
    private static void forceRenderStateRecompute(Display.ItemDisplay display) {
        try {
            Method setShadowStrength = Display.class.getDeclaredMethod("setShadowStrength", float.class);
            setShadowStrength.setAccessible(true);
            setShadowStrength.invoke(display, 0f);
        } catch (ReflectiveOperationException e) {
            // Best-effort; if this fails the entity may not render until some other synced-data field changes.
        }
    }

    private static void sendAllTo(ServerPlayer player, ServerModelPlacements placements) {
        for (Map.Entry<UUID, String> placement : placements.all().entrySet()) {
            ServerPlayNetworking.send(player, new NilumModelSpawnPayload(
                    new ModelSpawnPacket(placement.getKey(), placement.getValue()).encode()));
        }
    }
}
