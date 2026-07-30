package io.github.r4t2.nilum.common.config;

import java.util.function.Function;
import java.util.function.Predicate;


public final class ConfigKey<T> {

    private final String section;
    private final String key;
    private final T defaultValue;
    private final String comment;
    private final Function<String, T> parser;
    private final Function<T, String> serializer;
    private final Predicate<T> validator;
    private final String validationMessage;

    private ConfigKey(String section, String key, T defaultValue, String comment,
                       Function<String, T> parser, Function<T, String> serializer,
                       Predicate<T> validator, String validationMessage) {
        this.section = section;
        this.key = key;
        this.defaultValue = defaultValue;
        this.comment = comment;
        this.parser = parser;
        this.serializer = serializer;
        this.validator = validator;
        this.validationMessage = validationMessage;
    }

    public static ConfigKey<String> ofString(String section, String key, String defaultValue, String comment) {
        return new ConfigKey<>(section, key, defaultValue, comment, s -> s, s -> s, s -> true, null);
    }

    public static ConfigKey<String> ofString(String section, String key, String defaultValue, String comment,
                                              Predicate<String> validator, String validationMessage) {
        return new ConfigKey<>(section, key, defaultValue, comment, s -> s, s -> s, validator, validationMessage);
    }

    public static ConfigKey<Integer> ofInt(String section, String key, int defaultValue, String comment) {
        return new ConfigKey<>(section, key, defaultValue, comment, Integer::parseInt, String::valueOf, i -> true, null);
    }

    public static ConfigKey<Integer> ofInt(String section, String key, int defaultValue, String comment,
                                            Predicate<Integer> validator, String validationMessage) {
        return new ConfigKey<>(section, key, defaultValue, comment, Integer::parseInt, String::valueOf, validator, validationMessage);
    }

    public static ConfigKey<Boolean> ofBoolean(String section, String key, boolean defaultValue, String comment) {
        return new ConfigKey<>(section, key, defaultValue, comment, Boolean::parseBoolean, String::valueOf, b -> true, null);
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
