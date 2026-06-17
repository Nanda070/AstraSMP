package com.astrasmp.service;

import com.astrasmp.AstraSMPPlugin;
import com.astrasmp.model.PlayerProfile;
import com.astrasmp.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class QuestManager {
    private final AstraSMPPlugin plugin;
    private final ServiceManager services;
    
    private final Map<Integer, QuestData> baseQuests = new ConcurrentHashMap<>();
    private final Map<String, QuestData> dailyQuestsPool = new ConcurrentHashMap<>();

    public enum QuestAction {
        MINE_BLOCK, KILL_MOB, SELL_ITEM, SMELT, AH_SELL, ATTEND_EVENT, USE_RTP, LINK_DISCORD, USE_COMMAND, CUSTOM
    }

    public record QuestData(String id, String name, QuestAction action, String target, int requiredAmount, String rewardInfo, List<String> rewardCommands) {}

    public QuestManager(AstraSMPPlugin plugin, ServiceManager services) {
        this.plugin = plugin;
        this.services = services;
        loadConfigs();
    }

    public void loadConfigs() {
        baseQuests.clear();
        dailyQuestsPool.clear();

        com.astrasmp.util.ConfigUpdater.updateConfig(plugin, "quests.yml");
        File questsFile = new File(plugin.getDataFolder(), "quests.yml");
        if (questsFile.exists()) {
            YamlConfiguration qConfig = YamlConfiguration.loadConfiguration(questsFile);
            if (qConfig.getConfigurationSection("base") != null) {
                for (String key : qConfig.getConfigurationSection("base").getKeys(false)) {
                    try {
                        int step = Integer.parseInt(key);
                        String path = "base." + key;
                        QuestData qd = parseQuestData(key, qConfig, path);
                        baseQuests.put(step, qd);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        com.astrasmp.util.ConfigUpdater.updateConfig(plugin, "daily_quests.yml");
        File dailyFile = new File(plugin.getDataFolder(), "daily_quests.yml");
        if (dailyFile.exists()) {
            YamlConfiguration dConfig = YamlConfiguration.loadConfiguration(dailyFile);
            if (dConfig.getConfigurationSection("quests") != null) {
                for (String key : dConfig.getConfigurationSection("quests").getKeys(false)) {
                    String path = "quests." + key;
                    QuestData qd = parseQuestData(key, dConfig, path);
                    dailyQuestsPool.put(key, qd);
                }
            }
        }
    }

    private QuestData parseQuestData(String id, YamlConfiguration config, String path) {
        String name = config.getString(path + ".name", "Unknown Quest");
        QuestAction action = QuestAction.valueOf(config.getString(path + ".action", "CUSTOM").toUpperCase());
        String target = config.getString(path + ".target", "");
        int required = config.getInt(path + ".required", 1);
        String rewardInfo = config.getString(path + ".rewardInfo", "");
        List<String> rewardCmds = config.getStringList(path + ".rewardCommands");
        return new QuestData(id, name, action, target, required, rewardInfo, rewardCmds);
    }

    public boolean matchesTarget(String configTarget, String currentTarget) {
        if (configTarget == null || configTarget.isEmpty()) return true;
        if (configTarget.equalsIgnoreCase(currentTarget)) return true;
        
        String upperTarget = currentTarget.toUpperCase();
        if (configTarget.equals("LOG") && upperTarget.endsWith("_LOG")) return true;
        if (configTarget.equals("STONE") && (upperTarget.equals("STONE") || upperTarget.equals("COBBLESTONE") || upperTarget.equals("DEEPSLATE"))) return true;
        if (configTarget.equals("INGOT") && upperTarget.endsWith("_INGOT")) return true;
        if (configTarget.equals("ORE") && (upperTarget.endsWith("_ORE") || upperTarget.startsWith("RAW_"))) return true;
        
        return upperTarget.contains(configTarget.toUpperCase());
    }

    public String generateDescription(QuestData q) {
        return switch (q.action()) {
            case MINE_BLOCK -> "Добыть блоки: " + q.target() + " (" + q.requiredAmount() + " шт.)";
            case KILL_MOB -> "Убить мобов: " + (q.target().isEmpty() ? "Любых" : q.target()) + " (" + q.requiredAmount() + " шт.)";
            case SELL_ITEM -> "Продать предметы " + q.requiredAmount() + " раз";
            case SMELT -> "Переплавить " + q.target() + " (" + q.requiredAmount() + " шт.)";
            case AH_SELL -> "Продать на аукционе " + q.requiredAmount() + " раз";
            case ATTEND_EVENT -> "Посетить ивенты (" + q.requiredAmount() + " раз)";
            case USE_RTP -> "Использовать РТП (" + q.requiredAmount() + " раз)";
            case LINK_DISCORD -> "Привязать Discord";
            case USE_COMMAND -> "Использовать команду: " + q.target();
            case CUSTOM -> "Специальное задание";
        };
    }

    public void processAction(Player player, QuestAction action, String target, int amount) {
        PlayerProfile profile = services.economy().profile(player.getUniqueId(), player.getName());
        checkDailyQuestsDate(profile);

        int step = profile.getQuestStep();
        QuestData baseQ = baseQuests.get(step);
        if (baseQ != null && baseQ.action() == action && matchesTarget(baseQ.target(), target)) {
            profile.setQuestProgress(profile.getQuestProgress() + amount);
            int req = baseQ.requiredAmount();
            
            sendActionBarProgress(player, baseQ.name(), profile.getQuestProgress(), req);

            if (profile.getQuestProgress() >= req) {
                completeBaseQuest(player, profile, baseQ);
            }
        }

        boolean updatedDaily = false;
        for (Map.Entry<String, Integer> entry : profile.getDailyQuests().entrySet()) {
            String qId = entry.getKey();
            int progress = entry.getValue();
            QuestData dailyQ = dailyQuestsPool.get(qId);

            if (dailyQ != null && dailyQ.action() == action && matchesTarget(dailyQ.target(), target)) {
                if (progress < dailyQ.requiredAmount()) {
                    int newProgress = progress + amount;
                    profile.getDailyQuests().put(qId, newProgress);
                    updatedDaily = true;
                    
                    sendActionBarProgress(player, dailyQ.name(), newProgress, dailyQ.requiredAmount());

                    if (newProgress >= dailyQ.requiredAmount()) {
                        completeDailyQuest(player, profile, dailyQ);
                    }
                }
            }
        }
        
        if (updatedDaily) {
            services.store().requestSave();
        }
    }

    private void sendActionBarProgress(Player player, String name, int current, int max) {
        if (current <= max) {
            player.sendActionBar(Component.text(TextUtil.color("&8[&e⚔&8] &fКвест: &e" + name + " &8(&a" + current + "&7/&a" + max + "&8)")));
        }
    }

    public void checkDailyQuestsDate(PlayerProfile profile) {
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        if (!today.equals(profile.getDailyQuestDate())) {
            profile.setDailyQuestDate(today);
            profile.getDailyQuests().clear();
            
            if (!dailyQuestsPool.isEmpty()) {
                List<String> keys = new ArrayList<>(dailyQuestsPool.keySet());
                Collections.shuffle(keys);
                for (int i = 0; i < Math.min(3, keys.size()); i++) {
                    profile.getDailyQuests().put(keys.get(i), 0);
                }
            }
            services.store().requestSave();
        }
    }

    private void completeBaseQuest(Player player, PlayerProfile profile, QuestData q) {
        giveRewards(player, q);
        playCompletionEffects(player);

        profile.setQuestStep(profile.getQuestStep() + 1);
        profile.setQuestProgress(0);
        services.store().requestSave();

        QuestData next = baseQuests.get(profile.getQuestStep());
        if (next != null) {
            TextUtil.send(player, "&b&lКВЕСТЫ &8» &fСледующая цель: &e" + next.name());
        } else {
            TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_d8d7b3", "&b&lКВЕСТЫ &8» &aВы завершили все начальные задания!"));
            player.getWorld().spawn(player.getLocation(), org.bukkit.entity.Firework.class);
        }
    }

    private void completeDailyQuest(Player player, PlayerProfile profile, QuestData q) {
        giveRewards(player, q);
        playCompletionEffects(player);
        TextUtil.send(player, "&e&lЕЖЕДНЕВКИ &8» &fВы выполнили задание &a" + q.name() + "&f!");
    }

    private void giveRewards(Player player, QuestData q) {
        if (q.rewardCommands() != null) {
            java.util.Random randObj = new java.util.Random();
            for (String cmd : q.rewardCommands()) {
                String parsed = cmd.replace("%player%", player.getName());
                
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\{(\\d+)-(\\d+)\\}").matcher(parsed);
                StringBuffer sb = new StringBuffer();
                while (m.find()) {
                    int min = Integer.parseInt(m.group(1));
                    int max = Integer.parseInt(m.group(2));
                    int randVal = min + randObj.nextInt(max - min + 1);
                    m.appendReplacement(sb, String.valueOf(randVal));
                }
                m.appendTail(sb);
                parsed = sb.toString();
                
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
            }
        }
    }

    private void playCompletionEffects(Player player) {
        player.showTitle(Title.title(
                Component.text(TextUtil.color("&a&lКВЕСТ ВЫПОЛНЕН")),
                Component.text(TextUtil.color("&fВы получили награду!"))
        ));
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
    }

    public Map<Integer, QuestData> getBaseQuests() { return baseQuests; }
    public Map<String, QuestData> getDailyQuestsPool() { return dailyQuestsPool; }

    public void sendQuestInfo(Player player) {
        services.gui().openQuests(player);
    }

    public ItemStack createQuestItem(Player player) {
        ItemStack item = new ItemStack(Material.BOOK);
        item.editMeta(meta -> {
            meta.displayName(Component.text(TextUtil.color("&b&lКвесты и Ежедневки")));
            List<String> lore = new ArrayList<>();
            lore.add(TextUtil.color("&7Нажми, чтобы открыть меню заданий"));
            meta.lore(lore.stream().map(l -> Component.text(l)).toList());
        });
        return item;
    }
}