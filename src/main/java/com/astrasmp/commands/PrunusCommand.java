package com.astrasmp.commands;

import com.astrasmp.service.ServiceManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PrunusCommand implements CommandExecutor, TabCompleter {

    private final ServiceManager services;

    public PrunusCommand(ServiceManager services) {
        this.services = services;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (args.length == 0) {
            // Вход в свое измерение
            if (!services.store().profile(player.getUniqueId().toString(), player.getName()).isUnlockedPocketDimension()) {
                player.sendMessage("§cВы еще не открыли свое Карманное Измерение.");
                return true;
            }
            services.pockets().enterPocket(player, player.getUniqueId());
            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("invite")) {
            // Пригласить игрока
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                player.sendMessage("§cИгрок не найден.");
                return true;
            }
            if (!services.store().profile(player.getUniqueId().toString(), player.getName()).isUnlockedPocketDimension()) {
                player.sendMessage("§cСначала разблокируйте измерение.");
                return true;
            }
            services.pockets().invitePlayer(player, target);
            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("join")) {
            // Войти по инвайту
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                player.sendMessage("§cИгрок не найден.");
                return true;
            }
            if (services.pockets().hasInvite(target.getUniqueId(), player.getUniqueId())) {
                services.pockets().enterPocket(player, target.getUniqueId());
            } else {
                player.sendMessage("§cУ вас нет приглашения от этого игрока.");
            }
            return true;
        }

        player.sendMessage("§eИспользование: §f/prunus [invite|join] [игрок]");
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("invite");
            completions.add("join");
        } else if (args.length == 2) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                completions.add(p.getName());
            }
        }
        return completions;
    }
}
