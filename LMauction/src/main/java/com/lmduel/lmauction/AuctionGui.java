package com.lmduel.lmauction;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class AuctionGui {

    private final Player player;

    public static NamespacedKey listingKey() {
        return new NamespacedKey(Lmauction.getInstance(), "listing_id");
    }

    public static NamespacedKey categoryKey() {
        return new NamespacedKey(Lmauction.getInstance(), "category_id");
    }

    public AuctionGui(Player player) {
        this.player = player;
    }

    public void open() {
        AHHolder holder = new AHHolder(1, AuctionCategory.ALL, AHHolder.View.LOTS, SortType.NEWEST, null, null, null);
        renderAndOpen(holder);
    }

    public void openSearch(String query) {
        AHHolder holder = new AHHolder(1, AuctionCategory.ALL, AHHolder.View.LOTS, SortType.NEWEST, null, null, query);
        renderAndOpen(holder);
    }

    public void openCustom(UUID sellerId, String sellerName) {
        AHHolder holder = new AHHolder(1, AuctionCategory.ALL, AHHolder.View.LOTS, SortType.NEWEST, sellerId, sellerName, null);
        renderAndOpen(holder);
    }

    public void update(AHHolder holder) {
        renderAndOpen(holder);
    }

    private void renderAndOpen(AHHolder holder) {
        String title = Lmauction.hex("&8Витрина аукциона");
        if (holder.getFilterSellerName() != null) {
            title = Lmauction.hex("&8Товары: &0" + holder.getFilterSellerName());
        } else if (holder.getSearchQuery() != null) {
            title = Lmauction.hex("&8Поиск: &0" + holder.getSearchQuery());
        }

        Inventory inv = Bukkit.createInventory(holder, 54, title);
        holder.setInventory(inv);

        if (holder.getView() == AHHolder.View.CATEGORIES) {
            renderCategories(inv, holder);
        } else {
            renderListings(inv, holder);
        }

        renderBottomPanel(inv, holder);
        player.openInventory(inv);
    }

    private void renderListings(Inventory inv, AHHolder holder) {
        List<AuctionItem> items = new ArrayList<>(Lmauction.getInstance().getListings());

        if (holder.getFilterSeller() != null) {
            items.removeIf(l -> !l.getSeller().equals(holder.getFilterSeller()));
        }

        if (holder.getSearchQuery() != null) {
            items.removeIf(l -> !Lmauction.getInstance().isSmartMatch(l.getDisplayItem(), holder.getSearchQuery()));
        }

        if (holder.getCategory() != AuctionCategory.ALL) {
            items.removeIf(l -> !AuctionCategories.matches(l.getDisplayItem(), holder.getCategory()));
        }

        switch (holder.getSortType()) {
            case NEWEST -> items.sort(Comparator.comparingLong(AuctionItem::getTimestamp).reversed());
            case OLDEST -> items.sort(Comparator.comparingLong(AuctionItem::getTimestamp));
            case UNIT_CHEAP -> items.sort(Comparator.comparingDouble(AuctionItem::getUnitPrice));
            case UNIT_EXPENSIVE -> items.sort(Comparator.comparingDouble(AuctionItem::getUnitPrice).reversed());
            case CHEAP -> items.sort(Comparator.comparingDouble(AuctionItem::getPrice));
            case EXPENSIVE -> items.sort(Comparator.comparingDouble(AuctionItem::getPrice).reversed());
        }

        int pageSize = 45;
        int maxPages = Math.max(1, (int) Math.ceil((double) items.size() / pageSize));
        if (holder.getPage() > maxPages) holder.setPage(maxPages);
        if (holder.getPage() < 1) holder.setPage(1);

        int startIndex = (holder.getPage() - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, items.size());

        for (int i = startIndex; i < endIndex; i++) {
            AuctionItem listing = items.get(i);
            int slot = i - startIndex;
            inv.setItem(slot, createListingItem(listing, player.getUniqueId()));
        }
    }

    private void renderCategories(Inventory inv, AHHolder holder) {
        AuctionCategory[] categories = AuctionCategory.values();
        int slot = 0;
        for (AuctionCategory cat : categories) {
            if (cat == AuctionCategory.ALL) continue;
            if (slot >= 45) break;

            long count = Lmauction.getInstance().getListings().stream()
                    .filter(l -> AuctionCategories.matches(l.getDisplayItem(), cat))
                    .count();

            ItemStack display = getCategoryIcon(cat);
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(Lmauction.hex("&a" + cat.getDisplayName()));
                List<String> lore = new ArrayList<>();
                lore.add(Lmauction.hex("&fВ продаже: &#7ACF87" + count + " &#4B9189лотов"));
                lore.add("");
                lore.add(Lmauction.hex("&#3390A2ЛКМ - открыть категорию"));
                meta.setLore(lore);
                display.setItemMeta(meta);
            }

            Lmauction.setPDC(display, categoryKey(), cat.name());
            inv.setItem(slot++, display);
        }
    }

    private ItemStack getCategoryIcon(AuctionCategory cat) {
        for (AuctionItem listing : Lmauction.getInstance().getListings()) {
            if (AuctionCategories.matches(listing.getDisplayItem(), cat)) {
                ItemStack icon = listing.getDisplayItem().clone();
                icon.setAmount(1);
                return icon;
            }
        }
        return new ItemStack(cat.getDefaultIcon());
    }

    public static ItemStack createListingItem(AuctionItem listing, UUID viewerUUID) {
        /*
         * The auction icon uses a fresh, empty bundle. Its contents are not
         * copied into the GUI icon, so Geyser can display our custom lore and
         * price. The stored ItemStack and the bag given to a buyer remain the
         * original bundle with all of its metadata and contents.
         */
        ItemStack item = listing.isMinecraftBundle()
                ? createMinecraftBundleListingIcon(listing, viewerUUID)
                : listing.getDisplayItem().clone();
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            List<String> lore = new ArrayList<>();
            // Если у предмета уже был лор (например, описание от LMpridmet), сохраняем его
            if (listing.getDisplayItem().hasItemMeta() && listing.getDisplayItem().getItemMeta().hasLore()) {
                lore.addAll(listing.getDisplayItem().getItemMeta().getLore());
            }

            lore.add("");

            String priceStr = Lmauction.formatPrice(listing.getPrice());
            String unitPriceStr = Lmauction.formatPrice(listing.getUnitPrice());

            lore.add(Lmauction.hex("&fЦена: &#7ACF87" + priceStr + " &#4B9189(&#7ACF87" + unitPriceStr + " &#4B9189за шт.)"));
            lore.add(Lmauction.hex("&fПродавец: &#0DA070" + listing.getSellerName()));
            lore.add(Lmauction.hex("&fИстекает через " + listing.getTimeLeft() + " ч."));
            lore.add("");

            if (listing.getSeller().equals(viewerUUID)) {
                lore.add(Lmauction.hex("&#B1687DНажмите - снять лот"));
            } else if (listing.isPreviewBundle()) {
                lore.add(Lmauction.hex("&#3390A2Нажмите - посмотреть содержимое и купить"));
            } else {
                lore.add(Lmauction.hex("&#3390A2Нажмите - открыть меню покупки"));
                lore.add(Lmauction.hex("&#4B9189Количество выбирается в следующем меню"));
            }

            // Modern Purpurbedrock sends item descriptions as data-component
            // lore. This is required for the Geyser Bedrock item translator.
            meta.lore(lore.stream().map(Lmauction::component).toList());
            item.setItemMeta(meta);
        }

        Lmauction.setPDC(item, listingKey(), listing.getId().toString());
        return item;
    }

    private static ItemStack createMinecraftBundleListingIcon(AuctionItem listing, UUID viewerUUID) {
        ItemStack icon = new ItemStack(Material.BUNDLE);
        ItemMeta iconMeta = icon.getItemMeta();
        if (iconMeta == null) {
            return icon;
        }

        /*
         * Bedrock reliably displays a custom item name but certain Geyser
         * versions suppress BundleMeta lore. Put the same auction information
         * in the multi-line name as a compatibility fallback. Geyser supports
         * line breaks in a custom name, while Java players still receive the
         * ordinary lore created above.
         */
        List<String> titleLines = new ArrayList<>();
        ItemStack original = listing.getContent().get(0);
        ItemMeta originalMeta = original.getItemMeta();
        if (originalMeta != null && originalMeta.hasDisplayName()) {
            titleLines.add(originalMeta.getDisplayName());
        } else {
            titleLines.add(Lmauction.hex("&#D5A45CМешок с предметами"));
        }
        if (originalMeta != null && originalMeta.hasLore()) {
            titleLines.addAll(originalMeta.getLore());
        }

        titleLines.add("");
        titleLines.add(Lmauction.hex("&fЦена: &#7ACF87" + Lmauction.formatPrice(listing.getPrice())));
        titleLines.add(Lmauction.hex("&fПродавец: &#0DA070" + listing.getSellerName()));
        titleLines.add(Lmauction.hex("&fИстекает через " + listing.getTimeLeft() + " ч."));
        titleLines.add("");
        if (listing.getSeller().equals(viewerUUID)) {
            titleLines.add(Lmauction.hex("&#B1687DНажмите - снять лот"));
        } else {
            titleLines.add(Lmauction.hex("&#3390A2Нажмите - посмотреть содержимое и купить"));
        }

        iconMeta.displayName(Lmauction.component(String.join("\n", titleLines)));
        icon.setItemMeta(iconMeta);
        return icon;
    }

    private void renderBottomPanel(Inventory inv, AHHolder holder) {
        // Отрисовка нижней панели (без изменений, но должна быть в файле)
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) { backMeta.setDisplayName(Lmauction.hex("&#65AC6FН&#5EA86Aа&#58A564з&#51A15Fа&#4A9D59д")); back.setItemMeta(backMeta); }
        inv.setItem(45, back);

        ItemStack sort = new ItemStack(Material.HOPPER);
        ItemMeta sortMeta = sort.getItemMeta();
        if (sortMeta != null) {
            sortMeta.setDisplayName(Lmauction.hex("&#0DA047С&#0E9F49о&#0F9E4Cр&#109E4Eт&#119D50и&#129C53р&#139B55о&#149A57в&#159A59к&#16995Cа: &#25CB67" + holder.getSortType().getDisplay()));
            sort.setItemMeta(sortMeta);
        }
        inv.setItem(47, sort);

        ItemStack view = new ItemStack(Material.SPYGLASS);
        ItemMeta viewMeta = view.getItemMeta();
        if (viewMeta != null) { viewMeta.setDisplayName(Lmauction.hex("&#33A25FВи&#32A260д &#31A261о&#31A161т&#30A162об&#2FA162р&#2FA163а&#2EA163ж&#2EA164е&#2DA164н&#2DA064и&#2CA065я &#2BA066пр&#2AA067ед&#29A067м&#299F68е&#289F68т&#289F69о&#279F69в")); view.setItemMeta(viewMeta); }
        inv.setItem(48, view);

        ItemStack claim = new ItemStack(Material.CHEST);
        ItemMeta claimMeta = claim.getItemMeta();
        if (claimMeta != null) {
            int claimCount = Lmauction.getInstance().getClaimCount(player.getUniqueId());
            claimMeta.setDisplayName(Lmauction.hex("&#40B760З&#3DB461а&#39B162б&#36AE63р&#33AB63а&#2FA864т&#2CA565ь &#3390A2(&#82C88C" + claimCount + "&#3390A2)"));
            claim.setItemMeta(claimMeta);
        }
        inv.setItem(49, claim);

        ItemStack my = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta myMeta = my.getItemMeta();
        if (myMeta != null) { myMeta.setDisplayName(Lmauction.hex("&#40B760М&#3DB461о&#3AB261и &#35AD63л&#32AA64о&#2FA864т&#2CA565ы")); my.setItemMeta(myMeta); }
        inv.setItem(50, my);

        ItemStack refresh = new ItemStack(Material.CLOCK);
        ItemMeta refreshMeta = refresh.getItemMeta();
        if (refreshMeta != null) { refreshMeta.setDisplayName(Lmauction.hex("&#40B760О&#3DB461б&#3AB261н&#37AF62о&#35AD63в&#32AA64и&#2FA864т&#2CA565ь")); refresh.setItemMeta(refreshMeta); }
        inv.setItem(52, refresh);

        ItemStack next = new ItemStack(Material.ARROW);
        ItemMeta nextMeta = next.getItemMeta();
        if (nextMeta != null) { nextMeta.setDisplayName(Lmauction.hex("&#40B760В&#3CB361п&#38B062е&#34AC63р&#30A964ё&#2CA565д")); next.setItemMeta(nextMeta); }
        inv.setItem(53, next);
    }
}