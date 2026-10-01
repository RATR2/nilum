package io.github.r4t2.nilum.fabric.font;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.r4t2.nilum.fabric.NilumFabricMod;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Client-side registry of streamed font-provider icons, each auto-assigned a Private Use Area codepoint on first sight. */
public final class ClientFontIconStore {

    private static final int FIRST_CODEPOINT = 0xE000;

    private final Map<String, Integer> codepointByIconId = new ConcurrentHashMap<>();
    private final Map<Integer, NativeImage> imagesByCodepoint = new ConcurrentHashMap<>();
    private final AtomicInteger nextCodepoint = new AtomicInteger(FIRST_CODEPOINT);

    public void install(String iconId, byte[] pngBytes) {
        try {
            NativeImage image = NativeImage.read(pngBytes);
            int codepoint = codepointByIconId.computeIfAbsent(iconId, id -> nextCodepoint.getAndIncrement());
            NativeImage previous = imagesByCodepoint.put(codepoint, image);
            if (previous != null) {
                previous.close();
            }
        } catch (IOException e) {
            NilumFabricMod.LOGGER.warn("Failed to load font icon '" + iconId + "': " + e);
        }
    }

    public Optional<Integer> codepointFor(String iconId) {
        return Optional.ofNullable(codepointByIconId.get(iconId));
    }

    public Optional<NativeImage> imageFor(int codepoint) {
        return Optional.ofNullable(imagesByCodepoint.get(codepoint));
    }

    public Set<Integer> codepoints() {
        return Set.copyOf(imagesByCodepoint.keySet());
    }
}
