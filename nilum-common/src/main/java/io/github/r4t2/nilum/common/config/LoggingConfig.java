package io.github.r4t2.nilum.common.config;


public final class LoggingConfig {

    public static final ConfigKey<Boolean> DEBUG = ConfigKey.ofBoolean(
            "logging", "debug", false,
            "Show debug-level Nilum messages in the console. They're always\n"
                    + "written to the dedicated Nilum log file regardless of this setting.");

    public static final ConfigKey<Integer> MAX_LOG_FILES = ConfigKey.ofInt(
            "logging", "max-log-files", 5,
            "How many rotated Nilum log files to keep. On the next one past\n"
                    + "this, the oldest gets deleted. Must be between 2 and 20.",
            n -> n >= 2 && n <= 20,
            "must be between 2 and 20");

    private LoggingConfig() {
    }
}
