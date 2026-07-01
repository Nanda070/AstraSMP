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

public final class TpmeCommand implements org.bukkit.command.TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission("astrasmp.admin")) {
            TextUtil.send(sender, "&cУ вас нет прав для использования этой команды.");
            return true;
        }

        if (!(sender instanceof Player initiator)) {
            TextUtil.send(sender, "&cЭта команда доступна только игрокам.");
            return true;
        }

        if (args.length == 0) {
            TextUtil.send(sender, "&cИспользование: /tpme <игрок>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            TextUtil.send(sender, "&cИгрок &e" + args[0] + " &cне найден.");
            return true;
        }

        if (target.equals(initiator)) {
            TextUtil.send(sender, "&cВы не можете телепортировать себя к себе.");
            return true;
        }

        target.teleport(initiator.getLocation());
        TextUtil.send(target, "&aВас телепортировал к себе администратор &e" + initiator.getName() + "&a.");
        TextUtil.send(initiator, "&aИгрок &e" + target.getName() + " &aбыл телепортирован к вам.");

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
