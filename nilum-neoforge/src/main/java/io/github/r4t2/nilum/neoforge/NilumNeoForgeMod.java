package io.github.r4t2.nilum.neoforge;

import io.github.r4t2.nilum.common.config.ConfigSchema;
import io.github.r4t2.nilum.common.config.LoggingConfig;
import io.github.r4t2.nilum.common.config.NilumConfigManager;
import io.github.r4t2.nilum.common.config.TcpConfig;
import io.github.r4t2.nilum.common.logging.NilumLogger;
import io.github.r4t2.nilum.neoforge.handshake.NeoForgeServerHandshake;
import io.github.r4t2.nilum.neoforge.logging.NeoForgeLogSink;
import io.github.r4t2.nilum.neoforge.network.NilumAssetManifestPayload;
import io.github.r4t2.nilum.neoforge.network.NilumHelloAckPayload;
import io.github.r4t2.nilum.neoforge.network.NilumHelloPayload;
import io.github.r4t2.nilum.neoforge.network.NilumModListPayload;
import io.github.r4t2.nilum.neoforge.network.NilumModListRequestPayload;
import io.github.r4t2.nilum.neoforge.network.NilumModelSpawnPayload;
import io.github.r4t2.nilum.neoforge.network.NilumTcpOfferPayload;
import io.github.r4t2.nilum.neoforge.network.NilumTcpUnavailablePayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Common entrypoint: runs regardless of physical side, registers the shared payload types/codecs.
 */
@Mod(NilumNeoForgeMod.MOD_ID)
public final class NilumNeoForgeMod {

    public static final String MOD_ID = "nilum";

    private final NilumConfigManager configManager;
    private final NilumLogger logger;
    private final NeoForgeServerHandshake serverHandshake;

    public NilumNeoForgeMod(IEventBus modEventBus, ModContainer modContainer) {
        String modVersion = modContainer.getModInfo().getVersion().toString();
        Path configDir = FMLPaths.CONFIGDIR.get().resolve("nilum");

        this.configManager = new NilumConfigManager(
                configDir.resolve("config").resolve("main.yml"),
                "Nilum configuration",
                new ConfigSchema(List.of(
                        TcpConfig.BIND_ADDRESS, TcpConfig.PORT, TcpConfig.ADVERTISED_HOST,
                        LoggingConfig.DEBUG, LoggingConfig.WARNING, LoggingConfig.ERROR,
                        LoggingConfig.MODERATION, LoggingConfig.MAX_LOG_FILES)),
                List.of());

        try {
            configManager.load();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load Nilum's config", e);
        }

        this.logger = new NilumLogger(new NeoForgeLogSink(), configDir.resolve("logs").resolve("nilum.log"),
                LoggingConfig.destinationLookup(configManager), configManager.get(LoggingConfig.MAX_LOG_FILES));

        this.serverHandshake = FMLEnvironment.getDist() == Dist.DEDICATED_SERVER
                ? NeoForgeServerHandshake.register(modEventBus, configManager, logger, modVersion)
                : null;

        modEventBus.addListener(this::onRegisterPayloadHandlers);

        if (FMLEnvironment.getDist().isClient()) {
            NilumNeoForgeClient.register(modEventBus, logger, modVersion);
        }

        logger.info("Nilum initialized.");
    }

    private void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        // Registered on both phases: a Paper server sends hello in PLAY, a NeoForge-hosted
        // server sends it during configuration (see NeoForgeServerHandshake).
        registrar.configurationToClient(NilumHelloPayload.TYPE, NilumHelloPayload.CODEC);
        registrar.configurationToServer(NilumHelloAckPayload.TYPE, NilumHelloAckPayload.CODEC,
                (payload, context) -> serverHandshake.onHelloAck(payload, context));
        registrar.playToClient(NilumHelloPayload.TYPE, NilumHelloPayload.CODEC);
        registrar.playToServer(NilumHelloAckPayload.TYPE, NilumHelloAckPayload.CODEC,
                (payload, context) -> serverHandshake.onHelloAck(payload, context));
        registrar.playToClient(NilumTcpOfferPayload.TYPE, NilumTcpOfferPayload.CODEC);
        registrar.playToServer(NilumTcpUnavailablePayload.TYPE, NilumTcpUnavailablePayload.CODEC,
                (payload, context) -> serverHandshake.onTcpUnavailable((ServerPlayer) context.player()));
        registrar.playToClient(NilumAssetManifestPayload.TYPE, NilumAssetManifestPayload.CODEC);
        registrar.playToClient(NilumModelSpawnPayload.TYPE, NilumModelSpawnPayload.CODEC);
        registrar.playToClient(NilumModListRequestPayload.TYPE, NilumModListRequestPayload.CODEC);
        // Only Paper's HandshakeListener currently requests/processes mod lists; a NeoForge-hosted
        // server never sends the request, so this handler is unreachable there.
        registrar.playToServer(NilumModListPayload.TYPE, NilumModListPayload.CODEC, (payload, context) -> { });
    }
}
