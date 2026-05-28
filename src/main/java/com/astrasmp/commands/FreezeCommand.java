package com.astrasmp.commands;

import com.astrasmp.model.PlayerProfile;
import com.astrasmp.service.ServiceManager;
import com.astrasmp.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class FreezeCommand implements CommandExecutor {
    private final ServiceManager services;

    public FreezeCommand(ServiceManager services) {
        this.services = services;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(TextUtil.color("&cУ вас нет прав!"));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(TextUtil.color("&eИспользование: &f/" + label + " <ник>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(TextUtil.color("&cИгрок не найден или оффлайн!"));
            return true;
        }

        PlayerProfile profile = services.economy().profile(target.getUniqueId(), target.getName());
        boolean freeze = label.equalsIgnoreCase("freeze");

        profile.setFrozen(freeze);

        if (freeze) {
            TextUtil.send(sender, "&bВы заморозили игрока &f" + target.getName());
            TextUtil.send(target, "&b&lВНИМАНИЕ! &cВы были заморожены администратором. Запрещено двигаться и взаимодействовать с миром.");
        } else {
            TextUtil.send(sender, "&aВы разморозили игрока &f" + target.getName());
            TextUtil.send(target, "&a&lВНИМАНИЕ! &aВы были разморожены.");
        }

        return true;
    }
}