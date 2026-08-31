package io.github.r4t2.nilum.paper.ui;

import io.github.r4t2.nilum.common.protocol.NilumChannels;
import io.github.r4t2.nilum.common.protocol.OpenUiPacket;
import io.github.r4t2.nilum.paper.NilumPlugin;
import io.github.r4t2.nilum.paper.event.NilumUiCloseEvent;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Tracks which custom UI, if any, each player currently has open; a thin fire-and-forget sender like HudAtlasService. */
public final class UiSessionService {

    private final NilumPlugin plugin;
    private final Map<UUID, String> openUiByPlayer = new ConcurrentHashMap<>();

    public UiSessionService(NilumPlugin plugin) {
        this.plugin = plugin;
    }

    /** @return false if uiId isn't a loaded custom UI. */
    public boolean open(Player player, String uiId) {
        if (plugin.uis().assetBytes(uiId).isEmpty()) {
            return false;
        }
        if (!plugin.handshakes().hasClient(player.getUniqueId())) {
            return false;
        }
        openUiByPlayer.put(player.getUniqueId(), uiId);
        player.sendPluginMessage(plugin, NilumChannels.OPEN_UI_QUALIFIED, new OpenUiPacket(uiId).encode());
        return true;
    }

    public Optional<String> openUiFor(Player player) {
        return Optional.ofNullable(openUiByPlayer.get(player.getUniqueId()));
    }

    public void onClosed(Player player, String uiId) {
        openUiByPlayer.remove(player.getUniqueId());
        plugin.getServer().getPluginManager().callEvent(new NilumUiCloseEvent(player, uiId));
    }
}
