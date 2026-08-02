package io.github.r4t2.nilum.common.config;

public final class TcpConfig {

    public static final ConfigKey<String> BIND_ADDRESS = ConfigKey.ofString(
            "tcp", "bind-address", "0.0.0.0",
            "Address the TCP side-channel binds to. 0.0.0.0 = all network interfaces.", 1);

    public static final ConfigKey<Integer> PORT = ConfigKey.ofInt(
            "tcp", "port", 25566,
            "Port the TCP side-channel listens on.",
            1,
            port -> port >= 1 && port <= 65535,
            "must be between 1 and 65535");

    public static final ConfigKey<String> ADVERTISED_HOST = ConfigKey.ofString(
            "tcp", "advertised-host", "",
            "Publicly reachable host/IP clients should connect to for the TCP\n"
                    + "channel. Leave empty to disable the TCP side-channel entirely\n"
                    + "(falls back to plugin-channel-only asset transfer) until you set this.", 1);

    private TcpConfig() {
    }
}
