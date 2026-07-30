package io.github.r4t2.nilum.common.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class BbModelParser {

    private BbModelParser() {
    }

    public static BbModel parse(String json) {
        JsonObject root;
        try {
            root = JsonParser.parseString(json).getAsJsonObject();
        } catch (JsonParseException | IllegalStateException e) {
            throw new BbModelParseException("Not a valid .bbmodel JSON object", e);
        }

        String formatVersion = root.has("meta") && root.getAsJsonObject("meta").has("format_version")
                ? root.getAsJsonObject("meta").get("format_version").getAsString()
                : "unknown";

        JsonObject resolution = root.has("resolution") ? root.getAsJsonObject("resolution") : null;
        int resolutionWidth = resolution != null && resolution.has("width") ? resolution.get("width").getAsInt() : 16;
        int resolutionHeight = resolution != null && resolution.has("height") ? resolution.get("height").getAsInt() : 16;

        if (!root.has("elements") || !root.has("outliner")) {
            throw new BbModelParseException("Missing required 'elements' or 'outliner' field");
        }

        Map<String, BbElement> elements = parseElements(root.getAsJsonArray("elements"));
        Map<String, GroupMeta> groupMeta = parseGroupMeta(root.getAsJsonArray("groups"));
        List<BbOutlinerNode> outliner = parseOutliner(root.getAsJsonArray("outliner"), groupMeta);
        List<BbTexture> textures = parseTextures(root.getAsJsonArray("textures"));

        // Nilum-specific convention
        Optional<String> collisionIntent = root.has("collision_intent")
                ? Optional.of(root.get("collision_intent").getAsString())
                : Optional.empty();

        return new BbModel(formatVersion, resolutionWidth, resolutionHeight, elements, outliner, textures, collisionIntent);
    }

    private static Map<String, BbElement> parseElements(JsonArray elementsJson) {
        Map<String, BbElement> elements = new LinkedHashMap<>();
        for (JsonElement raw : elementsJson) {
            JsonObject el = raw.getAsJsonObject();

            String type = el.has("type") ? el.get("type").getAsString() : "cube";
            if (!"cube".equals(type)) {
                continue;
            }

            String uuid = requireString(el, "uuid");
            String name = el.has("name") ? el.get("name").getAsString() : uuid;
            BbVector3 from = parseVector3(el.getAsJsonArray("from"));
            BbVector3 to = parseVector3(el.getAsJsonArray("to"));
            BbVector3 origin = el.has("origin") ? parseVector3(el.getAsJsonArray("origin")) : BbVector3.ZERO;
            BbVector3 rotation = el.has("rotation") ? parseVector3(el.getAsJsonArray("rotation")) : BbVector3.ZERO;
            Map<String, BbFace> faces = el.has("faces") ? parseFaces(el.getAsJsonObject("faces")) : Map.of();

            elements.put(uuid, new BbElement(uuid, name, from, to, origin, rotation, faces));
        }
        return elements;
    }

    private static Map<String, BbFace> parseFaces(JsonObject facesJson) {
        Map<String, BbFace> faces = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : facesJson.entrySet()) {
            JsonObject face = entry.getValue().getAsJsonObject();
            JsonArray uv = face.getAsJsonArray("uv");

            Integer textureIndex = null;
            if (face.has("texture") && face.get("texture").isJsonPrimitive()
                    && face.get("texture").getAsJsonPrimitive().isNumber()) {
                textureIndex = face.get("texture").getAsInt();
            }
            int rotation = face.has("rotation") ? face.get("rotation").getAsInt() : 0;

            faces.put(entry.getKey(), new BbFace(
                    uv.get(0).getAsDouble(), uv.get(1).getAsDouble(),
                    uv.get(2).getAsDouble(), uv.get(3).getAsDouble(),
                    textureIndex, rotation));
        }
        return faces;
    }

    /** Metadata for a group, sourced from the top-level "groups" array. Purely an internal parse-time detail. */
    private record GroupMeta(String name, BbVector3 origin, BbVector3 rotation) {
    }

    private static Map<String, GroupMeta> parseGroupMeta(JsonArray groupsJson) {
        Map<String, GroupMeta> groupMeta = new LinkedHashMap<>();
        if (groupsJson == null) {
            return groupMeta;
        }
        for (JsonElement raw : groupsJson) {
            JsonObject g = raw.getAsJsonObject();
            String uuid = requireString(g, "uuid");
            String name = g.has("name") ? g.get("name").getAsString() : uuid;
            BbVector3 origin = g.has("origin") ? parseVector3(g.getAsJsonArray("origin")) : BbVector3.ZERO;
            BbVector3 rotation = g.has("rotation") ? parseVector3(g.getAsJsonArray("rotation")) : BbVector3.ZERO;
            groupMeta.put(uuid, new GroupMeta(name, origin, rotation));
        }
        return groupMeta;
    }

    private static List<BbOutlinerNode> parseOutliner(JsonArray nodesJson, Map<String, GroupMeta> groupMeta) {
        List<BbOutlinerNode> nodes = new ArrayList<>();
        for (JsonElement raw : nodesJson) {
            if (raw.isJsonPrimitive()) {
                nodes.add(new BbOutlinerElementRef(raw.getAsString()));
                continue;
            }

            JsonObject obj = raw.getAsJsonObject();
            String uuid = requireString(obj, "uuid");

            String name;
            BbVector3 origin;
            BbVector3 rotation;
            if (obj.has("name")) {
                name = obj.get("name").getAsString();
                origin = obj.has("origin") ? parseVector3(obj.getAsJsonArray("origin")) : BbVector3.ZERO;
                rotation = obj.has("rotation") ? parseVector3(obj.getAsJsonArray("rotation")) : BbVector3.ZERO;
            } else {
                GroupMeta meta = groupMeta.get(uuid);
                if (meta == null) {
                    throw new BbModelParseException(
                            "Outliner group " + uuid + " has no 'name' and no matching entry in 'groups'");
                }
                name = meta.name();
                origin = meta.origin();
                rotation = meta.rotation();
            }

            List<BbOutlinerNode> children = obj.has("children")
                    ? parseOutliner(obj.getAsJsonArray("children"), groupMeta)
                    : List.of();

            nodes.add(new BbOutlinerGroup(uuid, name, origin, rotation, children));
        }
        return nodes;
    }

    private static List<BbTexture> parseTextures(JsonArray texturesJson) {
        List<BbTexture> textures = new ArrayList<>();
        if (texturesJson == null) {
            return textures;
        }
        for (JsonElement raw : texturesJson) {
            JsonObject t = raw.getAsJsonObject();
            String id = t.has("id") ? t.get("id").getAsString() : "";
            String name = t.has("name") ? t.get("name").getAsString() : id;
            int width = t.has("width") ? t.get("width").getAsInt() : 0;
            int height = t.has("height") ? t.get("height").getAsInt() : 0;

            byte[] pngBytes = new byte[0];
            if (t.has("source")) {
                String source = t.get("source").getAsString();
                int base64Marker = source.indexOf("base64,");
                if (base64Marker >= 0) {
                    pngBytes = Base64.getDecoder().decode(source.substring(base64Marker + "base64,".length()));
                }
            }

            textures.add(new BbTexture(id, name, width, height, pngBytes));
        }
        return textures;
    }

    private static BbVector3 parseVector3(JsonArray arr) {
        if (arr == null || arr.size() < 3) {
            return BbVector3.ZERO;
        }
        return new BbVector3(arr.get(0).getAsDouble(), arr.get(1).getAsDouble(), arr.get(2).getAsDouble());
    }

    private static String requireString(JsonObject obj, String field) {
        if (!obj.has(field)) {
            throw new BbModelParseException("Missing required '" + field + "' field");
        }
        return obj.get(field).getAsString();
    }
}
