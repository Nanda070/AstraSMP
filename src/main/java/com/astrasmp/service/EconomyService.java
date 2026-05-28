package com.astrasmp.service;

import com.astrasmp.AstraSMPPlugin;
import com.astrasmp.model.PlayerProfile;
import com.astrasmp.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.*;

public final class EconomyService {
    private final AstraSMPPlugin plugin;
    private final DataStore store;
    private final Map<Material, Double> prices = new EnumMap<>(Material.class);

    private final List<Material> resourceItems = Arrays.asList(
            Material.COBBLESTONE, Material.COAL, Material.RAW_IRON, Material.IRON_INGOT,
            Material.RAW_GOLD, Material.GOLD_INGOT, Material.REDSTONE, Material.LAPIS_LAZULI,
            Material.QUARTZ, Material.AMETHYST_SHARD, Material.GLOWSTONE_DUST, Material.DIAMOND,
            Material.EMERALD, Material.ANCIENT_DEBRIS, Material.NETHERITE_SCRAP
    );

    private final List<Material> foodItems = Arrays.asList(
            Material.APPLE, Material.BREAD, Material.GOLDEN_APPLE,
            Material.CARROT, Material.POTATO, Material.PUMPKIN,
            Material.MELON_SLICE, Material.SWEET_BERRIES, Material.GLOW_BERRIES,
            Material.MUTTON, Material.RABBIT,
            Material.COD, Material.SALMON, Material.TROPICAL_FISH, Material.PUFFERFISH
    );

    public EconomyService(AstraSMPPlugin plugin, DataStore store) {
        this.plugin = plugin;
        this.store = store;
        loadPrices();
    }

    public void loadPrices() {
        prices.clear();
        var sec = plugin.getConfig().getConfigurationSection("economy.prices");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            try {
                prices.put(Material.valueOf(key.toUpperCase()), sec.getDouble(key));
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public List<Material> getResourceItems() { return resourceItems; }
    public List<Material> getFoodItems() { return foodItems; }
    public double getPrice(Material material) { return prices.getOrDefault(material, 0.0); }

    public PlayerProfile profile(UUID uuid, String name) { return store.profile(uuid.toString(), name); }
    public PlayerProfile getProfile(UUID uuid, String name) { return profile(uuid, name); }

    public long getBalance(UUID uuid) {
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        return store.profile(uuid.toString(), name == null ? "Unknown" : name).getCoins();
    }

    public void setBalance(UUID uuid, String name, long amount) {
        PlayerProfile p = store.profile(uuid.toString(), name);
        p.setCoins(amount);
        store.requestSave();
    }

    public void addBalance(UUID uuid, String name, long amount) {
        PlayerProfile p = store.profile(uuid.toString(), name);
        p.setCoins(p.getCoins() + Math.max(0L, amount));
        store.requestSave();
    }

    public boolean pay(Player from, Player to, long amount) {
        if (amount <= 0) return false;
        PlayerProfile sender = profile(from.getUniqueId(), from.getName());
        if (sender.getCoins() < amount) return false;
        PlayerProfile receiver = profile(to.getUniqueId(), to.getName());
        sender.setCoins(sender.getCoins() - amount);
        receiver.setCoins(receiver.getCoins() + amount);
        store.requestSave();
        return true;
    }

    // --- МЕТОДЫ ПРОДАЖИ С ПОДДЕРЖКОЙ ГИЛЬДИЙ ---

    public long sellHand(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) return 0;

        double price = getPrice(item.getType());
        if (price <= 0) return 0;

        long income = Math.round(price * item.getAmount());

        // Внедряем налог
        long tax = plugin.getServices().guilds().applyTax(player, income);
        long finalIncome = income - tax;

        addBalance(player.getUniqueId(), player.getName(), finalIncome);
        player.getInventory().setItemInMainHand(null);

        if (tax > 0) {
            TextUtil.send(player, "&7Налог гильдии: &c-" + tax + " ❂ &7(отправлено в казну)");
        }

        plugin.getServices().quests().checkProgress(player, 3, 1);
        return finalIncome;
    }

    public long sellItem(Player player, Material material) {
        double price = getPrice(material);
        if (price <= 0) return 0;

        int totalAmount = 0;
        ItemStack[] contents = player.getInventory().getContents();

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == material) {
                if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) continue;
                totalAmount += item.getAmount();
                player.getInventory().setItem(i, null);
            }
        }

        if (totalAmount > 0) {
            long income = (long) (totalAmount * price);

            // Внедряем налог
            long tax = plugin.getServices().guilds().applyTax(player, income);
            long finalIncome = income - tax;

            PlayerProfile profile = profile(player.getUniqueId(), player.getName());
            profile.setCoins(profile.getCoins() + finalIncome);

            if (tax > 0) {
                TextUtil.send(player, "&7Налог гильдии: &c-" + tax + " ❂");
            }

            store.requestSave();
            plugin.getServices().quests().checkProgress(player, 3, 1);

            return finalIncome;
        }

        return 0;
    }

    public long sellInventory(Player player) {
        long totalIncome = 0;
        boolean soldAnything = false;

        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType().isAir()) continue;
            if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) continue;

            double price = getPrice(item.getType());
            if (price <= 0) continue;

            totalIncome += Math.round(price * item.getAmount());
            item.setAmount(0);
            soldAnything = true;
        }

        if (soldAnything) {
            // Внедряем налог
            long tax = plugin.getServices().guilds().applyTax(player, totalIncome);
            long finalIncome = totalIncome - tax;

            addBalance(player.getUniqueId(), player.getName(), finalIncome);

            if (tax > 0) {
                TextUtil.send(player, "&7Общий налог гильдии: &c-" + tax + " ❂");
            }

            plugin.getServices().quests().checkProgress(player, 3, 1);
            return finalIncome;
        }

        return 0;
    }
}