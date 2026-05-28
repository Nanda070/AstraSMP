package com.astrasmp.commands;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.astrasmp.model.PlayerProfile;
import com.astrasmp.service.ServiceManager;
import com.astrasmp.util.TextUtil;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class LocationCommand implements CommandExecutor {
    private final ServiceManager services;

    public LocationCommand(ServiceManager services) {
        this.services = services;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;

        String cmd = command.getName().toLowerCase();

        switch (cmd) {
            case "pvp" -> {
                if (args.length > 0 && args[0].equalsIgnoreCase("holo")) {
                    if (!player.isOp()) {
                        TextUtil.send(player, "&cУ вас нет прав для этой команды!");
                        return true;
                    }

                    // Удаление голограммы
                    if (args.length > 1 && args[1].equalsIgnoreCase("remove")) {
                        int count = 0;
                        for (Entity entity : player.getNearbyEntities(5, 5, 5)) {
                            if (entity instanceof ArmorStand as && as.isMarker() && as.isCustomNameVisible()) {
                                as.remove();
                                count++;
                            }
                        }
                        TextUtil.send(player, "&aУдалено &e" + count + " &aстрок голограмм(ы) поблизости.");
                        return true;
                    }

                    // Установка голограммы
                    spawnPvPHologram(player.getLocation());
                    TextUtil.send(player, "&aГолограмма топ PvP установлена!");
                    return true;
                }
                services.afk().teleportToLocation(player, "pvp");
                TextUtil.send(player, "&cВы телепортированы на PvP Арену!");
            }
            case "casino" -> {
                services.afk().teleportToLocation(player, "casino");
                TextUtil.send(player, "&6Вы прибыли в Казино!");
            }
            case "eventshop" -> {
                services.afk().teleportToLocation(player, "eventshop");
                TextUtil.send(player, "&dДобро пожаловать в Ивент Шоп!");
            }
            case "afk" -> {
                services.afk().teleportToLocation(player, "afk");
                TextUtil.send(player, "&eВы прибыли в AFK-зону.");
            }
            case "duel" -> services.duels().handleCommand(player, args);
        }

        return true;
    }

    private void spawnPvPHologram(Location loc) {
        List<PlayerProfile> top = services.store().profiles().values().stream()
                .sorted((p1, p2) -> Integer.compare(p2.getMmr(), p1.getMmr()))
                .limit(5)
                .toList();

        spawnLine(loc.clone().add(0, 2.0, 0), "&c&lТОП 5 БОЙЦОВ СЕРВЕРА");

        double offset = 1.7;
        for (int i = 0; i < top.size(); i++) {
            PlayerProfile p = top.get(i);
            String color = switch(i) { case 0->"&6"; case 1->"&7"; case 2->"&c"; default->"&f"; };
            spawnLine(loc.clone().add(0, offset, 0), color + (i+1) + ". " + p.getName() + " &8- &c" + p.getMmr() + " MMR");
            offset -= 0.3;
        }
    }

    private void spawnLine(Location loc, String text) {
        loc.getWorld().spawn(loc, ArmorStand.class, as -> {
            as.setVisible(false);
            as.setGravity(false);
            as.customName(LegacyComponentSerializer.legacyAmpersand().deserialize(text));
            as.setCustomNameVisible(true);
            as.setMarker(true);
        });
    }
}