package com.lmduel.lmauction;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

public class Lmauction extends JavaPlugin implements CommandExecutor {

    private static Lmauction instance;
    private static Economy econ = null;

    public static final long EXPIRE_TIME = 3L * 24L * 60L * 60L * 1000L; // 3 суток

    private final List<AuctionItem> listings = new ArrayList<>();
    private final Map<UUID, List<ItemStack>> claims = new HashMap<>();
    private final Map<UUID, Double> offlineSales = new HashMap<>();
    private final Set<UUID> fakeSellers = new HashSet<>();

    private ItemTranslator itemTranslator;
    private File dataFile;
    private YamlConfiguration dataConfig;

    private final List<FakeItemTemplate> fakeTemplates = new ArrayList<>();
    private final List<LMpridmetTemplate> lmTemplates = new ArrayList<>();

    @Override
    public void onEnable() {
        instance = this;

        if (!setupEconomy()) {
            getLogger().severe("Vault или Economy плагин не найден! Отключение...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();
        saveResourceIfNotExists("items_ru.yml");

        this.itemTranslator = new ItemTranslator(this);

        loadAuctionData();
        loadTemplates();

        getServer().getPluginManager().registerEvents(new GuiListener(), this);
        if (getCommand("ah") != null) {
            getCommand("ah").setExecutor(this);
            getCommand("ah").setTabCompleter(new AuctionTabCompleter(itemTranslator));
        }

        // Автосохранение раз в 1 минуту. Auction data and Bukkit ItemStacks
        // must only be read on the server thread; asynchronous saves can write
        // an old claim back after it has already been collected.
        Bukkit.getScheduler().runTaskTimer(this, this::saveAuctionData, 1200L, 1200L);

        // Обработка просроченных лотов раз в 1 минуту
        Bukkit.getScheduler().runTaskTimer(this, this::processExpiredItems, 1200L, 1200L);

        getLogger().info("Lmauction успешно запущен!");
    }

    @Override
    public void onDisable() {
        saveAuctionData();
    }

    public static Lmauction getInstance() {
        return instance;
    }

    public static Economy getEconomy() {
        return econ;
    }

    public ItemTranslator getItemTranslator() {
        return itemTranslator;
    }

    public List<AuctionItem> getListings() {
        return listings;
    }

    public AuctionItem findListing(UUID id) {
        for (AuctionItem item : listings) {
            if (item.getId().equals(id)) return item;
        }
        return null;
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        econ = rsp.getProvider();
        return econ != null;
    }

    private void saveResourceIfNotExists(String resourcePath) {
        File file = new File(getDataFolder(), resourcePath);
        if (!file.exists()) {
            saveResource(resourcePath, false);
        }
    }

    /* -----------------------------------------------------------------
     * ОБРАБОТКА КОМАНД /ah
     * ----------------------------------------------------------------- */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Команда доступна только игрокам.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("menu")) {
            new AuctionGui(player).open();
            return true;
        }

        String sub = args[0].toLowerCase();

        // Поиск (/ah search или /ah searsh)
        if (sub.equals("search") || sub.equals("searsh")) {
            if (args.length < 2) {
                player.sendMessage(hex("&cВведите запрос для поиска. Пример: /ah search Алмаз"));
                return true;
            }
            String query = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            new AuctionGui(player).openSearch(query);
            return true;
        }

        // Продажа одного предмета (/ah sell <цена>)
        if (sub.equals("sell")) {
            if (args.length < 2) {
                player.sendMessage(hex("&cИспользование: /ah sell <цена>"));
                return true;
            }

            double price = parsePrice(args[1]);
            if (price <= 0) {
                player.sendMessage(hex("&cНекорректная цена (от $1 до $2 500 000)."));
                return true;
            }

            ItemStack mainHand = player.getInventory().getItemInMainHand();
            if (mainHand == null || mainHand.getType().isAir()) {
                player.sendMessage(hex("&cВозьмите предмет в основную руку!"));
                return true;
            }

            int maxSlots = getMaxSlots(player);
            long currentCount = listings.stream().filter(l -> l.getSeller().equals(player.getUniqueId())).count();
            if (currentCount >= maxSlots) {
                player.sendMessage(hex("&cВы достигли лимита лотов (&6" + maxSlots + "&c)."));
                return true;
            }

            // Remove the exact held stack from the server inventory before
            // creating a listing. Explicit AIR plus an inventory resync is
            // required for Geyser clients and prevents ghost/duplicate bags.
            ItemStack toSell = takeMainHandItem(player);
            if (toSell == null) {
                player.sendMessage(hex("&cНе удалось забрать предмет из руки. Попробуйте ещё раз."));
                return true;
            }

            AuctionItem newListing = new AuctionItem(player.getUniqueId(), player.getName(), List.of(toSell), price, System.currentTimeMillis(), false);
            listings.add(newListing);
            saveAuctionData();
            syncInventory(player);

            player.sendMessage(hex("&aПредмет &f" + getItemDisplayName(toSell) + " &aвыставлен за &6$" + formatPrice(price)));
            return true;
        }

        // Массовая продажа инвентаря (/ah sellinv <цена>)
        if (sub.equals("sellinv")) {
            if (args.length < 2) {
                player.sendMessage(hex("&cИспользование: /ah sellinv <цена на каждый лот>"));
                return true;
            }

            double price = parsePrice(args[1]);
            if (price <= 0) {
                player.sendMessage(hex("&cНекорректная цена (от $1 до $2 500 000)."));
                return true;
            }

            int maxSlots = getMaxSlots(player);
            long currentCount = listings.stream().filter(l -> l.getSeller().equals(player.getUniqueId())).count();

            // Безопасная продажа: СЛОТЫ 9-35 (Основной инвентарь)
            // Хотбар (0-8), Броня (36-39) и Оффхенд (40) ЗАЩИЩЕНЫ!
            int sold = 0;
            for (int slot = 9; slot <= 35; slot++) {
                if (currentCount >= maxSlots) break;

                ItemStack item = player.getInventory().getItem(slot);
                if (item == null || item.getType().isAir()) continue;

                ItemStack toSell = takeItemFromSlot(player, slot);
                if (toSell == null) continue;

                AuctionItem newListing = new AuctionItem(player.getUniqueId(), player.getName(), List.of(toSell), price, System.currentTimeMillis(), false);
                listings.add(newListing);
                currentCount++;
                sold++;
            }

            if (sold > 0) {
                saveAuctionData();
                syncInventory(player);
                player.sendMessage(hex("&aВыставлено предметов: &f" + sold + " &aпо &6$" + formatPrice(price) + " &aкаждый."));
            } else {
                player.sendMessage(hex("&cНет доступных предметов в основном инвентаре (хотбар, броня и руки защищены)."));
            }
            return true;
        }

        // Забрать купленные/снятые вещи (/ah claim)
        if (sub.equals("claim")) {
            int claimed = claimAll(player);
            if (claimed > 0) {
                player.sendMessage(hex("&aВы успешно забрали &f" + claimed + " &aпредм."));
            } else {
                player.sendMessage(hex("&cУ вас нет доступных предметов для получения."));
            }
            return true;
        }

        // Просмотр лотов конкретного игрока (/ah view <игрок>)
        if (sub.equals("view")) {
            if (args.length < 2) {
                player.sendMessage(hex("&cИспользование: /ah view <ник>"));
                return true;
            }
            String targetName = args[1];
            Player target = Bukkit.getPlayer(targetName);
            UUID targetUUID = (target != null) ? target.getUniqueId() : UUID.nameUUIDFromBytes(("FakePlayer:" + targetName).getBytes());
            new AuctionGui(player).openCustom(targetUUID, targetName);
            return true;
        }

        // АДМИН КОМАНДЫ
        if (sub.equals("reload")) {
            if (!player.hasPermission("lmauction.admin")) return true;
            reloadConfig();
            loadTemplates();
            itemTranslator.reload();
            player.sendMessage(hex("&aКонфигурация, переводы и интеграции аукциона перезагружены."));
            return true;
        }

        if (sub.equals("clearfake")) {
            if (!player.hasPermission("lmauction.admin")) return true;
            int initial = listings.size();
            listings.removeIf(l -> isFakeSeller(l.getSeller()));
            int removed = initial - listings.size();
            saveAuctionData();
            player.sendMessage(hex("&aУдалено фейковых лотов: &f" + removed));
            return true;
        }

        if (sub.equals("fillout")) {
            if (!player.hasPermission("lmauction.admin")) return true;
            int pages = 1;
            if (args.length >= 2) {
                try { pages = Integer.parseInt(args[1]); } catch (Exception ignored) {}
            }
            generateFakeListings(pages);
            saveAuctionData();
            player.sendMessage(hex("&aСгенерировано " + (pages * 45) + " фейковых лотов!"));
            return true;
        }

        sendHelp(player);
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(hex("&6&lLMAUCTION &7- Помощь по командам:"));
        player.sendMessage(hex("&e/ah &7- Открыть аукцион"));
        player.sendMessage(hex("&e/ah sell <цена> &7- Выставить предмет из руки"));
        player.sendMessage(hex("&e/ah sellinv <цена> &7- Выставить вещи из инвентаря"));
        player.sendMessage(hex("&e/ah search <запрос> &7- Поиск предметов"));
        player.sendMessage(hex("&e/ah claim &7- Забрать купленные / снятые вещи"));
        player.sendMessage(hex("&e/ah view <ник> &7- Посмотреть витрину игрока"));
        if (player.hasPermission("lmauction.admin")) {
            player.sendMessage(hex("&c/ah fillout <страниц> &7- Заполнить аукцион фейками"));
            player.sendMessage(hex("&c/ah clearfake &7- Очистить фейковые лоты"));
            player.sendMessage(hex("&c/ah reload &7- Перезагрузить конфиг"));
        }
    }

