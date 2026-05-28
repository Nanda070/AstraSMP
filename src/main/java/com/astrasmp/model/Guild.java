package com.astrasmp.model;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Guild {

    // ── XP до следующего уровня ─────────────────────────────────────────────
    private static final long[] LEVEL_XP = {
        0,          // уровень 1 → старт
        5_000,      // уровень 2
        15_000,     // уровень 3
        35_000,     // уровень 4
        75_000,     // уровень 5
        150_000,    // уровень 6
        300_000,    // уровень 7
        600_000,    // уровень 8
        1_200_000,  // уровень 9
        Long.MAX_VALUE // уровень 10 — максимальный
    };

    public static final int MAX_LEVEL = LEVEL_XP.length;

    private final UUID id;
    private String name;
    private String coreLocation;
    private int coreRadius;
    private UUID leader;
    private long balance;
    private int level;
    private long xp;
    private String homeLocation;
    private String forumThreadId;

    // UUID игрока → ID ранга
    private final Map<UUID, String> members = new ConcurrentHashMap<>();
    // ID ранга → объект ранга
    private final Map<String, Rank> ranks = new ConcurrentHashMap<>();
    private final Set<String> unlockedPerks = new HashSet<>();

    public Guild(UUID id, String name, UUID leader) {
        this.id = id;
        this.name = name;
        this.leader = leader;
        this.balance = 0;
        this.level = 1;
        this.xp = 0;
        this.coreRadius = 15;

        setupDefaultRanks();
        this.members.put(leader, "leader");
    }

    private void setupDefaultRanks() {
        ranks.put("leader",  new Rank("leader",  "§4Лидер",      100, new HashSet<>(List.of("*"))));
        ranks.put("officer", new Rank("officer", "§cОфицер",      80,  new HashSet<>(List.of("guild.invite", "guild.kick", "guild.bank", "guild.home.set"))));
        ranks.put("member",  new Rank("member",  "§fУчастник",    50,  new HashSet<>(List.of("guild.home"))));
        ranks.put("recruit", new Rank("recruit", "§7Новобранец",  10,  new HashSet<>()));
    }

    // ── Права ────────────────────────────────────────────────────────────────

    public boolean hasPermission(UUID player, String node) {
        if (player.equals(leader)) return true;
        String rankId = members.get(player);
        if (rankId == null) return false;
        Rank rank = ranks.get(rankId);
        if (rank == null) return false;
        return rank.getPermissions().contains("*") || rank.getPermissions().contains(node);
    }

    public Rank getMemberRank(UUID player) {
        if (player.equals(leader)) return ranks.get("leader");
        String rankId = members.get(player);
        return rankId != null ? ranks.get(rankId) : null;
    }

    // ── XP и уровни ──────────────────────────────────────────────────────────

    /**
     * Добавляет XP гильдии. Возвращает true, если произошёл level-up.
     */
    public boolean addXp(long amount) {
        if (level >= MAX_LEVEL) return false;
        xp += amount;
        if (xp >= LEVEL_XP[level]) {
            xp -= LEVEL_XP[level];
            level++;
            // Автоматически расширяем радиус базы при повышении уровня
            coreRadius = 15 + (level - 1) * 5; // 15→20→25→...
            return true;
        }
        return false;
    }

    /** XP, необходимый для перехода на следующий уровень. */
    public long getXpForNextLevel() {
        if (level >= MAX_LEVEL) return 0;
        return LEVEL_XP[level];
    }

    /** Прогресс текущего уровня от 0.0 до 1.0. */
    public double getLevelProgress() {
        if (level >= MAX_LEVEL) return 1.0;
        long needed = LEVEL_XP[level];
        return needed == 0 ? 1.0 : Math.min(1.0, (double) xp / needed);
    }

    /** Текстовая прогресс-бара для GUI (20 символов). */
    public String buildXpBar() {
        if (level >= MAX_LEVEL) return "&a████████████████████ &fMAX";
        int total = 20;
        int filled = (int) (getLevelProgress() * total);
        String green = "&a" + "█".repeat(filled);
        String gray  = "&8" + "█".repeat(total - filled);
        return green + gray + " &7" + xp + "/" + getXpForNextLevel();
    }

    // ── Стандартные геттеры/сеттеры ──────────────────────────────────────────

    public UUID getId()                   { return id; }
    public String getName()               { return name; }
    public void setName(String name)      { this.name = name; }
    public UUID getLeader()               { return leader; }
    public void setLeader(UUID leader)    { this.leader = leader; }
    public long getBalance()              { return balance; }
    public void setBalance(long balance)  { this.balance = balance; }
    public int getLevel()                 { return level; }
    public void setLevel(int level)       { this.level = Math.max(1, Math.min(MAX_LEVEL, level)); }
    public long getXp()                   { return xp; }
    public void setXp(long xp)           { this.xp = Math.max(0, xp); }
    public String getHomeLocation()       { return homeLocation; }
    public void setHomeLocation(String h) { this.homeLocation = h; }
    public String getCoreLocation()       { return coreLocation; }
    public void setCoreLocation(String c) { this.coreLocation = c; }
    public int getCoreRadius()            { return coreRadius; }
    public void setCoreRadius(int r)      { this.coreRadius = r; }
    public String getForumThreadId()      { return forumThreadId; }
    public void setForumThreadId(String t){ this.forumThreadId = t; }

    public Map<UUID, String> getMembers()     { return members; }
    public Map<String, Rank> getRanks()       { return ranks; }
    public Set<String> getUnlockedPerks()     { return unlockedPerks; }

    // ── Вложенный класс Rank ──────────────────────────────────────────────────

    public static class Rank {
        private String id;
        private String name;
        private int priority;
        private Set<String> permissions;

        public Rank(String id, String name, int priority, Set<String> permissions) {
            this.id          = id;
            this.name        = name;
            this.priority    = priority;
            this.permissions = permissions;
        }

        public String getId()                        { return id; }
        public void setId(String id)                 { this.id = id; }
        public String getName()                      { return name; }
        public void setName(String name)             { this.name = name; }
        public int getPriority()                     { return priority; }
        public void setPriority(int priority)        { this.priority = priority; }
        public Set<String> getPermissions()          { return permissions; }
        public void setPermissions(Set<String> perms){ this.permissions = perms; }
    }
}