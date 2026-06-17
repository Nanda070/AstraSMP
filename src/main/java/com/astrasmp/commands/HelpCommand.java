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

        String serverName = com.astrasmp.AstraSMPPlugin.getInstance().getConfig().getString("server.name", "ChetCraft");
        TextUtil.send(player, "&b&l--- [ " + serverName + " Help ] ---");
        TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_b7cb38", "&f/menu &7- Главное меню сервера"));
        TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_ddb819", "&f/balance &7- Проверить свой баланс"));
        TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_874e33", "&f/pay &7- Перевести монеты игроку"));
        TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_f5e5c4", "&f/dm &7- Написать личное сообщение"));
        TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_8a3684", "&f/ah &7- Аукцион предметов"));
        TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_c1eee5", "&f/link &7- Привязать Discord (&aБонус!&7)"));
        TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_5a4364", "&f/quest &7- Список твоих заданий"));
        TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_ff8d16", "&f/talents &7- Дерево талантов"));
        TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_ad1da6", "&f/rewards &7- Ежедневные награды"));
        TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_2d7102", "&f/stats &7- Твоя статистика"));
        TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_ddea71", "&f/top &7- Топ игроков сервера"));
        TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_f7380a", "&f/sell &7- Продать предметы (Меню)"));
        TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_a7f267", "&f/blacksmith &7- Кузнец (Улучшение)"));
        TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_65719a", "&f/rtp &7- Рандомная телепортация"));
        TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_e34a51", "&f/spawn &7- Вернуться на спавн"));
        TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_96c062", "&f/contract &7- Заказать убийство или контракт"));
        TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_b6aea7", "&f/bounty &7- Список наград за головы"));
        TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_a94ef1", "&f/marry &7- Свадьбы и отношения"));
        TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_138445", "&f/guild &7- Управление твоей гильдией"));
        TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_b89635", "&f/prefix &7- Управление твоим префиксом"));
        TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_2ff073", "&f/feed &7- Утолить голод"));
        TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_fd488e", "&f/heal &7- Вылечить себя"));
        TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_555f57", "&f/location &7- Поделиться координатами"));
        TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_24e747", "&f/tutorial &7- Пройти обучение"));
        TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_21fd5e", "&b&l-------------------------"));

        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) {
        return java.util.Collections.emptyList();
    }
}
