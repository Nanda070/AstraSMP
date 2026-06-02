package com.astrasmp.commands;

import com.astrasmp.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class InvseeCommand implements CommandExecutor, org.bukkit.command.TabCompleter {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player admin)) return true;
        if (!admin.isOp()) {
            TextUtil.send(admin, "&cУ вас нет прав!");
            return true;
        }
        if (args.length < 1) {
            TextUtil.send(admin, "&eИспользование: &f/invsee <ник>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            TextUtil.send(admin, "&cИгрок не найден или оффлайн!");
            return true;
        }

        admin.openInventory(target.getInventory());
        TextUtil.send(admin, "&aОткрыт инвентарь игрока &f" + target.getName());
        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) return null; // Bukkit will autocomplete online players
        return java.util.Collections.emptyList();
    }
}