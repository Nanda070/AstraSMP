package com.astrasmp.commands;

import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.astrasmp.service.ServiceManager;
import com.astrasmp.util.TextUtil;

public final class AuctionCommand implements org.bukkit.command.TabExecutor {
    private final ServiceManager services;
    public AuctionCommand(ServiceManager services) { this.services = services; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            TextUtil.send(sender, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_a60b9b", "&cPlayer only."));
            return true;
        }
        if (args.length == 0) {
            services.gui().openAuction(player, 0, "");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "sell" -> {
                if (args.length < 2) {
                    TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_986684", "&c/ah sell <price> [amount]"));
                    return true;
                }
                long price;
                try { price = Long.parseLong(args[1]); } catch (Exception ex) {
                    TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_6beac8", "&cНеверная цена."));
                    return true;
                }
                if (player.getInventory().getItemInMainHand() == null || player.getInventory().getItemInMainHand().getType().isAir()) {
                    TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_efad46", "&cВозьми предмет в руку."));
                    return true;
                }
                int amount = player.getInventory().getItemInMainHand().getAmount();
                if (args.length >= 3) {
                    try { amount = Math.min(amount, Integer.parseInt(args[2])); } catch (Exception ignored) {}
                }
                var stack = player.getInventory().getItemInMainHand().clone();
                stack.setAmount(amount);
                player.getInventory().getItemInMainHand().setAmount(player.getInventory().getItemInMainHand().getAmount() - amount);

                var lot = services.auction().createLot(player, stack, price);

                if (lot == null) {
                    player.getInventory().addItem(stack);
                    TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_108d80", "&cНе удалось выставить лот."));
                } else {
                    // --- КВЕСТ №5: Выставить предмет на аукцион ---
                    services.quests().processAction(player, com.astrasmp.service.QuestManager.QuestAction.AH_SELL, "", 1);
                    // ----------------------------------------------

                    TextUtil.send(player, "&aЛот #" + lot.getId() + " создан.");
                }
            }
            case "buy" -> {
                if (args.length < 2) { TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_df3f27", "&c/ah buy <id>")); return true; }
                long id = Long.parseLong(args[1]);
                if (!services.auction().buyLot(player, id)) TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_a9560c", "&cПокупка не удалась."));
                else TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_d67113", "&aПредмет куплен."));
            }
            case "cancel" -> {
                if (args.length < 2) { TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_63da23", "&c/ah cancel <id>")); return true; }
                long id = Long.parseLong(args[1]);
                if (!services.auction().cancelLot(player, id)) TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_381045", "&cОтмена не удалась."));
                else TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_155c15", "&aЛот возвращён."));
            }
            case "search" -> services.gui().openAuction(player, 0, args.length >= 2 ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)) : "");
            default -> services.gui().openAuction(player, 0, "");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("sell", "buy", "cancel", "search").stream().filter(s -> s.startsWith(args[0])).toList();
        return List.of();
    }

}
