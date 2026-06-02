package com.astrasmp.commands;

import com.astrasmp.service.QuestManager;
import com.astrasmp.service.ServiceManager;
import com.astrasmp.util.TextUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TutorialCommand implements CommandExecutor {
    private final ServiceManager services;

    public TutorialCommand(ServiceManager services) {
        this.services = services;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Только для игроков.");
            return true;
        }

        if (args.length == 0) {
            TextUtil.send(player, "&cИспользование: /tutorial <classes|quarry|me>");
            return true;
        }

        String topic = args[0].toLowerCase();

        switch (topic) {
            case "classes":
                TextUtil.send(player, "");
                TextUtil.send(player, "&b&l[AstraOP] Система классов");
                TextUtil.send(player, "&7Каждый игрок может выбрать свой класс: &cТанк, &aХиллер, &eДД &7и др.");
                TextUtil.send(player, "&7Каждый класс имеет уникальные способности и пассивные бонусы.");
                TextUtil.send(player, "&7Соберите уникальное оружие и покажите свое мастерство.");
                TextUtil.send(player, "");
                break;
            case "quarry":
                TextUtil.send(player, "");
                TextUtil.send(player, "&b&l[AstraBuild] Карьеры и механизмы");
                TextUtil.send(player, "&7Автоматизируйте добычу с помощью Карьеров (Quarry).");
                TextUtil.send(player, "&7Вам нужен &eКонтроллер Карьера&7 и &eТрубы&7 для перенаправления ресурсов.");
                TextUtil.send(player, "&7Выделите зону добычи и подайте топливо, чтобы карьер начал работу!");
                TextUtil.send(player, "");
                break;
            case "me":
                TextUtil.send(player, "");
                TextUtil.send(player, "&b&l[AstraSMP] Цифровая память (МЭ Сеть)");
                TextUtil.send(player, "&7МЭ Сеть позволяет хранить миллионы предметов в одном блоке!");
                TextUtil.send(player, "&7Для старта скрафтите &eМЭ Терминал&7, &eМЭ Накопитель&7 и &eЯчейки памяти&7.");
                TextUtil.send(player, "&7Вставьте ячейку в накопитель и подключите терминал для доступа ко всем вещам.");
                TextUtil.send(player, "");
                break;
            default:
                TextUtil.send(player, "&cНеизвестная тема. Доступные: classes, quarry, me");
                return true;
        }

        // Notify QuestManager
        services.quests().processAction(player, QuestManager.QuestAction.USE_COMMAND, "/tutorial " + topic, 1);

        return true;
    }
}
