package io.github.r4t2.nilum.neoforge;

import io.github.r4t2.nilum.common.config.NilumConfigManager;
import io.github.r4t2.nilum.common.hosting.NilumAssetHost;
import io.github.r4t2.nilum.common.logging.NilumLogger;
import io.github.r4t2.nilum.neoforge.handshake.NeoForgeServerHandshake;
import io.github.r4t2.nilum.neoforge.network.NilumHelloAckPayload;
import io.github.r4t2.nilum.neoforge.server.NilumNeoForgeServerBlocks;
import io.github.r4t2.nilum.neoforge.server.NilumNeoForgeServerModels;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Dedicated-server-only setup: real asset hosting, server handshake, hosted model/block registration. */
public final class NilumNeoForgeDedicatedServer {

    public record Handlers(BiConsumer<NilumHelloAckPayload, IPayloadContext> helloAck, Consumer<ServerPlayer> tcpUnavailable) {
    }

    private NilumNeoForgeDedicatedServer() {
    }

    public static Handlers register(IEventBus modEventBus, NilumConfigManager configManager, NilumLogger logger,
                                     String modVersion, Path configDir) {
        NilumAssetHost assetHost = new NilumAssetHost(configDir, logger);
        assetHost.loadAll();
        logger.info("Loaded " + assetHost.models().modelIds().size() + " model(s), "
                + assetHost.icons().iconIds().size() + " icon(s), "
                + assetHost.hudAtlases().atlasIds().size() + " HUD atlas(es), "
                + assetHost.shaderPacks().packIds().size() + " shader pack(s), "
                + assetHost.fonts().fontIds().size() + " font(s), "
                + assetHost.blocks().blockIds().size() + " block type(s), "
                + assetHost.uis().uiIds().size() + " custom UI(s) for this NeoForge-hosted server.");

        NeoForgeServerHandshake handshake = NeoForgeServerHandshake.register(modEventBus, configManager, logger, modVersion, assetHost);
        NilumNeoForgeServerModels.register(assetHost, logger);
        NilumNeoForgeServerBlocks.register(assetHost, logger);

        return new Handlers(handshake::onHelloAck, handshake::onTcpUnavailable);
    }
}
