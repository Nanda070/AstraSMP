package com.astrasmp.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import com.astrasmp.AstraSMPPlugin;
import com.astrasmp.items.ItemRegistry;
import com.astrasmp.model.AuctionLot;
import com.astrasmp.model.ContractRecord;
import com.astrasmp.model.Guild;
import com.astrasmp.model.PlayerProfile;
import com.astrasmp.service.AuctionService;
import com.astrasmp.service.ContractService;
import com.astrasmp.service.ServiceManager;
import com.astrasmp.util.TextUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.*;

public final class GuiManager {

    public enum MenuType {
        MAIN, AUCTION, ITEMS, RECIPE_VIEW, STATS, CONTRACTS, ADMIN, SELL_RESOURCES, SELL_FOOD,
        ADMIN_PLAYERS, GUILD_UPGRADE, ADMIN_GUILDS, ADMIN_GIVE_ITEMS, GUILD, GUILD_MEMBERS, GUILD_RANKS_LIST, GUILD_RANK_SETTINGS, GUILD_RANK_PERMISSIONS, GUILD_TREASURY
    }

    public record MenuHolder(MenuType type, int page, String query, String metadata) implements InventoryHolder {
        @Override
        public @NotNull Inventory getInventory() {
            return Bukkit.createInventory(null, 9);
        }
    }

    private final AstraSMPPlugin plugin;
    private final ServiceManager services;
    private final AuctionService auction;
    private final ContractService contracts;

    private final NamespacedKey targetNameKey;
    private final NamespacedKey rankIdKey;
    private final NamespacedKey permNodeKey;

    public GuiManager(AstraSMPPlugin plugin, ServiceManager services, AuctionService auction, ContractService contracts) {
        this.plugin = plugin;
        this.services = services;
        this.auction = auction;
        this.contracts = contracts;

        this.targetNameKey = new NamespacedKey(plugin, "target_name");
        this.rankIdKey = new NamespacedKey(plugin, "rank_id");
        this.permNodeKey = new NamespacedKey(plugin, "perm_node");
    }

    private Component title(String text) {
        return Component.text(TextUtil.color(plugin.getConfig().getString("gui.title-color", "&8") + text));
    }

    private ItemStack button(Material material, String name, String... lore) {
        ItemStack stack = new ItemStack(material);
        stack.editMeta(meta -> {
            meta.displayName(Component.text(TextUtil.color(name)));
            if (lore != null && lore.length > 0) {
                meta.lore(Arrays.stream(lore).map(line -> Component.text(TextUtil.color(line))).toList());
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        });
        return stack;
    }

    private ItemStack build(Material mat, String name, String lore) {
        ItemStack item = new ItemStack(mat);
        item.editMeta(meta -> {
            meta.displayName(Component.text(TextUtil.color(name)));
            List<Component> loreComponents = new ArrayList<>();
            if (lore != null && !lore.isEmpty()) {
                for (String line : lore.split("\n")) {
                    loreComponents.add(Component.text(TextUtil.color(line)));
                }
            }
            meta.lore(loreComponents);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        });
        return item;
    }

    private void fill(Inventory inv) {
        ItemStack filler = new ItemStack(Material.valueOf(plugin.getConfig().getString("gui.filler", "GRAY_STAINED_GLASS_PANE")));
        filler.editMeta(meta -> meta.displayName(Component.empty()));
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);
    }

    public void openMain(Player player) {
        Inventory inv = Bukkit.createInventory(new MenuHolder(MenuType.MAIN, 0, "", ""), 45, title("ChetCraft Меню"));
        fill(inv);

        inv.setItem(13, services.quests().createQuestItem(player));
        inv.setItem(20, button(Material.GOLD_INGOT, "&aСкупщик", "&7Продать ресурсы и еду", "&eНажми, чтобы открыть"));
        inv.setItem(21, button(Material.ANVIL, "&dАукцион", "&7Рынок предметов"));
        inv.setItem(22, button(Material.NETHERITE_SWORD, "&cПредметы", "&7Список всех уникальных вещей"));
        inv.setItem(23, button(Material.ENDER_EYE, "&bСтатистика", "&7Топ игроков и MMR"));
        inv.setItem(24, button(Material.BOOK, "&5Контракты", "&7Заказы на убийства"));
        inv.setItem(31, button(Material.WHITE_BANNER, "&6Моя Гильдия", "&7Управление кланом", "&eКлик: Открыть меню"));

        inv.setItem(37, build(Material.COMPASS, "&bСпавн", "&7Телепортация в\n&7безопасную зону"));
        inv.setItem(38, build(Material.DIAMOND_SWORD, "&cPvP Арена", "&7Сражайся с другими\n&7игроками!"));
        inv.setItem(40, build(Material.EMERALD, "&6Казино", "&7Испытай удачу\n&7в рулетке"));
        inv.setItem(42, build(Material.GOLDEN_APPLE, "&dИвент Шоп", "&7Уникальные вещи\n&7за Event Points"));
        inv.setItem(43, build(Material.CAMPFIRE, "&eAFK Зона", "&7Стой и получай\n&7по 5 ❂ в минуту"));

        player.openInventory(inv);
    }

    public void openSellResources(Player player) {
        Inventory inv = Bukkit.createInventory(new MenuHolder(MenuType.SELL_RESOURCES, 0, "", ""), 36, title("Скупщик: Ресурсы"));
        fill(inv);
        int slot = 0;
        for (Material mat : services.economy().getResourceItems()) {
            double price = services.economy().getPrice(mat);
            if (price <= 0 || slot >= 27) continue;
            inv.setItem(slot++, createSellIcon(mat, price));
        }
        inv.setItem(35, button(Material.ARROW, "&eСледующая страница: Еда"));
        inv.setItem(31, button(Material.BARRIER, "&cНазад в меню"));
        player.openInventory(inv);
    }

    public void openSellFood(Player player) {
        Inventory inv = Bukkit.createInventory(new MenuHolder(MenuType.SELL_FOOD, 1, "", ""), 36, title("Скупщик: Еда"));
        fill(inv);
        int slot = 0;
        for (Material mat : services.economy().getFoodItems()) {
            double price = services.economy().getPrice(mat);
            if (price <= 0 || slot >= 27) continue;
            inv.setItem(slot++, createSellIcon(mat, price));
        }
        inv.setItem(27, button(Material.ARROW, "&eНазад: Ресурсы"));
        inv.setItem(31, button(Material.BARRIER, "&cНазад в меню"));
        player.openInventory(inv);
    }

