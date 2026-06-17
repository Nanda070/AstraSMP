package com.astrasmp.commands;

import org.bukkit.Bukkit;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.astrasmp.model.ContractRecord;
import com.astrasmp.service.ServiceManager;
import com.astrasmp.util.TextUtil;

public final class ContractCommand implements org.bukkit.command.TabExecutor {
    private final ServiceManager services;
    public ContractCommand(ServiceManager services) { this.services = services; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            TextUtil.send(sender, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_446f98", "&8Active contracts:"));
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
                TextUtil.send(sender, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_a60b9b", "&cPlayer only."));
                return true;
            }
            if (args.length < 4 || !args[1].equalsIgnoreCase("bounty")) return usage(sender, "/contract create bounty <player> <reward> [note]");
            Player onlineTarget = Bukkit.getPlayerExact(args[2]);
            if (onlineTarget == null) {
                TextUtil.send(sender, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_1d3ab6", "&cTarget must be online in this base."));
                return true;
            }
            long reward;
            try {
                reward = Long.parseLong(args[3]);
            } catch (NumberFormatException e) {
                TextUtil.send(sender, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_7ae596", "&cОшибка: количество должно быть числом."));
                return true;
            }
            if (reward <= 0) {
                TextUtil.send(sender, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_1d3ab7", "&cНаграда должна быть больше нуля."));
                return true;
            }
            String note = args.length >= 5 ? String.join(" ", java.util.Arrays.copyOfRange(args, 4, args.length)) : "";
            ContractRecord record = services.contracts().createBounty(creator, onlineTarget, reward, note);
            if (record == null) TextUtil.send(sender, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_d1d9aa", "&cNot enough coins."));
            else TextUtil.send(sender, "&aBounty created #" + record.getId());
            return true;
        }

        return usage(sender, "&c/contract list | /contract create bounty <player> <reward> [note] | /contract cancel <id>");
    }

    private boolean usage(CommandSender sender, String msg) {
        TextUtil.send(sender, msg);
        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(@org.jetbrains.annotations.NotNull CommandSender sender, @org.jetbrains.annotations.NotNull Command command, @org.jetbrains.annotations.NotNull String alias, String[] args) {
        if (args.length == 1) {
            java.util.List<String> subcommands = java.util.List.of("list", "create", "cancel");
            return subcommands.stream().filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        } else if (args.length == 2 && args[0].equalsIgnoreCase("create")) {
            java.util.List<String> types = java.util.List.of("bounty");
            return types.stream().filter(s -> s.startsWith(args[1].toLowerCase())).toList();
        } else if (args.length == 3 && args[0].equalsIgnoreCase("create") && args[1].equalsIgnoreCase("bounty")) {
            return null; // autocomplete players
        } else if (args.length == 4 && args[0].equalsIgnoreCase("create")) {
            return java.util.List.of("<награда_в_монетах>");
        } else if (args.length == 5 && args[0].equalsIgnoreCase("create")) {
            return java.util.List.of("[причина...]");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("cancel")) {
            return java.util.List.of("<ID_контракта>");
        }
        return java.util.Collections.emptyList();
    }

}
