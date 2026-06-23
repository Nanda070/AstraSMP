package com.astrasmp.discord;

import com.astrasmp.AstraSMPPlugin;
import com.astrasmp.model.Guild;
import com.astrasmp.model.LinkRecord;
import com.astrasmp.model.PlayerProfile;
import com.astrasmp.service.*;
import com.astrasmp.util.TextUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.components.label.Label;

import java.awt.Color;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.Font;
import java.awt.RenderingHints;
import java.awt.BasicStroke;
import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.net.URL;

public final class DiscordBridge {
    private final AstraSMPPlugin plugin;
    private final EventService events;

    private JDA jda;

    public DiscordBridge(AstraSMPPlugin plugin, EconomyService economy, MMRService mmr, ContractService contracts,
            
            EventService events, LeaderboardService leaderboard) {
        this.plugin = plugin;
        this.events = events;
    }

    public boolean isEnabled() {
        return plugin.getDiscordConfig().getBoolean("enabled", false)
                && !plugin.getDiscordConfig().getString("token", "").isBlank();
    }

    @SuppressWarnings("null")
    public void connect()
            {
        if (!isEnabled())
            return;
        String token = plugin.getDiscordConfig().getString("token", "");
        EnumSet<GatewayIntent> intents = EnumSet.of(
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS,
                GatewayIntent.DIRECT_MESSAGES);
        String activity = plugin.getDiscordConfig().getString("bot-activity", "AstraSMP ✨");
        jda = JDABuilder.createDefault(token, Objects.requireNonNull(intents))
                .setActivity(net.dv8tion.jda.api.entities.Activity.watching(Objects.requireNonNullElse(activity, "AstraSMP ✨")))
                .addEventListeners(new BridgeListener())
                .build();
        plugin.getLogger().info("Discord bridge initialization started.");
    }

    public void shutdown() {
        if (jda != null) {
            jda.shutdownNow();
            jda = null;
        }
    }

    public void sendChat(String player, String message) {
        sendToChannel("chat-channel-id", "💬 `" + player + "` **»** " + message);
    }

    public void sendJoinQuitMessage(String player, boolean join) {
        String icon = join ? "🟢" : "🔴";
        String action = join ? "присоединился к игре" : "покинул сервер";
        String prefixRaw = plugin.getConfigManager().getMessage("prefix", "ChetCraft » ");
        String prefix = (prefixRaw != null ? prefixRaw.replaceAll("(?i)[&§][0-9a-fk-or]", "") : "ChetCraft » ").trim();
        sendToChannel("chat-channel-id", "**[" + prefix + "]** " + icon + " **" + player + "** " + action + ".");
    }

    public void sendDeathMessage(String player, String reason) {
        sendToChannel("chat-channel-id", "☠️ **" + player + "** " + reason);
    }

    public void sendBountyAnnouncement(String targetName, long reward, boolean isCompleted, String killerName) {
        if (!isEnabled() || jda == null) return;
        String announceChannelId = plugin.getDiscordConfig().getString("announce-channel-id", plugin.getDiscordConfig().getString("chat-channel-id", ""));
        if (announceChannelId == null || announceChannelId.isBlank()) return;
        
        net.dv8tion.jda.api.entities.channel.concrete.TextChannel channel = jda.getTextChannelById(announceChannelId);
        if (channel != null) {
            net.dv8tion.jda.api.EmbedBuilder embed = new net.dv8tion.jda.api.EmbedBuilder();
            if (isCompleted) {
                embed.setColor(new java.awt.Color(0x55FF55))
                     .setTitle("🎯 Заказ Выполнен!")
                     .setDescription("Игрок **" + killerName + "** успешно устранил цель **" + targetName + "** и получил награду **" + reward + " ❂**!");
            } else {
                embed.setColor(new java.awt.Color(0xFF5555))
                     .setTitle("☠ Объявлена Охота!")
                     .setDescription("На игрока **" + targetName + "** назначен заказ!\nУбейте цель, чтобы получить **" + reward + " ❂**!");
            }
            channel.sendMessageEmbeds(embed.build()).queue();
        }
    }

