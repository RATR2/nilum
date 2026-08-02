package io.github.r4t2.nilum.common.font;

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

/** Server-side registry of custom .ttf fonts (fonts/<id>.ttf), streamed the same way models/icons/shaderpacks are. */
public final class FontRegistry {

    private final Map<String, byte[]> rawBytesById = new ConcurrentHashMap<>();
    private final Map<String, String> hashById = new ConcurrentHashMap<>();

    public void loadDirectory(Path directory) throws IOException {
        rawBytesById.clear();
        hashById.clear();
        Files.createDirectories(directory);

        try (Stream<Path> files = Files.list(directory)) {
            for (Path file : files.filter(FontRegistry::isTtfFile).toList()) {
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

    public Set<String> fontIds() {
        return Set.copyOf(rawBytesById.keySet());
    }

    public List<AssetManifestEntry> manifest() {
        List<AssetManifestEntry> entries = new ArrayList<>();
        for (Map.Entry<String, String> entry : hashById.entrySet()) {
            entries.add(new AssetManifestEntry(entry.getKey(), entry.getValue(), AssetKind.FONT));
        }
        return entries;
    }

    private static boolean isTtfFile(Path path) {
        return Files.isRegularFile(path) && path.getFileName().toString().endsWith(".ttf");
    }

    private static String stripExtension(String fileName) {
        return fileName.substring(0, fileName.length() - ".ttf".length());
    }
}
