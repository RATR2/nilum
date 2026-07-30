package io.github.r4t2.nilum.fabric;

import io.github.r4t2.nilum.common.config.ConfigSchema;
import io.github.r4t2.nilum.common.config.LoggingConfig;
import io.github.r4t2.nilum.common.config.NilumConfigManager;
import io.github.r4t2.nilum.common.config.NilumConfigVersion;
import io.github.r4t2.nilum.common.config.TcpConfig;
import io.github.r4t2.nilum.common.logging.NilumLogger;
import io.github.r4t2.nilum.fabric.handshake.FabricServerHandshake;
import io.github.r4t2.nilum.fabric.logging.FabricLogSink;
import io.github.r4t2.nilum.fabric.network.NilumAssetManifestPayload;
import io.github.r4t2.nilum.fabric.network.NilumHelloAckPayload;
import io.github.r4t2.nilum.fabric.network.NilumHelloPayload;
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
 * Common entrypoint: runs regardless of physical side. Registers the shared
 * payload types/codecs and the Tier 2 server-side handshake. {@code CONFIG}
 * and {@code LOGGER} are static since Fabric gives entrypoints no way to pass
 * objects to each other directly.
 */
public final class NilumFabricMod implements ModInitializer {

    public static NilumConfigManager CONFIG;
    public static NilumLogger LOGGER;

    @Override
    public void onInitialize() {
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve("nilum");

        CONFIG = new NilumConfigManager(
                configDir.resolve("config.yml"),
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

        LOGGER = new NilumLogger(new FabricLogSink(), configDir.resolve("nilum.log"),
                () -> CONFIG.get(LoggingConfig.DEBUG), CONFIG.get(LoggingConfig.MAX_LOG_FILES));

        PayloadTypeRegistry.playS2C().register(NilumHelloPayload.TYPE, NilumHelloPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(NilumHelloAckPayload.TYPE, NilumHelloAckPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NilumTcpOfferPayload.TYPE, NilumTcpOfferPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(NilumTcpUnavailablePayload.TYPE, NilumTcpUnavailablePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NilumAssetManifestPayload.TYPE, NilumAssetManifestPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NilumModelSpawnPayload.TYPE, NilumModelSpawnPayload.CODEC);

        FabricServerHandshake.register(CONFIG, LOGGER);

        LOGGER.info("Nilum initialized.");
    }
}
