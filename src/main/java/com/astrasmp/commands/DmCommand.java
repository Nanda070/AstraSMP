package com.astrasmp.commands;

import com.astrasmp.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class DmCommand implements org.bukkit.command.TabExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length < 2) {
            TextUtil.send(sender, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_7eb352", "&cИспользование: /dm <игрок> <сообщение>"));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            TextUtil.send(sender, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_426f15", "&cИгрок не найден."));
            return true;
        }

        String message = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        String senderName = sender instanceof Player ? sender.getName() : "Сервер";

        TextUtil.send(target, "&8[&dЛС от &e" + senderName + "&8] &f" + message);
        TextUtil.send(sender, "&8[&dЛС для &e" + target.getName() + "&8] &f" + message);
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream().map(p -> p.getName()).filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase())).toList();
        }
        return List.of();
    }

}
