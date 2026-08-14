package io.github.r4t2.nilum.fabric.hud;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.r4t2.nilum.common.expr.ExprEvaluationException;
import io.github.r4t2.nilum.common.expr.ExprEvaluator;
import io.github.r4t2.nilum.common.expr.ExprNode;
import io.github.r4t2.nilum.common.expr.ExprParser;
import io.github.r4t2.nilum.common.expr.TextValueSource;
import io.github.r4t2.nilum.common.expr.ValueSource;
import io.github.r4t2.nilum.common.hud.HudAtlasAssetPayload;
import io.github.r4t2.nilum.common.hud.HudAtlasDescriptor;
import io.github.r4t2.nilum.common.hud.HudAtlasElement;
import io.github.r4t2.nilum.common.hud.HudElementType;
import io.github.r4t2.nilum.fabric.NilumFabricMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** One server-streamed HUD atlas: its texture(s), parsed .atlas descriptor, and client-side per-frame state. */
public final class HudAtlas {

    /** Sentinel key for the shared spritesheet in texturesByKey, distinct from any real filename. */
    private static final String SHEET_KEY = "";

    public record TextureRef(Identifier id, int width, int height) {
    }

    private record LoadedTexture(Identifier id, NativeImage canvas, DynamicTexture gpuTexture) {
    }

    /** -1 means "hold indefinitely until a release", matching HudFrameOverridePacket. */
    private record FrameOverride(int frame, long expiryEpochMillis) {
        boolean active(long nowEpochMillis) {
            return expiryEpochMillis < 0 || nowEpochMillis < expiryEpochMillis;
        }
    }

    private final String atlasId;
    private final HudAtlasDescriptor descriptor;
    private final Map<String, ExprNode> parsedAutoExpressions;
    private final Map<String, LoadedTexture> texturesByKey;

    private final Map<String, Integer> serverFrameByElement = new ConcurrentHashMap<>();
    private final Map<String, FrameOverride> overrideByElement = new ConcurrentHashMap<>();
    private final Map<String, String> serverTextByElement = new ConcurrentHashMap<>();

    private HudAtlas(String atlasId, HudAtlasDescriptor descriptor, Map<String, ExprNode> parsedAutoExpressions,
                      Map<String, LoadedTexture> texturesByKey) {
        this.atlasId = atlasId;
        this.descriptor = descriptor;
        this.parsedAutoExpressions = parsedAutoExpressions;
        this.texturesByKey = texturesByKey;
    }

    /** Must be called on the render thread; decodes every PNG in the payload and registers real GPU textures. */
    static HudAtlas load(String atlasId, byte[] assetBytes) throws IOException {
        HudAtlasAssetPayload payload = HudAtlasAssetPayload.decode(assetBytes);
        HudAtlasDescriptor descriptor = payload.decodeDescriptor();

        Map<String, ExprNode> parsed = new HashMap<>();
        descriptor.elements().forEach((elementId, element) -> {
            Optional<String> clientConnector = switch (element) {
                case HudAtlasElement.Sprite sprite when sprite.type() == HudElementType.AUTO -> sprite.clientConnector();
                case HudAtlasElement.Image image when image.type() == HudElementType.AUTO -> image.clientConnector();
                case HudAtlasElement.Duplicate dup when dup.type() == HudElementType.AUTO -> dup.clientConnector();
                case HudAtlasElement.Text text -> text.clientConnector();
                default -> Optional.empty();
            };
            clientConnector.ifPresent(source -> {
                try {
                    parsed.put(elementId, ExprParser.parse(source));
                } catch (RuntimeException e) {
                    NilumFabricMod.LOGGER.warn("HUD atlas '" + atlasId + "' element '" + elementId
                            + "' has an invalid client_connector, treating as frame 0/empty: " + e);
                }
            });
        });

        Map<String, LoadedTexture> textures = new HashMap<>();
        if (payload.spritesheetPngBytes().isPresent()) {
            textures.put(SHEET_KEY, loadTexture(atlasId, "sheet", payload.spritesheetPngBytes().get()));
        }
        for (Map.Entry<String, byte[]> entry : payload.imageTextures().entrySet()) {
            textures.put(entry.getKey(), loadTexture(atlasId, entry.getKey(), entry.getValue()));
        }

        return new HudAtlas(atlasId, descriptor, parsed, textures);
    }