    public void sendEventEmbed(String eventName) {
        if (jda == null)
            return;
            
        String channelId = Objects.requireNonNullElse(plugin.getDiscordConfig().getString("chat-channel-id", ""), "");
        if (channelId.isBlank())
            return;
            

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null)
            return;

        String roleId = plugin.getDiscordConfig().getString("event-role-id", "");
        String pingId = plugin.getDiscordConfig().getString("event-ping-id", "");
        String timeMSK = ZonedDateTime.now(ZoneId.of("Europe/Moscow")).format(DateTimeFormatter.ofPattern("HH:mm"));

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("✨ Глобальное Событие: " + eventName + " ✨");
        embed.setDescription("🔥 **Внимание всем игрокам!** 🔥\nНа сервере только что стартовало событие: **"
                + eventName + "**!\n\n⚔️ Не упустите шанс принять участие и сразиться за уникальные награды.");
        embed.setColor(new Color(0xFF4500)); // OrangeRed
        String ip = plugin.getDiscordConfig().getString("server-ip", "play.example.com");
        String footer = plugin.getDiscordConfig().getString("embed-footer", "AstraSMP");
        embed.addField("🕒 Время (МСК)", "`" + timeMSK + "`", true);
        embed.addField("🎮 Как зайти?", "`IP: " + ip + "`", true);
        embed.setFooter(footer + " • Участвуй и побеждай!");

