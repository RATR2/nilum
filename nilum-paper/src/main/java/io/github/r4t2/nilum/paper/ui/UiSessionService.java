package io.github.r4t2.nilum.paper.ui;

import io.github.r4t2.nilum.common.protocol.NilumChannels;
import io.github.r4t2.nilum.common.protocol.OpenUiPacket;
import io.github.r4t2.nilum.common.protocol.SetUiElementVisibilityPacket;
import io.github.r4t2.nilum.common.protocol.SetUiTextPacket;
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
        plugin.uiState().onOpen(player, uiId);
        return true;
    }

    public Optional<String> openUiFor(Player player) {
        return Optional.ofNullable(openUiByPlayer.get(player.getUniqueId()));
    }

    /** A snapshot of every player currently in a UI session, for UiStateService's own periodic tick. */
    public Map<UUID, String> openSessions() {
        return Map.copyOf(openUiByPlayer);
    }

    public void setElementVisible(Player player, String uiId, String elementId, boolean visible) {
        send(player, NilumChannels.SET_UI_ELEMENT_VISIBILITY_QUALIFIED,
                new SetUiElementVisibilityPacket(uiId, elementId, visible).encode());
    }

    public void setElementText(Player player, String uiId, String elementId, String text) {
        send(player, NilumChannels.SET_UI_TEXT_QUALIFIED, new SetUiTextPacket(uiId, elementId, text).encode());
    }

    private void send(Player player, String channel, byte[] data) {
        if (plugin.handshakes().hasClient(player.getUniqueId())) {
            player.sendPluginMessage(plugin, channel, data);
        }
    }

    public void onClosed(Player player, String uiId) {
        openUiByPlayer.remove(player.getUniqueId());
        plugin.getServer().getPluginManager().callEvent(new NilumUiCloseEvent(player, uiId));
    }
}
