package com.lmduel.lmauction;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ItemTranslator {

    private final Lmauction plugin;
    private final Map<String, String> translations = new HashMap<>();

    public ItemTranslator(Lmauction plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        translations.clear();
        File file = new File(plugin.getDataFolder(), "items_ru.yml");
        if (!file.exists()) {
            plugin.saveResource("items_ru.yml", false);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            String val = config.getString(key);
            if (val != null) {
                translations.put(key.toUpperCase(Locale.ROOT), val);
            }
        }
    }

    // МЕТОД ДЛЯ ТАБ-КОМПЛИТЕРА
    public List<String> getTranslations() {
        return new ArrayList<>(translations.values());
    }

    public boolean matchesItem(ItemStack item, String query) {
        if (item == null || item.getType().isAir()) return false;
        String q = query.toLowerCase(Locale.ROOT).trim();

        String matKey = item.getType().name().toUpperCase(Locale.ROOT);
        String ruName = translations.get(matKey);
        if (ruName != null && ruName.toLowerCase(Locale.ROOT).contains(q)) {
            return true;
        }

        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                String stripName = meta.getDisplayName().replaceAll("(?i)§[0-9A-FK-OR#]", "").toLowerCase(Locale.ROOT);
                if (stripName.contains(q)) {
                    return true;
                }
            }
        }
        return false;
    }

    public String getRussianName(ItemStack item) {
        if (item == null || item.getType().isAir()) return "";
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        String matKey = item.getType().name().toUpperCase(Locale.ROOT);
        String ru = translations.get(matKey);
        return (ru != null) ? ru : item.getType().name();
    }
}