    /* -----------------------------------------------------------------
     * СИСТЕМА CLAIMИРОВАНИЯ И ХРАНЕНИЯ
     * ----------------------------------------------------------------- */
    public void addClaimItem(UUID uuid, ItemStack item) {
        if (item == null || item.getType().isAir()) return;
        claims.computeIfAbsent(uuid, k -> new ArrayList<>()).add(item.clone());
        saveAuctionData();
    }

    public int getClaimCount(UUID uuid) {
        List<ItemStack> list = claims.get(uuid);
        return (list != null) ? list.size() : 0;
    }

    public int claimAll(Player player) {
        UUID uuid = player.getUniqueId();
        boolean changed = false;

        // Process offline earnings first and make their removal persistent even
        // if the player has no item claims.
        if (offlineSales.containsKey(uuid)) {
            double amount = offlineSales.remove(uuid);
            econ.depositPlayer(player, amount);
            player.sendMessage(hex("&aВам зачислено &6$" + formatPrice(amount) + " &aза продажи в офлайне!"));
            changed = true;
        }

        /*
         * Detach the complete claim list before adding anything to the player
         * inventory. The empty state is saved first, so reconnects, Geyser
         * inventory refreshes, and server restarts cannot issue the same bag a
         * second time.
         */
        List<ItemStack> claimedItems = claims.remove(uuid);
        if (claimedItems != null && !claimedItems.isEmpty()) {
            changed = true;
        }

        if (changed) {
            saveAuctionData();
        }

        if (claimedItems == null || claimedItems.isEmpty()) {
            return 0;
        }

        for (ItemStack item : claimedItems) {
            AntiBackpackProtector.protect(player, item);
        }
        syncInventory(player);
        return claimedItems.size();
    }

