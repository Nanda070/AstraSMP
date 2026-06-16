package com.astrasmp.database;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import com.astrasmp.AstraSMPPlugin;
import com.astrasmp.model.Guild;
import com.astrasmp.model.PlayerProfile;
import com.astrasmp.model.AuctionLot;
import com.astrasmp.model.ContractRecord;
import com.astrasmp.model.MENetwork;
import com.astrasmp.model.MENode;
import com.astrasmp.util.ItemSerializer;
import com.astrasmp.util.LocationKey;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

public class DatabaseService {
    private final AstraSMPPlugin plugin;
    private HikariDataSource dataSource;
    private final Gson gson;
    // Диалект SQL: "sqlite" или "mysql"
    private String dialect = "sqlite";

    public DatabaseService(AstraSMPPlugin plugin) {
        this.plugin = plugin;
        this.gson = new Gson();
    }

    public void connect() {
        String mode = plugin.getConfig().getString("storage.mode", "sqlite").toLowerCase();
        HikariConfig config = new HikariConfig();

        if (mode.equals("mysql")) {
            dialect = "mysql";
            String host     = plugin.getConfig().getString("storage.mysql.host", "localhost");
            int    port     = plugin.getConfig().getInt("storage.mysql.port", 3306);
            String database = plugin.getConfig().getString("storage.mysql.database", "astrasmp");
            String user     = plugin.getConfig().getString("storage.mysql.username", "root");
            String pass     = plugin.getConfig().getString("storage.mysql.password", "");
            int    poolSize = plugin.getConfig().getInt("storage.mysql.pool-size", 8);

            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database
                    + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8");
            config.setUsername(user);
            config.setPassword(pass);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            config.setMaximumPoolSize(poolSize);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(10000);
            config.setConnectionTestQuery("SELECT 1");
            plugin.getLogger().info("[DB] Режим хранилища: MySQL (" + host + ":" + port + "/" + database + ")");
        } else {
            dialect = "sqlite";
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) dataFolder.mkdirs();
            File dbFile = new File(dataFolder, "astrasmp.db");
            config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            config.setDriverClassName("org.sqlite.JDBC");
            config.addDataSourceProperty("journal_mode", "WAL");
            config.addDataSourceProperty("synchronous", "NORMAL");
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(10000);
            plugin.getLogger().info("[DB] Режим хранилища: SQLite");
        }

        dataSource = new HikariDataSource(config);

