package com.astrasmp.commands;

import com.astrasmp.util.TextUtil;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles: /ban, /unban, /banip, /kick
 *
 * Uses Bukkit console commands internally to avoid BanList generic API ambiguity in 1.21.
 */
public final class BanCommand implements org.bukkit.command.TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission("astrasmp.admin")) {
            TextUtil.send(sender, "&cУ вас нет прав для использования этой команды.");
            return true;
        }

        switch (label.toLowerCase()) {
            case "ban"   -> handleBan(sender, args);
            case "unban" -> handleUnban(sender, args);
            case "banip" -> handleBanIp(sender, args);
            case "kick"  -> handleKick(sender, args);
        }
        return true;
    }

    // ─────────────────────────────────────────────
    //  /ban <player> [reason]
    // ─────────────────────────────────────────────
    private void handleBan(CommandSender sender, String[] args) {
        if (args.length == 0) {
            TextUtil.send(sender, "&cИспользование: /ban <игрок> [причина]");
            return;
        }
        String targetName = args[0];
        String reason = args.length > 1
                ? String.join(" ", Arrays.copyOfRange(args, 1, args.length))
                : "Заблокирован администратором.";

        // Dispatch vanilla ban command via console to bypass generic API
        String banCmd = "ban " + targetName + " " + reason;
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), banCmd);

        // Kick the player if online
        Player online = Bukkit.getPlayerExact(targetName);
        if (online != null) {
            online.kick(legacy("&cВы были заблокированы.\n&7Причина: &f" + reason));
        }

        TextUtil.send(sender, "&aИгрок &e" + targetName + " &aзаблокирован. Причина: &f" + reason);
        Bukkit.broadcast(legacy("&8[&cБан&8] &e" + targetName + " &7заблокирован. Причина: &f" + reason));
    }

    // ─────────────────────────────────────────────
    //  /unban <player>
    // ─────────────────────────────────────────────
    private void handleUnban(CommandSender sender, String[] args) {
        if (args.length == 0) {
            TextUtil.send(sender, "&cИспользование: /unban <игрок>");
            return;
        }
        String targetName = args[0];

        // Use vanilla pardon command via console
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "pardon " + targetName);
        TextUtil.send(sender, "&aИгрок &e" + targetName + " &aразблокирован.");
    }

    // ─────────────────────────────────────────────
    //  /banip <player|IP> [reason]
    // ─────────────────────────────────────────────
    private void handleBanIp(CommandSender sender, String[] args) {
        if (args.length == 0) {
            TextUtil.send(sender, "&cИспользование: /banip <игрок|IP> [причина]");
            return;
        }
        String target = args[0];
        String reason = args.length > 1
                ? String.join(" ", Arrays.copyOfRange(args, 1, args.length))
                : "Заблокирован по IP администратором.";

        // Resolve IP from online player if possible
        Player online = Bukkit.getPlayerExact(target);
        String ip = null;
        if (online != null && online.getAddress() != null) {
            ip = online.getAddress().getAddress().getHostAddress();
        } else {
            ip = target;
        }

        // Dispatch vanilla ban-ip command
        String banIpCmd = "ban-ip " + ip + " " + reason;
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), banIpCmd);

        if (online != null) {
            online.kick(legacy("&cВы были заблокированы по IP.\n&7Причина: &f" + reason));
        }
        TextUtil.send(sender, "&aIP &e" + ip + " &aзаблокирован. Причина: &f" + reason);
    }

    // ─────────────────────────────────────────────
    //  /kick <player> [reason]
    // ─────────────────────────────────────────────
    private void handleKick(CommandSender sender, String[] args) {
        if (args.length == 0) {
            TextUtil.send(sender, "&cИспользование: /kick <игрок> [причина]");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            TextUtil.send(sender, "&cИгрок &e" + args[0] + " &cне найден или не в сети.");
            return;
        }
        String reason = args.length > 1
                ? String.join(" ", Arrays.copyOfRange(args, 1, args.length))
                : "Выгнан с сервера администратором.";

        target.kick(legacy("&cВы были выгнаны.\n&7Причина: &f" + reason));
        TextUtil.send(sender, "&aИгрок &e" + target.getName() + " &aвыгнан. Причина: &f" + reason);
        Bukkit.broadcast(legacy("&8[&eКик&8] &e" + target.getName() + " &7выгнан. Причина: &f" + reason));
    }

    // ─────────────────────────────────────────────
    //  Tab Completion
    // ─────────────────────────────────────────────
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (!sender.hasPermission("astrasmp.admin")) return Collections.emptyList();

        String label = alias.toLowerCase();

        if (args.length == 1) {
            List<String> names = Bukkit.getOnlinePlayers().stream()
                    .map(p -> p.getName())
                    .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toCollection(ArrayList::new));

            if ("unban".equals(label)) {
                // Also suggest banned offline players
                for (OfflinePlayer op : Bukkit.getBannedPlayers()) {
                    if (op.getName() != null && op.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                        names.add(op.getName());
                    }
                }
            }
            return names;
        }

        if (args.length == 2 && !"unban".equals(label)) {
            return List.of("<причина>");
        }

        return Collections.emptyList();
    }

    // ─────────────────────────────────────────────
    //  Helper
    // ─────────────────────────────────────────────
    private static net.kyori.adventure.text.Component legacy(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }
}
