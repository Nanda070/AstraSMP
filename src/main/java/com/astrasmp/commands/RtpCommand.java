package com.astrasmp.commands;

import com.astrasmp.gui.RtpGui;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class RtpCommand implements org.bukkit.command.TabExecutor {
    private final RtpGui gui;

    public RtpCommand(RtpGui gui) { this.gui = gui; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (sender instanceof Player player) {
            gui.open(player);
        }
        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) {
        return java.util.Collections.emptyList();
    }
}
