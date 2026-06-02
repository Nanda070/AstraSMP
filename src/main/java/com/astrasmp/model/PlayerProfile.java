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

    // Поля для кастомных префиксов
    private String customPrefix = "";
    private String prefixColor = "&7";

    // transient поля не сохраняются в конфиг-файлы
    private transient boolean frozen = false;

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
    }

    public String getPrefixColor() {
        return prefixColor;
    }

    public void setPrefixColor(String prefixColor) {
        this.prefixColor = prefixColor;
    }

    public int getQuestStep() { return questStep; }
    public void setQuestStep(int questStep) { this.questStep = questStep; }

    public int getQuestProgress() { return questProgress; }
    public void setQuestProgress(int questProgress) { this.questProgress = questProgress; }

    public Map<String, Integer> getDailyQuests() { return dailyQuests; }
    public void setDailyQuests(Map<String, Integer> dailyQuests) { this.dailyQuests = dailyQuests; }

    public String getDailyQuestDate() { return dailyQuestDate; }
    public void setDailyQuestDate(String dailyQuestDate) { this.dailyQuestDate = dailyQuestDate; }

    // --- ОСТАЛЬНЫЕ ГЕТТЕРЫ И СЕТТЕРЫ ---

    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getCoins() {
        return coins;
    }

    public void setCoins(long coins) {
        this.coins = Math.max(0L, coins);
    }

    public int getMmr() {
        return mmr;
    }

    public void setMmr(int mmr) {
        this.mmr = Math.max(0, mmr);
    }

    public int getKills() {
        return kills;
    }

    public void setKills(int kills) {
        this.kills = Math.max(0, kills);
    }

    public int getDeaths() {
        return deaths;
    }

    public void setDeaths(int deaths) {
        this.deaths = Math.max(0, deaths);
    }

    public long getSoldValue() {
        return soldValue;
    }

    public void setSoldValue(long soldValue) {
        this.soldValue = Math.max(0L, soldValue);
    }

    public int getEventPoints() {
        return eventPoints;
    }

    public void setEventPoints(int eventPoints) {
        this.eventPoints = Math.max(0, eventPoints);
    }

    public String getFaction() {
        return faction;
    }

    public void setFaction(String faction) {
        this.faction = faction == null ? "" : faction;
    }

    public int getBaseLevel() {
        return baseLevel;
    }

    public void setBaseLevel(int baseLevel) {
        this.baseLevel = Math.max(1, baseLevel);
    }

    public String getPartnerUuid() {
        return partnerUuid;
    }

    public void setPartnerUuid(String partnerUuid) {
        this.partnerUuid = partnerUuid;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
    }
}