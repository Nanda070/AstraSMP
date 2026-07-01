package com.astrasmp.commands;

import com.astrasmp.AstraSMPPlugin;
import com.astrasmp.service.QuestManager;
import com.astrasmp.service.ServiceManager;
import com.astrasmp.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TutorialCommand implements org.bukkit.command.TabExecutor, Listener {

    private static final String TITLE_MAIN    = "§8§lГид по Серверу ChetCraft";
    private static final String TITLE_MAGIC   = "§5§l🔮 Магия & Оккультизм";
    private static final String TITLE_CLASSES = "§b§l⚔ AstraOP — Классы";
    private static final String TITLE_BUILD   = "§e§l⚙ AstraBuild — Механизмы";
    private static final String TITLE_PARKOUR = "§a§l🏃 AstraIP — Паркур";
    private static final String TITLE_ECONOMY = "§6§l💰 Экономика & Гильдии";
    private static final String TITLE_PVP     = "§c§l⚔ PvP & Арены";

    private final ServiceManager services;

    public TutorialCommand(ServiceManager services) {
        this.services = services;
        Bukkit.getPluginManager().registerEvents(this, AstraSMPPlugin.getInstance());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cТолько для игроков.");
            return true;
        }

        if (args.length == 0) {
            openMainMenu(player);
            services.quests().processAction(player, QuestManager.QuestAction.USE_COMMAND, "/tutorial", 1);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "classes", "классы"   -> openClassesMenu(player);
            case "magic", "магия"      -> openMagicMenu(player);
            case "build", "механизмы", "quarry" -> openBuildMenu(player);
            case "parkour", "паркур"   -> openParkourMenu(player);
            case "economy", "экономика" -> openEconomyMenu(player);
            case "pvp"                 -> openPvpMenu(player);
            case "me"                  -> openMainMenu(player);
            default -> {
                TextUtil.send(player, AstraSMPPlugin.getInstance().getConfigManager().getMessage(
                        "msg_46ad0f",
                        "&cНеизвестный раздел. Используй /tutorial без аргументов для открытия меню."));
                return true;
            }
        }
        services.quests().processAction(player, QuestManager.QuestAction.USE_COMMAND,
                "/tutorial " + args[0].toLowerCase(), 1);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>(List.of(
                    "classes", "magic", "build", "parkour", "economy", "pvp"));
            completions.removeIf(c -> !c.startsWith(args[0].toLowerCase()));
            return completions;
        }
        return Collections.emptyList();
    }

    private void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(new MenuHolder(MenuType.MAIN), 54, Component.text(TITLE_MAIN));
        fillBorder(inv, Material.BLACK_STAINED_GLASS_PANE);

        inv.setItem(10, makeItem(Material.AMETHYST_SHARD, "§5§l🔮 Магия & Оккультизм", "§7Ритуальные круги, пакты с демонами,", "§7астральная проекция, разломы,", "§7карманные измерения и многое другое.", "", "§d➤ Нажмите для открытия раздела"));
        inv.setItem(12, makeItem(Material.NETHERITE_SWORD, "§b§l⚔ AstraOP — Классы", "§713 уникальных классов со своими", "§7пассивными бонусами и активными", "§7способностями на кулдауне.", "", "§d➤ Нажмите для открытия раздела"));
        inv.setItem(14, makeItem(Material.PISTON, "§e§l⚙ AstraBuild — Механизмы", "§7Индустриальные механизмы: реактор,", "§7карьер, центрифуга, трубы, нано-броня", "§7и орбитальные удары из космоса!", "", "§d➤ Нажмите для открытия раздела"));
        inv.setItem(28, makeItem(Material.FEATHER, "§a§l🏃 AstraIP — Паркур", "§7Динамические паркур-трассы,", "§7режимы игры, рейтинги и призы", "§7за лучшее прохождение.", "", "§d➤ Нажмите для открытия раздела"));
        inv.setItem(30, makeItem(Material.GOLD_INGOT, "§6§l💰 Экономика & Гильдии", "§7Монеты, аукцион, контракты,", "§7гильдии, браки, ежедневные", "§7награды и дерево талантов.", "", "§d➤ Нажмите для открытия раздела"));
        inv.setItem(32, makeItem(Material.IRON_SWORD, "§c§l⚔ PvP & Арены", "§7Дуэли 1×1, многопользовательские", "§7арены, рейтинг MMR, артефакты", "§7и кузница улучшений.", "", "§d➤ Нажмите для открытия раздела"));
        inv.setItem(22, makeItem(Material.ENCHANTED_BOOK, "§f§lChetCraft — Добро пожаловать!", "§7Это интерактивный гид по всем", "§7механикам и плагинам сервера.", "", "§fВыберите нужный раздел слева.", "§8discord.gg/cheterin"));

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.8f, 1.2f);
    }

    private void openMagicMenu(Player player) {
        Inventory inv = Bukkit.createInventory(new MenuHolder(MenuType.MAGIC), 54, Component.text(TITLE_MAGIC));
        fillBorder(inv, Material.PURPLE_STAINED_GLASS_PANE);

        inv.setItem(10, makeItem(Material.REDSTONE, "§c§lРитуальные Круги", "§7Постройте ритуальный круг из красного", "§7камня и свечей, принесите жертву,", "§7получите мощные предметы и скверну!", "", "§d✦ Тир 1: §7Красный камень + 4 свечи", "§d✦ Тир 2: §7+Рамка 5×5 + черепа иссушителя", "§d✦ Тир 3: §7+Незерит + огонь душ", "", "§e➤ Нажмите для полного гайда"));
        inv.setItem(12, makeItem(Material.DRAGON_BREATH, "§5§lДемонические Пакты", "§7Заключи контракт с тёмными силами.", "§7Ты теряешь §c4 сердца §7навсегда,", "§7но получаешь уникальный бонус:", "", "§4Пакт Крови: §7иммунитет к огню + вампиризм 15%", "§8Пакт Бездны: §7иммунитет к падению, но урон в воде", "§dПакт Теней: §7невидимость/скорость в темноте", "", "§e➤ Нажмите для полного гайда"));
        inv.setItem(14, makeItem(Material.PHANTOM_MEMBRANE, "§7§lАстральная Проекция", "§7Выйди из тела и исследуй мир", "§7в бестелесной форме!", "", "§7Ритуал: §fОсколок Души + Мембрана фантома", "§7+ Флакон Крови, жертва — Фантом (Тир 3)", "", "§e➤ Нажмите для полного гайда"));
        inv.setItem(16, makeItem(Material.ENDER_EYE, "§9§lПространственные Разломы", "§7Открывай порталы в другие измерения!", "", "§7Ритуал: §fДуша Демона + Плачущий обсидиан", "§7Жертва: Эндермен (Тир 3)", "§7Стоимость: §c+200 скверны", "", "§e➤ Нажмите для полного гайда"));
        inv.setItem(28, makeItem(Material.NETHER_STAR, "§d§lКарманные Измерения", "§7Личный пустотный мир! Пригласи друзей", "§7и стройте в собственном пространстве.", "", "§7Ритуал (Семя Бездны): §fЗвезда Нижнего Мира", "§7+ Осколок Души + Флакон Крови", "§7Жертва: Скелет-иссушитель (Тир 3)", "§7", "§a/prunus §7— войти  §c/malus §7— выйти", "", "§e➤ Нажмите для полного гайда"));
        inv.setItem(30, makeItem(Material.BLAZE_POWDER, "§4§lКровавая Луна", "§7Ивент, меняющий правила игры.", "§7Мобы становятся агрессивнее,", "§7скверна от ритуалов удваивается!", "", "§7Запуск: §f/admin event bloodnight", "§8(только для администраторов)", "", "§e➤ Нажмите для полного гайда"));
        inv.setItem(32, makeItem(Material.WITHER_ROSE, "§8§lСкверна & Порча Чанков", "§7Каждый ритуал заражает чанк Скверной.", "§7Высокий уровень заражения: дебаффы", "§7для всех игроков в зоне.", "", "§7Очищение ритуалом: §fЗолотое яблоко", "§7+ Слеза гаста, жертва — Житель (Тир 2)", "§7Результат: §a-50 Скверны чанка", "", "§e➤ Нажмите для полного гайда"));
        inv.setItem(34, makeItem(Material.BOOK, "§6§lВсе Ритуальные Рецепты", "§7Полный список ритуалов сервера.", "", "§e➤ Нажмите для списка рецептов"));

        addBackButton(inv, 49);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.8f, 0.7f);
    }

    private void openClassesMenu(Player player) {
        Inventory inv = Bukkit.createInventory(new MenuHolder(MenuType.CLASSES), 54, Component.text(TITLE_CLASSES));
        fillBorder(inv, Material.CYAN_STAINED_GLASS_PANE);

        inv.setItem(4, makeItem(Material.WRITTEN_BOOK, "§b§lО Системе Классов", "§7Выбери класс у Хранителя Храма.", "§7За смену класса платишь монетами", "§7или Катализатором Астры.", "", "§7У каждого класса уникальные", "§7пассивные бонусы и активная", "§7способность.", "", "§e/astra keybind §7— выбрать привязку"));
        inv.setItem(10, makeItem(Material.OAK_LOG, "§f§lHUMAN — Человек", "§7Стартовый класс.", "§8Нет особых бонусов, нет штрафов.", "§7Может пройти ритуал §4Bloodmage§7.", "§8Способность: нет"));
        inv.setItem(11, makeItem(Material.CRAFTING_TABLE, "§3§lARCHITECT — Архитектор", "§a+ §7Радиус взаимодействия (+2 блока)", "§a+ §7Иммунитет к падению", "§a+ §7Спешка I + Скорость I (постоянно)", "§c- §7Урон в ближнем бою ×0.7", "§c- §7Взрывной урон ×1.5"));
        inv.setItem(12, makeItem(Material.IRON_CHESTPLATE, "§9§lVANGUARD — Авангард", "§a+ §7ПКМ-атака ×1.2", "§c- §7Урон стрелами ×0.5", "§c- §7Голод (50% быстрее)", "", "§e§lСпособность — Адреналин (30с CD):", "§7При <4.5 HP: Сила I + Скорость II"));
        inv.setItem(13, makeItem(Material.DIAMOND_CHESTPLATE, "§8§lJUGGERNAUT — Джаггернаут", "§a+ §710 HP бонус", "§a+ §74 брони + 2 прочности брони", "§a+ §7Иммунитет к отбрасыванию", "§c- §7Замедление I", "§c- §7Нельзя элитру"));
        inv.setItem(14, makeItem(Material.BLACK_STAINED_GLASS, "§8§lSHADOW — Тень", "§a+ §7Скорость II", "§a+ §7Невидимость при приседании в темноте", "§a+ §7Урон из невидимости ×1.5", "§c- §76 HP штраф", "§c- §7На солнце: Слабость I"));
        inv.setItem(15, makeItem(Material.BOW, "§2§lRANGER — Рейнджер", "§a+ §7Скорость стрел ×1.2", "§a+ §730% шанс не расходовать стрелу", "§a+ §7Урон стрелами ×1.5", "§c- §7Ближний бой ×0.6 урона", "§c- §7Нет щитов"));
        inv.setItem(16, makeItem(Material.IRON_AXE, "§4§lBERSERKER — Берсерк", "§a+ §7Пробивание брони", "§a+ §7При убийстве: Сила II + Скорость II", "§c- §7Без луков/арбалетов", "§c- §7Броня ломается быстрее"));
        inv.setItem(19, makeItem(Material.ELYTRA, "§f§lWINDWALKER — Странник Ветра", "§a+ §7Встроенная элитра (полёт в выживании)", "§c- §7Ниже Y=50: Усталость + Слабость", "§c- §7Урон стрелами ×1.5", "", "§e§lСпособность — Апдрафт (45с CD)"));
        inv.setItem(20, makeItem(Material.PHANTOM_MEMBRANE, "§7§lPHANTOM — Фантом", "§a+ §7Невидимость при приседании", "§c- §74 HP штраф", "§c- §7Запрет тяжёлой брони", "", "§e§lСпособность — Фазовый Рывок (20с CD)"));
        inv.setItem(21, makeItem(Material.FIRE_CHARGE, "§6§lPHOENIX — Феникс", "§a+ §7Полный иммунитет к огню/взрывам", "§c- §74 HP штраф", "§c- §7В воде: урон", "", "§e§lСпособность — Сверхновая (60с CD)"));
        inv.setItem(22, makeItem(Material.OAK_SAPLING, "§2§lDRUID — Друид", "§a+ §7Безопасная гнилая еда", "§a+ §7Нейтральные мобы", "§c- §7Запрет тяжёлой брони", "", "§e§lСпособность — Объятие Природы (45с CD)"));
        inv.setItem(23, makeItem(Material.FERMENTED_SPIDER_EYE, "§5§lVAMPIRE — Вампир", "§a+ §7Вампиризм 20%", "§c- §7Огонь/лава: урон ×1.5", "", "§e§lСпособность — Кровавый Рывок (30с CD)"));
        inv.setItem(24, makeItem(Material.GOLDEN_CHESTPLATE, "§e§lGOLIATH — Голиаф", "§a+ §710 HP бонус", "§a+ §7Иммунитет к отбрасыванию", "§a+ §7Спешка II", "§c- §7Голод", "", "§e§lСпособность — Сотрясение (35с CD)"));
        inv.setItem(25, makeItem(Material.REDSTONE, "§4§lBLOODMAGE — Маг Крови", "§7Доступен через ритуал (HUMAN).", "§a+ §7Кровавое Копьё (ЛКМ пустой рукой)", "§a+ §7За убийства: +1 HP", "§c- §74 HP штраф, нет регена", "", "§e§lСпособность — Щит Жизни (45с CD)"));
        addBackButton(inv, 49);

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.5f);
    }

    private void openBuildMenu(Player player) {
        Inventory inv = Bukkit.createInventory(new MenuHolder(MenuType.BUILD), 54, Component.text(TITLE_BUILD));
        fillBorder(inv, Material.YELLOW_STAINED_GLASS_PANE);

        inv.setItem(4, makeItem(Material.WRITTEN_BOOK, "§e§lAstraBuild — Введение", "§7Индустриальный плагин, превращающий", "§7сервер в технологический мир.", "", "§7Энергия (EU) — основной ресурс.", "§7Все механизмы потребляют EU.", "§7Источники: Ядерный Реактор (500 EU/t)", "§7Хранилище: МФСУ (до 10,000,000 EU)"));
        inv.setItem(10, makeItem(Material.TNT, "§c§lЯдерный Реактор", "§7Самый мощный источник энергии.", "§7Генерирует 500 EU/t, но перегревается!", "", "§c⚠ Перегрев = взрыв + радиация на 24 ч!", "", "§e➤ Нажмите для полного гайда"));
        inv.setItem(12, makeItem(Material.BEACON, "§a§lМФСУ — Аккумулятор", "§7Хранит до 10,000,000 EU.", "§7Заряжает нано-броню (50 EU/t).", "", "§e➤ Нажмите для полного гайда"));
        inv.setItem(14, makeItem(Material.DROPPER, "§b§lЛогистика — Трубы и Коннекторы", "§7Логистические трубы: §f светло-голубое стекло", "§7Коннектор Вставки: §fвыбрасыватель (вход)", "§7Коннектор Извлечения: §fвыбрасыватель (выход)", "", "§e➤ Нажмите для полного гайда"));
        inv.setItem(16, makeItem(Material.DIAMOND_PICKAXE, "§6§lКарьер (Quarry)", "§7Автоматически добывает ресурсы", "§7в указанной области.", "", "§e➤ Нажмите для полного гайда"));
        inv.setItem(28, makeItem(Material.NETHERITE_CHESTPLATE, "§8§lНано-Броня & Оружие", "§7Нано-Меч: расходует EU, наносит доп. урон.", "§7Нано-Нагрудник: снижает получаемый урон.", "§7Нано-Джетпак: полёт.", "§7Химзащита: §aзащита от радиации!", "", "§e➤ Нажмите для полного гайда"));
        inv.setItem(30, makeItem(Material.BREWING_STAND, "§7§lЦентрифуга & Генератор Материи", "§7Центрифуга: переработка руд.", "§7Генератор Материи: Создаёт", "§7Универсальную Материю (1,000,000 EU).", "", "§e➤ Нажмите для полного гайда"));
        inv.setItem(32, makeItem(Material.LIGHTNING_ROD, "§c§lОрбитальный Удар (Спутник)", "§7Мощнейшее оружие уничтожения!", "§7Требует 36,000,000 EU.", "", "§c⚠ Радиация остаётся после взрыва!", "", "§e➤ Нажмите для полного гайда"));
        inv.setItem(34, makeItem(Material.LEATHER_HELMET, "§a§lРадиация — Защита", "§7После взрыва реактора или орбитального", "§7удара чанки заражены радиацией §c24 часа§7.", "", "§a§lЗащита: §7полный сет §fХимзащиты", "", "§e➤ Нажмите для полного гайда"));

        addBackButton(inv, 49);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_PISTON_EXTEND, 0.8f, 0.8f);
    }

    private void openParkourMenu(Player player) {
        Inventory inv = Bukkit.createInventory(new MenuHolder(MenuType.PARKOUR), 27, Component.text(TITLE_PARKOUR));
        fillBorder(inv, Material.LIME_STAINED_GLASS_PANE);

        inv.setItem(10, makeItem(Material.FEATHER, "§a§lКак начать паркур?", "§71. Подойди к стартовой площадке", "§72. Трасса генерируется автоматически", "§73. Прыгай по блокам, не падай!", "§74. Финишируй — получи приз и очки", "", "§e➤ Нажмите для полного гайда"));
        inv.setItem(12, makeItem(Material.SPYGLASS, "§b§lРежимы игры", "§a§lСтандартный: §7на время.", "§7§lНаблюдение: §7смотри за другими", "", "§e➤ Нажмите для полного гайда"));
        inv.setItem(14, makeItem(Material.GOLD_INGOT, "§6§lРейтинг & Награды", "§7Лучшие результаты сохраняются", "§7в таблице лидеров.", "§7Монеты за финиш и рекорды.", "", "§e➤ Нажмите для полного гайда"));

        addBackButton(inv, 22);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 1.5f);
    }

    private void openEconomyMenu(Player player) {
        Inventory inv = Bukkit.createInventory(new MenuHolder(MenuType.ECONOMY), 54, Component.text(TITLE_ECONOMY));
        fillBorder(inv, Material.ORANGE_STAINED_GLASS_PANE);

        inv.setItem(10, makeItem(Material.GOLD_INGOT, "§6§lМонеты (Coins)", "§7Основная валюта сервера.", "§f/balance §7— посмотреть баланс", "§f/sell §7— продать ресурсы", "", "§e➤ Нажмите для полного гайда"));
        inv.setItem(12, makeItem(Material.CHEST, "§e§lАукцион", "§7Торгуй предметами с игроками!", "§f/ah §7— открыть аукцион", "", "§e➤ Нажмите для полного гайда"));
        inv.setItem(14, makeItem(Material.CREEPER_HEAD, "§c§lКонтракты (Баунти)", "§7Назначь награду за голову врага!", "§f/contract §7— управление", "§f/bounty §7— список охот", "", "§e➤ Нажмите для полного гайда"));
        inv.setItem(16, makeItem(Material.SHIELD, "§b§lГильдии", "§7Создавай кланы и захватывай территории!", "§f/guild create <название>", "", "§e➤ Нажмите для полного гайда"));
        inv.setItem(28, makeItem(Material.DIAMOND, "§d§lДерево Талантов", "§7Вкладывай монеты и EP в улучшения!", "§f/talents §7— открыть дерево", "", "§e➤ Нажмите для полного гайда"));
        inv.setItem(30, makeItem(Material.SUNFLOWER, "§e§lЕжедневные Награды", "§7Заходи каждый день за бонусами!", "§f/rewards §7— открыть календарь", "", "§e➤ Нажмите для полного гайда"));
        inv.setItem(32, makeItem(Material.HEART_OF_THE_SEA, "§d§lБраки", "§7Зарегистрируй отношения с другим игроком!", "§f/marry <игрок> §7— предложение", "", "§e➤ Нажмите для полного гайда"));
        inv.setItem(34, makeItem(Material.MAP, "§f§lКвесты", "§7Ежедневные задания для всех!", "§f/quest §7— текущие квесты", "", "§e➤ Нажмите для полного гайда"));

        addBackButton(inv, 49);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.0f);
    }

    private void openPvpMenu(Player player) {
        Inventory inv = Bukkit.createInventory(new MenuHolder(MenuType.PVP), 54, Component.text(TITLE_PVP));
        fillBorder(inv, Material.RED_STAINED_GLASS_PANE);

        inv.setItem(10, makeItem(Material.IRON_SWORD, "§c§lДуэли 1×1", "§7Вызови игрока на честный бой!", "§f/duel <игрок>", "", "§e➤ Нажмите для полного гайда"));
        inv.setItem(12, makeItem(Material.SHIELD, "§e§lАрены", "§7Командные и одиночные бои!", "§f/arena §7— войти", "", "§e➤ Нажмите для полного гайда"));
        inv.setItem(14, makeItem(Material.DIAMOND, "§b§lMMR — Рейтинг", "§7Сражайся и улучшай свой рейтинг!", "§f/mmr §7— узнать рейтинг", "", "§e➤ Нажмите для полного гайда"));
        inv.setItem(16, makeItem(Material.NETHERITE_AXE, "§6§lАртефакты & Кузница", "§7Уникальные предметы с особыми свойствами!", "§f/items §7— просмотр артефактов", "§f/blacksmith §7— кузница", "", "§e➤ Нажмите для полного гайда"));
        inv.setItem(28, makeItem(Material.TOTEM_OF_UNDYING, "§a§lТотем Бессмертия", "§c15 секунд §7кулдауна после использования!", "", "§e➤ Нажмите для полного гайда"));
        inv.setItem(32, makeItem(Material.ENDER_PEARL, "§d§lИвенты", "§7Регулярные события (Метеорит, Босс, Аирдроп)", "", "§e➤ Нажмите для полного гайда"));

        addBackButton(inv, 49);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.8f, 0.9f);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof MenuHolder holder)) return;

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        if (clicked.getType().name().contains("STAINED_GLASS_PANE")) return;

        MenuType type = holder.getType();

        if (clicked.getType() == Material.ARROW) {
            openMainMenu(player);
            return;
        }

        switch (type) {
            case MAIN -> handleMainClick(player, event.getSlot());
            case MAGIC -> handleMagicClick(player, event.getSlot());
            case CLASSES -> handleClassesClick(player, event.getSlot());
            case BUILD -> handleBuildClick(player, event.getSlot());
            case PARKOUR -> handleParkourClick(player, event.getSlot());
            case ECONOMY -> handleEconomyClick(player, event.getSlot());
            case PVP -> handlePvpClick(player, event.getSlot());
        }
    }

    private void handleMainClick(Player player, int slot) {
        switch (slot) {
            case 10 -> openMagicMenu(player);
            case 12 -> openClassesMenu(player);
            case 14 -> openBuildMenu(player);
            case 28 -> openParkourMenu(player);
            case 30 -> openEconomyMenu(player);
            case 32 -> openPvpMenu(player);
        }
    }

    private void handleMagicClick(Player player, int slot) {
        player.closeInventory();
        switch (slot) {
            case 10 -> sendGuide(player, "Ритуальные Круги", "Для магии нужен круг из красного камня и свечей (Тир 1).", "Тир 2: 5x5 рамка + черепа.", "Тир 3: +незерит и огонь душ.");
            case 12 -> sendGuide(player, "Пакты", "Контракт забирает 4 сердца навсегда.", "Кровь: защита от огня, вампиризм.", "Бездна: защита от падения, урон в воде.", "Тень: инвиз в темноте, слабость днём.");
            case 14 -> sendGuide(player, "Астрал", "Ритуал: Осколок души + Мембрана + Кровь. Выход из тела!");
            case 16 -> sendGuide(player, "Разломы", "Ритуал: Душа Демона + Плачущий обсидиан. Порталы!");
            case 28 -> sendGuide(player, "Карманные измерения", "Ритуал Семя Бездны (Тир 3). /prunus - войти, /malus - выйти.");
            case 30 -> sendGuide(player, "Кровавая Луна", "Ивент! Мобы злее, скверна х2.");
            case 32 -> sendGuide(player, "Скверна", "Ритуалы копят скверну. Очищение: Зол. яблоко + Слеза гаста.");
            case 34 -> sendGuide(player, "Рецепты", "Для всех рецептов экспериментируйте с кровью, душами и драг. металлами в кругах.");
            default -> openMagicMenu(player);
        }
    }

    private void handleClassesClick(Player player, int slot) {
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
    }

    private void handleBuildClick(Player player, int slot) {
        player.closeInventory();
        switch (slot) {
            case 10 -> sendGuide(player, "Реактор", "500 EU/t. Требует водяного охлаждения, иначе взорвётся!");
            case 12 -> sendGuide(player, "МФСУ", "Хранит 10кк EU. Заряжает нано-предметы.");
            case 14 -> sendGuide(player, "Логистика", "Соединяет сундуки. Выбрасыватель = коннектор.");
            case 16 -> sendGuide(player, "Карьер", "Автоматически копает. Нужны трубы и энергия.");
            case 28 -> sendGuide(player, "Нано-экипировка", "Меч, броня и джетпак. Заряжаются в МФСУ.");
            case 30 -> sendGuide(player, "Механизмы", "Центрифуга и Ген. Материи. Материя = 1кк EU.");
            case 32 -> sendGuide(player, "Орбитальный удар", "36кк EU. Полное уничтожение зоны. Радиация!");
            case 34 -> sendGuide(player, "Радиация", "Используй полный Hazmat Suit для защиты.");
            default -> openBuildMenu(player);
        }
    }

    private void handleParkourClick(Player player, int slot) {
        player.closeInventory();
        switch (slot) {
            case 10 -> sendGuide(player, "Паркур", "Прыгай на стартовую зону. Трасса генерируется на лету.");
            case 12 -> sendGuide(player, "Режимы", "Стандартный - на время. Наблюдатель - смотреть за другими.");
            case 14 -> sendGuide(player, "Рейтинг", "Таблица лидеров сохраняется. За рекорды дают монеты.");
            default -> openParkourMenu(player);
        }
    }

    private void handleEconomyClick(Player player, int slot) {
        player.closeInventory();
        switch (slot) {
            case 10 -> sendGuide(player, "Монеты", "/bal, /pay, /sell. Основа экономики.");
            case 12 -> sendGuide(player, "Аукцион", "/ah. Максимум 12 лотов.");
            case 14 -> sendGuide(player, "Контракты", "/contract. Награда за голову.");
            case 16 -> sendGuide(player, "Гильдии", "/guild create. Кланы до 30 человек.");
            case 28 -> sendGuide(player, "Таланты", "/talents. Вампиризм, Гладиатор и т.д. за монеты и EP.");
            case 30 -> sendGuide(player, "Награды", "/rewards. Заходи каждый день!");
            case 32 -> sendGuide(player, "Браки", "/marry. Зарегистрируй отношения.");
            case 34 -> sendGuide(player, "Квесты", "/quest. Выполняй задания для EP.");
            default -> openEconomyMenu(player);
        }
    }

    private void handlePvpClick(Player player, int slot) {
        player.closeInventory();
        switch (slot) {
            case 10 -> sendGuide(player, "Дуэли", "/duel <игрок>. Честный бой 1х1.");
            case 12 -> sendGuide(player, "Арены", "/arena. Командные бои и мясорубка.");
            case 14 -> sendGuide(player, "MMR", "Рейтинг. От Бронзы до Элиты.");
            case 16 -> sendGuide(player, "Артефакты", "/items. Уникальная броня с абилками.");
            case 28 -> sendGuide(player, "Тотем", "После юза тотема - 15 секунд КД на спасение!");
            case 32 -> sendGuide(player, "Ивенты", "Метеориты, боссы, аирдропы каждые пару часов.");
            default -> openPvpMenu(player);
        }
    }

    private void sendGuide(Player player, String title, String... lines) {
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
        send(player, "");
        send(player, "§6§l━━━━━━━━ " + title + " ━━━━━━━━");
        send(player, "");
        for (String line : lines) {
            send(player, "§7" + line);
        }
        send(player, "");
    }

    private void send(Player player, String message) {
        TextUtil.send(player, message);
    }

    private static ItemStack makeItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        item.editMeta(meta -> {
            meta.displayName(Component.text(TextUtil.color(name)));
            if (lore != null && lore.length > 0) {
                List<Component> loreComponents = new ArrayList<>();
                for (String line : lore) {
                    loreComponents.add(Component.text(TextUtil.color(line)));
                }
                meta.lore(loreComponents);
            }
        });
        return item;
    }

    private static void addBackButton(Inventory inv, int slot) {
        inv.setItem(slot, makeItem(Material.ARROW, "§f◀ Назад", "§7Вернуться в главное меню"));
    }

    private static void fillBorder(Inventory inv, Material borderMaterial) {
        ItemStack pane = new ItemStack(borderMaterial);
        pane.editMeta(meta -> meta.displayName(Component.empty()));
        int size = inv.getSize();
        int rows = size / 9;
        for (int i = 0; i < size; i++) {
            int row = i / 9;
            int col = i % 9;
            if (row == 0 || row == rows - 1 || col == 0 || col == 8) {
                inv.setItem(i, pane);
            }
        }
    }

    private enum MenuType {
        MAIN, MAGIC, CLASSES, BUILD, PARKOUR, ECONOMY, PVP
    }

    private static class MenuHolder implements InventoryHolder {
        private final MenuType type;
        private Inventory inventory;

        MenuHolder(MenuType type) {
            this.type = type;
        }

        public MenuType getType() {
            return type;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }
}
