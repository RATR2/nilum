package io.github.r4t2.nilum.common.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public final class ConfigFileWriter {

    private ConfigFileWriter() {
    }

    public static String write(int version, String header, ConfigSchema schema, ConfigValueLookup values) {
        Map<String, List<ConfigKey<?>>> bySection = new LinkedHashMap<>();
        for (ConfigKey<?> key : schema.keys()) {
            bySection.computeIfAbsent(key.section(), s -> new ArrayList<>()).add(key);
        }

        StringBuilder out = new StringBuilder();
        if (header != null && !header.isBlank()) {
            for (String headerLine : header.split("\n")) {
                out.append("# ").append(headerLine).append('\n');
            }
        }
        out.append("version: ").append(version).append("\n\n");

        boolean firstSection = true;
        for (Map.Entry<String, List<ConfigKey<?>>> entry : bySection.entrySet()) {
            if (!firstSection) {
                out.append('\n');
            }
            firstSection = false;

            out.append(entry.getKey()).append(":\n");
            for (ConfigKey<?> key : entry.getValue()) {
                appendKey(out, key, values);
            }
        }

        return out.toString();
    }

    private static <T> void appendKey(StringBuilder out, ConfigKey<T> key, ConfigValueLookup values) {
        if (key.comment() != null && !key.comment().isBlank()) {
            for (String commentLine : key.comment().split("\n")) {
                out.append("  # ").append(commentLine).append('\n');
            }
        }
        String serialized = key.serialize(values.valueOf(key));
        out.append("  ").append(key.key()).append(": ").append(quoteIfNeeded(serialized)).append('\n');
    }

    private static String quoteIfNeeded(String value) {
        if (value.isEmpty() || !value.equals(value.trim()) || value.contains("#")) {
            return "\"" + value + "\"";
        }
        return value;
    }
}
