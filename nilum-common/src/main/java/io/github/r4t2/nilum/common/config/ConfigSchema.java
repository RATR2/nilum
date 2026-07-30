package io.github.r4t2.nilum.common.config;

import java.util.List;


public final class ConfigSchema {

    private final List<ConfigKey<?>> keys;

    public ConfigSchema(List<ConfigKey<?>> keys) {
        this.keys = List.copyOf(keys);
    }

    public List<ConfigKey<?>> keys() {
        return keys;
    }
}
