package io.github.r4t2.nilum.common.hud;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Parses a {@code .atlas} descriptor - the design doc's example is YAML, but this reads the same
 * field structure as JSON instead: Gson is already on every platform's runtime classpath (used
 * for {@code .bbmodel} too), while SnakeYAML isn't available on the Fabric/NeoForge client
 * without bundling a new dependency just for this one file format.
 */
public final class HudAtlasParser {

    private HudAtlasParser() {
    }

    public static HudAtlasDescriptor parse(String json) {
        JsonObject root;
        try {
            root = JsonParser.parseString(json).getAsJsonObject();
        } catch (JsonParseException | IllegalStateException e) {
            throw new HudAtlasParseException("Not a valid .atlas JSON object", e);
        }

        if (!root.has("atlas_size")) {
            throw new HudAtlasParseException("Missing required 'atlas_size' field");
        }
        JsonArray atlasSize = root.getAsJsonArray("atlas_size");
        int atlasWidth = atlasSize.get(0).getAsInt();
        int atlasHeight = atlasSize.get(1).getAsInt();

        if (!root.has("elements")) {
            throw new HudAtlasParseException("Missing required 'elements' field");
        }

        Map<String, HudAtlasElement> elements = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("elements").entrySet()) {
            elements.put(entry.getKey(), parseElement(entry.getKey(), entry.getValue().getAsJsonObject()));
        }

        return new HudAtlasDescriptor(atlasWidth, atlasHeight, elements);
    }

    private static HudAtlasElement parseElement(String id, JsonObject el) {
        JsonArray origin = requireArray(el, id, "origin");
        int originX = origin.get(0).getAsInt();
        int originY = origin.get(1).getAsInt();

        JsonArray frameSize = requireArray(el, id, "frame_size");
        int frameWidth = frameSize.get(0).getAsInt();
        int frameHeight = frameSize.get(1).getAsInt();

        int frameCount = el.has("frame_count") ? el.get("frame_count").getAsInt() : 1;

        HudElementLayout layout = el.has("layout")
                ? HudElementLayout.valueOf(el.get("layout").getAsString().toUpperCase(Locale.ROOT))
                : HudElementLayout.HORIZONTAL;

        if (!el.has("type")) {
            throw new HudAtlasParseException("Element '" + id + "' is missing required 'type' field");
        }
        HudElementType type = HudElementType.valueOf(el.get("type").getAsString().toUpperCase(Locale.ROOT));

        Optional<String> clientConnector = el.has("client_connector")
                ? Optional.of(el.get("client_connector").getAsString())
                : Optional.empty();

        if (type == HudElementType.AUTO && clientConnector.isEmpty()) {
            throw new HudAtlasParseException("Element '" + id + "' is type 'auto' but has no 'client_connector'");
        }

        int staticFrame = el.has("static_frame") ? el.get("static_frame").getAsInt() : 0;

        int screenX = 0;
        int screenY = 0;
        if (el.has("screen_position")) {
            JsonArray screenPosition = el.getAsJsonArray("screen_position");
            screenX = screenPosition.get(0).getAsInt();
            screenY = screenPosition.get(1).getAsInt();
        }

        return new HudAtlasElement(originX, originY, frameWidth, frameHeight, frameCount,
                layout, type, clientConnector, staticFrame, screenX, screenY);
    }

    private static JsonArray requireArray(JsonObject el, String id, String field) {
        if (!el.has(field)) {
            throw new HudAtlasParseException("Element '" + id + "' is missing required '" + field + "' field");
        }
        return el.getAsJsonArray(field);
    }
}
