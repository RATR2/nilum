package io.github.r4t2.nilum.paper.ui;

import io.github.r4t2.nilum.common.ui.ChestUiElement;
import io.github.r4t2.nilum.paper.NilumPlugin;
import io.github.r4t2.nilum.paper.skript.NilumSkriptEffectRunner;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

/** Cancels item movement in an open Nilum chest UI and runs a clicked slot's configured action. */
public final class ChestUiInteractionListener implements Listener {

    private final NilumPlugin plugin;

    public ChestUiInteractionListener(NilumPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof NilumChestUiHolder holder)) {
            return;
        }
        event.setCancelled(true);

        Inventory clicked = event.getClickedInventory();
        if (clicked == null || !(clicked.getHolder() instanceof NilumChestUiHolder)
                || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        holder.descriptor().elements().values().stream()
                .filter(element -> element.slot() == event.getSlot())
                .findFirst()
                .flatMap(ChestUiElement::action)
                .ifPresent(effectLine -> NilumSkriptEffectRunner.run(effectLine, player));
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof NilumChestUiHolder
                && event.getPlayer() instanceof Player player) {
            plugin.chestUiSessions().onClosed(player);
        }
    }
}
