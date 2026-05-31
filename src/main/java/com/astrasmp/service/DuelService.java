package com.astrasmp.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.astrasmp.AstraSMPPlugin;
import com.astrasmp.util.TextUtil;

public final class DuelService implements Listener {
    private final ServiceManager services;

    // Кто кому отправил запрос (K: Отправитель, V: Получатель)
    private final Map<UUID, UUID> requests = new HashMap<>();
    // Активные дуэли (K: Игрок 1, V: Игрок 2 и наоборот для быстрого поиска)
    private final Map<UUID, UUID> activeDuels = new HashMap<>();

    public DuelService(AstraSMPPlugin plugin, ServiceManager services) {
        this.services = services;
    }

    public void handleCommand(Player sender, String[] args) {
        if (args.length == 0) {
            TextUtil.send(sender, "&cИспользование: /duel <ник>");
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || target.equals(sender)) {
            TextUtil.send(sender, "&cИгрок не найден или вы указали себя!");
            return;
        }

        if (activeDuels.containsKey(sender.getUniqueId())) {
            TextUtil.send(sender, "&cВы уже находитесь в дуэли!");
            return;
        }

        // Проверяем, есть ли встречный запрос (принятие дуэли)
        if (requests.containsKey(target.getUniqueId()) && requests.get(target.getUniqueId()).equals(sender.getUniqueId())) {
            startDuel(sender, target);
            requests.remove(target.getUniqueId());
        } else {
            // Отправляем запрос
            requests.put(sender.getUniqueId(), target.getUniqueId());
            TextUtil.send(sender, "&aВы бросили вызов игроку &e" + target.getName() + "&a!");
            TextUtil.send(target, "&c&lДУЭЛЬ! &e" + sender.getName() + " &fвызывает вас на бой! Напишите &a/duel " + sender.getName() + " &fчтобы принять.");
        }
    }

    private void startDuel(Player p1, Player p2) {
        activeDuels.put(p1.getUniqueId(), p2.getUniqueId());
        activeDuels.put(p2.getUniqueId(), p1.getUniqueId());

        services.afk().teleportToLocation(p1, "duel_pos1");
        services.afk().teleportToLocation(p2, "duel_pos2");

        TextUtil.send(p1, "&c&lБОЙ НАЧАЛСЯ! Убейте противника!");
        TextUtil.send(p2, "&c&lБОЙ НАЧАЛСЯ! Убейте противника!");
    }

    private void endDuel(Player loser, Player winner) {
        activeDuels.remove(loser.getUniqueId());
        if (winner != null) {
            activeDuels.remove(winner.getUniqueId());
            TextUtil.send(winner, "&a&lПОБЕДА! &fВы одолели противника.");
            services.afk().teleportToLocation(winner, "spawn"); // Тепаем победителя на спавн
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player loser = event.getEntity();
        if (activeDuels.containsKey(loser.getUniqueId())) {
            Player winner = Bukkit.getPlayer(activeDuels.get(loser.getUniqueId()));
            endDuel(loser, winner);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player loser = event.getPlayer();
        if (activeDuels.containsKey(loser.getUniqueId())) {
            Player winner = Bukkit.getPlayer(activeDuels.get(loser.getUniqueId()));
            endDuel(loser, winner);
        }
    }
}