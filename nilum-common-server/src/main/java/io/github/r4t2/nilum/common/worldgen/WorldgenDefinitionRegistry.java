package io.github.r4t2.nilum.common.worldgen;

import io.github.r4t2.nilum.common.logging.NilumLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Per-file Nilum biome/dimension definitions: biomes/<id>.yml and dimensions/<id>.yml, same
 * hand-rolled grammar and loading convention as BlockDefinitionRegistry.
 * Loader-agnostic by design so a Fabric/NeoForge-hosted server can reuse it later; only
 * DatapackWriter needs to actually act on what it loads.
 */
public final class WorldgenDefinitionRegistry {

    private static final Pattern SECTION_HEADER = Pattern.compile("^(\\S[\\w.-]*):\\s*$");
    private static final Pattern FIELD_LINE = Pattern.compile("^\\s+(\\S[\\w.-]*):\\s*(.*)$");
    private static final Pattern TOP_FIELD = Pattern.compile("^(\\S[\\w.-]*):\\s*(\\S.*)$");

    private final NilumLogger logger;
    private final Map<String, BiomeDefinition> biomesById = new ConcurrentHashMap<>();
    private final Map<String, DimensionDefinition> dimensionsById = new ConcurrentHashMap<>();

    public WorldgenDefinitionRegistry(NilumLogger logger) {
        this.logger = logger;
    }

    public void loadBiomes(Path directory) throws IOException {
        biomesById.clear();
        Files.createDirectories(directory);
        try (Stream<Path> files = Files.list(directory)) {
            for (Path file : files.filter(p -> p.getFileName().toString().endsWith(".yml")).toList()) {
                String id = stripExtension(file.getFileName().toString());
                try {
                    biomesById.put(id, parseBiome(id, Files.readString(file)));
                } catch (RuntimeException e) {
                    logger.warn("Failed to load biome definition '" + id + "': " + e);
                }
            }
        }
    }

    public void loadDimensions(Path directory) throws IOException {
        dimensionsById.clear();
        Files.createDirectories(directory);
        try (Stream<Path> files = Files.list(directory)) {
            for (Path file : files.filter(p -> p.getFileName().toString().endsWith(".yml")).toList()) {
                String id = stripExtension(file.getFileName().toString());
                try {
                    dimensionsById.put(id, parseDimension(id, Files.readString(file)));
                } catch (RuntimeException e) {
                    logger.warn("Failed to load dimension definition '" + id + "': " + e);
                }
            }
        }
    }

    public Optional<BiomeDefinition> biome(String id) {
        return Optional.ofNullable(biomesById.get(id));
    }

    public Set<String> biomeIds() {
        return Set.copyOf(biomesById.keySet());
    }

    public Collection<BiomeDefinition> biomes() {
        return List.copyOf(biomesById.values());
    }

    public Optional<DimensionDefinition> dimension(String id) {
        return Optional.ofNullable(dimensionsById.get(id));
    }

    public Set<String> dimensionIds() {
        return Set.copyOf(dimensionsById.keySet());
    }

    public Collection<DimensionDefinition> dimensions() {
        return List.copyOf(dimensionsById.values());
    }

    private BiomeDefinition parseBiome(String id, String source) {
        Map<String, String> topLevel = new LinkedHashMap<>();
        List<Map<String, String>> spawnSections = new ArrayList<>();
        parseFile(source, topLevel, spawnSections, "spawn");

        boolean precipitation = parseBoolean(topLevel, "precipitation", true);
        float temperature = parseFloat(topLevel, "temperature", 0.8f);
        float downfall = parseFloat(topLevel, "downfall", 0.4f);
        int fogColor = parseColor(topLevel, "fog_color", "#C0D8FF");
        int waterColor = parseColor(topLevel, "water_color", "#3F76E4");
        int waterFogColor = parseColor(topLevel, "water_fog_color", "#050533");
        int skyColor = parseColor(topLevel, "sky_color", "#78A7FF");
        Integer grassColor = topLevel.containsKey("grass_color") ? parseColor(topLevel, "grass_color", "#000000") : null;
        Integer foliageColor = topLevel.containsKey("foliage_color") ? parseColor(topLevel, "foliage_color", "#000000") : null;

        List<BiomeSpawnEntry> spawners = new ArrayList<>();
        for (Map<String, String> fields : spawnSections) {
            String entityId = fields.get("entity");
            if (entityId == null || entityId.isBlank()) {
                throw new IllegalArgumentException("a 'spawn:' entry is missing required 'entity'");
            }
            String category = fields.getOrDefault("category", "monster");
            int weight = fields.containsKey("weight") ? Integer.parseInt(fields.get("weight")) : 1;
            int minCount = fields.containsKey("min_count") ? Integer.parseInt(fields.get("min_count")) : 1;
            int maxCount = fields.containsKey("max_count") ? Integer.parseInt(fields.get("max_count")) : 1;
            spawners.add(new BiomeSpawnEntry(category, entityId, weight, minCount, maxCount));
        }

        return new BiomeDefinition(id, precipitation, temperature, downfall, fogColor, waterColor,
                waterFogColor, skyColor, grassColor, foliageColor, spawners);
    }

