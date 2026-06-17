package com.astrasmp.commands;

import com.astrasmp.model.PlayerProfile;
import com.astrasmp.service.ServiceManager;
import com.astrasmp.util.TextUtil;
import org.bukkit.Bukkit;

import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.List;

public final class StatsCommand implements org.bukkit.command.TabExecutor {
    private final ServiceManager services;
    public StatsCommand(ServiceManager services) { this.services = services; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        java.util.UUID targetUuid;
        String targetName;
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                TextUtil.send(sender, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_5b0427", "&cUse /stats <player> from console."));
                return true;
            }
            targetUuid = player.getUniqueId();
            targetName = player.getName();
        } else {
            targetName = args[0];
            String uuidStr = services.plugin().getDatabase().getUuidByName(targetName);
            if (uuidStr == null) {
                TextUtil.send(sender, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_720a57", "&cИгрок никогда не заходил на сервер."));
                return true;
            }
            targetUuid = java.util.UUID.fromString(uuidStr);
        }
        
        PlayerProfile p = services.economy().getProfile(targetUuid, targetName);
        if (p == null) {
            TextUtil.send(sender, "&cОшибка загрузки профиля.");
            return true;
        }
        
        TextUtil.send(sender, "&8--- &f" + targetName + " &8---");
        TextUtil.send(sender, "&7Coins: &a" + p.getCoins());
        TextUtil.send(sender, "&7Kills: &f" + p.getKills() + " &7Deaths: &f" + p.getDeaths());
        TextUtil.send(sender, "&7Sold: &f" + p.getSoldValue() + " &7Event points: &f" + p.getEventPoints());
        TextUtil.send(sender, "&7MMR: &e" + p.getMmr() + " &7(" + services.mmr().rankFor(p.getMmr()) + ")");
        TextUtil.send(sender, "&7Faction: &f" + (p.getFaction().isBlank() ? "None" : p.getFaction()) + " &7Base: &f" + p.getBaseLevel());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return args.length == 1 ? Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.startsWith(args[0])).toList() : List.of();
    }

}
