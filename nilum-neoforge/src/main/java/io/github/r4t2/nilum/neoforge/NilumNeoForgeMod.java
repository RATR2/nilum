package io.github.r4t2.nilum.neoforge;

import io.github.r4t2.nilum.common.config.ConfigSchema;
import io.github.r4t2.nilum.common.config.LoggingConfig;
import io.github.r4t2.nilum.common.config.NilumConfigManager;
import io.github.r4t2.nilum.common.config.TcpConfig;
import io.github.r4t2.nilum.common.logging.NilumLogger;
import io.github.r4t2.nilum.neoforge.block.NilumBlocks;
import io.github.r4t2.nilum.neoforge.logging.NeoForgeLogSink;
import io.github.r4t2.nilum.neoforge.network.NilumActivateShaderPackPayload;
import io.github.r4t2.nilum.neoforge.network.NilumAssetManifestPayload;
import io.github.r4t2.nilum.neoforge.network.NilumItemDefinedAssetsPayload;
import io.github.r4t2.nilum.neoforge.network.NilumAtlasPatchPayload;
import io.github.r4t2.nilum.neoforge.network.NilumBlockAnimationPlayPayload;
import io.github.r4t2.nilum.neoforge.network.NilumBlockAnimationStopPayload;
import io.github.r4t2.nilum.neoforge.network.NilumChunkBlocksPayload;
import io.github.r4t2.nilum.neoforge.network.NilumDeactivateShaderPackPayload;
import io.github.r4t2.nilum.neoforge.network.NilumEntityAnimationPlayPayload;
import io.github.r4t2.nilum.neoforge.network.NilumItemAnimationPlayPayload;
import io.github.r4t2.nilum.neoforge.network.NilumItemAnimationStopPayload;
import io.github.r4t2.nilum.neoforge.network.NilumEntityAnimationStopPayload;
import io.github.r4t2.nilum.neoforge.network.NilumHelloAckPayload;
import io.github.r4t2.nilum.neoforge.network.NilumHelloPayload;
import io.github.r4t2.nilum.neoforge.network.NilumHudFrameOverridePayload;
import io.github.r4t2.nilum.neoforge.network.NilumHudFramePayload;
import io.github.r4t2.nilum.neoforge.network.NilumHudFrameReleasePayload;
import io.github.r4t2.nilum.neoforge.network.NilumKeybindPayload;
import io.github.r4t2.nilum.neoforge.network.NilumModListPayload;
import io.github.r4t2.nilum.neoforge.network.NilumModListRequestPayload;
import io.github.r4t2.nilum.neoforge.network.NilumModelSpawnPayload;
import io.github.r4t2.nilum.neoforge.network.NilumOpenUiPayload;
import io.github.r4t2.nilum.neoforge.network.NilumSetHudAtlasVisibilityPayload;
import io.github.r4t2.nilum.neoforge.network.NilumSetHudElementVisibilityPayload;
import io.github.r4t2.nilum.neoforge.network.NilumRegisterClientVarPayload;
import io.github.r4t2.nilum.neoforge.network.NilumSetClientVarPayload;
import io.github.r4t2.nilum.neoforge.network.NilumSetHudTextPayload;
import io.github.r4t2.nilum.neoforge.network.NilumTcpOfferPayload;
import io.github.r4t2.nilum.neoforge.network.NilumTcpUnavailablePayload;
import io.github.r4t2.nilum.neoforge.network.NilumUiButtonClickedPayload;
import io.github.r4t2.nilum.neoforge.network.NilumUiClosedPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Common entrypoint: runs regardless of physical side, registers the shared payload types/codecs.
 */
@Mod(NilumNeoForgeMod.MOD_ID)
public final class NilumNeoForgeMod {

    public static final String MOD_ID = "nilum";

    private final NilumConfigManager configManager;
    private final NilumLogger logger;
    private final BiConsumer<NilumHelloAckPayload, IPayloadContext> helloAckHandler;
    private final Consumer<ServerPlayer> tcpUnavailableHandler;

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

        NilumBlocks.register(modEventBus);

