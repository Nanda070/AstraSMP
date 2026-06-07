package com.astrasmp.gui;

import com.astrasmp.AstraSMPPlugin;
import com.astrasmp.model.ContractRecord;
import com.astrasmp.service.ServiceManager;
import com.astrasmp.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class BountiesGui implements Listener {
    private final ServiceManager services;

    public BountiesGui(AstraSMPPlugin plugin, ServiceManager services) {
        this.services = services;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public record BountiesHolder() implements InventoryHolder {
        @Override
        public @NotNull Inventory getInventory() {
            return Bukkit.createInventory(null, 9);
        }
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(new BountiesHolder(), 54, Component.text(TextUtil.color("&8Охота за головами")));
        
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        filler.editMeta(meta -> meta.displayName(Component.empty()));
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);

        int slot = 0;
        for (ContractRecord c : services.contracts().active()) {
            if (!"BOUNTY".equalsIgnoreCase(c.getType())) continue;
            if (slot >= 45) break;

            try {
                UUID targetUuid = UUID.fromString(c.getTargetUuid());
                OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);
                
                UUID creatorUuid = UUID.fromString(c.getCreatorUuid());
                OfflinePlayer creator = Bukkit.getOfflinePlayer(creatorUuid);

                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                head.editMeta(SkullMeta.class, meta -> {
                    meta.setOwningPlayer(target);
                    meta.displayName(Component.text(TextUtil.color("&c&lОхота: &f" + (target.getName() != null ? target.getName() : "Неизвестный"))));
                    meta.lore(List.of(
                            Component.text(TextUtil.color("&7Цель: &c" + target.getName())),
                            Component.text(TextUtil.color("&7Заказчик: &e" + (creator.getName() != null ? creator.getName() : "Аноним"))),
                            Component.text(""),
                            Component.text(TextUtil.color("&7Награда: &a&l" + c.getReward() + " ❂")),
                            Component.text(TextUtil.color("&8Убейте эту цель, чтобы получить награду!"))
                    ));
                });
                inv.setItem(slot++, head);
            } catch (Exception ignored) {}
        }

        ItemStack info = new ItemStack(Material.BOOK);
        info.editMeta(meta -> {
            meta.displayName(Component.text(TextUtil.color("&eИнформация")));
            meta.lore(List.of(
                    Component.text(TextUtil.color("&7Чтобы объявить охоту на игрока,")),
                    Component.text(TextUtil.color("&7используйте команду:")),
                    Component.text(TextUtil.color("&a/bounty <ник> <сумма>"))
            ));
        });
        inv.setItem(48, info);

        ItemStack back = new ItemStack(Material.ARROW);
        back.editMeta(meta -> {
            meta.displayName(Component.text(TextUtil.color("&cНазад в меню")));
        });
        inv.setItem(49, back);

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 1f, 1f);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof BountiesHolder)) return;
        event.setCancelled(true);
        
        Player player = (Player) event.getWhoClicked();
        if (event.getSlot() == 49) {
            player.performCommand("menu");
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
        }
    }
}
