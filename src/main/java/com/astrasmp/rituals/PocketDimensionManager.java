package com.astrasmp.rituals;

import com.astrasmp.AstraSMPPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PocketDimensionManager {

    private final AstraSMPPlugin plugin;
    private final Map<UUID, Location> returnLocations = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> invites = new ConcurrentHashMap<>();

    public PocketDimensionManager(AstraSMPPlugin plugin) {
        this.plugin = plugin;
    }

    public Location getCenterLocation(UUID playerUuid) {
        World world = Bukkit.getWorld("astrasmp_pockets");
        if (world == null) return null;

        int hash = Math.abs(playerUuid.hashCode());
        int x = (hash % 1000) * 10000;
        int z = ((hash / 1000) % 1000) * 10000;

        return new Location(world, x + 8.5, 65, z + 8.5);
    }

    public void generatePlatform(Location center) {
        World world = center.getWorld();
        int cx = center.getBlockX();
        int cz = center.getBlockZ();
        int cy = 64;

        if (world.getBlockAt(cx, cy, cz).getType() == Material.AIR) {
            for (int x = -3; x <= 3; x++) {
                for (int z = -3; z <= 3; z++) {
                    world.getBlockAt(cx + x, cy, cz + z).setType(Material.CRYING_OBSIDIAN);
                }
            }
        }
    }

    public boolean isInsidePocket(Location loc, UUID ownerUuid) {
        if (!loc.getWorld().getName().equals("astrasmp_pockets")) return false;
        Location center = getCenterLocation(ownerUuid);
        if (center == null) return false;

        // Расстояние не больше 16 блоков (квадратная зона под WorldBorder 32х32)
        return Math.abs(loc.getX() - center.getX()) <= 16 && Math.abs(loc.getZ() - center.getZ()) <= 16;
    }

    public void enterPocket(Player player, UUID ownerUuid) {
        Location center = getCenterLocation(ownerUuid);
        if (center == null) {
            player.sendMessage("§cМир карманных измерений не загружен.");
            return;
        }

        generatePlatform(center);
        returnLocations.put(player.getUniqueId(), player.getLocation());
        player.teleport(center);
        
        org.bukkit.WorldBorder border = Bukkit.createWorldBorder();
        border.setCenter(center);
        border.setSize(32.0);
        player.setWorldBorder(border);
        
        player.sendMessage("§5[Бездна] §dВы вошли в карманное измерение.");
    }

    public void leavePocket(Player player) {
        if (!player.getWorld().getName().equals("astrasmp_pockets")) {
            player.sendMessage("§cВы не находитесь в карманном измерении.");
            return;
        }

        Location returnLoc = returnLocations.remove(player.getUniqueId());
        if (returnLoc != null) {
            player.teleport(returnLoc);
        } else {
            plugin.getServices().afk().teleportToLocation(player, "spawn");
        }
        
        player.setWorldBorder(null);
        player.sendMessage("§5[Бездна] §dВы покинули карманное измерение.");
    }

    public void invitePlayer(Player owner, Player target) {
        invites.computeIfAbsent(owner.getUniqueId(), k -> new HashSet<>()).add(target.getUniqueId());
        owner.sendMessage("§5[Бездна] §dВы пригласили " + target.getName() + " в свое измерение.");
        target.sendMessage("§5[Бездна] §dИгрок " + owner.getName() + " пригласил вас в свое измерение. Напишите §f/prunus join " + owner.getName() + "§d для входа.");
    }

    public boolean hasInvite(UUID owner, UUID guest) {
        return invites.getOrDefault(owner, Collections.emptySet()).contains(guest);
    }
}
