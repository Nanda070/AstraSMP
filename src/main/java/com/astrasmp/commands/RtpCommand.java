package com.astrasmp.commands;

import com.astrasmp.service.ServiceManager;
import com.astrasmp.util.TextUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import java.util.Random;

public final class RtpCommand implements org.bukkit.command.TabExecutor {
    private final ServiceManager services;
    private final Random random = new Random();

    public RtpCommand(ServiceManager services) { this.services = services; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        int radius = 1000;
        Location loc = null;
        int maxAttempts = 10;

        for (int i = 0; i < maxAttempts; i++) {
            int x = random.nextInt(radius * 2) - radius;
            int z = random.nextInt(radius * 2) - radius;
            loc = player.getWorld().getHighestBlockAt(x, z).getLocation().add(0.5, 1, 0.5);

            Material type = loc.getBlock().getRelative(0, -1, 0).getType();
            if (type != Material.WATER && type != Material.LAVA) {
                break;
            }
            if (i == maxAttempts - 1) {
                TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_3a1628", "&cНе удалось найти безопасную локацию, попробуйте еще раз."));
                return true;
            }
        }

        player.teleport(loc);
        TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_d1d176", "&bВы успешно телепортированы!"));

        // ГАЗ: Засчитываем квест №8
        services.quests().processAction(player, com.astrasmp.service.QuestManager.QuestAction.USE_RTP, "", 1);
        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) {
        return java.util.Collections.emptyList();
    }
}
