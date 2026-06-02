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
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.awt.Color;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public final class DiscordBridge {
    private final AstraSMPPlugin plugin;
    private final EventService events;

    private JDA jda;
    private static final String LEADER_ROLE_ID = "1508625935409086644";

    public DiscordBridge(AstraSMPPlugin plugin, EconomyService economy, MMRService mmr, ContractService contracts,
            
            EventService events, LeaderboardService leaderboard) {
        this.plugin = plugin;
        this.events = events;
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("discord.enabled", false)
                && !plugin.getConfig().getString("discord.token", "").isBlank();
    }

    public void connect()
            {
        if (!isEnabled())
            return;
        String token = plugin.getConfig().getString("discord.token", "");
        EnumSet<GatewayIntent> intents = EnumSet.of(
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS,
                GatewayIntent.DIRECT_MESSAGES);
        jda = JDABuilder.createDefault(token, Objects.requireNonNull(intents))
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
        sendToChannel("discord.chat-channel-id", "💬 `" + player + "` **»** " + message);
    }

    public void sendJoinQuitMessage(String player, boolean join) {
        String icon = join ? "🟢" : "🔴";
        String action = join ? "присоединился к игре" : "покинул сервер";
        sendToChannel("discord.chat-channel-id", "**[ChetCraft]** " + icon + " **" + player + "** " + action + ".");
    }

    public void sendDeathMessage(String player, String reason) {
        sendToChannel("discord.chat-channel-id", "☠️ **" + player + "** " + reason);
    }

    public void sendEventEmbed(String eventName) {
        if (jda == null)
            return;
            
        String channelId = Objects.requireNonNullElse(plugin.getConfig().getString("discord.chat-channel-id", ""), "");
        if (channelId.isBlank())
            return;
            

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null)
            return;

        String roleId = plugin.getConfig().getString("discord.event-role-id", "");
        String timeMSK = ZonedDateTime.now(ZoneId.of("Europe/Moscow")).format(DateTimeFormatter.ofPattern("HH:mm"));

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("✨ Глобальное Событие: " + eventName + " ✨");
        embed.setDescription("🔥 **Внимание всем игрокам!** 🔥\nНа сервере только что стартовало событие: **"
                + eventName + "**!\n\n⚔️ Не упустите шанс принять участие и сразиться за уникальные награды.");
        embed.setColor(new Color(0xFF4500)); // OrangeRed
        embed.addField("🕒 Время (МСК)", "`" + timeMSK + "`", true);
        embed.addField("🎮 Как зайти?", "`IP: chetcraft.org`", true);
        embed.setFooter("ChetCraft Network • Участвуй и побеждай!");

        String mention = roleId.isBlank() ? "" : "<@&" + roleId + "> ";
        mention += "<@&1493417811765235732>";
        channel.sendMessage(mention).setEmbeds(embed.build()).queue();
    }

    public void sendLog(String message) {
        sendToChannel("discord.log-channel-id", message);
    }
            

    private void sendToChannel(String key, String message) {
        if (jda == null)
            return;
        String channelId = Objects.requireNonNullElse(plugin.getConfig().getString(key, ""), "");
        if (channelId.isBlank())
            return;
        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel != null && message != null)
            channel.sendMessage(message).queue();
    }

    public void createGuildThread(Guild guild) {
        if (jda == null)
            return;

        ForumChannel forum = jda.getForumChannelById("1508625325985108170");
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
            
            String guildId = plugin.getConfig().getString("discord.guild-id", "");
            if (guildId == null || guildId.isBlank()) return;
            net.dv8tion.jda.api.entities.Guild discordGuild = jda.getGuildById(guildId);
            if (discordGuild == null) return;
            Role leaderRole = discordGuild.getRoleById(LEADER_ROLE_ID);

            if (leaderRole == null)
                return;

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

        String adminRoleId = "1493411595089346612";
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
        String roleId = plugin.getConfig().getString("discord.admin-role-id", "");
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
                            .addOption(OptionType.STRING, "name", "Ник игрока", true))
                    .queue(
                            cmds -> plugin.getLogger()
                                    .info("[Discord] Слэш-команды зарегистрированы (" + cmds.size() + " шт.)"),
                            error -> plugin.getLogger()
                                    .severe("[Discord] Ошибка регистрации слэш-команд: " + error.getMessage()));
        }

        private EmbedBuilder baseEmbed() {
            return new EmbedBuilder().setFooter("ChetCraft Network", null).setTimestamp(java.time.Instant.now());
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

                                    
                    match.setVerified(true);

                    String linkedRoleId = Objects
                            .requireNonNullElse(plugin.getConfig().getString("discord.linked-role-id", ""), "");
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
                            TextUtil.send(player,
                                    "&a&lChetCraft &8» &fАккаунт привязан! Роль в Discord и награда выданы.");
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
                                
                    OfflinePlayer target = Bukkit.getOfflinePlayer(name);
                    PlayerProfile profile = plugin.getServices().store().profile(target.getUniqueId().toString(),
                            target.getName());

                    EmbedBuilder embed = baseEmbed();
                    embed.setTitle("📊 Статистика: " + name);
                    embed.setColor(new Color(0x00FFFF));
                    embed.setThumbnail("https://mc-heads.net/avatar/" + name + "/100.png");
                    embed.addField("💰 Баланс", "`" + profile.getCoins() + " ❂`", true);
                    embed.addField("🏆 MMR", "`" + profile.getMmr() + "`", true);
                    embed.addField("✨ Event Points", "`" + profile.getEventPoints() + "`", true);

                    Guild g = plugin.getServices().guilds() != null
                            ? plugin.getServices().guilds().getPlayerGuild(target.getUniqueId())
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
            }
        }

        @Override
        public void onMessageReceived(@javax.annotation.Nonnull MessageReceivedEvent event) {
            if (event.getAuthor().isBot())
                return;

            String chatChannelId = plugin.getConfig().getString("discord.chat-channel-id", "");

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
    }
}
