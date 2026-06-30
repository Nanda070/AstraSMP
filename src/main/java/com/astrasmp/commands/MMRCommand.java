package com.astrasmp.commands;

import com.astrasmp.model.PlayerProfile;
import com.astrasmp.service.ServiceManager;
import com.astrasmp.util.TextUtil;
import org.bukkit.Bukkit;

import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.List;

public final class MMRCommand implements org.bukkit.command.TabExecutor {
    private final ServiceManager services;
    public MMRCommand(ServiceManager services) { this.services = services; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        java.util.UUID targetUuid;
        String targetName;
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                TextUtil.send(sender, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_f886b5", "&cUse /mmr <player> from console."));
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
        
        PlayerProfile profile = services.economy().getProfile(targetUuid, targetName);
        if (profile == null) {
            TextUtil.send(sender, "&cОшибка загрузки профиля.");
            return true;
        }
        
        TextUtil.send(sender, "&7MMR of &f" + targetName + "&7: &e" + profile.getMmr() + " &7(" + services.mmr().rankFor(profile.getMmr()) + ")");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return args.length == 1 ? Bukkit.getOnlinePlayers().stream().map(p -> p.getName()).filter(n -> n.startsWith(args[0])).toList() : List.of();
    }

}
