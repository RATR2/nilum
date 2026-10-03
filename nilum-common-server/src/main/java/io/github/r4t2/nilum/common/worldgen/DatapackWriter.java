package io.github.r4t2.nilum.common.worldgen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Generates a real vanilla datapack from BiomeDefinition/DimensionDefinition
 * instances: biome/dimension/dimension_type JSON plus pack.mcmeta. No NMS/Paper types here on
 * purpose, so a Fabric/NeoForge-hosted server can reuse it later.
 *
 * <p>Unlike every other Nilum reload, writing these files does <b>not</b> take effect live:
 * Minecraft only reads worldgen registries (biomes, dimensions, dimension types) once, from
 * datapacks, at server bootstrap. A restart is required after regenerating.
 *
 * <p>Phase 1 generates dimensions with a flat generator (stacked layers, using
 * DimensionDefinition.biomeIds()'s first entry as the fixed biome source) rather than
 * noise-based terrain, to avoid needing vanilla's surface-rule/density-function machinery.
 * Placing Nilum's own custom blocks during generation is a separate follow-up (see ROADMAP.md).
 */
public final class DatapackWriter {

    /** pack_format for the targeted Minecraft version (see README/build.gradle.kts); verify
     * against the game's current data-driven-pack format before relying on this in production. */
    private static final int PACK_FORMAT = 71;

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private DatapackWriter() {
    }

    /** Regenerates the whole nilum_worldgen datapack folder from scratch for the given definitions. */
    public static int writeAll(Path datapackRoot, WorldgenDefinitionRegistry registry) throws IOException {
        deleteRecursively(datapackRoot);

        DatapackWriter writer = new DatapackWriter();
        writer.writePackMcmeta(datapackRoot);

        int count = 0;
        for (BiomeDefinition biome : registry.biomes()) {
            writer.writeBiome(datapackRoot, biome);
            count++;
        }
        for (DimensionDefinition dimension : registry.dimensions()) {
            writer.writeDimensionType(datapackRoot, dimension);
            writer.writeDimension(datapackRoot, dimension);
            count++;
        }
        return count;
    }

    private void writePackMcmeta(Path datapackRoot) throws IOException {
        JsonObject pack = new JsonObject();
        pack.addProperty("pack_format", PACK_FORMAT);
        pack.addProperty("description", "Nilum-generated custom biomes and dimensions");
        JsonObject root = new JsonObject();
        root.add("pack", pack);
        write(datapackRoot.resolve("pack.mcmeta"), root);
    }

    private void writeBiome(Path datapackRoot, BiomeDefinition biome) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("has_precipitation", biome.hasPrecipitation());
        root.addProperty("temperature", biome.temperature());
        root.addProperty("downfall", biome.downfall());

        JsonObject effects = new JsonObject();
        effects.addProperty("fog_color", biome.fogColor());
        effects.addProperty("water_color", biome.waterColor());
        effects.addProperty("water_fog_color", biome.waterFogColor());
        effects.addProperty("sky_color", biome.skyColor());
        if (biome.grassColor() != null) {
            effects.addProperty("grass_color", biome.grassColor());
        }
        if (biome.foliageColor() != null) {
            effects.addProperty("foliage_color", biome.foliageColor());
        }
        JsonObject moodSound = new JsonObject();
        moodSound.addProperty("sound", "minecraft:ambient.cave");
        moodSound.addProperty("tick_delay", 6000);
        moodSound.addProperty("block_search_extent", 8);
        moodSound.addProperty("offset", 2.0);
        effects.add("mood_sound", moodSound);
        root.add("effects", effects);

        root.add("carvers", new JsonArray());
        JsonArray features = new JsonArray();
        for (int i = 0; i < 11; i++) {
            features.add(new JsonArray());
        }
        root.add("features", features);

        Map<String, JsonArray> spawnersByCategory = new LinkedHashMap<>();
        for (BiomeSpawnEntry spawner : biome.spawners()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("type", spawner.entityId());
            entry.addProperty("weight", spawner.weight());
            entry.addProperty("minCount", spawner.minCount());
            entry.addProperty("maxCount", spawner.maxCount());
            spawnersByCategory.computeIfAbsent(spawner.category(), c -> new JsonArray()).add(entry);
        }
        JsonObject spawners = new JsonObject();
        spawnersByCategory.forEach(spawners::add);
        root.add("spawners", spawners);
        root.add("spawn_costs", new JsonObject());

        write(biomePath(datapackRoot, biome.id()), root);
    }

    private void writeDimensionType(Path datapackRoot, DimensionDefinition dimension) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("ultrawarm", dimension.ultrawarm());
        root.addProperty("natural", dimension.natural());
        root.addProperty("piglin_safe", dimension.piglinSafe());
        root.addProperty("respawn_anchor_works", dimension.respawnAnchorWorks());
        root.addProperty("bed_works", dimension.bedWorks());
        root.addProperty("has_raids", false);
        root.addProperty("has_skylight", dimension.hasSkylight());
        root.addProperty("has_ceiling", dimension.hasCeiling());
        root.addProperty("coordinate_scale", 1.0);
        root.addProperty("ambient_light", dimension.ambientLight());
        root.addProperty("logical_height", dimension.logicalHeight());
        root.addProperty("min_y", dimension.minY());
        root.addProperty("height", dimension.height());
        root.addProperty("infiniburn", dimension.infiniburn());
        root.addProperty("effects", dimension.effects());
        root.addProperty("monster_spawn_light_level", 7);
        root.addProperty("monster_spawn_block_light_limit", 0);

        write(dimensionTypePath(datapackRoot, dimension.id()), root);
    }

    private void writeDimension(Path datapackRoot, DimensionDefinition dimension) throws IOException {
        JsonObject settings = new JsonObject();
        settings.addProperty("biome", "nilum:" + dimension.biomeIds().get(0));
        settings.addProperty("lakes", false);
        settings.addProperty("features", false);
        JsonArray layers = new JsonArray();
        for (FlatLayer layer : dimension.layers()) {
            JsonObject layerJson = new JsonObject();
            layerJson.addProperty("block", layer.block());
            layerJson.addProperty("height", layer.height());
            layers.add(layerJson);
        }
        settings.add("layers", layers);

        JsonObject generator = new JsonObject();
        generator.addProperty("type", "minecraft:flat");
        generator.add("settings", settings);

        JsonObject root = new JsonObject();
        root.addProperty("type", "nilum:" + dimension.id());
        root.add("generator", generator);

        write(dimensionPath(datapackRoot, dimension.id()), root);
    }

    private Path biomePath(Path datapackRoot, String id) {
        return datapackRoot.resolve("data").resolve("nilum").resolve("worldgen").resolve("biome").resolve(id + ".json");
    }

    private Path dimensionTypePath(Path datapackRoot, String id) {
        return datapackRoot.resolve("data").resolve("nilum").resolve("dimension_type").resolve(id + ".json");
    }

    private Path dimensionPath(Path datapackRoot, String id) {
        return datapackRoot.resolve("data").resolve("nilum").resolve("dimension").resolve(id + ".json");
    }

    private void write(Path file, JsonObject json) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, gson.toJson(json), StandardCharsets.UTF_8);
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }
}
