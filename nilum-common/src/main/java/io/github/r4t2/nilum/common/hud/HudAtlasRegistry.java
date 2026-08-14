package io.github.r4t2.nilum.common.hud;

import io.github.r4t2.nilum.common.protocol.AssetKind;
import io.github.r4t2.nilum.common.protocol.AssetManifestEntry;
import io.github.r4t2.nilum.common.util.SHA256;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Server-side registry of HUD atlases: an <id>.atlas descriptor, an optional spritesheet, and
 * any Image element textures. Served/hashed as a HudAtlasAssetPayload bundling all of it.
 */
public final class HudAtlasRegistry {

    private final Map<String, byte[]> assetBytesById = new ConcurrentHashMap<>();
    private final Map<String, String> hashById = new ConcurrentHashMap<>();

    public void loadDirectory(Path hudDirectory, Path texturesDirectory, Consumer<String> onWarning) throws IOException {
        assetBytesById.clear();
        hashById.clear();

        Files.createDirectories(hudDirectory);

        try (Stream<Path> files = Files.list(hudDirectory)) {
            for (Path descriptorFile : files.filter(HudAtlasRegistry::isAtlasFile).toList()) {
                String atlasId = stripExtension(descriptorFile.getFileName().toString(), ".atlas");
                try {
                    load(atlasId, descriptorFile, hudDirectory, texturesDirectory, onWarning);
                } catch (RuntimeException | IOException e) {
                    onWarning.accept("Failed to load HUD atlas '" + atlasId + "': " + e);
                }
            }
        }
    }

    private void load(String atlasId, Path descriptorFile, Path hudDirectory, Path texturesDirectory,
                       Consumer<String> onWarning) throws IOException {
        byte[] descriptorBytes = Files.readAllBytes(descriptorFile);
        HudAtlasDescriptor descriptor = HudAtlasParser.parse(new String(descriptorBytes, StandardCharsets.UTF_8));

        Path sheetFile = hudDirectory.resolve(atlasId + ".png");
        Optional<byte[]> spritesheetPngBytes = Files.isRegularFile(sheetFile)
                ? Optional.of(Files.readAllBytes(sheetFile)) : Optional.empty();

        Set<String> referencedTextures = new LinkedHashSet<>();
        for (HudAtlasElement element : descriptor.elements().values()) {
            if (element instanceof HudAtlasElement.Image image) {
                referencedTextures.add(image.textureFile());
            }
        }

        Map<String, byte[]> imageTextures = new LinkedHashMap<>();
        for (String fileName : referencedTextures) {
            Path textureFile = texturesDirectory.resolve(fileName);
            if (!Files.isRegularFile(textureFile)) {
                onWarning.accept("HUD atlas '" + atlasId + "' references texture '" + fileName
                        + "', which doesn't exist in the textures folder.");
                continue;
            }
            imageTextures.put(fileName, Files.readAllBytes(textureFile));
        }

        byte[] assetBytes = new HudAtlasAssetPayload(spritesheetPngBytes, imageTextures, descriptorBytes).encode();
        assetBytesById.put(atlasId, assetBytes);
        hashById.put(atlasId, SHA256.of(assetBytes));
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

    private static boolean isAtlasFile(Path path) {
        return Files.isRegularFile(path) && path.getFileName().toString().endsWith(".atlas");
    }

    private static String stripExtension(String fileName, String extension) {
        return fileName.substring(0, fileName.length() - extension.length());
    }
}
