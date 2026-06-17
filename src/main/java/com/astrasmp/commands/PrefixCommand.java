package com.astrasmp.commands;

import com.astrasmp.model.PlayerProfile;
import com.astrasmp.service.ServiceManager;
import com.astrasmp.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PrefixCommand implements org.bukkit.command.TabExecutor {
    private final ServiceManager services;
    private final List<String> colors = List.of("Red", "Green", "Blue", "Gold", "Yellow", "Aqua", "Purple", "White");

    public PrefixCommand(ServiceManager services) {
        this.services = services;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (args.length < 3) {
            TextUtil.send(sender, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_1be08a", "&cИспользование: /prefix <игрок> <текст> <цвет>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            TextUtil.send(sender, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_426f15", "&cИгрок не найден."));
            return true;
        }

        String prefixText = args[1];
        String colorName = args[2];
        String colorCode = getColorCode(colorName);

        PlayerProfile profile = services.economy().profile(target.getUniqueId(), target.getName());
        profile.setCustomPrefix(prefixText);
        profile.setPrefixColor(colorCode);

        services.store().requestSave();
        TextUtil.send(sender, "&aИгроку &f" + target.getName() + " &aустановлен префикс: " + colorCode + "[" + prefixText + "]");
        return true;
    }

    // Тот самый метод для "предложений" (Tab Completion)
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String alias, String[] args) {
        if (args.length == 1) return null; // null вернет список онлайн игроков (стандартно)
        if (args.length == 2) return List.of("Префикс_Здесь");
        if (args.length == 3) {
            return colors.stream().filter(c -> c.toLowerCase().startsWith(args[2].toLowerCase())).toList();
        }
        return new ArrayList<>();
    }

    private String getColorCode(String name) {
        return switch (name.toLowerCase()) {
            case "red" -> "&c";
            case "green" -> "&a";
            case "blue" -> "&b";
            case "gold" -> "&6";
            case "yellow" -> "&e";
            case "aqua" -> "&3";
            case "purple" -> "&d";
            default -> "&7";
        };
    }

}
