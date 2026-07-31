package io.github.r4t2.nilum.fabric.render;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.r4t2.nilum.common.icon.IconAssetPayload;
import io.github.r4t2.nilum.common.icon.IconDisplay;
import io.github.r4t2.nilum.fabric.NilumFabricMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A single shared, growable texture atlas for icon-only custom items. Icons are packed
 * left-to-right into shelves (rows); a shelf that doesn't fit starts a new one, and the
 * canvas only ever grows taller (never wider, never repacked) so an already-placed icon's
 * pixel rect never moves once assigned. Canvas width is fixed; height grows in shelves as
 * needed. Not vanilla's real item atlas - that's tied to the resourcepack reload lifecycle,
 * and icons can arrive from the server at any time mid-session.
 *
 * <p>Icons larger than {@link #MAX_ICON_DIMENSION} in either axis are downscaled (preserving
 * aspect ratio) before packing - admins shouldn't need to hand-tune source image dimensions
 * for what's meant to be a small inventory icon.
 *
 * <p>All packing/upload work happens on the render thread (icons can arrive off-thread, from
 * AssetSyncSession's fetch thread) via {@link Minecraft#execute}. The GPU texture
 * is only recreated when the canvas actually grows (a new GPU allocation is unavoidable then);
 * otherwise an icon addition just re-uploads the mutated pixels into the existing texture.
 */
public final class IconAtlas {

    private static final int CANVAS_WIDTH = 256;
    private static final int MAX_ICON_DIMENSION = 128;
    private static final Identifier ATLAS_ID = Identifier.fromNamespaceAndPath("nilum", "dynamic/icon_atlas");

    private final Map<String, IconUv> uvById = new ConcurrentHashMap<>();
    private final Map<String, IconDisplay> displayById = new ConcurrentHashMap<>();

    private final Path debugDumpPath;

    private NativeImage canvas = new NativeImage(CANVAS_WIDTH, 16, true);
    private DynamicTexture gpuTexture;
    private int shelfX;
    private int shelfY;
    private int shelfHeight;

    public record IconUv(int x, int y, int width, int height) {
    }

    /** @param debugDumpPath where to write the current atlas contents as a PNG after every change, for inspection. */
    public IconAtlas(Path debugDumpPath) {
        this.debugDumpPath = debugDumpPath;
    }

    public void add(String iconId, byte[] assetBytes) {
        Minecraft.getInstance().execute(() -> {
            try {
                place(iconId, assetBytes);
            } catch (IOException | RuntimeException e) {
                NilumFabricMod.LOGGER.warn("Failed to add icon '" + iconId + "' to the icon atlas: " + e);
            }
        });
    }

    public Optional<IconUv> uvOf(String iconId) {
        return Optional.ofNullable(uvById.get(iconId));
    }

    public Set<String> iconIds() {
        return Set.copyOf(uvById.keySet());
    }

    public Optional<IconDisplay> displayOf(String iconId) {
        return Optional.ofNullable(displayById.get(iconId));
    }

    public int width() {
        return canvas.getWidth();
    }

    public int height() {
        return canvas.getHeight();
    }

    public Identifier textureId() {
        return ATLAS_ID;
    }

    private void place(String iconId, byte[] assetBytes) throws IOException {
        IconAssetPayload payload = IconAssetPayload.decode(assetBytes);
        displayById.put(iconId, payload.display());

        NativeImage icon = NativeImage.read(payload.pngBytes());
        icon = downscaleIfNeeded(icon);

        int iconWidth = icon.getWidth();
        int iconHeight = icon.getHeight();

        if (shelfX + iconWidth > CANVAS_WIDTH) {
            shelfY += shelfHeight;
            shelfX = 0;
            shelfHeight = 0;
        }

        int requiredHeight = shelfY + Math.max(iconHeight, shelfHeight);
        boolean grew = requiredHeight > canvas.getHeight();
        if (grew) {
            growCanvasTo(requiredHeight);
        }

        blit(icon, shelfX, shelfY);
        icon.close();

        uvById.put(iconId, new IconUv(shelfX, shelfY, iconWidth, iconHeight));

        shelfX += iconWidth;
        shelfHeight = Math.max(shelfHeight, iconHeight);

        if (grew || gpuTexture == null) {
            gpuTexture = new DynamicTexture(() -> "Nilum icon atlas", canvas);
            Minecraft.getInstance().getTextureManager().register(ATLAS_ID, gpuTexture);
        } else {
            gpuTexture.upload();
        }

        dumpForInspection();
    }

    private void dumpForInspection() {
        try {
            Files.createDirectories(debugDumpPath.getParent());
            canvas.writeToFile(debugDumpPath);
        } catch (IOException e) {
            NilumFabricMod.LOGGER.warn("Failed to write icon atlas debug dump: " + e);
        }
    }

    private static NativeImage downscaleIfNeeded(NativeImage icon) {
        int width = icon.getWidth();
        int height = icon.getHeight();
        if (width <= MAX_ICON_DIMENSION && height <= MAX_ICON_DIMENSION) {
            return icon;
        }

        float scale = Math.min((float) MAX_ICON_DIMENSION / width, (float) MAX_ICON_DIMENSION / height);
        int scaledWidth = Math.max(1, Math.round(width * scale));
        int scaledHeight = Math.max(1, Math.round(height * scale));

        NativeImage scaled = new NativeImage(scaledWidth, scaledHeight, true);
        icon.resizeSubRectTo(0, 0, width, height, scaled);
        icon.close();
        return scaled;
    }

    private void blit(NativeImage icon, int destX, int destY) {
        for (int y = 0; y < icon.getHeight(); y++) {
            for (int x = 0; x < icon.getWidth(); x++) {
                canvas.setPixel(destX + x, destY + y, icon.getPixel(x, y));
            }
        }
    }

    private void growCanvasTo(int newHeight) {
        NativeImage grown = new NativeImage(CANVAS_WIDTH, newHeight, true);
        for (int y = 0; y < canvas.getHeight(); y++) {
            for (int x = 0; x < CANVAS_WIDTH; x++) {
                grown.setPixel(x, y, canvas.getPixel(x, y));
            }
        }
        canvas = grown;
    }
}
