package com.astrasmp.service;

import com.astrasmp.AstraSMPPlugin;
import com.astrasmp.model.PlayerProfile;
import com.astrasmp.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class TabService {
    private final AstraSMPPlugin plugin;
    private final ServiceManager services;

    public TabService(AstraSMPPlugin plugin, ServiceManager services) {
        this.plugin = plugin;
        this.services = services;
    }

    public void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::updateAll, 20L, 20L);
    }

    public void stop() {

    }

    public void updateAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayer(player);
        }
    }

    private void updatePlayer(Player player) {
        PlayerProfile profile = services.economy().profile(player.getUniqueId(), player.getName());


        String color = profile.getPrefixColor().isEmpty() ? "&7" : profile.getPrefixColor();
        String prefix = profile.getCustomPrefix().isEmpty() ? "" : color + "[" + profile.getCustomPrefix() + "] ";

        player.playerListName(Component.text(TextUtil.color(prefix + "&f" + player.getName())));

        player.sendPlayerListHeaderAndFooter(
                Component.text(TextUtil.color("\n&b&lChet&f&lCraft &7- &f404?\n")),
                Component.text(TextUtil.color("\n&fТвой баланс: &e" + profile.getCoins() + " ❂\n&fОнлайн: &b" + Bukkit.getOnlinePlayers().size() + "\n"))
        );
    }
}