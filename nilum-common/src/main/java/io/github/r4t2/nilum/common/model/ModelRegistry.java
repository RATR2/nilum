package io.github.r4t2.nilum.common.model;

import io.github.r4t2.nilum.common.protocol.AssetKind;
import io.github.r4t2.nilum.common.protocol.AssetManifestEntry;
import io.github.r4t2.nilum.common.util.SHA256;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public final class ModelRegistry {

    private final Map<String, BbModel> modelsById = new ConcurrentHashMap<>();
    private final Map<String, byte[]> rawBytesById = new ConcurrentHashMap<>();
    private final Map<String, String> hashById = new ConcurrentHashMap<>();

    public List<ModelLoadError> loadDirectory(Path directory) throws IOException {
        modelsById.clear();
        rawBytesById.clear();
        hashById.clear();
        List<ModelLoadError> errors = new ArrayList<>();

        Files.createDirectories(directory);

        try (Stream<Path> files = Files.list(directory)) {
            for (Path file : files.filter(ModelRegistry::isBbModelFile).toList()) {
                String modelId = stripExtension(file.getFileName().toString());
                try {
                    byte[] rawBytes = Files.readAllBytes(file);
                    modelsById.put(modelId, BbModelParser.parse(new String(rawBytes, StandardCharsets.UTF_8)));
                    rawBytesById.put(modelId, rawBytes);
                    hashById.put(modelId, SHA256.of(rawBytes));
                } catch (Exception e) {
                    errors.add(new ModelLoadError(file.getFileName().toString(), e));
                }
            }
        }

        return errors;
    }

    /**
     * Registers a model built from bytes that didn't come from a file on disk (e.g. a synthesized
     * cube model), in the same map slots as a real .bbmodel. loadDirectory clears these on every
     * rescan; callers depending on a synthetic id surviving a reload must re-register it after.
     */
    public void registerSynthetic(String modelId, byte[] rawBytes) {
        modelsById.put(modelId, BbModelParser.parse(new String(rawBytes, StandardCharsets.UTF_8)));
        rawBytesById.put(modelId, rawBytes);
        hashById.put(modelId, SHA256.of(rawBytes));
    }

    public Optional<BbModel> get(String modelId) {
        return Optional.ofNullable(modelsById.get(modelId));
    }

    public Optional<byte[]> rawBytes(String modelId) {
        return Optional.ofNullable(rawBytesById.get(modelId));
    }

    public Set<String> modelIds() {
        return Set.copyOf(modelsById.keySet());
    }

    public List<AssetManifestEntry> manifest() {
        List<AssetManifestEntry> entries = new ArrayList<>();
        for (Map.Entry<String, String> entry : hashById.entrySet()) {
            entries.add(new AssetManifestEntry(entry.getKey(), entry.getValue(), AssetKind.MODEL));
        }
        return entries;
    }

    private static boolean isBbModelFile(Path path) {
        return Files.isRegularFile(path) && path.getFileName().toString().endsWith(".bbmodel");
    }

    private static String stripExtension(String fileName) {
        return fileName.substring(0, fileName.length() - ".bbmodel".length());
    }
}
