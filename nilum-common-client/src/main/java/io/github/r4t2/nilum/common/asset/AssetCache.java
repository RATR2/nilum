package io.github.r4t2.nilum.common.asset;

import io.github.r4t2.nilum.common.protocol.AssetKind;
import io.github.r4t2.nilum.common.protocol.AssetManifestEntry;
import io.github.r4t2.nilum.common.util.SHA256;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Content-hash cache scoped to whichever server rebase() last pointed it at. */
public final class AssetCache {

    private volatile Path root;

    public AssetCache(Path root) {
        this.root = root;
    }

    /** Repoints the cache at a different server's own directory, e.g. on a new connection. */
    public void rebase(Path root) {
        this.root = root;
    }

    /** Deletes any cached file the given manifest no longer references, since the server no longer has it. */
    public synchronized void pruneExcept(List<AssetManifestEntry> keep) {
        if (!Files.isDirectory(root)) {
            return;
        }
        Set<String> keepNames = new HashSet<>();
        for (AssetManifestEntry entry : keep) {
            keepNames.add(fileFor(entry.assetId(), entry.kind()).getFileName().toString());
        }
        try (var files = Files.list(root)) {
            for (Path file : (Iterable<Path>) files::iterator) {
                if (Files.isRegularFile(file) && !keepNames.contains(file.getFileName().toString())) {
                    Files.delete(file);
                }
            }
        } catch (IOException ignored) {
            // Best-effort: a stale file left behind just gets pruned next manifest instead.
        }
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
            case SHADER_PACK -> ".zip";
            case FONT -> ".ttf";
            case CUSTOM_UI -> ".nilumui";
        };
        return root.resolve(assetId + extension);
    }
}
