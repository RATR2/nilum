package io.github.r4t2.nilum.paper.block;

import io.github.r4t2.nilum.common.logging.NilumLogger;
import io.github.r4t2.nilum.common.model.CubeModelGenerator;
import io.github.r4t2.nilum.common.model.ModelRegistry;
import io.github.r4t2.nilum.paper.item.DropEntry;
import io.github.r4t2.nilum.paper.texture.TextureUsageTracker;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/** Per-file Nilum block types: blocks/<id>.yml, same convention as icons/items. */
public final class BlockDefinitionRegistry {

    /** YAML key -> the bbmodel face key CubeModelGenerator expects. */
    private static final List<String[]> RETEXTURE_FACES = List.of(
            new String[]{"north", "north"},
            new String[]{"south", "south"},
            new String[]{"east", "east"},
            new String[]{"west", "west"},
            new String[]{"top", "up"},
            new String[]{"bottom", "down"}
    );

    private final NilumLogger logger;
    private final ModelRegistry modelRegistry;

    private final Map<String, BlockDefinition> definitionsById = new ConcurrentHashMap<>();
    private Path texturesDirectory;

    public BlockDefinitionRegistry(NilumLogger logger, ModelRegistry modelRegistry) {
        this.logger = logger;
        this.modelRegistry = modelRegistry;
    }

    public void loadDirectory(Path directory, Path texturesDirectory) throws IOException {
        definitionsById.clear();
        Files.createDirectories(directory);
        Files.createDirectories(texturesDirectory);
        this.texturesDirectory = texturesDirectory;

        try (Stream<Path> files = Files.list(directory)) {
            for (Path file : files.filter(p -> p.getFileName().toString().endsWith(".yml")).toList()) {
                String id = stripExtension(file.getFileName().toString());
                try {
                    definitionsById.put(id, parse(id, YamlConfiguration.loadConfiguration(file.toFile())));
                } catch (RuntimeException e) {
                    logger.warn("Failed to load block definition '" + id + "': " + e);
                }
            }
        }
    }

    public Optional<BlockDefinition> get(String id) {
        return Optional.ofNullable(definitionsById.get(id));
    }

    public Set<String> blockIds() {
        return Set.copyOf(definitionsById.keySet());
    }

    private BlockDefinition parse(String id, ConfigurationSection section) {
        String modelId = section.getString("model");
        ConfigurationSection texturesSection = section.getConfigurationSection("textures");

        if (modelId != null && !modelId.isBlank()) {
            if (modelRegistry.get(modelId).isEmpty()) {
                logger.warn("Block definition '" + id + "' references model '" + modelId + "', which isn't loaded (yet).");
            }
        } else if (texturesSection != null) {
            modelId = generateCubeModel(id, texturesSection);
        } else {
            throw new IllegalArgumentException("missing required 'model' or 'textures' field");
        }

        ConfigurationSection proxySection = section.getConfigurationSection("proxy");
        if (proxySection == null) {
            throw new IllegalArgumentException("missing required 'proxy' section");
        }
        BlockProxy proxy = parseProxy(id, proxySection);

        List<DropEntry> drops = DropEntry.parseList(section.getMapList("drops"));

        return new BlockDefinition(id, modelId, proxy, drops);
    }

    private BlockProxy parseProxy(String id, ConfigurationSection proxySection) {
        String mode = proxySection.getString("mode", "custom");
        return switch (mode) {
            case "vanilla" -> {
                String materialName = proxySection.getString("vanilla_block");
                Material material = materialName != null ? Material.matchMaterial(materialName) : null;
                if (material == null) {
                    throw new IllegalArgumentException("block definition '" + id
                            + "' has proxy.mode: vanilla but an invalid/missing 'vanilla_block'");
                }
                yield new BlockProxy.Vanilla(material);
            }
            case "custom" -> {
                String wireBlockName = proxySection.getString("wire_block");
                // Default to glass, not a fully-opaque block: the wire block's real occlusion
                // shape drives vanilla's neighbor face culling regardless of our own render pass
                // suppressing its visual, and most custom models don't fill the full 1x1x1 voxel.
                // An opaque default would silently punch "see-through" holes in the world wherever
                // the model doesn't cover the culled faces. Admins whose model IS a full cube can
                // still opt into an opaque wire block explicitly.
                Material wireBlock = wireBlockName != null ? Material.matchMaterial(wireBlockName) : Material.GLASS;
                if (wireBlock == null) {
                    throw new IllegalArgumentException("block definition '" + id
                            + "' has an invalid proxy.wire_block '" + wireBlockName + "'");
                }
                if (wireBlockName == null) {
                    logger.warn("Block definition '" + id + "' has proxy.mode: custom with no 'wire_block' set, "
                            + "defaulting to glass - pick a non-occluding material whose real hardness roughly "
                            + "matches break_time_seconds, or an opaque one only if your model fills the full block.");
                }
                yield new BlockProxy.Custom(wireBlock,
                        proxySection.getDouble("break_time_seconds", 1.0),
                        proxySection.getBoolean("explosion_resistant", false));
            }
            default -> throw new IllegalArgumentException("block definition '" + id
                    + "' has unknown proxy.mode '" + mode + "' (expected 'vanilla' or 'custom')");
        };
    }

    /** "Default retexture" mode: a full 0..16 cube reskinned per face, generating the same synthetic id per block id. */
    private String generateCubeModel(String id, ConfigurationSection texturesSection) {
        Map<String, byte[]> bytesByFileName = new HashMap<>();
        Map<String, byte[]> facePngBytes = new LinkedHashMap<>();

        for (String[] faceEntry : RETEXTURE_FACES) {
            String yamlKey = faceEntry[0];
            String bbFaceKey = faceEntry[1];
            String fileName = resolveTextureFileName(texturesSection, yamlKey);
            if (fileName == null) {
                throw new IllegalArgumentException("block definition '" + id + "' textures section has no texture "
                        + "for face '" + yamlKey + "' (checked '" + yamlKey + "', 'side', 'all')");
            }
            byte[] bytes = bytesByFileName.computeIfAbsent(fileName, name -> readTexture(id, name));
            TextureUsageTracker.recordUsage(texturesDirectory, fileName, "blocks");
            facePngBytes.put(bbFaceKey, bytes);
        }

        String syntheticId = "__cube_" + id;
        modelRegistry.registerSynthetic(syntheticId, CubeModelGenerator.generate(facePngBytes));
        return syntheticId;
    }

    private static String resolveTextureFileName(ConfigurationSection textures, String yamlKey) {
        String specific = textures.getString(yamlKey);
        if (specific != null && !specific.isBlank()) {
            return specific;
        }
        if (yamlKey.equals("north") || yamlKey.equals("south") || yamlKey.equals("east") || yamlKey.equals("west")) {
            String side = textures.getString("side");
            if (side != null && !side.isBlank()) {
                return side;
            }
        }
        String all = textures.getString("all");
        return (all != null && !all.isBlank()) ? all : null;
    }

    private byte[] readTexture(String id, String fileName) {
        try {
            return Files.readAllBytes(texturesDirectory.resolve(fileName));
        } catch (IOException e) {
            throw new IllegalArgumentException("block definition '" + id + "' references texture '" + fileName
                    + "', which couldn't be read from the textures folder", e);
        }
    }

    private static String stripExtension(String fileName) {
        return fileName.substring(0, fileName.length() - ".yml".length());
    }
}
