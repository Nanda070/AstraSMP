package com.astrasmp.commands;

import com.astrasmp.service.ServiceManager;
import com.astrasmp.util.LocationKey;
import com.astrasmp.util.TextUtil;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class WarpCommand implements org.bukkit.command.TabExecutor {
    private final ServiceManager services;

    public WarpCommand(ServiceManager services) {
        this.services = services;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (args.length == 0) {
            if (services.store().getWarps().isEmpty()) {
                TextUtil.send(player, "&cНет доступных варпов.");
                return true;
            }
            TextUtil.send(player, "&aДоступные варпы: &f" + String.join(", ", services.store().getWarps().keySet()));
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("set")) {
            if (!player.hasPermission("astrasmp.admin")) {
                TextUtil.send(player, "&cУ вас нет прав для установки варпа.");
                return true;
            }
            if (args.length < 2) {
                TextUtil.send(player, "&cИспользование: /warp set <название>");
                return true;
            }
            String name = args[1].toLowerCase();
            services.store().getWarps().put(name, LocationKey.fromLocation(player.getLocation()));
            services.store().requestSave();
            TextUtil.send(player, "&aВарп &f" + name + " &aуспешно установлен!");
            return true;
        }

        if (sub.equals("remove")) {
            if (!player.hasPermission("astrasmp.admin")) {
                TextUtil.send(player, "&cУ вас нет прав для удаления варпа.");
                return true;
            }
            if (args.length < 2) {
                TextUtil.send(player, "&cИспользование: /warp remove <название>");
                return true;
            }
            String name = args[1].toLowerCase();
            if (services.store().getWarps().remove(name) != null) {
                services.store().requestSave();
                TextUtil.send(player, "&aВарп &f" + name + " &aуспешно удален!");
            } else {
                TextUtil.send(player, "&cВарп &f" + name + " &cне найден.");
            }
            return true;
        }

        LocationKey key = services.store().getWarps().get(sub);
        if (key != null) {
            Location loc = key.toLocation();
            if (loc != null) {
                player.teleport(loc);
                TextUtil.send(player, "&aВы телепортированы на варп &f" + sub);
            } else {
                TextUtil.send(player, "&cОшибка: мир варпа не найден.");
            }
        } else {
            TextUtil.send(player, "&cВарп &f" + sub + " &cне найден.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            List<String> list = new java.util.ArrayList<>(services.store().getWarps().keySet());
            if (sender.hasPermission("astrasmp.admin")) {
                list.add("set");
                list.add("remove");
            }
            return list.stream().filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            return services.store().getWarps().keySet().stream().filter(s -> s.startsWith(args[1].toLowerCase())).toList();
        }
        return java.util.Collections.emptyList();
    }
}
