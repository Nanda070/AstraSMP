package com.astrasmp.commands;

import com.astrasmp.model.LinkRecord;
import com.astrasmp.service.ServiceManager;
import com.astrasmp.util.TextUtil;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.security.SecureRandom;

public final class LinkCommand implements org.bukkit.command.TabExecutor {
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final ServiceManager services;
    private final SecureRandom random = new SecureRandom();

    public LinkCommand(ServiceManager services) {
        this.services = services;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            TextUtil.send(sender, "&cPlayer only.");
            return true;
        }
        String code = generate();
        LinkRecord record = services.store().link(player.getUniqueId().toString());
        record.setCode(code);
        record.setVerified(false);
        services.store().requestSave();
        // Квест LINK_DISCORD засчитывается в DiscordBridge после реальной верификации
        TextUtil.send(player, "&aКод привязки: &f" + code);
        TextUtil.send(player, "&7Отправь в Discord: &f" + services.discordPrefix() + " link " + code);
        services.discord().sendLog("Link requested for " + player.getName() + " code=" + code);
        return true;
    }

    private String generate() {
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++)
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        return sb.toString();
    }

    @Override
    public java.util.List<String> onTabComplete(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) {
        return java.util.Collections.emptyList();
    }
}
