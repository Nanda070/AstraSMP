package com.astrasmp.commands;

import com.astrasmp.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class DiscordCommand implements org.bukkit.command.TabExecutor {

    private static final String DISCORD_LINK = "discord.gg/cheterin";
    private static final String DISCORD_URL  = "https://discord.gg/cheterin";

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) {
                sendDiscordMessage(player);
            } else {
                TextUtil.send(sender, "&bСсылка на Discord: &f" + DISCORD_LINK);
            }
        } else {
            // Send to another player — admin only
            if (!sender.hasPermission("astrasmp.admin")) {
                TextUtil.send(sender, "&cУ вас нет прав отправлять ссылку другому игроку.");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                TextUtil.send(sender, "&cИгрок &e" + args[0] + " &cне найден.");
                return true;
            }
            sendDiscordMessage(target);
            TextUtil.send(sender, "&aВы отправили ссылку на Discord игроку &e" + target.getName() + "&a.");
        }
        return true;
    }

    private void sendDiscordMessage(Player player) {
        LegacyComponentSerializer legacy = LegacyComponentSerializer.legacyAmpersand();

        player.sendMessage(legacy.deserialize("&8&m                              "));
        player.sendMessage(legacy.deserialize(" &b&lChetCraft &7\u2014 Discord"));
        player.sendMessage(legacy.deserialize(" &7\u041f\u0440\u0438\u0441\u043e\u0435\u0434\u0438\u043d\u044f\u0439\u0441\u044f \u043a \u043d\u0430\u0448\u0435\u043c\u0443 \u0441\u0435\u0440\u0432\u0435\u0440\u0443:"));

        // Clickable Adventure link component
        Component link = legacy.deserialize(" &b&l\u00bb &f&n" + DISCORD_LINK)
                .clickEvent(ClickEvent.openUrl(DISCORD_URL))
                .hoverEvent(HoverEvent.showText(legacy.deserialize("&7\u041d\u0430\u0436\u043c\u0438\u0442\u0435, \u0447\u0442\u043e\u0431\u044b \u043e\u0442\u043a\u0440\u044b\u0442\u044c \u0441\u0441\u044b\u043b\u043a\u0443")));

        player.sendMessage(link);
        player.sendMessage(legacy.deserialize("&8&m                              "));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("astrasmp.admin")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(p -> p.getName())
                    .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
