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
        MAIN, AUCTION, ITEMS, RECIPE_VIEW, STATS, CONTRACTS, ADMIN, SELL_RESOURCES, SELL_FOOD, SELL_DROPS,
        ADMIN_PLAYERS, GUILD_UPGRADE, ADMIN_GUILDS, ADMIN_GIVE_ITEMS, GUILD, GUILD_MEMBERS, GUILD_RANKS_LIST, GUILD_RANK_SETTINGS, GUILD_RANK_PERMISSIONS, GUILD_TREASURY, QUESTS, RITUAL_GUIDE, VOODOO_DOLL
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
        Inventory inv = Bukkit.createInventory(new MenuHolder(MenuType.MAIN, 0, "", ""), 54, title("§8✦ §0Меню Сервера §8✦"));
        fill(inv);

        // Декорации
        ItemStack decor = button(Material.BLACK_STAINED_GLASS_PANE, " ", "");
        int[] borders = {0,1,7,8,9,17,36,44,45,46,52,53};
        for (int b : borders) inv.setItem(b, decor);

        inv.setItem(4, button(Material.NETHER_STAR, "&e&lAstraSMP", "&7Главное меню сервера"));

        inv.setItem(19, button(Material.MINECART, "&aЕжедневные Награды", "&7Забирай призы каждый день!"));
        inv.setItem(20, services.quests().createQuestItem(player));
        inv.setItem(21, button(Material.ENCHANTED_BOOK, "&eДерево Талантов", "&7Прокачивай пассивные навыки!"));

        inv.setItem(23, button(Material.GOLD_INGOT, "&aСкупщик", "&7Продать ресурсы и еду", "&eНажми, чтобы открыть"));
        inv.setItem(24, button(Material.ANVIL, "&dАукцион", "&7Рынок предметов"));
        inv.setItem(25, button(Material.NETHERITE_SWORD, "&cПредметы", "&7Список всех уникальных вещей"));

        inv.setItem(29, button(Material.ENDER_EYE, "&bСтатистика", "&7Топ игроков и MMR"));
        inv.setItem(30, button(Material.BOOK, "&5Контракты", "&7Заказы на убийства"));
        inv.setItem(31, button(Material.WHITE_BANNER, "&6Моя Гильдия", "&7Управление кланом", "&eКлик: Открыть меню"));
        inv.setItem(33, button(Material.CRYING_OBSIDIAN, "&5Темная Магия", "&7Ритуалы, Контракты и Алтари", "&eКлик: Открыть гайд"));

        inv.setItem(48, build(Material.COMPASS, "&bСпавн", "&7Телепортация в\n&7безопасную зону"));
        inv.setItem(49, build(Material.DIAMOND_SWORD, "&cPvP Арена", "&7Сражайся с другими\n&7игроками!"));
        inv.setItem(50, build(Material.EMERALD, "&6Казино", "&7Испытай удачу\n&7в рулетке"));
        inv.setItem(51, build(Material.GOLDEN_APPLE, "&dИвент Шоп", "&7Уникальные вещи\n&7за Event Points"));
        inv.setItem(52, build(Material.CAMPFIRE, "&eAFK Зона", "&7Стой и получай\n&7по 5 ❂ в минуту"));

        player.openInventory(inv);
    }

    public void openRitualGuide(Player player) {
        Inventory inv = Bukkit.createInventory(new MenuHolder(MenuType.RITUAL_GUIDE, 0, "", ""), 54, title("§5✦ §0Гримуар Бездны §5✦"));
        fill(inv);

        // Декорации (Темно-фиолетовая и черная рамка)
        ItemStack blackDecor = button(Material.BLACK_STAINED_GLASS_PANE, " ", "");
        ItemStack purpleDecor = button(Material.PURPLE_STAINED_GLASS_PANE, " ", "");
        int[] blackBorders = {0,8,9,17,36,44,45,53};
        int[] purpleBorders = {1,7,46,52};
        for (int b : blackBorders) inv.setItem(b, blackDecor);
        for (int p : purpleBorders) inv.setItem(p, purpleDecor);

        // Основы
        inv.setItem(4, button(Material.CRYING_OBSIDIAN, "&5&lОсновы Темной Магии", 
                "&7Алтарь строится из Плачущего Обсидиана (центр).", 
                "&7Ступени 1-3 требуют Редстоун, Черепа и Незерит.",
                "",
                "&cКровь&7 добывается Ритуальным Кинжалом",
                "&7с игроков (шанс 10%)."));

        // Ритуалы (1-2 тиры)
        inv.setItem(20, button(Material.REDSTONE, "&cИзвлечение крови", "&7Ур: 1 | Жертва: Свинья | Скверна: +5", "&7Предмет: Гнилая Плоть"));
        inv.setItem(21, button(Material.DIAMOND, "&bДуша Демона", "&7Ур: 2 | Жертва: Зомби | Скверна: +15", "&7Предмет: Алмаз"));
        inv.setItem(22, button(Material.CAULDRON, "&4Кровавая Чаша", "&7Ур: 2 | Жертва: Ведьма | Скверна: +20", "&7Предметы: Котел, Золото, Капля крови", "", "&eПассивный сбор крови (15%)"));
        inv.setItem(23, button(Material.TOTEM_OF_UNDYING, "&5Кукла Вуду", "&7Ур: 2 | Жертва: Зомби | Скверна: +25", "&7Предметы: Флакон крови, Плоть, Нить", "", "&eНакладывает порчу на врага"));
        inv.setItem(24, button(Material.GOLDEN_APPLE, "&aОчищение Скверны", "&7Ур: 2 | Жертва: Житель | Скверна: -50", "&7Предметы: Яблоко, Слеза Гаста", "", "&eСнимает эффекты и очищает чанк"));

        // Ритуалы (3 тир)
        inv.setItem(29, button(Material.NETHERITE_SWORD, "&8Теневой Клинок", "&7Ур: 3 | Жертва: Игрок | Скверна: +50", "&7Предметы: Незерит. меч, Капля крови"));
        inv.setItem(30, button(Material.PAPER, "&4Демонический Контракт", "&7Ур: 3 | Жертва: Визер Скелет | Скверна: +100", "&7Предметы: Бумага, Душа Демона", "", "&c-2 Сердца навсегда, +Вампиризм"));
        inv.setItem(31, button(Material.ENDER_PEARL, "&3Врата Призыва", "&7Ур: 3 | Жертва: Эндермен | Скверна: +50", "&7Предметы: Флакон Крови, Жемчуг Края", "", "&eТелепортирует врага к вам"));
        inv.setItem(32, button(Material.ENDER_EYE, "&dСфера Безумия", "&7Ур: 3 | Жертва: Ведьма | Скверна: +50", "&7Предметы: Осколок Души, Душа Демона", "", "&eОдержимость врага на 10 мин."));

        // Ритуалы (Высшие)
        inv.setItem(39, button(Material.PHANTOM_MEMBRANE, "&bАстральная Проекция", "&7Ур: 3 | Жертва: Фантом | Скверна: +150", "&7Предметы: Осколок Души, Мембрана, Кровь", "", "&dВыход из тела на 2 минуты"));
        inv.setItem(41, button(Material.NETHER_STAR, "&0Семя Бездны", "&7Ур: 3 | Жертва: Визер Скелет | Скверна: +250", "&7Предметы: Звезда Незера, Осколок, Кровь", "", "&5Доступ в Карманное Измерение"));

        inv.setItem(49, button(Material.ARROW, "&cНазад в меню"));

        player.openInventory(inv);
    }

    public void openVoodooGui(Player player, String targetName, String targetUuid) {
        Inventory inv = Bukkit.createInventory(new MenuHolder(MenuType.VOODOO_DOLL, 0, targetUuid, targetName), 27, title("Кукла Вуду: " + targetName));
        fill(inv);

        ItemStack needle = button(Material.IRON_SWORD, "&cУкол Иглой", "&7Наносит 3 сердца магического", "&7урона, игнорируя броню.", "", "&eКлик: Использовать");
        ItemStack choke = button(Material.COBWEB, "&5Удушье", "&7Накладывает Отравление II", "&7и Замедление на 10 секунд.", "", "&eКлик: Использовать");
        ItemStack blind = button(Material.INK_SAC, "&8Слепота", "&7Погружает жертву в полную", "&7тьму на 15 секунд.", "", "&eКлик: Использовать");

        inv.setItem(11, needle);
        inv.setItem(13, choke);
        inv.setItem(15, blind);

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
        inv.setItem(35, button(Material.ARROW, "&eСледующая страница: Дроп"));
        inv.setItem(27, button(Material.ARROW, "&eНазад: Ресурсы"));
        inv.setItem(31, button(Material.BARRIER, "&cНазад в меню"));
        player.openInventory(inv);
    }

    public void openSellDrops(Player player) {
        Inventory inv = Bukkit.createInventory(new MenuHolder(MenuType.SELL_DROPS, 2, "", ""), 36, title("Скупщик: Дроп"));
        fill(inv);
        int slot = 0;
        for (Material mat : services.economy().getDropItems()) {
            double price = services.economy().getPrice(mat);
            if (price <= 0 || slot >= 27) continue;
            inv.setItem(slot++, createSellIcon(mat, price));
        }
        inv.setItem(27, button(Material.ARROW, "&eНазад: Еда"));
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

    public void openQuests(Player player) {
        Inventory inv = Bukkit.createInventory(new MenuHolder(MenuType.QUESTS, 0, "", ""), 36, title("Квесты и Ежедневки"));
        fill(inv);

        PlayerProfile profile = services.economy().profile(player.getUniqueId(), player.getName());

        int step = profile.getQuestStep();
        com.astrasmp.service.QuestManager.QuestData baseQ = services.quests().getBaseQuests().get(step);
        if (baseQ != null) {
            int current = profile.getQuestProgress();
            int max = baseQ.requiredAmount();
            inv.setItem(11, button(Material.WRITABLE_BOOK, "&b&lНачальный Квест", 
                "&7Шаг: &f" + step,
                "&7Цель: &e" + baseQ.name(),
                "&7Действие: &b" + services.quests().generateDescription(baseQ),
                "&7Прогресс: &a" + current + "&7/&a" + max,
                "",
                "&7Награда: &a" + baseQ.rewardInfo()
            ));
        } else {
            inv.setItem(11, button(Material.WRITTEN_BOOK, "&b&lНачальные Квесты", "&aВсе начальные задания выполнены!"));
        }

        int slot = 14;
        for (Map.Entry<String, Integer> entry : profile.getDailyQuests().entrySet()) {
            String qId = entry.getKey();
            int progress = entry.getValue();
            com.astrasmp.service.QuestManager.QuestData dailyQ = services.quests().getDailyQuestsPool().get(qId);
            if (dailyQ != null) {
                int max = dailyQ.requiredAmount();
                Material mat = progress >= max ? Material.EMERALD_BLOCK : Material.PAPER;
                String status = progress >= max ? "&a✔ Выполнено" : "&eВ процессе: &a" + progress + "&7/&a" + max;
                inv.setItem(slot++, button(mat, "&e&lЕжедневка", 
                    "&7Цель: &f" + dailyQ.name(),
                    "&7Действие: &b" + services.quests().generateDescription(dailyQ),
                    status,
                    "",
                    "&7Награда: &a" + dailyQ.rewardInfo()
                ));
            }
        }

        inv.setItem(31, button(Material.ARROW, "&cНазад в меню"));
        player.openInventory(inv);
    }

    public void openAdmin(Player player) {
        Inventory inv = Bukkit.createInventory(
            new MenuHolder(MenuType.ADMIN, 0, "", ""), 54,
            title("§4✦ §0Панель Админа §4✦"));
        fill(inv);
        
        ItemStack decor = button(Material.RED_STAINED_GLASS_PANE, " ", "");
        int[] borders = {0,1,7,8,9,17,36,44,45,46,52,53};
        for (int b : borders) inv.setItem(b, decor);

        inv.setItem(4, button(Material.COMMAND_BLOCK, "&c&lУправление Сервером", "&7Главная панель"));

        inv.setItem(20, button(Material.BEACON,  "&e⚡ Ивенты",          "&7Запуск глобальных ивентов", "&7/admin event <тип>"));
        inv.setItem(21, button(Material.CHEST,           "&6Выдача предметов",   "&7Меню выдачи кастомных вещей"));
        inv.setItem(22, button(Material.GOLD_BLOCK,      "&aЭкономика",          "&7Управление балансами игроков", "&7/admin setcoins <игрок> <сумма>"));
        
        inv.setItem(29, button(Material.WHITE_BANNER,    "&dГильдии",            "&7Управление гильдиями", "&7/admin guildadmin"));
        inv.setItem(30, button(Material.DIAMOND_SWORD,   "&cPVP / Арены",        "&7Управление PvP аренами", "&7/admin addduelspawn"));
        inv.setItem(31, button(Material.CRYING_OBSIDIAN, "&5Темная Магия",       "&7Ритуалы, Скверна и Луна", "&7(В разработке)"));
        inv.setItem(32, button(Material.PLAYER_HEAD,     "&bИгроки",             "&7Модерация: бан, мут, инвентарь", "&7/admin invsee"));
        inv.setItem(33, button(Material.COMPARATOR,      "&cСистемное",          "&7Перезагрузка конфигов", "&7/admin reload"));

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

    public void openAdminItems(Player player, String category) {
        if (category == null || category.isEmpty()) category = "Броня";
        Inventory inv = Bukkit.createInventory(new MenuHolder(MenuType.ADMIN_GIVE_ITEMS, 0, category, ""), 54, title("Выдача: " + category));
        fill(inv);

        if (category.equals("Броня")) {
            inv.setItem(10, ItemRegistry.mercenaryHelmet());
            inv.setItem(11, ItemRegistry.berserkerHelmet());
            inv.setItem(12, ItemRegistry.inquisitorHelmet());
            inv.setItem(13, ItemRegistry.juggernautHelmet());
            inv.setItem(14, ItemRegistry.minerHelmet());
            inv.setItem(15, ItemRegistry.bloodHunterHelmet());

            inv.setItem(19, ItemRegistry.mercenaryChestplate());
            inv.setItem(20, ItemRegistry.berserkerChestplate());
            inv.setItem(21, ItemRegistry.inquisitorChestplate());
            inv.setItem(22, ItemRegistry.juggernautChestplate());
            inv.setItem(23, ItemRegistry.minerChestplate());
            inv.setItem(24, ItemRegistry.bloodHunterChestplate());

            inv.setItem(28, ItemRegistry.mercenaryLeggings());
            inv.setItem(29, ItemRegistry.berserkerLeggings());
            inv.setItem(30, ItemRegistry.inquisitorLeggings());
            inv.setItem(31, ItemRegistry.juggernautLeggings());
            inv.setItem(32, ItemRegistry.minerLeggings());
            inv.setItem(33, ItemRegistry.bloodHunterLeggings());

            inv.setItem(37, ItemRegistry.mercenaryBoots());
            inv.setItem(38, ItemRegistry.berserkerBoots());
            inv.setItem(39, ItemRegistry.inquisitorBoots());
            inv.setItem(40, ItemRegistry.juggernautBoots());
            inv.setItem(41, ItemRegistry.minerBoots());
            inv.setItem(42, ItemRegistry.bloodHunterBoots());
        } else if (category.equals("Оружие")) {
            inv.setItem(10, ItemRegistry.shadowBlade());
            inv.setItem(11, ItemRegistry.infernoSword());
            inv.setItem(12, ItemRegistry.vampireDagger());
            
            inv.setItem(19, ItemRegistry.frostAxe());
            inv.setItem(20, ItemRegistry.thunderHammer());
            
            inv.setItem(28, ItemRegistry.venomBow());
            inv.setItem(29, ItemRegistry.reaperScythe());
        } else if (category.equals("Инструменты")) {
            inv.setItem(10, ItemRegistry.mining3x3());
            inv.setItem(11, ItemRegistry.mining5x5());
            inv.setItem(12, ItemRegistry.veinMiner());
            inv.setItem(13, ItemRegistry.autoSmelt());
            inv.setItem(14, ItemRegistry.magnet());

            inv.setItem(19, ItemRegistry.mining3x3Netherite());
            inv.setItem(20, ItemRegistry.mining5x5Netherite());
            inv.setItem(21, ItemRegistry.veinMinerNetherite());
            inv.setItem(22, ItemRegistry.autoSmeltNetherite());
            inv.setItem(23, ItemRegistry.magnetNetherite());
        } else if (category.equals("ТоТемы")) {
            inv.setItem(10, ItemRegistry.totemSpeed());
            inv.setItem(11, ItemRegistry.totemShield());
            inv.setItem(12, ItemRegistry.totemLightning());
            inv.setItem(13, ItemRegistry.totemExplosion());
            inv.setItem(14, ItemRegistry.totemTeleport());
        } else if (category.equals("МЭ Сеть")) {
            inv.setItem(10, ItemRegistry.meTerminal());
            inv.setItem(11, ItemRegistry.meDrive());
            inv.setItem(12, ItemRegistry.meController());
            inv.setItem(19, ItemRegistry.meCell4k());
            inv.setItem(20, ItemRegistry.meCell16k());
            inv.setItem(21, ItemRegistry.meCell64k());
        } else if (category.equals("Разное")) {
            inv.setItem(10, ItemRegistry.guildHeart());
            inv.setItem(11, ItemRegistry.trampoline());
            inv.setItem(12, ItemRegistry.soulOfNanda());
            
            inv.setItem(19, ItemRegistry.trophy("common", org.bukkit.Material.PAPER, "§fОбычный трофей", "Обычный"));
            inv.setItem(20, ItemRegistry.trophy("legendary", org.bukkit.Material.NETHER_STAR, "§6Легендарный трофей", "Легендарный"));
            inv.setItem(28, ItemRegistry.relic("time_core", org.bukkit.Material.CLOCK, "§dЯдро времени", "Замедляет время вокруг владельца."));
            inv.setItem(29, ItemRegistry.relic("void_fragment", org.bukkit.Material.AMETHYST_SHARD, "§5Фрагмент пустоты", "Рывок через пустоту."));
            inv.setItem(30, ItemRegistry.artifact("heart_of_world", org.bukkit.Material.HEART_OF_THE_SEA, "§bСердце мира", "Пассивная защита для носителя."));
        } else if (category.equals("Ритуалы")) {
            inv.setItem(10, ItemRegistry.sacrificialDagger());
            inv.setItem(11, ItemRegistry.bloodDrop());
            inv.setItem(12, ItemRegistry.demonSoul());
            inv.setItem(13, ItemRegistry.demonicPact());
            inv.setItem(14, ItemRegistry.bloodCauldron());
            inv.setItem(15, ItemRegistry.ritualAltar());
            inv.setItem(16, ItemRegistry.cleansingTotem());
            inv.setItem(19, ItemRegistry.bloodChalice());
            inv.setItem(20, ItemRegistry.bloodVial());
            inv.setItem(21, ItemRegistry.voodooDoll());
            inv.setItem(22, ItemRegistry.soulFragment());
            inv.setItem(23, ItemRegistry.madnessSphere());
            inv.setItem(24, ItemRegistry.seedOfAbyss());
            inv.setItem(25, ItemRegistry.astralCrystal());
        }

        inv.setItem(46, button(Material.NETHERITE_SWORD, "&aОружие", "&7Клик для фильтра"));
        inv.setItem(47, button(Material.NETHERITE_CHESTPLATE, "&bБроня", "&7Клик для фильтра"));
        inv.setItem(48, button(Material.DIAMOND_PICKAXE, "&eИнструменты", "&7Клик для фильтра"));
        inv.setItem(49, button(Material.BARRIER, "&cНазад в админ-панель"));
        inv.setItem(50, button(Material.TOTEM_OF_UNDYING, "&6ТоТемы", "&7Клик для фильтра"));
        inv.setItem(51, button(Material.LODESTONE, "&dМЭ Сеть", "&7Клик для фильтра"));
        inv.setItem(52, button(Material.NETHER_STAR, "&dРазное", "&7Клик для фильтра"));
        inv.setItem(53, button(Material.CRYING_OBSIDIAN, "&5Ритуалы", "&7Клик для фильтра"));

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
            case "mining5x5" -> { return makeRecipe(Material.NETHERITE_BLOCK, Material.NETHERITE_INGOT, Material.NETHERITE_BLOCK, Material.NETHERITE_INGOT, Material.NETHERITE_PICKAXE, Material.NETHERITE_INGOT, Material.NETHERITE_BLOCK, Material.NETHERITE_INGOT, Material.NETHERITE_BLOCK); }
            case "veinMiner" -> { return makeRecipe(Material.AMETHYST_CLUSTER, Material.AMETHYST_CLUSTER, Material.AMETHYST_CLUSTER, Material.AMETHYST_CLUSTER, Material.NETHERITE_PICKAXE, Material.AMETHYST_CLUSTER, Material.AMETHYST_CLUSTER, Material.AMETHYST_CLUSTER, Material.AMETHYST_CLUSTER); }
            case "autoSmelt" -> { return makeRecipe(Material.MAGMA_BLOCK, Material.MAGMA_BLOCK, Material.MAGMA_BLOCK, Material.MAGMA_BLOCK, Material.NETHERITE_PICKAXE, Material.MAGMA_BLOCK, Material.MAGMA_BLOCK, Material.MAGMA_BLOCK, Material.MAGMA_BLOCK); }
            case "magnet" -> { return makeRecipe(Material.GOLD_INGOT, Material.IRON_INGOT, Material.GOLD_INGOT, Material.IRON_INGOT, Material.COMPASS, Material.IRON_INGOT, Material.GOLD_INGOT, Material.IRON_INGOT, Material.GOLD_INGOT); }
            
            case "shadowBlade" -> { return makeRecipe(Material.ECHO_SHARD, Material.COAL_BLOCK, Material.ECHO_SHARD, Material.ECHO_SHARD, Material.NETHERITE_SWORD, Material.ECHO_SHARD, Material.ECHO_SHARD, Material.COAL_BLOCK, Material.ECHO_SHARD); }
            case "thunderHammer" -> { return makeRecipe(Material.COPPER_BLOCK, Material.LIGHTNING_ROD, Material.COPPER_BLOCK, Material.COPPER_BLOCK, Material.NETHERITE_AXE, Material.COPPER_BLOCK, Material.COPPER_BLOCK, Material.COPPER_BLOCK, Material.COPPER_BLOCK); }
            case "vampireDagger" -> { return makeRecipe(Material.REDSTONE_BLOCK, Material.GHAST_TEAR, Material.REDSTONE_BLOCK, Material.REDSTONE_BLOCK, Material.DIAMOND_SWORD, Material.REDSTONE_BLOCK, Material.REDSTONE_BLOCK, Material.GHAST_TEAR, Material.REDSTONE_BLOCK); }
            case "infernoSword" -> { return makeRecipe(Material.BLAZE_ROD, Material.MAGMA_CREAM, Material.BLAZE_ROD, Material.BLAZE_ROD, Material.NETHERITE_SWORD, Material.BLAZE_ROD, Material.BLAZE_ROD, Material.MAGMA_CREAM, Material.BLAZE_ROD); }
            case "frostAxe" -> { return makeRecipe(Material.PACKED_ICE, Material.PACKED_ICE, Material.PACKED_ICE, Material.PACKED_ICE, Material.DIAMOND_AXE, Material.PACKED_ICE, Material.PACKED_ICE, Material.PACKED_ICE, Material.PACKED_ICE); }
            case "venomBow" -> { return makeRecipe(AIR, Material.SPIDER_EYE, AIR, Material.SPIDER_EYE, Material.BOW, Material.SPIDER_EYE, AIR, Material.SPIDER_EYE, AIR); }
            case "reaperScythe" -> { return makeRecipe(Material.NETHERITE_BLOCK, Material.NETHERITE_BLOCK, Material.NETHERITE_BLOCK, AIR, Material.NETHERITE_HOE, AIR, AIR, Material.NETHERITE_BLOCK, AIR); }

            case "mercenary_helmet" -> { return makeRecipe(Material.COAL_BLOCK, Material.LEATHER, Material.COAL_BLOCK, Material.COAL_BLOCK, AIR, Material.COAL_BLOCK, AIR, AIR, AIR); }
            case "mercenary_chestplate" -> { return makeRecipe(Material.COAL_BLOCK, AIR, Material.COAL_BLOCK, Material.COAL_BLOCK, Material.LEATHER, Material.COAL_BLOCK, Material.COAL_BLOCK, Material.COAL_BLOCK, Material.COAL_BLOCK); }
            case "mercenary_leggings" -> { return makeRecipe(Material.COAL_BLOCK, Material.COAL_BLOCK, Material.COAL_BLOCK, Material.LEATHER, AIR, Material.LEATHER, Material.COAL_BLOCK, AIR, Material.COAL_BLOCK); }
            case "mercenary_boots" -> { return makeRecipe(AIR, AIR, AIR, Material.COAL_BLOCK, AIR, Material.COAL_BLOCK, Material.LEATHER, AIR, Material.LEATHER); }

            case "berserker_helmet" -> { return makeRecipe(Material.IRON_BLOCK, Material.IRON_BLOCK, Material.IRON_BLOCK, Material.REDSTONE_BLOCK, AIR, Material.REDSTONE_BLOCK, AIR, AIR, AIR); }
            case "berserker_chestplate" -> { return makeRecipe(Material.IRON_BLOCK, AIR, Material.IRON_BLOCK, Material.IRON_BLOCK, Material.REDSTONE_BLOCK, Material.IRON_BLOCK, Material.IRON_BLOCK, Material.IRON_BLOCK, Material.IRON_BLOCK); }
            case "berserker_leggings" -> { return makeRecipe(Material.IRON_BLOCK, Material.IRON_BLOCK, Material.IRON_BLOCK, Material.REDSTONE_BLOCK, AIR, Material.REDSTONE_BLOCK, Material.IRON_BLOCK, AIR, Material.IRON_BLOCK); }
            case "berserker_boots" -> { return makeRecipe(AIR, AIR, AIR, Material.REDSTONE_BLOCK, AIR, Material.REDSTONE_BLOCK, Material.IRON_BLOCK, AIR, Material.IRON_BLOCK); }

            case "inquisitor_helmet" -> { return makeRecipe(Material.GOLD_BLOCK, Material.GOLD_BLOCK, Material.GOLD_BLOCK, Material.IRON_BLOCK, AIR, Material.IRON_BLOCK, AIR, AIR, AIR); }
            case "inquisitor_chestplate" -> { return makeRecipe(Material.GOLD_BLOCK, AIR, Material.GOLD_BLOCK, Material.GOLD_BLOCK, Material.IRON_BLOCK, Material.GOLD_BLOCK, Material.GOLD_BLOCK, Material.GOLD_BLOCK, Material.GOLD_BLOCK); }
            case "inquisitor_leggings" -> { return makeRecipe(Material.GOLD_BLOCK, Material.GOLD_BLOCK, Material.GOLD_BLOCK, Material.IRON_BLOCK, AIR, Material.IRON_BLOCK, Material.GOLD_BLOCK, AIR, Material.GOLD_BLOCK); }
            case "inquisitor_boots" -> { return makeRecipe(AIR, AIR, AIR, Material.IRON_BLOCK, AIR, Material.IRON_BLOCK, Material.GOLD_BLOCK, AIR, Material.GOLD_BLOCK); }

            case "juggernaut_helmet" -> { return makeRecipe(Material.OBSIDIAN, Material.OBSIDIAN, Material.OBSIDIAN, Material.NETHERITE_INGOT, AIR, Material.NETHERITE_INGOT, AIR, AIR, AIR); }
            case "juggernaut_chestplate" -> { return makeRecipe(Material.OBSIDIAN, AIR, Material.OBSIDIAN, Material.OBSIDIAN, Material.NETHERITE_INGOT, Material.OBSIDIAN, Material.OBSIDIAN, Material.OBSIDIAN, Material.OBSIDIAN); }
            case "juggernaut_leggings" -> { return makeRecipe(Material.OBSIDIAN, Material.OBSIDIAN, Material.OBSIDIAN, Material.NETHERITE_INGOT, AIR, Material.NETHERITE_INGOT, Material.OBSIDIAN, AIR, Material.OBSIDIAN); }
            case "juggernaut_boots" -> { return makeRecipe(AIR, AIR, AIR, Material.NETHERITE_INGOT, AIR, Material.NETHERITE_INGOT, Material.OBSIDIAN, AIR, Material.OBSIDIAN); }

            case "miner_helmet" -> { return makeRecipe(Material.DIAMOND_BLOCK, Material.DIAMOND_BLOCK, Material.DIAMOND_BLOCK, Material.EMERALD, AIR, Material.EMERALD, AIR, AIR, AIR); }
            case "miner_chestplate" -> { return makeRecipe(Material.DIAMOND_BLOCK, AIR, Material.DIAMOND_BLOCK, Material.DIAMOND_BLOCK, Material.EMERALD, Material.DIAMOND_BLOCK, Material.DIAMOND_BLOCK, Material.DIAMOND_BLOCK, Material.DIAMOND_BLOCK); }
            case "miner_leggings" -> { return makeRecipe(Material.DIAMOND_BLOCK, Material.DIAMOND_BLOCK, Material.DIAMOND_BLOCK, Material.EMERALD, AIR, Material.EMERALD, Material.DIAMOND_BLOCK, AIR, Material.DIAMOND_BLOCK); }
            case "miner_boots" -> { return makeRecipe(AIR, AIR, AIR, Material.EMERALD, AIR, Material.EMERALD, Material.DIAMOND_BLOCK, AIR, Material.DIAMOND_BLOCK); }

            case "bloodhunter_helmet" -> { return makeRecipe(Material.REDSTONE_BLOCK, Material.REDSTONE_BLOCK, Material.REDSTONE_BLOCK, Material.DIAMOND_BLOCK, AIR, Material.DIAMOND_BLOCK, AIR, AIR, AIR); }
            case "bloodhunter_chestplate" -> { return makeRecipe(Material.REDSTONE_BLOCK, AIR, Material.REDSTONE_BLOCK, Material.REDSTONE_BLOCK, Material.DIAMOND_BLOCK, Material.REDSTONE_BLOCK, Material.REDSTONE_BLOCK, Material.REDSTONE_BLOCK, Material.REDSTONE_BLOCK); }
            case "bloodhunter_leggings" -> { return makeRecipe(Material.REDSTONE_BLOCK, Material.REDSTONE_BLOCK, Material.REDSTONE_BLOCK, Material.DIAMOND_BLOCK, AIR, Material.DIAMOND_BLOCK, Material.REDSTONE_BLOCK, AIR, Material.REDSTONE_BLOCK); }
            case "bloodhunter_boots" -> { return makeRecipe(AIR, AIR, AIR, Material.DIAMOND_BLOCK, AIR, Material.DIAMOND_BLOCK, Material.REDSTONE_BLOCK, AIR, Material.REDSTONE_BLOCK); }

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

    public void openItems(Player player, String category) {
        if (category == null || category.isEmpty()) category = "Броня";
        Inventory inv = Bukkit.createInventory(new MenuHolder(MenuType.ITEMS, 0, category, ""), 54, title("Предметы: " + category));
        fill(inv);

        if (category.equals("Броня")) {
            inv.setItem(10, withLoreHint(ItemRegistry.mercenaryHelmet()));
            inv.setItem(11, withLoreHint(ItemRegistry.berserkerHelmet()));
            inv.setItem(12, withLoreHint(ItemRegistry.inquisitorHelmet()));
            inv.setItem(13, withLoreHint(ItemRegistry.juggernautHelmet()));
            inv.setItem(14, withLoreHint(ItemRegistry.minerHelmet()));
            inv.setItem(15, withLoreHint(ItemRegistry.bloodHunterHelmet()));

            inv.setItem(19, withLoreHint(ItemRegistry.mercenaryChestplate()));
            inv.setItem(20, withLoreHint(ItemRegistry.berserkerChestplate()));
            inv.setItem(21, withLoreHint(ItemRegistry.inquisitorChestplate()));
            inv.setItem(22, withLoreHint(ItemRegistry.juggernautChestplate()));
            inv.setItem(23, withLoreHint(ItemRegistry.minerChestplate()));
            inv.setItem(24, withLoreHint(ItemRegistry.bloodHunterChestplate()));

            inv.setItem(28, withLoreHint(ItemRegistry.mercenaryLeggings()));
            inv.setItem(29, withLoreHint(ItemRegistry.berserkerLeggings()));
            inv.setItem(30, withLoreHint(ItemRegistry.inquisitorLeggings()));
            inv.setItem(31, withLoreHint(ItemRegistry.juggernautLeggings()));
            inv.setItem(32, withLoreHint(ItemRegistry.minerLeggings()));
            inv.setItem(33, withLoreHint(ItemRegistry.bloodHunterLeggings()));

            inv.setItem(37, withLoreHint(ItemRegistry.mercenaryBoots()));
            inv.setItem(38, withLoreHint(ItemRegistry.berserkerBoots()));
            inv.setItem(39, withLoreHint(ItemRegistry.inquisitorBoots()));
            inv.setItem(40, withLoreHint(ItemRegistry.juggernautBoots()));
            inv.setItem(41, withLoreHint(ItemRegistry.minerBoots()));
            inv.setItem(42, withLoreHint(ItemRegistry.bloodHunterBoots()));
        } else if (category.equals("Оружие")) {
            inv.setItem(10, withLoreHint(ItemRegistry.shadowBlade()));
            inv.setItem(11, withLoreHint(ItemRegistry.infernoSword()));
            inv.setItem(12, withLoreHint(ItemRegistry.vampireDagger()));
            
            inv.setItem(19, withLoreHint(ItemRegistry.frostAxe()));
            inv.setItem(20, withLoreHint(ItemRegistry.thunderHammer()));
            
            inv.setItem(28, withLoreHint(ItemRegistry.venomBow()));
            inv.setItem(29, withLoreHint(ItemRegistry.reaperScythe()));
        } else if (category.equals("Инструменты")) {
            inv.setItem(10, withLoreHint(ItemRegistry.mining3x3()));
            inv.setItem(11, withLoreHint(ItemRegistry.mining5x5()));
            inv.setItem(12, withLoreHint(ItemRegistry.veinMiner()));
            inv.setItem(13, withLoreHint(ItemRegistry.autoSmelt()));
            inv.setItem(14, withLoreHint(ItemRegistry.magnet()));

            inv.setItem(19, withLoreHint(ItemRegistry.mining3x3Netherite()));
            inv.setItem(20, withLoreHint(ItemRegistry.mining5x5Netherite()));
            inv.setItem(21, withLoreHint(ItemRegistry.veinMinerNetherite()));
            inv.setItem(22, withLoreHint(ItemRegistry.autoSmeltNetherite()));
            inv.setItem(23, withLoreHint(ItemRegistry.magnetNetherite()));
        } else if (category.equals("ТоТемы")) {
            inv.setItem(10, withLoreHint(ItemRegistry.totemSpeed()));
            inv.setItem(11, withLoreHint(ItemRegistry.totemShield()));
            inv.setItem(12, withLoreHint(ItemRegistry.totemLightning()));
            inv.setItem(13, withLoreHint(ItemRegistry.totemExplosion()));
            inv.setItem(14, withLoreHint(ItemRegistry.totemTeleport()));
        } else if (category.equals("МЭ Сеть")) {
            inv.setItem(10, withLoreHint(ItemRegistry.meTerminal()));
            inv.setItem(11, withLoreHint(ItemRegistry.meDrive()));
            inv.setItem(12, withLoreHint(ItemRegistry.meController()));
            inv.setItem(19, withLoreHint(ItemRegistry.meCell4k()));
            inv.setItem(20, withLoreHint(ItemRegistry.meCell16k()));
            inv.setItem(21, withLoreHint(ItemRegistry.meCell64k()));
        } else if (category.equals("Разное")) {
            inv.setItem(10, withLoreHint(ItemRegistry.guildHeart()));
            inv.setItem(11, withLoreHint(ItemRegistry.trampoline()));
            inv.setItem(12, withLoreHint(ItemRegistry.soulOfNanda()));
            
            inv.setItem(19, withLoreHint(ItemRegistry.trophy("common", org.bukkit.Material.PAPER, "§fОбычный трофей", "Обычный")));
            inv.setItem(20, withLoreHint(ItemRegistry.trophy("legendary", org.bukkit.Material.NETHER_STAR, "§6Легендарный трофей", "Легендарный")));
            inv.setItem(28, withLoreHint(ItemRegistry.relic("time_core", org.bukkit.Material.CLOCK, "§dЯдро времени", "Замедляет время вокруг владельца.")));
            inv.setItem(29, withLoreHint(ItemRegistry.relic("void_fragment", org.bukkit.Material.AMETHYST_SHARD, "§5Фрагмент пустоты", "Рывок через пустоту.")));
            inv.setItem(30, withLoreHint(ItemRegistry.artifact("heart_of_world", org.bukkit.Material.HEART_OF_THE_SEA, "§bСердце мира", "Пассивная защита для носителя.")));
        } else if (category.equals("Ритуалы")) {
            inv.setItem(10, withLoreHint(ItemRegistry.sacrificialDagger()));
            inv.setItem(11, withLoreHint(ItemRegistry.bloodDrop()));
            inv.setItem(12, withLoreHint(ItemRegistry.demonSoul()));
            inv.setItem(13, withLoreHint(ItemRegistry.demonicPact()));
            inv.setItem(14, withLoreHint(ItemRegistry.bloodCauldron()));
            inv.setItem(15, withLoreHint(ItemRegistry.ritualAltar()));
            inv.setItem(16, withLoreHint(ItemRegistry.cleansingTotem()));
            inv.setItem(19, withLoreHint(ItemRegistry.bloodChalice()));
            inv.setItem(20, withLoreHint(ItemRegistry.bloodVial()));
            inv.setItem(21, withLoreHint(ItemRegistry.voodooDoll()));
            inv.setItem(22, withLoreHint(ItemRegistry.soulFragment()));
            inv.setItem(23, withLoreHint(ItemRegistry.madnessSphere()));
            inv.setItem(24, withLoreHint(ItemRegistry.seedOfAbyss()));
            inv.setItem(25, withLoreHint(ItemRegistry.astralCrystal()));
        }

        inv.setItem(46, button(Material.NETHERITE_SWORD, "&aОружие", "&7Клик для просмотра"));
        inv.setItem(47, button(Material.NETHERITE_CHESTPLATE, "&bБроня", "&7Клик для просмотра"));
        inv.setItem(48, button(Material.DIAMOND_PICKAXE, "&eИнструменты", "&7Клик для просмотра"));
        inv.setItem(49, button(Material.BARRIER, "&cНазад в меню"));
        inv.setItem(50, button(Material.TOTEM_OF_UNDYING, "&6ТоТемы", "&7Клик для просмотра"));
        inv.setItem(51, button(Material.LODESTONE, "&dМЭ Сеть", "&7Клик для просмотра"));
        inv.setItem(52, button(Material.NETHER_STAR, "&dРазное", "&7Клик для просмотра"));
        inv.setItem(53, button(Material.CRYING_OBSIDIAN, "&5Ритуалы", "&7Клик для просмотра"));

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
                    case 19 -> Bukkit.getScheduler().runTask(plugin, () -> { player.closeInventory(); player.performCommand("rewards"); });
                    case 20 -> Bukkit.getScheduler().runTask(plugin, () -> { player.closeInventory(); player.performCommand("quest"); });
                    case 21 -> Bukkit.getScheduler().runTask(plugin, () -> { player.closeInventory(); player.performCommand("talents"); });
                    
                    case 23 -> Bukkit.getScheduler().runTask(plugin, () -> openSellResources(player));
                    case 24 -> Bukkit.getScheduler().runTask(plugin, () -> openAuction(player, 0, ""));
                    case 25 -> Bukkit.getScheduler().runTask(plugin, () -> openItems(player, "Броня"));
                    
                    case 29 -> Bukkit.getScheduler().runTask(plugin, () -> openStatsMenu(player));
                    case 30 -> Bukkit.getScheduler().runTask(plugin, () -> { player.closeInventory(); player.performCommand("bounty"); });
                    case 31 -> Bukkit.getScheduler().runTask(plugin, () -> {
                        Guild guild = services.guilds().getPlayerGuild(player.getUniqueId());
                        if (guild != null) openGuildMain(player, guild);
                        else TextUtil.send(player, "&cУ вас нет гильдии! Создайте её: /guild create <название>");
                    });
                    case 33 -> Bukkit.getScheduler().runTask(plugin, () -> openRitualGuide(player));
                    
                    case 48 -> Bukkit.getScheduler().runTask(plugin, () -> { player.closeInventory(); services.afk().teleportToLocation(player, "spawn"); });
                    case 49 -> Bukkit.getScheduler().runTask(plugin, () -> { player.closeInventory(); services.afk().teleportToLocation(player, "pvp"); });
                    case 50 -> Bukkit.getScheduler().runTask(plugin, () -> { player.closeInventory(); services.afk().teleportToLocation(player, "casino"); });
                    case 51 -> Bukkit.getScheduler().runTask(plugin, () -> { player.closeInventory(); services.afk().teleportToLocation(player, "eventshop"); });
                    case 52 -> Bukkit.getScheduler().runTask(plugin, () -> { player.closeInventory(); services.afk().teleportToLocation(player, "afk"); });
                }
            }
            case RITUAL_GUIDE -> {
                if (event.getSlot() == 49) Bukkit.getScheduler().runTask(plugin, () -> openMain(player));
            }
            case SELL_RESOURCES, SELL_FOOD, SELL_DROPS -> {
                if (clicked.getType() == Material.ARROW) {
                    if (event.getSlot() == 35) {
                        if (holder.type() == MenuType.SELL_RESOURCES) openSellFood(player);
                        else if (holder.type() == MenuType.SELL_FOOD) openSellDrops(player);
                    } else if (event.getSlot() == 27) {
                        if (holder.type() == MenuType.SELL_FOOD) openSellResources(player);
                        else if (holder.type() == MenuType.SELL_DROPS) openSellFood(player);
                    }
                } else if (clicked.getType() == Material.BARRIER) {
                    openMain(player);
                } else {
                    long income = services.economy().sellItem(player, clicked.getType());
                    if (income > 0) {
                        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                        TextUtil.send(player, "&aПродано за &f" + income + " монет!");
                        player.updateInventory();
                        services.quests().processAction(player, com.astrasmp.service.QuestManager.QuestAction.SELL_ITEM, "", 1);
                    } else {
                        TextUtil.send(player, "&cУ вас нет этого предмета!");
                    }
                }
            }
            case ITEMS -> {
                if (event.getSlot() == 49) {
                    openMain(player);
                } else if (event.getSlot() == 46) {
                    openItems(player, "Оружие");
                } else if (event.getSlot() == 47) {
                    openItems(player, "Броня");
                } else if (event.getSlot() == 48) {
                    openItems(player, "Инструменты");
                } else if (event.getSlot() == 50) {
                    openItems(player, "ТоТемы");
                } else if (event.getSlot() == 51) {
                    openItems(player, "МЭ Сеть");
                } else if (event.getSlot() == 52) {
                    openItems(player, "Разное");
                } else if (event.getSlot() == 53) {
                    openItems(player, "Ритуалы");
                } else if (event.getSlot() < 45) {
                    String customId = ItemRegistry.id(clicked);
                    if (customId != null && getRecipeMatrix(customId) != null) {
                        openRecipeView(player, clicked.clone(), customId);
                    }
                }
            }
            case ADMIN -> {
                switch (event.getSlot()) {
                    case 20 -> { player.closeInventory(); TextUtil.send(player, "&eИспользуйте: &f/admin event <тип> &7или &f/admin spawnevent <тип>"); }
                    case 21 -> openAdminItems(player, "Броня");
                    case 22 -> { player.closeInventory(); TextUtil.send(player, "&eИспользуйте: &f/admin setcoins <игрок> <сумма>"); }
                    case 29 -> openAdminGuilds(player);
                    case 30 -> { player.closeInventory(); TextUtil.send(player, "&eИспользуйте: &f/admin addduelspawn &7или &f/admin sethub"); }
                    case 31 -> { player.closeInventory(); TextUtil.send(player, "&5Управление темной магией в разработке."); }
                    case 32 -> openPlayerList(player, "invsee");
                    case 33 -> { player.performCommand("admin reload"); player.closeInventory(); }
                }
            }
            case ADMIN_GIVE_ITEMS -> {
                if (event.getSlot() == 49) {
                    openAdmin(player);
                } else if (event.getSlot() == 46) {
                    openAdminItems(player, "Оружие");
                } else if (event.getSlot() == 47) {
                    openAdminItems(player, "Броня");
                } else if (event.getSlot() == 48) {
                    openAdminItems(player, "Инструменты");
                } else if (event.getSlot() == 50) {
                    openAdminItems(player, "ТоТемы");
                } else if (event.getSlot() == 51) {
                    openAdminItems(player, "МЭ Сеть");
                } else if (event.getSlot() == 52) {
                    openAdminItems(player, "Разное");
                } else if (event.getSlot() == 53) {
                    openAdminItems(player, "Ритуалы");
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
            case RECIPE_VIEW -> {
                if (event.getSlot() == 40) {
                    openItems(player, "Броня");
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
            case VOODOO_DOLL -> {
                String targetUuid = holder.query();
                String targetName = holder.metadata();
                org.bukkit.entity.Player target = org.bukkit.Bukkit.getPlayer(java.util.UUID.fromString(targetUuid));
                
                if (target == null || !target.isOnline()) {
                    player.sendMessage("§cИгрок " + targetName + " вне сети или недоступен.");
                    player.closeInventory();
                    return true;
                }

                if (event.getSlot() == 11) { // Укол
                    target.damage(8.5); // +40% (было 6.0)
                    target.sendMessage("§4[Вуду] §cНевидимая игла пронзила вашу плоть!");
                    player.sendMessage("§4[Вуду] §cВы пронзили " + targetName + " иглой.");
                } else if (event.getSlot() == 13) { // Удушье
                    target.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.POISON, 280, 1)); // +40% (было 200)
                    target.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 280, 0));
                    target.sendMessage("§5[Вуду] §dНевидимая рука сжала ваше горло!");
                    player.sendMessage("§5[Вуду] §dВы начали душить " + targetName + ".");
                } else if (event.getSlot() == 15) { // Слепота
                    target.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS, 420, 0)); // +40% (было 300)
                    target.sendMessage("§8[Вуду] §0Тьма застилает ваши глаза...");
                    player.sendMessage("§8[Вуду] §0Вы ослепили " + targetName + ".");
                } else {
                    return true;
                }
                
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITCH_CELEBRATE, 1f, 0.5f);
                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GHAST_HURT, 1f, 0.5f);

                // Уменьшаем прочность куклы Вуду
                ItemStack doll = player.getInventory().getItemInMainHand();
                if (doll != null && doll.getItemMeta() != null) {
                    org.bukkit.inventory.meta.ItemMeta meta = doll.getItemMeta();
                    org.bukkit.persistence.PersistentDataContainer data = meta.getPersistentDataContainer();
                    org.bukkit.NamespacedKey usesKey = new org.bukkit.NamespacedKey(com.astrasmp.AstraSMPPlugin.getInstance(), "voodoo_uses");
                    if (data.has(usesKey, org.bukkit.persistence.PersistentDataType.INTEGER)) {
                        int uses = data.get(usesKey, org.bukkit.persistence.PersistentDataType.INTEGER);
                        if (uses <= 1) {
                            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                            player.sendMessage("§c[Вуду] Кукла рассыпалась в прах.");
                            player.closeInventory();
                        } else {
                            data.set(usesKey, org.bukkit.persistence.PersistentDataType.INTEGER, uses - 1);
                            List<Component> lore = meta.lore();
                            if (lore != null) {
                                lore.set(2, Component.text(TextUtil.color("&cПрочность: " + (uses - 1) + "/3")));
                                meta.lore(lore);
                            }
                            doll.setItemMeta(meta);
                        }
                    }
                }
                player.closeInventory();
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