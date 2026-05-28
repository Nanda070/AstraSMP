package com.astrasmp.commands;

import com.astrasmp.service.ServiceManager;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public final class MenuCommand implements CommandExecutor {
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
}
