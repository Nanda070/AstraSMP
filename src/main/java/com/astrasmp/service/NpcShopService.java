package com.astrasmp.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import com.astrasmp.AstraSMPPlugin;
import com.astrasmp.items.ItemRegistry;
import com.astrasmp.model.PlayerProfile;
import com.astrasmp.util.TextUtil;

import net.kyori.adventure.text.Component;

public final class NpcShopService implements Listener {
    private final ServiceManager services;
    private final NamespacedKey npcKey;
    private final NamespacedKey priceKey;
    private final NamespacedKey currencyKey;
    private final NamespacedKey vegasActionKey;

    public NpcShopService(AstraSMPPlugin plugin, ServiceManager services) {
        this.services = services;
        this.npcKey = new NamespacedKey(plugin, "npc_shop_type");
        this.priceKey = new NamespacedKey(plugin, "shop_price");
        this.currencyKey = new NamespacedKey(plugin, "shop_currency");
        this.vegasActionKey = new NamespacedKey(plugin, "vegas_action");
    }

    @SuppressWarnings("deprecation")
    public void spawnNpc(Player player, String type) {
        Villager npc = (Villager) player.getWorld().spawnEntity(player.getLocation(), EntityType.VILLAGER);
        npc.setAI(false);
        npc.setInvulnerable(true);
        npc.setCollidable(false);
        npc.getPersistentDataContainer().set(npcKey, PersistentDataType.STRING, type);

        String name = switch (type) {
            case "1" -> "§aНачальный торговец";
            case "2" -> "§eДекоратор";
            case "3" -> "§dБиблиотекарь";
            case "4" -> "§6Фермер";
            case "5" -> "§bГид ChetCraft";
            case "6" -> "§cКазино (Рулетка и Слоты)";
            case "7" -> "§4Казино (Карты и Настолки)";
            case "8" -> "§5Ивент Магазин";
            default -> "§7Торговец";
        };
        npc.setCustomName(name);
        npc.setCustomNameVisible(true);
        TextUtil.send(player, "&aNPC " + name + " &aуспешно заспавнен!");
    }

