package com.lmduel.lmauction;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class SellInvGui {

    private final Player player;

    public SellInvGui(Player player) {
        this.player = player;
    }

    public void open() {
        SellInvHolder holder = new SellInvHolder();
        Inventory inv = Bukkit.createInventory(holder, 27, Lmauction.hex("&8Массовая продажа"));
        holder.setInventory(inv);
        player.openInventory(inv);
    }
}