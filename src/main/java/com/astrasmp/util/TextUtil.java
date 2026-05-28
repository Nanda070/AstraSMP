package com.astrasmp.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public final class TextUtil {
    private TextUtil() {}

    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    public static void send(CommandSender sender, String text) {
        sender.sendMessage(color(text));
    }

    public static void send(Player player, String text) {
        player.sendMessage(color(text));
    }

    public static String prefix(String message) {
        return color("&8[&dAstraSMP&8] &7") + color(message);
    }

    public static List<String> colorList(List<String> lines) {
        return lines.stream().map(TextUtil::color).toList();
    }
}
