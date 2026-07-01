package com.astrasmp.commands;

import com.astrasmp.util.TextUtil;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class WeatherCommand implements org.bukkit.command.TabExecutor {

    private static final List<String> WEATHER_TYPES = List.of("clear", "rain", "thunder", "storm");

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission("astrasmp.admin")) {
            TextUtil.send(sender, "&cУ вас нет прав для использования этой команды.");
            return true;
        }

        if (args.length == 0) {
            TextUtil.send(sender, "&cИспользование: /weather <clear|rain|thunder|storm>");
            return true;
        }

        // Resolve world: if sender is a player use their world, else use default world
        World world;
        if (sender instanceof Player player) {
            world = player.getWorld();
        } else {
            world = org.bukkit.Bukkit.getWorlds().get(0);
        }

        switch (args[0].toLowerCase()) {
            case "clear", "sun", "sunny" -> {
                world.setStorm(false);
                world.setThundering(false);
                world.setWeatherDuration(Integer.MAX_VALUE);
                TextUtil.send(sender, "&aПогода изменена на &eясно &aв мире &e" + world.getName());
            }
            case "rain" -> {
                world.setStorm(true);
                world.setThundering(false);
                world.setWeatherDuration(Integer.MAX_VALUE);
                TextUtil.send(sender, "&aПогода изменена на &eдождь &aв мире &e" + world.getName());
            }
            case "thunder", "storm" -> {
                world.setStorm(true);
                world.setThundering(true);
                world.setWeatherDuration(Integer.MAX_VALUE);
                TextUtil.send(sender, "&aПогода изменена на &eгрозу &aв мире &e" + world.getName());
            }
            default -> TextUtil.send(sender, "&cНеизвестный тип погоды. Используйте: clear, rain, thunder, storm");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (!sender.hasPermission("astrasmp.admin")) return Collections.emptyList();

        if (args.length == 1) {
            String input = args[0].toLowerCase();
            return WEATHER_TYPES.stream()
                    .filter(w -> w.startsWith(input))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
