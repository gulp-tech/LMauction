package com.lmduel.lmauction;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

public class LMpridmetHook {

    public record LMpridmetResult(ItemStack item) {}

    public static LMpridmetResult getItem(String id) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("LMpridmet");
        if (plugin == null || !plugin.isEnabled()) {
            return null;
        }

        try {
            Class<?> apiClass = Class.forName("com.lmduel.lmpridmet.api.LMpridmetAPI");
            Method method = apiClass.getMethod("getItem", String.class);
            Object result = method.invoke(null, id);
            if (result instanceof ItemStack stack) {
                return new LMpridmetResult(stack);
            }
        } catch (Exception ignored) {}

        return null;
    }
}