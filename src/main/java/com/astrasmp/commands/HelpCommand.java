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
        TextUtil.send(player, "&f/talents &7- Дерево талантов");
        TextUtil.send(player, "&f/rewards &7- Ежедневные награды");
        TextUtil.send(player, "&f/stats &7- Твоя статистика");
        TextUtil.send(player, "&f/top &7- Топ игроков сервера");
        TextUtil.send(player, "&f/sell &7- Продать предметы (Меню)");
        TextUtil.send(player, "&f/blacksmith &7- Кузнец (Улучшение)");
        TextUtil.send(player, "&f/rtp &7- Рандомная телепортация");
        TextUtil.send(player, "&f/spawn &7- Вернуться на спавн");
        TextUtil.send(player, "&f/contract &7- Заказать убийство или контракт");
        TextUtil.send(player, "&f/bounty &7- Список наград за головы");
        TextUtil.send(player, "&f/marry &7- Свадьбы и отношения");
        TextUtil.send(player, "&f/guild &7- Управление твоей гильдией");
        TextUtil.send(player, "&f/prefix &7- Управление твоим префиксом");
        TextUtil.send(player, "&f/feed &7- Утолить голод");
        TextUtil.send(player, "&f/heal &7- Вылечить себя");
        TextUtil.send(player, "&f/location &7- Поделиться координатами");
        TextUtil.send(player, "&f/tutorial &7- Пройти обучение");
        TextUtil.send(player, "&b&l-------------------------");

        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) {
        return java.util.Collections.emptyList();
    }
}
