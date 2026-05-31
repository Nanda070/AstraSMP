package com.astrasmp.commands;

import com.astrasmp.model.PlayerProfile;
import com.astrasmp.service.ServiceManager;
import com.astrasmp.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public final class MarryCommand implements CommandExecutor, TabCompleter {
    private final ServiceManager services;
    // Карта для хранения активных предложений: Кому пришло -> Кто отправил
    private final Map<UUID, UUID> pendingRequests = new HashMap<>();

    public MarryCommand(ServiceManager services) {
        this.services = services;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (args.length == 0) {
            return sendUsage(player);
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "status" -> {
                String targetName = args.length > 1 ? args[1] : player.getName();
                OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
                PlayerProfile profile = services.economy().profile(target.getUniqueId(), target.getName());

                if (profile.getPartnerUuid() == null || profile.getPartnerUuid().isEmpty()) {
                    TextUtil.send(player, "&f" + targetName + " &7сейчас одинок(а).");
                } else {
                    UUID partnerUuid = UUID.fromString(profile.getPartnerUuid());
                    OfflinePlayer partner = Bukkit.getOfflinePlayer(partnerUuid);
                    TextUtil.send(player, "&f" + targetName + " &7в браке с &d" + (partner.getName() != null ? partner.getName() : "неизвестным игроком"));
                }
                return true;
            }

            case "acc" -> {
                UUID requesterId = pendingRequests.remove(player.getUniqueId());
                if (requesterId == null) {
                    TextUtil.send(player, "&cУ вас нет активных предложений.");
                    return true;
                }

                Player requester = Bukkit.getPlayer(requesterId);
                if (requester == null) {
                    TextUtil.send(player, "&cИгрок, сделавший предложение, вышел из сети.");
                    return true;
                }

                completeMarriage(requester, player);
                return true;
            }

            case "dec" -> {
                if (pendingRequests.remove(player.getUniqueId()) == null) {
                    TextUtil.send(player, "&cУ вас нет активных предложений.");
                    return true;
                }
                TextUtil.send(player, "&7Вы отклонили предложение.");
                return true;
            }

            case "unmarry" -> {
                PlayerProfile profile = services.economy().profile(player.getUniqueId(), player.getName());
                if (profile.getPartnerUuid().isEmpty()) {
                    TextUtil.send(player, "&cВы не состоите в браке.");
                    return true;
                }

                UUID partnerId = UUID.fromString(profile.getPartnerUuid());
                profile.setPartnerUuid("");

                // Пытаемся обнулить профиль партнера
                PlayerProfile partnerProfile = services.economy().profile(partnerId, "Partner");
                partnerProfile.setPartnerUuid("");

                Bukkit.broadcastMessage(TextUtil.color("&8[&d❤&8] &f" + player.getName() + " &7развелся. Теперь он снова свободен!"));
                services.store().requestSave();
                return true;
            }

            default -> {
                // Если аргумент не подкоманда, значит это ник игрока
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null) {
                    TextUtil.send(player, "&cИгрок не найден.");
                    return true;
                }

                if (target.equals(player)) {
                    TextUtil.send(player, "&cВы не можете жениться на самом себе.");
                    return true;
                }

                PlayerProfile p1 = services.economy().profile(player.getUniqueId(), player.getName());
                PlayerProfile p2 = services.economy().profile(target.getUniqueId(), target.getName());

                if (!p1.getPartnerUuid().isEmpty()) {
                    TextUtil.send(player, "&cВы уже женаты/замужем.");
                    return true;
                }
                if (!p2.getPartnerUuid().isEmpty()) {
                    TextUtil.send(player, "&cЭтот игрок уже в браке.");
                    return true;
                }

                pendingRequests.put(target.getUniqueId(), player.getUniqueId());
                TextUtil.send(player, "&7Вы отправили предложение &f" + target.getName());
                TextUtil.send(target, "&d" + player.getName() + " &7сделал(а) вам предложение! &f/marry acc &7или &f/marry dec");
                return true;
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void completeMarriage(Player p1, Player p2) {
        PlayerProfile profile1 = services.economy().profile(p1.getUniqueId(), p1.getName());
        PlayerProfile profile2 = services.economy().profile(p2.getUniqueId(), p2.getName());

        profile1.setPartnerUuid(p2.getUniqueId().toString());
        profile2.setPartnerUuid(p1.getUniqueId().toString());

        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(TextUtil.color("&d&l❤ СВАДЬБА ❤"));
        Bukkit.broadcastMessage(TextUtil.color("&f" + p1.getName() + " &7и &f" + p2.getName() + " &7теперь женаты!"));
        Bukkit.broadcastMessage("");

        p1.playSound(p1.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1, 1);
        p2.playSound(p2.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1, 1);

        services.store().requestSave();
    }

    private boolean sendUsage(Player p) {
        TextUtil.send(p, "&d&lСвадьбы:");
        TextUtil.send(p, "&e/marry <ник> &7- сделать предложение");
        TextUtil.send(p, "&e/marry status [ник] &7- проверить статус");
        TextUtil.send(p, "&e/marry acc &7- принять предложение");
        TextUtil.send(p, "&e/marry dec &7- отказать");
        TextUtil.send(p, "&e/unmarry &7- развестись");
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            return List.of("status", "acc", "dec", "unmarry");
        }
        return null;
    }
}