package com.astrasmp.casino.games;

import com.astrasmp.casino.CasinoGame;
import com.astrasmp.casino.CasinoService;
import com.astrasmp.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class ClassicGame implements CasinoGame, Listener, InventoryHolder {
    private final CasinoService casinoService;
    private final Random random = new Random();
    private final Map<UUID, Integer> rollingPlayers = new HashMap<>();

    public ClassicGame(CasinoService casinoService) {
        this.casinoService = casinoService;
        Bukkit.getPluginManager().registerEvents(this, casinoService.getPlugin());
    }

    @Override
    public void open(Player player) {
        if (!casinoService.startSession(player)) return;

        Inventory inv = Bukkit.createInventory(this, 27, Component.text("Vegas: Классика"));
        
        inv.setItem(10, buildItem(Material.IRON_NUGGET, "&fСтавка: 100 ❂"));
        inv.setItem(12, buildItem(Material.GOLD_NUGGET, "&eСтавка: 500 ❂"));
        inv.setItem(14, buildItem(Material.DIAMOND, "&bСтавка: 1000 ❂"));
        inv.setItem(16, buildItem(Material.EMERALD, "&aСтавка: 5000 ❂"));
        
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (rollingPlayers.containsKey(player.getUniqueId())) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        int bet = 0;
        switch (event.getRawSlot()) {
            case 10 -> bet = 100;
            case 12 -> bet = 500;
            case 14 -> bet = 1000;
            case 16 -> bet = 5000;
        }

        if (bet > 0) {
            startRoll(player, bet, event.getInventory());
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() == this) {
            Player player = (Player) event.getPlayer();
            rollingPlayers.remove(player.getUniqueId());
            casinoService.endSession(player);
        }
    }

    private void startRoll(Player player, int bet, Inventory inv) {
        if (!casinoService.processBet(player, bet)) return;

        rollingPlayers.put(player.getUniqueId(), bet);
        inv.clear();

        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 30;

            @Override
            public void run() {
                if (ticks >= maxTicks || !player.getOpenInventory().getTopInventory().equals(inv)) {
                    finishRoll(player, bet, inv);
                    this.cancel();
                    return;
                }

                Material[] glass = {Material.RED_STAINED_GLASS_PANE, Material.GREEN_STAINED_GLASS_PANE, Material.GRAY_STAINED_GLASS_PANE};
                inv.setItem(13, buildItem(glass[random.nextInt(glass.length)], "&7Крутим..."));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 1f);
                
                ticks++;
            }
        }.runTaskTimer(casinoService.getPlugin(), 0L, 2L);
    }

    private void finishRoll(Player player, int bet, Inventory inv) {
        if (!rollingPlayers.containsKey(player.getUniqueId())) return;
        rollingPlayers.remove(player.getUniqueId());

        int chance = random.nextInt(100);
        double multiplier;
        Material resultMat;
        String resultText;

        if (chance < 45) {
            multiplier = 0;
            resultMat = Material.RED_STAINED_GLASS_PANE;
            resultText = "&cПроигрыш";
        } else if (chance < 80) {
            multiplier = 1.5;
            resultMat = Material.YELLOW_STAINED_GLASS_PANE;
            resultText = "&eВыигрыш x1.5!";
        } else if (chance < 95) {
            multiplier = 2.0;
            resultMat = Material.GREEN_STAINED_GLASS_PANE;
            resultText = "&aВыигрыш x2.0!";
        } else {
            multiplier = 5.0;
            resultMat = Material.DIAMOND_BLOCK;
            resultText = "&b&lДЖЕКПОТ x5.0!";
        }

        inv.setItem(13, buildItem(resultMat, resultText));
        
        int payout = (int) (bet * multiplier);
        if (payout > 0) {
            casinoService.processPayout(player, payout);
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
    }

    private ItemStack buildItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(TextUtil.color(name)));
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return Bukkit.createInventory(null, 9);
    }
}