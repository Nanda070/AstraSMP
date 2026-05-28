package com.astrasmp.commands;

import com.astrasmp.service.ServiceManager;
import com.astrasmp.util.LocationKey;
import com.astrasmp.util.TextUtil;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class RtpBlockCommand implements CommandExecutor {
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
            TextUtil.send(player, "&aБлок RTP установлен на ваших координатах!");
            services.store().requestSave();
        } else {
            services.store().getRtpBlocks().remove(key);
            TextUtil.send(player, "&eБлок RTP удален.");
            services.store().requestSave();
        }
        return true;
    }
}