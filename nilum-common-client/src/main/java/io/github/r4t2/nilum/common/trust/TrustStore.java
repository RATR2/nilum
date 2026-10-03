package io.github.r4t2.nilum.common.trust;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Remembers which servers the player has already allowed to push Nilum content, the same way
 * vanilla remembers a server's resource pack trust. Keyed by the same per-server id
 * ServerCacheId already uses to scope the asset cache, so "this server" means the same thing in both places.
 */
public final class TrustStore {

    private final Path file;
    private final Set<String> trusted = ConcurrentHashMap.newKeySet();

    public TrustStore(Path file) {
        this.file = file;
        load();
    }

    public boolean isTrusted(String serverId) {
        return trusted.contains(serverId);
    }

    /** Remembers the player's Allow choice for this server; a failure to persist just means they're asked again next join. */
    public void trust(String serverId) {
        if (!trusted.add(serverId)) {
            return;
        }
        save();
    }

    private void load() {
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    trusted.add(trimmed);
                }
            }
        } catch (IOException ignored) {
            // Best-effort: an unreadable trust file just means every server gets re-prompted.
        }
    }

    private synchronized void save() {
        try {
            Files.createDirectories(file.getParent());
            String content = trusted.stream().sorted().collect(Collectors.joining("\n"));
            Files.write(file, content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) {
            // Best-effort: a failed save just means this server prompts again next join.
        }
    }
}
