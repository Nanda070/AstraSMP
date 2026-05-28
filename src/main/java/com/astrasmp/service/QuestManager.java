package com.astrasmp.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.astrasmp.AstraSMPPlugin;
import com.astrasmp.model.PlayerProfile;
import com.astrasmp.util.TextUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;

public final class QuestManager {
    private final AstraSMPPlugin plugin;
    private final ServiceManager services;
    private final Map<Integer, QuestData> questRegistry = new ConcurrentHashMap<>();

    private record QuestData(String name, int requiredAmount, String rewardInfo, Consumer<PlayerRewardContext> rewardAction) {}
    private record PlayerRewardContext(Player player, PlayerProfile profile) {}

    public QuestManager(AstraSMPPlugin plugin, ServiceManager services) {
        this.plugin = plugin;
        this.services = services;
        initQuests();
    }

    private void initQuests() {
        questRegistry.put(1, new QuestData("Добыть 32 дерева", 32, "100 ❂, +5 MMR", ctx -> {
            ctx.profile().setCoins(ctx.profile().getCoins() + 100); ctx.profile().setMmr(ctx.profile().getMmr() + 5);
        }));
        questRegistry.put(2, new QuestData("Добыть 64 булыжника", 64, "150 ❂, +5 MMR", ctx -> {
            ctx.profile().setCoins(ctx.profile().getCoins() + 150); ctx.profile().setMmr(ctx.profile().getMmr() + 5);
        }));
        questRegistry.put(3, new QuestData("Продать ресурсы через /sell", 1, "200 ❂, +10 MMR", ctx -> {
            ctx.profile().setCoins(ctx.profile().getCoins() + 200); ctx.profile().setMmr(ctx.profile().getMmr() + 10);
        }));
        questRegistry.put(4, new QuestData("Переплавить 16 руды", 16, "300 ❂, 32 Стейка", ctx -> {
            ctx.profile().setCoins(ctx.profile().getCoins() + 300); ctx.player().getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 32));
        }));
        questRegistry.put(5, new QuestData("Выставить предмет на /ah", 1, "Монеты и MMR", ctx -> {
            ctx.profile().setCoins(ctx.profile().getCoins() + 500); ctx.profile().setMmr(ctx.profile().getMmr() + 15);
        }));
        questRegistry.put(6, new QuestData("Победить 20 любых существ", 20, "500 ❂, +15 MMR", ctx -> {
            ctx.profile().setCoins(ctx.profile().getCoins() + 500); ctx.profile().setMmr(ctx.profile().getMmr() + 15);
        }));
        questRegistry.put(7, new QuestData("Побывать на ивенте", 1, "Монеты и MMR", ctx -> {
            ctx.profile().setCoins(ctx.profile().getCoins() + 500); ctx.profile().setMmr(ctx.profile().getMmr() + 15);
        }));
        questRegistry.put(8, new QuestData("Использовать /rtp", 1, "Монеты и MMR", ctx -> {
            ctx.profile().setCoins(ctx.profile().getCoins() + 500); ctx.profile().setMmr(ctx.profile().getMmr() + 15);
        }));
        questRegistry.put(9, new QuestData("Добыть 5 алмазов", 5, "1000 ❂, +25 MMR", ctx -> {
            ctx.profile().setCoins(ctx.profile().getCoins() + 1000); ctx.profile().setMmr(ctx.profile().getMmr() + 25);
        }));
        questRegistry.put(10, new QuestData("Привязать Discord /link", 1, "1000 ❂, 16 Дуба", ctx -> {
            ctx.profile().setCoins(ctx.profile().getCoins() + 1000);
            ItemStack oakLogs = new ItemStack(Material.OAK_LOG, 16);
            if (ctx.player().getInventory().firstEmpty() == -1) {
                ctx.player().getWorld().dropItemNaturally(ctx.player().getLocation(), oakLogs);
                TextUtil.send(ctx.player(), "&eИнвентарь полон! Дуб выпал рядом с вами.");
            } else {
                ctx.player().getInventory().addItem(oakLogs);
            }
        }));
    }

    public void checkProgress(Player player, int step, int amount) {
        PlayerProfile profile = services.economy().profile(player.getUniqueId(), player.getName());
        if (profile.getQuestStep() != step) return;

        profile.setQuestProgress(profile.getQuestProgress() + amount);

        int required = getRequiredAmount(step);
        if (profile.getQuestProgress() >= required) {
            completeQuest(player, profile);
        } else {
            sendQuestStatus(player, profile);
        }
    }

    public void completeQuest(Player player, PlayerProfile profile) {
        int step = profile.getQuestStep();
        giveReward(player, profile, step);

        player.showTitle(Title.title(
                Component.text(TextUtil.color("&a&lКВЕСТ ВЫПОЛНЕН")),
                Component.text(TextUtil.color("&fВы получили награду!"))
        ));
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);

        profile.setQuestStep(step + 1);
        profile.setQuestProgress(0);
        services.store().requestSave();

        if (step < 10) {
            TextUtil.send(player, "&b&lКВЕСТЫ &8» &fСледующая цель: &e" + getQuestName(step + 1));
        } else {
            TextUtil.send(player, "&b&lКВЕСТЫ &8» &aВы завершили все задания! Добро пожаловать в ChetCraft!");
            player.getWorld().spawn(player.getLocation(), org.bukkit.entity.Firework.class);
        }
    }

    public void sendQuestStatus(Player player, PlayerProfile profile) {
        int step = profile.getQuestStep();
        if (step > 10) {
            TextUtil.send(player, "&b&lКВЕСТЫ &8» &aВсе задания выполнены!");
            return;
        }

        int current = profile.getQuestProgress();
        int needed = getRequiredAmount(step);

        TextUtil.send(player, "&b&lКВЕСТ &7» &f" + getQuestName(step));
        if (needed > 1) {
            TextUtil.send(player, "&fПрогресс: &e" + current + "&7/&e" + needed + " &8[" + getProgressBar(current, needed) + "&8]");
        }
    }

    private String getProgressBar(int current, int needed) {
        int bars = 10;
        int completed = (int) ((double) current / needed * bars);
        return "&a" + "|".repeat(Math.min(bars, completed)) + "&7" + "|".repeat(Math.max(0, bars - completed));
    }

    public void sendQuestInfo(Player player) {
        PlayerProfile profile = services.economy().profile(player.getUniqueId(), player.getName());
        int step = profile.getQuestStep();

        if (step > 10) {
            TextUtil.send(player, "&b&lКВЕСТЫ &8» &aВы настоящий гражданин ChetCraft!");
            return;
        }

        TextUtil.send(player, "");
        TextUtil.send(player, "  &b&lТЕКУЩИЙ КВЕСТ &7(Шаг " + step + "/10)");
        TextUtil.send(player, "  &fЦель: &e" + getQuestName(step));
        int req = getRequiredAmount(step);
        if (req > 1) {
            TextUtil.send(player, "  &fПрогресс: &b" + profile.getQuestProgress() + " &7/ &b" + req + " &8[" + getProgressBar(profile.getQuestProgress(), req) + "&8]");
        }
        TextUtil.send(player, "  &fНаграда: &a" + getQuestRewardInfo(step));
        TextUtil.send(player, "");
    }

    public ItemStack createQuestItem(Player player) {
        PlayerProfile profile = services.economy().profile(player.getUniqueId(), player.getName());
        int step = profile.getQuestStep();
        ItemStack item = new ItemStack(Material.BOOK);
        item.editMeta(meta -> {
            meta.displayName(Component.text(TextUtil.color("&b&lНачальные квесты")));
            List<String> lore = new ArrayList<>();
            if (step <= 10) {
                lore.add(TextUtil.color("&7Шаг: &f" + step + "/10"));
                lore.add(TextUtil.color("&fЦель: &e" + getQuestName(step)));

                int req = getRequiredAmount(step);
                if (req > 1) {
                    lore.add(TextUtil.color("&fПрогресс: &e" + profile.getQuestProgress() + "&7/&e" + req));
                }
                lore.add("");
                lore.add(TextUtil.color("&eНажми, чтобы узнать статус"));
            } else {
                lore.add(TextUtil.color("&aВсе задания выполнены!"));
            }
            meta.lore(lore.stream().map(l -> Component.text(l)).toList());
        });
        return item;
    }

    public String getQuestName(int step) {
        QuestData q = questRegistry.get(step);
        return q != null ? q.name() : "Завершено!";
    }

    public int getRequiredAmount(int step) {
        QuestData q = questRegistry.get(step);
        return q != null ? q.requiredAmount() : 1;
    }

    public String getQuestRewardInfo(int step) {
        QuestData q = questRegistry.get(step);
        return q != null ? q.rewardInfo() : "Монеты и MMR";
    }

    private void giveReward(Player player, PlayerProfile profile, int step) {
        QuestData q = questRegistry.get(step);
        if (q != null) {
            q.rewardAction().accept(new PlayerRewardContext(player, profile));
        } else {
            profile.setCoins(profile.getCoins() + 500);
            profile.setMmr(profile.getMmr() + 15);
        }
    }
}