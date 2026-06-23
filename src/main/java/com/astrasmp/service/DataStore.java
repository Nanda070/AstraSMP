package com.astrasmp.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;

import com.astrasmp.AstraSMPPlugin;
import com.astrasmp.model.AuctionLot;
import com.astrasmp.model.ContractRecord;
import com.astrasmp.model.LinkRecord;
import com.astrasmp.model.MENetwork;
import com.astrasmp.model.PlayerProfile;
import com.astrasmp.util.LocationKey;

public final class DataStore {
    private final AstraSMPPlugin plugin;
    private final Map<String, PlayerProfile> profiles = new ConcurrentHashMap<>();
    private final Map<Long, AuctionLot> lots = new ConcurrentHashMap<>();
    private final Map<String, LinkRecord> links = new ConcurrentHashMap<>();
    private final Map<Long, ContractRecord> contracts = new ConcurrentHashMap<>();
    
    private final Set<LocationKey> rtpBlocks = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<LocationKey> trampolineBlocks = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Map<UUID, MENetwork> meNetworks = new ConcurrentHashMap<>();
    
    private final Map<LocationKey, Long> awardBlocks = new ConcurrentHashMap<>();
    private final Map<LocationKey, Set<UUID>> awardHistory = new ConcurrentHashMap<>();

    private final Map<String, LocationKey> warps = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, LocationKey>> homes = new ConcurrentHashMap<>();

    private final AtomicBoolean saveQueued = new AtomicBoolean(false);

    private final java.util.concurrent.atomic.AtomicLong nextLotIdCounter = new java.util.concurrent.atomic.AtomicLong(1L);
    private final java.util.concurrent.atomic.AtomicLong nextContractIdCounter = new java.util.concurrent.atomic.AtomicLong(1L);

    public DataStore(AstraSMPPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        profiles.clear();
        lots.clear();
        links.clear();
        contracts.clear();
        rtpBlocks.clear();
        trampolineBlocks.clear();
        meNetworks.clear();
        awardBlocks.clear();
        awardHistory.clear();
        warps.clear();
        homes.clear();

        // Мы БОЛЬШЕ не загружаем все профили сразу в память!
        // Это решает проблему утечки памяти при старте сервера.
        // Загружаем только аукционы, контракты и ME сети.
        
        plugin.getDatabase().loadAllLots().forEach(l -> {
            lots.put(l.getId(), l);
            if (l.getId() >= nextLotIdCounter.get()) nextLotIdCounter.set(l.getId() + 1);
        });

        plugin.getDatabase().loadAllContracts().forEach(c -> {
            contracts.put(c.getId(), c);
            if (c.getId() >= nextContractIdCounter.get()) nextContractIdCounter.set(c.getId() + 1);
        });

        plugin.getDatabase().loadAllMENetworks().forEach(net -> meNetworks.put(net.getNetworkId(), net));

        // Загрузка локальных файлов конфигурации YAML
        loadLinks();
        loadRtpBlocks();
        loadTrampolines();
        loadAwards();
        loadWarps();
        loadHomes();
    }