    private static LoadedTexture loadTexture(String atlasId, String key, byte[] pngBytes) throws IOException {
        NativeImage canvas = NativeImage.read(pngBytes);
        Identifier id = Identifier.fromNamespaceAndPath("nilum", "dynamic/hud_atlas/" + atlasId + "/" + sanitize(key));
        DynamicTexture gpuTexture = new DynamicTexture(() -> "Nilum HUD atlas '" + atlasId + "' texture '" + key + "'", canvas);
        Minecraft.getInstance().getTextureManager().register(id, gpuTexture);
        return new LoadedTexture(id, canvas, gpuTexture);
    }

    private static String sanitize(String key) {
        return key.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_./-]", "_");
    }

    /** The GPU texture and pixel dimensions to draw with: the shared spritesheet, or one Image element's own file. */
    public Optional<TextureRef> textureFor(Optional<String> imageFile) {
        LoadedTexture texture = texturesByKey.get(imageFile.orElse(SHEET_KEY));
        return texture == null ? Optional.empty()
                : Optional.of(new TextureRef(texture.id(), texture.canvas().getWidth(), texture.canvas().getHeight()));
    }

    /**
     * Rewrites one frame's pixels and re-uploads the full texture. Sprite only; Image elements
     * don't source from a shared, patchable canvas.
     */
    void applyPatch(String elementId, int frame, byte[] png) {
        var raw = descriptor.elements().get(elementId);
        if (!(raw instanceof HudAtlasElement.Sprite element)) {
            NilumFabricMod.LOGGER.warn("Atlas patch for unknown or non-sprite HUD element '" + elementId + "' in atlas '" + atlasId + "'.");
            return;
        }
        LoadedTexture sheet = texturesByKey.get(SHEET_KEY);
        if (sheet == null) {
            NilumFabricMod.LOGGER.warn("Atlas patch for '" + atlasId + ":" + elementId + "' but this atlas has no spritesheet loaded.");
            return;
        }

        NativeImage patch;
        try {
            patch = NativeImage.read(png);
        } catch (IOException e) {
            NilumFabricMod.LOGGER.warn("Atlas patch for '" + atlasId + ":" + elementId + "' isn't a valid PNG: " + e);
            return;
        }

        if (patch.getWidth() != element.frameWidth() || patch.getHeight() != element.frameHeight()) {
            NilumFabricMod.LOGGER.warn("Atlas patch for '" + atlasId + ":" + elementId + "' is "
                    + patch.getWidth() + "x" + patch.getHeight() + ", expected "
                    + element.frameWidth() + "x" + element.frameHeight() + " - discarding, existing frame kept.");
            patch.close();
            return;
        }

        int destX = element.frameOriginX(frame);
        int destY = element.frameOriginY(frame);
        for (int y = 0; y < patch.getHeight(); y++) {
            for (int x = 0; x < patch.getWidth(); x++) {
                sheet.canvas().setPixel(destX + x, destY + y, patch.getPixel(x, y));
            }
        }
        patch.close();
        sheet.gpuTexture().upload();
    }

    void setServerFrame(String elementId, int frame) {
        serverFrameByElement.put(elementId, frame);
    }

    void setServerText(String elementId, String text) {
        serverTextByElement.put(elementId, text);
    }

    void override(String elementId, int frame, int durationTicks) {
        long expiry = durationTicks < 0 ? -1 : System.currentTimeMillis() + durationTicks * 50L;
        overrideByElement.put(elementId, new FrameOverride(frame, expiry));
    }

    void release(String elementId) {
        overrideByElement.remove(elementId);
    }

    public HudAtlasDescriptor descriptor() {
        return descriptor;
    }

    /** Resolves a Sprite or Image element's frame to draw right now: override, then server/auto/static per its type. */
    public int currentFrame(String elementId, ValueSource valueSource, double timeSeconds) {
        HudAtlasElement element = descriptor.elements().get(elementId);

        FrameOverride override = overrideByElement.get(elementId);
        if (override != null) {
            if (override.active(System.currentTimeMillis())) {
                return override.frame();
            }
            overrideByElement.remove(elementId);
        }

        return switch (element) {
            case HudAtlasElement.Sprite sprite -> resolveFrame(elementId, sprite.type(), sprite.staticFrame(), valueSource, timeSeconds);
            case HudAtlasElement.Image image -> resolveFrame(elementId, image.type(), image.staticFrame(), valueSource, timeSeconds);
            default -> 0;
        };
    }

    /** Resolves one duplicate element's repeat count right now: override, then server/auto/static per its type. */
    public int currentCount(String elementId, ValueSource valueSource, double timeSeconds) {
        if (!(descriptor.elements().get(elementId) instanceof HudAtlasElement.Duplicate element)) {
            return 0;
        }

        FrameOverride override = overrideByElement.get(elementId);
        if (override != null) {
            if (override.active(System.currentTimeMillis())) {
                return override.frame();
            }
            overrideByElement.remove(elementId);
        }

        return resolveFrame(elementId, element.type(), element.staticFrame(), valueSource, timeSeconds);
    }

    private int resolveFrame(String elementId, HudElementType type, int staticFrame, ValueSource valueSource, double timeSeconds) {
        return switch (type) {
            case STATIC -> staticFrame;
            case SERVER -> serverFrameByElement.getOrDefault(elementId, 0);
            case AUTO -> evaluateAuto(elementId, valueSource, timeSeconds);
        };
    }

    /** Resolves one render_text element's display string. With no format, a pushed server_connector value wins over the client one. */
    public String currentText(String elementId, ValueSource valueSource, TextValueSource textSource, double timeSeconds) {
        if (!(descriptor.elements().get(elementId) instanceof HudAtlasElement.Text element)) {
            return "";
        }

        String serverText = serverTextByElement.get(elementId);

        Optional<String> format = element.format();
        if (format.isPresent()) {
            String clientResult = evaluateClientText(elementId, valueSource, textSource, timeSeconds);
            String serverResult = serverText == null ? "" : serverText;
            return format.get().replace("%client%", clientResult).replace("%server%", serverResult);
        }

        if (serverText != null) {
            return serverText;
        }
        return evaluateClientText(elementId, valueSource, textSource, timeSeconds);
    }

    private String evaluateClientText(String elementId, ValueSource valueSource, TextValueSource textSource, double timeSeconds) {
        ExprNode expr = parsedAutoExpressions.get(elementId);
        if (expr == null) {
            return "";
        }
        try {
            return ExprEvaluator.evaluateText(expr, valueSource, textSource, timeSeconds);
        } catch (ExprEvaluationException e) {
            NilumFabricMod.LOGGER.warn("HUD atlas '" + atlasId + "' text element '" + elementId
                    + "' failed to evaluate: " + e.getMessage());
            return "";
        }
    }

    private int evaluateAuto(String elementId, ValueSource valueSource, double timeSeconds) {
        ExprNode expr = parsedAutoExpressions.get(elementId);
        if (expr == null) {
            return 0;
        }
        try {
            long start = System.nanoTime();
            double result = ExprEvaluator.evaluate(expr, valueSource, timeSeconds);
            long elapsedNanos = System.nanoTime() - start;
            if (elapsedNanos > 2_000_000L) {
                NilumFabricMod.LOGGER.warn("HUD atlas '" + atlasId + "' element '" + elementId
                        + "' took " + (elapsedNanos / 1_000_000.0) + "ms to evaluate, falling back to frame 0.");
                return 0;
            }
            return (int) Math.round(result);
        } catch (ExprEvaluationException e) {
            NilumFabricMod.LOGGER.warn("HUD atlas '" + atlasId + "' element '" + elementId
                    + "' failed to evaluate, falling back to frame 0: " + e.getMessage());
            return 0;
        }
    }
}
