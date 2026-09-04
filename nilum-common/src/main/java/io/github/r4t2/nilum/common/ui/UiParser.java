package io.github.r4t2.nilum.common.ui;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses a .ui descriptor in the same hand-rolled grammar as .atlas: top-level key: value lines plus element blocks. */
public final class UiParser {

    private static final Pattern ELEMENT_HEADER = Pattern.compile("^element\\s+(\\S[\\w.-]*):\\s*$");
    private static final Pattern FIELD_LINE = Pattern.compile("^\\s+(\\S[\\w.-]*):\\s*(.*)$");
    private static final Pattern TOP_LEVEL_LINE = Pattern.compile("^(\\S[\\w.-]*):\\s*(.*)$");

    private UiParser() {
    }

    public static UiDescriptor parse(String source) {
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
                    throw new UiParseException("Line " + lineNumber + ": indented field outside any 'element' block: " + line);
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

            throw new UiParseException("Line " + lineNumber + ": couldn't parse: " + line);
        }

        String type = topLevel.getOrDefault("type", "custom");
        if (!type.equalsIgnoreCase("custom")) {
            throw new UiParseException("Unsupported UI type '" + type + "', only 'custom' is supported so far");
        }

        UiAnchor anchor = parseAnchor(topLevel.getOrDefault("anchor", "top-left"));

        Map<String, UiElement> elements = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, String>> entry : elementFields.entrySet()) {
            elements.put(entry.getKey(), parseElement(entry.getKey(), entry.getValue()));
        }

        return new UiDescriptor(anchor, elements);
    }

    private static UiAnchor parseAnchor(String raw) {
        return switch (raw.toLowerCase(java.util.Locale.ROOT)) {
            case "top-left" -> UiAnchor.TOP_LEFT;
            case "center" -> UiAnchor.CENTER;
            default -> throw new UiParseException("Unknown anchor '" + raw + "', expected 'top-left' or 'center'");
        };
    }

    private static UiElement parseElement(String id, Map<String, String> fields) {
        String typeStr = require(fields, id, "type");
        int[] position = requireIntArray(fields, "position", "Element '" + id + "'");
        int layer = fields.containsKey("layer") ? Integer.parseInt(fields.get("layer")) : 0;
        Optional<String> requirement = Optional.ofNullable(fields.get("requirement"));

        if (typeStr.equalsIgnoreCase("button")) {
            String imageFile = require(fields, id, "image");
            String pressedImageFile = require(fields, id, "pressed");
            Optional<String> action = Optional.ofNullable(fields.get("action"));
            return new UiElement.Button(imageFile, pressedImageFile, position[0], position[1], layer, requirement, action);
        }
        if (typeStr.equalsIgnoreCase("image")) {
            String imageFile = require(fields, id, "image");
            return new UiElement.Image(imageFile, position[0], position[1], layer, requirement);
        }
        if (typeStr.equalsIgnoreCase("text")) {
            return parseTextElement(id, fields, position, layer, requirement);
        }
        if (typeStr.equalsIgnoreCase("head")) {
            String player = require(fields, id, "player");
            return new UiElement.Head(player, position[0], position[1], layer, requirement);
        }
        throw new UiParseException("Element '" + id + "' has unknown type '" + typeStr + "'");
    }

    private static UiElement.Text parseTextElement(String id, Map<String, String> fields, int[] position, int layer,
                                                     Optional<String> requirement) {
        String font = fields.getOrDefault("font", "default");
        Optional<String> text = Optional.ofNullable(fields.get("text"));
        Optional<String> clientConnector = Optional.ofNullable(fields.get("client_connector"));
        if (text.isEmpty() && clientConnector.isEmpty()) {
            throw new UiParseException("Element '" + id + "' is type 'text' but has neither 'text' nor 'client_connector'");
        }
        if (text.isPresent() && clientConnector.isPresent()) {
            throw new UiParseException("Element '" + id + "' has both 'text' and 'client_connector', only one is allowed");
        }
        int color = fields.containsKey("color") ? parseColor(fields.get("color")) : 0xFFFFFFFF;
        return new UiElement.Text(font, text, clientConnector, color, position[0], position[1], layer, requirement);
    }

    private static int parseColor(String raw) {
        String hex = raw.startsWith("#") ? raw.substring(1) : raw;
        return 0xFF000000 | Integer.parseInt(hex, 16);
    }

    private static String require(Map<String, String> fields, String id, String field) {
        String value = fields.get(field);
        if (value == null) {
            throw new UiParseException("Element '" + id + "' is missing required '" + field + "' field");
        }
        return value;
    }

    private static int[] requireIntArray(Map<String, String> fields, String field, String context) {
        String raw = fields.get(field);
        if (raw == null) {
            throw new UiParseException(context + " is missing required '" + field + "' field");
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
