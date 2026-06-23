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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HomeCommand implements org.bukkit.command.TabExecutor {
    private final ServiceManager services;

    public HomeCommand(ServiceManager services) {
        this.services = services;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        String cmd = command.getName().toLowerCase();
        Map<String, LocationKey> playerHomes = services.store().getHomes().computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>());

        if (cmd.equals("sethome")) {
            String name = args.length > 0 ? args[0].toLowerCase() : "home";
            playerHomes.put(name, LocationKey.fromLocation(player.getLocation()));
            services.store().requestSave();
            TextUtil.send(player, "&aДом &f" + name + " &aуспешно сохранен!");
            return true;
        }

        if (cmd.equals("delhome")) {
            String name = args.length > 0 ? args[0].toLowerCase() : "home";
            if (playerHomes.remove(name) != null) {
                services.store().requestSave();
                TextUtil.send(player, "&aДом &f" + name + " &aуспешно удален!");
            } else {
                TextUtil.send(player, "&cДом &f" + name + " &cне найден.");
            }
            return true;
        }

        if (cmd.equals("homelist")) {
            if (playerHomes.isEmpty()) {
                TextUtil.send(player, "&cУ вас нет сохраненных домов.");
                return true;
            }
            TextUtil.send(player, "&aВаши дома: &f" + String.join(", ", playerHomes.keySet()));
            return true;
        }

        if (cmd.equals("homesreload")) {
            if (!player.hasPermission("astrasmp.admin")) {
                TextUtil.send(player, "&cУ вас нет прав для перезагрузки.");
                return true;
            }
            TextUtil.send(player, "&aИспользуйте &f/reload &aдля полной перезагрузки плагина.");
            return true;
        }

        if (cmd.equals("home")) {
            if (playerHomes.isEmpty()) {
                TextUtil.send(player, "&cУ вас нет сохраненных домов.");
                return true;
            }
            String name = args.length > 0 ? args[0].toLowerCase() : "home";
            LocationKey key = playerHomes.get(name);
            if (key != null) {
                Location loc = key.toLocation();
                if (loc != null) {
                    player.teleport(loc);
                    TextUtil.send(player, "&aВы телепортированы домой: &f" + name);
                } else {
                    TextUtil.send(player, "&cОшибка: мир дома не найден.");
                }
            } else {
                TextUtil.send(player, "&cДом &f" + name + " &cне найден. Ваши дома: " + String.join(", ", playerHomes.keySet()));
            }
            return true;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (!(sender instanceof Player player)) return java.util.Collections.emptyList();
        String cmd = command.getName().toLowerCase();
        
        if ((cmd.equals("home") || cmd.equals("delhome")) && args.length == 1) {
            Map<String, LocationKey> playerHomes = services.store().getHomes().get(player.getUniqueId());
            if (playerHomes != null) {
                return playerHomes.keySet().stream().filter(s -> s.startsWith(args[0].toLowerCase())).toList();
            }
        }
        return java.util.Collections.emptyList();
    }
}
