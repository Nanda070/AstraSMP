package com.astrasmp.commands;

import com.astrasmp.gui.TalentsGui;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TalentsCommand implements CommandExecutor {
    private final TalentsGui talentsGui;

    public TalentsCommand(TalentsGui talentsGui) {
        this.talentsGui = talentsGui;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Только для игроков.");
            return true;
        }
        
        talentsGui.open(player);
        return true;
    }
}
