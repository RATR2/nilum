package io.github.r4t2.nilum.paper.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Fired when a player closes a Nilum custom UI they had open. */
public final class NilumUiCloseEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String uiId;

    public NilumUiCloseEvent(Player player, String uiId) {
        this.player = player;
        this.uiId = uiId;
    }

    public Player getPlayer() {
        return player;
    }

    public String getUiId() {
        return uiId;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
