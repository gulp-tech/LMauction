package com.lmduel.lmauction;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class BundleHolder implements InventoryHolder {

    private Inventory inventory;
    private final AuctionItem listing;
    private final int page;

    public BundleHolder(AuctionItem listing, int page) {
        this.listing = listing;
        this.page = Math.max(1, page);
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

    public int getPage() {
        return page;
    }
}
