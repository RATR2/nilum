package io.github.r4t2.nilum.common.util;

import java.util.Locale;

/**
 * Utility class for turning a server address into a filesystem-safe cache directory name.
 */
public final class ServerCacheId {

    private ServerCacheId() {
    }

    /**
     * @param address the server's address as the client knows it (e.g. "play.example.com:25565")
     * @return a lowercase name safe to use as a single path segment; "unknown" if address is blank
     */
    public static String sanitize(String address) {
        if (address == null || address.isBlank()) {
            return "unknown";
        }
        return address.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "_");
    }
}
