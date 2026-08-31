package io.github.r4t2.nilum.common.config;

public final class HandshakeConfig {

    public static final ConfigKey<Boolean> ALLOW_VANILLA_CLIENTS = ConfigKey.ofBoolean(
            "handshake", "allow-vanilla-clients", false,
            "Let players without Nilum installed join anyway instead of\n"
                    + "kicking them. They won't see any of Nilum's custom content.", 3);

    private HandshakeConfig() {
    }
}
