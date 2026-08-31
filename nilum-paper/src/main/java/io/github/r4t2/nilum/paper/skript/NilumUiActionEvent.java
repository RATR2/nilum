package io.github.r4t2.nilum.paper.skript;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Synthetic event context for running a Custom UI action's Skript effect line, exposing the acting player as a real Skript event value. */
public final class NilumUiActionEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;

    public NilumUiActionEvent(Player player) {
        this.player = player;
    }

    public Player player() {
        return player;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