    public void openStatsMenu(Player player) {
        PlayerProfile profile = services.economy().profile(player.getUniqueId(), player.getName());
        openStats(player, profile);
    }

    public void openStats(Player viewer, PlayerProfile profile) {
        Inventory inv = Bukkit.createInventory(new MenuHolder(MenuType.STATS, 0, "", ""), 27, title("Статистика"));
        fill(inv);
        inv.setItem(13, button(Material.PLAYER_HEAD, "&b" + profile.getName(),
                "&7Баланс: &a" + profile.getCoins(),
                "&7MMR: &f" + profile.getMmr(),
                "&7Убийств: &f" + profile.getKills(),
                "&7Смертей: &f" + profile.getDeaths()
        ));
        inv.setItem(22, button(Material.ARROW, "&7Назад в меню"));
        viewer.openInventory(inv);
    }

    public void openAdmin(Player player) {
        Inventory inv = Bukkit.createInventory(
            new MenuHolder(MenuType.ADMIN, 0, "", ""), 45,
            title("Панель администратора"));
        fill(inv);
    
        inv.setItem(10, button(Material.COMMAND_BLOCK,  "&e⚡ Ивенты",          "&7/admin event <тип>",
                                                                                "&7/admin spawnevent <тип>"));
        inv.setItem(11, button(Material.CHEST,           "&6Выдача предметов",   "&7Все кастомные вещи сервера"));
        inv.setItem(12, button(Material.GOLD_BLOCK,      "&aЭкономика",          "&7Управление балансами",
                                                                                "&7/admin setcoins <игрок> <сумма>"));
        inv.setItem(13, button(Material.WHITE_BANNER,    "&dГильдии",            "&7Управление гильдиями",
                                                                                "&7Просмотр, удаление, изменение уровня"));
        inv.setItem(14, button(Material.PLAYER_HEAD,     "&bИгроки",             "&7Invsee, заморозка, баны"));
        inv.setItem(15, button(Material.COMPARATOR,      "&cКонфигурация",       "&7/admin reload"));
    
        inv.setItem(20, button(Material.ENDER_CHEST,     "&bInvsee",             "&7Открыть инвентарь игрока"));
        inv.setItem(21, button(Material.PACKED_ICE,      "&bЗаморозить",         "&7Остановить игрока"));
        inv.setItem(23, button(Material.MAGMA_BLOCK,     "&cРазморозить",        "&7Снять заморозку"));
        inv.setItem(24, button(Material.RECOVERY_COMPASS,"&7История",            "&7Журнал действий"));
    
        player.openInventory(inv);
    }

    public void openAdminGuilds(Player admin) {
        Collection<Guild> allGuilds = services.guilds().getGuilds().values();
        Inventory inv = Bukkit.createInventory(
            new MenuHolder(MenuType.ADMIN_GUILDS, 0, "", ""), 54,
            title("Админ: Гильдии (" + allGuilds.size() + ")"));
        fill(inv);
    
        int slot = 0;
        for (Guild guild : allGuilds) {
            if (slot >= 45) break;
            String leaderName = Objects.requireNonNullElse(
                Bukkit.getOfflinePlayer(guild.getLeader()).getName(), "Неизвестный");
    
            ItemStack icon = button(Material.WHITE_BANNER,
                "&6" + guild.getName(),
                "&7ID: &8" + guild.getId().toString().substring(0, 8),
                "&7Лидер: &f" + leaderName,
                "&7Уровень: &e" + guild.getLevel(),
                "&7Участников: &f" + guild.getMembers().size(),
                "&7Казна: &a" + guild.getBalance() + " ❂",
                "&7База: " + (guild.getCoreLocation() != null ? "&aУстановлена" : "&cНет"),
                "",
                "&eЛКМ &7— Управление",
                "&cShift+ПКМ &7— Распустить"
            );
            // Сохраняем ID гильдии в PDC для обработки клика
            icon.editMeta(meta -> meta.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey("astrasmp", "guild_id"),
                PersistentDataType.STRING,
                guild.getId().toString()
            ));
            inv.setItem(slot++, icon);
        }
    
