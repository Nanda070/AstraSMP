package com.astrasmp.commands;

import com.astrasmp.AstraSMPPlugin;
import com.astrasmp.service.ServiceManager;
import com.astrasmp.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ArenaCommand implements TabExecutor {
    private final AstraSMPPlugin plugin;
    private final ServiceManager services;

    public ArenaCommand(AstraSMPPlugin plugin, ServiceManager services) {
        this.plugin = plugin;
        this.services = services;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Только игроки могут использовать эту команду.");
            return true;
        }

        String cmd = command.getName().toLowerCase();

        if (cmd.equals("arena")) {
            List<String> spawns = plugin.getConfig().getStringList("locations.arena");
            if (spawns.isEmpty()) {
                TextUtil.send(player, "&cЛокации арены не настроены в config.yml!");
                return true;
            }

            String randomLocStr = spawns.get(ThreadLocalRandom.current().nextInt(spawns.size()));
            Location loc = parseLocation(randomLocStr);

            if (loc != null) {
                player.teleport(loc);
                TextUtil.send(player, "&aВы телепортировались на арену! Для выхода используйте /leave");
            } else {
                TextUtil.send(player, "&cОшибка парсинга локации арены.");
            }
        } else if (cmd.equals("leave")) {
            services.afk().teleportToLocation(player, "spawn");
            TextUtil.send(player, "&aВы покинули арену и были перемещены на спавн.");
        }

        return true;
    }

    private Location parseLocation(String locStr) {
        if (locStr != null && locStr.contains(";")) {
            String[] parts = locStr.split(";");
            if (parts.length >= 4) {
                return new Location(Bukkit.getWorld(parts[0]),
                        Double.parseDouble(parts[1]),
                        Double.parseDouble(parts[2]),
                        Double.parseDouble(parts[3]));
            }
        }
        return null;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
