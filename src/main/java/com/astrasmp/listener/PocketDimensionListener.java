package com.astrasmp.listener;

import com.astrasmp.service.ServiceManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.UUID;

public class PocketDimensionListener implements Listener {

    private final ServiceManager services;

    public PocketDimensionListener(ServiceManager services) {
        this.services = services;
    }

    private boolean isOutOfBounds(Location loc, UUID playerUuid) {
        if (!loc.getWorld().getName().equals("astrasmp_pockets")) return false;
        
        // В карманном мире каждый игрок привязан к СВОЕМУ острову
        // Если игрок в гостях, мы должны найти чье это измерение.
        // Для упрощения: мы можем проверять, к какому центру игрок ближе всего.
        // Но так как островки на расстоянии 10000 блоков друг от друга, 
        // игрок физически не сможет дойти до чужого без телепорта.
        // Поэтому нам достаточно вычислить "владельца" по координатам.
        
        int x = loc.getBlockX();
        int z = loc.getBlockZ();
        
        // Находим координаты центра этого острова (округляем до 10000)
        int centerX = Math.round((float) x / 10000) * 10000;
        int centerZ = Math.round((float) z / 10000) * 10000;
        
        Location center = new Location(loc.getWorld(), centerX + 8.5, 65, centerZ + 8.5);
        
        // 16 блоков от центра (квадрат)
        return Math.abs(loc.getX() - center.getX()) > 16 || Math.abs(loc.getZ() - center.getZ()) > 16;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();
        if (to == null || !to.getWorld().getName().equals("astrasmp_pockets")) return;

        if (to.getY() < -64) {
            // Упал в бездну
            player.sendMessage("§cВы упали в Бездну.");
            services.pockets().leavePocket(player);
            return;
        }

        if (isOutOfBounds(to, player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage("§cСтена пустоты не дает вам пройти дальше.");
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (isOutOfBounds(event.getBlock().getLocation(), event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cВы не можете ломать блоки за пределами острова.");
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isOutOfBounds(event.getBlock().getLocation(), event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cВы не можете ставить блоки за пределами острова.");
        }
    }

    @EventHandler
    public void onWorldChange(org.bukkit.event.player.PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (player.getWorld().getName().equals("astrasmp_pockets")) {
            Location loc = player.getLocation();
            int centerX = Math.round((float) loc.getBlockX() / 10000) * 10000;
            int centerZ = Math.round((float) loc.getBlockZ() / 10000) * 10000;
            Location center = new Location(player.getWorld(), centerX + 8.5, 65, centerZ + 8.5);
            
            org.bukkit.WorldBorder border = org.bukkit.Bukkit.createWorldBorder();
            border.setCenter(center);
            border.setSize(32.0);
            player.setWorldBorder(border);
        } else if (event.getFrom().getName().equals("astrasmp_pockets")) {
            player.setWorldBorder(null);
        }
    }

    @EventHandler
    public void onJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.getWorld().getName().equals("astrasmp_pockets")) {
            Location loc = player.getLocation();
            int centerX = Math.round((float) loc.getBlockX() / 10000) * 10000;
            int centerZ = Math.round((float) loc.getBlockZ() / 10000) * 10000;
            Location center = new Location(player.getWorld(), centerX + 8.5, 65, centerZ + 8.5);
            
            org.bukkit.WorldBorder border = org.bukkit.Bukkit.createWorldBorder();
            border.setCenter(center);
            border.setSize(32.0);
            player.setWorldBorder(border);
        }
    }
}
