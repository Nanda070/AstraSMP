package com.astrasmp.service;

import com.astrasmp.AstraSMPPlugin;
import com.astrasmp.model.PlayerProfile;
import org.bukkit.entity.Player;

public final class MMRService {
    private final AstraSMPPlugin plugin;
    private final DataStore store;
    private static final int K_FACTOR = 32;

    public MMRService(AstraSMPPlugin plugin, DataStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    public int start() {
        return plugin.getConfig().getInt("mmr.start", 1000);
    }

    public String rankFor(int mmr) {
        if (mmr >= plugin.getConfig().getInt("mmr.elite", 2000)) return "Elite";
        if (mmr >= plugin.getConfig().getInt("mmr.diamond", 1600)) return "Diamond";
        if (mmr >= plugin.getConfig().getInt("mmr.gold", 1300)) return "Gold";
        if (mmr >= plugin.getConfig().getInt("mmr.silver", 1100)) return "Silver";
        return "Bronze";
    }

    public int adjustOnKill(Player killer, Player victim) {
        PlayerProfile k = store.profile(killer.getUniqueId().toString(), killer.getName());
        PlayerProfile v = store.profile(victim.getUniqueId().toString(), victim.getName());
        
        int ratingA = k.getMmr();
        int ratingB = v.getMmr();

        double expectedA = 1.0 / (1.0 + Math.pow(10.0, (ratingB - ratingA) / 400.0));
        double expectedB = 1.0 / (1.0 + Math.pow(10.0, (ratingA - ratingB) / 400.0));

        int deltaK = (int) Math.round(K_FACTOR * (1.0 - expectedA));
        int deltaV = (int) Math.round(K_FACTOR * (0.0 - expectedB));

        k.setMmr(Math.max(0, ratingA + deltaK));
        v.setMmr(Math.max(0, ratingB + deltaV));
        
        store.requestSave();
        return deltaK;
    }

    public int adjustOnDeath(Player player) {
        PlayerProfile p = store.profile(player.getUniqueId().toString(), player.getName());
        int loss = 8 + Math.min(20, p.getMmr() / 200);
        p.setMmr(Math.max(0, p.getMmr() - loss));
        store.requestSave();
        return loss;
    }
}