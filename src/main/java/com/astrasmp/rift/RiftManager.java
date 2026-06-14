package com.astrasmp.rift;

import com.astrasmp.AstraSMPPlugin;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class RiftManager {

    private final AstraSMPPlugin plugin;
    private final List<Location> activeRifts = new CopyOnWriteArrayList<>();

    public RiftManager(AstraSMPPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            activeRifts.addAll(plugin.getDatabase().loadAllRifts());
        }, 20L);
        startCorruptionTask();
    }

    public void createRift(Location loc) {
        activeRifts.add(loc);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getDatabase().saveRift(loc);
        });
        loc.getWorld().playSound(loc, Sound.ENTITY_ENDER_DRAGON_DEATH, 1.0f, 0.5f);
        // Визуальное появление портала
        loc.getWorld().spawnParticle(Particle.PORTAL, loc, 500, 2, 2, 2, 1);
        Bukkit.broadcast(LegacyComponentSerializer.legacySection().deserialize("§4[Аномалия] §cКто-то открыл Врата Бездны! Мир начинает искажаться..."));
    }

    public Location getNearbyRift(Location loc, double radius) {
        for (Location rift : activeRifts) {
            if (rift.getWorld().equals(loc.getWorld()) && rift.distanceSquared(loc) <= radius * radius) {
                return rift;
            }
        }
        return null;
    }

    public void closeRift(Location loc) {
        activeRifts.remove(loc);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getDatabase().deleteRift(loc);
        });
        loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_DEATH, 1.0f, 1.0f);
        loc.getWorld().spawnParticle(Particle.CLOUD, loc, 500, 2, 2, 2, 0.1);
    }

    private void startCorruptionTask() {
        // Раз в 10 секунд разлом заражает 1 случайный блок в радиусе 15
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Location rift : activeRifts) {
                // Рисуем сам портал
                rift.getWorld().spawnParticle(Particle.PORTAL, rift, 50, 1, 1, 1, 0.5);
                rift.getWorld().spawnParticle(Particle.DUST, rift, 20, 1, 1, 1, 0, new Particle.DustOptions(org.bukkit.Color.PURPLE, 2.0f));

                // Искажаем блок
                int rx = (int) (Math.random() * 30 - 15);
                int rz = (int) (Math.random() * 30 - 15);
                int ry = (int) (Math.random() * 10 - 5);
                
                Block b = rift.clone().add(rx, ry, rz).getBlock();
                corruptBlock(b);
            }
        }, 200L, 200L);
    }

    private void corruptBlock(Block b) {
        Material type = b.getType();
        if (type == Material.GRASS_BLOCK || type == Material.DIRT) {
            b.setType(Material.CRIMSON_NYLIUM);
        } else if (type == Material.WATER) {
            b.setType(Material.LAVA);
        } else if (type == Material.STONE || type == Material.COBBLESTONE) {
            b.setType(Material.NETHERRACK);
        } else if (type == Material.OAK_LOG || type == Material.BIRCH_LOG || type == Material.SPRUCE_LOG) {
            b.setType(Material.CRIMSON_STEM);
        } else if (type == Material.OAK_LEAVES || type == Material.BIRCH_LEAVES || type == Material.SPRUCE_LEAVES) {
            b.setType(Material.NETHER_WART_BLOCK);
        }
    }
}
