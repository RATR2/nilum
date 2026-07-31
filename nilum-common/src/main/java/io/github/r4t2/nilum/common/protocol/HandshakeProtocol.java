package io.github.r4t2.nilum.common.protocol;

public final class HandshakeProtocol {

    public static final String PROTOCOL_VERSION = "1.0.0";
    public static final long TIMEOUT_MILLIS = 5000L;
    public static final String REQUIRES_MOD_MESSAGE =
            "This server requires Nilum. Get it at: https://github.com/RATR2/nilum";

    public static String tooOldMessage(String clientVersion, String requiredVersion) {
        return "Your Nilum version (" + clientVersion + ") is too old for this server, which requires "
                + requiredVersion + " or newer. Get it at: https://github.com/RATR2/nilum";
    }

    private HandshakeProtocol() {
    }
}
