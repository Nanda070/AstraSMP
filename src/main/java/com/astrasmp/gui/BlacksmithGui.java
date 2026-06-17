package com.astrasmp.gui;

import com.astrasmp.items.ItemRegistry;
import com.astrasmp.model.PlayerProfile;
import com.astrasmp.service.ServiceManager;
import com.astrasmp.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BlacksmithGui implements Listener {

    public record BlacksmithHolder() implements InventoryHolder {
        @Override
        public @NotNull Inventory getInventory() {
            return Bukkit.createInventory(null, 9);
        }
    }

    private final ServiceManager services;

    public BlacksmithGui(ServiceManager services) {
        this.services = services;
    }

    private Component title(String text) {
        return Component.text(TextUtil.color("&8" + text));
    }

    private ItemStack button(Material material, String name, String... lore) {
        ItemStack stack = new ItemStack(material);
        stack.editMeta(meta -> {
            meta.displayName(Component.text(TextUtil.color(name)));
            if (lore != null && lore.length > 0) {
                meta.lore(Arrays.stream(lore).map(line -> Component.text(TextUtil.color(line))).toList());
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        });
        return stack;
    }

    private void fill(Inventory inv) {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        filler.editMeta(meta -> meta.displayName(Component.empty()));
        for (int i = 0; i < inv.getSize(); i++) {
            if (i != 13) {
                inv.setItem(i, filler);
            }
        }
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(new BlacksmithHolder(), 45, title("Кузница Артефактов"));
        fill(inv);
        
        updateButton(inv, null); // Set default anvil state

        player.openInventory(inv);
    }

    private void updateButton(Inventory inv, ItemStack target) {
        if (target == null || target.getType().isAir() || ItemRegistry.id(target) == null || !isUpgradable(ItemRegistry.id(target))) {
            inv.setItem(31, button(Material.ANVIL, "&cКузница", "&7Положите артефакт в свободный", "&7слот, чтобы узнать стоимость", "&7и шанс улучшения."));
            return;
        }

        int currentLevel = ItemRegistry.getUpgradeLevel(target);
        if (currentLevel >= 5) {
            inv.setItem(31, button(Material.BARRIER, "&cМаксимальный уровень", "&7Этот предмет больше нельзя улучшить!"));
            return;
        }

        int nextLevel = currentLevel + 1;
        long coinsCost = nextLevel * 1000L;
        int epCost = nextLevel * 50;
        int diamondCost = nextLevel;
        int chance = 100 - (currentLevel * 15); // 100, 85, 70, 55, 40%
        if (chance < 20) chance = 20;

        List<String> lore = new ArrayList<>();
        lore.add("&7Улучшение: &a[+" + currentLevel + "] &7-> &a[+" + nextLevel + "]");
        lore.add("");
        lore.add("&fСтоимость:");
        lore.add("&8- &e" + coinsCost + " ❂");
        lore.add("&8- &b" + epCost + " EP");
        lore.add("&8- &b" + diamondCost + " Алмазов");
        lore.add("");
        lore.add("&fШанс успеха: &a" + chance + "%");
        lore.add("&cПри неудаче предмет останется, но ресурсы сгорят!");
        lore.add("");
        lore.add("&eНажмите, чтобы улучшить!");

        inv.setItem(31, button(Material.ANVIL, "&aУлучшить артефакт", lore.toArray(new String[0])));
    }

    private boolean isUpgradable(String id) {
        if (id == null) return false;
        return switch (id) {
            case "shadowBlade", "thunderHammer", "vampireDagger", "infernoSword", "frostAxe", "venomBow", "reaperScythe" -> true;
            default -> false;
        };
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof BlacksmithHolder)) return;

        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        if (slot == 13) {
            // Let the player put/take items from slot 13
            Bukkit.getScheduler().runTaskLater(services.plugin(), () -> {
                updateButton(event.getInventory(), event.getInventory().getItem(13));
            }, 1L);
            return;
        }

        if (slot >= 0 && slot < 45) {
            event.setCancelled(true);
        }

        if (slot == 31) {
            ItemStack target = event.getInventory().getItem(13);
            if (target == null || target.getType().isAir() || ItemRegistry.id(target) == null || !isUpgradable(ItemRegistry.id(target))) {
                return;
            }

            int currentLevel = ItemRegistry.getUpgradeLevel(target);
            if (currentLevel >= 5) {
                TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_279ba6", "&cЭтот артефакт уже максимального уровня!"));
                return;
            }

            int nextLevel = currentLevel + 1;
            long coinsCost = nextLevel * 1000L;
            int epCost = nextLevel * 50;
            int diamondCost = nextLevel;
            int chance = 100 - (currentLevel * 15);
            if (chance < 20) chance = 20;

            PlayerProfile profile = services.economy().profile(player.getUniqueId(), player.getName());

            if (profile.getCoins() < coinsCost) {
                TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_7c7c2a", "&cНедостаточно монет!"));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                return;
            }

            if (profile.getEventPoints() < epCost) {
                TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_e52dd5", "&cНедостаточно очков ивента (EP)!"));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                return;
            }

            if (!hasItems(player, Material.DIAMOND, diamondCost)) {
                TextUtil.send(player, "&cУ вас нет " + diamondCost + " алмазов!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                return;
            }

            // Pay costs
            profile.setCoins(profile.getCoins() - coinsCost);
            profile.setEventPoints(profile.getEventPoints() - epCost);
            removeItems(player, Material.DIAMOND, diamondCost);

            // Roll chance
            if (Math.random() * 100 <= chance) {
                // Success
                ItemRegistry.setUpgradeLevel(target, nextLevel);
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1, 1);
                TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_d34df0", "&aУлучшение прошло успешно! Ваше оружие стало сильнее."));
                updateButton(event.getInventory(), target);
            } else {
                // Fail
                player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1, 1);
                TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_9c101c", "&cЗаточка не удалась... Вы потеряли ресурсы, но артефакт цел."));
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof BlacksmithHolder)) return;
        
        ItemStack target = event.getInventory().getItem(13);
        if (target != null && !target.getType().isAir()) {
            event.getPlayer().getInventory().addItem(target).values().forEach(
                item -> event.getPlayer().getWorld().dropItem(event.getPlayer().getLocation(), item)
            );
        }
    }

    private boolean hasItems(Player player, Material mat, int amount) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == mat) {
                count += item.getAmount();
            }
        }
        return count >= amount;
    }

    private void removeItems(Player player, Material mat, int amount) {
        int left = amount;
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.getType() == mat) {
                if (item.getAmount() > left) {
                    item.setAmount(item.getAmount() - left);
                    break;
                } else {
                    left -= item.getAmount();
                    player.getInventory().setItem(i, null);
                    if (left <= 0) break;
                }
            }
        }
    }
}
