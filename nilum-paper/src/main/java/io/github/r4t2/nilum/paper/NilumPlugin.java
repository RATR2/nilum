package io.github.r4t2.nilum.paper;

import io.github.r4t2.nilum.common.config.ConfigSchema;
import io.github.r4t2.nilum.common.config.HandshakeConfig;
import io.github.r4t2.nilum.common.config.LoggingConfig;
import io.github.r4t2.nilum.common.config.ModerationConfig;
import io.github.r4t2.nilum.common.config.NilumConfigManager;
import io.github.r4t2.nilum.common.config.NilumConfigVersion;
import io.github.r4t2.nilum.common.config.TcpConfig;
import io.github.r4t2.nilum.common.icon.IconDisplay;
import io.github.r4t2.nilum.common.icon.IconRegistry;
import io.github.r4t2.nilum.common.logging.NilumLogger;
import io.github.r4t2.nilum.common.model.ModelLoadError;
import io.github.r4t2.nilum.common.model.ModelRegistry;
import io.github.r4t2.nilum.common.protocol.NilumChannels;
import io.github.r4t2.nilum.paper.handshake.HandshakeListener;
import io.github.r4t2.nilum.paper.icon.IconsYamlManager;
import io.github.r4t2.nilum.paper.item.CustomItemService;
import io.github.r4t2.nilum.paper.item.IconItemService;
import io.github.r4t2.nilum.paper.logging.PaperConsoleSink;
import io.github.r4t2.nilum.paper.model.ModelDisplayService;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public final class NilumPlugin extends JavaPlugin {

    private NilumConfigManager configManager;
    private NilumLogger logger;
    private HandshakeListener handshakeListener;
    private ModelRegistry modelRegistry;
    private IconRegistry iconRegistry;
    private IconsYamlManager iconsYamlManager;
    private ModelDisplayService modelDisplayService;
    private CustomItemService customItemService;
    private IconItemService iconItemService;
    private String buildCommit = "unknown";

    @Override
    public void onEnable() {
        buildCommit = readBuildCommit();

        configManager = new NilumConfigManager(
                getDataFolder().toPath().resolve("config").resolve("main.yml"),
                NilumConfigVersion.CURRENT,
                "Nilum configuration",
                new ConfigSchema(List.of(
                        TcpConfig.BIND_ADDRESS, TcpConfig.PORT, TcpConfig.ADVERTISED_HOST,
                        HandshakeConfig.ALLOW_VANILLA_CLIENTS,
                        ModerationConfig.LOG_CLIENT_MODS, ModerationConfig.DISABLED_MODS,
                        ModerationConfig.DISABLED_KICK_MESSAGE,
                        LoggingConfig.DEBUG, LoggingConfig.WARNING, LoggingConfig.ERROR,
                        LoggingConfig.MODERATION, LoggingConfig.MAX_LOG_FILES)),
                List.of());

        try {
            configManager.load();
        } catch (IOException e) {
            getLogger().severe("Failed to load Nilum's config, disabling: " + e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        logger = new NilumLogger(new PaperConsoleSink(), getDataFolder().toPath().resolve("logs").resolve("nilum.log"),
                LoggingConfig.destinationLookup(configManager), configManager.get(LoggingConfig.MAX_LOG_FILES));

        handshakeListener = new HandshakeListener(this, logger, configManager);

        modelRegistry = new ModelRegistry();
        iconRegistry = new IconRegistry();
        iconsYamlManager = new IconsYamlManager(getDataFolder().toPath().resolve("icons"), logger);
        reloadModels();
        reloadIcons();
        modelDisplayService = new ModelDisplayService(this, logger, modelRegistry);
        customItemService = new CustomItemService(modelRegistry);
        iconItemService = new IconItemService(iconRegistry);

        getServer().getMessenger().registerOutgoingPluginChannel(this, NilumChannels.HELLO_QUALIFIED);
        getServer().getMessenger().registerOutgoingPluginChannel(this, NilumChannels.TCP_OFFER_QUALIFIED);
        getServer().getMessenger().registerOutgoingPluginChannel(this, NilumChannels.MODEL_SPAWN_QUALIFIED);
        getServer().getMessenger().registerOutgoingPluginChannel(this, NilumChannels.ASSET_MANIFEST_QUALIFIED);
        getServer().getMessenger().registerOutgoingPluginChannel(this, NilumChannels.MOD_LIST_REQUEST_QUALIFIED);
        getServer().getMessenger().registerIncomingPluginChannel(this, NilumChannels.HELLO_ACK_QUALIFIED, handshakeListener);
        getServer().getMessenger().registerIncomingPluginChannel(this, NilumChannels.TCP_UNAVAILABLE_QUALIFIED, handshakeListener);
        getServer().getMessenger().registerIncomingPluginChannel(this, NilumChannels.MOD_LIST_QUALIFIED, handshakeListener);
        getServer().getPluginManager().registerEvents(handshakeListener, this);

        var command = getCommand("nilum");
        if (command != null) {
            NilumCommand executor = new NilumCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
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

    /** @return true if reading the config back succeeded. Doesn't touch the TCP socket or models. */
    public boolean reloadSettings() {
        try {
            configManager.reload();
            logger.info("Config reloaded.");
            return true;
        } catch (IOException e) {
            logger.error("Failed to reload config", e);
            return false;
        }
    }

    /** @return true if the reload succeeded. Re-reads the config first, then restarts the TCP socket. */
    public boolean reloadTcp() {
        try {
            configManager.reload();
        } catch (IOException e) {
            logger.error("Failed to reload config", e);
            return false;
        }
        handshakeListener.applyTcpConfig();
        logger.info("TCP side-channel reloaded.");
        return true;
    }

    /** @return true if the reload succeeded. */
    public boolean reloadModels() {
        try {
            List<ModelLoadError> errors = modelRegistry.loadDirectory(getDataFolder().toPath().resolve("models"));
            for (ModelLoadError error : errors) {
                logger.warn("Failed to load model '" + error.fileName() + "': " + error.cause());
            }
            logger.info("Loaded " + modelRegistry.modelIds().size() + " model(s) from the models folder.");
            handshakeListener.broadcastAssetManifest();
            return true;
        } catch (IOException e) {
            logger.error("Failed to reload the models folder", e);
            return false;
        }
    }

    /** @return true if the reload succeeded. */
    public boolean reloadIcons() {
        try {
            iconRegistry.loadDirectory(getDataFolder().toPath().resolve("icons"));
            Map<String, IconDisplay> resolved = iconsYamlManager.reload(iconRegistry.iconIds(), modelRegistry);
            for (Map.Entry<String, IconDisplay> entry : resolved.entrySet()) {
                iconRegistry.applyDisplay(entry.getKey(), entry.getValue());
            }
            logger.info("Loaded " + iconRegistry.iconIds().size() + " icon(s) from the icons folder.");
            handshakeListener.broadcastAssetManifest();
            return true;
        } catch (IOException e) {
            logger.error("Failed to reload the icons folder", e);
            return false;
        }
    }

    public String buildCommit() {
        return buildCommit;
    }

    private String readBuildCommit() {
        try (InputStream in = getClass().getResourceAsStream("/nilum-build.properties")) {
            if (in == null) {
                return "unknown";
            }
            Properties properties = new Properties();
            properties.load(in);
            return properties.getProperty("commit", "unknown");
        } catch (IOException e) {
            return "unknown";
        }
    }

    public HandshakeListener handshakes() {
        return handshakeListener;
    }

    public ModelRegistry models() {
        return modelRegistry;
    }

    public IconRegistry icons() {
        return iconRegistry;
    }

    public ModelDisplayService modelDisplays() {
        return modelDisplayService;
    }

    public CustomItemService customItems() {
        return customItemService;
    }

    public IconItemService iconItems() {
        return iconItemService;
    }
}
