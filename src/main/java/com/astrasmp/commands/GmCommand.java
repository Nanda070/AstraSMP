package com.astrasmp.commands;

import com.astrasmp.util.TextUtil;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class GmCommand implements org.bukkit.command.TabExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player) && args.length < 2) {
            TextUtil.send(sender, "&cКонсоль должна указывать ник игрока.");
            return true;
        }

        if (!sender.hasPermission("astrasmp.admin")) {
            TextUtil.send(sender, "&cУ вас нет прав для использования этой команды.");
            return true;
        }

        if (args.length == 0) {
            TextUtil.send(sender, "&cИспользование: /gm <0|1|2|3> [игрок]");
            return true;
        }

        Player target;
        if (args.length > 1) {
            target = org.bukkit.Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                TextUtil.send(sender, "&cИгрок &e" + args[1] + " &cне найден.");
                return true;
            }
        } else {
            target = (Player) sender;
        }

        GameMode gm = switch (args[0]) {
            case "0" -> GameMode.SURVIVAL;
            case "1" -> GameMode.CREATIVE;
            case "2" -> GameMode.ADVENTURE;
            case "3" -> GameMode.SPECTATOR;
            default -> null;
        };

        if (gm == null) {
            TextUtil.send(sender, "&cНеверный режим игры. Используйте 0, 1, 2 или 3.");
            return true;
        }

        target.setGameMode(gm);
        TextUtil.send(target, "&aВаш игровой режим изменен на &e" + gm.name());
        if (target != sender) {
            TextUtil.send(sender, "&aВы изменили игровой режим &e" + target.getName() + " &aна &e" + gm.name());
        }
        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            java.util.List<String> modes = java.util.List.of("0", "1", "2", "3");
            return modes.stream().filter(m -> m.startsWith(args[0])).toList();
        }
        if (args.length == 2) {
            return null; // Return default online players list
        }
        return java.util.Collections.emptyList();
    }

}
