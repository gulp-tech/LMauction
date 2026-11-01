package com.lmduel.lmauction;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AuctionItem {

    private final UUID id;
    private final UUID seller;
    private final String sellerName;
    private final List<ItemStack> content;
    private double price;
    private final long timestamp;
    private final boolean bundle;

    public AuctionItem(UUID seller, String sellerName, List<ItemStack> content, double price, long timestamp, boolean bundle) {
        this(UUID.randomUUID(), seller, sellerName, content, price, timestamp, bundle);
    }

    public AuctionItem(UUID id, UUID seller, String sellerName, List<ItemStack> content, double price, long timestamp, boolean bundle) {
        this.id = id;
        this.seller = seller;
        this.sellerName = sellerName;
        this.content = (content != null) ? new ArrayList<>(content) : new ArrayList<>();
        this.price = price;
        this.timestamp = timestamp;
        this.bundle = bundle;
    }

    public UUID getId() { return id; }
    public UUID getSeller() { return seller; }
    public String getSellerName() { return sellerName; }
    public List<ItemStack> getContent() { return content; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public long getTimestamp() { return timestamp; }
    public boolean isBundle() { return bundle; }

    /**
     * A real Minecraft bundle is stored as a single ItemStack whose contents
     * live in its BundleMeta. It is different from this plugin's older
     * multi-item "bundle" listing (the {@link #bundle} flag).
     */
    public boolean isMinecraftBundle() {
        if (content.size() != 1) {
            return false;
        }

        ItemStack item = content.get(0);
        return item != null
                && item.getType() == Material.BUNDLE
                && item.getItemMeta() instanceof BundleMeta;
    }

    /**
     * True when a lot needs a contents-preview screen before it can be bought.
     */
    public boolean isPreviewBundle() {
        return bundle || isMinecraftBundle();
    }

    /**
     * Returns safe copies of the items displayed in the contents-preview GUI.
     * For a Minecraft bundle this reads its BundleMeta; for the legacy bundle
     * listing it returns the items that make up the lot.
     */
    public List<ItemStack> getBundleContents() {
        List<ItemStack> result = new ArrayList<>();

        if (isMinecraftBundle()) {
            BundleMeta meta = (BundleMeta) content.get(0).getItemMeta();
            if (meta != null) {
                for (ItemStack item : meta.getItems()) {
                    if (item != null && !item.getType().isAir()) {
                        result.add(item.clone());
                    }
                }
            }
            return result;
        }

        for (ItemStack item : content) {
            if (item != null && !item.getType().isAir()) {
                result.add(item.clone());
            }
        }
        return result;
    }

    public ItemStack getDisplayItem() {
        if (content.isEmpty()) {
            return new ItemStack(Material.BARRIER);
        }
        // If this is a plugin-created set of several different items,
        // represent it with a bundle icon in the auction list.
        if (bundle) {
            ItemStack b = new ItemStack(Material.BUNDLE);
            ItemMeta meta = b.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(Lmauction.hex("&aНабор предметов (" + content.size() + " шт.)"));
                b.setItemMeta(meta);
            }
            return b;
        }
        // Ordinary lot, including a real Minecraft bundle.
        return content.get(0).clone();
    }

    public int getAmount() {
        if (bundle) {
            int total = 0;
            for (ItemStack is : content) {
                if (is != null) total += is.getAmount();
            }
            return Math.max(1, total);
        }
        if (content.isEmpty()) return 1;
        return Math.max(1, content.get(0).getAmount());
    }

    public double getUnitPrice() {
        double amt = getAmount();
        return price / amt;
    }

    public long getTimeLeft() {
        long elapsed = System.currentTimeMillis() - timestamp;
        long remaining = Lmauction.EXPIRE_TIME - elapsed;
        if (remaining <= 0) return 0;
        return remaining / (1000L * 60L * 60L);
    }
}
