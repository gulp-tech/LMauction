package com.lmduel.lmauction;

import org.bukkit.Material;

public enum AuctionCategory {
    ALL("Все предметы", Material.NETHER_STAR),
    HELMETS("Шлемы", Material.DIAMOND_HELMET),
    CHESTPLATES("Нагрудники", Material.DIAMOND_CHESTPLATE),
    LEGGINGS("Поножи", Material.DIAMOND_LEGGINGS),
    BOOTS("Ботинки", Material.DIAMOND_BOOTS),
    SWORDS("Мечи", Material.DIAMOND_SWORD),
    WEAPONS("Оружие", Material.NETHERITE_AXE),
    TOOLS("Инструменты", Material.DIAMOND_PICKAXE),
    PICKAXES("Кирки", Material.IRON_PICKAXE),
    SHOVELS("Лопаты", Material.IRON_SHOVEL),
    AXES("Топоры", Material.IRON_AXE),
    HOES("Мотыги", Material.IRON_HOE),
    BLOCKS("Блоки", Material.GRASS_BLOCK),
    FOOD("Еда", Material.COOKED_BEEF),
    POTIONS("Зелья", Material.POTION),
    ENCHANTED_BOOKS("Зачарованные книги", Material.ENCHANTED_BOOK),
    MACES("Булавы", Material.AIR), // Булава доступна в 1.21+, для 1.20.4 можно поставить AIR или IRON_MACE
    SPEARS("Копья", Material.TRIDENT),
    BOWS("Луки", Material.BOW),
    CROSSBOWS("Арбалеты", Material.CROSSBOW),
    TRIDENTS("Трезубцы", Material.TRIDENT),
    CUSTOM("Кастомные", Material.DRAGON_BREATH),
    OTHER("Разное", Material.CHEST);

    private final String displayName;
    private final Material defaultIcon;

    AuctionCategory(String displayName, Material defaultIcon) {
        this.displayName = displayName;
        this.defaultIcon = (defaultIcon == null) ? Material.BARRIER : defaultIcon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getDefaultIcon() {
        // Защита: если Material.AIR (например для булавы в 1.20.4), заменим на похожий
        if (defaultIcon == Material.AIR) return Material.IRON_AXE;
        return defaultIcon;
    }
}