        if (FMLEnvironment.getDist() == Dist.DEDICATED_SERVER) {
            NilumNeoForgeDedicatedServer.Handlers handlers =
                    NilumNeoForgeDedicatedServer.register(modEventBus, configManager, logger, modVersion, configDir);
            this.helloAckHandler = handlers.helloAck();
            this.tcpUnavailableHandler = handlers.tcpUnavailable();
        } else {
            this.helloAckHandler = (payload, context) -> { };
            this.tcpUnavailableHandler = player -> { };
        }

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
        registrar.configurationToServer(NilumHelloAckPayload.TYPE, NilumHelloAckPayload.CODEC, helloAckHandler::accept);
        registrar.playToClient(NilumHelloPayload.TYPE, NilumHelloPayload.CODEC);
        registrar.playToServer(NilumHelloAckPayload.TYPE, NilumHelloAckPayload.CODEC, helloAckHandler::accept);
        registrar.playToClient(NilumTcpOfferPayload.TYPE, NilumTcpOfferPayload.CODEC);
        registrar.playToServer(NilumTcpUnavailablePayload.TYPE, NilumTcpUnavailablePayload.CODEC,
                (payload, context) -> tcpUnavailableHandler.accept((ServerPlayer) context.player()));
        registrar.playToClient(NilumAssetManifestPayload.TYPE, NilumAssetManifestPayload.CODEC);
        registrar.playToClient(NilumItemDefinedAssetsPayload.TYPE, NilumItemDefinedAssetsPayload.CODEC);
        registrar.playToClient(NilumModelSpawnPayload.TYPE, NilumModelSpawnPayload.CODEC);
        registrar.playToClient(NilumModListRequestPayload.TYPE, NilumModListRequestPayload.CODEC);
        // Only Paper's HandshakeListener currently requests/processes mod lists; a NeoForge-hosted
        // server never sends the request, so this handler is unreachable there.
        registrar.playToServer(NilumModListPayload.TYPE, NilumModListPayload.CODEC, (payload, context) -> { });
        registrar.playToClient(NilumHudFramePayload.TYPE, NilumHudFramePayload.CODEC);
        registrar.playToClient(NilumAtlasPatchPayload.TYPE, NilumAtlasPatchPayload.CODEC);
        registrar.playToClient(NilumHudFrameOverridePayload.TYPE, NilumHudFrameOverridePayload.CODEC);
        registrar.playToClient(NilumHudFrameReleasePayload.TYPE, NilumHudFrameReleasePayload.CODEC);
        registrar.playToClient(NilumRegisterClientVarPayload.TYPE, NilumRegisterClientVarPayload.CODEC);
        registrar.playToClient(NilumSetClientVarPayload.TYPE, NilumSetClientVarPayload.CODEC);
        registrar.playToClient(NilumSetHudTextPayload.TYPE, NilumSetHudTextPayload.CODEC);
        registrar.playToClient(NilumActivateShaderPackPayload.TYPE, NilumActivateShaderPackPayload.CODEC);
        registrar.playToClient(NilumDeactivateShaderPackPayload.TYPE, NilumDeactivateShaderPackPayload.CODEC);
        registrar.playToClient(NilumEntityAnimationPlayPayload.TYPE, NilumEntityAnimationPlayPayload.CODEC);
        registrar.playToClient(NilumEntityAnimationStopPayload.TYPE, NilumEntityAnimationStopPayload.CODEC);
        registrar.playToClient(NilumBlockAnimationPlayPayload.TYPE, NilumBlockAnimationPlayPayload.CODEC);
        registrar.playToClient(NilumBlockAnimationStopPayload.TYPE, NilumBlockAnimationStopPayload.CODEC);
        registrar.playToClient(NilumItemAnimationPlayPayload.TYPE, NilumItemAnimationPlayPayload.CODEC);
        registrar.playToClient(NilumItemAnimationStopPayload.TYPE, NilumItemAnimationStopPayload.CODEC);
        registrar.playToServer(NilumKeybindPayload.TYPE, NilumKeybindPayload.CODEC, (payload, context) -> { });
        // Paper still needs this: real registry access doesn't exist there, so it always proxies a
        // vanilla material and streams the overlay position/model over this same wire format.
        registrar.playToClient(NilumChunkBlocksPayload.TYPE, NilumChunkBlocksPayload.CODEC);
        registrar.playToClient(NilumOpenUiPayload.TYPE, NilumOpenUiPayload.CODEC);
        // Custom UI open/close is Skript/Paper-only for now, a NeoForge-hosted server has no
        // consumer for this, same as keybinds above.
        registrar.playToServer(NilumUiClosedPayload.TYPE, NilumUiClosedPayload.CODEC, (payload, context) -> { });
        registrar.playToServer(NilumUiButtonClickedPayload.TYPE, NilumUiButtonClickedPayload.CODEC, (payload, context) -> { });
        registrar.playToClient(NilumSetHudAtlasVisibilityPayload.TYPE, NilumSetHudAtlasVisibilityPayload.CODEC);
        registrar.playToClient(NilumSetHudElementVisibilityPayload.TYPE, NilumSetHudElementVisibilityPayload.CODEC);
    }
}
