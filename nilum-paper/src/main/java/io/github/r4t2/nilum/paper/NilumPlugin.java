package io.github.r4t2.nilum.paper;

import io.github.r4t2.nilum.common.config.ConfigSchema;
import io.github.r4t2.nilum.common.config.LoggingConfig;
import io.github.r4t2.nilum.common.config.NilumConfigManager;
import io.github.r4t2.nilum.common.config.NilumConfigVersion;
import io.github.r4t2.nilum.common.config.TcpConfig;
import io.github.r4t2.nilum.common.logging.NilumLogger;
import io.github.r4t2.nilum.common.model.ModelLoadError;
import io.github.r4t2.nilum.common.model.ModelRegistry;
import io.github.r4t2.nilum.common.protocol.NilumChannels;
import io.github.r4t2.nilum.paper.handshake.HandshakeListener;
import io.github.r4t2.nilum.paper.logging.PaperConsoleSink;
import io.github.r4t2.nilum.paper.model.ModelDisplayService;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.List;

public final class NilumPlugin extends JavaPlugin {

    private NilumConfigManager configManager;
    private NilumLogger logger;
    private HandshakeListener handshakeListener;
    private ModelRegistry modelRegistry;
    private ModelDisplayService modelDisplayService;

    @Override
    public void onEnable() {
        configManager = new NilumConfigManager(
                getDataFolder().toPath().resolve("config.yml"),
                NilumConfigVersion.CURRENT,
                "Nilum configuration",
                new ConfigSchema(List.of(
                        TcpConfig.BIND_ADDRESS, TcpConfig.PORT, TcpConfig.ADVERTISED_HOST,
                        LoggingConfig.DEBUG, LoggingConfig.MAX_LOG_FILES)),
                List.of());

        try {
            configManager.load();
        } catch (IOException e) {
            getLogger().severe("Failed to load Nilum's config, disabling: " + e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        logger = new NilumLogger(new PaperConsoleSink(), getDataFolder().toPath().resolve("nilum.log"),
                () -> configManager.get(LoggingConfig.DEBUG), configManager.get(LoggingConfig.MAX_LOG_FILES));

        handshakeListener = new HandshakeListener(this, logger, configManager);

        modelRegistry = new ModelRegistry();
        try {
            List<ModelLoadError> errors = modelRegistry.loadDirectory(getDataFolder().toPath().resolve("models"));
            for (ModelLoadError error : errors) {
                logger.warn("Failed to load model '" + error.fileName() + "': " + error.cause());
            }
            logger.info("Loaded " + modelRegistry.modelIds().size() + " model(s) from the models folder.");
        } catch (IOException e) {
            logger.error("Failed to scan the models folder", e);
        }
        modelDisplayService = new ModelDisplayService(this, logger, modelRegistry);

        getServer().getMessenger().registerOutgoingPluginChannel(this, NilumChannels.HELLO_QUALIFIED);
        getServer().getMessenger().registerOutgoingPluginChannel(this, NilumChannels.TCP_OFFER_QUALIFIED);
        getServer().getMessenger().registerOutgoingPluginChannel(this, NilumChannels.MODEL_SPAWN_QUALIFIED);
        getServer().getMessenger().registerOutgoingPluginChannel(this, NilumChannels.ASSET_MANIFEST_QUALIFIED);
        getServer().getMessenger().registerIncomingPluginChannel(this, NilumChannels.HELLO_ACK_QUALIFIED, handshakeListener);
        getServer().getMessenger().registerIncomingPluginChannel(this, NilumChannels.TCP_UNAVAILABLE_QUALIFIED, handshakeListener);
        getServer().getPluginManager().registerEvents(handshakeListener, this);

        var command = getCommand("nilum");
        if (command != null) {
            command.setExecutor(new NilumCommand(this));
        }

        logger.info("Nilum enabled.");
    }

    @Override
    public void onDisable() {
        if (handshakeListener != null) {
            handshakeListener.shutdown();
        }
        if (logger != null) {
            logger.info("Nilum disabled.");
        }
    }

    /** @return true if the reload succeeded. */
    public boolean reloadNilumConfig() {
        try {
            configManager.reload();
            handshakeListener.applyTcpConfig();
            logger.info("Config reloaded.");
            return true;
        } catch (IOException e) {
            logger.error("Failed to reload config", e);
            return false;
        }
    }

    public HandshakeListener handshakes() {
        return handshakeListener;
    }

    public ModelRegistry models() {
        return modelRegistry;
    }

    public ModelDisplayService modelDisplays() {
        return modelDisplayService;
    }
}
