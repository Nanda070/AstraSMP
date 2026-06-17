package com.astrasmp.commands;

import com.astrasmp.service.ServiceManager;
import com.astrasmp.util.LocationKey;
import com.astrasmp.util.TextUtil;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class RtpBlockCommand implements org.bukkit.command.TabExecutor {
    private final ServiceManager services;

    public RtpBlockCommand(ServiceManager services) {
        this.services = services;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player) || !player.isOp()) return true;

        Location loc = player.getLocation().getBlock().getLocation();
        LocationKey key = LocationKey.fromLocation(loc);
        
        if (services.store().getRtpBlocks().add(key)) {
            TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_7f192c", "&aБлок RTP установлен на ваших координатах!"));
            services.store().requestSave();
        } else {
            services.store().getRtpBlocks().remove(key);
            TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_e3fe44", "&eБлок RTP удален."));
            services.store().requestSave();
        }
        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) {
        return java.util.Collections.emptyList();
    }
}