        inv.setItem(49, button(Material.ARROW, "&7Назад в админ-панель"));
        admin.openInventory(inv);
    }

    public void openAdminItems(Player player) {
        Inventory inv = Bukkit.createInventory(new MenuHolder(MenuType.ADMIN_GIVE_ITEMS, 0, "", ""), 54, title("Админ: Выдача предметов"));
        fill(inv);
        int slot = 0;
        for (ItemStack item : ItemRegistry.getAllItems()) {
            if (slot >= 45) break;
            inv.setItem(slot++, item);
        }
        inv.setItem(49, button(Material.ARROW, "&cНазад в админ-панель"));
        player.openInventory(inv);
    }

    public void openPlayerList(Player admin, String actionCmd) {
        Inventory inv = Bukkit.createInventory(new MenuHolder(MenuType.ADMIN_PLAYERS, 0, actionCmd, ""), 54, title("Выберите игрока"));
        fill(inv);
        int slot = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (slot >= 45) break;
            inv.setItem(slot++, button(Material.PLAYER_HEAD, "&b" + p.getName(), "&7Кликните для применения", "&eКоманда: /" + actionCmd));
        }
        inv.setItem(49, button(Material.ARROW, "&cНазад"));
        admin.openInventory(inv);
    }

    public void openAuction(Player player, int page, String query) {
        Inventory inv = Bukkit.createInventory(new MenuHolder(MenuType.AUCTION, page, query, ""), 54, title("Аукцион"));
        fill(inv);
        List<AuctionLot> lots = auction.search(query);
        int from = page * 45;
        int slot = 0;
        for (int i = from; i < lots.size() && slot < 45; i++) {
            AuctionLot lot = lots.get(i);
            ItemStack display = lot.getItem().clone();
            display.editMeta(meta -> {
                meta.displayName(Component.text(TextUtil.color("&fЛот #" + lot.getId())));
                meta.lore(List.of(
                        Component.text(TextUtil.color("&7Цена: &a" + lot.getPrice() + " монет")),
                        Component.text(TextUtil.color("&eНажми, чтобы купить"))
                ));
            });
            inv.setItem(slot++, display);
        }
        inv.setItem(45, button(Material.ARROW, "&7Назад"));
        inv.setItem(49, button(Material.ARROW, "&cВ меню"));
        inv.setItem(53, button(Material.ARROW, "&7Вперед"));
        player.openInventory(inv);
    }

    public void openContracts(Player player) {
        Inventory inv = Bukkit.createInventory(new MenuHolder(MenuType.CONTRACTS, 0, "", ""), 54, title("Доска заказов"));
        fill(inv);
        int slot = 0;
        for (ContractRecord c : contracts.active()) {
            if (slot >= 45) break;
            inv.setItem(slot++, button(Material.PAPER, "&fКонтракт #" + c.getId(),
                    "&7Цель: &c" + c.getTargetUuid(),
                    "&7Награда: &a" + c.getReward()));
        }
        inv.setItem(49, button(Material.ARROW, "&7Назад в меню"));
        player.openInventory(inv);
    }

    // ==========================================
    // ГИЛЬДИИ (PDC ИНТЕГРАЦИЯ)
    // ==========================================

    public void openGuildMain(Player player, Guild guild) {
        Inventory inv = Bukkit.createInventory(
            new MenuHolder(MenuType.GUILD, 0, "", ""), 27,
            title("Гильдия: " + guild.getName()));
        fill(inv);
    
        // Иконка гильдии с прогресс-баром XP
        boolean maxLevel = guild.getLevel() >= Guild.MAX_LEVEL;
        String xpLine = maxLevel
            ? "&aМаксимальный уровень!"
            : "&7XP: " + guild.buildXpBar();
    
        inv.setItem(4, button(Material.WHITE_BANNER,
            "&6&l" + guild.getName(),
            "&7Уровень: &e" + guild.getLevel() + (maxLevel ? " &a(MAX)" : "/" + Guild.MAX_LEVEL),
            xpLine,
            "&7Казна: &a" + guild.getBalance() + " ❂",
            "&7Участников: &f" + guild.getMembers().size(),
            "&7Радиус базы: &b" + guild.getCoreRadius() + " блоков"
        ));
    
        inv.setItem(10, button(Material.PLAYER_HEAD,     "&bУчастники",      "&7Просмотр и управление составом"));
        inv.setItem(11, button(Material.EXPERIENCE_BOTTLE,"&dРазвитие",      "&7Прокачка гильдии и перки",
                                                                            "&7Уровень: &e" + guild.getLevel()));
        inv.setItem(15, button(Material.GOLD_INGOT,      "&aКазна",          "&7Пополнение и снятие монет",
                                                                            "&7Баланс: &f" + guild.getBalance() + " ❂"));
        inv.setItem(16, button(Material.COMPARATOR,      "&eНастройка прав", "&7Матрица рангов и доступов"));
        inv.setItem(22, button(Material.ARROW,           "&7Назад в меню"));
    
        player.openInventory(inv);
    }

    public void openGuildUpgrade(Player player, Guild guild) {
        Inventory inv = Bukkit.createInventory(
            new MenuHolder(MenuType.GUILD_UPGRADE, 0, "", ""), 45,
            title("Развитие гильдии"));
        fill(inv);
    
        // Текущий уровень
        boolean maxLevel = guild.getLevel() >= Guild.MAX_LEVEL;
        inv.setItem(4, button(Material.EXPERIENCE_BOTTLE,
            "&dГильдия &f" + guild.getName(),
            "&7Уровень: &e" + guild.getLevel() + (maxLevel ? " &a(MAX)" : "/" + Guild.MAX_LEVEL),
            "&7XP: " + guild.buildXpBar(),
            "",
            "&7XP начисляется автоматически",
            "&7за активность участников"
        ));
    
        // Перки по уровням
        record Perk(int level, Material mat, String name, String desc) {}
        List<Perk> perks = List.of(
            new Perk(1,  Material.GRASS_BLOCK,   "&aУровень 1 &8— Старт",             "&7Радиус базы: &b15 блоков"),
            new Perk(2,  Material.OAK_PLANKS,    "&aУровень 2 &8— Расширение",        "&7Радиус базы: &b20 блоков"),
            new Perk(3,  Material.IRON_BLOCK,    "&aУровень 3 &8— Укрепление",        "&7Радиус базы: &b25 блоков\n&7+5% к налогу в казну"),
            new Perk(4,  Material.GOLD_BLOCK,    "&aУровень 4 &8— Процветание",       "&7Радиус базы: &b30 блоков\n&7Казна принимает большие суммы"),
            new Perk(5,  Material.DIAMOND_BLOCK, "&aУровень 5 &8— Господство",        "&7Радиус базы: &b35 блоков\n&7Кастомные цвета в названии"),
            new Perk(6,  Material.EMERALD_BLOCK, "&aУровень 6 &8— Элита",             "&7Радиус базы: &b40 блоков"),
            new Perk(7,  Material.NETHERITE_BLOCK,"&aУровень 7 &8— Легенда",          "&7Радиус базы: &b45 блоков"),
            new Perk(8,  Material.BEACON,        "&aУровень 8 &8— Бастион",           "&7Радиус базы: &b50 блоков"),
            new Perk(9,  Material.DRAGON_EGG,    "&aУровень 9 &8— Фракция",           "&7Радиус базы: &b55 блоков"),
            new Perk(10, Material.NETHER_STAR,   "&6&lУровень 10 &8— Вознесение",     "&7Радиус базы: &b60 блоков\n&7Максимальный уровень!")
        );
    
        int[] slots = {10, 11, 12, 13, 14, 28, 29, 30, 31, 32};
        for (int i = 0; i < perks.size(); i++) {
            Perk perk = perks.get(i);
            boolean unlocked = guild.getLevel() >= perk.level();
            boolean current  = guild.getLevel() == perk.level();
    
            String status = unlocked ? "&a✔ Разблокировано" : "&8✘ Заблокировано";
            if (current) status = "&e► Текущий уровень";
    
            List<String> lore = new ArrayList<>();
            lore.add(status);
            lore.add("");
            for (String line : perk.desc().split("\n")) lore.add(line);
    
            Material mat = unlocked ? perk.mat() : Material.GRAY_STAINED_GLASS_PANE;
            String name  = unlocked ? perk.name() : "&8" + "Уровень " + perk.level();
    
            // ItemStack с учётом разблокировки
            ItemStack icon = button(mat, name, lore.toArray(new String[0]));
    
            // Подсветка текущего уровня
            if (current) {
                icon.editMeta(meta -> meta.addEnchant(
                    org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true));
            }
            inv.setItem(slots[i], icon);
        }
    
        inv.setItem(40, button(Material.ARROW, "&7Назад в меню гильдии"));
        player.openInventory(inv);
    }

    public void openGuildMembers(Player viewer, Guild guild) {
        Inventory inv = Bukkit.createInventory(new MenuHolder(MenuType.GUILD_MEMBERS, 0, "", ""), 54, title("Состав: " + guild.getName()));
        fill(inv);
        int slot = 0;
        for (var entry : guild.getMembers().entrySet()) {
            if (slot >= 45) break;
            UUID uuid = entry.getKey();
            String rankId = entry.getValue();
            Guild.Rank rankObj = guild.getRanks().get(rankId);
            String rankTitle = rankObj != null ? rankObj.getName() : rankId;

            String name = Bukkit.getOfflinePlayer(uuid).getName();
            String finalName = name != null ? name : "Unknown";

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            head.editMeta(SkullMeta.class, meta -> {
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));
                meta.displayName(Component.text(TextUtil.color("&b" + finalName)));
                meta.lore(List.of(
                        Component.text(TextUtil.color("&7Ранг: " + rankTitle)),
                        Component.text(""),
                        Component.text(TextUtil.color("&eЛКМ &7- Повысить")),
                        Component.text(TextUtil.color("&eПКМ &7- Понизить")),
                        Component.text(TextUtil.color("&cShift+ПКМ &7- Кик"))));

                meta.getPersistentDataContainer().set(targetNameKey, PersistentDataType.STRING, finalName);
            });
            inv.setItem(slot++, head);
        }
        inv.setItem(49, button(Material.ARROW, "&cНазад"));
        viewer.openInventory(inv);
    }

    public void openGuildTreasury(Player player, Guild guild) {
        long balance    = guild.getBalance();
        long playerBal  = services.economy().getBalance(player.getUniqueId());
        boolean canBank = guild.hasPermission(player.getUniqueId(), "guild.bank");
    
        Inventory inv = Bukkit.createInventory(
            new MenuHolder(MenuType.GUILD_TREASURY, 0, "", ""), 27,
            title("Казна гильдии"));
        fill(inv);
    
        // Кнопка внесения (доступна всем)
        inv.setItem(10, button(Material.GOLD_INGOT,
            "&a✚ Внести монеты",
            "&7Ваш баланс: &f" + playerBal + " ❂",
            "",
            "&eЛКМ &7— внести &f1,000 ❂",
            "&eShift+ЛКМ &7— внести &f10,000 ❂"
        ));
    
        // Баланс казны в центре
        inv.setItem(13, button(Material.CHEST,
            "&6Казна гильдии",
            "&7Баланс: &a&l" + balance + " ❂",
            "",
            "&7Уровень гильдии: &e" + guild.getLevel()
        ));
    
        // Кнопка снятия (только с правом guild.bank)
        if (canBank) {
            inv.setItem(16, button(Material.IRON_INGOT,
                "&c✖ Снять монеты",
                "&7Баланс казны: &f" + balance + " ❂",
                "",
                "&eЛКМ &7— снять &f1,000 ❂",
                "&eShift+ЛКМ &7— снять &f10,000 ❂"
            ));
        } else {
            inv.setItem(16, button(Material.BARRIER,
                "&8Снятие заблокировано",
                "&7Необходимо право: &cguild.bank"
            ));
        }
    
        inv.setItem(22, button(Material.ARROW, "&7Назад в меню гильдии"));
        player.openInventory(inv);
    }

    public void openGuildRanksList(Player player, Guild guild) {
        Inventory inv = Bukkit.createInventory(new MenuHolder(MenuType.GUILD_RANKS_LIST, 0, "", ""), 54, title("Управление рангами"));
        fill(inv);

        int slot = 0;
        List<Guild.Rank> sortedRanks = new ArrayList<>(guild.getRanks().values());
        sortedRanks.sort((r1, r2) -> Integer.compare(r2.getPriority(), r1.getPriority()));

        for (Guild.Rank rank : sortedRanks) {
            if (slot >= 45) break;
            boolean isLeader = rank.getId().equals("leader");
            ItemStack icon = button(isLeader ? Material.NETHER_STAR : Material.IRON_CHESTPLATE,
                    rank.getName(),
                    "&7ID: &f" + rank.getId(),
                    "&7Вес (Приоритет): &f" + rank.getPriority(),
                    "&7Прав: &f" + (rank.getPermissions().contains("*") ? "Все" : rank.getPermissions().size()),
                    "",
                    isLeader ? "&c[Лидерский ранг нельзя редактировать]" : "&eЛКМ: &aНастроить ранг"
            );

            icon.editMeta(meta -> meta.getPersistentDataContainer().set(rankIdKey, PersistentDataType.STRING, rank.getId()));
            inv.setItem(slot++, icon);
        }

        inv.setItem(49, button(Material.ARROW, "&cНазад в меню гильдии"));
        inv.setItem(53, button(Material.EMERALD, "&a+ Создать новый ранг", "&7Нажмите, чтобы добавить", "&7новую должность в матрицу."));
        player.openInventory(inv);
    }

    public void openRankSettings(Player player, Guild guild, String rankId) {
        Guild.Rank rank = guild.getRanks().get(rankId);
        if (rank == null || rankId.equals("leader")) return;

        Inventory inv = Bukkit.createInventory(new MenuHolder(MenuType.GUILD_RANK_SETTINGS, 0, "", rankId), 27, title("Настройка: " + rank.getName()));
        fill(inv);

        inv.setItem(10, button(Material.NAME_TAG, "&eИзменить название", "&7Текущее: " + rank.getName(), "&eЛКМ: &aВвести в чат"));
        inv.setItem(12, button(Material.COMPARATOR, "&bМатрица прав", "&7Включить/выключить доступы", "&eЛКМ: &aОткрыть настройки"));
        inv.setItem(14, button(Material.ANVIL, "&6Изменить вес (Приоритет)", "&7Текущий вес: &f" + rank.getPriority(), "&7Чем выше вес, тем старше ранг.", "&eЛКМ: &aВвести в чат"));
        inv.setItem(16, button(Material.BARRIER, "&cУдалить ранг", "&7Участники с этим рангом", "&7станут Новобранцами.", "&cShift + ЛКМ для удаления"));
        inv.setItem(22, button(Material.ARROW, "&cНазад к списку"));
        player.openInventory(inv);
    }

    public void openRankPermissions(Player player, Guild guild, String rankId) {
        Guild.Rank rank = guild.getRanks().get(rankId);
        if (rank == null) return;

        Inventory inv = Bukkit.createInventory(new MenuHolder(MenuType.GUILD_RANK_PERMISSIONS, 0, "", rankId), 45, title("Права: " + rank.getName()));
        fill(inv);

        inv.setItem(10, toggleButton(Material.COMPASS, "Установка точки дома", guild, rankId, "guild.home.set"));
        inv.setItem(11, toggleButton(Material.GOLD_INGOT, "Управление казной (Снятие)", guild, rankId, "guild.bank"));
        inv.setItem(12, toggleButton(Material.PLAYER_HEAD, "Приглашение игроков", guild, rankId, "guild.invite"));
        inv.setItem(13, toggleButton(Material.IRON_SWORD, "Исключение игроков (Kick)", guild, rankId, "guild.kick"));
        inv.setItem(14, toggleButton(Material.EMERALD, "Повышение/Понижение", guild, rankId, "guild.promote"));
        inv.setItem(15, toggleButton(Material.ENCHANTED_BOOK, "Прокачка перков", guild, rankId, "guild.upgrade"));

        inv.setItem(40, button(Material.ARROW, "&cНазад в настройки ранга"));
        player.openInventory(inv);
    }

    private ItemStack toggleButton(Material mat, String name, Guild guild, String rankId, String node) {
        Guild.Rank rank = guild.getRanks().get(rankId);
        boolean has = rank != null && rank.getPermissions().contains(node);
        ItemStack btn = button(mat, (has ? "&a" : "&c") + name,
                "&7Узел: &8" + node,
                "&7Статус: " + (has ? "&aРазрешено" : "&cЗапрещено"),
                "",
                "&eКлик: &7Изменить");
        btn.editMeta(meta -> meta.getPersistentDataContainer().set(permNodeKey, PersistentDataType.STRING, node));
        return btn;
    }

    // ==========================================
    // ЛОГИКА РЕЦЕПТОВ (ВЕРСТАК)
    // ==========================================

    private ItemStack[] makeRecipe(Material... mats) {
        ItemStack[] items = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            if (i < mats.length && mats[i] != null && mats[i] != Material.AIR) {
                items[i] = new ItemStack(mats[i]);
            }
        }
        return items;
    }

    private ItemStack[] getRecipeMatrix(String id) {
        Material AIR = Material.AIR;
        switch (id) {
            case "mining3x3" -> { return makeRecipe(Material.DIAMOND_BLOCK, Material.DIAMOND_BLOCK, Material.DIAMOND_BLOCK, Material.TNT, Material.STICK, Material.TNT, AIR, Material.STICK, AIR); }
            case "magnet" -> { return makeRecipe(Material.REDSTONE_BLOCK, Material.IRON_BLOCK, Material.REDSTONE_BLOCK, Material.IRON_BLOCK, Material.DIAMOND_PICKAXE, Material.IRON_BLOCK, Material.REDSTONE_BLOCK, Material.IRON_BLOCK, Material.REDSTONE_BLOCK); }
            case "frostAxe" -> { return makeRecipe(Material.BLUE_ICE, Material.BLUE_ICE, Material.BLUE_ICE, Material.BLUE_ICE, Material.DIAMOND_AXE, Material.BLUE_ICE, Material.BLUE_ICE, Material.BLUE_ICE, Material.BLUE_ICE); }
            case "vampireDagger" -> { return makeRecipe(AIR, Material.REDSTONE_BLOCK, AIR, Material.GHAST_TEAR, Material.DIAMOND_SWORD, Material.GHAST_TEAR, AIR, Material.REDSTONE_BLOCK, AIR); }
            case "infernoSword" -> { return makeRecipe(Material.MAGMA_BLOCK, Material.BLAZE_ROD, Material.MAGMA_BLOCK, Material.BLAZE_ROD, Material.NETHERITE_SWORD, Material.BLAZE_ROD, Material.MAGMA_BLOCK, Material.BLAZE_ROD, Material.MAGMA_BLOCK); }
            case "berserker_helmet" -> { return makeRecipe(Material.IRON_BLOCK, Material.IRON_BLOCK, Material.IRON_BLOCK, Material.REDSTONE_BLOCK, AIR, Material.REDSTONE_BLOCK, AIR, AIR, AIR); }
            case "berserker_chestplate" -> { return makeRecipe(Material.IRON_BLOCK, AIR, Material.IRON_BLOCK, Material.IRON_BLOCK, Material.REDSTONE_BLOCK, Material.IRON_BLOCK, Material.IRON_BLOCK, Material.IRON_BLOCK, Material.IRON_BLOCK); }
            case "berserker_leggings" -> { return makeRecipe(Material.IRON_BLOCK, Material.IRON_BLOCK, Material.IRON_BLOCK, Material.REDSTONE_BLOCK, AIR, Material.REDSTONE_BLOCK, Material.IRON_BLOCK, AIR, Material.IRON_BLOCK); }
            case "berserker_boots" -> { return makeRecipe(Material.REDSTONE_BLOCK, AIR, Material.REDSTONE_BLOCK, Material.IRON_BLOCK, AIR, Material.IRON_BLOCK, AIR, AIR, AIR); }
            case "juggernaut_helmet" -> { return makeRecipe(Material.OBSIDIAN, Material.OBSIDIAN, Material.OBSIDIAN, Material.NETHERITE_INGOT, AIR, Material.NETHERITE_INGOT, AIR, AIR, AIR); }
            case "juggernaut_chestplate" -> { return makeRecipe(Material.OBSIDIAN, AIR, Material.OBSIDIAN, Material.OBSIDIAN, Material.GOLDEN_APPLE, Material.OBSIDIAN, Material.OBSIDIAN, Material.OBSIDIAN, Material.OBSIDIAN); }
            case "juggernaut_leggings" -> { return makeRecipe(Material.OBSIDIAN, Material.OBSIDIAN, Material.OBSIDIAN, Material.NETHERITE_INGOT, AIR, Material.NETHERITE_INGOT, Material.OBSIDIAN, AIR, Material.OBSIDIAN); }
            case "juggernaut_boots" -> { return makeRecipe(Material.NETHERITE_INGOT, AIR, Material.NETHERITE_INGOT, Material.OBSIDIAN, AIR, Material.OBSIDIAN, AIR, AIR, AIR); }
            default -> { return null; }
        }
    }

    public void openRecipeView(Player player, ItemStack resultItem, String customId) {
        Inventory inv = Bukkit.createInventory(new MenuHolder(MenuType.RECIPE_VIEW, 0, "", customId), 45, title("Рецепт: Крафт"));
        fill(inv);

        ItemStack[] matrix = getRecipeMatrix(customId);
        if (matrix == null) return;

        int[] slots = {10, 11, 12, 19, 20, 21, 28, 29, 30};

        for (int i = 0; i < 9; i++) {
            if (matrix[i] != null && matrix[i].getType() != Material.AIR) {
                inv.setItem(slots[i], matrix[i]);
            } else {
                inv.setItem(slots[i], new ItemStack(Material.AIR));
            }
        }

        inv.setItem(24, resultItem);
        inv.setItem(23, button(Material.GREEN_STAINED_GLASS_PANE, "&aРезультат ->"));
        inv.setItem(40, button(Material.ARROW, "&cНазад к списку"));

        player.openInventory(inv);
    }

    private ItemStack withLoreHint(ItemStack original) {
        if (original == null || original.getType().isAir()) return original;
        ItemStack displayItem = original.clone();
        displayItem.editMeta(meta -> {
            List<Component> lore = meta.lore();
            if (lore == null) lore = new ArrayList<>();
            else lore = new ArrayList<>(lore);

            lore.add(Component.text(""));
            String id = meta.getPersistentDataContainer().get(
                    new org.bukkit.NamespacedKey("astrasmp", "custom_id"),
                    org.bukkit.persistence.PersistentDataType.STRING
            );

            if (id != null && getRecipeMatrix(id) != null) {
                lore.add(Component.text(TextUtil.color("&eЛКМ: &aПосмотреть рецепт")));
            } else {
                lore.add(Component.text(TextUtil.color("&cРецепта нет (Добывается в мире)")));
            }
            meta.lore(lore);
        });
        return displayItem;
    }

    // ==========================================
    // ВИТРИНА
    // ==========================================

    public void openItems(Player player) {
        Inventory inv = Bukkit.createInventory(new MenuHolder(MenuType.ITEMS, 0, "", ""), 54, title("Уникальные предметы"));
        fill(inv);

        inv.setItem(2, withLoreHint(ItemRegistry.mining3x3()));
        inv.setItem(3, withLoreHint(ItemRegistry.mining5x5()));
        inv.setItem(4, withLoreHint(ItemRegistry.veinMiner()));
        inv.setItem(5, withLoreHint(ItemRegistry.autoSmelt()));
        inv.setItem(6, withLoreHint(ItemRegistry.magnet()));

        inv.setItem(11, withLoreHint(ItemRegistry.mining3x3Netherite()));
        inv.setItem(12, withLoreHint(ItemRegistry.mining5x5Netherite()));
        inv.setItem(13, withLoreHint(ItemRegistry.veinMinerNetherite()));
        inv.setItem(14, withLoreHint(ItemRegistry.autoSmeltNetherite()));
        inv.setItem(15, withLoreHint(ItemRegistry.magnetNetherite()));

        inv.setItem(20, withLoreHint(ItemRegistry.shadowBlade()));
        inv.setItem(21, withLoreHint(ItemRegistry.thunderHammer()));
        inv.setItem(22, withLoreHint(ItemRegistry.vampireDagger()));
        inv.setItem(23, withLoreHint(ItemRegistry.infernoSword()));
        inv.setItem(24, withLoreHint(ItemRegistry.frostAxe()));

        inv.setItem(28, withLoreHint(ItemRegistry.mercenaryChestplate()));
        inv.setItem(30, withLoreHint(ItemRegistry.berserkerChestplate()));
        inv.setItem(32, withLoreHint(ItemRegistry.inquisitorChestplate()));
        inv.setItem(34, withLoreHint(ItemRegistry.juggernautChestplate()));

        inv.setItem(38, withLoreHint(ItemRegistry.totemSpeed()));
        inv.setItem(39, withLoreHint(ItemRegistry.totemShield()));
        inv.setItem(40, withLoreHint(ItemRegistry.totemLightning()));
        inv.setItem(41, withLoreHint(ItemRegistry.totemExplosion()));
        inv.setItem(42, withLoreHint(ItemRegistry.totemTeleport()));

        inv.setItem(47, withLoreHint(ItemRegistry.minerChestplate()));
        inv.setItem(49, button(Material.BARRIER, "&cНазад в меню"));
        inv.setItem(51, withLoreHint(ItemRegistry.bloodHunterChestplate()));

        player.openInventory(inv);
    }

    // ==========================================
    // HANDLER
    // ==========================================

    public boolean handleClick(Player player, InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof MenuHolder holder)) return false;
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return true;

        if (clicked.getType() == Material.valueOf(plugin.getConfig().getString("gui.filler", "GRAY_STAINED_GLASS_PANE"))) {
            return true;
        }

        switch (holder.type()) {
            case MAIN -> {
                switch (event.getSlot()) {
                    case 13 -> player.performCommand("quest");
                    case 20 -> openSellResources(player);
                    case 21 -> openAuction(player, 0, "");
                    case 22 -> openItems(player);
                    case 23 -> openStatsMenu(player);
                    case 24 -> openContracts(player);
                    case 31 -> {
                        Guild guild = services.guilds().getPlayerGuild(player.getUniqueId());
                        if (guild != null) openGuildMain(player, guild);
                        else TextUtil.send(player, "&cУ вас нет гильдии! Создайте её: /guild create <название>");
                    }
                    case 37 -> { player.closeInventory(); services.afk().teleportToLocation(player, "spawn"); }
                    case 38 -> { player.closeInventory(); services.afk().teleportToLocation(player, "pvp"); }
                    case 40 -> { player.closeInventory(); services.afk().teleportToLocation(player, "casino"); }
                    case 42 -> { player.closeInventory(); services.afk().teleportToLocation(player, "eventshop"); }
                    case 43 -> { player.closeInventory(); services.afk().teleportToLocation(player, "afk"); }
                }
            }
            case SELL_RESOURCES, SELL_FOOD -> {
                if (clicked.getType() == Material.ARROW) {
                    if (holder.type() == MenuType.SELL_RESOURCES) openSellFood(player);
                    else openSellResources(player);
                } else if (clicked.getType() == Material.BARRIER) {
                    openMain(player);
                } else {
                    long income = services.economy().sellItem(player, clicked.getType());
                    if (income > 0) {
                        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                        TextUtil.send(player, "&aПродано за &f" + income + " монет!");
                        player.updateInventory();
                        services.quests().checkProgress(player, 3, 1);
                    } else {
                        TextUtil.send(player, "&cУ вас нет этого предмета!");
                    }
                }
            }
            case ADMIN -> {
                switch (event.getSlot()) {
                    case 10 -> { player.closeInventory(); TextUtil.send(player, "&eИспользуйте: &f/admin event <тип> &7или &f/admin spawnevent <тип>"); }
                    case 11 -> openAdminItems(player);
                    case 12 -> { player.closeInventory(); TextUtil.send(player, "&eИспользуйте: &f/admin setcoins <игрок> <сумма>"); }
                    case 13 -> openAdminGuilds(player);
                    case 14 -> openPlayerList(player, "invsee");
                    case 15 -> { player.performCommand("admin reload"); player.closeInventory(); }
                    case 20 -> openPlayerList(player, "invsee");
                    case 21 -> openPlayerList(player, "freeze");
                    case 23 -> openPlayerList(player, "unfreeze");
                    case 24 -> { player.closeInventory(); TextUtil.send(player, "&7История действий пока в разработке."); }
                }
            }
            case ADMIN_GIVE_ITEMS -> {
                if (event.getSlot() == 49) {
                    openAdmin(player);
                } else if (event.getSlot() < 45) {
                    player.getInventory().addItem(clicked.clone());
                    TextUtil.send(player, "&a&l[!] &aВы выдали себе предмет!");
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
                }
            }
            case ADMIN_PLAYERS -> {
                if (event.getSlot() == 49) {
                    openAdmin(player);
                } else if (clicked.getType() == Material.PLAYER_HEAD) {
                    ItemMeta meta = clicked.getItemMeta();
                    if (meta != null && meta.hasDisplayName()) {
                        String targetName = PlainTextComponentSerializer.plainText().serialize(Objects.requireNonNull(meta.displayName()));
                        targetName = targetName.replace("[", "").replace("]", "").trim();
                        player.performCommand(holder.query() + " " + targetName);
                        player.closeInventory();
                    }
                }
            }
            case ITEMS -> {
                if (event.getSlot() == 49) {
                    openMain(player);
                } else if (event.getSlot() < 45) {
                    ItemMeta meta = clicked.getItemMeta();
                    if (meta != null) {
                        String id = meta.getPersistentDataContainer().get(
                                new org.bukkit.NamespacedKey("astrasmp", "custom_id"),
                                org.bukkit.persistence.PersistentDataType.STRING
                        );
                        if (id != null && getRecipeMatrix(id) != null) {
                            openRecipeView(player, clicked.clone(), id);
                            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                        }
                    }
                }
            }
            case RECIPE_VIEW -> {
                if (event.getSlot() == 40) {
                    openItems(player);
                }
            }
            case STATS, CONTRACTS -> {
                if (clicked.getType() == Material.ARROW || event.getSlot() == 49 || event.getSlot() == 22) openMain(player);
            }
            case AUCTION -> {
                if (event.getSlot() == 49) openMain(player);
                else if (event.getSlot() == 45 && holder.page() > 0) openAuction(player, holder.page() - 1, holder.query());
                else if (event.getSlot() == 53) openAuction(player, holder.page() + 1, holder.query());
                else if (event.getSlot() < 45) {
                    long id = extractLotId(clicked);
                    if (id > 0 && auction.buyLot(player, id)) {
                        TextUtil.send(player, "&aУспешная покупка!");
                        openAuction(player, holder.page(), holder.query());
                    }
                }
            }
            case GUILD -> {
                Guild guild = services.guilds().getPlayerGuild(player.getUniqueId());
                if (guild == null) return true;
                switch (event.getSlot()) {
                    case 10 -> openGuildMembers(player, guild);
                    case 11 -> services.gui().openGuildUpgrade(player, guild);
                    case 15 -> openGuildTreasury(player, guild);
                    case 16 -> openGuildRanksList(player, guild);
                    case 22 -> openMain(player);
                }
            }
            case GUILD_MEMBERS -> {
                Guild guild = services.guilds().getPlayerGuild(player.getUniqueId());
                if (guild == null) return true;
                if (event.getSlot() == 49) {
                    openGuildMain(player, guild);
                } else if (event.getSlot() < 45 && clicked.getType() == Material.PLAYER_HEAD) {
                    ItemMeta meta = clicked.getItemMeta();
                    if (meta != null && meta.getPersistentDataContainer().has(targetNameKey, PersistentDataType.STRING)) {
                        String targetName = meta.getPersistentDataContainer().get(targetNameKey, PersistentDataType.STRING);
                        if (targetName != null) {
                            if (event.isShiftClick() && event.isRightClick()) {
                                services.gui().openGuildMembers(player, guild);
                                player.closeInventory();
                            } else if (event.isLeftClick()) {
                                UUID targetUuid = Bukkit.getOfflinePlayer(targetName).getUniqueId();
                                services.guilds().promote(guild, targetUuid);
                                player.closeInventory();
                            } else if (event.isRightClick()) {
                                UUID targetUuid = Bukkit.getOfflinePlayer(targetName).getUniqueId();
                                services.guilds().demote(guild, targetUuid);
                                player.closeInventory();
                            }
                        }
                    }
                }
            }
            case GUILD_TREASURY -> { Guild guild = services.guilds().getPlayerGuild(player.getUniqueId());
                if (guild == null) return true;
                long amount = event.isShiftClick() ? 10_000L : 1_000L;
        
                if (event.getSlot() == 10) {
                    // Внести
                    long playerBal = services.economy().getBalance(player.getUniqueId());
                    if (playerBal < amount) {
                        TextUtil.send(player, "&cУ вас недостаточно монет! Нужно: &f" + amount + " ❂");
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                        return true;
                    }
                    services.economy().addBalance(player.getUniqueId(), player.getName(), -amount);
                    guild.setBalance(guild.getBalance() + amount);
                    services.guilds().saveGuildAsync(guild);
                    TextUtil.send(player, "&aВы внесли &f" + amount + " ❂ &aв казну гильдии.");
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
                    services.gui().openGuildTreasury(player, guild);
        
                } else if (event.getSlot() == 16) {
                    // Снять
                    if (!guild.hasPermission(player.getUniqueId(), "guild.bank")) {
                        TextUtil.send(player, "&cУ вас нет прав снимать деньги из казны!");
                        return true;
                    }
                    if (guild.getBalance() < amount) {
                        TextUtil.send(player, "&cВ казне недостаточно денег! Есть: &f" + guild.getBalance() + " ❂");
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                        return true;
                    }
                    guild.setBalance(guild.getBalance() - amount);
                    services.economy().addBalance(player.getUniqueId(), player.getName(), amount);
                    services.guilds().saveGuildAsync(guild);
                    TextUtil.send(player, "&aВы сняли &f" + amount + " ❂ &aиз казны.");
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 0.8f);
                    services.gui().openGuildTreasury(player, guild);
        
                } else if (event.getSlot() == 22) {
                    services.gui().openGuildMain(player, guild);
                }
                return true;
            }

            case GUILD_UPGRADE -> {
                Guild guild = services.guilds().getPlayerGuild(player.getUniqueId());
                if (event.getSlot() == 40 && guild != null) {
                    services.gui().openGuildMain(player, guild);
                }
                return true;
            }

            case ADMIN_GUILDS -> {
                if (event.getSlot() == 49) {
                    services.gui().openAdmin(player);
                    return true;
                }
                if (clicked.getType() != Material.WHITE_BANNER) return true;
                ItemMeta meta = clicked.getItemMeta();
                if (meta == null) return true;
        
                String guildIdStr = meta.getPersistentDataContainer().get(
                    new org.bukkit.NamespacedKey("astrasmp", "guild_id"),
                    PersistentDataType.STRING);
                if (guildIdStr == null) return true;
        
                Guild targetGuild = services.guilds().getGuilds().get(UUID.fromString(guildIdStr));
                if (targetGuild == null) return true;
        
                if (event.isShiftClick() && event.isRightClick()) {
                    // Роспуск гильдии из админ-панели
                    services.guilds().disbandGuild(player, targetGuild);
                    TextUtil.send(player, "&c[ADMIN] Гильдия &f" + targetGuild.getName() + " &cраспущена.");
                    services.gui().openAdminGuilds(player);
                } else {
                    // Показываем информацию о гильдии в чат
                    player.closeInventory();
                    TextUtil.send(player, "&b=== Гильдия: " + targetGuild.getName() + " ===");
                    TextUtil.send(player, "&7ID: &f" + targetGuild.getId());
                    TextUtil.send(player, "&7Уровень: &e" + targetGuild.getLevel()
                        + " &7| XP: &f" + targetGuild.getXp() + "/" + targetGuild.getXpForNextLevel());
                    TextUtil.send(player, "&7Казна: &a" + targetGuild.getBalance() + " ❂");
                    TextUtil.send(player, "&7Участников: &f" + targetGuild.getMembers().size());
                    TextUtil.send(player, "&7Команды: &8/admin guildlevel <id> <уровень>");
                }
                return true;
            }

            case GUILD_RANKS_LIST -> {
                Guild guild = services.guilds().getPlayerGuild(player.getUniqueId());
                if (guild == null) return true;
                if (event.getSlot() == 49) {
                    openGuildMain(player, guild);
                } else if (event.getSlot() == 53) {
                    player.closeInventory();
                    TextUtil.send(player, "&eВведите команду: /guild rank create <название>");
                } else if (event.getSlot() < 45 && clicked.getType() != Material.NETHER_STAR) {
                    ItemMeta meta = clicked.getItemMeta();
                    if (meta != null && meta.getPersistentDataContainer().has(rankIdKey, PersistentDataType.STRING)) {
                        String rankId = meta.getPersistentDataContainer().get(rankIdKey, PersistentDataType.STRING);
                        if (rankId != null) {
                            openRankSettings(player, guild, rankId);
                            return true;
                        }
                    }
                }
            }
            case GUILD_RANK_SETTINGS -> {
                Guild guild = services.guilds().getPlayerGuild(player.getUniqueId());
                if (guild == null) return true;
                String rankId = holder.metadata();
                switch (event.getSlot()) {
                    case 10 -> {
                        player.closeInventory();
                        TextUtil.send(player, "&eИспользуйте команду: /guild rank rename " + rankId + " <имя>");
                    }
                    case 12 -> openRankPermissions(player, guild, rankId);
                    case 14 -> {
                        player.closeInventory();
                        TextUtil.send(player, "&eИспользуйте команду: /guild rank setweight " + rankId + " <вес>");
                    }
                    case 16 -> {
                        if (event.isShiftClick() && event.isLeftClick()) {
                            player.performCommand("guild rank delete " + rankId);
                            player.closeInventory();
                        }
                    }
                    case 22 -> openGuildRanksList(player, guild);
                }
            }
            case GUILD_RANK_PERMISSIONS -> {
                Guild guild = services.guilds().getPlayerGuild(player.getUniqueId());
                if (guild == null) return true;
                String rankId = holder.metadata();
                if (event.getSlot() == 40) {
                    openRankSettings(player, guild, rankId);
                } else if (event.getSlot() >= 10 && event.getSlot() <= 15) {
                    ItemMeta meta = clicked.getItemMeta();
                    if (meta != null && meta.getPersistentDataContainer().has(permNodeKey, PersistentDataType.STRING)) {
                        String node = meta.getPersistentDataContainer().get(permNodeKey, PersistentDataType.STRING);
                        if (node != null) {
                            player.performCommand("guild rank toggleperm " + rankId + " " + node);
                            player.closeInventory();
                        }
                    }
                }
            }
            default -> {}
        }
        return true;
    }

    private long extractLotId(ItemStack stack) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return -1;
        try {
            String name = PlainTextComponentSerializer.plainText().serialize(Objects.requireNonNull(meta.displayName()));
            return Long.parseLong(name.replaceAll("[^0-9]", ""));
        } catch (Exception e) { return -1; }
    }

    private ItemStack createSellIcon(Material mat, double price) {
        ItemStack item = new ItemStack(mat);
        item.editMeta(meta -> {
            meta.lore(List.of(
                    Component.text(TextUtil.color("&7Цена: &a" + price + " монет/шт")),
                    Component.text(TextUtil.color("&eНажми, чтобы продать всё из инвентаря"))
            ));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        });
        return item;
    }
}