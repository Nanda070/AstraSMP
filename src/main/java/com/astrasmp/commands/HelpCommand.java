package com.astrasmp.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.astrasmp.service.ServiceManager;
import com.astrasmp.util.TextUtil;

public final class HelpCommand implements org.bukkit.command.TabExecutor {
    public HelpCommand(ServiceManager services) {
        // Services not used in HelpCommand currently
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        TextUtil.send(player, "&b&l--- [ ChetCraft Help ] ---");
        TextUtil.send(player, "&f/menu &7- Главное меню сервера");
        TextUtil.send(player, "&f/balance &7- Проверить свой баланс");
        TextUtil.send(player, "&f/pay &7- Перевести монеты игроку");
        TextUtil.send(player, "&f/dm &7- Написать личное сообщение");
        TextUtil.send(player, "&f/ah &7- Аукцион предметов");
        TextUtil.send(player, "&f/link &7- Привязать Discord (&aБонус!&7)");
        TextUtil.send(player, "&f/quest &7- Список твоих заданий");
        TextUtil.send(player, "&f/rtp &7- Рандомная телепортация");
        TextUtil.send(player, "&f/contract &7- Заказать убийство или контракт");
        TextUtil.send(player, "&f/marry &7- Свадьбы и отношения");
        TextUtil.send(player, "&f/prefix &7- Управление твоим префиксом");
        TextUtil.send(player, "&f/guild &7- Управление твоей гильдией");
        TextUtil.send(player, "&b&l-------------------------");

        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) {
        return java.util.Collections.emptyList();
    }
}
