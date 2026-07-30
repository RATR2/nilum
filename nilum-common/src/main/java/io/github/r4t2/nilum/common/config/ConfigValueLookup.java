package io.github.r4t2.nilum.common.config;


@FunctionalInterface
public interface ConfigValueLookup {
    <T> T valueOf(ConfigKey<T> key);
}
