package com.astrasmp.casino;

import com.astrasmp.AstraSMPPlugin;
import com.astrasmp.model.PlayerProfile;
import com.astrasmp.service.ServiceManager;
import com.astrasmp.util.TextUtil;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CasinoService {
    private final AstraSMPPlugin plugin;
    private final ServiceManager services;
    
    private final Set<UUID> activeSessions = new HashSet<>();

    public CasinoService(AstraSMPPlugin plugin, ServiceManager services) {
        this.plugin = plugin;
        this.services = services;
    }

    public boolean startSession(Player player) {
        if (activeSessions.contains(player.getUniqueId())) {
            TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_01297a", "&cЗавершите текущую игру перед началом новой!"));
            return false;
        }
        activeSessions.add(player.getUniqueId());
        return true;
    }

    public void endSession(Player player) {
        activeSessions.remove(player.getUniqueId());
    }

    public boolean processBet(Player player, int amount) {
        PlayerProfile profile = services.economy().profile(player.getUniqueId(), player.getName());
        if (profile.getCoins() < amount) {
            TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_4cd731", "&cНедостаточно ❂ для ставки!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return false;
        }
        
        profile.setCoins(profile.getCoins() - amount);
        services.store().requestSave();
        return true;
    }

    public void processPayout(Player player, int amount) {
        if (amount <= 0) return;
        PlayerProfile profile = services.economy().profile(player.getUniqueId(), player.getName());
        profile.setCoins(profile.getCoins() + amount);
        services.store().requestSave();
        TextUtil.send(player, "&aВыигрыш: &e+" + amount + " ❂");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
    }

    public ServiceManager getServices() {
        return services;
    }

    public AstraSMPPlugin getPlugin() {
        return plugin;
    }
}