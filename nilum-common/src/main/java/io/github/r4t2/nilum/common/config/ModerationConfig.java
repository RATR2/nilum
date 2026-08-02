package io.github.r4t2.nilum.common.config;

import java.util.List;

public final class ModerationConfig {

    public static final ConfigKey<Boolean> LOG_CLIENT_MODS = ConfigKey.ofBoolean(
            "moderation", "log-client-mods", false,
            "Logs the mods on the client, for moderation purposes.\n"
                    + "Default: False", 2);

    public static final ConfigKey<List<String>> DISABLED_MODS = ConfigKey.ofStringList(
            "moderation", "disabled-mods", List.of(),
            "This only works if log-client-mods is true.", 2);

    public static final ConfigKey<String> DISABLED_KICK_MESSAGE = ConfigKey.ofString(
            "moderation", "disabled-kick-message",
            "&cYou have invalid mods on your client %list of disabled mods found on the client%",
            "Disabled mods kick message (standard '&' color codes).", 2);

    private ModerationConfig() {
    }
}
