package io.github.r4t2.nilum.common.hud;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses a .atlas descriptor in the project's hand-rolled YAML-flavored grammar: top-level key: value lines plus element blocks. */
public final class HudAtlasParser {

    private static final Pattern ELEMENT_HEADER = Pattern.compile("^element\\s+(\\S[\\w.-]*):\\s*$");
    private static final Pattern FIELD_LINE = Pattern.compile("^\\s+(\\S[\\w.-]*):\\s*(.*)$");
    private static final Pattern TOP_LEVEL_LINE = Pattern.compile("^(\\S[\\w.-]*):\\s*(.*)$");

    private HudAtlasParser() {
    }

    public static HudAtlasDescriptor parse(String source) {
        Map<String, String> topLevel = new LinkedHashMap<>();
        Map<String, Map<String, String>> elementFields = new LinkedHashMap<>();

        String currentElement = null;
        String[] lines = source.split("\n", -1);
        for (int lineNumber = 1; lineNumber <= lines.length; lineNumber++) {
            String line = lines[lineNumber - 1];
            if (line.isBlank() || line.stripLeading().startsWith("#")) {
                continue;
            }

            Matcher elementMatcher = ELEMENT_HEADER.matcher(line);
            if (elementMatcher.matches()) {
                currentElement = elementMatcher.group(1);
                elementFields.putIfAbsent(currentElement, new LinkedHashMap<>());
                continue;
            }

            Matcher fieldMatcher = FIELD_LINE.matcher(line);
            if (fieldMatcher.matches()) {
                if (currentElement == null) {
                    throw new HudAtlasParseException("Line " + lineNumber + ": indented field outside any 'element' block: " + line);
                }
                elementFields.get(currentElement).put(fieldMatcher.group(1), unquote(fieldMatcher.group(2).trim()));
                continue;
            }

            Matcher topMatcher = TOP_LEVEL_LINE.matcher(line);
            if (topMatcher.matches()) {
                currentElement = null;
                topLevel.put(topMatcher.group(1), unquote(topMatcher.group(2).trim()));
                continue;
            }

            throw new HudAtlasParseException("Line " + lineNumber + ": couldn't parse: " + line);
        }

        // Only Sprite/Duplicate elements need the shared spritesheet's dimensions; an atlas made
        // entirely of Text/Image elements (each sourcing its own texture) doesn't need one.
        int[] atlasSize = topLevel.containsKey("atlas_size") ? parseIntArray(topLevel.get("atlas_size")) : new int[]{0, 0};

        Map<String, HudAtlasElement> elements = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, String>> entry : elementFields.entrySet()) {
            elements.put(entry.getKey(), parseElement(entry.getKey(), entry.getValue()));
        }

