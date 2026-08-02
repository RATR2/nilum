package io.github.r4t2.nilum.common.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


public final class NilumConfigManager {

    private static final Logger LOGGER = Logger.getLogger("nilum");

    private final Path file;
    private final int currentVersion;
    private final String header;
    private final ConfigSchema schema;
    private final List<ConfigMigration> migrations;

    private volatile Map<ConfigKey<?>, Object> values;

    public NilumConfigManager(Path file, String header, ConfigSchema schema, List<ConfigMigration> migrations) {
        this.file = file;
        this.currentVersion = schema.currentVersion();
        this.header = header;
        this.schema = schema;
        this.migrations = migrations;
    }

    public synchronized void load() throws IOException {
        RawConfigFile raw = Files.exists(file)
                ? ConfigFileParser.parse(Files.readAllLines(file, StandardCharsets.UTF_8))
                : RawConfigFile.empty(currentVersion);

        for (ConfigMigration migration : migrations) {
            if (migration.fromVersion() == raw.version()) {
                raw = migration.migrate(raw);
            }
        }

        Map<ConfigKey<?>, Object> resolved = new HashMap<>();
        for (ConfigKey<?> key : schema.keys()) {
            resolved.put(key, resolveValue(key, raw));
        }

        this.values = resolved;
        save();
    }

    public void reload() throws IOException {
        load();
    }

    public <T> T get(ConfigKey<T> key) {
        @SuppressWarnings("unchecked")
        T value = (T) values.get(key);
        return value != null ? value : key.defaultValue();
    }

    private <T> T resolveValue(ConfigKey<T> key, RawConfigFile raw) {
        String rawValue = raw.get(key.section(), key.key());
        if (rawValue == null) {
            return key.defaultValue();
        }

        T parsed = key.parseOrNull(rawValue);
        if (parsed == null) {
            String reason = key.validationMessage() != null ? key.validationMessage() : "invalid value";
            LOGGER.log(Level.WARNING, "Nilum config: " + key.section() + "." + key.key()
                    + " = \"" + rawValue + "\" is invalid (" + reason + "), using default "
                    + key.defaultValue());
            return key.defaultValue();
        }

        return parsed;
    }

    private void save() throws IOException {
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        String content = ConfigFileWriter.write(currentVersion, header, schema, new ConfigValueLookup() {
            @Override
            public <T> T valueOf(ConfigKey<T> key) {
                return get(key);
            }
        });

        Files.writeString(file, content, StandardCharsets.UTF_8);
    }
}