    @EventHandler
    public void onNpcInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager npc)) return;
        if (!npc.getPersistentDataContainer().has(npcKey, PersistentDataType.STRING)) return;

        event.setCancelled(true);
        String type = npc.getPersistentDataContainer().get(npcKey, PersistentDataType.STRING);
        Player p = event.getPlayer();

        switch (type) {
            case "1" -> openStarterShop(p);
            case "2" -> openDecorShop(p);
            case "3" -> openBooksShop(p);
            case "4" -> openFarmerShop(p);
            case "5" -> sendGuideInfo(p);
            case "6" -> openVegasSlots(p);
            case "7" -> openVegasTables(p);
            case "8" -> openEventShop(p);
        }
    }

    // ==========================================
    // ИНТЕГРАЦИЯ VEGAS (ПРОКСИ-ИНТЕРФЕЙСЫ)
    // ==========================================
    private void openVegasSlots(Player p) {
        Inventory inv = Bukkit.createInventory(new VegasHolder(), 27, Component.text("Vegas: Слоты и Рулетка"));
        inv.setItem(11, buildVegasTrigger(Material.MAGMA_CREAM, "&cРулетка", "casino play roulette"));
        inv.setItem(13, buildVegasTrigger(Material.GOLD_BLOCK, "&6Барабаны (Слоты)", "casino play drums"));
        inv.setItem(15, buildVegasTrigger(Material.DIAMOND, "&bКлассика", "casino play classic"));
        p.openInventory(inv);
    }

    private void openVegasTables(Player p) {
        Inventory inv = Bukkit.createInventory(new VegasHolder(), 27, Component.text("Vegas: Карты и Настолки"));
        inv.setItem(11, buildVegasTrigger(Material.PAPER, "&fБлэкджек", "casino play blackjack"));
        inv.setItem(15, buildVegasTrigger(Material.IRON_SWORD, "&aКрестики-Нолики", "casino play tictactoe"));
        p.openInventory(inv);
    }

    // ==========================================
    // ЛОГИКА ГАЙДА
    // ==========================================
    private void sendGuideInfo(Player p) {
        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_TRADE, 1f, 1f);
        TextUtil.send(p, "");
        TextUtil.send(p, "&b&lChetCraft &8» &fПриветствую, путник! Я местный гид.");
        TextUtil.send(p, "&fНаш сервер — это выживание с уникальными предметами и экономикой.");
        TextUtil.send(p, "");
        TextUtil.send(p, "&e&lПолезные команды:");
        TextUtil.send(p, " &8• &a/menu &7- Открыть главное меню");
        TextUtil.send(p, " &8• &a/spawn &7- Вернуться на спавн");
        TextUtil.send(p, " &8• &a/ip &7- Бесконечный Паркур");
        TextUtil.send(p, " &8• &a/quests &7- Твои задания");
        TextUtil.send(p, "");
        TextUtil.send(p, "&9&lНаш Discord: &nhttps://discord.gg/cheterin");
        TextUtil.send(p, "");
    }

    // ==========================================
    // ЛОГИКА СТАНДАРТНЫХ МАГАЗИНОВ
    // ==========================================
    private void openEventShop(Player p) {
        Inventory inv = Bukkit.createInventory(new ShopHolder(), 54, Component.text("Ивент Магазин"));
        inv.setItem(10, buildEventItem(ItemRegistry.mining3x3(), "&bШахтер 3x3", 500));
        inv.setItem(11, buildEventItem(ItemRegistry.mining5x5(), "&5Шахтер 5x5", 850));
        inv.setItem(12, buildEventItem(ItemRegistry.veinMiner(), "&bЖильный шахтер", 600));
        inv.setItem(13, buildEventItem(ItemRegistry.autoSmelt(), "&6Авто-плавка", 450));
        inv.setItem(14, buildEventItem(ItemRegistry.magnet(), "&eМагнит", 400));
        inv.setItem(19, buildEventItem(ItemRegistry.mining3x3Netherite(), "&dШахтер 3x3+", 1000));
        inv.setItem(20, buildEventItem(ItemRegistry.mining5x5Netherite(), "&5Шахтер 5x5+", 1700));
        inv.setItem(21, buildEventItem(ItemRegistry.veinMinerNetherite(), "&dЖильный шахтер+", 1200));
        inv.setItem(22, buildEventItem(ItemRegistry.autoSmeltNetherite(), "&6Авто-плавка+", 900));
        inv.setItem(23, buildEventItem(ItemRegistry.magnetNetherite(), "&eМагнит+", 800));
        inv.setItem(28, buildEventItem(ItemRegistry.shadowBlade(), "&8Теневой клинок", 700));
        inv.setItem(29, buildEventItem(ItemRegistry.thunderHammer(), "&bМолот грома", 750));
        inv.setItem(30, buildEventItem(ItemRegistry.vampireDagger(), "&cVampire Dagger", 600));
        inv.setItem(31, buildEventItem(ItemRegistry.infernoSword(), "&6Меч инферно", 700));
        inv.setItem(32, buildEventItem(ItemRegistry.frostAxe(), "&9Ледяной топор", 650));
        inv.setItem(37, buildEventItem(ItemRegistry.totemSpeed(), "&bТотем скорости", 200));
        inv.setItem(38, buildEventItem(ItemRegistry.totemShield(), "&7Тотем щита", 250));
        inv.setItem(39, buildEventItem(ItemRegistry.inquisitorHelmet(), "&eШлем Инквизитора", 600));
        inv.setItem(40, buildEventItem(ItemRegistry.inquisitorChestplate(), "&eНагрудник Инквизитора", 800));
        inv.setItem(41, buildEventItem(ItemRegistry.inquisitorLeggings(), "&eПоножи Инквизитора", 700));
        inv.setItem(42, buildEventItem(ItemRegistry.inquisitorBoots(), "&eСапоги Инквизитора", 500));
        p.openInventory(inv);
    }

    private void openStarterShop(Player p) {
        Inventory inv = Bukkit.createInventory(new ShopHolder(), 54, Component.text("Начальный торговец"));
        inv.setItem(10, build(Material.IRON_INGOT, 16, "&fЖелезный слиток", 400, "coins"));
        inv.setItem(11, build(Material.GOLD_INGOT, 16, "&eЗолотой слиток", 600, "coins"));
        inv.setItem(12, build(Material.LAPIS_LAZULI, 32, "&9Лазурит", 300, "coins"));
        inv.setItem(13, build(Material.REDSTONE, 64, "&cРедстоун", 250, "coins"));
        inv.setItem(14, build(Material.COAL, 64, "&8Уголь", 200, "coins"));
        inv.setItem(19, build(Material.COOKED_BEEF, 32, "&cЖареная говядина", 250, "coins"));
        inv.setItem(20, build(Material.COOKED_CHICKEN, 32, "&fЖареная курятина", 200, "coins"));
        inv.setItem(21, build(Material.BREAD, 32, "&eХлеб", 150, "coins"));
        inv.setItem(22, build(Material.GOLDEN_CARROT, 16, "&6Золотая морковь", 500, "coins"));
        inv.setItem(28, buildShopItem(ItemRegistry.mercenaryHelmet(), 800, "coins"));
        inv.setItem(29, buildShopItem(ItemRegistry.mercenaryChestplate(), 1200, "coins"));
        inv.setItem(30, buildShopItem(ItemRegistry.mercenaryLeggings(), 1000, "coins"));
        inv.setItem(31, buildShopItem(ItemRegistry.mercenaryBoots(), 800, "coins"));
        inv.setItem(37, buildShopItem(ItemRegistry.minerHelmet(), 1000, "coins"));
        inv.setItem(38, buildShopItem(ItemRegistry.minerChestplate(), 1500, "coins"));
        inv.setItem(39, buildShopItem(ItemRegistry.minerLeggings(), 1300, "coins"));
        inv.setItem(40, buildShopItem(ItemRegistry.minerBoots(), 1000, "coins"));
        p.openInventory(inv);
    }

    private void openDecorShop(Player p) {
        Inventory inv = Bukkit.createInventory(new ShopHolder(), 54, Component.text("Декоратор"));
        inv.setItem(10, build(Material.GLASS, 64, "§fСтекло", 150, "coins"));
        inv.setItem(11, build(Material.STONE_BRICKS, 64, "§7Каменные кирпичи", 200, "coins"));
        inv.setItem(12, build(Material.QUARTZ_BLOCK, 64, "§fКварцевый блок", 500, "coins"));
        inv.setItem(13, build(Material.SMOOTH_STONE, 64, "§7Гладкий камень", 200, "coins"));
        inv.setItem(14, build(Material.DEEPSLATE_BRICKS, 64, "§8Кирпичи из глубинного сланца", 250, "coins"));
        inv.setItem(15, build(Material.TUFF, 64, "§7Туф", 150, "coins"));
        inv.setItem(16, build(Material.CALCITE, 64, "§fКальцит", 300, "coins"));
        inv.setItem(19, build(Material.WHITE_CONCRETE, 64, "§fБелый бетон", 300, "coins"));
        inv.setItem(20, build(Material.GRAY_CONCRETE, 64, "§8Серый бетон", 300, "coins"));
        inv.setItem(21, build(Material.BLACK_CONCRETE, 64, "§0Черный бетон", 300, "coins"));
        inv.setItem(22, build(Material.RED_CONCRETE, 64, "§cКрасный бетон", 300, "coins"));
        inv.setItem(23, build(Material.TERRACOTTA, 64, "§6Терракота", 250, "coins"));
        inv.setItem(24, build(Material.BROWN_TERRACOTTA, 64, "§6Коричневая терракота", 250, "coins"));
        inv.setItem(25, build(Material.WHITE_TERRACOTTA, 64, "§fБелая терракота", 250, "coins"));
        inv.setItem(28, build(Material.SPRUCE_LOG, 64, "§6Еловое бревно", 300, "coins"));
        inv.setItem(29, build(Material.CHERRY_LOG, 64, "§dВишневое бревно", 400, "coins"));
        inv.setItem(30, build(Material.MANGROVE_LOG, 64, "§cМангровое бревно", 400, "coins"));
        inv.setItem(31, build(Material.OAK_LEAVES, 64, "§aЛиства дуба", 400, "coins"));
        inv.setItem(32, build(Material.CHERRY_LEAVES, 64, "§dВишневая листва", 650, "coins"));
        inv.setItem(33, build(Material.MOSS_BLOCK, 64, "§aБлок мха", 500, "coins"));
        inv.setItem(34, build(Material.DIRT_PATH, 64, "§6Тропинка", 400, "coins"));
        inv.setItem(37, build(Material.LANTERN, 16, "§eФонарь", 600, "coins"));
        inv.setItem(38, build(Material.SOUL_LANTERN, 16, "§bФонарь душ", 600, "coins"));
        inv.setItem(39, build(Material.SEA_LANTERN, 64, "§bМорской фонарь", 600, "coins"));
        inv.setItem(40, build(Material.GLOWSTONE, 64, "§eСветокамень", 400, "coins"));
        inv.setItem(41, build(Material.IRON_BARS, 16, "§7Железная решетка", 300, "coins"));
        inv.setItem(42, build(Material.AMETHYST_BLOCK, 64, "§dАметистовый блок", 500, "coins"));
        inv.setItem(43, build(Material.COPPER_BLOCK, 64, "§6Медный блок", 400, "coins"));
        inv.setItem(44, buildShopItem(ItemRegistry.trampoline(), 50000, "coins"));
        p.openInventory(inv);
    }

    private void openBooksShop(Player p) {
        var enchReg = io.papermc.paper.registry.RegistryAccess.registryAccess()
                .getRegistry(io.papermc.paper.registry.RegistryKey.ENCHANTMENT);
        Inventory inv = Bukkit.createInventory(new ShopHolder(), 45, Component.text("Библиотекарь"));
        inv.setItem(10, buildBook(enchReg.getOrThrow(NamespacedKey.minecraft("protection")), 2, 1500));
        inv.setItem(11, buildBook(enchReg.getOrThrow(NamespacedKey.minecraft("sharpness")), 3, 1900));
        inv.setItem(12, buildBook(enchReg.getOrThrow(NamespacedKey.minecraft("power")), 3, 1850));
        inv.setItem(13, buildBook(enchReg.getOrThrow(NamespacedKey.minecraft("fire_aspect")), 1, 1700));
        inv.setItem(20, buildBook(enchReg.getOrThrow(NamespacedKey.minecraft("efficiency")), 3, 1800));
        inv.setItem(21, buildBook(enchReg.getOrThrow(NamespacedKey.minecraft("unbreaking")), 2, 1600));
        inv.setItem(22, buildBook(enchReg.getOrThrow(NamespacedKey.minecraft("fortune")), 2, 1200));
        inv.setItem(23, buildBook(enchReg.getOrThrow(NamespacedKey.minecraft("silk_touch")), 1, 1500));
        inv.setItem(30, buildBook(enchReg.getOrThrow(NamespacedKey.minecraft("feather_falling")), 2, 1700));
        inv.setItem(31, buildBook(enchReg.getOrThrow(NamespacedKey.minecraft("looting")), 2, 1000));
        p.openInventory(inv);
    }

    private void openFarmerShop(Player p) {
        Inventory inv = Bukkit.createInventory(new ShopHolder(), 45, Component.text("Фермер"));
        inv.setItem(10, build(Material.WHEAT_SEEDS, 16, "&aСемена пшеницы", 350, "coins"));
        inv.setItem(11, build(Material.PUMPKIN_SEEDS, 8, "&6Семена тыквы", 550, "coins"));
        inv.setItem(12, build(Material.MELON_SEEDS, 8, "&aСемена арбуза", 650, "coins"));
        inv.setItem(13, build(Material.BEETROOT_SEEDS, 16, "&cСемена свеклы", 300, "coins"));
        inv.setItem(14, build(Material.CARROT, 16, "&6Морковь", 350, "coins"));
        inv.setItem(15, build(Material.POTATO, 16, "&eКартофель", 350, "coins"));
        inv.setItem(28, build(Material.SUGAR_CANE, 16, "&fТростник", 250, "coins"));
        inv.setItem(29, build(Material.BAMBOO, 32, "&aБамбук", 400, "coins"));
        inv.setItem(30, build(Material.CACTUS, 12, "&2Кактус", 400, "coins"));
        inv.setItem(31, build(Material.BONE_MEAL, 12, "&fКостная мука", 300, "coins"));
        inv.setItem(32, build(Material.MUSHROOM_STEW, 1, "&6Грибной суп", 300, "coins"));
        p.openInventory(inv);
    }

    // ==========================================
    // ОБРАБОТЧИКИ КЛИКОВ (МАГАЗИН И ПРОКСИ КАЗИНО)
    // ==========================================
    @EventHandler
    public void onShopClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof ShopHolder) && !(holder instanceof VegasHolder)) return;
        event.setCancelled(true);

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
        Player player = (Player) event.getWhoClicked();
        ItemMeta meta = event.getCurrentItem().getItemMeta();
        if (meta == null) return;

        // Перехват кликов по прокси-интерфейсу Vegas -> Нативный вызов AstraSMP
        if (holder instanceof VegasHolder) {
            if (meta.getPersistentDataContainer().has(vegasActionKey, PersistentDataType.STRING)) {
                String action = meta.getPersistentDataContainer().get(vegasActionKey, PersistentDataType.STRING);
                player.closeInventory();
                
                // Делегирование на нативные классы
                if ("casino play classic".equals(action)) {
                    services.classicGame().open(player);
                } else if ("casino play roulette".equals(action)) {
                    services.rouletteGame().open(player);
                } else if ("casino play tictactoe".equals(action)) {
                   services.ticTacToeGame().open(player);
                } else if ("casino play drums".equals(action)) {
                    services.drumsGame().open(player);
                } else if ("casino play blackjack".equals(action)) {
                    services.blackjackGame().open(player);
                } else {
                    TextUtil.send(player, "&cЭтот режим находится в разработке и будет добавлен позже!");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                }
            }
            return;
        }

        // Логика покупки в стандартных магазинах
        if (!meta.getPersistentDataContainer().has(priceKey, PersistentDataType.INTEGER)) return;

        int price = meta.getPersistentDataContainer().get(priceKey, PersistentDataType.INTEGER);
        String currency = meta.getPersistentDataContainer().getOrDefault(currencyKey, PersistentDataType.STRING, "coins");
        PlayerProfile profile = services.economy().profile(player.getUniqueId(), player.getName());

        if (currency.equals("events") && profile.getEventPoints() < price) {
            TextUtil.send(player, "&cНедостаточно Event Points!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        } else if (currency.equals("coins") && profile.getCoins() < price) {
            TextUtil.send(player, "&cНедостаточно ❂!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        if (player.getInventory().firstEmpty() == -1) {
            TextUtil.send(player, "&cИнвентарь заполнен!");
            return;
        }

        if (currency.equals("events")) profile.setEventPoints(profile.getEventPoints() - price);
        else profile.setCoins(profile.getCoins() - price);

        services.store().requestSave();

        ItemStack giveItem = event.getCurrentItem().clone();
        ItemMeta giveMeta = giveItem.getItemMeta();

        // Если это кастомный предмет (с custom_id) — восстанавливаем лор из ItemRegistry
        // чтобы не затирать описание предмета при очистке служебных данных магазина
        String customId = giveMeta.getPersistentDataContainer().get(
                new NamespacedKey(services.plugin(), "custom_id"),
                org.bukkit.persistence.PersistentDataType.STRING);
        if (customId == null) {
            // Для кастомного id из другого namespace
            customId = giveMeta.getPersistentDataContainer().get(
                    new NamespacedKey("astrasmp", "custom_id"),
                    org.bukkit.persistence.PersistentDataType.STRING);
        }

        if (customId != null && customId.equals("trampoline")) {
            giveItem = com.astrasmp.items.ItemRegistry.trampoline();
            giveItem.setAmount(1);
        } else {
            giveMeta.lore(null);
            giveMeta.getPersistentDataContainer().remove(priceKey);
            giveMeta.getPersistentDataContainer().remove(currencyKey);
            giveItem.setItemMeta(giveMeta);
        }

        player.getInventory().addItem(giveItem);
        TextUtil.send(player, "&aПокупка успешна!");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
    }

    // ==========================================
    // УТИЛИТЫ ДЛЯ ПРЕДМЕТОВ
    // ==========================================
    private ItemStack buildVegasTrigger(Material mat, String name, String commandAction) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(TextUtil.color(name)));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(""));
        lore.add(Component.text(TextUtil.color("&7Нажмите, чтобы начать игру")));
        meta.lore(lore);
        meta.getPersistentDataContainer().set(vegasActionKey, PersistentDataType.STRING, commandAction);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildEventItem(ItemStack baseItem, String name, int price) {
        return buildShopItem(baseItem, price, "events");
    }

    private ItemStack buildShopItem(ItemStack baseItem, int price, String currency) {
        ItemStack item = baseItem.clone();
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = meta.hasLore() ? Objects.requireNonNull(meta.lore()) : new ArrayList<>();
        String currencyStr = currency.equals("events") ? "&dEvent Points" : "&e❂";
        lore.add(Component.text(""));
        lore.add(Component.text(TextUtil.color("&7Цена: " + price + " " + currencyStr)));
        lore.add(Component.text(TextUtil.color("&aНажмите, чтобы купить")));
        meta.lore(lore);
        meta.getPersistentDataContainer().set(priceKey, PersistentDataType.INTEGER, price);
        meta.getPersistentDataContainer().set(currencyKey, PersistentDataType.STRING, currency);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack build(Material mat, int amount, String name, int price, String currency) {
        ItemStack item = new ItemStack(mat, amount);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(TextUtil.color(name)));
        List<Component> lore = new ArrayList<>();
        String currencyStr = currency.equals("events") ? "&dEvent Points" : "&e❂";
        lore.add(Component.text(TextUtil.color("&7Цена: " + price + " " + currencyStr)));
        meta.lore(lore);
        meta.getPersistentDataContainer().set(priceKey, PersistentDataType.INTEGER, price);
        meta.getPersistentDataContainer().set(currencyKey, PersistentDataType.STRING, currency);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildBook(Enchantment ench, int level, int price) {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK, 1);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
        meta.addStoredEnchant(ench, level, true);
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(TextUtil.color("&7Цена: " + price + " &e❂")));
        meta.lore(lore);
        meta.getPersistentDataContainer().set(priceKey, PersistentDataType.INTEGER, price);
        meta.getPersistentDataContainer().set(currencyKey, PersistentDataType.STRING, "coins");
        item.setItemMeta(meta);
        return item;
    }

    // Холдеры
    private static class ShopHolder implements InventoryHolder { @Override public @NotNull Inventory getInventory() { return null; } }
    private static class VegasHolder implements InventoryHolder { @Override public @NotNull Inventory getInventory() { return null; } }
}