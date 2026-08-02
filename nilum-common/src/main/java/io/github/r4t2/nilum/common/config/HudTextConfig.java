package io.github.r4t2.nilum.common.config;

public final class HudTextConfig {

    public static final ConfigKey<Boolean> ENABLED = ConfigKey.ofBoolean(
            "hud-text", "enabled", true,
            "Whether server_connector HUD text elements (PlaceholderAPI-backed) get\n"
                    + "evaluated and pushed to clients at all. Has no effect if PlaceholderAPI\n"
                    + "isn't installed - those elements just stay blank either way.", 5);

    public static final ConfigKey<Integer> UPDATE_INTERVAL_TICKS = ConfigKey.ofInt(
            "hud-text", "update-interval-ticks", 20,
            "How often (in server ticks) server_connector HUD text elements are\n"
                    + "re-evaluated per online player. Runs on the main thread, so keep this\n"
                    + "reasonable if you're using expensive PlaceholderAPI expansions.", 5,
            value -> value >= 1, "must be at least 1");

    private HudTextConfig() {
    }
}
