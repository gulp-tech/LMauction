package com.lmduel.lmauction;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class BuyAmountHolder implements InventoryHolder {

    private Inventory inventory;
    private final AuctionItem listing;
    private int selectedAmount;

    public BuyAmountHolder(AuctionItem listing, int selectedAmount) {
        this.listing = listing;
        this.selectedAmount = selectedAmount;
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

    public int getSelectedAmount() {
        return selectedAmount;
    }

    public void setSelectedAmount(int selectedAmount) {
        this.selectedAmount = selectedAmount;
    }
}