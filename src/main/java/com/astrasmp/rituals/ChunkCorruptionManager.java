package com.astrasmp.rituals;

import com.astrasmp.AstraSMPPlugin;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class ChunkCorruptionManager implements Listener {

    private final AstraSMPPlugin plugin;
    private final NamespacedKey CORRUPTION_KEY;
    private final Random random = new Random();

    public ChunkCorruptionManager(AstraSMPPlugin plugin) {
        this.plugin = plugin;
        this.CORRUPTION_KEY = new NamespacedKey(plugin, "astrasmp_corruption");
    }

    public int getCorruption(Chunk chunk) {
        PersistentDataContainer data = chunk.getPersistentDataContainer();
        return data.getOrDefault(CORRUPTION_KEY, PersistentDataType.INTEGER, 0);
    }

    public void setCorruption(Chunk chunk, int amount) {
        PersistentDataContainer data = chunk.getPersistentDataContainer();
        if (amount <= 0) {
            data.remove(CORRUPTION_KEY);
        } else {
            data.set(CORRUPTION_KEY, PersistentDataType.INTEGER, Math.min(amount, 100)); // Максимум 100
        }
    }

    public void addCorruption(Chunk chunk, int amount) {
        int current = getCorruption(chunk);
        int newAmount = current + amount;
        setCorruption(chunk, newAmount);

        if (newAmount >= 50) {
            corruptChunkBlocks(chunk);
        }
    }

    private void corruptChunkBlocks(Chunk chunk) {
        // Мы запускаем замену блоков асинхронно или постепенно, чтобы не вешать сервер
        // Для 1.21 лучше использовать синхронный таск, но разбив на части или просто быстро пробежать по поверхности
        new BukkitRunnable() {
            @Override
            public void run() {
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        if (random.nextInt(100) < 30) { // 30% шанс заразить столбец
                            int highestY = chunk.getWorld().getHighestBlockYAt(chunk.getX() * 16 + x, chunk.getZ() * 16 + z);
                            Block block = chunk.getBlock(x, highestY, z);
                            if (block.getType() == Material.GRASS_BLOCK || block.getType() == Material.DIRT) {
                                block.setType(Material.CRIMSON_NYLIUM);
                            } else if (block.getType() == Material.STONE) {
                                block.setType(Material.NETHERRACK);
                            } else if (block.getType() == Material.OAK_LOG || block.getType() == Material.BIRCH_LOG || block.getType() == Material.SPRUCE_LOG) {
                                block.setType(Material.CRIMSON_STEM);
                            } else if (block.getType() == Material.OAK_LEAVES || block.getType() == Material.BIRCH_LEAVES) {
                                block.setType(Material.NETHER_WART_BLOCK);
                            }
                        }
                    }
                }
            }
        }.runTask(plugin);
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;
        
        Chunk chunk = event.getLocation().getChunk();
        if (getCorruption(chunk) >= 50) {
            
            boolean hasDruid = false;
            for (org.bukkit.entity.Player p : event.getLocation().getWorld().getNearbyEntitiesByType(org.bukkit.entity.Player.class, event.getLocation(), 16)) {
                org.bukkit.persistence.PersistentDataContainer pdc = p.getPersistentDataContainer();
                org.bukkit.NamespacedKey classKey = new org.bukkit.NamespacedKey("astraop", "class_id");
                if ("druid".equalsIgnoreCase(pdc.get(classKey, org.bukkit.persistence.PersistentDataType.STRING))) {
                    hasDruid = true;
                    break;
                }
            }
            
            if (hasDruid) return; // Присутствие Друида очищает ауру, предотвращая мутации

            EntityType type = event.getEntityType();
            Location loc = event.getLocation();
            
            if (type == EntityType.PIG || type == EntityType.COW || type == EntityType.SHEEP || type == EntityType.CHICKEN) {
                event.setCancelled(true);
                
                // Мутация
                EntityType newType = type == EntityType.PIG ? EntityType.ZOGLIN :
                                     type == EntityType.COW ? EntityType.RAVAGER :
                                     type == EntityType.SHEEP ? EntityType.HOGLIN :
                                     EntityType.ZOMBIE;
                                     
                loc.getWorld().spawnEntity(loc, newType);
                loc.getWorld().spawnParticle(org.bukkit.Particle.SCULK_SOUL, loc, 30, 0.5, 0.5, 0.5, 0.1);
            }
        }
    }
}
