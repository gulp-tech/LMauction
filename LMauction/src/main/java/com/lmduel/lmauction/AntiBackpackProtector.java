package com.lmduel.lmauction;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;

public final class AntiBackpackProtector {

    private AntiBackpackProtector() {
    }

    public static void protect(Player player, ItemStack source) {
        if (player == null
                || source == null
                || source.getType() == Material.AIR
                || source.getAmount() <= 0) {
            return;
        }

        ItemStack item = clean(source);

        /*
         * addItem сам правильно распределяет стаки.
         * Всё, что не поместилось, возвращается в leftovers.
         */
        Map<Integer, ItemStack> leftovers =
                player.getInventory().addItem(item);

        for (ItemStack leftover : leftovers.values()) {
            if (leftover == null || leftover.getType() == Material.AIR) {
                continue;
            }

            player.getWorld().dropItemNaturally(
                    player.getLocation(),
                    leftover
            );
        }
    }

    private static ItemStack clean(ItemStack source) {
        ItemStack copy = source.clone();
        ItemMeta meta = copy.getItemMeta();

        if (meta == null) {
            return copy;
        }

        NamespacedKey oldProtectionKey =
                new NamespacedKey(
                        Lmauction.getInstance(),
                        "ah_protect_id"
                );

        meta.getPersistentDataContainer().remove(oldProtectionKey);

        copy.setItemMeta(meta);

        return copy;
    }
}