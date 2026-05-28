package com.astrasmp.commands;

import com.astrasmp.service.ServiceManager;
import com.astrasmp.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.List;

public final class PayCommand implements CommandExecutor, TabCompleter {
    private final ServiceManager services;
    public PayCommand(ServiceManager services) { this.services = services; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player from)) {
            TextUtil.send(sender, "&cPlayer only.");
            return true;
        }
        if (args.length < 2) {
            TextUtil.send(from, "&c/pay <player> <amount>");
            return true;
        }
        Player to = Bukkit.getPlayerExact(args[0]);
        if (to == null) {
            TextUtil.send(from, "&cИгрок не найден.");
            return true;
        }
        long amount;
        try { amount = Long.parseLong(args[1]); } catch (NumberFormatException ex) {
            TextUtil.send(from, "&cНеверная сумма.");
            return true;
        }
        if (!services.economy().pay(from, to, amount)) {
            TextUtil.send(from, "&cНедостаточно coins.");
            return true;
        }
        TextUtil.send(from, "&aПеревод выполнен.");
        TextUtil.send(to, "&aВы получили &f" + amount + " &acoins от &f" + from.getName());
        services.discord().sendLog("Pay: " + from.getName() + " -> " + to.getName() + " amount=" + amount);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return args.length == 1 ? Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.startsWith(args[0])).toList() : List.of();
    }
}
