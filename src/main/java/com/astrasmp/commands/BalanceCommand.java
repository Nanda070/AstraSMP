package com.astrasmp.commands;

import com.astrasmp.model.PlayerProfile;
import com.astrasmp.service.ServiceManager;
import com.astrasmp.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.List;

public final class BalanceCommand implements CommandExecutor, TabCompleter {
    private final ServiceManager services;

    public BalanceCommand(ServiceManager services) {
        this.services = services;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        OfflinePlayer target;

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                TextUtil.send(sender, "&cИспользуйте /balance <игрок> из консоли.");
                return true;
            }
            target = player;
        } else {
            target = Bukkit.getOfflinePlayer(args[0]);
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                TextUtil.send(sender, "&cИгрок никогда не заходил на сервер.");
                return true;
            }
        }

        // Получаем профиль для доступа ко всем валютам
        PlayerProfile profile = services.economy().profile(target.getUniqueId(), target.getName());

        if (profile == null) {
            TextUtil.send(sender, "&cОшибка: не удалось загрузить данные игрока.");
            return true;
        }

        // Красивый вывод баланса
        TextUtil.send(sender, "");
        TextUtil.send(sender, "&b&lЭкономика &8» &fБаланс игрока &e" + target.getName() + "&f:");
        TextUtil.send(sender, " &8• &fМонеты: &e" + profile.getCoins() + " ❂");
        TextUtil.send(sender, " &8• &fИвентовые очки: &d" + profile.getEventPoints() + " EP");
        TextUtil.send(sender, "");

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return List.of();
    }
}