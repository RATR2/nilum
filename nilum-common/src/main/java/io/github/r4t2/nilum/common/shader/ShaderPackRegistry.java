package io.github.r4t2.nilum.common.shader;

import io.github.r4t2.nilum.common.protocol.AssetKind;
import io.github.r4t2.nilum.common.protocol.AssetManifestEntry;
import io.github.r4t2.nilum.common.util.SHA256;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Server-side registry of Iris/OptiFine-format shaderpack zips (shaderpacks/<id>.zip). Streamed
 * to the client the same way models/icons are, then written into Iris's own shaderpacks folder
 * so Iris can load them through its normal, unmodified pipeline.
 */
public final class ShaderPackRegistry {

    private final Map<String, byte[]> rawBytesById = new ConcurrentHashMap<>();
    private final Map<String, String> hashById = new ConcurrentHashMap<>();

    public void loadDirectory(Path directory) throws IOException {
        rawBytesById.clear();
        hashById.clear();
        Files.createDirectories(directory);

        try (Stream<Path> files = Files.list(directory)) {
            for (Path file : files.filter(ShaderPackRegistry::isZipFile).toList()) {
                String id = stripExtension(file.getFileName().toString());
                byte[] rawBytes = Files.readAllBytes(file);
                rawBytesById.put(id, rawBytes);
                hashById.put(id, SHA256.of(rawBytes));
            }
        }
    }

    public Optional<byte[]> rawBytes(String id) {
        return Optional.ofNullable(rawBytesById.get(id));
    }

    public Set<String> packIds() {
        return Set.copyOf(rawBytesById.keySet());
    }

    public List<AssetManifestEntry> manifest() {
        List<AssetManifestEntry> entries = new ArrayList<>();
        for (Map.Entry<String, String> entry : hashById.entrySet()) {
            entries.add(new AssetManifestEntry(entry.getKey(), entry.getValue(), AssetKind.SHADER_PACK));
        }
        return entries;
    }

    private static boolean isZipFile(Path path) {
        return Files.isRegularFile(path) && path.getFileName().toString().endsWith(".zip");
    }

    private static String stripExtension(String fileName) {
        return fileName.substring(0, fileName.length() - ".zip".length());
    }
}
