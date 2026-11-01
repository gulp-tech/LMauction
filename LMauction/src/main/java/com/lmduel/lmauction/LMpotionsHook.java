package com.lmduel.lmauction;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.logging.Level;

public class LMpotionsHook {

    private static boolean enabled = false;
    private static Plugin plugin = null;

    public static void init() {
        plugin = Bukkit.getPluginManager().getPlugin("LMpotions");
        enabled = plugin != null && plugin.isEnabled();

        if (enabled) {
            Lmauction.getInstance().getLogger().info("✔ LMpotions интеграция активирована.");
        } else {
            Lmauction.getInstance().getLogger().info("LMpotions не найден. Интеграция отключена.");
        }
    }

    public static void reload() {
        plugin = null;
        enabled = false;
        init();
    }

    public static boolean isEnabled() {
        return enabled && plugin != null && plugin.isEnabled();
    }

    public static ItemStack createFakePotion(String id) {
        try {
            return switch (id.toLowerCase(Locale.ROOT)) {
                case "berserk" -> buildPotion(
                        Material.POTION,
                        "&#FF5555&l✦ Зелье Берсерка ✦",
                        List.of(
                                "&#AAAAAAСильная боевая смесь",
                                "&#FF5555Сила II &7(1:30)",
                                "&#55FF55Регенерация I &7(0:45)"
                        ),
                        List.of(
                                new PotionEffect(PotionEffectType.STRENGTH, 20 * 90, 1, false, true, true),
                                new PotionEffect(PotionEffectType.REGENERATION, 20 * 45, 0, false, true, true)
                        ),
                        Color.fromRGB(190, 40, 40)
                );

                case "raider" -> buildPotion(
                        Material.SPLASH_POTION,
                        "&#FFAA00&l✦ Рейдерская смесь ✦",
                        List.of(
                                "&#AAAAAAПолезно для PvP и рейдов",
                                "&#55FFFFСкорость II &7(1:20)",
                                "&#FFAA00Огнестойкость I &7(2:00)"
                        ),
                        List.of(
                                new PotionEffect(PotionEffectType.SPEED, 20 * 80, 1, false, true, true),
                                new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 120, 0, false, true, true)
                        ),
                        Color.fromRGB(255, 140, 20)
                );

                case "guardian" -> buildPotion(
                        Material.POTION,
                        "&#55FFFF&l✦ Зелье Стража ✦",
                        List.of(
                                "&#AAAAAAЗащитная смесь",
                                "&#55FFFFСопротивление I &7(1:00)",
                                "&#55FF55Регенерация II &7(0:25)"
                        ),
                        List.of(
                                new PotionEffect(PotionEffectType.RESISTANCE, 20 * 60, 0, false, true, true),
                                new PotionEffect(PotionEffectType.REGENERATION, 20 * 25, 1, false, true, true)
                        ),
                        Color.fromRGB(60, 200, 220)
                );

                case "escape" -> buildPotion(
                        Material.SPLASH_POTION,
                        "&#AA55FF&l✦ Зелье Побега ✦",
                        List.of(
                                "&#AAAAAAБыстро уйти из боя",
                                "&#55FFFFСкорость III &7(0:35)",
                                "&#FFFFFFНевидимость I &7(0:20)"
                        ),
                        List.of(
                                new PotionEffect(PotionEffectType.SPEED, 20 * 35, 2, false, true, true),
                                new PotionEffect(PotionEffectType.INVISIBILITY, 20 * 20, 0, false, true, true)
                        ),
                        Color.fromRGB(140, 60, 200)
                );

                case "hunter" -> buildPotion(
                        Material.POTION,
                        "&#55FF55&l✦ Зелье Охотника ✦",
                        List.of(
                                "&#AAAAAAДля погони и добивания",
                                "&#55FFFFСкорость II &7(1:00)",
                                "&#FF5555Сила I &7(1:00)"
                        ),
                        List.of(
                                new PotionEffect(PotionEffectType.SPEED, 20 * 60, 1, false, true, true),
                                new PotionEffect(PotionEffectType.STRENGTH, 20 * 60, 0, false, true, true)
                        ),
                        Color.fromRGB(60, 200, 60)
                );

                case "toxic" -> buildPotion(
                        Material.LINGERING_POTION,
                        "&#55AA55&l✦ Токсичная смесь ✦",
                        List.of(
                                "&#AAAAAAОпасная зона контроля",
                                "&#55AA55Яд II &7(0:18)",
                                "&#AAAAAAСлабость I &7(0:30)"
                        ),
                        List.of(
                                new PotionEffect(PotionEffectType.POISON, 20 * 18, 1, false, true, true),
                                new PotionEffect(PotionEffectType.WEAKNESS, 20 * 30, 0, false, true, true)
                        ),
                        Color.fromRGB(50, 120, 40)
                );

                case "void" -> buildPotion(
                        Material.SPLASH_POTION,
                        "&#5555FF&l✦ Зелье Бездны ✦",
                        List.of(
                                "&#AAAAAAРедкая мобильная смесь",
                                "&#FFFFFFНевидимость I &7(0:45)",
                                "&#55FFFFНочное зрение I &7(2:00)",
                                "&#55AAFFПлавное падение I &7(1:30)"
                        ),
                        List.of(
                                new PotionEffect(PotionEffectType.INVISIBILITY, 20 * 45, 0, false, true, true),
                                new PotionEffect(PotionEffectType.NIGHT_VISION, 20 * 120, 0, false, true, true),
                                new PotionEffect(PotionEffectType.SLOW_FALLING, 20 * 90, 0, false, true, true)
                        ),
                        Color.fromRGB(70, 70, 200)
                );

                case "royal" -> buildPotion(
                        Material.POTION,
                        "&#FFD700&l✦ Королевский эликсир ✦",
                        List.of(
                                "&#AAAAAAОчень редкая элитная смесь",
                                "&#FF5555Сила III &7(0:35)",
                                "&#55FF55Регенерация II &7(0:20)",
                                "&#FFAA00Огнестойкость I &7(2:00)"
                        ),
                        List.of(
                                new PotionEffect(PotionEffectType.STRENGTH, 20 * 35, 2, false, true, true),
                                new PotionEffect(PotionEffectType.REGENERATION, 20 * 20, 1, false, true, true),
                                new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 120, 0, false, true, true)
                        ),
                        Color.fromRGB(240, 190, 30)
                );

                case "healing" -> buildPotion(
                        Material.POTION,
                        "&#FF77AA&l✦ Зелье Исцеления Боя ✦",
                        List.of(
                                "&#AAAAAAПолезно в PvP",
                                "&#FF77AAМгновенное лечение II",
                                "&#55FF55Регенерация I &7(0:15)"
                        ),
                        List.of(
                                new PotionEffect(PotionEffectType.INSTANT_HEALTH, 1, 1, false, true, true),
                                new PotionEffect(PotionEffectType.REGENERATION, 20 * 15, 0, false, true, true)
                        ),
                        Color.fromRGB(255, 110, 160)
                );

                case "fury" -> buildPotion(
                        Material.POTION,
                        "&#FF3300&l✦ Зелье Ярости ✦",
                        List.of(
                                "&#AAAAAAМаксимум урона на короткое время",
                                "&#FF3300Сила III &7(0:25)",
                                "&#55FFFFСкорость I &7(0:25)"
                        ),
                        List.of(
                                new PotionEffect(PotionEffectType.STRENGTH, 20 * 25, 2, false, true, true),
                                new PotionEffect(PotionEffectType.SPEED, 20 * 25, 0, false, true, true)
                        ),
                        Color.fromRGB(220, 40, 20)
                );

                default -> null;
            };
        } catch (Exception e) {
            if (Lmauction.getInstance() != null) {
                Lmauction.getInstance().getLogger().log(Level.WARNING,
                        "Ошибка создания фейкового LMpotions зелья '" + id + "': " + e.getMessage());
            }
            return null;
        }
    }

    private static ItemStack buildPotion(Material material, String name, List<String> lore, List<PotionEffect> effects, Color color) {
        ItemStack item = new ItemStack(material, 1);
        if (!(item.getItemMeta() instanceof PotionMeta meta)) {
            return item;
        }

        meta.setDisplayName(Lmauction.hex(name));

        List<String> coloredLore = new ArrayList<>();
        for (String line : lore) {
            coloredLore.add(Lmauction.hex(line));
        }
        meta.setLore(coloredLore);

        for (PotionEffect effect : effects) {
            meta.addCustomEffect(effect, true);
        }

        if (color != null) {
            meta.setColor(color);
        }

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        try {
            meta.addItemFlags(ItemFlag.valueOf("HIDE_ADDITIONAL_TOOLTIP"));
        } catch (IllegalArgumentException ignored) {
        }

        item.setItemMeta(meta);
        return item;
    }

    public static Set<String> getAvailableFakeIds() {
        return new HashSet<>(Arrays.asList(
                "berserk",
                "raider",
                "guardian",
                "escape",
                "hunter",
                "toxic",
                "void",
                "royal",
                "healing",
                "fury"
        ));
    }
}