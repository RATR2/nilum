package io.github.r4t2.nilum.common.config;

public interface ConfigMigration {
    int fromVersion();
    RawConfigFile migrate(RawConfigFile old);
}
