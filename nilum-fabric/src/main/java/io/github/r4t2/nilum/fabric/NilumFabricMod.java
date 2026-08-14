package io.github.r4t2.nilum.fabric;

import io.github.r4t2.nilum.common.config.ConfigSchema;
import io.github.r4t2.nilum.common.config.LoggingConfig;
import io.github.r4t2.nilum.common.config.NilumConfigManager;
import io.github.r4t2.nilum.common.config.TcpConfig;
import io.github.r4t2.nilum.common.logging.NilumLogger;
import io.github.r4t2.nilum.fabric.logging.FabricLogSink;
import io.github.r4t2.nilum.fabric.network.NilumActivateShaderPackPayload;
import io.github.r4t2.nilum.fabric.network.NilumAssetManifestPayload;
import io.github.r4t2.nilum.fabric.network.NilumAtlasPatchPayload;
import io.github.r4t2.nilum.fabric.network.NilumBlockAnimationPlayPayload;
import io.github.r4t2.nilum.fabric.network.NilumBlockAnimationStopPayload;
import io.github.r4t2.nilum.fabric.network.NilumDeactivateShaderPackPayload;
import io.github.r4t2.nilum.fabric.network.NilumChunkBlocksPayload;
import io.github.r4t2.nilum.fabric.network.NilumEntityAnimationPlayPayload;
import io.github.r4t2.nilum.fabric.network.NilumEntityAnimationStopPayload;
import io.github.r4t2.nilum.fabric.network.NilumHelloAckPayload;
import io.github.r4t2.nilum.fabric.network.NilumHelloPayload;
import io.github.r4t2.nilum.fabric.network.NilumHudFrameOverridePayload;
import io.github.r4t2.nilum.fabric.network.NilumHudFramePayload;
import io.github.r4t2.nilum.fabric.network.NilumHudFrameReleasePayload;
import io.github.r4t2.nilum.fabric.network.NilumKeybindPayload;
import io.github.r4t2.nilum.fabric.network.NilumModListPayload;
import io.github.r4t2.nilum.fabric.network.NilumModListRequestPayload;
import io.github.r4t2.nilum.fabric.network.NilumModelSpawnPayload;
import io.github.r4t2.nilum.fabric.network.NilumRegisterClientVarPayload;
import io.github.r4t2.nilum.fabric.network.NilumSetClientVarPayload;
import io.github.r4t2.nilum.fabric.network.NilumSetHudTextPayload;
import io.github.r4t2.nilum.fabric.network.NilumTcpOfferPayload;
import io.github.r4t2.nilum.fabric.network.NilumTcpUnavailablePayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Common entrypoint: runs regardless of physical side, registers the shared payload types/codecs.
 */
public final class NilumFabricMod implements ModInitializer {

    public static NilumConfigManager CONFIG;
    public static NilumLogger LOGGER;

    @Override
    public void onInitialize() {
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve("nilum");

        CONFIG = new NilumConfigManager(
                configDir.resolve("config").resolve("main.yml"),
                "Nilum configuration",
                new ConfigSchema(List.of(
                        TcpConfig.BIND_ADDRESS, TcpConfig.PORT, TcpConfig.ADVERTISED_HOST,
                        LoggingConfig.DEBUG, LoggingConfig.WARNING, LoggingConfig.ERROR,
                        LoggingConfig.MODERATION, LoggingConfig.MAX_LOG_FILES)),
                List.of());

        try {
            CONFIG.load();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load Nilum's config", e);
        }

        LOGGER = new NilumLogger(new FabricLogSink(), configDir.resolve("logs").resolve("nilum.log"),
                LoggingConfig.destinationLookup(CONFIG), CONFIG.get(LoggingConfig.MAX_LOG_FILES));

        // Registered on both phases: a Paper server sends hello in PLAY (see HandshakeListener),
        // a Fabric-hosted server sends it during configuration (see FabricServerHandshake).
        PayloadTypeRegistry.configurationS2C().register(NilumHelloPayload.TYPE, NilumHelloPayload.CODEC);
        PayloadTypeRegistry.configurationC2S().register(NilumHelloAckPayload.TYPE, NilumHelloAckPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NilumHelloPayload.TYPE, NilumHelloPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(NilumHelloAckPayload.TYPE, NilumHelloAckPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NilumTcpOfferPayload.TYPE, NilumTcpOfferPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(NilumTcpUnavailablePayload.TYPE, NilumTcpUnavailablePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NilumAssetManifestPayload.TYPE, NilumAssetManifestPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NilumModelSpawnPayload.TYPE, NilumModelSpawnPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NilumModListRequestPayload.TYPE, NilumModListRequestPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(NilumModListPayload.TYPE, NilumModListPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NilumHudFramePayload.TYPE, NilumHudFramePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NilumAtlasPatchPayload.TYPE, NilumAtlasPatchPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NilumHudFrameOverridePayload.TYPE, NilumHudFrameOverridePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NilumHudFrameReleasePayload.TYPE, NilumHudFrameReleasePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NilumRegisterClientVarPayload.TYPE, NilumRegisterClientVarPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NilumSetClientVarPayload.TYPE, NilumSetClientVarPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NilumSetHudTextPayload.TYPE, NilumSetHudTextPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(NilumKeybindPayload.TYPE, NilumKeybindPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NilumChunkBlocksPayload.TYPE, NilumChunkBlocksPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NilumActivateShaderPackPayload.TYPE, NilumActivateShaderPackPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NilumDeactivateShaderPackPayload.TYPE, NilumDeactivateShaderPackPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NilumEntityAnimationPlayPayload.TYPE, NilumEntityAnimationPlayPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NilumEntityAnimationStopPayload.TYPE, NilumEntityAnimationStopPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NilumBlockAnimationPlayPayload.TYPE, NilumBlockAnimationPlayPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NilumBlockAnimationStopPayload.TYPE, NilumBlockAnimationStopPayload.CODEC);

        LOGGER.info("Nilum initialized.");
    }
}
