package com.astrasmp.service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import com.astrasmp.AstraSMPPlugin;
import com.astrasmp.model.Guild;
import com.astrasmp.util.TextUtil;

public class GuildService {

    private final AstraSMPPlugin plugin;
    private final Map<UUID, Guild> guilds        = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> playerGuildMap = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> pendingInvites = new ConcurrentHashMap<>();

    public GuildService(AstraSMPPlugin plugin) {
        this.plugin = plugin;
    }

    // ── Загрузка / Сохранение ─────────────────────────────────────────────────

    public void loadAll() {
        guilds.clear();
        playerGuildMap.clear();
        List<Guild> loaded = plugin.getDatabase().loadAllGuilds();
        for (Guild g : loaded) {
            guilds.put(g.getId(), g);
            g.getMembers().keySet().forEach(uuid -> playerGuildMap.put(uuid, g.getId()));
        }
        plugin.getLogger().info("Загружено гильдий из БД: " + guilds.size());
    }

    public void saveAll() {
        guilds.values().forEach(g -> plugin.getDatabase().saveGuild(g));
    }

    public void saveGuildAsync(Guild guild) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                plugin.getDatabase().saveGuild(guild);
            } catch (Exception e) {
                plugin.getLogger().severe("[GuildService] Ошибка сохранения '"
                    + guild.getName() + "' (ID: " + guild.getId() + "): " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    // ── Создание / Роспуск ────────────────────────────────────────────────────

    public void createGuild(Player leader, String name) {
        UUID id = UUID.randomUUID();
        Guild guild = new Guild(id, name, leader.getUniqueId());
        guilds.put(id, guild);
        playerGuildMap.put(leader.getUniqueId(), id);
        saveGuildAsync(guild);

        if (plugin.getDiscord().isEnabled()) {
            plugin.getDiscord().createGuildThread(guild);
        }
    }

    public void disbandGuild(Player leader, Guild guild) {
        // Уничтожаем Сердце в мире
        if (guild.getCoreLocation() != null) {
            try {
                String[] p = guild.getCoreLocation().split(",");
                Location loc = new Location(Bukkit.getWorld(p[0]),
                    Double.parseDouble(p[1]), Double.parseDouble(p[2]), Double.parseDouble(p[3]));
                loc.getBlock().setType(org.bukkit.Material.AIR);
            } catch (Exception e) {
                plugin.getLogger().warning("Не удалось удалить Сердце Гильдии: " + guild.getName());
            }
        }

        if (plugin.getDiscord().isEnabled()) plugin.getDiscord().archiveGuildThread(guild);

        // Уведомляем всех онлайн-участников
        String msg = TextUtil.color("&c&lГильдия &f" + guild.getName() + " &c&lбыла распущена лидером.");
        for (UUID memberUuid : guild.getMembers().keySet()) {
            Player member = Bukkit.getPlayer(memberUuid);
            if (member != null && !member.equals(leader)) TextUtil.send(member, msg);
            playerGuildMap.remove(memberUuid);
        }
        guilds.remove(guild.getId());
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
            plugin.getDatabase().deleteGuild(guild.getId()));

        leader.playSound(leader.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 0.8f);
    }

    // ── Вступление / Выход ────────────────────────────────────────────────────

    public void joinGuild(Player player, UUID guildId) {
        // Убираем из старой гильдии, если есть (защита от дублей)
        Guild existing = getPlayerGuild(player.getUniqueId());
        if (existing != null && !existing.getId().equals(guildId)) {
            existing.getMembers().remove(player.getUniqueId());
            playerGuildMap.remove(player.getUniqueId());
            saveGuildAsync(existing);
            plugin.getLogger().warning("[GuildService] Автоочистка: " + player.getName()
                + " был в " + existing.getName() + " при вступлении в другую.");
        }

        Guild guild = guilds.get(guildId);
        if (guild == null) {
            plugin.getLogger().severe("[GuildService] joinGuild: гильдия " + guildId + " не найдена!");
            return;
        }

        guild.getMembers().put(player.getUniqueId(), "recruit");
        playerGuildMap.put(player.getUniqueId(), guildId);
        saveGuildAsync(guild);

        if (plugin.getDiscord().isEnabled()) plugin.getDiscord().updateGuildRoster(guild);

        // Уведомляем участников
        String announcement = TextUtil.color("&b&lChetCraft &8» &fИгрок &b"
            + player.getName() + " &fвступил в гильдию!");
        guild.getMembers().keySet().stream()
            .map(Bukkit::getPlayer)
            .filter(Objects::nonNull)
            .forEach(m -> TextUtil.send(m, announcement));
    }

    public void leaveGuild(UUID playerUuid) {
        Guild guild = getPlayerGuild(playerUuid);
        if (guild == null) return;

        // БАГ-FIX: leaveGuild лидера раньше молча удалял гильдию без уведомления участников.
        // Теперь лидер не может покинуть — он должен распустить или передать лидерство.
        // Этот метод вызывается только для не-лидеров (проверено в GuildCommand.leave).
        guild.getMembers().remove(playerUuid);
        playerGuildMap.remove(playerUuid);
        saveGuildAsync(guild);

        if (plugin.getDiscord().isEnabled()) plugin.getDiscord().updateGuildRoster(guild);

        // Уведомляем остальных участников
        Player leaving = Bukkit.getPlayer(playerUuid);
        String name = leaving != null ? leaving.getName()
            : Objects.requireNonNullElse(Bukkit.getOfflinePlayer(playerUuid).getName(), "Неизвестный");

        String msg = TextUtil.color("&7Игрок &f" + name + " &7покинул гильдию.");
        guild.getMembers().keySet().stream()
            .map(Bukkit::getPlayer)
            .filter(Objects::nonNull)
            .forEach(m -> TextUtil.send(m, msg));
    }

    // ── Управление составом ───────────────────────────────────────────────────

    /**
     * Повышает игрока, используя приоритет рангов вместо хардкода officer/member/recruit.
     * БАГ-FIX: раньше promote/demote игнорировали кастомные ранги.
     */
    public void promote(Guild guild, UUID target) {
        String currentRankId = guild.getMembers().get(target);
        if (currentRankId == null) return;

        Guild.Rank currentRank = guild.getRanks().get(currentRankId);
        if (currentRank == null) return;

        // Ищем ближайший ранг с бо́льшим приоритетом
        Guild.Rank next = guild.getRanks().values().stream()
            .filter(r -> !r.getId().equals("leader"))        // лидерский ранг недостижим через promote
            .filter(r -> r.getPriority() > currentRank.getPriority())
            .min(Comparator.comparingInt(Guild.Rank::getPriority)) // ближайший выше
            .orElse(null);

        if (next == null) {
            Player p = Bukkit.getPlayer(target);
            if (p != null) TextUtil.send(p, "&7Ваш ранг уже максимальный.");
            return;
        }

        guild.getMembers().put(target, next.getId());
        saveGuildAsync(guild);
        if (plugin.getDiscord().isEnabled()) plugin.getDiscord().updateGuildRoster(guild);

        Player p = Bukkit.getPlayer(target);
        if (p != null) TextUtil.send(p, "&aВы повышены до ранга &f" + next.getName() + "&a!");
    }

    public void demote(Guild guild, UUID target) {
        String currentRankId = guild.getMembers().get(target);
        if (currentRankId == null) return;

        Guild.Rank currentRank = guild.getRanks().get(currentRankId);
        if (currentRank == null) return;

        // Ближайший ранг с меньшим приоритетом
        Guild.Rank prev = guild.getRanks().values().stream()
            .filter(r -> r.getPriority() < currentRank.getPriority())
            .max(Comparator.comparingInt(Guild.Rank::getPriority))
            .orElse(null);

        if (prev == null) {
            Player p = Bukkit.getPlayer(target);
            if (p != null) TextUtil.send(p, "&7Ваш ранг уже минимальный.");
            return;
        }

        guild.getMembers().put(target, prev.getId());
        saveGuildAsync(guild);
        if (plugin.getDiscord().isEnabled()) plugin.getDiscord().updateGuildRoster(guild);
    }

    public void kick(Guild guild, UUID target) {
        guild.getMembers().remove(target);
        playerGuildMap.remove(target);
        saveGuildAsync(guild);
        if (plugin.getDiscord().isEnabled()) plugin.getDiscord().updateGuildRoster(guild);

        Player p = Bukkit.getPlayer(target);
        if (p != null) TextUtil.send(p, "&cВы были исключены из гильдии &f" + guild.getName() + "&c.");
    }

    public void setPlayerRank(Guild guild, UUID target, String newRankId) {
        if (!guild.getRanks().containsKey(newRankId)) return;
        guild.getMembers().put(target, newRankId);
        saveGuildAsync(guild);
        if (plugin.getDiscord().isEnabled()) plugin.getDiscord().updateGuildRoster(guild);
    }

    /** Передача лидерства другому игроку. */
    public void transferLeadership(Guild guild, Player oldLeader, Player newLeader) {
        guild.setLeader(newLeader.getUniqueId());
        guild.getMembers().put(oldLeader.getUniqueId(), "officer");
        guild.getMembers().put(newLeader.getUniqueId(), "leader");
        saveGuildAsync(guild);
        if (plugin.getDiscord().isEnabled()) plugin.getDiscord().updateGuildRoster(guild);
    }

    // ── Дом / Телепорт ────────────────────────────────────────────────────────

    public void setHome(Player player, Guild guild) {
        Location loc = player.getLocation();
        String locString = String.format(Locale.US, "%s,%.2f,%.2f,%.2f,%.2f,%.2f",
            loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        guild.setHomeLocation(locString);
        saveGuildAsync(guild);
    }

    public void teleportHome(Player player, Guild guild) {
        String locString = guild.getHomeLocation();
        if (locString == null) return;
        try {
            String[] parts = locString.split(",");
            Location loc = new Location(Bukkit.getWorld(parts[0]),
                Double.parseDouble(parts[1]), Double.parseDouble(parts[2]),
                Double.parseDouble(parts[3]), Float.parseFloat(parts[4]), Float.parseFloat(parts[5]));
            TextUtil.send(player, "&aТелепортация в дом гильдии...");
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
            player.teleport(loc);
        } catch (Exception e) {
            TextUtil.send(player, "&cОшибка телепортации! Возможно, мир был удалён.");
        }
    }

    // ── Приглашения ───────────────────────────────────────────────────────────

    public void sendInvite(Player sender, Player target, Guild guild) {
        pendingInvites.put(target.getUniqueId(), guild.getId());
        // Инвайт истекает через 60 секунд
        Bukkit.getScheduler().runTaskLater(plugin,
            () -> pendingInvites.remove(target.getUniqueId()), 1200L);
    }

    public UUID getPendingInvite(UUID uuid) {
        return pendingInvites.remove(uuid);
    }

    // ── Экономика ─────────────────────────────────────────────────────────────

    public long applyTax(Player player, long income) {
        Guild guild = getPlayerGuild(player.getUniqueId());
        if (guild == null) return 0;
        double taxPercent = plugin.getConfig().getDouble("server.tax-rate", 0.05);
        long tax = (long) (income * taxPercent);
        guild.setBalance(guild.getBalance() + tax);
        saveGuildAsync(guild);
        return tax;
    }

    // ── Утилиты ───────────────────────────────────────────────────────────────

    public boolean hasPermission(Player player, String node) {
        Guild guild = getPlayerGuild(player.getUniqueId());
        return guild != null && guild.hasPermission(player.getUniqueId(), node);
    }

    public Guild getPlayerGuild(UUID playerUuid) {
        UUID guildId = playerGuildMap.get(playerUuid);
        return guildId != null ? guilds.get(guildId) : null;
    }

    public Map<UUID, Guild> getGuilds() {
        return guilds;
    }
}