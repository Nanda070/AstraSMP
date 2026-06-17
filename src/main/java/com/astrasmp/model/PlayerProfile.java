package com.astrasmp.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Модель профиля игрока AstraSMP.
 * Хранит всю статистику, экономические данные и кастомизацию.
 */
public final class PlayerProfile {
    private final String uuid;
    private String name;
    private long coins;
    private int mmr;
    private int kills;
    private int deaths;
    private long soldValue;
    private int eventPoints;
    private String faction;
    private int baseLevel;
    private String partnerUuid = "";

    private int questStep = 1;      // Текущий номер квеста
    private int questProgress = 0;  // Прогресс (например, сколько блоков сломано)

    // Поля для ежедневных квестов
    private Map<String, Integer> dailyQuests = new ConcurrentHashMap<>();
    private String dailyQuestDate = "";

    // Поля для календаря наград и талантов
    private int loginStreak = 0;
    private String lastLoginDate = "";
    private int dailyRewardDay = 1;
    private Map<String, Integer> talents = new ConcurrentHashMap<>();

    // Поля для кастомных префиксов
    private String customPrefix = "";
    private String prefixColor = "&7";

    // Темная Магия
    private int corruption = 0;
    private String pactType = "";
    private boolean unlockedPocketDimension = false;

    // transient поля не сохраняются в конфиг-файлы
    private transient boolean frozen = false;
    private transient boolean dirty = true;

    public boolean isDirty() { return dirty; }
    public void setDirty(boolean dirty) { this.dirty = dirty;  this.dirty = true; }

    public PlayerProfile(String uuid, String name, long coins, int mmr, int kills, int deaths, long soldValue, int eventPoints, String faction, int baseLevel) {
        this.uuid = uuid;
        this.name = name;
        this.coins = coins;
        this.mmr = mmr;
        this.kills = kills;
        this.deaths = deaths;
        this.soldValue = soldValue;
        this.eventPoints = eventPoints;
        this.faction = faction;
        this.baseLevel = baseLevel;
    }

    // --- ГЕТТЕРЫ И СЕТТЕРЫ ДЛЯ ПРЕФИКСОВ ---

    public String getCustomPrefix() {
        return customPrefix;
    }

    public void setCustomPrefix(String customPrefix) {
        this.customPrefix = customPrefix;
        this.dirty = true;
    }

    public String getPrefixColor() {
        return prefixColor;
    }

    public void setPrefixColor(String prefixColor) {
        this.prefixColor = prefixColor;
        this.dirty = true;
    }

    public int getQuestStep() { return questStep; }
    public void setQuestStep(int questStep) { this.questStep = questStep;  this.dirty = true; }

    public int getQuestProgress() { return questProgress; }
    public void setQuestProgress(int questProgress) { this.questProgress = questProgress;  this.dirty = true; }

    public Map<String, Integer> getDailyQuests() { return dailyQuests; }
    public void setDailyQuests(Map<String, Integer> dailyQuests) { this.dailyQuests = dailyQuests;  this.dirty = true; }

    public String getDailyQuestDate() { return dailyQuestDate; }
    public void setDailyQuestDate(String dailyQuestDate) { this.dailyQuestDate = dailyQuestDate;  this.dirty = true; }

    // --- ГЕТТЕРЫ И СЕТТЕРЫ НАГРАД И ТАЛАНТОВ ---
    public int getLoginStreak() { return loginStreak; }
    public void setLoginStreak(int loginStreak) { this.loginStreak = loginStreak;  this.dirty = true; }

    public String getLastLoginDate() { return lastLoginDate; }
    public void setLastLoginDate(String lastLoginDate) { this.lastLoginDate = lastLoginDate;  this.dirty = true; }

    public int getDailyRewardDay() { return dailyRewardDay; }
    public void setDailyRewardDay(int dailyRewardDay) { this.dailyRewardDay = dailyRewardDay;  this.dirty = true; }

    public Map<String, Integer> getTalents() { return talents; }
    public void setTalents(Map<String, Integer> talents) { this.talents = talents;  this.dirty = true; }
    public int getTalentLevel(String talentId) { return talents.getOrDefault(talentId, 0); }
    public void setTalentLevel(String talentId, int level) { talents.put(talentId, level);  this.dirty = true; }

    // --- ГЕТТЕРЫ И СЕТТЕРЫ ТЕМНОЙ МАГИИ ---
    public int getCorruption() { return corruption; }
    public void setCorruption(int corruption) { this.corruption = Math.max(0, corruption);  this.dirty = true; }

    public boolean hasPact() { return pactType != null && !pactType.isEmpty(); }
    public String getPactType() { return pactType == null ? "" : pactType; }
    public void setPactType(String pactType) { this.pactType = pactType == null ? "" : pactType;  this.dirty = true; }

    public boolean isUnlockedPocketDimension() {
        return unlockedPocketDimension;
    }

    public void setUnlockedPocketDimension(boolean unlocked) {
        this.unlockedPocketDimension = unlocked;
        this.dirty = true;
    }

    // --- ОСТАЛЬНЫЕ ГЕТТЕРЫ И СЕТТЕРЫ ---

    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.dirty = true;
    }

    public long getCoins() {
        return coins;
    }

    public void setCoins(long coins) {
        this.coins = Math.max(0L, coins);
        this.dirty = true;
    }

    public int getMmr() {
        return mmr;
    }

    public void setMmr(int mmr) {
        this.mmr = Math.max(0, mmr);
        this.dirty = true;
    }

    public int getKills() {
        return kills;
    }

    public void setKills(int kills) {
        this.kills = Math.max(0, kills);
        this.dirty = true;
    }

    public int getDeaths() {
        return deaths;
    }

    public void setDeaths(int deaths) {
        this.deaths = Math.max(0, deaths);
        this.dirty = true;
    }

    public long getSoldValue() {
        return soldValue;
    }

    public void setSoldValue(long soldValue) {
        this.soldValue = Math.max(0L, soldValue);
        this.dirty = true;
    }

    public int getEventPoints() {
        return eventPoints;
    }

    public void setEventPoints(int eventPoints) {
        this.eventPoints = Math.max(0, eventPoints);
        this.dirty = true;
    }

    public String getFaction() {
        return faction;
    }

    public void setFaction(String faction) {
        this.faction = faction == null ? "" : faction;
        this.dirty = true;
    }

    public int getBaseLevel() {
        return baseLevel;
    }

    public void setBaseLevel(int baseLevel) {
        this.baseLevel = Math.max(1, baseLevel);
        this.dirty = true;
    }

    public String getPartnerUuid() {
        return partnerUuid;
    }

    public void setPartnerUuid(String partnerUuid) {
        this.partnerUuid = partnerUuid;
        this.dirty = true;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
        this.dirty = true;
    }
}