package com.lmduel.lmauction;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class GuiListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder == null) return;

        // Если это любое наше меню аукциона - отменяем любой клик по умолчанию
        if (holder instanceof AHHolder || holder instanceof BuyAmountHolder ||
                holder instanceof BundleHolder || holder instanceof SellInvHolder) {

            event.setCancelled(true);

            if (!(event.getWhoClicked() instanceof Player player)) return;

            // Обрабатываем клики ТОЛЬКО в верхнем инвентаре (меню)
            if (event.getClickedInventory() == event.getView().getTopInventory()) {
                int slot = event.getSlot();

                if (holder instanceof AHHolder ah) {
                    handleAuction(player, ah, slot, event.getClick());
                } else if (holder instanceof BuyAmountHolder bah) {
                    handleBuy(player, bah, slot, event.isShiftClick());
                } else if (holder instanceof BundleHolder bh) {
                    if (slot == 49) player.closeInventory();
                } else if (holder instanceof SellInvHolder sih) {
                    handleSellInv(player, sih, slot);
                }
            }
        }
    }

    private void handleAuction(Player player, AHHolder holder, int slot, org.bukkit.event.inventory.ClickType click) {
        // Логика главного меню (уже была у вас, просто убедитесь, что она здесь)
        if (slot >= 0 && slot < 45) {
            ItemStack item = player.getOpenInventory().getTopInventory().getItem(slot);
            if (item == null || item.getType() == Material.AIR) return;

            // Если заглушка "Лот недоступен"
            if (item.getType() == Material.BARRIER) {
                new AuctionGui(player).update(holder);
                return;
            }

            String idStr = Lmauction.getPDC(item, AuctionGui.listingKey());
            if (idStr != null) {
                AuctionItem ai = Lmauction.getInstance().findListing(java.util.UUID.fromString(idStr));
                if (ai == null) {
                    new AuctionGui(player).update(holder);
                    return;
                }

                if (ai.getSeller().equals(player.getUniqueId())) {
                    if (click.isLeftClick()) {
                        Lmauction.getInstance().getListings().remove(ai);
                        Lmauction.getInstance().addClaimItem(player.getUniqueId(), ai.getContent().get(0));
                        player.sendMessage(Lmauction.hex("&aЛот снят и отправлен в 'Забрать'."));
                        new AuctionGui(player).update(holder);
                    }
                } else {
                    if (ai.isBundle()) {
                        new BuyAmountGui(player, ai).openBundlePreview();
                    } else {
                        if (click.isRightClick()) {
                            new BuyAmountGui(player, ai).open(1);
                        } else {
                            new BuyAmountGui(player, ai).open(ai.getAmount());
                        }
                    }
                }
                return;
            }

            // Клик по категории
            String catId = Lmauction.getPDC(item, AuctionGui.categoryKey());
            if (catId != null) {
                holder.setCategory(AuctionCategory.valueOf(catId));
                holder.setView(AHHolder.View.LOTS);
                holder.setPage(1);
                new AuctionGui(player).update(holder);
            }
        }

        // Нижняя панель
        if (slot == 45) { // Назад
            if (holder.getView() == AHHolder.View.CATEGORIES) {
                holder.setView(AHHolder.View.LOTS);
                holder.setCategory(AuctionCategory.ALL);
            } else if (holder.getCategory() != AuctionCategory.ALL) {
                holder.setCategory(AuctionCategory.ALL);
                holder.setView(AHHolder.View.CATEGORIES);
            } else if (holder.getPage() > 1) {
                holder.setPage(holder.getPage() - 1);
            } else {
                player.closeInventory();
                return;
            }
            new AuctionGui(player).update(holder);
        }

        if (slot == 47) { // Сортировка
            holder.setSortType(click.isLeftClick() ? holder.getSortType().next() : holder.getSortType().prev());
            new AuctionGui(player).update(holder);
        }

        if (slot == 48) { // Вид
            holder.setView(holder.getView() == AHHolder.View.LOTS ? AHHolder.View.CATEGORIES : AHHolder.View.LOTS);
            holder.setPage(1);
            new AuctionGui(player).update(holder);
        }

        if (slot == 49) { // Забрать
            Lmauction.getInstance().claimAll(player);
            new AuctionGui(player).update(holder);
        }

        if (slot == 50) { // Мои лоты
            new AuctionGui(player).openCustom(player.getUniqueId(), player.getName());
        }

        if (slot == 52) { // Обновить
            new AuctionGui(player).update(holder);
        }

        if (slot == 53) { // Вперед
            holder.setPage(holder.getPage() + 1);
            new AuctionGui(player).update(holder);
        }
    }

    private void handleBuy(Player player, BuyAmountHolder holder, int slot, boolean shift) {
        if (slot < 0 || slot >= 45) return;

        AuctionItem listing = holder.getListing();
        if (slot == 24) { // Назад
            new AuctionGui(player).open();
            return;
        }

        if (slot == 31) { // Больше товаров игрока
            new AuctionGui(player).openCustom(listing.getSeller(), listing.getSellerName());
            return;
        }

        if (slot == 12) { // +
            int add = shift ? 10 : 1;
            int next = Math.min(listing.getAmount(), holder.getSelectedAmount() + add);
            if (next != holder.getSelectedAmount()) {
                new BuyAmountGui(player, listing).open(next);
            }
        }

        if (slot == 14) { // -
            int sub = shift ? 10 : 1;
            int next = Math.max(1, holder.getSelectedAmount() - sub);
            if (next != holder.getSelectedAmount()) {
                new BuyAmountGui(player, listing).open(next);
            }
        }

        if (slot == 20) { // КУПИТЬ
            int toBuy = holder.getSelectedAmount();
            double totalPrice = (toBuy >= listing.getAmount()) ? listing.getPrice() : listing.getUnitPrice() * toBuy;

            if (!Lmauction.getEconomy().has(player, totalPrice)) {
                player.sendMessage(Lmauction.hex("&cНедостаточно средств!"));
                return;
            }

            // Проверка: существует ли еще лот
            if (!Lmauction.getInstance().getListings().contains(listing)) {
                player.sendMessage(Lmauction.hex("&cЭтот лот уже недоступен."));
                player.closeInventory();
                return;
            }

            // Деньги списываем сразу
            Lmauction.getEconomy().withdrawPlayer(player, totalPrice);

            // Выдаем товар
            ItemStack prize = listing.getContent().get(0).clone();
            prize.setAmount(toBuy);
            AntiBackpackProtector.protect(player, prize);

            // Обновляем или удаляем лот
            if (toBuy >= listing.getAmount()) {
                Lmauction.getInstance().getListings().remove(listing);
            } else {
                listing.getContent().get(0).setAmount(listing.getAmount() - toBuy);
                listing.setPrice(listing.getPrice() - totalPrice);
            }

            // Перевод продавцу (если не фейк)
            if (!Lmauction.getInstance().isFakeSeller(listing.getSeller())) {
                var response = Lmauction.getEconomy().depositPlayer(org.bukkit.Bukkit.getOfflinePlayer(listing.getSeller()), totalPrice);
                if (!response.transactionSuccess()) {
                    Lmauction.getInstance().registerOfflineSale(listing.getSeller(), totalPrice);
                }
            }

            player.sendMessage(Lmauction.hex("&aВы успешно купили &f" + toBuy + " шт. &aза &6$" + Lmauction.formatPrice(totalPrice)));
            Lmauction.getInstance().saveAuctionData();
            player.closeInventory();
        }
    }

    private void handleSellInv(Player player, SellInvHolder holder, int slot) {
        // Если используете GUI для подтверждения массовой продажи
        if (slot == 22) { // Кнопка "Выставить все"
            // Эта логика теперь перенесена в команду /ah sellinv для безопасности
            player.closeInventory();
        }
    }
}