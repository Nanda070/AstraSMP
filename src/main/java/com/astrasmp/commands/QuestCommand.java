package com.astrasmp.commands;

import com.astrasmp.service.ServiceManager;
import com.astrasmp.util.TextUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class QuestCommand implements org.bukkit.command.TabExecutor {
    private final ServiceManager services;

    public QuestCommand(ServiceManager services) {
        this.services = services;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            TextUtil.send(sender, "&cТолько для игроков.");
            return true;
        }

        // Мы вызываем метод из QuestManager, который делает всё:
        // проверяет шаг, прогресс, награды и выводит красиво в чат.
        services.quests().sendQuestInfo(player);
        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) {
        return java.util.Collections.emptyList();
    }
}
