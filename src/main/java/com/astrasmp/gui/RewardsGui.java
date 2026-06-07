package com.astrasmp.gui;

import com.astrasmp.AstraSMPPlugin;
import com.astrasmp.model.PlayerProfile;
import com.astrasmp.service.ServiceManager;
import com.astrasmp.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.util.List;

public class RewardsGui implements Listener {
    private final AstraSMPPlugin plugin;
    private final ServiceManager services;

    public RewardsGui(AstraSMPPlugin plugin, ServiceManager services) {
        this.plugin = plugin;
        this.services = services;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public record RewardsHolder() implements InventoryHolder {
        @Override
        public @NotNull Inventory getInventory() {
            return Bukkit.createInventory(null, 9);
        }
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(new RewardsHolder(), 54, Component.text(TextUtil.color("&8Ежедневные награды")));
        
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        filler.editMeta(meta -> meta.displayName(Component.empty()));
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);

        PlayerProfile profile = services.store().profile(player.getUniqueId().toString(), player.getName());
        int currentDay = profile.getDailyRewardDay(); // 1..30
        int lastClaimDay = profile.getTalentLevel("last_claim_day");
        int todayEpoch = (int) LocalDate.now().toEpochDay();
        boolean canClaimToday = lastClaimDay != todayEpoch;

        int[] slots = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43,
            49, 50
        };

        for (int i = 0; i < 30; i++) {
            int day = i + 1;
            int slot = slots[i];

            Material mat = Material.MINECART;
            if (day % 7 == 0) mat = Material.CHEST_MINECART;
            if (day == 30) mat = Material.ENDER_CHEST;

            ItemStack item = new ItemStack(mat);
            final int fDay = day;
            item.editMeta(meta -> {
                String status = "&7[ Ожидает ]";
                if (fDay < currentDay) status = "&a[ Забрано ]";
                else if (fDay == currentDay) {
                    status = canClaimToday ? "&e[ Нажмите, чтобы забрать! ]" : "&c[ Приходите завтра ]";
                }

                meta.displayName(Component.text(TextUtil.color("&6&lДень " + fDay)));
                meta.lore(List.of(
                        Component.text(TextUtil.color("&fНаграда: &e" + getRewardDesc(fDay))),
                        Component.text(""),
                        Component.text(TextUtil.color(status))
                ));
            });

            if (day < currentDay) {
                item = item.withType(Material.MINECART);
                item.editMeta(meta -> meta.displayName(Component.text(TextUtil.color("&a&lДень " + fDay + " (Забрано)"))));
            } else if (day == currentDay && canClaimToday) {
                // Highlight
                item.editMeta(meta -> meta.setEnchantmentGlintOverride(true));
            }

            inv.setItem(slot, item);
        }

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1f, 1f);
    }

    private String getRewardDesc(int day) {
        long baseCoins = plugin.getConfig().getLong("rewards.base-coins", 500L);
        long weeklyCoins = plugin.getConfig().getLong("rewards.weekly-coins", 10000L);
        int weeklyEp = plugin.getConfig().getInt("rewards.weekly-ep", 100);
        long monthlyCoins = plugin.getConfig().getLong("rewards.monthly-coins", 50000L);
        int monthlyEp = plugin.getConfig().getInt("rewards.monthly-ep", 1000);

        List<String> items = plugin.getConfig().getStringList("rewards.base-items");
        
        if (day == 30) {
            items = plugin.getConfig().getStringList("rewards.monthly-items");
            return monthlyCoins + " ❂, " + monthlyEp + " EP и " + items.size() + " предм.";
        }
        if (day % 7 == 0) {
            items = plugin.getConfig().getStringList("rewards.weekly-items");
            return weeklyCoins + " ❂, " + weeklyEp + " EP и " + items.size() + " предм.";
        }
        return (day * baseCoins) + " ❂ и " + items.size() + " предм.";
    }

    private List<ItemStack> parseItems(List<String> list) {
        List<ItemStack> items = new java.util.ArrayList<>();
        for (String s : list) {
            String[] parts = s.split(":");
            if (parts.length == 2) {
                try {
                    Material mat = Material.valueOf(parts[0].toUpperCase());
                    int amount = Integer.parseInt(parts[1]);
                    items.add(new ItemStack(mat, amount));
                } catch (Exception ignored) {}
            }
        }
        return items;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof RewardsHolder)) return;
        event.setCancelled(true);
        
        Player player = (Player) event.getWhoClicked();
        PlayerProfile profile = services.store().profile(player.getUniqueId().toString(), player.getName());
        
        int currentDay = profile.getDailyRewardDay();
        int lastClaimDay = profile.getTalentLevel("last_claim_day");
        int todayEpoch = (int) LocalDate.now().toEpochDay();
        boolean canClaimToday = lastClaimDay != todayEpoch;

        int[] slots = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43,
            49, 50
        };

        int clickedDay = -1;
        for (int i = 0; i < 30; i++) {
            if (event.getSlot() == slots[i]) {
                clickedDay = i + 1;
                break;
            }
        }

        if (clickedDay == currentDay && canClaimToday) {
            claimReward(player, profile, clickedDay);
            profile.setTalentLevel("last_claim_day", todayEpoch);
            profile.setDailyRewardDay(currentDay == 30 ? 1 : currentDay + 1);
            services.store().requestSave();
            
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            player.closeInventory();
            open(player); // Reopen to refresh
        } else if (clickedDay != -1) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
    }

    private void claimReward(Player player, PlayerProfile profile, int day) {
        long baseCoins = plugin.getConfig().getLong("rewards.base-coins", 500L);
        long weeklyCoins = plugin.getConfig().getLong("rewards.weekly-coins", 10000L);
        int weeklyEp = plugin.getConfig().getInt("rewards.weekly-ep", 100);
        long monthlyCoins = plugin.getConfig().getLong("rewards.monthly-coins", 50000L);
        int monthlyEp = plugin.getConfig().getInt("rewards.monthly-ep", 1000);

        List<String> itemStrings = plugin.getConfig().getStringList("rewards.base-items");
        long coins = day * baseCoins;
        int ep = 0;
        
        if (day == 30) {
            coins = monthlyCoins;
            ep = monthlyEp;
            itemStrings = plugin.getConfig().getStringList("rewards.monthly-items");
        } else if (day % 7 == 0) {
            coins = weeklyCoins;
            ep = weeklyEp;
            itemStrings = plugin.getConfig().getStringList("rewards.weekly-items");
        }

        profile.setCoins(profile.getCoins() + coins);
        if (ep > 0) profile.setEventPoints(profile.getEventPoints() + ep);

        List<ItemStack> itemsToGive = parseItems(itemStrings);
        for (ItemStack item : itemsToGive) {
            if (!player.getInventory().addItem(item).isEmpty()) {
                player.getWorld().dropItem(player.getLocation(), item);
            }
        }

        String msg = "&aВы забрали ежедневную награду (День " + day + "): &e" + coins + " ❂";
        if (ep > 0) msg += " &fи &d" + ep + " EP";
        if (!itemsToGive.isEmpty()) msg += " &a+Предметы!";
        TextUtil.send(player, msg);
    }
}
