package com.astrasmp.commands;

import com.astrasmp.AstraSMPPlugin;
import com.astrasmp.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VanishCommand implements org.bukkit.command.TabExecutor, Listener {
    private final AstraSMPPlugin plugin;
    private static final Set<UUID> vanished = ConcurrentHashMap.newKeySet();

    public VanishCommand(AstraSMPPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            TextUtil.send(sender, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_6c0497", "&cТолько для игроков."));
            return true;
        }

        if (!player.hasPermission("astrasmp.admin")) {
            TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_34bdb4", "&cУ вас нет прав для использования этой команды."));
            return true;
        }

        if (label.equalsIgnoreCase("unvanish")) {
            if (vanished.remove(player.getUniqueId())) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.showPlayer(plugin, player);
                }
                TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_f44f84", "&aВы вышли из ваниша (стали видимым)."));
            } else {
                TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_d8821b", "&cВы и так не в ванише."));
            }
        } else {
            if (vanished.add(player.getUniqueId())) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!p.hasPermission("astrasmp.admin")) {
                        p.hidePlayer(plugin, player);
                    }
                }
                TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_4f019f", "&aВы вошли в ваниш (стали невидимым)."));
            } else {
                TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_9fff7d", "&cВы уже в ванише. Используйте /unvanish чтобы выйти."));
            }
        }
        return true;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player joined = event.getPlayer();
        // Если игрок без прав, скрываем от него всех ванишнутых
        if (!joined.hasPermission("astrasmp.admin")) {
            for (UUID uuid : vanished) {
                Player vPlayer = Bukkit.getPlayer(uuid);
                if (vPlayer != null) {
                    joined.hidePlayer(plugin, vPlayer);
                }
            }
        }
    }

    @Override
    public java.util.List<String> onTabComplete(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) {
        return java.util.Collections.emptyList();
    }
}
