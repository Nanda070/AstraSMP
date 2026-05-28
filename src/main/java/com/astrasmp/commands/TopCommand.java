package com.astrasmp.commands;

import com.astrasmp.model.PlayerProfile;
import com.astrasmp.service.ServiceManager;
import com.astrasmp.util.TextUtil;
import org.bukkit.command.*;

import java.util.List;

public final class TopCommand implements CommandExecutor {
    private final ServiceManager services;
    public TopCommand(ServiceManager services) { this.services = services; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String type = args.length == 0 ? "wealth" : args[0].toLowerCase();
        List<PlayerProfile> top = switch (type) {
            case "kills" -> services.leaderboard().topKills(10);
            case "sold" -> services.leaderboard().topSold(10);
            case "events" -> services.leaderboard().topEvents(10);
            case "mmr" -> services.leaderboard().topMmr(10);
            default -> services.leaderboard().topCoins(10);
        };
        TextUtil.send(sender, "&8Top " + type + ":");
        int i = 1;
        for (PlayerProfile p : top) {
            TextUtil.send(sender, "&7" + i++ + ". &f" + p.getName() + " &8- &a" + switch (type) {
                case "kills" -> p.getKills();
                case "sold" -> p.getSoldValue();
                case "events" -> p.getEventPoints();
                case "mmr" -> p.getMmr();
                default -> p.getCoins();
            });
        }
        return true;
    }
}
