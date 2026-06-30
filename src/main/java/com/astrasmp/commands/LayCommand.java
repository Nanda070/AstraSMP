package com.astrasmp.commands;

import com.astrasmp.AstraSMPPlugin;
import com.astrasmp.service.SitLayService;
import com.astrasmp.util.TextUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LayCommand implements CommandExecutor {
    private final SitLayService sitLayService;
    private final AstraSMPPlugin plugin;

    public LayCommand(SitLayService sitLayService, AstraSMPPlugin plugin) {
        this.sitLayService = sitLayService;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            TextUtil.send(sender, plugin.getConfigManager().getMessage("player-only", "&cКоманда только для игрока."));
            return true;
        }

        sitLayService.toggleLay(player);
        return true;
    }
}
