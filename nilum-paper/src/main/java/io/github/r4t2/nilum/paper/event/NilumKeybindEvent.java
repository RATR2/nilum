package io.github.r4t2.nilum.paper.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when a player presses or releases one of Nilum's 4 general-purpose client keybinds.
 * There's no built-in behavior for any slot; a plugin decides what each one does by listening
 * to this event, like any other Bukkit event.
 */
public final class NilumKeybindEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final int slot;
    private final boolean pressed;

    public NilumKeybindEvent(Player player, int slot, boolean pressed) {
        this.player = player;
        this.slot = slot;
        this.pressed = pressed;
    }

    public Player getPlayer() {
        return player;
    }

    /** 0-based; slot 0 is "Nilum Keybind 1" in the client's Controls menu. */
    public int getSlot() {
        return slot;
    }

    /** True on key-down, false on key-up. */
    public boolean isPressed() {
        return pressed;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
