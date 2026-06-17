package com.astrasmp.commands;

import com.astrasmp.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public final class HealCommand implements org.bukkit.command.TabExecutor {
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

        target.setHealth(target.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue());
        target.setFoodLevel(20);
        target.setSaturation(20f);
        target.setFireTicks(0);
        
        TextUtil.send(target, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_06c3ee", "&aВаше здоровье и сытость полностью восстановлены."));
        if (target != sender) {
            TextUtil.send(sender, "&aВы исцелили игрока &e" + target.getName() + "&a.");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            return null;
        }
        return Collections.emptyList();
    }
}
