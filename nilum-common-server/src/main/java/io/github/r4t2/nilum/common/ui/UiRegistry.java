package io.github.r4t2.nilum.common.ui;

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

/** Server-side registry of custom UIs: a <id>.ui descriptor plus every image its elements reference, served/hashed as a UiAssetPayload. */
public final class UiRegistry {

    private final Map<String, byte[]> assetBytesById = new ConcurrentHashMap<>();
    private final Map<String, String> hashById = new ConcurrentHashMap<>();
    private final Map<String, UiDescriptor> descriptorById = new ConcurrentHashMap<>();

    public void loadDirectory(Path uiDirectory, Path texturesDirectory, Consumer<String> onWarning) throws IOException {
        assetBytesById.clear();
        hashById.clear();
        descriptorById.clear();

        Files.createDirectories(uiDirectory);

        try (Stream<Path> files = Files.list(uiDirectory)) {
            for (Path descriptorFile : files.filter(UiRegistry::isUiFile).toList()) {
                String uiId = stripExtension(descriptorFile.getFileName().toString(), ".ui");
                try {
                    load(uiId, descriptorFile, texturesDirectory, onWarning);
                } catch (RuntimeException | IOException e) {
                    onWarning.accept("Failed to load custom UI '" + uiId + "': " + e);
                }
            }
        }
    }

    private void load(String uiId, Path descriptorFile, Path texturesDirectory, Consumer<String> onWarning) throws IOException {
        byte[] descriptorBytes = Files.readAllBytes(descriptorFile);
        UiDescriptor descriptor = UiParser.parse(new String(descriptorBytes, StandardCharsets.UTF_8));

        Set<String> referencedImages = new LinkedHashSet<>();
        for (UiElement element : descriptor.elements().values()) {
            if (element instanceof UiElement.Image image) {
                referencedImages.add(image.imageFile());
            } else if (element instanceof UiElement.Button button) {
                referencedImages.add(button.imageFile());
                referencedImages.add(button.pressedImageFile());
            }
        }

        Map<String, byte[]> images = new LinkedHashMap<>();
        for (String fileName : referencedImages) {
            Path textureFile = texturesDirectory.resolve(fileName);
            if (!Files.isRegularFile(textureFile)) {
                onWarning.accept("Custom UI '" + uiId + "' references image '" + fileName
                        + "', which doesn't exist in the textures folder.");
                continue;
            }
            images.put(fileName, Files.readAllBytes(textureFile));
        }

        byte[] assetBytes = new UiAssetPayload(images, descriptorBytes).encode();
        assetBytesById.put(uiId, assetBytes);
        hashById.put(uiId, SHA256.of(assetBytes));
        descriptorById.put(uiId, descriptor);
    }

    public Optional<byte[]> assetBytes(String uiId) {
        return Optional.ofNullable(assetBytesById.get(uiId));
    }

    public Optional<UiDescriptor> descriptor(String uiId) {
        return Optional.ofNullable(descriptorById.get(uiId));
    }

    public Set<String> uiIds() {
        return Set.copyOf(assetBytesById.keySet());
    }

    public List<AssetManifestEntry> manifest() {
        List<AssetManifestEntry> entries = new ArrayList<>();
        for (Map.Entry<String, String> entry : hashById.entrySet()) {
            entries.add(new AssetManifestEntry(entry.getKey(), entry.getValue(), AssetKind.CUSTOM_UI));
        }
        return entries;
    }

    private static boolean isUiFile(Path path) {
        return Files.isRegularFile(path) && path.getFileName().toString().endsWith(".ui");
    }

    private static String stripExtension(String fileName, String extension) {
        return fileName.substring(0, fileName.length() - extension.length());
    }
}
