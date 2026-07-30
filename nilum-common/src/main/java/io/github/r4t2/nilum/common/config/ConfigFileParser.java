package io.github.r4t2.nilum.common.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class ConfigFileParser {

    private static final Logger LOGGER = Logger.getLogger("nilum");

    private static final Pattern VERSION_LINE = Pattern.compile("^version:\\s*(\\d+)\\s*$");
    private static final Pattern SECTION_LINE = Pattern.compile("^(\\S[\\w.-]*):\\s*$");
    private static final Pattern KEY_LINE = Pattern.compile("^\\s+(\\S[\\w.-]*):\\s*(.*)$");

    private ConfigFileParser() {
    }

    public static RawConfigFile parse(List<String> lines) {
        int version = 0;
        Map<String, Map<String, String>> sections = new LinkedHashMap<>();
        String currentSection = null;

        for (int lineNumber = 1; lineNumber <= lines.size(); lineNumber++) {
            String line = lines.get(lineNumber - 1);

            if (line.isBlank() || line.stripLeading().startsWith("#")) {
                continue;
            }

            Matcher versionMatcher = VERSION_LINE.matcher(line);
            if (versionMatcher.matches()) {
                version = Integer.parseInt(versionMatcher.group(1));
                continue;
            }

            Matcher keyMatcher = KEY_LINE.matcher(line);
            if (keyMatcher.matches()) {
                if (currentSection == null) {
                    LOGGER.warning("Nilum config line " + lineNumber + ": key outside any section, ignoring: " + line);
                    continue;
                }
                sections.computeIfAbsent(currentSection, s -> new LinkedHashMap<>())
                        .put(keyMatcher.group(1), unquote(keyMatcher.group(2).trim()));
                continue;
            }

            Matcher sectionMatcher = SECTION_LINE.matcher(line);
            if (sectionMatcher.matches()) {
                currentSection = sectionMatcher.group(1);
                sections.putIfAbsent(currentSection, new LinkedHashMap<>());
                continue;
            }

            LOGGER.warning("Nilum config line " + lineNumber + ": couldn't parse, ignoring: " + line);
        }

        return new RawConfigFile(version, sections);
    }

    private static String unquote(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
