package com.astrasmp.service;

import com.astrasmp.AstraSMPPlugin;
import com.astrasmp.model.PlayerProfile;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public final class LeaderboardService {
    private final AstraSMPPlugin plugin;
    private final DataStore store;

    public LeaderboardService(AstraSMPPlugin plugin, DataStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    public List<PlayerProfile> topCoins(int limit) {
        return store.profiles().values().stream()
                .sorted(Comparator.comparingLong(PlayerProfile::getCoins).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<PlayerProfile> topKills(int limit) {
        return store.profiles().values().stream()
                .sorted(Comparator.comparingInt(PlayerProfile::getKills).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<PlayerProfile> topSold(int limit) {
        return store.profiles().values().stream()
                .sorted(Comparator.comparingLong(PlayerProfile::getSoldValue).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<PlayerProfile> topEvents(int limit) {
        return store.profiles().values().stream()
                .sorted(Comparator.comparingInt(PlayerProfile::getEventPoints).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<PlayerProfile> topMmr(int limit) {
        return store.profiles().values().stream()
                .sorted(Comparator.comparingInt(PlayerProfile::getMmr).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public void startUpdateTask() {
        org.bukkit.Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // Логика обновления топа
            plugin.getLogger().info("Топ игроков обновлен!");
        }, 20L * 60, 20L * 60 * 5); // Каждые 5 минут
    }
}
