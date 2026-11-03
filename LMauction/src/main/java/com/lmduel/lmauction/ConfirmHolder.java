package com.lmduel.lmauction;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Holder for the final, explicit purchase confirmation of a bundle listing.
 */
public class ConfirmHolder implements InventoryHolder {

    private Inventory inventory;
    private final AuctionItem listing;

    public ConfirmHolder(AuctionItem listing) {
        this.listing = listing;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public AuctionItem getListing() {
        return listing;
    }
}
