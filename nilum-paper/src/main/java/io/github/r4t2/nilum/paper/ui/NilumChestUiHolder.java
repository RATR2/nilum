package io.github.r4t2.nilum.paper.ui;

import io.github.r4t2.nilum.common.ui.ChestUiDescriptor;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/** Identifies a real Bukkit inventory as one of Nilum's chest UIs, for InventoryClickEvent/InventoryCloseEvent routing. */
public final class NilumChestUiHolder implements InventoryHolder {

    private final String uiId;
    private final ChestUiDescriptor descriptor;
    private Inventory inventory;

    public NilumChestUiHolder(String uiId, ChestUiDescriptor descriptor) {
        this.uiId = uiId;
        this.descriptor = descriptor;
    }

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public String uiId() {
        return uiId;
    }

    public ChestUiDescriptor descriptor() {
        return descriptor;
    }

    @Override
    @NotNull
    public Inventory getInventory() {
        return inventory;
    }
}
