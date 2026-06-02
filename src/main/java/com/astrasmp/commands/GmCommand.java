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
        if (!(sender instanceof Player player)) {
            TextUtil.send(sender, "&cТолько для игроков.");
            return true;
        }

        if (!player.hasPermission("astrasmp.admin")) {
            TextUtil.send(player, "&cУ вас нет прав для использования этой команды.");
            return true;
        }

        if (args.length == 0) {
            TextUtil.send(player, "&cИспользование: /gm <0|1|2|3>");
            return true;
        }

        GameMode gm = switch (args[0]) {
            case "0" -> GameMode.SURVIVAL;
            case "1" -> GameMode.CREATIVE;
            case "2" -> GameMode.ADVENTURE;
            case "3" -> GameMode.SPECTATOR;
            default -> null;
        };

        if (gm == null) {
            TextUtil.send(player, "&cНеверный режим игры. Используйте 0, 1, 2 или 3.");
            return true;
        }

        player.setGameMode(gm);
        TextUtil.send(player, "&aВаш игровой режим изменен на &e" + gm.name());
        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            java.util.List<String> modes = java.util.List.of("0", "1", "2", "3");
            return modes.stream().filter(m -> m.startsWith(args[0])).toList();
        }
        return java.util.Collections.emptyList();
    }

}
