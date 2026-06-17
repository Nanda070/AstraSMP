package com.astrasmp.commands;

import com.astrasmp.AstraSMPPlugin;
import com.astrasmp.service.ServiceManager;
import com.astrasmp.util.TextUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SpawnCommand implements org.bukkit.command.TabExecutor {
    private final ServiceManager services;

    public SpawnCommand(AstraSMPPlugin plugin, ServiceManager services) {
        this.services = services;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Только игроки могут использовать эту команду.");
            return true;
        }

        // 1. Проверка на заморозку
        var profile = services.economy().profile(player.getUniqueId(), player.getName());
        if (profile != null && profile.isFrozen()) {
            TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_9047db", "&cВы не можете использовать телепортацию сейчас!"));
            return true;
        }

        // 2. Используем единый метод телепортации из AfkService
        // Этот метод автоматически берет данные из "locations.spawn" в config.yml
        services.afk().teleportToLocation(player, "spawn");

        TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_c956d2", "&aВы были телепортированы на спавн!"));

        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) {
        return java.util.Collections.emptyList();
    }
}
