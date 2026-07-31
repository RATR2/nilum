package io.github.r4t2.nilum.common.icon;

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
 * Server-side registry of icon-only custom item textures. What's actually served/hashed for an
 * icon is an {@link IconAssetPayload} - the PNG bundled with its resolved {@link IconDisplay} -
 * not just the raw file bytes, so a display-config-only change (no PNG edit at all) still
 * changes the asset's hash and triggers a client re-fetch. {@link #loadDirectory} alone leaves
 * every icon's display at identity; call {@link #applyDisplay} once it's actually resolved
 * (from icons.yml, server-side only - this class stays loader-agnostic).
 */
public final class IconRegistry {

    private final Map<String, byte[]> pngBytesById = new ConcurrentHashMap<>();
    private final Map<String, byte[]> assetBytesById = new ConcurrentHashMap<>();
    private final Map<String, String> hashById = new ConcurrentHashMap<>();

    public void loadDirectory(Path directory) throws IOException {
        pngBytesById.clear();
        assetBytesById.clear();
        hashById.clear();

        Files.createDirectories(directory);

        try (Stream<Path> files = Files.list(directory)) {
            for (Path file : files.filter(IconRegistry::isPngFile).toList()) {
                String iconId = stripExtension(file.getFileName().toString());
                byte[] pngBytes = Files.readAllBytes(file);
                pngBytesById.put(iconId, pngBytes);
                applyDisplay(iconId, IconDisplay.allIdentity());
            }
        }
    }

    /** Rebuilds the served payload/hash for one icon from its loaded PNG bytes plus a resolved display. */
    public void applyDisplay(String iconId, IconDisplay display) {
        byte[] pngBytes = pngBytesById.get(iconId);
        if (pngBytes == null) {
            return;
        }
        byte[] assetBytes = new IconAssetPayload(pngBytes, display).encode();
        assetBytesById.put(iconId, assetBytes);
        hashById.put(iconId, SHA256.of(assetBytes));
    }

    public Optional<byte[]> assetBytes(String iconId) {
        return Optional.ofNullable(assetBytesById.get(iconId));
    }

    public Set<String> iconIds() {
        return Set.copyOf(pngBytesById.keySet());
    }

    public List<AssetManifestEntry> manifest() {
        List<AssetManifestEntry> entries = new ArrayList<>();
        for (Map.Entry<String, String> entry : hashById.entrySet()) {
            entries.add(new AssetManifestEntry(entry.getKey(), entry.getValue(), AssetKind.ICON));
        }
        return entries;
    }

    private static boolean isPngFile(Path path) {
        return Files.isRegularFile(path) && path.getFileName().toString().endsWith(".png");
    }

    private static String stripExtension(String fileName) {
        return fileName.substring(0, fileName.length() - ".png".length());
    }
}
