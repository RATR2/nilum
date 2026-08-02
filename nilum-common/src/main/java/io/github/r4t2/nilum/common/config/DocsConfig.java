package io.github.r4t2.nilum.common.config;

public final class DocsConfig {

    public static final ConfigKey<Boolean> GENERATE_README = ConfigKey.ofBoolean(
            "docs", "generate-readme", true,
            "Writes a README.md into this plugin's data folder explaining the\n"
                    + "folder layout, for admins new to Nilum. Regenerated on every\n"
                    + "startup/config reload while true; set to false once you've\n"
                    + "customized or removed it and don't want it rewritten.", 4);

    private DocsConfig() {
    }
}
