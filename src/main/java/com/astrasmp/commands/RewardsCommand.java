package com.astrasmp.commands;

import com.astrasmp.gui.RewardsGui;
import com.astrasmp.service.ServiceManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class RewardsCommand implements CommandExecutor {
    private final RewardsGui rewardsGui;

    public RewardsCommand(ServiceManager services, RewardsGui rewardsGui) {
        this.rewardsGui = rewardsGui;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Только для игроков.");
            return true;
        }
        
        rewardsGui.open(player);
        return true;
    }
}
