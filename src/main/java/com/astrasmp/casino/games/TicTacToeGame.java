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
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class TicTacToeGame implements CasinoGame, Listener {
    private final CasinoService casinoService;
    private final Map<UUID, GameSession> sessions = new HashMap<>();

    public TicTacToeGame(CasinoService casinoService) {
        this.casinoService = casinoService;
        Bukkit.getPluginManager().registerEvents(this, casinoService.getPlugin());
    }

    @Override
    public void open(Player player) {
        if (!casinoService.startSession(player)) return;
        
        // Ставка 200 монет за партию
        if (!casinoService.processBet(player, 200)) {
            casinoService.endSession(player);
            return;
        }

        GameSession session = new GameSession();
        sessions.put(player.getUniqueId(), session);
        Inventory inv = Bukkit.createInventory(session, 36, Component.text("Vegas: Крестики-Нолики"));
        session.inventory = inv;
        renderUI(inv, session);
        player.openInventory(inv);
    }

    private void renderUI(Inventory inv, GameSession session) {
        inv.clear();

        // Сетка 3x3 (слоты 10-12, 19-21, 28-30)
        int[] slots = {10, 11, 12, 19, 20, 21, 28, 29, 30};
        for (int i = 0; i < 9; i++) {
            char cell = session.board[i];
            if (cell == 'X') inv.setItem(slots[i], buildItem(Material.RED_WOOL, "&c&lX"));
            else if (cell == 'O') inv.setItem(slots[i], buildItem(Material.BLUE_WOOL, "&b&lO"));
            else inv.setItem(slots[i], buildItem(Material.GRAY_STAINED_GLASS_PANE, "&7Ходить"));
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof GameSession)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        GameSession session = sessions.get(player.getUniqueId());
        if (session == null || session.turn != 'X') return;

        int[] slots = {10, 11, 12, 19, 20, 21, 28, 29, 30};
        for (int i = 0; i < 9; i++) {
            if (event.getRawSlot() == slots[i] && session.board[i] == ' ') {
                session.board[i] = 'X';
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                
                if (checkWin(session, 'X')) {
                    finishGame(player, session, "&a&lПОБЕДА!", 400);
                } else if (isBoardFull(session)) {
                    finishGame(player, session, "&e&lНИЧЬЯ", 200);
                } else {
                    session.turn = 'O';
                    renderUI(event.getInventory(), session);
                    serverMove(player, session, event.getInventory());
                }
                break;
            }
        }
    }

    private void serverMove(Player player, GameSession session, Inventory inv) {
        Bukkit.getScheduler().runTaskLater(casinoService.getPlugin(), () -> {
            List<Integer> empty = new ArrayList<>();
            for (int i = 0; i < 9; i++) if (session.board[i] == ' ') empty.add(i);
            
            if (!empty.isEmpty()) {
                int move = empty.get(new Random().nextInt(empty.size()));
                session.board[move] = 'O';
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);

                if (checkWin(session, 'O')) {
                    finishGame(player, session, "&c&lПОРАЖЕНИЕ", 0);
                } else if (isBoardFull(session)) {
                    finishGame(player, session, "&e&lНИЧЬЯ", 200);
                } else {
                    session.turn = 'X';
                    renderUI(inv, session);
                }
            }
        }, 20L);
    }

    private boolean checkWin(GameSession s, char p) {
        int[][] wins = {{0,1,2},{3,4,5},{6,7,8},{0,3,6},{1,4,7},{2,5,8},{0,4,8},{2,4,6}};
        for (int[] w : wins) if (s.board[w[0]] == p && s.board[w[1]] == p && s.board[w[2]] == p) return true;
        return false;
    }

    private boolean isBoardFull(GameSession s) {
        for (char c : s.board) if (c == ' ') return false;
        return true;
    }

    private void finishGame(Player player, GameSession session, String msg, int payout) {
        sessions.remove(player.getUniqueId());
        casinoService.endSession(player);
        if (payout > 0) casinoService.processPayout(player, payout);
        TextUtil.send(player, msg);
        player.closeInventory();
    }

    private ItemStack buildItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(TextUtil.color(name)));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof GameSession) {
            Player player = (Player) event.getPlayer();
            sessions.remove(player.getUniqueId());
            casinoService.endSession(player);
        }
    }

    private static class GameSession implements InventoryHolder {
        Inventory inventory;
        char[] board = {' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' '};
        char turn = 'X';
        @Override public @NotNull Inventory getInventory() { return inventory; }
    }
}