package com.astrasmp.commands;

import com.astrasmp.service.ServiceManager;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public final class MenuCommand implements org.bukkit.command.TabExecutor {
    private final ServiceManager services;
    public MenuCommand(ServiceManager services) { this.services = services; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Player only.");
            return true;
        }
        services.gui().openMain(player);
        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) {
        return java.util.Collections.emptyList();
    }
}