        String mention = roleId.isBlank() ? "" : "<@&" + roleId + "> ";
        if (!pingId.isBlank() && !pingId.equals(roleId)) mention += "<@&" + pingId + ">";
        String finalMention = mention.trim();
        if (finalMention.isEmpty()) {
            channel.sendMessageEmbeds(embed.build()).queue();
        } else {
            channel.sendMessage(Objects.requireNonNull(finalMention)).setEmbeds(embed.build()).queue();
        }
    }

    public void sendLog(String message) {
        sendToChannel("log-channel-id", message);
    }
            

    private void sendToChannel(String key, String message) {
        if (jda == null)
            return;
        String channelId = Objects.requireNonNullElse(plugin.getDiscordConfig().getString(key, ""), "");
        if (channelId.isBlank())
            return;
        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel != null && message != null)
            channel.sendMessage(message).queue();
    }

    public void createGuildThread(Guild guild) {
        if (jda == null) return;

        String forumChannelId = plugin.getDiscordConfig().getString("forum-channel-id", "");
        if (forumChannelId.isBlank()) {
            plugin.getLogger().warning("discord.forum-channel-id не настроен в config.yml!");
            return;
        }
        ForumChannel forum = jda.getForumChannelById(forumChannelId);
        if (forum == null) {
            plugin.getLogger().warning("Форумный канал гильдий не найден в Discord!");
            return;
        }

        MessageCreateData message = new MessageCreateBuilder()
                .setContent(buildGuildThreadContent(guild))
                .build();

        String postName = guild.getName() != null ? guild.getName() : "unknown";
        forum.createForumPost(Objects.requireNonNull(postName), message).queue(post -> {
            guild.setForumThreadId(post.getThreadChannel().getId());
            plugin.getServices().store().requestSave();
            plugin.getLogger().info("Форумный тред для гильдии " + guild.getName() + " успешно создан.");

                
            LinkRecord link = plugin.getServices().store().link(guild.getLeader().toString());
            if (link == null || !link.isVerified() || link.getDiscordId().isEmpty()) {
                return;
            }
            
            String guildId = plugin.getDiscordConfig().getString("guild-id", "");
            if (guildId == null || guildId.isBlank()) return;
            net.dv8tion.jda.api.entities.Guild discordGuild = jda.getGuildById(guildId);
            if (discordGuild == null) return;
            String leaderRoleId = plugin.getDiscordConfig().getString("leader-role-id", "");
            if (leaderRoleId.isBlank()) return;
            Role leaderRole = discordGuild.getRoleById(leaderRoleId);

            if (leaderRole == null) return;

            String discordId = Objects.requireNonNullElse(link.getDiscordId(), "");
            if (discordId.isEmpty())
                return;
            discordGuild.retrieveMemberById(discordId).queue(
                    member -> discordGuild.addRoleToMember(Objects.requireNonNull(member), leaderRole).queue(),
                    error -> {
                    });
        });
    }
                

    public void updateGuildRoster(Guild guild) {
        String threadId = guild.getForumThreadId();
        if (jda == null || threadId == null || threadId.isEmpty())
            return;

        ThreadChannel thread = jda.getThreadChannelById(threadId);
            
        if (thread == null)
            return;

            
        thread.retrieveStartMessage().queue(msg -> {
            String content = buildGuildThreadContent(guild);
            if (content == null)
                content = "";
            msg.editMessage(content).setEmbeds().queue();
        });
    }

    public void archiveGuildThread(Guild guild) {
        String threadId = guild.getForumThreadId();
        if (jda == null || threadId == null || threadId.isEmpty())
            return;

        ThreadChannel thread = jda.getThreadChannelById(threadId);
        if (thread == null)
            return;

        String archiveMsg = "## ❌ Фракция распущена\n> Данная гильдия была официально распущена в игре. Тред закрыт и перенесен в архив.";

        thread.sendMessage(archiveMsg).queue(msg -> {
            thread.getManager().setArchived(true).setLocked(true).queue(
                    success -> plugin.getLogger().info("Форумный тред гильдии " + guild.getName() + " заархивирован."),
                    error -> plugin.getLogger()
                            .warning("Не удалось заархивировать тред гильдии: " + error.getMessage()));
        });
                
    }

    private String buildGuildThreadContent(Guild guild) {
        StringBuilder sb = new StringBuilder();

        sb.append("🛡️ Регистрация Гильдии: ").append(guild.getName()).append("\n\n");

        String adminRoleId = plugin.getDiscordConfig().getString("admin-role-id", "1493411595089346612");
        String ping = "<@&" + adminRoleId + ">";

        LinkRecord link = plugin.getServices().store().link(guild.getLeader().toString());
        if (link != null && link.isVerified() && !link.getDiscordId().isEmpty()) {
            ping += " <@" + link.getDiscordId() + ">";
        }
        sb.append(ping).append("\n\n");
        sb.append(
                "> Фракция официально внесена в реестр сервера. Данный тред является основной информационной панелью и визитной карточкой.\n\n");
        sb.append("## 📝 Требуется заполнение\n");
        sb.append("Владельцу необходимо отправить в этот тред следующую информацию:\n");
        sb.append("- **Базирование:** Примерные координаты или название региона.\n");
        sb.append("- **Концепция:** Цели, стиль игры (PvP/PvE/Экономика), правила и дипломатия.\n");
        sb.append("- **Рекрутинг:** Открыт ли набор и какие требования к кандидатам.\n\n");
        sb.append("## ⚖️ Регламент и обязанности\n");
        sb.append("- Контролировать поведение состава и нести ответственность за нарушения.\n");
        sb.append("- Своевременно обновлять информацию при изменениях вектора развития.\n");
        sb.append("- *Состав гильдии синхронизируется сервером автоматически.*\n\n");
        sb.append("---\n");
        sb.append("Актуальный состав\n");

        int count = 0;
        for (var entry : guild.getMembers().entrySet()) {
            PlayerProfile p = plugin.getServices().store().profiles().get(entry.getKey().toString());
            String name = (p != null) ? p.getName() : "Unknown";

            Guild.Rank rank
            = guild.getRanks().get(entry.getValue());
            String rankName =
            (rank != null) ? rank.getName() : "Без ранга";
            String rankIndicator = switch (entry.getValue()) {
                case "leader" -> "👑";
                case "officer" -> "⚔️";
                default -> "🔹";
            };

            sb.append(rankIndicator).append(" **").append(name).append("** (").append(rankName).append(")\n");
            count++;
        }

        sb.append("\n*Всего участников: ").append(count).append("*");
        return sb.toString();
    }

    private boolean isAdmin(Member member) {
        if (member == null)
            return false;
        String roleId = plugin.getDiscordConfig().getString("admin-role-id", "");
        return !roleId.isBlank() && member.getRoles().stream().anyMatch(role -> role.getId().equals(roleId));
    }

    private final class BridgeListener extends ListenerAdapter {

        @Override
        public void onReady(@javax.annotation.Nonnull ReadyEvent event) {
            event.getJDA().updateCommands().addCommands(
                    Commands.slash("link", "Привязать Minecraft аккаунт к Discord")
                            .addOption(OptionType.STRING, "code", "Код привязки из /link в игре", true),
                    Commands.slash("reload", "Перезагрузить конфиг плагина (только для администраторов)"),
                    Commands.slash("event", "Запустить ивент на сервере (только для администраторов)")
                            .addOption(OptionType.STRING, "type", "Тип ивента", true),
                    Commands.slash("online", "Посмотреть список игроков на сервере"),
                    Commands.slash("player", "Посмотреть статистику игрока")
                            .addOption(OptionType.STRING, "name", "Ник игрока", true),
                    Commands.slash("setup_link_button", "Создать кнопку привязки (только для администраторов)"),
                    Commands.slash("setup_event_button", "Создать кнопку подписки на ивенты (только для администраторов)"),
                    Commands.slash("profile", "Сгенерировать карточку профиля (свою или чужую)")
                            .addOption(OptionType.USER, "user", "Пользователь", false))
                    .queue(
                            cmds -> plugin.getLogger()
                                    .info("[Discord] Слэш-команды зарегистрированы (" + cmds.size() + " шт.)"),
                            error -> plugin.getLogger()
                                    .severe("[Discord] Ошибка регистрации слэш-команд: " + error.getMessage()));
        }

        private EmbedBuilder baseEmbed() {
            String footer = plugin.getDiscordConfig().getString("embed-footer", "AstraSMP");
            return new EmbedBuilder().setFooter(footer, null).setTimestamp(java.time.Instant.now());
        }

        private byte[] generateProfileImage(PlayerProfile profile, String discordName, String pName) {
            try {
                int width = 600;
                int height = 300;
                BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = image.createGraphics();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                // Background
                g2d.setColor(new Color(30, 33, 36));
                g2d.fillRoundRect(0, 0, width, height, 30, 30);
                
                // Accent border
                g2d.setColor(new Color(88, 101, 242));
                g2d.setStroke(new BasicStroke(4));
                g2d.drawRoundRect(2, 2, width - 4, height - 4, 30, 30);

                // Fetch avatar from mc-heads.net
                try {
                    URL avatarUrl = java.net.URI.create("https://mc-heads.net/avatar/" + pName + "/120.png").toURL();
                    BufferedImage avatar = ImageIO.read(avatarUrl);
                    if (avatar != null) {
                        g2d.drawImage(avatar, 40, 40, null);
                    }
                } catch (Exception e) {
                    g2d.setColor(Color.GRAY);
                    g2d.fillRect(40, 40, 120, 120);
                }

                g2d.setFont(new Font("SansSerif", Font.BOLD, 32));
                g2d.setColor(Color.WHITE);
                g2d.drawString(pName, 180, 70);

                g2d.setFont(new Font("SansSerif", Font.PLAIN, 18));
                g2d.setColor(new Color(153, 170, 181));
                g2d.drawString("Discord: " + discordName, 180, 100);

                // Stats background
                g2d.setColor(new Color(43, 45, 49));
                g2d.fillRoundRect(40, 180, 520, 90, 20, 20);

                g2d.setFont(new Font("SansSerif", Font.BOLD, 20));
                g2d.setColor(new Color(255, 215, 0)); // Gold for coins
                g2d.drawString("Баланс: " + profile.getCoins() + " ❂", 60, 215);

                g2d.setColor(new Color(0, 255, 255)); // Cyan for MMR
                g2d.drawString("MMR: " + profile.getMmr(), 60, 250);

                g2d.setColor(new Color(255, 105, 180)); // Pink for EP
                g2d.drawString("Event Pts: " + profile.getEventPoints(), 300, 215);

                g2d.setColor(new Color(255, 100, 100)); // Red for Kills
                g2d.drawString("Убийства: " + profile.getKills(), 300, 250);

                g2d.dispose();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, "png", baos);
                return baos.toByteArray();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        public void onSlashCommandInteraction(@javax.annotation.Nonnull SlashCommandInteractionEvent event) {
            if (event.getUser().isBot())
                return;

            switch (event.getName()) {
                case "link" -> {
                    var codeOption = event.getOption("code");
                    if (codeOption == null) {
                        event.reply("❌ Укажи код привязки.").setEphemeral(true).queue();
                        return;
                    }

                    String code = codeOption.getAsString();

                    var match = plugin.getServices().store().links().values().stream()
                            .filter(link -> code.equalsIgnoreCase(link.getCode()))
                            .findFirst().orElse(null);

                    if (match == null) {
                        event.reply("❌ Код не найден или уже использован.").setEphemeral(true).queue();
                        return;
                    }

                    // Сохраняем Discord ID пользователя
                    match.setDiscordId(event.getUser().getId());
                    match.setVerified(true);

                    String linkedRoleId = Objects
                            .requireNonNullElse(plugin.getDiscordConfig().getString("linked-role-id", ""), "");
                    net.dv8tion.jda.api.entities.Guild g = event.getGuild();
                    Member m = event.getMember();
                    if (!linkedRoleId.isBlank() && g != null && m != null) {
                        Role role = g.getRoleById(linkedRoleId);
                        if (role != null)
                                
                            g.addRoleToMember(m, role).queue();
                    }

                    UUID playerUuid = UUID.fromString(match.getUuid());
                    Player player = Bukkit.getPlayer(playerUuid);
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUuid);
                    String pName = offlinePlayer.getName() != null ? offlinePlayer.getName() : "игрока";

                    if (player != null) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            plugin.getServices().quests().processAction(player, com.astrasmp.service.QuestManager.QuestAction.LINK_DISCORD, "", 1);
                            String prefix = plugin.getConfigManager().getMessage("prefix", "&b&lChetCraft &8» ");
                            TextUtil.send(player, prefix + "Аккаунт привязан! Роль в Discord и награда выданы.");
                        });
                    }

                    EmbedBuilder embed = baseEmbed().setColor(new Color(0x55FF55))
                            .setDescription("✅ Успешно! Аккаунт **" + pName + "** привязан.");
                    event.replyEmbeds(embed.build()).setEphemeral(true).queue();
                }

                case "online" -> {
                            
                    int count = Bukkit.getOnlinePlayers().size();
                    if (count == 0) {
                        event.replyEmbeds(baseEmbed().setColor(new Color(0xFF5555))
                                .setDescription("На сервере сейчас никого нет :(").build()).queue();
                        return;
                    }
                    StringBuilder sb = new StringBuilder();
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        sb.append("`").append(p.getName()).append("` ");
                    }
                            
                            
                    EmbedBuilder embed = baseEmbed();
                    embed.setTitle("🌐 Игроки онлайн: " + count);
                    embed.setDescription(sb.toString());
                    embed.setColor(new Color(0x55FF55));
                    event.replyEmbeds(embed.build()).queue();
                }

                case "player" -> {
                    var nameOption = event.getOption("name");
                    if (nameOption == null) {
                        event.reply("❌ Укажи ник игрока.").setEphemeral(true).queue();
                        return;
                    }
                    String name = nameOption.getAsString();
                                
                    String uuidStr = plugin.getServices().plugin().getDatabase().getUuidByName(name);
                    if (uuidStr == null) {
                        event.reply("❌ Игрок никогда не заходил на сервер.").setEphemeral(true).queue();
                        return;
                    }
                    java.util.UUID targetUuid = java.util.UUID.fromString(uuidStr);
                    PlayerProfile profile = plugin.getServices().store().profile(uuidStr, name);

                    EmbedBuilder embed = baseEmbed();
                    embed.setTitle("📊 Статистика: " + name);
                    embed.setColor(new Color(0x00FFFF));
                    embed.setThumbnail("https://mc-heads.net/avatar/" + name + "/100.png");
                    embed.addField("💰 Баланс", "`" + profile.getCoins() + " ❂`", true);
                    embed.addField("🏆 MMR", "`" + profile.getMmr() + "`", true);
                    embed.addField("✨ Event Points", "`" + profile.getEventPoints() + "`", true);

                    Guild g = plugin.getServices().guilds() != null
                            ? plugin.getServices().guilds().getPlayerGuild(targetUuid)
                            : null;
                    embed.addField("🛡️ Гильдия", g != null ? "`" + g.getName() + "`" : "`Нет`", false);

                    event.replyEmbeds(embed.build()).queue();
                }

                                    
                case "reload" -> {
                    if (!isAdmin(event.getMember())) {
                        event.reply("❌ Недостаточно прав.").setEphemeral(true).queue();
                        return;
                    }
                    event.deferReply(true).queue();
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        plugin.reloadConfig();
                        EmbedBuilder embed = baseEmbed().setColor(new Color(0x55FF55))
                                .setDescription("✅ Конфиг успешно перезагружен.");
                        event.getHook().sendMessageEmbeds(embed.build()).queue();
                    });
                }

                case "event" -> {
                    if (!isAdmin(event.getMember())) {
                        event.reply("❌ Недостаточно прав.").setEphemeral(true).queue();
                        return;
                    }
                    var typeOption = event.getOption("type");
                    if (typeOption == null) {
                        event.reply("❌ Укажи тип ивента.").setEphemeral(true).queue();
                        return;
                    }
                    String type = typeOption.getAsString().toUpperCase(Locale.ROOT);
                    event.deferReply(false).queue(); 
                    Bukkit.getScheduler().runTask(plugin, () -> {        try {
                            events.start(EventService.EventType.valueOf(type), null);
                            EmbedBuilder embed = baseEmbed().setColor(new Color(0x55FF55))
                                    .setDescription("🚀 Запущен ивент: **" + type + "**");
                            event.getHook().sendMessageEmbeds(embed.build()).queue();
                        } catch (IllegalArgumentException ex) {
                            String types = java.util.Arrays.stream(EventService.EventType.values())
                                    .map(e -> "`" + e.name() + "`").reduce((a, b) -> a + ", " + b).orElse("");
                            EmbedBuilder embed = baseEmbed().setColor(new Color(0xFF5555))
                                    .setDescription("❌ Неизвестный тип: `" + type + "`\n📝 Доступные: " + types);
                            event.getHook().sendMessageEmbeds(embed.build()).queue();
                        }
                    });
                }
                
                case "profile" -> {
                    event.deferReply(false).queue(); // Generating image takes time
                    var userOpt = event.getOption("user");
                    net.dv8tion.jda.api.entities.User targetUser = userOpt != null ? userOpt.getAsUser() : event.getUser();
                    
                    String discordId = targetUser.getId();
                    var match = plugin.getServices().store().links().values().stream()
                            .filter(link -> discordId.equals(link.getDiscordId()))
                            .findFirst().orElse(null);
                    
                    if (match == null) {
                        event.getHook().sendMessage("❌ Аккаунт Discord **" + targetUser.getEffectiveName() + "** не привязан к серверу.").queue();
                        return;
                    }

                    UUID playerUuid = UUID.fromString(match.getUuid());
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUuid);
                    String pName = offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown";
                    
                    PlayerProfile profile = plugin.getServices().store().profile(playerUuid.toString(), pName);
                    
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        byte[] imageBytes = generateProfileImage(profile, targetUser.getEffectiveName(), pName);
                        if (imageBytes != null) {
                            event.getHook().sendFiles(FileUpload.fromData(imageBytes, "profile.png")).queue();
                        } else {
                            event.getHook().sendMessage("❌ Ошибка при генерации профиля.").queue();
                        }
                    });
                }
                
                case "setup_link_button" -> {
                    if (!isAdmin(event.getMember())) {
                        event.reply(java.util.Objects.requireNonNull(plugin.getDiscordConfig().getString("messages.error-perms", "❌ Недостаточно прав."))).setEphemeral(true).queue();
                        return;
                    }
                    String title = plugin.getDiscordConfig().getString("messages.link-embed.title", "🔗 Привязка Аккаунта");
                    String desc = plugin.getDiscordConfig().getString("messages.link-embed.description", "Нажмите на кнопку ниже, чтобы привязать свой Minecraft аккаунт к Discord.");
                    String colorStr = plugin.getDiscordConfig().getString("messages.link-embed.color", "#5865F2");
                    String btnText = plugin.getDiscordConfig().getString("messages.link-embed.button-text", "Привязать аккаунт");

                    EmbedBuilder embed = baseEmbed()
                        .setTitle(title)
                        .setDescription(desc)
                        .setColor(java.awt.Color.decode(colorStr != null ? colorStr : "#5865F2"));
                        
                    MessageCreateData msgData = new MessageCreateBuilder()
                        .setEmbeds(embed.build())
                        .addComponents(ActionRow.of(Button.primary("btn_link_account", btnText != null ? btnText : "Привязать аккаунт")))
                        .build();
                    event.getChannel().sendMessage(msgData).queue();
                        
                    event.reply(java.util.Objects.requireNonNull(plugin.getDiscordConfig().getString("messages.success-link", "✅ Кнопка успешно создана!"))).setEphemeral(true).queue();
                }

                case "setup_event_button" -> {
                    if (!isAdmin(event.getMember())) {
                        event.reply(java.util.Objects.requireNonNull(plugin.getDiscordConfig().getString("messages.error-perms", "❌ Недостаточно прав."))).setEphemeral(true).queue();
                        return;
                    }
                    String title = plugin.getDiscordConfig().getString("messages.event-embed.title", "🎉 Уведомления об Ивентах");
                    String desc = plugin.getDiscordConfig().getString("messages.event-embed.description", "Нажмите на кнопку ниже, чтобы получить роль ивентов!");
                    String colorStr = plugin.getDiscordConfig().getString("messages.event-embed.color", "#FF4500");
                    String btnText = plugin.getDiscordConfig().getString("messages.event-embed.button-text", "Получить роль ивентов");

                    EmbedBuilder embed = baseEmbed()
                        .setTitle(title)
                        .setDescription(desc)
                        .setColor(java.awt.Color.decode(colorStr != null ? colorStr : "#FF4500"));
                        
                    MessageCreateData msgData = new MessageCreateBuilder()
                        .setEmbeds(embed.build())
                        .addComponents(ActionRow.of(Button.success("btn_event_role", btnText != null ? btnText : "Получить роль ивентов")))
                        .build();
                    event.getChannel().sendMessage(msgData).queue();
                        
                    event.reply(java.util.Objects.requireNonNull(plugin.getDiscordConfig().getString("messages.success-event", "✅ Кнопка ивентов успешно создана!"))).setEphemeral(true).queue();
                }
            }
        }

        @Override
        public void onMessageReceived(@javax.annotation.Nonnull MessageReceivedEvent event) {
            if (event.getAuthor().isBot())
                return;

            String chatChannelId = plugin.getDiscordConfig().getString("chat-channel-id", "");

            // Ретрансляция сообщений из Discord в игровой чат
            if (event.getChannel().getId().equals(chatChannelId)) {
                String content = event.getMessage().getContentDisplay();
                String authorName = event.getAuthor().getGlobalName() != null
                        ? event.getAuthor().getGlobalName()
                        : event.getAuthor().getName();
                Bukkit.getScheduler().runTask(plugin, () -> Bukkit
                        .broadcast(Component.text(TextUtil.color("&9[Discord] &f" + authorName + "&7: &f" + content))));
            }
        }

        @Override
        public void onButtonInteraction(@javax.annotation.Nonnull ButtonInteractionEvent event) {
            if (event.getComponentId().equals("btn_link_account")) {
                TextInput codeInput = TextInput.create("link_code", TextInputStyle.SHORT)
                    .setPlaceholder("Например: XyZ123")
                    .setMinLength(1)
                    .setMaxLength(16)
                    .setRequired(true)
                    .build();

                Modal modal = Modal.create("modal_link_account", "Привязка Аккаунта")
                    .addComponents(Label.of("Секретный код из игры", codeInput))
                    .build();

                event.replyModal(modal).queue();
            } else if (event.getComponentId().equals("btn_event_role")) {
                String roleId = plugin.getDiscordConfig().getString("event-button-role-id", "1493417811765235732");
                if (roleId.isBlank()) {
                    event.reply("❌ Роль для ивентов не настроена.").setEphemeral(true).queue();
                    return;
                }
                net.dv8tion.jda.api.entities.Guild g = event.getGuild();
                Member m = event.getMember();
                if (g != null && m != null) {
                    Role role = g.getRoleById(roleId);
                    if (role != null) {
                        if (m.getRoles().contains(role)) {
                            g.removeRoleFromMember(m, role).queue();
                            event.reply("🔔 Вы отписались от уведомлений об ивентах!").setEphemeral(true).queue();
                        } else {
                            g.addRoleToMember(m, role).queue();
                            event.reply("🔔 Вы подписались на уведомления об ивентах!").setEphemeral(true).queue();
                        }
                    } else {
                        event.reply("❌ Роль не найдена.").setEphemeral(true).queue();
                    }
                }
            }
        }

        @Override
        public void onModalInteraction(@javax.annotation.Nonnull ModalInteractionEvent event) {
            if (event.getModalId().equals("modal_link_account")) {
                var codeMapping = event.getValue("link_code");
                if (codeMapping == null) {
                    event.reply("❌ Произошла ошибка: код привязки не получен.").setEphemeral(true).queue();
                    return;
                }
                String code = codeMapping.getAsString();
                
                var match = plugin.getServices().store().links().values().stream()
                        .filter(link -> code.equalsIgnoreCase(link.getCode()))
                        .findFirst().orElse(null);

                if (match == null) {
                    event.reply("❌ Неверный или уже использованный код. Сгенерируйте новый командой `/link` в игре.").setEphemeral(true).queue();
                    return;
                }

                match.setDiscordId(event.getUser().getId());
                match.setVerified(true);
                plugin.getServices().store().requestSave();

                String linkedRoleId = plugin.getDiscordConfig().getString("linked-role-id", "");
                net.dv8tion.jda.api.entities.Guild g = event.getGuild();
                Member m = event.getMember();
                if (linkedRoleId != null && !linkedRoleId.isBlank() && g != null && m != null) {
                    Role role = g.getRoleById(linkedRoleId);
                    if (role != null)
                        g.addRoleToMember(m, role).queue();
                }

                UUID playerUuid = UUID.fromString(match.getUuid());
                Player player = Bukkit.getPlayer(playerUuid);
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUuid);
                String pName = offlinePlayer.getName() != null ? offlinePlayer.getName() : "игрока";

                if (player != null) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        plugin.getServices().quests().processAction(player, com.astrasmp.service.QuestManager.QuestAction.LINK_DISCORD, "", 1);
                        String prefix = plugin.getConfigManager().getMessage("prefix", "&b&lChetCraft &8» ");
                        TextUtil.send(player, prefix + "Аккаунт успешно привязан к Discord через кнопку!");
                    });
                }

                EmbedBuilder embed = baseEmbed().setColor(new Color(0x55FF55))
                        .setDescription("✅ Успешно! Вы привязали Minecraft аккаунт **" + pName + "** к своему Discord профилю.");
                event.replyEmbeds(embed.build()).setEphemeral(true).queue();
            }
        }
    }
}
