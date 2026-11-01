package com.lmduel.lmauction;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class SellInvHolder implements InventoryHolder {

    private Inventory inventory;

    public SellInvHolder() {}

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}