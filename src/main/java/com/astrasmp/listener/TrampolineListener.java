package com.astrasmp.listener;

import com.astrasmp.AstraSMPPlugin;
import com.astrasmp.items.ItemRegistry;
import com.astrasmp.service.DataStore;
import com.astrasmp.util.LocationKey;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Iterator;

public final class TrampolineListener implements Listener {
    private final DataStore store;

    public TrampolineListener(AstraSMPPlugin plugin, DataStore store) {
        this.store = store;
    }

    private boolean isTrampoline(Block block) {
        if (block.getType() != Material.SLIME_BLOCK) return false;
        LocationKey key = LocationKey.fromLocation(block.getLocation());
        return key != null && store.getTrampolineBlocks().contains(key);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (ItemRegistry.is(item, "trampoline")) {
            LocationKey key = LocationKey.fromLocation(event.getBlockPlaced().getLocation());
            if (key != null) {
                store.getTrampolineBlocks().add(key);
                store.requestSave();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (isTrampoline(block)) {
            LocationKey key = LocationKey.fromLocation(block.getLocation());
            store.getTrampolineBlocks().remove(key);
            store.requestSave();
            
            event.setDropItems(false);
            block.getWorld().dropItemNaturally(block.getLocation(), ItemRegistry.trampoline());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        Iterator<Block> it = event.blockList().iterator();
        boolean changed = false;
        while (it.hasNext()) {
            Block block = it.next();
            if (isTrampoline(block)) {
                LocationKey key = LocationKey.fromLocation(block.getLocation());
                store.getTrampolineBlocks().remove(key);
                changed = true;
                
                block.getWorld().dropItemNaturally(block.getLocation(), ItemRegistry.trampoline());
                it.remove();
                block.setType(Material.AIR);
            }
        }
        if (changed) {
            store.requestSave();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            if (isTrampoline(block)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block block : event.getBlocks()) {
            if (isTrampoline(block)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        if (to == null) return;
        
        Player player = event.getPlayer();
        Location locUnder = to.clone().subtract(0, 0.1, 0);
        Block blockUnder = locUnder.getBlock();
        
        // Проверяем, наступил ли игрок на батут
        if (isTrampoline(blockUnder) && !player.isSneaking()) {
            Vector vel = player.getVelocity();
            // Подбрасываем высоко вверх при любом наступании (если игрок уже не летит высоко)
            if (vel.getY() < 1.0) {
                player.setVelocity(new Vector(vel.getX(), 2.5, vel.getZ()));
                player.playSound(player.getLocation(), Sound.ENTITY_SLIME_SQUISH, 1.0f, 0.5f);
                player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.5f, 1.0f);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            // Проверяем блок прямо под ногами (Y-1) и чуть ниже для надёжности
            Location loc = event.getEntity().getLocation();
            Block blockAt = loc.getBlock();
            Block blockBelow = loc.clone().subtract(0, 1.0, 0).getBlock();
            if (isTrampoline(blockAt) || isTrampoline(blockBelow)) {
                event.setCancelled(true);
            }
        }
    }
}
