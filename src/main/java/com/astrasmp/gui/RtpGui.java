package com.astrasmp.gui;

import com.astrasmp.AstraSMPPlugin;
import com.astrasmp.service.ServiceManager;
import com.astrasmp.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Random;

public class RtpGui implements Listener {
    private final ServiceManager services;
    private final Random random = new Random();

    public RtpGui(AstraSMPPlugin plugin, ServiceManager services) {
        this.services = services;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public record RtpHolder() implements InventoryHolder {
        @Override
        public @NotNull Inventory getInventory() {
            return Bukkit.createInventory(null, 9);
        }
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(new RtpHolder(), 27, Component.text(TextUtil.color("&8Случайная Телепортация")));
        
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        filler.editMeta(meta -> meta.displayName(Component.empty()));
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);

        ItemStack rtpItem = new ItemStack(Material.ENDER_PEARL);
        rtpItem.editMeta(meta -> {
            meta.displayName(Component.text(TextUtil.color("&d&lТелепортироваться (RTP)")));
            meta.lore(List.of(
                    Component.text(TextUtil.color("&7Нажмите, чтобы переместиться")),
                    Component.text(TextUtil.color("&7в случайную безопасную точку мира."))
            ));
        });
        inv.setItem(13, rtpItem);

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 1f, 1f);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof RtpHolder)) return;
        event.setCancelled(true);
        
        Player player = (Player) event.getWhoClicked();
        if (event.getSlot() == 13) {
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
            net.kyori.adventure.title.Title.Times times = net.kyori.adventure.title.Title.Times.times(java.time.Duration.ofMillis(500), java.time.Duration.ofMillis(2000), java.time.Duration.ofMillis(500));
            player.showTitle(net.kyori.adventure.title.Title.title(Component.text(TextUtil.color("&dПоиск места...")), Component.text(TextUtil.color("&7Подождите немного")), times));
            
            Bukkit.getScheduler().runTaskLater(AstraSMPPlugin.getInstance(), () -> {
                int radius = 1000;
                Location loc = null;
                int maxAttempts = 10;

                for (int i = 0; i < maxAttempts; i++) {
                    int x = random.nextInt(radius * 2) - radius;
                    int z = random.nextInt(radius * 2) - radius;
                    loc = player.getWorld().getHighestBlockAt(x, z).getLocation().add(0.5, 1, 0.5);

                    Material type = loc.getBlock().getRelative(0, -1, 0).getType();
                    if (type != Material.WATER && type != Material.LAVA) {
                        break;
                    }
                    if (i == maxAttempts - 1) {
                        TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_3a1628", "&cНе удалось найти безопасную локацию, попробуйте еще раз."));
                        return;
                    }
                }

                player.teleport(loc);
                player.spawnParticle(org.bukkit.Particle.PORTAL, player.getLocation(), 50, 0.5, 1, 0.5, 0.1);
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.2f);
                TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_d1d176", "&bВы успешно телепортированы!"));

                services.quests().processAction(player, com.astrasmp.service.QuestManager.QuestAction.USE_RTP, "", 1);
            }, 20L);
        }
    }
}
