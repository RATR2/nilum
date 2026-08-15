package io.github.r4t2.nilum.common.hosting;

import io.github.r4t2.nilum.common.block.BlockDefinitionRegistry;
import io.github.r4t2.nilum.common.font.FontRegistry;
import io.github.r4t2.nilum.common.hud.HudAtlasRegistry;
import io.github.r4t2.nilum.common.icon.IconFileConfig;
import io.github.r4t2.nilum.common.icon.IconRegistry;
import io.github.r4t2.nilum.common.icon.IconsFileManager;
import io.github.r4t2.nilum.common.logging.NilumLogger;
import io.github.r4t2.nilum.common.model.ModelLoadError;
import io.github.r4t2.nilum.common.model.ModelRegistry;
import io.github.r4t2.nilum.common.protocol.AssetKind;
import io.github.r4t2.nilum.common.protocol.AssetManifestEntry;
import io.github.r4t2.nilum.common.shader.ShaderPackRegistry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Bundles the loader-agnostic asset registries a Fabric- or NeoForge-hosted server serves, mirroring nilum-paper's HandshakeListener. */
public final class NilumAssetHost {

    private final ModelRegistry models = new ModelRegistry();
    private final IconRegistry icons = new IconRegistry();
    private final HudAtlasRegistry hudAtlases = new HudAtlasRegistry();
    private final ShaderPackRegistry shaderPacks = new ShaderPackRegistry();
    private final FontRegistry fonts = new FontRegistry();
    private final BlockDefinitionRegistry blocks;

    private final Path modelsDirectory;
    private final IconsFileManager iconsFileManager;
    private final Path hudDirectory;
    private final Path texturesDirectory;
    private final Path shaderPacksDirectory;
    private final Path fontsDirectory;
    private final Path blocksDirectory;
    private final NilumLogger logger;

    public NilumAssetHost(Path configDir, NilumLogger logger) {
        this.modelsDirectory = configDir.resolve("models");
        this.iconsFileManager = new IconsFileManager(configDir.resolve("icons"), logger);
        this.hudDirectory = configDir.resolve("hud");
        this.texturesDirectory = configDir.resolve("textures");
        this.shaderPacksDirectory = configDir.resolve("shaderpacks");
        this.fontsDirectory = configDir.resolve("fonts");
        this.blocksDirectory = configDir.resolve("blocks");
        this.logger = logger;
        this.blocks = new BlockDefinitionRegistry(logger, models);
    }

    /** Loads (or reloads) every registry from disk; a folder that fails to load keeps its previous contents. */
    public void loadAll() {
        try {
            for (ModelLoadError error : models.loadDirectory(modelsDirectory)) {
                logger.warn("Failed to load model '" + error.fileName() + "': " + error.cause());
            }
        } catch (IOException e) {
            logger.error("Failed to load the models folder", e);
        }

        loadIcons();

        try {
            blocks.loadDirectory(blocksDirectory, texturesDirectory);
        } catch (IOException e) {
            logger.error("Failed to load the blocks folder", e);
        }

        try {
            hudAtlases.loadDirectory(hudDirectory, texturesDirectory, logger::warn);
        } catch (IOException e) {
            logger.error("Failed to load the hud folder", e);
        }

        try {
            shaderPacks.loadDirectory(shaderPacksDirectory);
        } catch (IOException e) {
            logger.error("Failed to load the shaderpacks folder", e);
        }

        try {
            fonts.loadDirectory(fontsDirectory);
        } catch (IOException e) {
            logger.error("Failed to load the fonts folder", e);
        }
    }

    private void loadIcons() {
        try {
            Files.createDirectories(texturesDirectory);

            Map<String, IconFileConfig> resolved = iconsFileManager.reload(models);
            icons.clear();
            for (Map.Entry<String, IconFileConfig> entry : resolved.entrySet()) {
                String iconId = entry.getKey();
                IconFileConfig config = entry.getValue();
                Path textureFile = texturesDirectory.resolve(config.textureFileName());
                if (!Files.isRegularFile(textureFile)) {
                    logger.warn("Icon '" + iconId + "' references texture '" + config.textureFileName()
                            + "', which doesn't exist in the textures folder.");
                    continue;
                }
                icons.load(iconId, Files.readAllBytes(textureFile));
                icons.applyDisplay(iconId, config.display());
            }
        } catch (IOException e) {
            logger.error("Failed to load the icons folder", e);
        }
    }

    public List<AssetManifestEntry> manifest() {
        List<AssetManifestEntry> entries = new ArrayList<>(models.manifest());
        entries.addAll(icons.manifest());
        entries.addAll(hudAtlases.manifest());
        entries.addAll(shaderPacks.manifest());
        entries.addAll(fonts.manifest());
        return entries;
    }

    /** Raw bytes for one asset over the TCP side-channel; null if unknown. */
    public byte[] assetBytes(AssetKind kind, String id) {
        return switch (kind) {
            case MODEL -> models.rawBytes(id).orElse(null);
            case ICON -> icons.assetBytes(id).orElse(null);
            case HUD_ATLAS -> hudAtlases.assetBytes(id).orElse(null);
            case SHADER_PACK -> shaderPacks.rawBytes(id).orElse(null);
            case FONT -> fonts.rawBytes(id).orElse(null);
        };
    }

    public ModelRegistry models() {
        return models;
    }

    public IconRegistry icons() {
        return icons;
    }

    public HudAtlasRegistry hudAtlases() {
        return hudAtlases;
    }

    public ShaderPackRegistry shaderPacks() {
        return shaderPacks;
    }

    public FontRegistry fonts() {
        return fonts;
    }

    public BlockDefinitionRegistry blocks() {
        return blocks;
    }
}
