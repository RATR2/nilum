package io.github.r4t2.nilum.common.config;

import io.github.r4t2.nilum.common.logging.LogDestination;
import io.github.r4t2.nilum.common.logging.NilumLogLevel;

import java.util.function.Function;

public final class LoggingConfig {

    public static final ConfigKey<LogDestination> DEBUG = ConfigKey.ofEnum(
            "logging", "debug", LogDestination.LOG,
            "These logs can be shown in the console, the log, or both, and can\n"
                    + "be disabled. Example:\n"
                    + "\n"
                    + "moderation: console\n"
                    + "debug: log\n"
                    + "warning: disabled\n"
                    + "error: both",
            LogDestination.class);

    public static final ConfigKey<LogDestination> WARNING = ConfigKey.ofEnum(
            "logging", "warning", LogDestination.BOTH, "", LogDestination.class);

    public static final ConfigKey<LogDestination> ERROR = ConfigKey.ofEnum(
            "logging", "error", LogDestination.BOTH, "", LogDestination.class);

    public static final ConfigKey<LogDestination> MODERATION = ConfigKey.ofEnum(
            "logging", "moderation", LogDestination.BOTH, "", LogDestination.class);

    public static final ConfigKey<Integer> MAX_LOG_FILES = ConfigKey.ofInt(
            "logging", "max-log-files", 5,
            "How many rotated Nilum log files to keep. On the next one past\n"
                    + "this, the oldest gets deleted. Must be between 2 and 20.",
            n -> n >= 2 && n <= 20,
            "must be between 2 and 20");

    /** Where each destination-routable log level should go, read live from {@code configManager}. */
    public static Function<NilumLogLevel, LogDestination> destinationLookup(NilumConfigManager configManager) {
        return level -> switch (level) {
            case DEBUG -> configManager.get(DEBUG);
            case WARN -> configManager.get(WARNING);
            case ERROR -> configManager.get(ERROR);
            case MODERATION -> configManager.get(MODERATION);
            case INFO -> LogDestination.BOTH;
        };
    }

    private LoggingConfig() {
    }
}
