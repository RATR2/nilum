package io.github.r4t2.nilum.fabric;

import io.github.r4t2.nilum.common.config.ConfigSchema;
import io.github.r4t2.nilum.common.config.LoggingConfig;
import io.github.r4t2.nilum.common.config.NilumConfigManager;
import io.github.r4t2.nilum.common.config.NilumConfigVersion;
import io.github.r4t2.nilum.common.config.TcpConfig;
import io.github.r4t2.nilum.common.logging.NilumLogger;
import io.github.r4t2.nilum.fabric.logging.FabricLogSink;
import io.github.r4t2.nilum.fabric.network.NilumAssetManifestPayload;
import io.github.r4t2.nilum.fabric.network.NilumHelloAckPayload;
import io.github.r4t2.nilum.fabric.network.NilumHelloPayload;
import io.github.r4t2.nilum.fabric.network.NilumModListPayload;
import io.github.r4t2.nilum.fabric.network.NilumModListRequestPayload;
import io.github.r4t2.nilum.fabric.network.NilumModelSpawnPayload;
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
                NilumConfigVersion.CURRENT,
                "Nilum configuration",
                new ConfigSchema(List.of(
                        TcpConfig.BIND_ADDRESS, TcpConfig.PORT, TcpConfig.ADVERTISED_HOST,
                        LoggingConfig.DEBUG, LoggingConfig.MAX_LOG_FILES)),
                List.of());

        try {
            CONFIG.load();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load Nilum's config", e);
        }

        LOGGER = new NilumLogger(new FabricLogSink(), configDir.resolve("logs").resolve("nilum.log"),
                () -> CONFIG.get(LoggingConfig.DEBUG), CONFIG.get(LoggingConfig.MAX_LOG_FILES));

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

        LOGGER.info("Nilum initialized.");
    }
}