        return new HudAtlasDescriptor(atlasSize[0], atlasSize[1], elements);
    }

    private static HudAtlasElement parseElement(String id, Map<String, String> fields) {
        String typeStr = require(fields, id, "type");

        if (typeStr.equalsIgnoreCase("render_text")) {
            return parseTextElement(id, fields);
        }
        if (typeStr.equalsIgnoreCase("duplicate")) {
            return parseDuplicateElement(id, fields);
        }
        if (typeStr.equalsIgnoreCase("image")) {
            return parseImageElement(id, fields);
        }
        return parseSpriteElement(id, fields, typeStr);
    }

    private static HudAtlasElement.Sprite parseSpriteElement(String id, Map<String, String> fields, String typeStr) {
        int[] origin = requireIntArray(fields, "origin", "Element '" + id + "'");
        int[] frameSize = requireIntArray(fields, "frame_size", "Element '" + id + "'");
        int frameCount = fields.containsKey("frame_count") ? Integer.parseInt(fields.get("frame_count")) : 1;

        HudElementLayout layout = fields.containsKey("layout")
                ? HudElementLayout.valueOf(fields.get("layout").toUpperCase(Locale.ROOT))
                : HudElementLayout.HORIZONTAL;

        HudElementType type = HudElementType.valueOf(typeStr.toUpperCase(Locale.ROOT));
        Optional<String> clientConnector = Optional.ofNullable(fields.get("client_connector"));
        if (type == HudElementType.AUTO && clientConnector.isEmpty()) {
            throw new HudAtlasParseException("Element '" + id + "' is type 'auto' but has no 'client_connector'");
        }

        int staticFrame = fields.containsKey("static_frame") ? Integer.parseInt(fields.get("static_frame")) : 0;
        int[] screenPosition = parseScreenPosition(fields);

        return new HudAtlasElement.Sprite(origin[0], origin[1], frameSize[0], frameSize[1], frameCount,
                layout, type, clientConnector, staticFrame, screenPosition[0], screenPosition[1]);
    }

    private static HudAtlasElement.Duplicate parseDuplicateElement(String id, Map<String, String> fields) {
        int[] origin = requireIntArray(fields, "origin", "Element '" + id + "'");
        int[] frameSize = requireIntArray(fields, "frame_size", "Element '" + id + "'");
        int frameCount = fields.containsKey("frame_count") ? Integer.parseInt(fields.get("frame_count")) : 1;

        HudElementLayout layout = fields.containsKey("layout")
                ? HudElementLayout.valueOf(fields.get("layout").toUpperCase(Locale.ROOT))
                : HudElementLayout.HORIZONTAL;

        Optional<String> clientConnector = Optional.ofNullable(fields.get("client_connector"));
        int staticCount = fields.containsKey("static_count") ? Integer.parseInt(fields.get("static_count")) : 0;
        HudElementType type = clientConnector.isPresent() ? HudElementType.AUTO
                : fields.containsKey("static_count") ? HudElementType.STATIC
                : HudElementType.SERVER;

        int imageFrame = fields.containsKey("image_frame") ? Integer.parseInt(fields.get("image_frame")) : 0;
        int[] offset = requireIntArray(fields, "duplicate_offset", "Element '" + id + "'");
        int[] screenPosition = parseScreenPosition(fields);

        return new HudAtlasElement.Duplicate(origin[0], origin[1], frameSize[0], frameSize[1], frameCount,
                layout, type, clientConnector, staticCount, imageFrame, offset[0], offset[1],
                screenPosition[0], screenPosition[1]);
    }

    private static HudAtlasElement.Image parseImageElement(String id, Map<String, String> fields) {
        String textureFile = require(fields, id, "image");
        int[] origin = requireIntArray(fields, "origin", "Element '" + id + "'");
        int[] frameSize = requireIntArray(fields, "frame_size", "Element '" + id + "'");
        int frameCount = fields.containsKey("frame_count") ? Integer.parseInt(fields.get("frame_count")) : 1;

        HudElementLayout layout = fields.containsKey("layout")
                ? HudElementLayout.valueOf(fields.get("layout").toUpperCase(Locale.ROOT))
                : HudElementLayout.HORIZONTAL;

        // "type" already names the element kind ("image"), so unlike Sprite, an Image element's
        // auto/server/static frame source is inferred from which other fields are present, same
        // as Duplicate does.
        Optional<String> clientConnector = Optional.ofNullable(fields.get("client_connector"));
        int staticFrame = fields.containsKey("static_frame") ? Integer.parseInt(fields.get("static_frame")) : 0;
        HudElementType type = clientConnector.isPresent() ? HudElementType.AUTO
                : fields.containsKey("static_frame") ? HudElementType.STATIC
                : HudElementType.SERVER;

        int[] screenPosition = parseScreenPosition(fields);
        return new HudAtlasElement.Image(textureFile, origin[0], origin[1], frameSize[0], frameSize[1], frameCount,
                layout, type, clientConnector, staticFrame, screenPosition[0], screenPosition[1]);
    }

    private static HudAtlasElement.Text parseTextElement(String id, Map<String, String> fields) {
        String font = fields.getOrDefault("font", "default");
        Optional<String> clientConnector = Optional.ofNullable(fields.get("client_connector"));
        Optional<String> serverConnector = Optional.ofNullable(fields.get("server_connector"));
        Optional<String> format = Optional.ofNullable(fields.get("format"));

        if (clientConnector.isEmpty() && serverConnector.isEmpty()) {
            throw new HudAtlasParseException("Element '" + id
                    + "' is type 'render_text' but has neither 'client_connector' nor 'server_connector'");
        }

        int[] screenPosition = parseScreenPosition(fields);
        return new HudAtlasElement.Text(font, clientConnector, serverConnector, format, screenPosition[0], screenPosition[1]);
    }

    private static int[] parseScreenPosition(Map<String, String> fields) {
        return fields.containsKey("screen_position") ? parseIntArray(fields.get("screen_position")) : new int[] {0, 0};
    }

    private static String require(Map<String, String> fields, String id, String field) {
        String value = fields.get(field);
        if (value == null) {
            throw new HudAtlasParseException("Element '" + id + "' is missing required '" + field + "' field");
        }
        return value;
    }

    private static int[] requireIntArray(Map<String, String> fields, String field, String context) {
        String raw = fields.get(field);
        if (raw == null) {
            throw new HudAtlasParseException(context + " is missing required '" + field + "' field");
        }
        return parseIntArray(raw);
    }

    private static int[] parseIntArray(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        String[] parts = trimmed.split(",");
        int[] values = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            values[i] = Integer.parseInt(parts[i].trim());
        }
        return values;
    }

    private static String unquote(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
