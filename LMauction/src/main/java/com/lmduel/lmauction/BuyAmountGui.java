package com.lmduel.lmauction;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class BuyAmountGui {

    private static final int CONTENT_SLOTS = 45;

    private final Player player;
    private final AuctionItem listing;

    public BuyAmountGui(Player player, AuctionItem listing) {
        this.player = player;
        this.listing = listing;
    }

    public void open(int selectedAmount) {
        int max = listing.getAmount();
        int selected = Math.max(1, Math.min(selectedAmount, max));

        BuyAmountHolder holder = new BuyAmountHolder(listing, selected);
        Inventory inv = Bukkit.createInventory(holder, 45, Lmauction.hex("&8Покупка предмета"));
        holder.setInventory(inv);

        // 13 - Предмет
        ItemStack preview = listing.getDisplayItem().clone();
        preview.setAmount(Math.min(selected, preview.getMaxStackSize()));
        inv.setItem(13, preview);

        // 12 - Прибавить
        if (selected < max) {
            ItemStack add = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
            ItemMeta meta = add.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(Lmauction.hex("&#C9F3C2П&#BFEDBAр&#B4E8B3и&#AAE2ABб&#9FDDA3а&#95D79Bв&#8AD194и&#80CC8Cт&#75C684ь"));
                List<String> lore = List.of(
                        Lmauction.hex("&#3390A2Нажмите - прибавить 1 шт."),
                        Lmauction.hex("&#3390A2Shift + клик - прибавить 10 шт.")
                );
                meta.setLore(lore);
                add.setItemMeta(meta);
            }
            inv.setItem(12, add);
        }

        // 14 - Убавить
        ItemStack sub = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta metaSub = sub.getItemMeta();
        if (metaSub != null) {
            metaSub.setDisplayName(Lmauction.hex("&cУбавить"));
            List<String> lore = List.of(
                    Lmauction.hex("&#3390A2Нажмите - убавить 1 шт."),
                    Lmauction.hex("&#3390A2Shift + клик - убавить 10 шт.")
            );
            metaSub.setLore(lore);
            sub.setItemMeta(metaSub);
        }
        inv.setItem(14, sub);

        // 20 - Приобрести
        double priceForSelected = (selected >= max) ? listing.getPrice() : listing.getUnitPrice() * selected;
        ItemStack buy = new ItemStack(Material.YELLOW_CONCRETE);
        ItemMeta buyMeta = buy.getItemMeta();
        if (buyMeta != null) {
            buyMeta.setDisplayName(Lmauction.hex("&#68B170П&#64B06Dр&#5FAE69и&#5BAD66о&#56AB63б&#52AA60р&#4EA85Cе&#49A759с&#45A556т&#40A452и за $" + Lmauction.formatPrice(priceForSelected) + "$"));
            List<String> lore = List.of(
                    Lmauction.hex("&fВы приобретаете: &#7ACF87" + selected + " шт."),
                    Lmauction.hex("&fИтоговая сумма: &#7ACF87" + Lmauction.formatPrice(priceForSelected) + " $"),
                    "",
                    Lmauction.hex("&#3390A2Нажмите, чтобы подтвердить покупку")
            );
            buyMeta.setLore(lore);
            buy.setItemMeta(buyMeta);
        }
        inv.setItem(20, buy);

        // 24 - Вернуться назад
        ItemStack back = new ItemStack(Material.RED_CONCRETE);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(Lmauction.hex("&cВернуться назад"));
            back.setItemMeta(backMeta);
        }
        inv.setItem(24, back);

        // 31 - Больше товаров игрока
        ItemStack spy = createSellerButton();
        inv.setItem(31, spy);

        player.openInventory(inv);
    }

    /**
     * Opens a read-only preview of a Minecraft bundle or a multi-item lot.
     * The lower row contains touch-friendly controls, so a Bedrock player
     * never needs a right-click or a shift-click to buy the bag.
     */
    public void openBundlePreview() {
        openBundlePreview(1);
    }

    public void openBundlePreview(int requestedPage) {
        List<ItemStack> content = listing.getBundleContents();
        int maxPages = Math.max(1, (int) Math.ceil((double) content.size() / CONTENT_SLOTS));
        int page = Math.max(1, Math.min(requestedPage, maxPages));

        BundleHolder holder = new BundleHolder(listing, page);
        String typeName = listing.isMinecraftBundle() ? "мешка" : "набора";
        String title = "&8Содержимое " + typeName;
        if (maxPages > 1) {
            title += " &7(" + page + "/" + maxPages + ")";
        }
        Inventory inv = Bukkit.createInventory(holder, 54, Lmauction.hex(title));
        holder.setInventory(inv);

        int startIndex = (page - 1) * CONTENT_SLOTS;
        int endIndex = Math.min(startIndex + CONTENT_SLOTS, content.size());
        for (int index = startIndex; index < endIndex; index++) {
            inv.setItem(index - startIndex, content.get(index).clone());
        }

        // 45 - previous page or return to the auction
        if (page > 1) {
            inv.setItem(45, createButton(Material.ARROW, "&#65AC6FПредыдущая страница",
                    List.of("&#3390A2Нажмите, чтобы посмотреть предыдущие предметы")));
        } else {
            inv.setItem(45, createButton(Material.RED_CONCRETE, "&cВернуться к аукциону",
                    List.of("&#3390A2Нажмите, чтобы вернуться к лотам")));
        }

        // 47 - price and seller information
        inv.setItem(47, createButton(Material.GOLD_INGOT, "&#F4C542Цена: &#7ACF87" + Lmauction.formatPrice(listing.getPrice()),
                List.of(
                        "&fПродавец: &#0DA070" + listing.getSellerName(),
                        "&fСрок лота: &#7ACF87" + listing.getTimeLeft() + " ч.",
                        "",
                        "&#4B9189Это информационная кнопка"
                )));

        // 49 - neutral contents marker
        inv.setItem(49, createButton(Material.BUNDLE, "&#7ACF87Содержимое " + typeName,
                List.of("&#4B9189Предметы выше можно только просматривать")));

        // 50 - never buys immediately; it opens a final price confirmation.
        inv.setItem(50, createButton(Material.LIME_CONCRETE,
                "&#68B170Продолжить: &#7ACF87$" + Lmauction.formatPrice(listing.getPrice()),
                List.of(
                        "&fВы получите весь " + typeName + ".",
                        "",
                        "&#F4C542Цена будет показана ещё раз перед списанием денег"
                )));

        // 52 - seller's lots
        inv.setItem(52, createSellerButton());

        // 53 - next page
        if (page < maxPages) {
            inv.setItem(53, createButton(Material.ARROW, "&#40B760Следующая страница",
                    List.of("&#3390A2Нажмите, чтобы посмотреть остальные предметы")));
        }

        player.openInventory(inv);
    }

    /**
     * A separate final step prevents a Bedrock player from purchasing a bundle
     * after only tapping its icon. The exact total is displayed in the title,
     * on a gold item, and on the confirmation button.
     */
    public void openBundleConfirmation() {
        ConfirmHolder holder = new ConfirmHolder(listing);
        String title = "&8Подтвердить: $" + Lmauction.formatPrice(listing.getPrice());
        Inventory inv = Bukkit.createInventory(holder, 27, Lmauction.hex(title));
        holder.setInventory(inv);

        inv.setItem(11, createButton(Material.RED_CONCRETE, "&cВернуться к содержимому",
                List.of("&#3390A2Нажмите, чтобы отменить покупку")));
        inv.setItem(13, createButton(Material.GOLD_BLOCK,
                "&#F4C542Итоговая цена: &#7ACF87$" + Lmauction.formatPrice(listing.getPrice()),
                List.of(
                        "&fПродавец: &#0DA070" + listing.getSellerName(),
                        "&fБудет куплен весь мешочек.",
                        "",
                        "&#B1687DПроверьте сумму перед подтверждением"
                )));
        inv.setItem(15, createButton(Material.LIME_CONCRETE,
                "&#68B170Подтвердить покупку за &#7ACF87$" + Lmauction.formatPrice(listing.getPrice()),
                List.of("&#3390A2Нажмите для списания средств")));

        player.openInventory(inv);
    }

    private ItemStack createSellerButton() {
        return createButton(Material.SPYGLASS,
                "&#5CB169Б&#5CB069о&#5BB068л&#5BAF68ь&#5AAE68ш&#5AAD67е &#59AC67т&#59AB66о&#58AA66ва&#58A965р&#57A865о&#57A764в &#56A664и&#56A563г&#55A463ро&#54A362к&#54A262а",
                List.of("&fПосмотреть все лоты продавца: &#0DA070" + listing.getSellerName()));
    }

    private ItemStack createButton(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Lmauction.component(name));
            meta.lore(lore.stream().map(Lmauction::component).toList());
            item.setItemMeta(meta);
        }
        return item;
    }
}
