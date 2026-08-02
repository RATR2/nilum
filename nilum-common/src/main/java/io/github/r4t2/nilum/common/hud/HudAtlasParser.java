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
 * Parses a .atlas descriptor as JSON (the design doc's example is YAML) since Gson is already
 * on every platform's runtime classpath and SnakeYAML isn't, without bundling a new dependency.
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
        if (!el.has("type")) {
            throw new HudAtlasParseException("Element '" + id + "' is missing required 'type' field");
        }
        String typeStr = el.get("type").getAsString();

        if (typeStr.equalsIgnoreCase("render_text")) {
            return parseTextElement(id, el);
        }
        return parseSpriteElement(id, el, typeStr);
    }

    private static HudAtlasElement.Sprite parseSpriteElement(String id, JsonObject el, String typeStr) {
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

        HudElementType type = HudElementType.valueOf(typeStr.toUpperCase(Locale.ROOT));

        Optional<String> clientConnector = el.has("client_connector")
                ? Optional.of(el.get("client_connector").getAsString())
                : Optional.empty();

        if (type == HudElementType.AUTO && clientConnector.isEmpty()) {
            throw new HudAtlasParseException("Element '" + id + "' is type 'auto' but has no 'client_connector'");
        }

        int staticFrame = el.has("static_frame") ? el.get("static_frame").getAsInt() : 0;
        int[] screenPosition = parseScreenPosition(el);

        return new HudAtlasElement.Sprite(originX, originY, frameWidth, frameHeight, frameCount,
                layout, type, clientConnector, staticFrame, screenPosition[0], screenPosition[1]);
    }

    private static HudAtlasElement.Text parseTextElement(String id, JsonObject el) {
        String font = el.has("font") ? el.get("font").getAsString() : "default";

        Optional<String> clientConnector = el.has("client_connector")
                ? Optional.of(el.get("client_connector").getAsString())
                : Optional.empty();
        Optional<String> serverConnector = el.has("server_connector")
                ? Optional.of(el.get("server_connector").getAsString())
                : Optional.empty();

        if (clientConnector.isEmpty() && serverConnector.isEmpty()) {
            throw new HudAtlasParseException("Element '" + id
                    + "' is type 'render_text' but has neither 'client_connector' nor 'server_connector'");
        }

        int[] screenPosition = parseScreenPosition(el);
        return new HudAtlasElement.Text(font, clientConnector, serverConnector, screenPosition[0], screenPosition[1]);
    }

    private static int[] parseScreenPosition(JsonObject el) {
        if (!el.has("screen_position")) {
            return new int[] {0, 0};
        }
        JsonArray screenPosition = el.getAsJsonArray("screen_position");
        return new int[] {screenPosition.get(0).getAsInt(), screenPosition.get(1).getAsInt()};
    }

    private static JsonArray requireArray(JsonObject el, String id, String field) {
        if (!el.has(field)) {
            throw new HudAtlasParseException("Element '" + id + "' is missing required '" + field + "' field");
        }
        return el.getAsJsonArray(field);
    }
}
