package com.astrasmp.casino.games;

import com.astrasmp.casino.CasinoGame;
import com.astrasmp.casino.CasinoService;
import com.astrasmp.casino.cards.Deck;
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

public class BlackjackGame implements CasinoGame, Listener, InventoryHolder {
    private final CasinoService casinoService;
    private final Map<UUID, GameSession> sessions = new HashMap<>();

    public BlackjackGame(CasinoService casinoService) {
        this.casinoService = casinoService;
        Bukkit.getPluginManager().registerEvents(this, casinoService.getPlugin());
    }

    @Override
    public void open(Player player) {
        if (!casinoService.startSession(player)) return;

        sessions.put(player.getUniqueId(), new GameSession());
        Inventory inv = Bukkit.createInventory(this, 54, Component.text("Vegas: Блэкджек"));
        renderBettingUI(inv, player.getUniqueId());
        player.openInventory(inv);
    }

    private void renderBettingUI(Inventory inv, UUID playerId) {
        GameSession session = sessions.get(playerId);
        if (session == null) return;

        inv.clear();
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, buildItem(Material.BLACK_STAINED_GLASS_PANE, " "));
        }

        inv.setItem(20, buildItem(Material.IRON_NUGGET, "&fСтавка: 100 ❂", session.betAmount == 100));
        inv.setItem(21, buildItem(Material.GOLD_NUGGET, "&eСтавка: 500 ❂", session.betAmount == 500));
        inv.setItem(22, buildItem(Material.DIAMOND, "&bСтавка: 1 000 ❂", session.betAmount == 1000));
        inv.setItem(23, buildItem(Material.EMERALD, "&aСтавка: 5 000 ❂", session.betAmount == 5000));

        inv.setItem(31, buildItem(Material.PAPER, "&a&lРАЗДАТЬ КАРТЫ"));
    }

    private void renderPlayingUI(Inventory inv, Player player, GameSession session) {
        inv.clear();

        // Рендер карт дилера (верхний ряд)
        int dealerScore = session.isDealerTurn ? Deck.calculateScore(session.dealerHand) : session.dealerHand.get(0).rank().value;
        inv.setItem(4, buildItem(Material.SKELETON_SKULL, "&cДилер &7(" + dealerScore + ")"));
        
        for (int i = 0; i < session.dealerHand.size(); i++) {
            if (i == 1 && !session.isDealerTurn) {
                inv.setItem(9 + i, buildItem(Material.MAP, "&8[Скрытая карта]"));
            } else {
                Deck.Card card = session.dealerHand.get(i);
                inv.setItem(9 + i, buildItem(Material.PAPER, card.getName()));
            }
        }

        // Рендер карт игрока (нижний ряд)
        int playerScore = Deck.calculateScore(session.playerHand);
        inv.setItem(40, buildItem(Material.PLAYER_HEAD, "&aВы &7(" + playerScore + ")"));

        for (int i = 0; i < session.playerHand.size(); i++) {
            Deck.Card card = session.playerHand.get(i);
            inv.setItem(27 + i, buildItem(Material.PAPER, card.getName()));
        }

        // Кнопки управления (только если ход игрока)
        if (!session.isDealerTurn) {
            inv.setItem(48, buildItem(Material.LIME_STAINED_GLASS_PANE, "&a&lВЗЯТЬ (Hit)"));
            inv.setItem(50, buildItem(Material.RED_STAINED_GLASS_PANE, "&c&lХВАТИТ (Stand)"));
        } else {
            inv.setItem(49, buildItem(Material.BARRIER, "&c&lПОДВЕДЕНИЕ ИТОГОВ..."));
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        GameSession session = sessions.get(player.getUniqueId());
        if (session == null) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        int slot = event.getRawSlot();

        if (session.state == GameState.BETTING) {
            boolean update = false;
            switch (slot) {
                case 20 -> { session.betAmount = 100; update = true; player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f); }
                case 21 -> { session.betAmount = 500; update = true; player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f); }
                case 22 -> { session.betAmount = 1000; update = true; player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f); }
                case 23 -> { session.betAmount = 5000; update = true; player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f); }
                case 31 -> dealInitialCards(player, session, event.getInventory());
            }
            if (update) renderBettingUI(event.getInventory(), player.getUniqueId());
        } else if (session.state == GameState.PLAYING && !session.isDealerTurn) {
            switch (slot) {
                case 48 -> playerHit(player, session, event.getInventory());
                case 50 -> playerStand(player, session, event.getInventory());
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() == this) {
            Player player = (Player) event.getPlayer();
            GameSession session = sessions.get(player.getUniqueId());

            if (session != null && session.state == GameState.PLAYING) {
                // Игрок ливнул во время активной раздачи. Ставка сгорает.
                sessions.remove(player.getUniqueId());
            } else if (session != null && session.state == GameState.BETTING) {
                sessions.remove(player.getUniqueId());
            }
            casinoService.endSession(player);
        }
    }

    private void dealInitialCards(Player player, GameSession session, Inventory inv) {
        if (!casinoService.processBet(player, session.betAmount)) return;

        session.state = GameState.PLAYING;
        session.deck = new Deck();
        
        session.playerHand.add(session.deck.draw());
        session.dealerHand.add(session.deck.draw());
        session.playerHand.add(session.deck.draw());
        session.dealerHand.add(session.deck.draw());

        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
        
        if (Deck.calculateScore(session.playerHand) == 21) {
            // Блэкджек со старта
            playerStand(player, session, inv);
        } else {
            renderPlayingUI(inv, player, session);
        }
    }

    private void playerHit(Player player, GameSession session, Inventory inv) {
        session.playerHand.add(session.deck.draw());
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);

        int score = Deck.calculateScore(session.playerHand);
        if (score > 21) {
            finishGame(player, session, inv, GameResult.BUST);
        } else if (score == 21) {
            playerStand(player, session, inv);
        } else {
            renderPlayingUI(inv, player, session);
        }
    }

    private void playerStand(Player player, GameSession session, Inventory inv) {
        session.isDealerTurn = true;
        renderPlayingUI(inv, player, session); // Открываем скрытую карту
        
        // Логика дилера (тянет до 17)
        int playerScore = Deck.calculateScore(session.playerHand);
        if (playerScore <= 21) {
            while (Deck.calculateScore(session.dealerHand) < 17) {
                session.dealerHand.add(session.deck.draw());
            }
        }

        int dealerScore = Deck.calculateScore(session.dealerHand);
        
        GameResult result;
        if (playerScore > 21) {
            result = GameResult.BUST;
        } else if (dealerScore > 21 || playerScore > dealerScore) {
            result = (playerScore == 21 && session.playerHand.size() == 2) ? GameResult.BLACKJACK : GameResult.WIN;
        } else if (playerScore == dealerScore) {
            result = GameResult.PUSH;
        } else {
            result = GameResult.LOSE;
        }

        finishGame(player, session, inv, result);
    }

    private void finishGame(Player player, GameSession session, Inventory inv, GameResult result) {
        sessions.remove(player.getUniqueId());
        casinoService.endSession(player);
        renderPlayingUI(inv, player, session); // Финальный рендер стола

        int payout = 0;
        switch (result) {
            case BLACKJACK -> {
                payout = (int) (session.betAmount * 2.5); // x2.5 выплата за натуральный блэкджек
                inv.setItem(49, buildItem(Material.DIAMOND_BLOCK, "&b&lБЛЭКДЖЕК! Выплата x2.5"));
            }
            case WIN -> {
                payout = session.betAmount * 2;
                inv.setItem(49, buildItem(Material.EMERALD_BLOCK, "&a&lПОБЕДА!"));
            }
            case PUSH -> {
                payout = session.betAmount; // Возврат ставки
                inv.setItem(49, buildItem(Material.GOLD_BLOCK, "&e&lНИЧЬЯ (Возврат)"));
            }
            case LOSE, BUST -> {
                inv.setItem(49, buildItem(Material.REDSTONE_BLOCK, "&c&lПРОИГРЫШ"));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }
        }

        if (payout > 0) {
            casinoService.processPayout(player, payout);
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
                meta.lore(List.of(Component.text(TextUtil.color("&a► Выбрано"))));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return Bukkit.createInventory(null, 9);
    }

    private enum GameState { BETTING, PLAYING }
    private enum GameResult { WIN, LOSE, PUSH, BUST, BLACKJACK }

    private static class GameSession {
        GameState state = GameState.BETTING;
        int betAmount = 100;
        boolean isDealerTurn = false;
        Deck deck;
        List<Deck.Card> playerHand = new ArrayList<>();
        List<Deck.Card> dealerHand = new ArrayList<>();
    }
}