    private File file(String name) {
        File dir = plugin.getDataFolder();
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, name);
    }

    public Map<String, PlayerProfile> profiles() { return profiles; }
    public Map<Long, AuctionLot> lots() { return lots; }
    public Map<String, LinkRecord> links() { return links; }
    public Map<Long, ContractRecord> contracts() { return contracts; }
    public Set<LocationKey> getRtpBlocks() { return rtpBlocks; }
    public Set<LocationKey> getTrampolineBlocks() { return trampolineBlocks; }
    public Map<UUID, MENetwork> meNetworks() { return meNetworks; }
    public Map<String, LocationKey> getWarps() { return warps; }
    public Map<UUID, Map<String, LocationKey>> getHomes() { return homes; }

    public Collection<AuctionLot> activeLots() {
        return lots.values().stream()
                .filter(l -> !l.isSold())
                .sorted(Comparator.comparingLong(AuctionLot::getId).reversed())
                .toList();
    }

    public Collection<ContractRecord> activeContracts() {
        return contracts.values().stream()
                .filter(ContractRecord::isActive)
                .sorted(Comparator.comparingLong(ContractRecord::getId).reversed())
                .toList();
    }

    public void requestSave() {
        if (!saveQueued.compareAndSet(false, true)) return;
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::saveAll);
    }

    public void saveAllNow() {
        profiles.values().stream().filter(PlayerProfile::isDirty).forEach(p -> {
            plugin.getDatabase().saveProfile(p);
            p.setDirty(false);
        });
        lots.values().forEach(plugin.getDatabase()::saveLot);
        contracts.values().forEach(plugin.getDatabase()::saveContract);
        meNetworks.values().forEach(plugin.getDatabase()::saveMENetwork);
        
        saveLinks();
        saveRtpBlocks();
        saveTrampolines();
        saveAwards();
        saveWarps();
        saveHomes();
    }

    private void saveAll() {
        // Сбрасываем флаг ДО сохранения, чтобы новые requestSave() во время
        // сохранения снова поставили задачу в очередь и данные не потерялись
        saveQueued.set(false);
        saveAllNow();
    }

    private void loadLinks() {
        File f = file("links.yml");
        if (!f.exists()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(f);
        var sec = yml.getConfigurationSection("links");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            var s = sec.getConfigurationSection(key);
            if (s == null) continue;
            links.put(key, new LinkRecord(key, s.getString("discordId", ""), s.getString("code", ""), s.getBoolean("verified", false)));
        }
    }

    private void saveLinks() {
        File f = file("links.yml");
        YamlConfiguration yml = new YamlConfiguration();
        links.forEach((uuid, l) -> {
            String path = "links." + uuid;
            yml.set(path + ".discordId", l.getDiscordId());
            yml.set(path + ".code", l.getCode());
            yml.set(path + ".verified", l.isVerified());
        });
        try { 
            yml.save(f); 
        } catch (IOException e) { 
            plugin.getLogger().log(Level.SEVERE, "Критическая ошибка сохранения файла links.yml", e); 
        }
    }

    private void loadRtpBlocks() {
        File f = file("rtp_blocks.yml");
        if (!f.exists()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(f);
        for (String s : yml.getStringList("blocks")) {
            String[] p = s.split(",");
            if (p.length == 4) {
                rtpBlocks.add(new LocationKey(p[0], Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3])));
            }
        }
    }

    private void saveRtpBlocks() {
        File f = file("rtp_blocks.yml");
        YamlConfiguration yml = new YamlConfiguration();
        List<String> locs = new ArrayList<>();
        rtpBlocks.forEach(l -> locs.add(l.worldName() + "," + l.x() + "," + l.y() + "," + l.z()));
        yml.set("blocks", locs);
        try { 
            yml.save(f); 
        } catch (IOException e) { 
            plugin.getLogger().log(Level.SEVERE, "Критическая ошибка сохранения файла rtp_blocks.yml", e); 
        }
    }

    private void loadTrampolines() {
        File f = file("trampoline_blocks.yml");
        if (!f.exists()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(f);
        for (String s : yml.getStringList("blocks")) {
            String[] p = s.split(",");
            if (p.length == 4) {
                trampolineBlocks.add(new LocationKey(p[0], Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3])));
            }
        }
    }

    private void saveTrampolines() {
        File f = file("trampoline_blocks.yml");
        YamlConfiguration yml = new YamlConfiguration();
        List<String> locs = new ArrayList<>();
        trampolineBlocks.forEach(l -> locs.add(l.worldName() + "," + l.x() + "," + l.y() + "," + l.z()));
        yml.set("blocks", locs);
        try { 
            yml.save(f); 
        } catch (IOException e) { 
            plugin.getLogger().log(Level.SEVERE, "Критическая ошибка сохранения файла trampoline_blocks.yml", e); 
        }
    }

    private void loadAwards() {
        File f = file("awards.yml");
        if (!f.exists()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(f);
        
        var blocksSec = yml.getConfigurationSection("blocks");
        if (blocksSec != null) {
            for (String locStr : blocksSec.getKeys(false)) {
                String[] p = locStr.split(",");
                if (p.length == 4) {
                    LocationKey key = new LocationKey(p[0], Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]));
                    awardBlocks.put(key, blocksSec.getLong(locStr));
                }
            }
        }
        
        var historySec = yml.getConfigurationSection("history");
        if (historySec != null) {
            for (String locStr : historySec.getKeys(false)) {
                String[] p = locStr.split(",");
                if (p.length == 4) {
                    LocationKey key = new LocationKey(p[0], Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]));
                    List<String> uuids = historySec.getStringList(locStr);
                    Set<UUID> set = Collections.newSetFromMap(new ConcurrentHashMap<>());
                    for (String u : uuids) set.add(UUID.fromString(u));
                    awardHistory.put(key, set);
                }
            }
        }
    }

    private void saveAwards() {
        File f = file("awards.yml");
        YamlConfiguration yml = new YamlConfiguration();
        
        awardBlocks.forEach((key, amount) -> {
            String locStr = key.worldName() + "," + key.x() + "," + key.y() + "," + key.z();
            yml.set("blocks." + locStr, amount);
        });
        
        awardHistory.forEach((key, uuids) -> {
            String locStr = key.worldName() + "," + key.x() + "," + key.y() + "," + key.z();
            List<String> list = new ArrayList<>();
            for (UUID u : uuids) list.add(u.toString());
            yml.set("history." + locStr, list);
        });
        
        try { 
            yml.save(f); 
        } catch (IOException e) { 
            plugin.getLogger().log(Level.SEVERE, "Критическая ошибка сохранения файла awards.yml", e); 
        }
    }

    private void loadWarps() {
        File f = file("warps.yml");
        if (!f.exists()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(f);
        var sec = yml.getConfigurationSection("warps");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                String locStr = sec.getString(key);
                if (locStr != null) {
                    String[] p = locStr.split(",");
                    if (p.length == 4) {
                        warps.put(key.toLowerCase(), new LocationKey(p[0], Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3])));
                    }
                }
            }
        }
    }

    private void saveWarps() {
        File f = file("warps.yml");
        YamlConfiguration yml = new YamlConfiguration();
        warps.forEach((name, loc) -> {
            yml.set("warps." + name, loc.worldName() + "," + loc.x() + "," + loc.y() + "," + loc.z());
        });
        try { yml.save(f); } catch (IOException e) { plugin.getLogger().log(Level.SEVERE, "Ошибка сохранения warps.yml", e); }
    }

    private void loadHomes() {
        File f = file("homes.yml");
        if (!f.exists()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(f);
        var sec = yml.getConfigurationSection("homes");
        if (sec != null) {
            for (String uuidStr : sec.getKeys(false)) {
                UUID uuid = UUID.fromString(uuidStr);
                var homeSec = sec.getConfigurationSection(uuidStr);
                if (homeSec != null) {
                    Map<String, LocationKey> playerHomes = new ConcurrentHashMap<>();
                    for (String homeName : homeSec.getKeys(false)) {
                        String locStr = homeSec.getString(homeName);
                        if (locStr != null) {
                            String[] p = locStr.split(",");
                            if (p.length == 4) {
                                playerHomes.put(homeName.toLowerCase(), new LocationKey(p[0], Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3])));
                            }
                        }
                    }
                    homes.put(uuid, playerHomes);
                }
            }
        }
    }

    private void saveHomes() {
        File f = file("homes.yml");
        YamlConfiguration yml = new YamlConfiguration();
        homes.forEach((uuid, playerHomes) -> {
            playerHomes.forEach((homeName, loc) -> {
                yml.set("homes." + uuid.toString() + "." + homeName, loc.worldName() + "," + loc.x() + "," + loc.y() + "," + loc.z());
            });
        });
        try { yml.save(f); } catch (IOException e) { plugin.getLogger().log(Level.SEVERE, "Ошибка сохранения homes.yml", e); }
    }

    public PlayerProfile profile(String uuid, String name) {
        PlayerProfile p = profiles.computeIfAbsent(uuid, k -> {
            PlayerProfile dbProfile = plugin.getDatabase().loadProfile(uuid);
            if (dbProfile != null) {
                return dbProfile;
            }
            String finalName = name;
            if (finalName == null) {
                try {
                    org.bukkit.OfflinePlayer op = org.bukkit.Bukkit.getOfflinePlayer(java.util.UUID.fromString(uuid));
                    finalName = op.getName() != null ? op.getName() : "Unknown";
                } catch (Exception e) {
                    finalName = "Unknown";
                }
            }
            return new PlayerProfile(uuid, finalName, 0L, 50, 0, 0, 0L, 0, "", 1);
        });
        if (name != null) p.setName(name);
        return p;
    }

    public void unloadProfile(String uuid) {
        PlayerProfile p = profiles.get(uuid);
        if (p != null) {
            if (p.isDirty()) {
                plugin.getDatabase().saveProfile(p);
                p.setDirty(false);
            }
            profiles.remove(uuid);
        }
    }

    public LinkRecord link(String uuid) { 
        return links.computeIfAbsent(uuid, k -> new LinkRecord(uuid, "", "", false)); 
    }
    
    public long nextLotId() { return nextLotIdCounter.getAndIncrement(); }
    public long nextContractId() { return nextContractIdCounter.getAndIncrement(); }

    public void addAwardBlock(Location loc, long amount) {
        LocationKey key = LocationKey.fromLocation(loc);
        if (key != null) awardBlocks.put(key, amount);
    }

    public void removeAwardBlock(Location loc) {
        LocationKey key = LocationKey.fromLocation(loc);
        if (key != null) {
            awardBlocks.remove(key);
            awardHistory.remove(key);
        }
    }


    public Long getAwardAmount(Location loc) {
        LocationKey key = LocationKey.fromLocation(loc);
        return key != null ? awardBlocks.get(key) : null;
    }

    public boolean hasAlreadyCollected(org.bukkit.entity.Player p, Location loc) {
        LocationKey key = LocationKey.fromLocation(loc);
        if (key == null || !awardHistory.containsKey(key)) return false;
        return awardHistory.get(key).contains(p.getUniqueId());
    }

    public void markAsCollected(org.bukkit.entity.Player p, Location loc) {
        LocationKey key = LocationKey.fromLocation(loc);
        if (key != null) {
            awardHistory.computeIfAbsent(key, k -> Collections.newSetFromMap(new ConcurrentHashMap<>())).add(p.getUniqueId());
        }
    }
}