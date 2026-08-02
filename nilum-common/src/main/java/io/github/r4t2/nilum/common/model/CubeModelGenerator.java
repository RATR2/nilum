package io.github.r4t2.nilum.common.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Synthesizes a plain .bbmodel JSON (the format BbModelParser reads) for a full 0..16 cube with
 * one texture per face, the "default retexture" block model for a solid block reskinned per-face
 * without authoring an actual Blockbench model. Downstream systems treat the output identically
 * to a hand-authored model.
 */
public final class CubeModelGenerator {

    private static final String[] FACES = {"north", "south", "east", "west", "up", "down"};

    private CubeModelGenerator() {
    }

    /** @param facePngBytes must contain exactly one entry per direction in FACES. */
    public static byte[] generate(Map<String, byte[]> facePngBytes) {
        // Dedupe identical textures (e.g. one PNG shared across all 6 faces) by their own base64
        // text, so we build/ship each distinct texture once rather than once per face.
        Map<String, Integer> textureIndexByBase64 = new LinkedHashMap<>();
        JsonArray texturesJson = new JsonArray();
        Map<String, Integer> faceToTextureIndex = new LinkedHashMap<>();

        for (String face : FACES) {
            byte[] png = facePngBytes.get(face);
            if (png == null) {
                throw new IllegalArgumentException("missing texture for face '" + face + "'");
            }
            String base64 = Base64.getEncoder().encodeToString(png);
            Integer index = textureIndexByBase64.get(base64);
            if (index == null) {
                index = texturesJson.size();
                textureIndexByBase64.put(base64, index);
                JsonObject texture = new JsonObject();
                texture.addProperty("id", String.valueOf(index));
                texture.addProperty("name", face);
                // UVs below are authored already-normalized (0..1), and uv_width/uv_height are set
                // to 1 to match, so the whole texture always maps to the whole face, regardless of
                // its real pixel dimensions. width/height are otherwise unused by the renderer.
                texture.addProperty("width", 1);
                texture.addProperty("height", 1);
                texture.addProperty("uv_width", 1);
                texture.addProperty("uv_height", 1);
                texture.addProperty("source", "data:image/png;base64," + base64);
                texturesJson.add(texture);
            }
            faceToTextureIndex.put(face, index);
        }

        JsonObject facesJson = new JsonObject();
        for (String face : FACES) {
            JsonObject faceJson = new JsonObject();
            JsonArray uv = new JsonArray();
            uv.add(0);
            uv.add(0);
            uv.add(1);
            uv.add(1);
            faceJson.add("uv", uv);
            faceJson.addProperty("texture", faceToTextureIndex.get(face));
            facesJson.add(face, faceJson);
        }

        JsonObject element = new JsonObject();
        element.addProperty("type", "cube");
        element.addProperty("uuid", "nilum-cube");
        element.addProperty("name", "cube");
        element.add("from", vector(0, 0, 0));
        element.add("to", vector(16, 16, 16));
        element.add("origin", vector(8, 8, 8));
        element.add("rotation", vector(0, 0, 0));
        element.add("faces", facesJson);

        JsonArray elementsJson = new JsonArray();
        elementsJson.add(element);

        JsonArray outlinerJson = new JsonArray();
        outlinerJson.add("nilum-cube");

        JsonObject meta = new JsonObject();
        meta.addProperty("format_version", "4.5");

        JsonObject resolution = new JsonObject();
        resolution.addProperty("width", 16);
        resolution.addProperty("height", 16);

        JsonObject root = new JsonObject();
        root.add("meta", meta);
        root.add("resolution", resolution);
        root.add("elements", elementsJson);
        root.add("outliner", outlinerJson);
        root.add("textures", texturesJson);

        return root.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static JsonArray vector(double x, double y, double z) {
        JsonArray array = new JsonArray();
        array.add(x);
        array.add(y);
        array.add(z);
        return array;
    }
}
