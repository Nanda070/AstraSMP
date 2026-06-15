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
        
        // 16 блоков от центра
        return loc.distance(center) > 16;
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
}