        try {
            createTables();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка создания таблиц базы данных", e);
        }
    }

    /** Возвращает SQL-выражение "INSERT OR REPLACE" для SQLite или "REPLACE INTO" для MySQL */
    private String upsert(String table, String columns, String placeholders) {
        if (dialect.equals("mysql")) {
            return "REPLACE INTO " + table + " (" + columns + ") VALUES (" + placeholders + ")";
        }
        return "INSERT OR REPLACE INTO " + table + " (" + columns + ") VALUES (" + placeholders + ")";
    }

    private void createTables() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement s = conn.createStatement()) {

            // Общий синтаксис совместим и с MySQL, и с SQLite
            s.execute("CREATE TABLE IF NOT EXISTS guilds (" +
                    "id VARCHAR(36) PRIMARY KEY, " +
                    "name VARCHAR(64), " +
                    "leader VARCHAR(36), " +
                    "balance BIGINT, " +
                    "level INT, " +
                    "xp BIGINT, " +
                    "home_location VARCHAR(512), " +
                    "permissions TEXT, " +
                    "forum_thread_id VARCHAR(64))");

            s.execute("CREATE TABLE IF NOT EXISTS guild_members (" +
                    "player_uuid VARCHAR(36) PRIMARY KEY, " +
                    "guild_id VARCHAR(36), " +
                    "rank VARCHAR(32))");

            s.execute("CREATE TABLE IF NOT EXISTS profiles (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "name VARCHAR(64), " +
                    "coins BIGINT, " +
                    "mmr INT, " +
                    "kills INT, " +
                    "deaths INT, " +
                    "sold_value BIGINT, " +
                    "event_points INT, " +
                    "faction VARCHAR(64), " +
                    "base_level INT, " +
                    "quest_step INT, " +
                    "quest_progress INT, " +
                    "prefix VARCHAR(64), " +
                    "prefix_color VARCHAR(16), " +
                    "daily_quests TEXT, " +
                    "daily_quest_date VARCHAR(16), " +
                    "login_streak INT DEFAULT 0, " +
                    "last_login_date VARCHAR(16), " +
                    "daily_reward_day INT DEFAULT 1, " +
                    "talents TEXT)");

            // Добавляем колонки для старых баз (ошибки тихо игнорируются, если колонки уже есть)
            try { s.execute("ALTER TABLE profiles ADD COLUMN daily_quests TEXT"); } catch (SQLException ignored) {}
            try { s.execute("ALTER TABLE profiles ADD COLUMN daily_quest_date VARCHAR(16)"); } catch (SQLException ignored) {}
            try { s.execute("ALTER TABLE profiles ADD COLUMN login_streak INT DEFAULT 0"); } catch (SQLException ignored) {}
            try { s.execute("ALTER TABLE profiles ADD COLUMN last_login_date VARCHAR(16)"); } catch (SQLException ignored) {}
            try { s.execute("ALTER TABLE profiles ADD COLUMN daily_reward_day INT DEFAULT 1"); } catch (SQLException ignored) {}
            try { s.execute("ALTER TABLE profiles ADD COLUMN talents TEXT"); } catch (SQLException ignored) {}
            try { s.execute("ALTER TABLE profiles ADD COLUMN corruption INT DEFAULT 0"); } catch (SQLException ignored) {}
            try { s.execute("ALTER TABLE profiles ADD COLUMN pact_type VARCHAR(32) DEFAULT ''"); } catch (SQLException ignored) {}

            s.execute("CREATE TABLE IF NOT EXISTS blood_tanks (" +
                    "world VARCHAR(64), " +
                    "x INT, " +
                    "y INT, " +
                    "z INT, " +
                    "amount INT, " +
                    "PRIMARY KEY (world, x, y, z))");

            s.execute("CREATE TABLE IF NOT EXISTS rifts (" +
                    "world VARCHAR(64), " +
                    "x INT, " +
                    "y INT, " +
                    "z INT, " +
                    "PRIMARY KEY (world, x, y, z))");

            s.execute("CREATE TABLE IF NOT EXISTS auction_lots (" +
                    "id BIGINT PRIMARY KEY, " +
                    "seller_uuid VARCHAR(36), " +
                    "item TEXT, " +
                    "price BIGINT, " +
                    "created_at BIGINT, " +
                    "expires_at BIGINT, " +
                    "sold BOOLEAN)");

            s.execute("CREATE TABLE IF NOT EXISTS contracts (" +
                    "id BIGINT PRIMARY KEY, " +
                    "creator_uuid VARCHAR(36), " +
                    "target_uuid VARCHAR(36), " +
                    "reward BIGINT, " +
                    "type VARCHAR(32), " +
                    "note TEXT, " +
                    "active BOOLEAN, " +
                    "created_at BIGINT)");

            s.execute("CREATE TABLE IF NOT EXISTS me_networks (" +
                    "id VARCHAR(36) PRIMARY KEY, " +
                    "owner VARCHAR(36), " +
                    "capacity BIGINT)");

            s.execute("CREATE TABLE IF NOT EXISTS me_nodes (" +
                    "network_id VARCHAR(36), " +
                    "world VARCHAR(64), " +
                    "x INT, " +
                    "y INT, " +
                    "z INT, " +
                    "type VARCHAR(32))");

            s.execute("CREATE TABLE IF NOT EXISTS me_items (" +
                    "network_id VARCHAR(36), " +
                    "item_hash TEXT(512), " +
                    "amount BIGINT, " +
                    "PRIMARY KEY (network_id, item_hash(255)))");

            s.execute("CREATE TABLE IF NOT EXISTS me_drives (" +
                    "network_id VARCHAR(36), " +
                    "world VARCHAR(64), " +
                    "x INT, " +
                    "y INT, " +
                    "z INT, " +
                    "inventory_base64 MEDIUMTEXT)");
        }
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    // --- ПРОФИЛИ ИГРОКОВ ---
    public List<PlayerProfile> loadAllProfiles() {
        List<PlayerProfile> loaded = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT * FROM profiles")) {
            while (rs.next()) {
                PlayerProfile p = new PlayerProfile(
                        rs.getString("uuid"), rs.getString("name"), rs.getLong("coins"), rs.getInt("mmr"),
                        rs.getInt("kills"), rs.getInt("deaths"), rs.getLong("sold_value"), rs.getInt("event_points"),
                        rs.getString("faction"), rs.getInt("base_level")
                );
                p.setQuestStep(rs.getInt("quest_step"));
                p.setQuestProgress(rs.getInt("quest_progress"));
                p.setCustomPrefix(rs.getString("prefix"));
                p.setPrefixColor(rs.getString("prefix_color"));
                
                String dailyQuestsJson = rs.getString("daily_quests");
                if (dailyQuestsJson != null && !dailyQuestsJson.isEmpty()) {
                    try {
                        Type mapType = new TypeToken<Map<String, Integer>>(){}.getType();
                        Map<String, Integer> dQuests = gson.fromJson(dailyQuestsJson, mapType);
                        if (dQuests != null) {
                            p.getDailyQuests().putAll(dQuests);
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Ошибка парсинга ежедневных квестов для " + p.getName());
                    }
                }
                String dDate = rs.getString("daily_quest_date");
                if (dDate != null) p.setDailyQuestDate(dDate);

                p.setLoginStreak(rs.getInt("login_streak"));
                String lastLogin = rs.getString("last_login_date");
                if (lastLogin != null) p.setLastLoginDate(lastLogin);
                
                int rewardDay = rs.getInt("daily_reward_day");
                if (rewardDay > 0) p.setDailyRewardDay(rewardDay);

                String talentsJson = rs.getString("talents");
                if (talentsJson != null && !talentsJson.isEmpty()) {
                    try {
                        Type mapType = new TypeToken<Map<String, Integer>>(){}.getType();
                        Map<String, Integer> tMap = gson.fromJson(talentsJson, mapType);
                        if (tMap != null) {
                            p.getTalents().putAll(tMap);
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Ошибка парсинга талантов для " + p.getName());
                    }
                }

                p.setCorruption(rs.getInt("corruption"));
                try {
                    String pactType = rs.getString("pact_type");
                    if (pactType != null && !pactType.isEmpty()) {
                        p.setPactType(pactType);
                    }
                } catch (SQLException ignored) {
                    // Fallback for old DBs with has_pact boolean column
                    try {
                        if (rs.getBoolean("has_pact")) {
                            p.setPactType("BLOOD");
                        }
                    } catch (SQLException ignored2) {}
                }

                loaded.add(p);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка загрузки профилей из БД", e);
        }
        return loaded;
    }

    public void saveProfile(PlayerProfile p) {
        String sql = upsert("profiles",
                "uuid, name, coins, mmr, kills, deaths, sold_value, event_points, faction, base_level, quest_step, quest_progress, prefix, prefix_color, daily_quests, daily_quest_date, login_streak, last_login_date, daily_reward_day, talents, corruption, pact_type",
                "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?");
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getUuid());
            ps.setString(2, p.getName());
            ps.setLong(3, p.getCoins());
            ps.setInt(4, p.getMmr());
            ps.setInt(5, p.getKills());
            ps.setInt(6, p.getDeaths());
            ps.setLong(7, p.getSoldValue());
            ps.setInt(8, p.getEventPoints());
            ps.setString(9, p.getFaction());
            ps.setInt(10, p.getBaseLevel());
            ps.setInt(11, p.getQuestStep());
            ps.setInt(12, p.getQuestProgress());
            ps.setString(13, p.getCustomPrefix());
            ps.setString(14, p.getPrefixColor());
            ps.setString(15, gson.toJson(p.getDailyQuests()));
            ps.setString(16, p.getDailyQuestDate());
            ps.setInt(17, p.getLoginStreak());
            ps.setString(18, p.getLastLoginDate());
            ps.setInt(19, p.getDailyRewardDay());
            ps.setString(20, gson.toJson(p.getTalents()));
            ps.setInt(21, p.getCorruption());
            ps.setString(22, p.getPactType());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка сохранения профиля игрока в БД", e);
        }
    }

    // --- АУКЦИОННЫЕ ЛОТЫ ---
    public List<AuctionLot> loadAllLots() {
        List<AuctionLot> loaded = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT * FROM auction_lots")) {
            while (rs.next()) {
                loaded.add(new AuctionLot(
                        rs.getLong("id"),
                        rs.getString("seller_uuid"),
                        ItemSerializer.fromBase64(rs.getString("item")),
                        rs.getLong("price"),
                        rs.getLong("created_at"),
                        rs.getLong("expires_at"),
                        rs.getBoolean("sold")
                ));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка загрузки лотов аукциона из БД", e);
        }
        return loaded;
    }

    public void saveLot(AuctionLot lot) {
        String sql = upsert("auction_lots", "id, seller_uuid, item, price, created_at, expires_at, sold", "?, ?, ?, ?, ?, ?, ?");
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, lot.getId());
            ps.setString(2, lot.getSellerUuid());
            ps.setString(3, ItemSerializer.toBase64(lot.getItem()));
            ps.setLong(4, lot.getPrice());
            ps.setLong(5, lot.getCreatedAt());
            ps.setLong(6, lot.getExpiresAt());
            ps.setBoolean(7, lot.isSold());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка сохранения лота аукциона в БД", e);
        }
    }

    // --- КОНТРАКТЫ ---
    public List<ContractRecord> loadAllContracts() {
        List<ContractRecord> loaded = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT * FROM contracts")) {
            while (rs.next()) {
                loaded.add(new ContractRecord(
                        rs.getLong("id"),
                        rs.getString("creator_uuid"),
                        rs.getString("target_uuid"),
                        rs.getLong("reward"),
                        rs.getString("type"),
                        rs.getString("note"),
                        rs.getBoolean("active"),
                        rs.getLong("created_at")
                ));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка загрузки контрактов из БД", e);
        }
        return loaded;
    }

    public void saveContract(ContractRecord c) {
        String sql = upsert("contracts", "id, creator_uuid, target_uuid, reward, type, note, active, created_at", "?, ?, ?, ?, ?, ?, ?, ?");
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, c.getId());
            ps.setString(2, c.getCreatorUuid());
            ps.setString(3, c.getTargetUuid());
            ps.setLong(4, c.getReward());
            ps.setString(5, c.getType());
            ps.setString(6, c.getNote());
            ps.setBoolean(7, c.isActive());
            ps.setLong(8, c.getCreatedAt());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка сохранения контракта в БД", e);
        }
    }

    // --- ГИЛЬДИИ ---
    public List<Guild> loadAllGuilds() {
        List<Guild> loaded = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT * FROM guilds")) {

            while (rs.next()) {
                UUID id = UUID.fromString(rs.getString("id"));
                String name = rs.getString("name");
                UUID leader = UUID.fromString(rs.getString("leader"));

                Guild guild = new Guild(id, name, leader);
                guild.setBalance(rs.getLong("balance"));
                guild.setLevel(rs.getInt("level"));
                guild.setXp(rs.getLong("xp"));
                guild.setHomeLocation(rs.getString("home_location"));
                guild.setForumThreadId(rs.getString("forum_thread_id")); 

                // Десериализация кастомных рангов
                String permsJson = rs.getString("permissions");
                if (permsJson != null && permsJson.startsWith("{")) {
                    try {
                        Type rankType = new TypeToken<Map<String, Guild.Rank>>(){}.getType();
                        Map<String, Guild.Rank> ranks = gson.fromJson(permsJson, rankType);
                        if (ranks != null && !ranks.isEmpty()) {
                            guild.getRanks().clear();
                            guild.getRanks().putAll(ranks);
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Ошибка парсинга рангов для гильдии " + name + ". Используются дефолтные.");
                    }
                }

                loadMembers(conn, guild);
                loaded.add(guild);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка загрузки гильдий из БД", e);
        }
        return loaded;
    }

    public void saveGuild(Guild guild) {
        String sql = upsert("guilds",
                "id, name, leader, balance, level, xp, home_location, permissions, forum_thread_id",
                "?, ?, ?, ?, ?, ?, ?, ?, ?");
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, guild.getId().toString());
                    ps.setString(2, guild.getName());
                    ps.setString(3, guild.getLeader().toString());
                    ps.setLong(4, guild.getBalance());
                    ps.setInt(5, guild.getLevel());
                    ps.setLong(6, guild.getXp());
                    ps.setString(7, guild.getHomeLocation());
                    String ranksJson = gson.toJson(guild.getRanks());
                    ps.setString(8, ranksJson);
                    ps.setString(9, guild.getForumThreadId());
                    ps.executeUpdate();
                }
                saveMembers(conn, guild);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка сохранения гильдии в БД", e);
        }
    }

    private void loadMembers(Connection conn, Guild guild) throws SQLException {
        String sql = "SELECT * FROM guild_members WHERE guild_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, guild.getId().toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID pUuid = UUID.fromString(rs.getString("player_uuid"));
                    String rankId = rs.getString("rank");
                    
                    // Fallback для старых баз данных, где использовался Enum в верхнем регистре
                    if (rankId.equals("LEADER")) rankId = "leader";
                    else if (rankId.equals("OFFICER")) rankId = "officer";
                    else if (rankId.equals("MEMBER")) rankId = "member";
                    else if (rankId.equals("RECRUIT")) rankId = "recruit";
                    
                    guild.getMembers().put(pUuid, rankId);
                }
            }
        }
    }

    private void saveMembers(Connection conn, Guild guild) throws SQLException {
        // Сначала удаляем всех участников, чтобы синхронизировать состав (убирает вышедших)
        try (PreparedStatement del = conn.prepareStatement("DELETE FROM guild_members WHERE guild_id = ?")) {
            del.setString(1, guild.getId().toString());
            del.executeUpdate();
        }
        String sql = upsert("guild_members", "player_uuid, guild_id, rank", "?, ?, ?");
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (var entry : guild.getMembers().entrySet()) {
                ps.setString(1, entry.getKey().toString());
                ps.setString(2, guild.getId().toString());
                ps.setString(3, entry.getValue());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public void deleteGuild(UUID guildId) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM guilds WHERE id = ?")) {
                    ps.setString(1, guildId.toString());
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM guild_members WHERE guild_id = ?")) {
                    ps.setString(1, guildId.toString());
                    ps.executeUpdate();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка удаления гильдии из БД", e);
        }
    }

    // --- ME СЕТИ ---
    public List<MENetwork> loadAllMENetworks() {
        List<MENetwork> networks = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT * FROM me_networks")) {
             
            while (rs.next()) {
                UUID id = UUID.fromString(rs.getString("id"));
                UUID owner = UUID.fromString(rs.getString("owner"));
                MENetwork net = new MENetwork(id, owner);
                net.setMaxCapacity(rs.getLong("capacity"));
                networks.add(net);
            }
            
            for (MENetwork net : networks) {
                try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM me_nodes WHERE network_id = ?")) {
                    ps.setString(1, net.getNetworkId().toString());
                    try (ResultSet nodeRs = ps.executeQuery()) {
                        while (nodeRs.next()) {
                            LocationKey loc = new LocationKey(nodeRs.getString("world"), nodeRs.getInt("x"), nodeRs.getInt("y"), nodeRs.getInt("z"));
                            net.addNode(new MENode(loc, MENode.NodeType.valueOf(nodeRs.getString("type"))));
                        }
                    }
                }
                
                try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM me_items WHERE network_id = ?")) {
                    ps.setString(1, net.getNetworkId().toString());
                    try (ResultSet itemRs = ps.executeQuery()) {
                        while (itemRs.next()) {
                            net.insertItem(itemRs.getString("item_hash"), itemRs.getLong("amount"));
                        }
                    }
                }
                
                try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM me_drives WHERE network_id = ?")) {
                    ps.setString(1, net.getNetworkId().toString());
                    try (ResultSet driveRs = ps.executeQuery()) {
                        while (driveRs.next()) {
                            LocationKey loc = new LocationKey(driveRs.getString("world"), driveRs.getInt("x"), driveRs.getInt("y"), driveRs.getInt("z"));
                            net.getDriveInventories().put(loc, driveRs.getString("inventory_base64"));
                        }
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка загрузки ME-сетей из БД", e);
        }
        return networks;
    }

    public void saveMENetwork(MENetwork net) {
        String netIdStr = net.getNetworkId().toString();
        
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
            try (PreparedStatement ps = conn.prepareStatement(upsert("me_networks", "id, owner, capacity", "?, ?, ?"))) {
                ps.setString(1, netIdStr);
                ps.setString(2, net.getOwner().toString());
                ps.setLong(3, net.getMaxCapacity());
                ps.executeUpdate();
            }
            
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM me_nodes WHERE network_id = ?")) { ps.setString(1, netIdStr); ps.executeUpdate(); }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM me_items WHERE network_id = ?")) { ps.setString(1, netIdStr); ps.executeUpdate(); }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM me_drives WHERE network_id = ?")) { ps.setString(1, netIdStr); ps.executeUpdate(); }
            
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO me_nodes (network_id, world, x, y, z, type) VALUES (?, ?, ?, ?, ?, ?)")) {
                for (MENode node : net.getNodes()) {
                    ps.setString(1, netIdStr);
                    ps.setString(2, node.getLocation().worldName());
                    ps.setInt(3, node.getLocation().x());
                    ps.setInt(4, node.getLocation().y());
                    ps.setInt(5, node.getLocation().z());
                    ps.setString(6, node.getType().name());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO me_items (network_id, item_hash, amount) VALUES (?, ?, ?)")) {
                for (var entry : net.getStorage().entrySet()) {
                    ps.setString(1, netIdStr);
                    ps.setString(2, entry.getKey());
                    ps.setLong(3, entry.getValue());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO me_drives (network_id, world, x, y, z, inventory_base64) VALUES (?, ?, ?, ?, ?, ?)")) {
                for (var entry : net.getDriveInventories().entrySet()) {
                    ps.setString(1, netIdStr);
                    ps.setString(2, entry.getKey().worldName());
                    ps.setInt(3, entry.getKey().x());
                    ps.setInt(4, entry.getKey().y());
                    ps.setInt(5, entry.getKey().z());
                    ps.setString(6, entry.getValue());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            
            conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка сохранения ME-сети в БД", e);
        }
    }

    // --- РЕЗЕРВУАРЫ КРОВИ ---
    public Map<org.bukkit.Location, Integer> loadAllBloodTanks() {
        Map<org.bukkit.Location, Integer> tanks = new java.util.concurrent.ConcurrentHashMap<>();
        try (Connection conn = dataSource.getConnection();
             Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT * FROM blood_tanks")) {
            while (rs.next()) {
                String worldName = rs.getString("world");
                int x = rs.getInt("x");
                int y = rs.getInt("y");
                int z = rs.getInt("z");
                int amount = rs.getInt("amount");
                org.bukkit.World world = org.bukkit.Bukkit.getWorld(worldName);
                if (world != null) {
                    tanks.put(new org.bukkit.Location(world, x, y, z), amount);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка загрузки кровавых котлов из БД", e);
        }
        return tanks;
    }

    public void saveBloodTank(org.bukkit.Location loc, int amount) {
        if (loc.getWorld() == null) return;
        if (amount <= 0) {
            // Удаляем если 0
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM blood_tanks WHERE world = ? AND x = ? AND y = ? AND z = ?")) {
                ps.setString(1, loc.getWorld().getName());
                ps.setInt(2, loc.getBlockX());
                ps.setInt(3, loc.getBlockY());
                ps.setInt(4, loc.getBlockZ());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Ошибка удаления кровавого котла из БД", e);
            }
            return;
        }
        String sql = upsert("blood_tanks", "world, x, y, z, amount", "?, ?, ?, ?, ?");
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, loc.getWorld().getName());
            ps.setInt(2, loc.getBlockX());
            ps.setInt(3, loc.getBlockY());
            ps.setInt(4, loc.getBlockZ());
            ps.setInt(5, amount);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка сохранения кровавого котла в БД", e);
        }
    }

    // --- ВРАТА БЕЗДНЫ (RIFTS) ---
    public List<org.bukkit.Location> loadAllRifts() {
        List<org.bukkit.Location> rifts = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT * FROM rifts")) {
            while (rs.next()) {
                String worldName = rs.getString("world");
                int x = rs.getInt("x");
                int y = rs.getInt("y");
                int z = rs.getInt("z");
                org.bukkit.World world = org.bukkit.Bukkit.getWorld(worldName);
                if (world != null) {
                    rifts.add(new org.bukkit.Location(world, x, y, z));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка загрузки врат бездны из БД", e);
        }
        return rifts;
    }

    public void saveRift(org.bukkit.Location loc) {
        if (loc.getWorld() == null) return;
        String sql = upsert("rifts", "world, x, y, z", "?, ?, ?, ?");
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, loc.getWorld().getName());
            ps.setInt(2, loc.getBlockX());
            ps.setInt(3, loc.getBlockY());
            ps.setInt(4, loc.getBlockZ());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка сохранения врат бездны в БД", e);
        }
    }

    public void deleteRift(org.bukkit.Location loc) {
        if (loc.getWorld() == null) return;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM rifts WHERE world = ? AND x = ? AND y = ? AND z = ?")) {
            ps.setString(1, loc.getWorld().getName());
            ps.setInt(2, loc.getBlockX());
            ps.setInt(3, loc.getBlockY());
            ps.setInt(4, loc.getBlockZ());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка удаления врат бездны из БД", e);
        }
    }
}