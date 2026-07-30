package io.github.r4t2.nilum.common.asset;

import io.github.r4t2.nilum.common.util.SHA256;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AssetCache {

    private final Path root;

    public AssetCache(Path root) {
        this.root = root;
    }

    public boolean isCached(String assetId, String expectedSha256) {
        Path file = fileFor(assetId);
        if (!Files.isRegularFile(file)) {
            return false;
        }
        try {
            return SHA256.of(Files.readAllBytes(file)).equals(expectedSha256);
        } catch (IOException e) {
            return false;
        }
    }

    public byte[] read(String assetId) throws IOException {
        return Files.readAllBytes(fileFor(assetId));
    }

    public void write(String assetId, byte[] data) throws IOException {
        Path file = fileFor(assetId);
        Files.createDirectories(file.getParent());
        Files.write(file, data);
    }

    private Path fileFor(String assetId) {
        return root.resolve(assetId + ".bbmodel");
    }
}
