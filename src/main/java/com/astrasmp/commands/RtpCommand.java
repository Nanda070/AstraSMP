package com.astrasmp.commands;

import com.astrasmp.service.ServiceManager;
import com.astrasmp.util.TextUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import java.util.Random;

public final class RtpCommand implements CommandExecutor {
    private final ServiceManager services;
    private final Random random = new Random();

    public RtpCommand(ServiceManager services) { this.services = services; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        int radius = 1000;
        int x = random.nextInt(radius * 2) - radius;
        int z = random.nextInt(radius * 2) - radius;
        Location loc = player.getWorld().getHighestBlockAt(x, z).getLocation().add(0.5, 1, 0.5);

        if (loc.getBlock().getRelative(0, -1, 0).getType() == Material.WATER) {
            TextUtil.send(player, "&cЛокация в воде, попробуйте еще раз.");
            return true;
        }

        player.teleport(loc);
        TextUtil.send(player, "&bВы успешно телепортированы!");

        // ГАЗ: Засчитываем квест №8
        services.quests().checkProgress(player, 8, 1);
        return true;
    }
}