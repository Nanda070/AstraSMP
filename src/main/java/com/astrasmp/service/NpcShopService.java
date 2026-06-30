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
        loadShops();
    }

    public record ShopData(String name, List<ItemStack> items) {}
    private final java.util.Map<String, ShopData> loadedShops = new java.util.HashMap<>();

    public void loadShops() {
        loadedShops.clear();
        java.io.File file = new java.io.File(services.plugin().getDataFolder(), "npc_shops.yml");
        if (!file.exists()) {
            services.plugin().saveResource("npc_shops.yml", false);
        }
        org.bukkit.configuration.file.FileConfiguration cfg = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
        
        org.bukkit.configuration.ConfigurationSection shopsSec = cfg.getConfigurationSection("shops");
        if (shopsSec == null) return;
        
        for (String shopId : shopsSec.getKeys(false)) {
            String shopName = shopsSec.getString(shopId + ".name", "&7Магазин");
            List<java.util.Map<?, ?>> itemsMap = shopsSec.getMapList(shopId + ".items");
            List<ItemStack> items = new ArrayList<>();
            
            for (java.util.Map<?, ?> map : itemsMap) {
                try {
                    String type = (String) map.get("type");
                    int price = (Integer) map.get("price");
                    String currency = (String) map.get("currency");
                    
                    ItemStack item = null;
                    if ("material".equals(type)) {
                        Material mat = Material.valueOf((String) map.get("material"));
                        int amount = (Integer) map.get("amount");
                        String name = (String) map.get("name");
                        item = build(mat, amount, name, price, currency);
                    } else if ("custom".equals(type)) {
                        String id = (String) map.get("id");
                        java.lang.reflect.Method m = ItemRegistry.class.getMethod(id);
                        ItemStack base = (ItemStack) m.invoke(null);
                        item = buildShopItem(base, price, currency);
                    } else if ("relic".equals(type) || "artifact".equals(type)) {
                        String id = (String) map.get("id");
                        Material mat = Material.valueOf((String) map.get("material"));
                        String name = (String) map.get("name");
                        String loreStr = (String) map.get("lore");
                        ItemStack base = "relic".equals(type) ? ItemRegistry.relic(id, mat, name, loreStr) 
                                                            : ItemRegistry.artifact(id, mat, name, loreStr);
                        item = buildShopItem(base, price, currency);
                    } else if ("book".equals(type)) {
                        String enchStr = (String) map.get("enchantment");
                        int level = (Integer) map.get("level");
                        var enchReg = io.papermc.paper.registry.RegistryAccess.registryAccess()
                                .getRegistry(io.papermc.paper.registry.RegistryKey.ENCHANTMENT);
                        Enchantment ench = enchReg.getOrThrow(NamespacedKey.minecraft(enchStr));
                        item = buildBook(ench, level, price);
                    } else if ("potion".equals(type)) {
                        String potionTypeStr = (String) map.get("potion_type");
                        Object amountObj = map.get("amount");
                        int amount = amountObj != null ? (Integer) amountObj : 1;
                        String name = (String) map.get("name");
                        org.bukkit.potion.PotionType potionType = org.bukkit.potion.PotionType.valueOf(potionTypeStr);
                        item = buildPotion(potionType, amount, name, price, currency);
                    }
                    if (item != null) items.add(item);
                } catch (Exception e) {
                    services.plugin().getLogger().warning("Failed to load item in shop " + shopId + ": " + e.getMessage());
                }
            }
            loadedShops.put(shopId, new ShopData(shopName, items));
        }
    }

    public void spawnNpc(Player player, String type) {
        spawnNpcAt(player.getLocation(), type);
        TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_b505be", "&aNPC успешно заспавнен!"));
    }

    public Villager spawnNpcAt(org.bukkit.Location loc, String type) {
        Villager npc = (Villager) loc.getWorld().spawnEntity(loc, EntityType.VILLAGER);
        npc.setAI(false);
        npc.setInvulnerable(true);
        npc.setCollidable(false);
        npc.getPersistentDataContainer().set(npcKey, PersistentDataType.STRING, type);

        String defaultName = switch (type) {
            case "1" -> "§aНачальный торговец";
            case "2" -> "§eДекоратор";
            case "3" -> "§dБиблиотекарь";
            case "4" -> "§6Фермер";
            case "5" -> "§bГид";
            case "6" -> "§cКазино (Рулетка и Слоты)";
            case "7" -> "§4Казино (Карты и Настолки)";
            case "8" -> "§5Ивент Магазин";
            case "9" -> "§d💎 Черный Рынок";
            case "10" -> "§8🔨 Кузнец Артефактов";
            default -> "§7Торговец";
        };
        String name = com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("npc.type_" + type + ".name", defaultName);
        npc.customName(net.kyori.adventure.text.Component.text(TextUtil.color(name)));
        npc.setCustomNameVisible(true);
        return npc;
    }

    @EventHandler
    public void onNpcInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager npc)) return;
        if (!npc.getPersistentDataContainer().has(npcKey, PersistentDataType.STRING)) return;

        event.setCancelled(true);
        String type = npc.getPersistentDataContainer().get(npcKey, PersistentDataType.STRING);
        Player p = event.getPlayer();

        switch (type) {
            case "5" -> sendGuideInfo(p);
            case "6" -> openVegasSlots(p);
            case "7" -> openVegasTables(p);
            case "10" -> services.blacksmithGui().open(p);
            default -> openShop(p, type, 0);
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
        String prefix = TextUtil.color(services.plugin().getConfig().getString("messages.prefix", "&8[&dChetCraft&8] &7"));
        TextUtil.send(p, prefix + "Приветствую, путник! Я местный гид.");
        TextUtil.send(p, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_3362f1", "&fНаш сервер — это выживание с уникальными предметами и экономикой."));
        TextUtil.send(p, "");
        TextUtil.send(p, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_e3949b", "&e&lПолезные команды:"));
        TextUtil.send(p, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_4f03c8", " &8• &a/menu &7- Открыть главное меню"));
        TextUtil.send(p, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_0bd677", " &8• &a/rewards &7- Ежедневные награды"));
        TextUtil.send(p, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_578872", " &8• &a/talents &7- Дерево Талантов"));
        TextUtil.send(p, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_aec4c6", " &8• &a/quests &7- Твои задания"));
        TextUtil.send(p, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_0eac8f", " &8• &a/spawn &7- Вернуться на спавн"));
        TextUtil.send(p, "");
        TextUtil.send(p, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_c3fe66", "&9&lНаш Discord: &nhttps://discord.gg/cheterin"));
        TextUtil.send(p, "");
    }

    // ==========================================
    // ЛОГИКА СТАНДАРТНЫХ МАГАЗИНОВ
    // ==========================================
    private void fillBorder(Inventory inv) {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.displayName(Component.empty());
        glass.setItemMeta(meta);
        for (int i = 0; i < inv.getSize(); i++) {
            if (i < 9 || i >= inv.getSize() - 9 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, glass);
            }
        }
    }

    private void openPaginatedShop(Player p, String type, String title, List<ItemStack> items, int page) {
        String displayTitle = TextUtil.color(title) + (page > 0 ? " (" + (page + 1) + ")" : "");
        Inventory inv = Bukkit.createInventory(new ShopHolder(type, page), 54, Component.text(displayTitle));
        fillBorder(inv);
        int[] slots = {10,11,12,13,14,15,16, 19,20,21,22,23,24,25, 28,29,30,31,32,33,34, 37,38,39,40,41,42,43};
        int maxItems = slots.length;
        int startIndex = page * maxItems;
        
        for (int i = 0; i < maxItems && startIndex + i < items.size(); i++) {
            inv.setItem(slots[i], items.get(startIndex + i));
        }

        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta m = prev.getItemMeta();
            m.displayName(Component.text(TextUtil.color("&e<- Предыдущая страница")));
            m.getPersistentDataContainer().set(new NamespacedKey(services.plugin(), "page_action"), PersistentDataType.STRING, "prev");
            prev.setItemMeta(m);
            inv.setItem(45, prev);
        }
        
        if (startIndex + maxItems < items.size()) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta m = next.getItemMeta();
            m.displayName(Component.text(TextUtil.color("&eСледующая страница ->")));
            m.getPersistentDataContainer().set(new NamespacedKey(services.plugin(), "page_action"), PersistentDataType.STRING, "next");
            next.setItemMeta(m);
            inv.setItem(53, next);
        }
        
        p.openInventory(inv);
    }

    private void openShop(Player p, String type, int page) {
        ShopData data = loadedShops.get(type);
        if (data == null) {
            TextUtil.send(p, "&cМагазин не найден в конфигурации!");
            return;
        }
        openPaginatedShop(p, type, data.name(), data.items(), page);
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
                    TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_3b1415", "&cЭтот режим находится в разработке и будет добавлен позже!"));
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                }
            }
            return;
        }

        // Обработка пагинации
        if (meta.getPersistentDataContainer().has(new NamespacedKey(services.plugin(), "page_action"), PersistentDataType.STRING)) {
            String action = meta.getPersistentDataContainer().get(new NamespacedKey(services.plugin(), "page_action"), PersistentDataType.STRING);
            ShopHolder sh = (ShopHolder) holder;
            if ("next".equals(action)) {
                openShop(player, sh.type(), sh.page() + 1);
            } else if ("prev".equals(action)) {
                openShop(player, sh.type(), sh.page() - 1);
            }
            return;
        }

        // Логика покупки в стандартных магазинах
        if (!meta.getPersistentDataContainer().has(priceKey, PersistentDataType.INTEGER)) return;

        int price = meta.getPersistentDataContainer().get(priceKey, PersistentDataType.INTEGER);
        String currency = meta.getPersistentDataContainer().getOrDefault(currencyKey, PersistentDataType.STRING, "coins");
        PlayerProfile profile = services.economy().profile(player.getUniqueId(), player.getName());

        if (currency.equals("events") && profile.getEventPoints() < price) {
            TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_023881", "&cНедостаточно Event Points!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        } else if (currency.equals("coins") && profile.getCoins() < price) {
            TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_61ab3a", "&cНедостаточно ❂!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        if (player.getInventory().firstEmpty() == -1) {
            TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_b1d79f", "&cИнвентарь заполнен!"));
            return;
        }

        if (currency.equals("events")) profile.setEventPoints(profile.getEventPoints() - price);
        else profile.setCoins(profile.getCoins() - price);

        services.store().requestSave();

        ItemStack giveItem = event.getCurrentItem().clone();
        ItemMeta giveMeta = giveItem.getItemMeta();

        // Восстанавливаем чистый предмет: для кастомных — пересоздаём из ItemRegistry,
        // для обычных материалов — просто убираем служебные данные магазина
        String customId = giveMeta.getPersistentDataContainer().get(
                new NamespacedKey(services.plugin(), "custom_id"),
                org.bukkit.persistence.PersistentDataType.STRING);
        if (customId == null) {
            customId = giveMeta.getPersistentDataContainer().get(
                    new NamespacedKey("astrasmp", "custom_id"),
                    org.bukkit.persistence.PersistentDataType.STRING);
        }

        if (customId != null) {
            // Кастомный предмет: пересоздаём из ItemRegistry, чтобы сохранить лор
            boolean restored = false;
            try {
                java.lang.reflect.Method m = ItemRegistry.class.getMethod(customId);
                giveItem = (ItemStack) m.invoke(null);
                restored = true;
            } catch (NoSuchMethodException ignored) {
                // relic/artifact — убираем только служебные строки
            } catch (Exception ignored) {}

            if (!restored) {
                List<Component> lore = giveMeta.hasLore()
                        ? new java.util.ArrayList<>(giveMeta.lore()) : new java.util.ArrayList<>();
                // Убираем 3 служебные строки магазина (пустая, цена, "Нажмите, чтобы купить")
                for (int i = 0; i < 3 && !lore.isEmpty(); i++) lore.remove(lore.size() - 1);
                giveMeta.lore(lore.isEmpty() ? null : lore);
                giveMeta.getPersistentDataContainer().remove(priceKey);
                giveMeta.getPersistentDataContainer().remove(currencyKey);
                giveItem.setItemMeta(giveMeta);
            }
        } else {
            // Обычный материал/книга/зелье — лор был только ценой
            giveMeta.lore(null);
            giveMeta.getPersistentDataContainer().remove(priceKey);
            giveMeta.getPersistentDataContainer().remove(currencyKey);
            giveItem.setItemMeta(giveMeta);
        }

        player.getInventory().addItem(giveItem);
        TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_c24556", "&aПокупка успешна!"));
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

    private ItemStack buildPotion(org.bukkit.potion.PotionType potionType, int amount, String name, int price, String currency) {
        ItemStack item = new ItemStack(Material.POTION, amount);
        org.bukkit.inventory.meta.PotionMeta meta = (org.bukkit.inventory.meta.PotionMeta) item.getItemMeta();
        meta.setBasePotionType(potionType);
        meta.displayName(Component.text(TextUtil.color(name)));
        List<Component> lore = new ArrayList<>();
        String currencyStr = currency.equals("events") ? "&dEvent Points" : "&e❂";
        lore.add(Component.text(TextUtil.color("§7Цена: " + price + " " + currencyStr)));
        meta.lore(lore);
        meta.getPersistentDataContainer().set(priceKey, PersistentDataType.INTEGER, price);
        meta.getPersistentDataContainer().set(currencyKey, PersistentDataType.STRING, currency);
        item.setItemMeta(meta);
        return item;
    }

    // Холдеры
    public record ShopHolder(String type, int page) implements InventoryHolder {
        @Override public @NotNull Inventory getInventory() { return Bukkit.createInventory(this, 54); }
    }
    private static class VegasHolder implements InventoryHolder { @Override public @NotNull Inventory getInventory() { return null; } }
}