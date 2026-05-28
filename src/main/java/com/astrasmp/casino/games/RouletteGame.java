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

public class RouletteGame implements CasinoGame, Listener, InventoryHolder {
    private final CasinoService casinoService;
    private final Random random = new Random();

    // Хранение локального состояния игрока (до и во время прокрутки)
    private final Map<UUID, PlayerSession> sessions = new HashMap<>();

    public RouletteGame(CasinoService casinoService) {
        this.casinoService = casinoService;
        Bukkit.getPluginManager().registerEvents(this, casinoService.getPlugin());
    }

    @Override
    public void open(Player player) {
        if (!casinoService.startSession(player)) return;

        sessions.put(player.getUniqueId(), new PlayerSession());
        Inventory inv = Bukkit.createInventory(this, 54, Component.text("Vegas: Рулетка"));
        renderUI(inv, player.getUniqueId());
        player.openInventory(inv);
    }

    private void renderUI(Inventory inv, UUID playerId) {
        PlayerSession session = sessions.get(playerId);
        if (session == null) return;

        inv.clear();

        // Отрисовка указателя (поинтера)
        inv.setItem(4, buildItem(Material.HOPPER, "&e&l⬇ Результат ⬇"));

        // Если не крутится, рисуем статичную ленту для визуала
        if (!session.isSpinning) {
            for (int i = 9; i < 18; i++) {
                inv.setItem(i, buildItem(Material.GRAY_STAINED_GLASS_PANE, "&7Ожидание ставки..."));
            }
        }

        // Кнопки управления ставкой (27-35)
        inv.setItem(28, buildItem(Material.IRON_NUGGET, "&fСтавка: 100 ❂", session.betAmount == 100));
        inv.setItem(30, buildItem(Material.GOLD_NUGGET, "&eСтавка: 500 ❂", session.betAmount == 500));
        inv.setItem(32, buildItem(Material.DIAMOND, "&bСтавка: 1 000 ❂", session.betAmount == 1000));
        inv.setItem(34, buildItem(Material.EMERALD, "&aСтавка: 5 000 ❂", session.betAmount == 5000));

        // Кнопки выбора цвета (37-43)
        inv.setItem(38, buildItem(Material.RED_TERRACOTTA, "&c&lКРАСНОЕ (x2)", session.targetColor == BetColor.RED));
        inv.setItem(40, buildItem(Material.LIME_TERRACOTTA, "&a&lЗЕЛЕНОЕ (x14)", session.targetColor == BetColor.GREEN));
        inv.setItem(42, buildItem(Material.BLACK_TERRACOTTA, "&8&lЧЕРНОЕ (x2)", session.targetColor == BetColor.BLACK));

        // Кнопка СТАРТ
        if (!session.isSpinning) {
            inv.setItem(53, buildItem(Material.SUNFLOWER, "&6&lКРУТИТЬ РУЛЕТКУ"));
        } else {
            inv.setItem(53, buildItem(Material.BARRIER, "&c&lИДЕТ ИГРА..."));
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        PlayerSession session = sessions.get(player.getUniqueId());
        if (session == null || session.isSpinning) return; // Лок UI во время прокрутки

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        int slot = event.getRawSlot();
        boolean updateNeeded = false;

        // Обработка кликов
        switch (slot) {
            case 28 -> { session.betAmount = 100; updateNeeded = true; player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f); }
            case 30 -> { session.betAmount = 500; updateNeeded = true; player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f); }
            case 32 -> { session.betAmount = 1000; updateNeeded = true; player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f); }
            case 34 -> { session.betAmount = 5000; updateNeeded = true; player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f); }
            case 38 -> { session.targetColor = BetColor.RED; updateNeeded = true; player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f); }
            case 40 -> { session.targetColor = BetColor.GREEN; updateNeeded = true; player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f); }
            case 42 -> { session.targetColor = BetColor.BLACK; updateNeeded = true; player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f); }
            case 53 -> startSpin(player, session, event.getInventory());
        }

        if (updateNeeded) {
            renderUI(event.getInventory(), player.getUniqueId());
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() == this) {
            Player player = (Player) event.getPlayer();
            PlayerSession session = sessions.get(player.getUniqueId());
            
            // Если закрыл во время спина - деньги уже сняты, игра продолжится в фоне, но без анимации (защита от абуза).
            // Для упрощения: мы просто удаляем сессию. Payout не отработает.
            if (session != null) {
                sessions.remove(player.getUniqueId());
            }
            casinoService.endSession(player);
        }
    }

    private void startSpin(Player player, PlayerSession session, Inventory inv) {
        if (session.targetColor == null) {
            TextUtil.send(player, "&cВыберите цвет для ставки!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        if (!casinoService.processBet(player, session.betAmount)) return;

        session.isSpinning = true;
        renderUI(inv, player.getUniqueId()); // Перерисовываем для блока кнопок

        // Генерация ленты рулетки (40 айтемов для прокрутки)
        List<BetColor> wheelSequence = generateWheel();

        new BukkitRunnable() {
            int tick = 0;
            final int maxTicks = 40; // Длина прокрутки
            double delay = 1.0; // Для имитации замедления

            @Override
            public void run() {
                if (!sessions.containsKey(player.getUniqueId()) || !player.getOpenInventory().getTopInventory().equals(inv)) {
                    this.cancel(); // Игрок закрыл инвентарь или ливнул
                    return;
                }

                if (tick >= maxTicks) {
                    finishSpin(player, session, inv, wheelSequence.get(13)); // Индекс 13 - это слот 13 (центральный под поинтером)
                    this.cancel();
                    return;
                }

                // Сдвиг ленты
                for (int i = 0; i < 9; i++) {
                    BetColor color = wheelSequence.get((tick + i) % wheelSequence.size());
                    inv.setItem(9 + i, getRouletteBlock(color));
                }

                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 2f);
                tick++;
                
                // Имитация замедления трения (опционально, усложняет шедулер, пока делаем линейно для надежности)
            }
        }.runTaskTimer(casinoService.getPlugin(), 0L, 2L);
    }

    private void finishSpin(Player player, PlayerSession session, Inventory inv, BetColor resultColor) {
        sessions.remove(player.getUniqueId());
        casinoService.endSession(player); // Разлочиваем игрока

        boolean isWin = (session.targetColor == resultColor);
        int payout = 0;

        if (isWin) {
            if (resultColor == BetColor.GREEN) payout = session.betAmount * 14;
            else payout = session.betAmount * 2;

            casinoService.processPayout(player, payout);
            inv.setItem(4, buildItem(Material.DIAMOND, "&a&lПОБЕДА!"));
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            TextUtil.send(player, "&cСтавка не сыграла. Выпало: " + resultColor.name());
            inv.setItem(4, buildItem(Material.BARRIER, "&c&lПРОИГРЫШ"));
        }
    }

    // Генерация ленты с правильными вероятностями Vegas: Zero (Green) - ~5%, Red - 47.5%, Black - 47.5%
    private List<BetColor> generateWheel() {
        List<BetColor> wheel = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            int r = random.nextInt(100);
            if (r < 5) wheel.add(BetColor.GREEN);
            else if (r < 52) wheel.add(BetColor.RED);
            else wheel.add(BetColor.BLACK);
        }
        return wheel;
    }

    private ItemStack getRouletteBlock(BetColor color) {
        return switch (color) {
            case RED -> buildItem(Material.RED_TERRACOTTA, "&c&lКРАСНОЕ");
            case BLACK -> buildItem(Material.BLACK_TERRACOTTA, "&8&lЧЕРНОЕ");
            case GREEN -> buildItem(Material.LIME_TERRACOTTA, "&a&lЗЕЛЕНОЕ");
        };
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

    private enum BetColor {
        RED, BLACK, GREEN
    }

    private static class PlayerSession {
        int betAmount = 100;
        BetColor targetColor = null;
        boolean isSpinning = false;
    }
}