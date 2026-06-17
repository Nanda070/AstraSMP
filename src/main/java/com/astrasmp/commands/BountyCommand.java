package com.astrasmp.commands;

import com.astrasmp.AstraSMPPlugin;
import com.astrasmp.gui.BountiesGui;
import com.astrasmp.model.ContractRecord;
import com.astrasmp.service.ServiceManager;
import com.astrasmp.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BountyCommand implements CommandExecutor {
    private final AstraSMPPlugin plugin;
    private final ServiceManager services;
    private final BountiesGui bountiesGui;

    public BountyCommand(AstraSMPPlugin plugin, ServiceManager services, BountiesGui bountiesGui) {
        this.plugin = plugin;
        this.services = services;
        this.bountiesGui = bountiesGui;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Только для игроков.");
            return true;
        }

        if (args.length == 0) {
            bountiesGui.open(player);
            return true;
        }

        if (args.length < 2) {
            TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_5945fa", "&cИспользование: /bounty <игрок> <сумма>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_ad54b4", "&cИгрок не найден или не в сети!"));
            return true;
        }
        
        if (target.equals(player)) {
            TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_eaff94", "&cВы не можете объявить охоту на самого себя!"));
            return true;
        }

        long reward;
        try {
            reward = Long.parseLong(args[1]);
        } catch (NumberFormatException e) {
            TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_68a06f", "&cСумма должна быть числом!"));
            return true;
        }

        if (reward < 1000) {
            TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_1388b4", "&cМинимальная награда: 1,000 ❂"));
            return true;
        }

        double feeRate = plugin.getConfig().getDouble("contracts.bounty-fee", 0.10);
        long fee = Math.max(1L, Math.round(reward * feeRate));
        long total = reward + fee;

        if (services.economy().getBalance(player.getUniqueId()) < total) {
            TextUtil.send(player, "&cНедостаточно средств! С учетом комиссии (" + (feeRate*100) + "%) нужно &f" + total + " ❂");
            return true;
        }

        ContractRecord record = services.contracts().createBounty(player, target, reward, "Bounty created via /bounty");
        if (record == null) {
            TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_bc81e5", "&cПроизошла ошибка при создании заказа."));
            return true;
        }

        // Global Announcement
        String msg = "&8[&c☠&8] &cНа игрока &f" + target.getName() + " &cобъявлена охота! Награда: &e" + reward + " ❂";
        for (Player p : Bukkit.getOnlinePlayers()) {
            TextUtil.send(p, msg);
        }

        if (plugin.getDiscord().isEnabled()) {
            plugin.getDiscord().sendBountyAnnouncement(target.getName(), reward, false, null);
        }

        return true;
    }
}
