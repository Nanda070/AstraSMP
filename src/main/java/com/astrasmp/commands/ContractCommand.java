package com.astrasmp.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.astrasmp.model.ContractRecord;
import com.astrasmp.service.ServiceManager;
import com.astrasmp.util.TextUtil;

public final class ContractCommand implements CommandExecutor {
    private final ServiceManager services;
    public ContractCommand(ServiceManager services) { this.services = services; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            TextUtil.send(sender, "&8Active contracts:");
            for (ContractRecord c : services.contracts().active()) {
                TextUtil.send(sender, "&7#" + c.getId() + " &f" + c.getType() + " &7target=&f" + c.getTargetUuid() + " &7reward=&a" + c.getReward());
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("cancel")) {
            if (args.length < 2) return usage(sender, "/contract cancel <id>");
            long id = Long.parseLong(args[1]);
            boolean ok = sender instanceof Player p && services.contracts().cancel(p, id);
            TextUtil.send(sender, ok ? "&aCancelled." : "&cCancel failed.");
            return true;
        }

        if (args[0].equalsIgnoreCase("create")) {
            if (!(sender instanceof Player creator)) {
                TextUtil.send(sender, "&cPlayer only.");
                return true;
            }
            if (args.length < 4 || !args[1].equalsIgnoreCase("bounty")) return usage(sender, "/contract create bounty <player> <reward> [note]");
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
            if (!(target instanceof Player onlineTarget)) {
                TextUtil.send(sender, "&cTarget must be online in this base.");
                return true;
            }
            long reward = Long.parseLong(args[3]);
            String note = args.length >= 5 ? String.join(" ", java.util.Arrays.copyOfRange(args, 4, args.length)) : "";
            ContractRecord record = services.contracts().createBounty(creator, onlineTarget, reward, note);
            if (record == null) TextUtil.send(sender, "&cNot enough coins.");
            else TextUtil.send(sender, "&aBounty created #" + record.getId());
            return true;
        }

        return usage(sender, "&c/contract list | /contract create bounty <player> <reward> [note] | /contract cancel <id>");
    }

    private boolean usage(CommandSender sender, String msg) {
        TextUtil.send(sender, msg);
        return true;
    }
}
