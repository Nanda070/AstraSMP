package com.astrasmp.commands;

import com.astrasmp.service.ServiceManager;
import com.astrasmp.util.TextUtil;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public final class SellCommand implements org.bukkit.command.TabExecutor {
    private final ServiceManager services;
    public SellCommand(ServiceManager services) { this.services = services; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            TextUtil.send(sender, "&cPlayer only.");
            return true;
        }
        
        if (args.length == 0) {
            // Открываем меню скупщика по умолчанию
            services.gui().openSellResources(player);
            return true;
        }
        
        long sold = 0L;
        if (args[0].equalsIgnoreCase("hand")) sold = services.economy().sellHand(player);
        else sold = services.economy().sellInventory(player);
        
        if (sold <= 0) {
            TextUtil.send(player, "&cНечего продавать.");
        } else {
            TextUtil.send(player, "&aПродано на &f" + sold + " coins");
        }
        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) {
        if (args.length == 1) {
            return java.util.Arrays.asList("hand", "all").stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .toList();
        }
        return java.util.Collections.emptyList();
    }
}
