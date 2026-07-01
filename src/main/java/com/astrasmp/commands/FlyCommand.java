package com.astrasmp.commands;

import com.astrasmp.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class FlyCommand implements org.bukkit.command.TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission("astrasmp.admin")) {
            TextUtil.send(sender, "&cУ вас нет прав для использования этой команды.");
            return true;
        }

        Player target;
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

        boolean newState = !target.getAllowFlight();
        target.setAllowFlight(newState);

        if (newState) {
            TextUtil.send(target, "&aРежим полёта &2включён&a.");
            if (target != sender) {
                TextUtil.send(sender, "&aВы включили полёт для &e" + target.getName() + "&a.");
            }
        } else {
            // Disable active flight too
            target.setFlying(false);
            TextUtil.send(target, "&cРежим полёта &4выключен&c.");
            if (target != sender) {
                TextUtil.send(sender, "&cВы выключили полёт для &e" + target.getName() + "&c.");
            }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (!sender.hasPermission("astrasmp.admin")) return Collections.emptyList();

        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(p -> p.getName())
                    .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
