package io.github.r4t2.nilum.common.config;

import java.util.LinkedHashMap;
import java.util.Map;


public final class RawConfigFile {

    private final int version;
    private final Map<String, Map<String, String>> sections;

    public RawConfigFile(int version, Map<String, Map<String, String>> sections) {
        this.version = version;
        this.sections = sections;
    }

    public static RawConfigFile empty(int version) {
        return new RawConfigFile(version, new LinkedHashMap<>());
    }

    public int version() {
        return version;
    }

    public String get(String section, String key) {
        Map<String, String> keys = sections.get(section);
        return keys == null ? null : keys.get(key);
    }
}
