package io.github.r4t2.nilum.common.hud;

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
 * Server-side registry of HUD atlases, each a paired <id>.png spritesheet and <id>.atlas
 * descriptor sharing a base filename. Served/hashed as a HudAtlasAssetPayload bundling both
 * files' bytes, so a descriptor-only edit still triggers a client re-fetch.
 */
public final class HudAtlasRegistry {

    private final Map<String, byte[]> assetBytesById = new ConcurrentHashMap<>();
    private final Map<String, String> hashById = new ConcurrentHashMap<>();

    public void loadDirectory(Path directory) throws IOException {
        assetBytesById.clear();
        hashById.clear();

        Files.createDirectories(directory);

        try (Stream<Path> files = Files.list(directory)) {
            for (Path pngFile : files.filter(HudAtlasRegistry::isPngFile).toList()) {
                String atlasId = stripExtension(pngFile.getFileName().toString(), ".png");
                Path descriptorFile = directory.resolve(atlasId + ".atlas");
                if (!Files.isRegularFile(descriptorFile)) {
                    continue;
                }

                byte[] pngBytes = Files.readAllBytes(pngFile);
                byte[] descriptorBytes = Files.readAllBytes(descriptorFile);
                byte[] assetBytes = new HudAtlasAssetPayload(pngBytes, descriptorBytes).encode();

                assetBytesById.put(atlasId, assetBytes);
                hashById.put(atlasId, SHA256.of(assetBytes));
            }
        }
    }

    public Optional<byte[]> assetBytes(String atlasId) {
        return Optional.ofNullable(assetBytesById.get(atlasId));
    }

    public Set<String> atlasIds() {
        return Set.copyOf(assetBytesById.keySet());
    }

    public List<AssetManifestEntry> manifest() {
        List<AssetManifestEntry> entries = new ArrayList<>();
        for (Map.Entry<String, String> entry : hashById.entrySet()) {
            entries.add(new AssetManifestEntry(entry.getKey(), entry.getValue(), AssetKind.HUD_ATLAS));
        }
        return entries;
    }

    private static boolean isPngFile(Path path) {
        return Files.isRegularFile(path) && path.getFileName().toString().endsWith(".png");
    }

    private static String stripExtension(String fileName, String extension) {
        return fileName.substring(0, fileName.length() - extension.length());
    }
}
