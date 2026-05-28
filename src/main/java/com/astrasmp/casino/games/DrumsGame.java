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

import java.util.*;

public class DrumsGame implements CasinoGame, Listener, InventoryHolder {
    private final CasinoService casinoService;
    private final Random random = new Random();
    private final Map<UUID, PlayerSession> sessions = new HashMap<>();

    // Символы для барабанов. Чем выше индекс, тем "дороже" символ, но мы будем роллить их случайно.
    private final Material[] symbols = {
            Material.COAL_BLOCK, 
            Material.IRON_BLOCK, 
            Material.GOLD_BLOCK, 
            Material.EMERALD_BLOCK, 
            Material.DIAMOND_BLOCK
    };

    public DrumsGame(CasinoService casinoService) {
        this.casinoService = casinoService;
        Bukkit.getPluginManager().registerEvents(this, casinoService.getPlugin());
    }

    @Override
    public void open(Player player) {
        if (!casinoService.startSession(player)) return;

        sessions.put(player.getUniqueId(), new PlayerSession());
        Inventory inv = Bukkit.createInventory(this, 36, Component.text("Vegas: Барабаны"));
        renderUI(inv, player.getUniqueId());
        player.openInventory(inv);
    }

    private void renderUI(Inventory inv, UUID playerId) {
        PlayerSession session = sessions.get(playerId);
        if (session == null) return;

        inv.clear();

        // Декор
        for (int i = 0; i < 9; i++) inv.setItem(i, buildItem(Material.BLACK_STAINED_GLASS_PANE, " "));
        for (int i = 18; i < 27; i++) inv.setItem(i, buildItem(Material.BLACK_STAINED_GLASS_PANE, " "));
        inv.setItem(10, buildItem(Material.GRAY_STAINED_GLASS_PANE, " "));
        inv.setItem(12, buildItem(Material.GRAY_STAINED_GLASS_PANE, " "));
        inv.setItem(14, buildItem(Material.GRAY_STAINED_GLASS_PANE, " "));
        inv.setItem(16, buildItem(Material.GRAY_STAINED_GLASS_PANE, " "));

        // Если не крутится, ставим дефолтные символы
        if (!session.isSpinning) {
            inv.setItem(11, buildItem(Material.BELL, "&7Барабан 1"));
            inv.setItem(13, buildItem(Material.BELL, "&7Барабан 2"));
            inv.setItem(15, buildItem(Material.BELL, "&7Барабан 3"));
        }

        // Кнопки ставок
        inv.setItem(28, buildItem(Material.IRON_NUGGET, "&fСтавка: 100 ❂", session.betAmount == 100));
        inv.setItem(29, buildItem(Material.GOLD_NUGGET, "&eСтавка: 500 ❂", session.betAmount == 500));
        inv.setItem(30, buildItem(Material.DIAMOND, "&bСтавка: 1 000 ❂", session.betAmount == 1000));
        inv.setItem(31, buildItem(Material.EMERALD, "&aСтавка: 5 000 ❂", session.betAmount == 5000));

        // Кнопка СТАРТ
        if (!session.isSpinning) {
            inv.setItem(34, buildItem(Material.LEVER, "&6&lДЕРНУТЬ РУЧКУ"));
        } else {
            inv.setItem(34, buildItem(Material.BARRIER, "&c&lКРУТИТСЯ..."));
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        PlayerSession session = sessions.get(player.getUniqueId());
        if (session == null || session.isSpinning) return; 

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        boolean updateNeeded = false;
        switch (event.getRawSlot()) {
            case 28 -> { session.betAmount = 100; updateNeeded = true; player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f); }
            case 29 -> { session.betAmount = 500; updateNeeded = true; player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f); }
            case 30 -> { session.betAmount = 1000; updateNeeded = true; player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f); }
            case 31 -> { session.betAmount = 5000; updateNeeded = true; player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f); }
            case 34 -> startSpin(player, session, event.getInventory());
        }

        if (updateNeeded) {
            renderUI(event.getInventory(), player.getUniqueId());
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() == this) {
            Player player = (Player) event.getPlayer();
            if (sessions.containsKey(player.getUniqueId())) {
                sessions.remove(player.getUniqueId());
            }
            casinoService.endSession(player);
        }
    }

    private void startSpin(Player player, PlayerSession session, Inventory inv) {
        if (!casinoService.processBet(player, session.betAmount)) return;

        session.isSpinning = true;
        renderUI(inv, player.getUniqueId());

        new BukkitRunnable() {
            int ticks = 0;
            final int stopReel1 = 20;  // Остановка первого барабана через 1 сек
            final int stopReel2 = 40;  // Второго через 2 сек
            final int stopReel3 = 60;  // Третьего через 3 сек
            
            Material r1 = null;
            Material r2 = null;
            Material r3 = null;

            @Override
            public void run() {
                if (!sessions.containsKey(player.getUniqueId()) || !player.getOpenInventory().getTopInventory().equals(inv)) {
                    this.cancel();
                    return;
                }

                // Вращение барабанов
                if (ticks < stopReel1) {
                    r1 = symbols[random.nextInt(symbols.length)];
                    inv.setItem(11, buildItem(r1, "&7Крутим..."));
                } else if (ticks == stopReel1) {
                    player.playSound(player.getLocation(), Sound.BLOCK_PISTON_CONTRACT, 1f, 1f);
                }

                if (ticks < stopReel2) {
                    r2 = symbols[random.nextInt(symbols.length)];
                    inv.setItem(13, buildItem(r2, "&7Крутим..."));
                } else if (ticks == stopReel2) {
                    player.playSound(player.getLocation(), Sound.BLOCK_PISTON_CONTRACT, 1f, 1f);
                }

                if (ticks < stopReel3) {
                    r3 = symbols[random.nextInt(symbols.length)];
                    inv.setItem(15, buildItem(r3, "&7Крутим..."));
                } else if (ticks == stopReel3) {
                    player.playSound(player.getLocation(), Sound.BLOCK_PISTON_CONTRACT, 1f, 1f);
                }

                if (ticks >= stopReel3) {
                    finishSpin(player, session, inv, r1, r2, r3);
                    this.cancel();
                    return;
                }

                // Звук трещотки
                if (ticks % 3 == 0) {
                    player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_DISPENSE, 0.3f, 2f);
                }
                ticks++;
            }
        }.runTaskTimer(casinoService.getPlugin(), 0L, 1L);
    }

    private void finishSpin(Player player, PlayerSession session, Inventory inv, Material r1, Material r2, Material r3) {
        sessions.remove(player.getUniqueId());
        casinoService.endSession(player); 

        // Имена для красивого вывода
        inv.setItem(11, buildItem(r1, "&aБарабан 1"));
        inv.setItem(13, buildItem(r2, "&aБарабан 2"));
        inv.setItem(15, buildItem(r3, "&aБарабан 3"));

        double multiplier = 0;

        // Математика выплат
        if (r1 == r2 && r2 == r3) { // 3 совпадения
            if (r1 == Material.DIAMOND_BLOCK) multiplier = 10.0; // Джекпот
            else if (r1 == Material.EMERALD_BLOCK) multiplier = 7.0;
            else if (r1 == Material.GOLD_BLOCK) multiplier = 5.0;
            else if (r1 == Material.IRON_BLOCK) multiplier = 3.0;
            else multiplier = 2.0;
        } else if (r1 == r2 || r2 == r3 || r1 == r3) { // 2 совпадения
            multiplier = 1.2; 
        }

        if (multiplier > 0) {
            int payout = (int) (session.betAmount * multiplier);
            casinoService.processPayout(player, payout);
            inv.setItem(34, buildItem(Material.DIAMOND, "&a&lВЫИГРЫШ x" + multiplier));
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            TextUtil.send(player, "&cКомбинация не собрана.");
            inv.setItem(34, buildItem(Material.BARRIER, "&c&lПРОИГРЫШ"));
        }
    }

    private ItemStack buildItem(Material mat, String name) {
        return buildItem(mat, name, false);
    }

    private ItemStack buildItem(Material mat, String name, boolean isSelected) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(TextUtil.color(name)));
            if (isSelected) {
                List<Component> lore = new ArrayList<>();
                lore.add(Component.text(TextUtil.color("&a► Выбрано")));
                meta.lore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return Bukkit.createInventory(null, 9);
    }

    private static class PlayerSession {
        int betAmount = 100;
        boolean isSpinning = false;
    }
}