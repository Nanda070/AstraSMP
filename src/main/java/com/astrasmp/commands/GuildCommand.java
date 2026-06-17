package com.astrasmp.commands;

import com.astrasmp.items.ItemRegistry;
import com.astrasmp.model.Guild;
import com.astrasmp.service.ServiceManager;
import com.astrasmp.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class GuildCommand implements org.bukkit.command.TabExecutor {

    private final ServiceManager services;

    public GuildCommand(ServiceManager services) {
        this.services = services;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;

        // ── Команды, доступные БЕЗ гильдии ─────────────────────────────────────

        if (args.length >= 1) {
            switch (args[0].toLowerCase()) {

                case "accept" -> {
                    // БАГ-FIX: проверяем членство ДО извлечения инвайта
                    if (services.guilds().getPlayerGuild(player.getUniqueId()) != null) {
                        TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_c94cb9", "&cВы уже состоите в гильдии! Сначала покиньте её через /guild leave."));
                        return true;
                    }
                    UUID guildId = services.guilds().getPendingInvite(player.getUniqueId());
                    if (guildId == null) {
                        TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_874df7", "&cУ вас нет активных приглашений."));
                        return true;
                    }
                    services.guilds().joinGuild(player, guildId);
                    TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_0a21cc", "&aВы успешно вступили в гильдию!"));
                    return true;
                }

                case "deny" -> {
                    // БАГ-FIX: раньше вызывался getPendingInvite (убирал инвайт), но ничего
                    // с результатом не делалось. Теперь явно убираем и уведомляем отправителя.
                    UUID guildId = services.guilds().getPendingInvite(player.getUniqueId());
                    if (guildId == null) {
                        TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_874df7", "&cУ вас нет активных приглашений."));
                        return true;
                    }
                    // Уведомляем онлайн-членов гильдии об отказе
                    Guild guild = services.guilds().getGuilds().get(guildId);
                    if (guild != null) {
                        for (UUID memberUuid : guild.getMembers().keySet()) {
                            Player member = Bukkit.getPlayer(memberUuid);
                            if (member != null) {
                                TextUtil.send(member, "&7Игрок &f" + player.getName() + " &7отклонил приглашение в гильдию.");
                            }
                        }
                    }
                    TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_65945e", "&7Вы отклонили приглашение."));
                    return true;
                }

                case "create" -> {
                    if (args.length < 2) {
                        TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_983bd3", "&cИспользование: /guild create <название>"));
                        return true;
                    }
                    if (services.guilds().getPlayerGuild(player.getUniqueId()) != null) {
                        TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_b5d8cf", "&cВы уже состоите в гильдии!"));
                        return true;
                    }
                    String name = args[1];
                    if (name.length() > 20) {
                        TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_8910dd", "&cНазвание гильдии не должно превышать 20 символов."));
                        return true;
                    }
                    services.guilds().createGuild(player, name);
                    TextUtil.send(player, "&aВы успешно создали гильдию &f" + name + "&a!");

                    ItemStack heart = ItemRegistry.guildHeart();
                    HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(heart);
                    if (!leftover.isEmpty()) {
                        player.getWorld().dropItem(player.getLocation(), leftover.get(0));
                        TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_aafc4f", "&eВаш инвентарь полон — Сердце Гильдии упало на землю!"));
                    } else {
                        TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_23910e", "&aСердце Гильдии выдано! Поставьте его, чтобы создать базу."));
                    }
                    return true;
                }
            }
        }

        // ── Команды, требующие членства в гильдии ───────────────────────────────

        Guild guild = services.guilds().getPlayerGuild(player.getUniqueId());

        if (args.length == 0 || guild == null) {
            if (guild == null) {
                TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_db6087", "&eУ вас пока нет гильдии."));
                TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_b672db", "&7Используйте: &f/guild create <название> &7или дождитесь приглашения."));
            } else {
                services.gui().openGuildMain(player, guild);
            }
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "invite" -> {
                if (args.length < 2) {
                    TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_8c62ff", "&cИспользование: /guild invite <ник>"));
                    return true;
                }
                // БАГ-FIX: было &&, нужно || — раньше требовало обоих нод одновременно
                if (!guild.hasPermission(player.getUniqueId(), "guild.invite")) {
                    TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_a50a9c", "&cУ вас нет прав приглашать игроков!"));
                    return true;
                }
                if (guild.getMembers().size() >= 30) {
                    TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_guild_full", "&cВ гильдии достигнут лимит участников (30)."));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_a950a7", "&cИгрок не в сети."));
                    return true;
                }
                if (target.equals(player)) {
                    TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_fb1000", "&cНельзя пригласить самого себя."));
                    return true;
                }
                if (services.guilds().getPlayerGuild(target.getUniqueId()) != null) {
                    TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_164f4b", "&cЭтот игрок уже состоит в гильдии."));
                    return true;
                }
                services.guilds().sendInvite(player, target, guild);
                sendFancyInvite(player, target, guild);
            }

            case "leave" -> {
                // БАГ-FIX: команда /guild leave существовала только в тексте подсказки,
                // но не была реализована. Теперь добавлена.
                if (guild.getLeader().equals(player.getUniqueId())) {
                    TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_ee94e0", "&cВы лидер гильдии — вы не можете её покинуть."));
                    TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_38fb14", "&7Передайте лидерство (/guild transfer <ник>) или распустите гильдию (/guild disband)."));
                    return true;
                }
                services.guilds().leaveGuild(player.getUniqueId());
                TextUtil.send(player, "&7Вы покинули гильдию &f" + guild.getName() + "&7.");
            }

            case "sethome", "setgate" -> {
                if (!guild.hasPermission(player.getUniqueId(), "guild.home.set")) {
                    TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_d8f444", "&cУ вас нет прав устанавливать точку дома!"));
                    return true;
                }
                services.guilds().setHome(player, guild);
                TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_56f881", "&aТочка дома гильдии успешно установлена."));
            }

            case "home", "spawn" -> {
                if (!guild.hasPermission(player.getUniqueId(), "guild.home")) {
                    TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_2821d9", "&cУ вас нет прав на телепортацию в дом гильдии!"));
                    return true;
                }
                if (guild.getHomeLocation() == null) {
                    TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_22eb58", "&cТочка дома вашей гильдии ещё не установлена."));
                    return true;
                }
                services.guilds().teleportHome(player, guild);
            }

            case "disband", "delete" -> {
                if (!guild.getLeader().equals(player.getUniqueId())) {
                    TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_e6749c", "&cТолько лидер может распустить гильдию!"));
                    return true;
                }
                services.guilds().disbandGuild(player, guild);
                String prefix = TextUtil.color(com.astrasmp.AstraSMPPlugin.getInstance().getConfig().getString("messages.prefix", "&8[&dAstraSMP&8] &7"));
                Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(
                    prefix + TextUtil.color("&fГильдия &c" + guild.getName() + " &fбыла распущена.")));
            }

            case "transfer" -> {
                if (!guild.getLeader().equals(player.getUniqueId())) {
                    TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_644fc5", "&cТолько лидер может передавать лидерство!"));
                    return true;
                }
                if (args.length < 2) {
                    TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_1c2240", "&cИспользование: /guild transfer <ник>"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_a950a7", "&cИгрок не в сети."));
                    return true;
                }
                if (!guild.getMembers().containsKey(target.getUniqueId())) {
                    TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_87102a", "&cЭтот игрок не состоит в вашей гильдии."));
                    return true;
                }
                services.guilds().transferLeadership(guild, player, target);
                TextUtil.send(player, "&aВы передали лидерство игроку &f" + target.getName() + "&a.");
                TextUtil.send(target, "&6Вы стали новым лидером гильдии &f" + guild.getName() + "&6!");
            }

            default -> services.gui().openGuildMain(player, guild);
        }

        return true;
    }

    private void sendFancyInvite(Player sender, Player target, Guild guild) {
        String prefix = TextUtil.color(com.astrasmp.AstraSMPPlugin.getInstance().getConfig().getString("messages.prefix", "&8[&dAstraSMP&8] &7"));
        Component inviteMsg = Component.text(TextUtil.color(
            "\n" + prefix + "&fИгрок &b" + sender.getName() +
            " &fприглашает вас в гильдию &b" + guild.getName() + "&f!\n" +
            "У вас есть 60 секунд, чтобы "));
        
        Component actions = Component.text("          ")
            .append(Component.text(TextUtil.color("&a&l[ПРИНЯТЬ]"))
                .clickEvent(ClickEvent.runCommand("/guild accept"))
                .hoverEvent(HoverEvent.showText(Component.text("Нажмите, чтобы вступить"))))
            .append(Component.text("   "))
            .append(Component.text(TextUtil.color("&c&l[ОТКЛОНИТЬ]"))
                .clickEvent(ClickEvent.runCommand("/guild deny"))
                .hoverEvent(HoverEvent.showText(Component.text("Нажмите, чтобы отказать"))));

        target.sendMessage(inviteMsg);
        target.sendMessage(actions);
        target.sendMessage(Component.text(""));
        TextUtil.send(sender, "&aПриглашение отправлено игроку &f" + target.getName());
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return List.of();
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            Guild guild = services.guilds().getPlayerGuild(player.getUniqueId());
            List<String> subs = guild == null
                ? List.of("create", "accept", "deny")
                : List.of("invite", "leave", "sethome", "home", "transfer", "disband");

            subs.stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .forEach(completions::add);

        } else if (args.length == 2 && (args[0].equalsIgnoreCase("invite") || args[0].equalsIgnoreCase("transfer"))) {
            Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                .forEach(completions::add);
        }

        return completions;
    }

}