    private DimensionDefinition parseDimension(String id, String source) {
        Map<String, String> topLevel = new LinkedHashMap<>();
        parseFile(source, topLevel, new ArrayList<>(), "spawn");

        String biomesField = topLevel.get("biomes");
        if (biomesField == null || biomesField.isBlank()) {
            throw new IllegalArgumentException("missing required 'biomes' field (comma-separated biome ids)");
        }
        List<String> biomeIds = List.of(biomesField.split("\\s*,\\s*"));

        String layersField = topLevel.getOrDefault("layers", "minecraft:bedrock:1,minecraft:stone:62,minecraft:dirt:3,minecraft:grass_block:1");
        List<FlatLayer> layers = new ArrayList<>();
        for (String entry : layersField.split("\\s*,\\s*")) {
            int lastColon = entry.lastIndexOf(':');
            if (lastColon <= 0) {
                throw new IllegalArgumentException("'layers' entry '" + entry + "' isn't 'block:id:height'");
            }
            layers.add(new FlatLayer(entry.substring(0, lastColon), Integer.parseInt(entry.substring(lastColon + 1))));
        }

        return new DimensionDefinition(
                id,
                biomeIds,
                parseBoolean(topLevel, "has_skylight", true),
                parseBoolean(topLevel, "has_ceiling", false),
                parseBoolean(topLevel, "ultrawarm", false),
                parseBoolean(topLevel, "natural", true),
                parseBoolean(topLevel, "bed_works", true),
                parseBoolean(topLevel, "respawn_anchor_works", false),
                parseBoolean(topLevel, "piglin_safe", false),
                topLevel.containsKey("ambient_light") ? Double.parseDouble(topLevel.get("ambient_light")) : 0.0,
                topLevel.containsKey("min_y") ? Integer.parseInt(topLevel.get("min_y")) : -64,
                topLevel.containsKey("height") ? Integer.parseInt(topLevel.get("height")) : 384,
                topLevel.containsKey("logical_height") ? Integer.parseInt(topLevel.get("logical_height")) : 384,
                topLevel.getOrDefault("infiniburn", "#minecraft:infiniburn_overworld"),
                topLevel.getOrDefault("effects", "minecraft:overworld"),
                layers
        );
    }

    /** Top-level "key: value" fields and repeatable "<repeatableSection>:" blocks collected into a list. */
    private void parseFile(String source, Map<String, String> topLevel, List<Map<String, String>> repeatable, String repeatableSection) {
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
                if (section.group(1).equals(repeatableSection)) {
                    repeatable.add(currentSection);
                }
            }
        }
    }

    private static boolean parseBoolean(Map<String, String> fields, String key, boolean fallback) {
        return fields.containsKey(key) ? Boolean.parseBoolean(fields.get(key)) : fallback;
    }

    private static float parseFloat(Map<String, String> fields, String key, float fallback) {
        return fields.containsKey(key) ? Float.parseFloat(fields.get(key)) : fallback;
    }

    private static int parseColor(Map<String, String> fields, String key, String fallbackHex) {
        String hex = fields.getOrDefault(key, fallbackHex);
        String digits = hex.startsWith("#") ? hex.substring(1) : hex;
        return Integer.parseInt(digits, 16);
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
