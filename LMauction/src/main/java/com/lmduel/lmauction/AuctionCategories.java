package com.lmduel.lmauction;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class AuctionCategories {

    public static boolean matches(ItemStack item, AuctionCategory category) {
        if (item == null || item.getType() == Material.AIR) return false;
        if (category == AuctionCategory.ALL) return true;

        Material mat = item.getType();
        String name = mat.name();

        switch (category) {
            case HELMETS -> { return name.endsWith("_HELMET"); }
            case CHESTPLATES -> { return name.endsWith("_CHESTPLATE"); }
            case LEGGINGS -> { return name.endsWith("_LEGGINGS"); }
            case BOOTS -> { return name.endsWith("_BOOTS"); }
            case SWORDS -> { return name.endsWith("_SWORD"); }
            case PICKAXES -> { return name.endsWith("_PICKAXE"); }
            case SHOVELS -> { return name.endsWith("_SHOVEL"); }
            case AXES -> { return name.endsWith("_AXE"); }
            case HOES -> { return name.endsWith("_HOE"); }
            case BOWS -> { return mat == Material.BOW; }
            case CROSSBOWS -> { return mat == Material.CROSSBOW; }
            case TRIDENTS -> { return mat == Material.TRIDENT; }
            case MACES -> { return name.endsWith("_MACE"); }
            case SPEARS -> { return mat == Material.TRIDENT; }
            case WEAPONS -> {
                return name.endsWith("_SWORD") || (name.endsWith("_AXE") && !name.endsWith("_PICKAXE")) || mat == Material.BOW || mat == Material.CROSSBOW || mat == Material.TRIDENT || name.endsWith("_MACE");
            }
            case TOOLS -> {
                return name.endsWith("_PICKAXE") || name.endsWith("_SHOVEL") || (name.endsWith("_AXE") && !name.endsWith("_SWORD")) || name.endsWith("_HOE") || name.endsWith("_SHEARS") || mat == Material.FLINT_AND_STEEL || mat == Material.FISHING_ROD || mat == Material.COMPASS || mat == Material.CLOCK || mat == Material.SPYGLASS || mat == Material.LEAD;
            }
            case BLOCKS -> { return mat.isBlock(); }
            case FOOD -> { return mat.isEdible(); }
            case POTIONS -> { return mat == Material.POTION || mat == Material.SPLASH_POTION || mat == Material.LINGERING_POTION; }
            case ENCHANTED_BOOKS -> { return mat == Material.ENCHANTED_BOOK; }
            case CUSTOM -> { return isCustomItem(item); }
            case OTHER -> {
                return !isAnyStandardCategory(item);
            }
            default -> { return false; }
        }
    }

    private static boolean isCustomItem(ItemStack item) {
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;
        String dName = meta.getDisplayName();
        return dName.contains("Эфирная") || dName.contains("Дезертира") ||
                dName.contains("Компостница") || dName.contains("Смерч") ||
                dName.contains("Лавоход") || dName.contains("Динамит") ||
                dName.contains("Спавнер") || dName.contains("Яйцо") ||
                dName.contains("Куб") || dName.contains("Кислотный") ||
                dName.contains("Слеза") || dName.contains("Титановый") ||
                dName.contains("Ледяной") || dName.contains("Молниеносный") ||
                dName.contains("Цветок") || dName.contains("Штормовой") ||
                dName.contains("Теневой") || dName.contains("Амулет") ||
                dName.contains("Авто-меч");
    }

    private static boolean isAnyStandardCategory(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        Material mat = item.getType();
        String name = mat.name();

        if (name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS")) return true;
        if (name.endsWith("_SWORD") || name.endsWith("_PICKAXE") || name.endsWith("_SHOVEL") || name.endsWith("_AXE") || name.endsWith("_HOE") || name.endsWith("_MACE")) return true;
        if (mat == Material.BOW || mat == Material.CROSSBOW || mat == Material.TRIDENT || mat == Material.ENCHANTED_BOOK) return true;
        if (mat == Material.POTION || mat == Material.SPLASH_POTION || mat == Material.LINGERING_POTION) return true;
        if (mat.isBlock() || mat.isEdible()) return true;
        if (isCustomItem(item)) return true;

        return false;
    }
}