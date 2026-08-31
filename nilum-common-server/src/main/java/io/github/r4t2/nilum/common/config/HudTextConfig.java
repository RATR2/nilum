package io.github.r4t2.nilum.common.config;

public final class HudTextConfig {

    public static final ConfigKey<Boolean> ENABLED = ConfigKey.ofBoolean(
            "hud-text", "enabled", true,
            "Whether server_connector HUD text elements get evaluated and pushed to\n"
                    + "clients at all: placeholderapi(...) calls (need PlaceholderAPI installed)\n"
                    + "and java(...) calls (any plugin registered with NilumValueRegistry).", 5);

    public static final ConfigKey<Integer> UPDATE_INTERVAL_TICKS = ConfigKey.ofInt(
            "hud-text", "update-interval-ticks", 20,
            "How often (in server ticks) server_connector HUD text elements are\n"
                    + "re-evaluated per online player. Runs on the main thread, so keep this\n"
                    + "reasonable if you're using expensive PlaceholderAPI expansions.", 5,
            value -> value >= 1, "must be at least 1");

    private HudTextConfig() {
    }
}
