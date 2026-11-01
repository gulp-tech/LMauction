package com.lmduel.lmauction;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class BuyAmountGui {

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
                        Lmauction.hex("&#3390A2ЛКМ - прибавить 1 шт."),
                        Lmauction.hex("&#3390A2Shift + ЛКМ - прибавить 10 шт.")
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
                    Lmauction.hex("&#3390A2ЛКМ - убавить 1 шт."),
                    Lmauction.hex("&#3390A2Shift + ЛКМ - убавить 10 шт.")
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
                    Lmauction.hex("&#3390A2Клик - подтвердить покупку")
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
        ItemStack spy = new ItemStack(Material.SPYGLASS);
        ItemMeta spyMeta = spy.getItemMeta();
        if (spyMeta != null) {
            spyMeta.setDisplayName(Lmauction.hex("&#5CB169Б&#5CB069о&#5BB068л&#5BAF68ь&#5AAE68ш&#5AAD67е &#59AC67т&#59AB66о&#58AA66ва&#58A965р&#57A865о&#57A764в &#56A664и&#56A563г&#55A463ро&#54A362к&#54A262а"));
            List<String> lore = List.of(
                    Lmauction.hex("&fПосмотреть все лоты продавца: &#0DA070" + listing.getSellerName())
            );
            spyMeta.setLore(lore);
            spy.setItemMeta(spyMeta);
        }
        inv.setItem(31, spy);

        player.openInventory(inv);
    }

    public void openBundlePreview() {
        BundleHolder holder = new BundleHolder(listing);
        Inventory inv = Bukkit.createInventory(holder, 54, Lmauction.hex("&8Содержимое набора"));
        holder.setInventory(inv);

        List<ItemStack> content = listing.getContent();
        for (int i = 0; i < Math.min(content.size(), 45); i++) {
            inv.setItem(i, content.get(i).clone());
        }

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta meta = back.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Lmauction.hex("&#65AC6FН&#5EA86Aа&#58A564з&#51A15Fа&#4A9D59д"));
            back.setItemMeta(meta);
        }
        inv.setItem(49, back);

        player.openInventory(inv);
    }
}