    public void registerOfflineSale(UUID uuid, double amount) {
        offlineSales.put(uuid, offlineSales.getOrDefault(uuid, 0.0) + amount);
        saveAuctionData();
    }

    public boolean isFakeSeller(UUID uuid) {
        if (fakeSellers.contains(uuid)) return true;
        return uuid.toString().startsWith("00000000-0000-0000-0000-");
    }

    public int getMaxSlots(Player player) {
        if (player.hasPermission("lmauction.slots.100")) return 100;
        if (player.hasPermission("lmauction.slots.45")) return 45;
        if (player.hasPermission("lmauction.slots.30")) return 30;
        if (player.hasPermission("lmauction.slots.20")) return 20;
        return getConfig().getInt("settings.default-max-slots", 10);
    }

    /* -----------------------------------------------------------------
     * ОЧИСТКА ИСТЕКШИХ ЛОТОВ
     * ----------------------------------------------------------------- */
    private void processExpiredItems() {
        long now = System.currentTimeMillis();
        Iterator<AuctionItem> it = listings.iterator();

        boolean changed = false;
        while (it.hasNext()) {
            AuctionItem item = it.next();
            if (now - item.getTimestamp() >= EXPIRE_TIME) {
                it.remove();
                changed = true;

                if (!isFakeSeller(item.getSeller())) {
                    for (ItemStack is : item.getContent()) {
                        addClaimItem(item.getSeller(), is);
                    }
                }
            }
        }

        if (changed) saveAuctionData();
    }

