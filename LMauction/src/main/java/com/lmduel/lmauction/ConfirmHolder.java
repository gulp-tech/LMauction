package com.lmduel.lmauction;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

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
    public Inventory getInventory() {
        return null;
    }
}