package io.github.r4t2.nilum.common.block;

import io.github.r4t2.nilum.common.logging.NilumLogger;
import io.github.r4t2.nilum.common.model.CubeModelGenerator;
import io.github.r4t2.nilum.common.model.ModelRegistry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** Loader-agnostic port of nilum-paper's BlockDefinitionRegistry, using the project's hand-rolled grammar instead of YamlConfiguration. */
public final class BlockDefinitionRegistry {

    /** Grammar key -> the bbmodel face key CubeModelGenerator expects. */
    private static final List<String[]> RETEXTURE_FACES = List.of(
            new String[]{"north", "north"},
            new String[]{"south", "south"},
            new String[]{"east", "east"},
            new String[]{"west", "west"},
            new String[]{"top", "up"},
            new String[]{"bottom", "down"}
    );

    private static final Pattern SECTION_HEADER = Pattern.compile("^(\\S[\\w.-]*):\\s*$");
    private static final Pattern FIELD_LINE = Pattern.compile("^\\s+(\\S[\\w.-]*):\\s*(.*)$");
    private static final Pattern TOP_FIELD = Pattern.compile("^(\\S[\\w.-]*):\\s*(\\S.*)$");

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
                    definitionsById.put(id, parse(id, Files.readString(file)));
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

    private BlockDefinition parse(String id, String source) {
        Map<String, String> topLevel = new LinkedHashMap<>();
        Map<String, Map<String, String>> sections = new LinkedHashMap<>();
        List<Map<String, String>> drops = new ArrayList<>();
        parseFile(source, topLevel, sections, drops);

        String modelId = topLevel.get("model");
        Map<String, String> texturesSection = sections.get("textures");

        if (modelId != null && !modelId.isBlank()) {
            if (modelRegistry.get(modelId).isEmpty()) {
                logger.warn("Block definition '" + id + "' references model '" + modelId + "', which isn't loaded (yet).");
            }
        } else if (texturesSection != null) {
            modelId = generateCubeModel(id, texturesSection);
        } else {
            throw new IllegalArgumentException("missing required 'model' or 'textures' field");
        }

        float hardness = topLevel.containsKey("hardness") ? Float.parseFloat(topLevel.get("hardness")) : 1.0f;
        float explosionResistance = topLevel.containsKey("explosion_resistance")
                ? Float.parseFloat(topLevel.get("explosion_resistance")) : hardness;

        return new BlockDefinition(id, modelId, hardness, explosionResistance, parseDrops(drops));
    }

    /** Top-level "key: value" fields, a unique "textures:" block, and repeatable "drop:" blocks collected into a list. */
    private void parseFile(String source, Map<String, String> topLevel, Map<String, Map<String, String>> sections,
                            List<Map<String, String>> drops) {
        Map<String, String> currentSection = null;
        for (String line : source.split("\n", -1)) {
            if (line.isBlank() || line.stripLeading().startsWith("#")) {
                continue;
            }

            Matcher field = FIELD_LINE.matcher(line);
            if (field.matches() && currentSection != null) {
                currentSection.put(field.group(1), unquote(field.group(2).trim()));
                continue;
            }

            Matcher topField = TOP_FIELD.matcher(line);
            if (topField.matches()) {
                currentSection = null;
                topLevel.put(topField.group(1), unquote(topField.group(2).trim()));
                continue;
            }

            Matcher section = SECTION_HEADER.matcher(line);
            if (section.matches()) {
                currentSection = new LinkedHashMap<>();
                if (section.group(1).equals("drop")) {
                    drops.add(currentSection);
                } else {
                    sections.put(section.group(1), currentSection);
                }
            }
        }
    }

    /** "count:" is either a bare number or a "min-max" range; chance defaults to 1.0, count defaults to 1. */
    private List<BlockDropEntry> parseDrops(List<Map<String, String>> raw) {
        List<BlockDropEntry> entries = new ArrayList<>();
        for (Map<String, String> dropFields : raw) {
            String itemId = dropFields.get("item");
            if (itemId == null || itemId.isBlank()) {
                continue;
            }
            double chance = dropFields.containsKey("chance") ? Double.parseDouble(dropFields.get("chance")) : 1.0;

            int minCount = 1;
            int maxCount = 1;
            String count = dropFields.get("count");
            if (count != null) {
                int dash = count.indexOf('-');
                if (dash > 0) {
                    minCount = Integer.parseInt(count.substring(0, dash).trim());
                    maxCount = Integer.parseInt(count.substring(dash + 1).trim());
                } else {
                    minCount = maxCount = Integer.parseInt(count.trim());
                }
            }
            entries.add(new BlockDropEntry(itemId, chance, minCount, maxCount));
        }
        return entries;
    }

    /** "Default retexture" mode: a full 0..16 cube reskinned per face, generating the same synthetic id per block id. */
    private String generateCubeModel(String id, Map<String, String> texturesSection) {
        Map<String, byte[]> bytesByFileName = new HashMap<>();
        Map<String, byte[]> facePngBytes = new LinkedHashMap<>();

        for (String[] faceEntry : RETEXTURE_FACES) {
            String key = faceEntry[0];
            String bbFaceKey = faceEntry[1];
            String fileName = resolveTextureFileName(texturesSection, key);
            if (fileName == null) {
                throw new IllegalArgumentException("block definition '" + id + "' textures section has no texture "
                        + "for face '" + key + "' (checked '" + key + "', 'side', 'all')");
            }
            byte[] bytes = bytesByFileName.computeIfAbsent(fileName, name -> readTexture(id, name));
            facePngBytes.put(bbFaceKey, bytes);
        }

        String syntheticId = "__cube_" + id;
        modelRegistry.registerSynthetic(syntheticId, CubeModelGenerator.generate(facePngBytes));
        return syntheticId;
    }

    private static String resolveTextureFileName(Map<String, String> textures, String key) {
        String specific = textures.get(key);
        if (specific != null && !specific.isBlank()) {
            return specific;
        }
        if (key.equals("north") || key.equals("south") || key.equals("east") || key.equals("west")) {
            String side = textures.get("side");
            if (side != null && !side.isBlank()) {
                return side;
            }
        }
        String all = textures.get("all");
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

    private static String unquote(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static String stripExtension(String fileName) {
        return fileName.substring(0, fileName.length() - ".yml".length());
    }
}
