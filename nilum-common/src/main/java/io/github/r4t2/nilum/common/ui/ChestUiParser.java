package io.github.r4t2.nilum.common.ui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses a type: chest .ui descriptor: top-level rows/title/background plus slot element blocks. */
public final class ChestUiParser {

    private static final Pattern ELEMENT_HEADER = Pattern.compile("^element\\s+(\\S[\\w.-]*):\\s*$");
    private static final Pattern FIELD_LINE = Pattern.compile("^\\s+(\\S[\\w.-]*):\\s*(.*)$");
    private static final Pattern TOP_LEVEL_LINE = Pattern.compile("^(\\S[\\w.-]*):\\s*(.*)$");

    private ChestUiParser() {
    }

    public static ChestUiDescriptor parse(String source) {
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
        if (!type.equalsIgnoreCase("chest")) {
            throw new UiParseException("ChestUiParser invoked on a non-chest UI file (type '" + type + "')");
        }

        int rows = Integer.parseInt(topLevel.getOrDefault("rows", "3"));
        if (rows < 1 || rows > 6) {
            throw new UiParseException("Chest UI 'rows' must be between 1 and 6, got " + rows);
        }
        String title = topLevel.getOrDefault("title", "");
        String background = require(topLevel, "<top level>", "background");

        Map<String, ChestUiElement> elements = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, String>> entry : elementFields.entrySet()) {
            elements.put(entry.getKey(), parseElement(entry.getKey(), entry.getValue(), rows));
        }

        return new ChestUiDescriptor(rows, title, background, elements);
    }

    private static ChestUiElement parseElement(String id, Map<String, String> fields, int rows) {
        int slot = Integer.parseInt(require(fields, id, "slot"));
        int slotCount = rows * 9;
        if (slot < 0 || slot >= slotCount) {
            throw new UiParseException("Element '" + id + "' has slot " + slot + ", out of range for "
                    + rows + " rows (0-" + (slotCount - 1) + ")");
        }
        String material = require(fields, id, "item");
        Optional<String> name = Optional.ofNullable(fields.get("name"));
        List<String> lore = fields.containsKey("lore") ? parseList(fields.get("lore")) : List.of();
        Optional<String> action = Optional.ofNullable(fields.get("action"));
        Optional<String> requirement = Optional.ofNullable(fields.get("requirement"));
        return new ChestUiElement(slot, material, name, lore, action, requirement);
    }

    private static List<String> parseList(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        if (trimmed.isBlank()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (String part : trimmed.split("\\|")) {
            values.add(unquote(part.trim()));
        }
        return values;
    }

    private static String require(Map<String, String> fields, String id, String field) {
        String value = fields.get(field);
        if (value == null) {
            throw new UiParseException("Element '" + id + "' is missing required '" + field + "' field");
        }
        return value;
    }

    private static String unquote(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
