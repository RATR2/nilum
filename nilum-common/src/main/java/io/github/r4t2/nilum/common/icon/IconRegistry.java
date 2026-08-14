package io.github.r4t2.nilum.common.icon;

import io.github.r4t2.nilum.common.protocol.AssetKind;
import io.github.r4t2.nilum.common.protocol.AssetManifestEntry;
import io.github.r4t2.nilum.common.util.SHA256;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Server-side registry of icon-only custom item textures. load() leaves display at identity; call applyDisplay() once resolved. */
public final class IconRegistry {

    private final Map<String, byte[]> pngBytesById = new ConcurrentHashMap<>();
    private final Map<String, byte[]> assetBytesById = new ConcurrentHashMap<>();
    private final Map<String, String> hashById = new ConcurrentHashMap<>();

    public void clear() {
        pngBytesById.clear();
        assetBytesById.clear();
        hashById.clear();
    }

    /** Registers one icon's already-read texture bytes under iconId, defaulting its display to identity. */
    public void load(String iconId, byte[] pngBytes) {
        pngBytesById.put(iconId, pngBytes);
        applyDisplay(iconId, IconDisplay.allIdentity());
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
}
