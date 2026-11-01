package com.lmduel.lmauction;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class ConfirmHolder implements InventoryHolder {

    private final UUID listingId;

    public ConfirmHolder(UUID listingId) {
        this.listingId = listingId;
    }

    public UUID getListingId() {
        return listingId;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return null;
    }
}