package io.github.r4t2.nilum.common.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;


public final class ConfigKey<T> {

    private final String section;
    private final String key;
    private final T defaultValue;
    private final String comment;
    private final int sinceVersion;
    private final Function<String, T> parser;
    private final Function<T, String> serializer;
    private final Predicate<T> validator;
    private final String validationMessage;

    private ConfigKey(String section, String key, T defaultValue, String comment, int sinceVersion,
                       Function<String, T> parser, Function<T, String> serializer,
                       Predicate<T> validator, String validationMessage) {
        this.section = section;
        this.key = key;
        this.defaultValue = defaultValue;
        this.comment = comment;
        this.sinceVersion = sinceVersion;
        this.parser = parser;
        this.serializer = serializer;
        this.validator = validator;
        this.validationMessage = validationMessage;
    }

    public static ConfigKey<String> ofString(String section, String key, String defaultValue, String comment, int sinceVersion) {
        return new ConfigKey<>(section, key, defaultValue, comment, sinceVersion, s -> s, s -> s, s -> true, null);
    }

    public static ConfigKey<String> ofString(String section, String key, String defaultValue, String comment, int sinceVersion,
                                              Predicate<String> validator, String validationMessage) {
        return new ConfigKey<>(section, key, defaultValue, comment, sinceVersion, s -> s, s -> s, validator, validationMessage);
    }

    public static ConfigKey<Integer> ofInt(String section, String key, int defaultValue, String comment, int sinceVersion) {
        return new ConfigKey<>(section, key, defaultValue, comment, sinceVersion, Integer::parseInt, String::valueOf, i -> true, null);
    }

    public static ConfigKey<Integer> ofInt(String section, String key, int defaultValue, String comment, int sinceVersion,
                                            Predicate<Integer> validator, String validationMessage) {
        return new ConfigKey<>(section, key, defaultValue, comment, sinceVersion, Integer::parseInt, String::valueOf, validator, validationMessage);
    }

    public static ConfigKey<Boolean> ofBoolean(String section, String key, boolean defaultValue, String comment, int sinceVersion) {
        return new ConfigKey<>(section, key, defaultValue, comment, sinceVersion, Boolean::parseBoolean, String::valueOf, b -> true, null);
    }

    /** Stores an enum constant as its lowercase name. */
    public static <E extends Enum<E>> ConfigKey<E> ofEnum(String section, String key, E defaultValue, String comment, int sinceVersion, Class<E> type) {
        return new ConfigKey<>(section, key, defaultValue, comment, sinceVersion,
                raw -> Enum.valueOf(type, raw.trim().toUpperCase(Locale.ROOT)),
                value -> value.name().toLowerCase(Locale.ROOT), value -> true, null);
    }

    public static ConfigKey<List<String>> ofStringList(String section, String key, List<String> defaultValue, String comment, int sinceVersion) {
        return new ConfigKey<>(section, key, defaultValue, comment, sinceVersion,
                ConfigKey::parseStringList, ConfigKey::serializeStringList, list -> true, null);
    }

    private static List<String> parseStringList(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        if (trimmed.isBlank()) {
            return List.of();
        }

        List<String> values = new ArrayList<>();
        for (String part : trimmed.split(",")) {
            String value = part.trim();
            if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1);
            }
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        return List.copyOf(values);
    }

    private static String serializeStringList(List<String> values) {
        StringBuilder out = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                out.append(",");
            }
            out.append('"').append(values.get(i)).append('"');
        }
        return out.append(']').toString();
    }

    public String section() {
        return section;
    }

    public String key() {
        return key;
    }

    public T defaultValue() {
        return defaultValue;
    }

    public String comment() {
        return comment;
    }

    /** The config schema version this key was introduced in; ConfigSchema.currentVersion() is derived from these. */
    public int sinceVersion() {
        return sinceVersion;
    }

    /** Parses and validates raw text from the file; null means "use the default," caller logs why. */
    public T parseOrNull(String rawValue) {
        T parsed;
        try {
            parsed = parser.apply(rawValue);
        } catch (RuntimeException e) {
            return null;
        }
        return validator.test(parsed) ? parsed : null;
    }

    public String serialize(T value) {
        return serializer.apply(value);
    }

    public String validationMessage() {
        return validationMessage;
    }
}
