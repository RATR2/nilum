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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One server-streamed HUD atlas: its spritesheet texture, parsed .atlas descriptor, and the
 * client-side state needed to pick each element's current frame every render tick.
 */
public final class HudAtlas {

    /** -1 means "hold indefinitely until a release", matching HudFrameOverridePacket. */
    private record FrameOverride(int frame, long expiryEpochMillis) {
        boolean active(long nowEpochMillis) {
            return expiryEpochMillis < 0 || nowEpochMillis < expiryEpochMillis;
        }
    }

    private final String atlasId;
    private final Identifier textureId;
    private final HudAtlasDescriptor descriptor;
    private final Map<String, ExprNode> parsedAutoExpressions;

    private NativeImage canvas;
    private DynamicTexture gpuTexture;

    private final Map<String, Integer> serverFrameByElement = new ConcurrentHashMap<>();
    private final Map<String, FrameOverride> overrideByElement = new ConcurrentHashMap<>();
    private final Map<String, String> serverTextByElement = new ConcurrentHashMap<>();

    private HudAtlas(String atlasId, Identifier textureId, HudAtlasDescriptor descriptor,
                      Map<String, ExprNode> parsedAutoExpressions, NativeImage canvas, DynamicTexture gpuTexture) {
        this.atlasId = atlasId;
        this.textureId = textureId;
        this.descriptor = descriptor;
        this.parsedAutoExpressions = parsedAutoExpressions;
        this.canvas = canvas;
        this.gpuTexture = gpuTexture;
    }

    /** Must be called on the render thread; decodes the PNG and registers a real GPU texture. */
    static HudAtlas load(String atlasId, byte[] assetBytes) throws IOException {
        HudAtlasAssetPayload payload = HudAtlasAssetPayload.decode(assetBytes);
        HudAtlasDescriptor descriptor = payload.decodeDescriptor();

        Map<String, ExprNode> parsed = new HashMap<>();
        descriptor.elements().forEach((elementId, element) -> {
            Optional<String> clientConnector = switch (element) {
                case HudAtlasElement.Sprite sprite when sprite.type() == HudElementType.AUTO -> sprite.clientConnector();
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

        NativeImage canvas = NativeImage.read(payload.pngBytes());
        Identifier textureId = Identifier.fromNamespaceAndPath("nilum", "dynamic/hud_atlas/" + atlasId);
        DynamicTexture gpuTexture = new DynamicTexture(() -> "Nilum HUD atlas '" + atlasId + "'", canvas);
        Minecraft.getInstance().getTextureManager().register(textureId, gpuTexture);

        return new HudAtlas(atlasId, textureId, descriptor, parsed, canvas, gpuTexture);
    }

    /**
     * Rewrites one frame's pixels and re-uploads. Vanishingly rare compared to a per-tick
     * render, so a full-texture re-upload is simpler than a sub-rectangle GPU upload.
     */
    void applyPatch(String elementId, int frame, byte[] png) {
        var raw = descriptor.elements().get(elementId);
        if (!(raw instanceof HudAtlasElement.Sprite element)) {
            NilumFabricMod.LOGGER.warn("Atlas patch for unknown or non-sprite HUD element '" + elementId + "' in atlas '" + atlasId + "'.");
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
                canvas.setPixel(destX + x, destY + y, patch.getPixel(x, y));
            }
        }
        patch.close();
        gpuTexture.upload();
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

    public Identifier textureId() {
        return textureId;
    }

    public int width() {
        return canvas.getWidth();
    }

    public int height() {
        return canvas.getHeight();
    }

    /** Resolves one sprite element's frame to draw right now: override, then server/auto/static per its type. */
    public int currentFrame(String elementId, ValueSource valueSource, double timeSeconds) {
        if (!(descriptor.elements().get(elementId) instanceof HudAtlasElement.Sprite element)) {
            return 0;
        }

        FrameOverride override = overrideByElement.get(elementId);
        if (override != null) {
            if (override.active(System.currentTimeMillis())) {
                return override.frame();
            }
            overrideByElement.remove(elementId);
        }

        return switch (element.type()) {
            case STATIC -> element.staticFrame();
            case SERVER -> serverFrameByElement.getOrDefault(elementId, 0);
            case AUTO -> evaluateAuto(elementId, valueSource, timeSeconds);
        };
    }

    /**
     * Resolves one render_text element's display string right now. A pushed server_connector
     * value always wins over a locally-evaluated client_connector.
     */
    public String currentText(String elementId, ValueSource valueSource, TextValueSource textSource, double timeSeconds) {
        if (!(descriptor.elements().get(elementId) instanceof HudAtlasElement.Text)) {
            return "";
        }

        String serverText = serverTextByElement.get(elementId);
        if (serverText != null) {
            return serverText;
        }

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
