package com.astrasmp.commands;

import com.astrasmp.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public final class FeedCommand implements org.bukkit.command.TabExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission("astrasmp.admin")) {
            TextUtil.send(sender, "&cУ вас нет прав для использования этой команды.");
            return true;
        }

        Player target = null;
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                TextUtil.send(sender, "&cКонсоль должна указать ник игрока.");
                return true;
            }
            target = player;
        } else {
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                TextUtil.send(sender, "&cИгрок &e" + args[0] + " &cне найден.");
                return true;
            }
        }

        target.setFoodLevel(20);
        target.setSaturation(20f);
        
        TextUtil.send(target, "&aВаша сытость восстановлена.");
        if (target != sender) {
            TextUtil.send(sender, "&aВы накормили игрока &e" + target.getName() + "&a.");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            return null;
        }
        return Collections.emptyList();
    }
}
