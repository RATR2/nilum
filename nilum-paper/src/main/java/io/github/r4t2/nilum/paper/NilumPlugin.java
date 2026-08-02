package io.github.r4t2.nilum.paper;

import io.github.r4t2.nilum.common.config.ConfigSchema;
import io.github.r4t2.nilum.common.config.DocsConfig;
import io.github.r4t2.nilum.common.config.HandshakeConfig;
import io.github.r4t2.nilum.common.config.HudTextConfig;
import io.github.r4t2.nilum.common.config.LoggingConfig;
import io.github.r4t2.nilum.common.config.ModerationConfig;
import io.github.r4t2.nilum.common.config.NilumConfigManager;
import io.github.r4t2.nilum.common.config.TcpConfig;
import io.github.r4t2.nilum.common.font.FontRegistry;
import io.github.r4t2.nilum.common.hud.HudAtlasRegistry;
import io.github.r4t2.nilum.common.icon.IconRegistry;
import io.github.r4t2.nilum.common.logging.NilumLogger;
import io.github.r4t2.nilum.common.model.ModelLoadError;
import io.github.r4t2.nilum.common.model.ModelRegistry;
import io.github.r4t2.nilum.common.protocol.NilumChannels;
import io.github.r4t2.nilum.common.shader.ShaderPackRegistry;
import io.github.r4t2.nilum.paper.block.BlockDefinitionRegistry;
import io.github.r4t2.nilum.paper.docs.ReadmeGenerator;
import io.github.r4t2.nilum.paper.block.CustomBlockChunkSync;
import io.github.r4t2.nilum.paper.block.CustomBlockInteractionListener;
import io.github.r4t2.nilum.paper.block.CustomBlockRegistry;
import io.github.r4t2.nilum.paper.handshake.HandshakeListener;
import io.github.r4t2.nilum.paper.hud.HudAtlasService;
import io.github.r4t2.nilum.paper.hud.HudTextService;
import io.github.r4t2.nilum.paper.icon.IconsYamlManager;
import io.github.r4t2.nilum.paper.icon.IconYamlConfig;
import io.github.r4t2.nilum.paper.item.CustomItemService;
import io.github.r4t2.nilum.paper.item.IconItemService;
import io.github.r4t2.nilum.paper.item.ItemDefinitionRegistry;
import io.github.r4t2.nilum.paper.logging.PaperConsoleSink;
import io.github.r4t2.nilum.paper.model.ModelDisplayService;
import io.github.r4t2.nilum.paper.shader.ShaderPackService;
import io.github.r4t2.nilum.paper.texture.TextureUsageTracker;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
    private HudAtlasRegistry hudAtlasRegistry;
    private HudAtlasService hudAtlasService;
    private HudTextService hudTextService;
    private ModelDisplayService modelDisplayService;
    private CustomItemService customItemService;
    private IconItemService iconItemService;
    private ItemDefinitionRegistry itemDefinitionRegistry;
    private BlockDefinitionRegistry blockDefinitionRegistry;
    private CustomBlockRegistry customBlockRegistry;
    private ShaderPackRegistry shaderPackRegistry;
    private ShaderPackService shaderPackService;
    private FontRegistry fontRegistry;
    private String buildCommit = "unknown";

    @Override
    public void onEnable() {
        buildCommit = readBuildCommit();

        configManager = new NilumConfigManager(
                getDataFolder().toPath().resolve("config").resolve("main.yml"),
                "Nilum configuration",
                new ConfigSchema(List.of(
                        TcpConfig.BIND_ADDRESS, TcpConfig.PORT, TcpConfig.ADVERTISED_HOST,
                        HandshakeConfig.ALLOW_VANILLA_CLIENTS,
                        ModerationConfig.LOG_CLIENT_MODS, ModerationConfig.DISABLED_MODS,
                        ModerationConfig.DISABLED_KICK_MESSAGE,
                        LoggingConfig.DEBUG, LoggingConfig.WARNING, LoggingConfig.ERROR,
                        LoggingConfig.MODERATION, LoggingConfig.MAX_LOG_FILES,
                        DocsConfig.GENERATE_README,
                        HudTextConfig.ENABLED, HudTextConfig.UPDATE_INTERVAL_TICKS)),
                List.of());

        try {
            configManager.load();
        } catch (IOException e) {
            getLogger().severe("Failed to load Nilum's config, disabling: " + e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (configManager.get(DocsConfig.GENERATE_README)) {
            try {
                ReadmeGenerator.generate(getDataFolder().toPath());
            } catch (IOException e) {
                getLogger().warning("Failed to write README.md: " + e);
            }
        }

        logger = new NilumLogger(new PaperConsoleSink(), getDataFolder().toPath().resolve("logs").resolve("nilum.log"),
                LoggingConfig.destinationLookup(configManager), configManager.get(LoggingConfig.MAX_LOG_FILES));

        handshakeListener = new HandshakeListener(this, logger, configManager);

        modelRegistry = new ModelRegistry();
        iconRegistry = new IconRegistry();
        iconsYamlManager = new IconsYamlManager(getDataFolder().toPath().resolve("icons"), logger);
        hudAtlasRegistry = new HudAtlasRegistry();
        // Must exist before reloadHudAtlases() below; it calls hudTextService.refresh() to
        // re-index server_connector text elements every time the hud folder is (re)loaded.
        hudTextService = new HudTextService(this, logger);
        // Must exist before the first reload*() call below; each of those can trigger a manifest
        // broadcast, and combinedManifest() reads every registry unconditionally.
        shaderPackRegistry = new ShaderPackRegistry();
        fontRegistry = new FontRegistry();
        reloadModels();
        reloadIcons();
        reloadHudAtlases();
        modelDisplayService = new ModelDisplayService(this, logger, modelRegistry);
        customItemService = new CustomItemService(modelRegistry);
        iconItemService = new IconItemService(iconRegistry);
        hudAtlasService = new HudAtlasService(this);
        itemDefinitionRegistry = new ItemDefinitionRegistry(logger, customItemService, iconItemService);
        reloadItems();
        blockDefinitionRegistry = new BlockDefinitionRegistry(logger, modelRegistry);
        reloadBlocks();
        customBlockRegistry = new CustomBlockRegistry(blockDefinitionRegistry);
        reloadShaderPacks();
        shaderPackService = new ShaderPackService(this);
        reloadFonts();
        hudTextService.start(configManager.get(HudTextConfig.ENABLED), configManager.get(HudTextConfig.UPDATE_INTERVAL_TICKS));

        getServer().getMessenger().registerOutgoingPluginChannel(this, NilumChannels.HELLO_QUALIFIED);
        getServer().getMessenger().registerOutgoingPluginChannel(this, NilumChannels.TCP_OFFER_QUALIFIED);
        getServer().getMessenger().registerOutgoingPluginChannel(this, NilumChannels.MODEL_SPAWN_QUALIFIED);
        getServer().getMessenger().registerOutgoingPluginChannel(this, NilumChannels.ASSET_MANIFEST_QUALIFIED);
        getServer().getMessenger().registerOutgoingPluginChannel(this, NilumChannels.MOD_LIST_REQUEST_QUALIFIED);
        getServer().getMessenger().registerOutgoingPluginChannel(this, NilumChannels.HUD_FRAME_QUALIFIED);
        getServer().getMessenger().registerOutgoingPluginChannel(this, NilumChannels.ATLAS_PATCH_QUALIFIED);
        getServer().getMessenger().registerOutgoingPluginChannel(this, NilumChannels.HUD_FRAME_OVERRIDE_QUALIFIED);
        getServer().getMessenger().registerOutgoingPluginChannel(this, NilumChannels.HUD_FRAME_RELEASE_QUALIFIED);
        getServer().getMessenger().registerOutgoingPluginChannel(this, NilumChannels.REGISTER_CLIENT_VAR_QUALIFIED);
        getServer().getMessenger().registerOutgoingPluginChannel(this, NilumChannels.SET_CLIENT_VAR_QUALIFIED);
        getServer().getMessenger().registerOutgoingPluginChannel(this, NilumChannels.SET_HUD_TEXT_QUALIFIED);
        getServer().getMessenger().registerOutgoingPluginChannel(this, NilumChannels.CHUNK_BLOCKS_QUALIFIED);
        getServer().getMessenger().registerOutgoingPluginChannel(this, NilumChannels.ACTIVATE_SHADER_PACK_QUALIFIED);
        getServer().getMessenger().registerOutgoingPluginChannel(this, NilumChannels.DEACTIVATE_SHADER_PACK_QUALIFIED);
        getServer().getMessenger().registerIncomingPluginChannel(this, NilumChannels.HELLO_ACK_QUALIFIED, handshakeListener);
        getServer().getMessenger().registerIncomingPluginChannel(this, NilumChannels.TCP_UNAVAILABLE_QUALIFIED, handshakeListener);
        getServer().getMessenger().registerIncomingPluginChannel(this, NilumChannels.MOD_LIST_QUALIFIED, handshakeListener);
        getServer().getMessenger().registerIncomingPluginChannel(this, NilumChannels.KEYBIND_QUALIFIED, handshakeListener);
        getServer().getPluginManager().registerEvents(handshakeListener, this);
        getServer().getPluginManager().registerEvents(new CustomBlockChunkSync(this, customBlockRegistry), this);
        getServer().getPluginManager().registerEvents(
                new CustomBlockInteractionListener(this, customBlockRegistry, itemDefinitionRegistry), this);

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
            if (configManager.get(DocsConfig.GENERATE_README)) {
                ReadmeGenerator.generate(getDataFolder().toPath());
            }
            hudTextService.start(configManager.get(HudTextConfig.ENABLED), configManager.get(HudTextConfig.UPDATE_INTERVAL_TICKS));
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
            // loadDirectory() above wipes every model id, including synthetic ones generated from
            // block definitions' "textures:" section; regenerate those now so a models reload
            // doesn't silently break any block using default retexture mode. Null on first boot,
            // since this runs before blockDefinitionRegistry is constructed. reloadBlocks() sends
            // its own manifest broadcast, so only send one here if it didn't already.
            if (blockDefinitionRegistry != null) {
                reloadBlocks();
            } else {
                handshakeListener.broadcastAssetManifest();
            }
            return true;
        } catch (IOException e) {
            logger.error("Failed to reload the models folder", e);
            return false;
        }
    }

    /** @return true if the reload succeeded. */
    public boolean reloadIcons() {
        try {
            Path texturesDirectory = getDataFolder().toPath().resolve("textures");
            Files.createDirectories(texturesDirectory);

            Map<String, IconYamlConfig> resolved = iconsYamlManager.reload(modelRegistry);
            iconRegistry.clear();
            for (Map.Entry<String, IconYamlConfig> entry : resolved.entrySet()) {
                String iconId = entry.getKey();
                IconYamlConfig config = entry.getValue();
                Path textureFile = texturesDirectory.resolve(config.textureFileName());
                if (!Files.isRegularFile(textureFile)) {
                    logger.warn("Icon '" + iconId + "' references texture '" + config.textureFileName()
                            + "', which doesn't exist in the textures folder.");
                    continue;
                }
                iconRegistry.load(iconId, Files.readAllBytes(textureFile));
                iconRegistry.applyDisplay(iconId, config.display());
                TextureUsageTracker.recordUsage(texturesDirectory, config.textureFileName(), "icons");
            }
            logger.info("Loaded " + iconRegistry.iconIds().size() + " icon(s) from the icons folder.");
            handshakeListener.broadcastAssetManifest();
            return true;
        } catch (IOException e) {
            logger.error("Failed to reload the icons folder", e);
            return false;
        }
    }

    /** @return true if the reload succeeded. */
    public boolean reloadHudAtlases() {
        try {
            hudAtlasRegistry.loadDirectory(getDataFolder().toPath().resolve("hud"));
            logger.info("Loaded " + hudAtlasRegistry.atlasIds().size() + " HUD atlas(es) from the hud folder.");
            hudTextService.refresh();
            handshakeListener.broadcastAssetManifest();
            return true;
        } catch (IOException e) {
            logger.error("Failed to reload the hud folder", e);
            return false;
        }
    }

    /** @return true if the reload succeeded. */
    public boolean reloadItems() {
        try {
            itemDefinitionRegistry.loadDirectory(getDataFolder().toPath().resolve("items"));
            logger.info("Loaded " + itemDefinitionRegistry.itemIds().size() + " item definition(s) from the items folder.");
            return true;
        } catch (IOException e) {
            logger.error("Failed to reload the items folder", e);
            return false;
        }
    }

    /** @return true if the reload succeeded. */
    public boolean reloadBlocks() {
        try {
            blockDefinitionRegistry.loadDirectory(
                    getDataFolder().toPath().resolve("blocks"),
                    getDataFolder().toPath().resolve("textures"));
            logger.info("Loaded " + blockDefinitionRegistry.blockIds().size() + " block definition(s) from the blocks folder.");
            // Default-retexture blocks register synthetic models here, which the manifest needs
            // to reflect so already-connected clients pick up new/changed ones.
            handshakeListener.broadcastAssetManifest();
            return true;
        } catch (IOException e) {
            logger.error("Failed to reload the blocks folder", e);
            return false;
        }
    }

    /** @return true if the reload succeeded. */
    public boolean reloadShaderPacks() {
        try {
            shaderPackRegistry.loadDirectory(getDataFolder().toPath().resolve("shaderpacks"));
            logger.info("Loaded " + shaderPackRegistry.packIds().size() + " shader pack(s) from the shaderpacks folder.");
            handshakeListener.broadcastAssetManifest();
            return true;
        } catch (IOException e) {
            logger.error("Failed to reload the shaderpacks folder", e);
            return false;
        }
    }

    /** @return true if the reload succeeded. */
    public boolean reloadFonts() {
        try {
            fontRegistry.loadDirectory(getDataFolder().toPath().resolve("fonts"));
            logger.info("Loaded " + fontRegistry.fontIds().size() + " font(s) from the fonts folder.");
            handshakeListener.broadcastAssetManifest();
            return true;
        } catch (IOException e) {
            logger.error("Failed to reload the fonts folder", e);
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

    public HudAtlasRegistry hudAtlases() {
        return hudAtlasRegistry;
    }

    public HudAtlasService hud() {
        return hudAtlasService;
    }

    public CustomBlockRegistry customBlocks() {
        return customBlockRegistry;
    }

    public ShaderPackRegistry shaderPacks() {
        return shaderPackRegistry;
    }

    public ShaderPackService shaderPackService() {
        return shaderPackService;
    }

    public FontRegistry fonts() {
        return fontRegistry;
    }

    public BlockDefinitionRegistry blockDefinitions() {
        return blockDefinitionRegistry;
    }

    public ItemDefinitionRegistry items() {
        return itemDefinitionRegistry;
    }
}
