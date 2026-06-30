package com.astrasmp.commands;

import com.astrasmp.model.PlayerProfile;
import com.astrasmp.service.ServiceManager;
import com.astrasmp.util.TextUtil;
import org.bukkit.Bukkit;

import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.List;

public final class BalanceCommand implements org.bukkit.command.TabExecutor {
    private final ServiceManager services;

    public BalanceCommand(ServiceManager services) {
        this.services = services;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        java.util.UUID targetUuid;
        String targetName;

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                TextUtil.send(sender, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_06f00f", "&cИспользуйте /balance <игрок> из консоли."));
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

        // Получаем профиль для доступа ко всем валютам
        PlayerProfile profile = services.economy().profile(targetUuid, targetName);

        if (profile == null) {
            TextUtil.send(sender, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_0bb183", "&cОшибка: не удалось загрузить данные игрока."));
            return true;
        }

        // Красивый вывод баланса
        TextUtil.send(sender, "");
        TextUtil.send(sender, "&b&lЭкономика &8» &fБаланс игрока &e" + targetName + "&f:");
        TextUtil.send(sender, " &8• &fМонеты: &e" + profile.getCoins() + " ❂");
        TextUtil.send(sender, " &8• &fИвентовые очки: &d" + profile.getEventPoints() + " EP");
        TextUtil.send(sender, "");

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(p -> p.getName())
                    .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return List.of();
    }

}
