package com.astrasmp.commands;

import com.astrasmp.AstraSMPPlugin;
import com.astrasmp.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public final class GodCommand implements org.bukkit.command.TabExecutor {
    private final NamespacedKey godKey;

    public GodCommand(AstraSMPPlugin plugin) {
        this.godKey = new NamespacedKey(plugin, "god_mode");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission("astrasmp.admin")) {
            TextUtil.send(sender, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_34bdb4", "&cУ вас нет прав для использования этой команды."));
            return true;
        }

        Player target = null;
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                TextUtil.send(sender, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_d8fdb1", "&cКонсоль должна указать ник игрока."));
                return true;
            }
            target = player;
        } else {
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                TextUtil.send(sender, "&cИгрок &e" + args[0] + " &cне найден.");
                return true;
            }
        }

        boolean current = target.getPersistentDataContainer().getOrDefault(godKey, PersistentDataType.BYTE, (byte) 0) == 1;
        byte newState = current ? (byte) 0 : (byte) 1;
        target.getPersistentDataContainer().set(godKey, PersistentDataType.BYTE, newState);

        if (newState == 1) {
            TextUtil.send(target, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_18f514", "&aРежим бога включен."));
            if (target != sender) {
                TextUtil.send(sender, "&aВы включили режим бога для &e" + target.getName() + "&a.");
            }
        } else {
            TextUtil.send(target, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_5f264a", "&cРежим бога выключен."));
            if (target != sender) {
                TextUtil.send(sender, "&cВы выключили режим бога для &e" + target.getName() + "&c.");
            }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            return null; // Возвращает список игроков онлайн по умолчанию
        }
        return Collections.emptyList();
    }
}
