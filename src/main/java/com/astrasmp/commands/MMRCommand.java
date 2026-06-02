package com.astrasmp.commands;

import com.astrasmp.model.PlayerProfile;
import com.astrasmp.service.ServiceManager;
import com.astrasmp.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.List;

public final class MMRCommand implements org.bukkit.command.TabExecutor {
    private final ServiceManager services;
    public MMRCommand(ServiceManager services) { this.services = services; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        OfflinePlayer target;
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                TextUtil.send(sender, "&cUse /mmr <player> from console.");
                return true;
            }
            target = player;
        } else {
            target = Bukkit.getOfflinePlayer(args[0]);
        }
        PlayerProfile profile = services.economy().getProfile(target.getUniqueId(), target.getName() == null ? target.getUniqueId().toString() : target.getName());
        TextUtil.send(sender, "&7MMR of &f" + target.getName() + "&7: &e" + profile.getMmr() + " &7(" + services.mmr().rankFor(profile.getMmr()) + ")");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return args.length == 1 ? Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.startsWith(args[0])).toList() : List.of();
    }

}
