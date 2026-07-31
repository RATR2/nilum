package io.github.r4t2.nilum.common.asset;

import io.github.r4t2.nilum.common.protocol.AssetKind;
import io.github.r4t2.nilum.common.util.SHA256;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AssetCache {

    private final Path root;

    public AssetCache(Path root) {
        this.root = root;
    }

    public boolean isCached(String assetId, AssetKind kind, String expectedSha256) {
        Path file = fileFor(assetId, kind);
        if (!Files.isRegularFile(file)) {
            return false;
        }
        try {
            return SHA256.of(Files.readAllBytes(file)).equals(expectedSha256);
        } catch (IOException e) {
            return false;
        }
    }

    public byte[] read(String assetId, AssetKind kind) throws IOException {
        return Files.readAllBytes(fileFor(assetId, kind));
    }

    public void write(String assetId, AssetKind kind, byte[] data) throws IOException {
        Path file = fileFor(assetId, kind);
        Files.createDirectories(file.getParent());
        Files.write(file, data);
    }

    private Path fileFor(String assetId, AssetKind kind) {
        String extension = switch (kind) {
            case MODEL -> ".bbmodel";
            case ICON -> ".png";
            case HUD_ATLAS -> ".hudatlas";
        };
        return root.resolve(assetId + extension);
    }
}
