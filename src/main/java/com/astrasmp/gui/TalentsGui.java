package com.astrasmp.gui;

import com.astrasmp.AstraSMPPlugin;
import com.astrasmp.model.PlayerProfile;
import com.astrasmp.service.ServiceManager;
import com.astrasmp.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

public class TalentsGui implements Listener {
    private final ServiceManager services;

    public TalentsGui(AstraSMPPlugin plugin, ServiceManager services) {
        this.services = services;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public record TalentsHolder() implements InventoryHolder {
        @Override
        public @NotNull Inventory getInventory() {
            return Bukkit.createInventory(null, 9);
        }
    }

    private static class TalentDef {
        String id;
        String name;
        Material icon;
        int maxLevel;
        String description;
        
        public TalentDef(String id, String name, Material icon, int maxLevel, String description) {
            this.id = id; this.name = name; this.icon = icon; this.maxLevel = maxLevel; this.description = description;
        }
    }

    private final Map<Integer, TalentDef> TALENTS = new LinkedHashMap<>();

    {
        TALENTS.put(10, new TalentDef("vampirism", "Вампиризм", Material.REDSTONE, 3, "Шанс восстановить здоровье при ударе"));
        TALENTS.put(12, new TalentDef("gladiator", "Гладиатор", Material.IRON_CHESTPLATE, 5, "Перманентно увеличивает максимальное здоровье"));
        TALENTS.put(14, new TalentDef("miner", "Обогатитель", Material.DIAMOND_PICKAXE, 3, "Шанс добыть руду в двойном размере"));
        TALENTS.put(16, new TalentDef("assassin", "Ассасин", Material.IRON_SWORD, 3, "Увеличивает наносимый урон в ближнем бою"));
        
        TALENTS.put(28, new TalentDef("acrobat", "Акробат", Material.FEATHER, 3, "Снижает урон от падения"));
        TALENTS.put(30, new TalentDef("tank", "Непробиваемый", Material.SHIELD, 3, "Шанс полностью проигнорировать получение урона"));
        TALENTS.put(32, new TalentDef("scavenger", "Мародер", Material.GOLD_INGOT, 3, "Увеличивает количество монет за убийство мобов"));
        TALENTS.put(34, new TalentDef("lucky_shot", "Меткий стрелок", Material.ARROW, 3, "Шанс не потратить стрелу при выстреле"));
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(new TalentsHolder(), 54, Component.text(TextUtil.color("&8Дерево Талантов")));
        
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        filler.editMeta(meta -> meta.displayName(Component.empty()));
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);

        PlayerProfile profile = services.store().profile(player.getUniqueId().toString(), player.getName());

        for (Map.Entry<Integer, TalentDef> entry : TALENTS.entrySet()) {
            int slot = entry.getKey();
            TalentDef t = entry.getValue();
            int level = profile.getTalentLevel(t.id);
            
            ItemStack item = new ItemStack(t.icon);
            item.editMeta(meta -> {
                meta.displayName(Component.text(TextUtil.color("&6&l" + t.name + " &e(" + level + "/" + t.maxLevel + ")")));
                
                long costCoins = getCostCoins(level + 1);
                int costEp = getCostEp(level + 1);
                
                if (level >= t.maxLevel) {
                    meta.lore(List.of(
                            Component.text(TextUtil.color("&7" + t.description)),
                            Component.text(""),
                            Component.text(TextUtil.color("&a&lМАКСИМАЛЬНЫЙ УРОВЕНЬ!"))
                    ));
                    meta.setEnchantmentGlintOverride(true);
                } else {
                    meta.lore(List.of(
                            Component.text(TextUtil.color("&7" + t.description)),
                            Component.text(""),
                            Component.text(TextUtil.color("&fЦена прокачки:")),
                            Component.text(TextUtil.color("&8• &e" + costCoins + " ❂")),
                            Component.text(TextUtil.color("&8• &d" + costEp + " EP")),
                            Component.text(""),
                            Component.text(TextUtil.color("&aНажмите, чтобы улучшить!"))
                    ));
                }
            });
            inv.setItem(slot, item);
        }

        player.openInventory(inv);
    }

    private long getCostCoins(int nextLevel) {
        return nextLevel * 10000L;
    }

    private int getCostEp(int nextLevel) {
        return nextLevel * 50;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof TalentsHolder)) return;
        event.setCancelled(true);
        
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();
        
        if (!TALENTS.containsKey(slot)) return;
        
        TalentDef t = TALENTS.get(slot);
        PlayerProfile profile = services.store().profile(player.getUniqueId().toString(), player.getName());
        int level = profile.getTalentLevel(t.id);
        
        if (level >= t.maxLevel) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }
        
        int nextLevel = level + 1;
        long costCoins = getCostCoins(nextLevel);
        int costEp = getCostEp(nextLevel);
        
        if (profile.getCoins() < costCoins || profile.getEventPoints() < costEp) {
            TextUtil.send(player, "&cНедостаточно средств для улучшения!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }
        
        profile.setCoins(profile.getCoins() - costCoins);
        profile.setEventPoints(profile.getEventPoints() - costEp);
        profile.setTalentLevel(t.id, nextLevel);
        services.store().requestSave();
        
        // Update attributes if needed
        if (t.id.equals("gladiator")) {
            if (player.getAttribute(Attribute.MAX_HEALTH) != null) {
                player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(20.0 + (nextLevel * 2.0));
            }
        }
        
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
        TextUtil.send(player, "&aТалант &e" + t.name + " &aповышен до уровня &f" + nextLevel + "&a!");
        open(player); // refresh
    }
}