    /* -----------------------------------------------------------------
     * ПОИСК И МАТЧИНГ ПРЕДМЕТОВ
     * ----------------------------------------------------------------- */
    public boolean isSmartMatch(ItemStack item, String query) {
        if (item == null || item.getType().isAir()) return false;
        String q = query.toLowerCase(Locale.ROOT).trim();

        if (itemTranslator.matchesItem(item, q)) return true;
        if (item.getType().name().toLowerCase(Locale.ROOT).contains(q)) return true;

        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                String stripName = meta.getDisplayName().replaceAll("(?i)§[0-9A-FK-OR#]", "").toLowerCase(Locale.ROOT);
                if (stripName.contains(q)) return true;
            }
        }
        return false;
    }

    /* -----------------------------------------------------------------
     * ФЕЙКОВЫЕ ЛОТЫ (/ah fillout)
     * ----------------------------------------------------------------- */
    private void generateFakeListings(int pages) {
        int total = pages * 45;
        Random rnd = new Random();

        List<String> fakeNames = List.of(
                "EnderBoy", "ShadowNinja", "VipPlayer", "DragonSlayer", "PvP_God",
                "CraftMaster", "Gamer365", "LmaO_Pro", "X_Dark_X", "MinerPro"
        );

        for (int i = 0; i < total; i++) {
            String fakeName = fakeNames.get(rnd.nextInt(fakeNames.size()));
            UUID fakeUUID = UUID.nameUUIDFromBytes(("FakePlayer:" + fakeName).getBytes());
            fakeSellers.add(fakeUUID);

            ItemStack item = null;
            double price = 100;

            if (rnd.nextInt(100) < 30 && !lmTemplates.isEmpty()) {
                LMpridmetTemplate tmpl = lmTemplates.get(rnd.nextInt(lmTemplates.size()));
                LMpridmetHook.LMpridmetResult res = LMpridmetHook.getItem(tmpl.id);
                if (res != null && res.item() != null) {
                    item = res.item().clone();
                    price = rnd.nextDouble() * (tmpl.maxPrice - tmpl.minPrice) + tmpl.minPrice;
                }
            }

            if (item == null && !fakeTemplates.isEmpty()) {
                FakeItemTemplate tmpl = fakeTemplates.get(rnd.nextInt(fakeTemplates.size()));
                int amount = rnd.nextInt(tmpl.maxCount - tmpl.minCount + 1) + tmpl.minCount;
                item = new ItemStack(tmpl.material, amount);
                price = rnd.nextDouble() * (tmpl.maxPrice - tmpl.minPrice) + tmpl.minPrice;
            }

            if (item == null) {
                item = new ItemStack(Material.DIAMOND, rnd.nextInt(5) + 1);
                price = 500;
            }

            price = Math.min(2500000, Math.max(1, Math.round(price)));
            long age = System.currentTimeMillis() - (long)(rnd.nextDouble() * (EXPIRE_TIME - 60000L));

            listings.add(new AuctionItem(fakeUUID, fakeName, List.of(item), price, age, false));
        }
    }

    private void loadTemplates() {
        fakeTemplates.clear();
        lmTemplates.clear();

        List<String> rawFakes = getConfig().getStringList("fake-items");
        for (String s : rawFakes) {
            try {
                String[] parts = s.split(":");
                Material mat = Material.matchMaterial(parts[0]);
                if (mat == null) continue;
                int minC = Integer.parseInt(parts[1]);
                int maxC = Integer.parseInt(parts[2]);
                double minP = Double.parseDouble(parts[3]);
                double maxP = Double.parseDouble(parts[4]);
                fakeTemplates.add(new FakeItemTemplate(mat, minC, maxC, minP, maxP));
            } catch (Exception ignored) {}
        }

        if (getConfig().contains("lmpridmet-items")) {
            for (String key : getConfig().getConfigurationSection("lmpridmet-items").getKeys(false)) {
                double minP = getConfig().getDouble("lmpridmet-items." + key + ".min-price", 1000);
                double maxP = getConfig().getDouble("lmpridmet-items." + key + ".max-price", 10000);
                lmTemplates.add(new LMpridmetTemplate(key, minP, maxP));
            }
        }
    }

    /* -----------------------------------------------------------------
     * СОХРАНЕНИЕ И ЗАГРУЗКА ДАННЫХ В data.yml
     * ----------------------------------------------------------------- */
    private void loadAuctionData() {
        dataFile = new File(getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            try { dataFile.createNewFile(); } catch (IOException ignored) {}
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        listings.clear();
        claims.clear();
        offlineSales.clear();

        if (dataConfig.contains("listings")) {
            for (String key : dataConfig.getConfigurationSection("listings").getKeys(false)) {
                try {
                    String path = "listings." + key + ".";
                    UUID id = UUID.fromString(dataConfig.getString(path + "id"));
                    UUID seller = UUID.fromString(dataConfig.getString(path + "seller"));
                    String name = dataConfig.getString(path + "name");
                    double price = dataConfig.getDouble(path + "price");
                    long time = dataConfig.getLong(path + "time");
                    boolean bundle = dataConfig.getBoolean(path + "bundle", false);
                    String base64Items = dataConfig.getString(path + "items");

                    List<ItemStack> items = Arrays.asList(itemStackArrayFromBase64(base64Items));
                    listings.add(new AuctionItem(id, seller, name, items, price, time, bundle));

                    if (name.startsWith("Fake") || isFakeSeller(seller)) {
                        fakeSellers.add(seller);
                    }
                } catch (Exception e) {
                    getLogger().warning("Ошибка загрузки лота " + key);
                }
            }
        }

        if (dataConfig.contains("claims")) {
            for (String uuidStr : dataConfig.getConfigurationSection("claims").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    String base64 = dataConfig.getString("claims." + uuidStr);
                    claims.put(uuid, new ArrayList<>(Arrays.asList(itemStackArrayFromBase64(base64))));
                } catch (Exception ignored) {}
            }
        }

        if (dataConfig.contains("offline-sales")) {
            for (String uuidStr : dataConfig.getConfigurationSection("offline-sales").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    offlineSales.put(uuid, dataConfig.getDouble("offline-sales." + uuidStr));
                } catch (Exception ignored) {}
            }
        }
    }

    public synchronized void saveAuctionData() {
        if (dataConfig == null || dataFile == null) return;

        dataConfig.set("listings", null);
        dataConfig.set("claims", null);
        dataConfig.set("offline-sales", null);

        for (int i = 0; i < listings.size(); i++) {
            AuctionItem item = listings.get(i);
            String path = "listings." + i + ".";
            dataConfig.set(path + "id", item.getId().toString());
            dataConfig.set(path + "seller", item.getSeller().toString());
            dataConfig.set(path + "name", item.getSellerName());
            dataConfig.set(path + "price", item.getPrice());
            dataConfig.set(path + "time", item.getTimestamp());
            dataConfig.set(path + "bundle", item.isBundle());
            dataConfig.set(path + "items", itemStackArrayToBase64(item.getContent().toArray(new ItemStack[0])));
        }

        for (Map.Entry<UUID, List<ItemStack>> entry : claims.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                dataConfig.set("claims." + entry.getKey().toString(), itemStackArrayToBase64(entry.getValue().toArray(new ItemStack[0])));
            }
        }

        for (Map.Entry<UUID, Double> entry : offlineSales.entrySet()) {
            dataConfig.set("offline-sales." + entry.getKey().toString(), entry.getValue());
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            getLogger().severe("Не удалось сохранить data.yml!");
        }
    }

    /* -----------------------------------------------------------------
     * ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ И PDC
     * ----------------------------------------------------------------- */
    public static void setPDC(ItemStack item, NamespacedKey key, String value) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, value);
            item.setItemMeta(meta);
        }
    }

    public static String getPDC(ItemStack item, NamespacedKey key) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.get(key, PersistentDataType.STRING);
    }

    public static String formatPrice(double price) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(' ');
        DecimalFormat df = new DecimalFormat("#,##0.##", symbols);
        return df.format(price);
    }

    public static String hex(String text) {
        if (text == null) return "";
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("&#[a-fA-F0-9]{6}").matcher(text);
        while (matcher.find()) {
            String color = text.substring(matcher.start() + 1, matcher.end());
            text = text.replace("&" + color, net.md_5.bungee.api.ChatColor.of(color).toString());
            matcher = java.util.regex.Pattern.compile("&#[a-fA-F0-9]{6}").matcher(text);
        }
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', text);
    }

    /**
     * Takes an item from the actual hotbar slot instead of relying on a client
     * hand update. This is important for Geyser inventory synchronization.
     */
    private ItemStack takeMainHandItem(Player player) {
        return takeItemFromSlot(player, player.getInventory().getHeldItemSlot());
    }

    private ItemStack takeItemFromSlot(Player player, int slot) {
        ItemStack current = player.getInventory().getItem(slot);
        if (current == null || current.getType().isAir()) {
            return null;
        }

        ItemStack taken = current.clone();
        player.getInventory().setItem(slot, new ItemStack(Material.AIR));
        return taken;
    }

    /**
     * Forces an immediate and next-tick inventory refresh for Bedrock players.
     */
    private void syncInventory(Player player) {
        if (!player.isOnline()) {
            return;
        }
        player.updateInventory();
        Bukkit.getScheduler().runTask(this, () -> {
            if (player.isOnline()) {
                player.updateInventory();
            }
        });
    }

    private String getItemDisplayName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        return item.getType().name();
    }

    private double parsePrice(String input) {
        try {
            double val = Double.parseDouble(input);
            if (val < 1 || val > 2500000) return -1;
            return val;
        } catch (Exception e) {
            return -1;
        }
    }

    private static String itemStackArrayToBase64(ItemStack[] items) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeInt(items.length);
            for (ItemStack item : items) {
                dataOutput.writeObject(item);
            }
            dataOutput.close();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Ошибка сохранения ItemStack в Base64", e);
        }
    }

    private static ItemStack[] itemStackArrayFromBase64(String data) throws IOException {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack[] items = new ItemStack[dataInput.readInt()];
            for (int i = 0; i < items.length; i++) {
                items[i] = (ItemStack) dataInput.readObject();
            }
            dataInput.close();
            return items;
        } catch (ClassNotFoundException e) {
            throw new IOException("Неизвестный класс при десериализации ItemStack", e);
        }
    }

    /* -----------------------------------------------------------------
     * ВНУТРЕННИЕ КЛАССЫ И ШАБЛОНЫ
     * ----------------------------------------------------------------- */
    private static class FakeItemTemplate {
        Material material;
        int minCount, maxCount;
        double minPrice, maxPrice;

        public FakeItemTemplate(Material material, int minCount, int maxCount, double minPrice, double maxPrice) {
            this.material = material;
            this.minCount = minCount;
            this.maxCount = maxCount;
            this.minPrice = minPrice;
            this.maxPrice = maxPrice;
        }
    }

    private static class LMpridmetTemplate {
        String id;
        double minPrice, maxPrice;

        public LMpridmetTemplate(String id, double minPrice, double maxPrice) {
            this.id = id;
            this.minPrice = minPrice;
            this.maxPrice = maxPrice;
        }
    }
}