package com.astrasmp.service;

import com.astrasmp.AstraSMPPlugin;
import com.astrasmp.util.TextUtil;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.EulerAngle;


import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SitLayService implements Listener {

    private final AstraSMPPlugin plugin;
    private final Map<UUID, ArmorStand> seatedPlayers = new HashMap<>();
    private final Map<UUID, ArmorStand> layingPlayers = new HashMap<>();

    public SitLayService(AstraSMPPlugin plugin) {
        this.plugin = plugin;
    }

    public void shutdown() {
        for (ArmorStand stand : seatedPlayers.values()) {
            if (stand.isValid()) stand.remove();
        }
        for (ArmorStand stand : layingPlayers.values()) {
            if (stand.isValid()) stand.remove();
        }
        seatedPlayers.clear();
        layingPlayers.clear();
    }

    public boolean toggleSit(Player player) {
        if (seatedPlayers.containsKey(player.getUniqueId())) {
            unsit(player);
            return false; // stopped sitting
        } else {
            return sit(player, player.getLocation());
        }
    }

    public boolean toggleLay(Player player) {
        if (layingPlayers.containsKey(player.getUniqueId())) {
            unlay(player);
            return false; // stopped laying
        } else {
            return lay(player, player.getLocation());
        }
    }

    public boolean sit(Player player, Location loc) {
        if (player.isInsideVehicle()) {
            TextUtil.send(player, plugin.getConfigManager().getMessage("sit-blocked-vehicle", "&cНевозможно сесть, находясь в транспорте."));
            return false;
        }
        if (!player.getPassengers().isEmpty()) {
            TextUtil.send(player, plugin.getConfigManager().getMessage("sit-blocked-passengers", "&cНевозможно сесть с пассажирами."));
            return false;
        }
        if (!((org.bukkit.entity.Entity)player).isOnGround() && loc.getBlock().getType().isAir() && loc.clone().subtract(0, 1, 0).getBlock().getType().isAir()) {
            TextUtil.send(player, plugin.getConfigManager().getMessage("sit-not-on-ground", "&cНеобходимо стоять на твердом блоке."));
            return false;
        }

        // ArmorStand marker: player visual appears ~1.8 blocks above spawn point.
        // Subtract 1.0 so the player sits on the surface they're standing on.
        Location seatLoc = loc.clone().subtract(0, 1.0, 0);
        ArmorStand stand = seatLoc.getWorld().spawn(seatLoc, ArmorStand.class, s -> {
            s.setVisible(false);
            s.setGravity(false);
            s.setMarker(true);
            s.setInvulnerable(true);
        });

        stand.addPassenger(player);
        seatedPlayers.put(player.getUniqueId(), stand);
        TextUtil.send(player, plugin.getConfigManager().getMessage("sit-enabled", "&aВы сели."));
        return true;
    }

    public void unsit(Player player) {
        ArmorStand stand = seatedPlayers.remove(player.getUniqueId());
        if (stand != null) {
            stand.eject();
            stand.remove();
            TextUtil.send(player, plugin.getConfigManager().getMessage("sit-disabled", "&eСидение отключено."));
        }
    }

    public boolean lay(Player player, Location loc) {
        if (player.isInsideVehicle()) {
            TextUtil.send(player, plugin.getConfigManager().getMessage("lay-blocked-vehicle", "&cНевозможно лечь, находясь в транспорте."));
            return false;
        }
        if (!player.getPassengers().isEmpty()) {
            TextUtil.send(player, plugin.getConfigManager().getMessage("lay-blocked-passengers", "&cНевозможно лечь с пассажирами."));
            return false;
        }
        if (!((org.bukkit.entity.Entity)player).isOnGround() && loc.getBlock().getType().isAir() && loc.clone().subtract(0, 1, 0).getBlock().getType().isAir()) {
            TextUtil.send(player, plugin.getConfigManager().getMessage("lay-not-on-ground", "&cНеобходимо стоять на твердом блоке."));
            return false;
        }

        // Same offset logic as sit: subtract 1.0 so player lies on the surface.
        Location layLoc = loc.clone().subtract(0, 1.0, 0);
        ArmorStand stand = layLoc.getWorld().spawn(layLoc, ArmorStand.class, s -> {
            s.setVisible(false);
            s.setGravity(false);
            s.setMarker(true);
            s.setInvulnerable(true);
            s.setHeadPose(new EulerAngle(Math.toRadians(90), 0, 0)); // Flat pose
        });

        stand.addPassenger(player);
        layingPlayers.put(player.getUniqueId(), stand);
        TextUtil.send(player, plugin.getConfigManager().getMessage("lay-enabled", "&aВы легли."));
        return true;
    }

    public void unlay(Player player) {
        ArmorStand stand = layingPlayers.remove(player.getUniqueId());
        if (stand != null) {
            stand.eject();
            stand.remove();
            TextUtil.send(player, plugin.getConfigManager().getMessage("lay-disabled", "&eЛежание отключено."));
        }
    }

    @EventHandler
    public void onDismount(EntityDismountEvent event) {
        if (event.getEntity() instanceof Player player) {
            Entity vehicle = event.getDismounted();
            if (seatedPlayers.containsValue(vehicle)) {
                seatedPlayers.remove(player.getUniqueId());
                vehicle.remove();
            } else if (layingPlayers.containsValue(vehicle)) {
                layingPlayers.remove(player.getUniqueId());
                vehicle.remove();
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        unsit(event.getPlayer());
        unlay(event.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        unsit(event.getEntity());
        unlay(event.getEntity());
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        unsit(event.getPlayer());
        unlay(event.getPlayer());
    }

    // Auto-sit on stairs
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            Player player = event.getPlayer();
            if (block != null && block.getType().name().contains("STAIRS") && !player.isSneaking()) {
                if (player.getInventory().getItemInMainHand().getType().isAir()) {
                    if (!seatedPlayers.containsKey(player.getUniqueId())) {
                        // Place the ArmorStand at the top surface of the stair block (block.Y + 1.0),
                        // centered on X/Z. sit() will subtract 1.0 so the stand ends up at block.Y,
                        // which puts the player visually sitting on top of the stair.
                        Location center = block.getLocation().add(0.5, 1.0, 0.5);
                        center.setYaw(player.getLocation().getYaw() + 180f);
                        sit(player, center);
                        event.setCancelled(true);
                    }
                }
            }
        }
    }
}
