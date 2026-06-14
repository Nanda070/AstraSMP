package com.astrasmp.blood;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.Bukkit;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BloodTankManager implements Listener {

    private final com.astrasmp.AstraSMPPlugin plugin;
    // Координаты котла -> Количество крови (допустим от 0 до 1000)
    private final Map<Location, Integer> tanks = new ConcurrentHashMap<>();

    public BloodTankManager(com.astrasmp.AstraSMPPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            tanks.putAll(plugin.getDatabase().loadAllBloodTanks());
        }, 20L); // загружаем через секунду, чтобы миры успели загрузиться
    }

    public void addBlood(Location loc, int amount) {
        int current = tanks.getOrDefault(loc, 0);
        int max = 1000;
        int newAmount = Math.min(current + amount, max);
        tanks.put(loc, newAmount);
        saveAsync(loc, newAmount);
    }

    public int getBlood(Location loc) {
        return tanks.getOrDefault(loc, 0);
    }

    public void removeBlood(Location loc, int amount) {
        int current = tanks.getOrDefault(loc, 0);
        int newAmount = Math.max(0, current - amount);
        tanks.put(loc, newAmount);
        saveAsync(loc, newAmount);
    }

    private void saveAsync(Location loc, int amount) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getDatabase().saveBloodTank(loc, amount);
        });
    }

    @EventHandler
    public void onExtractBlood(org.bukkit.event.player.PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        if (block.getType() != Material.CAULDRON && block.getType() != Material.WATER_CAULDRON) return;

        Player player = event.getPlayer();
        org.bukkit.inventory.ItemStack hand = player.getInventory().getItemInMainHand();

        if (hand.getType() == Material.GLASS_BOTTLE) {
            int currentBlood = getBlood(block.getLocation());
            if (currentBlood >= 10) {
                event.setCancelled(true);
                removeBlood(block.getLocation(), 10);
                hand.setAmount(hand.getAmount() - 1);
                
                org.bukkit.inventory.ItemStack drop = com.astrasmp.items.ItemRegistry.bloodDrop();
                if (!player.getInventory().addItem(drop).isEmpty()) {
                    player.getWorld().dropItem(player.getLocation(), drop);
                }
                player.playSound(player.getLocation(), Sound.ITEM_BOTTLE_FILL, 1.0f, 1.0f);
                player.sendMessage("§4[Кровь] §cВы собрали Каплю Крови! (Осталось: " + getBlood(block.getLocation()) + " ед.)");
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        if (killer == null) return;

        Location deathLoc = entity.getLocation();

        // Проверяем котлы в радиусе 10 блоков
        int radius = 10;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block b = deathLoc.clone().add(x, y, z).getBlock();
                    // Проверяем, что это котел
                    if (b.getType() == Material.CAULDRON || b.getType() == Material.WATER_CAULDRON) {
                        // Здесь в идеале нужна проверка, что это именно "Кровавый котел" (сохраненный в БД)
                        // Для примера просто наполняем любой котел кровью (условно)
                        
                        // Добавляем кровь (1 моб = 10 единиц)
                        addBlood(b.getLocation(), 10);

                        // Визуальный эффект всасывания крови
                        drawBloodBeam(deathLoc.clone().add(0, 0.5, 0), b.getLocation().clone().add(0.5, 0.5, 0.5));
                        
                        b.getWorld().playSound(b.getLocation(), Sound.ENTITY_SLIME_SQUISH, 0.5f, 0.5f);
                        return; // Заполняем только один котел
                    }
                }
            }
        }
    }

    private void drawBloodBeam(Location from, Location to) {
        double distance = from.distance(to);
        double step = 0.5;
        int points = (int) (distance / step);
        if (points == 0) return;

        double xStep = (to.getX() - from.getX()) / points;
        double yStep = (to.getY() - from.getY()) / points;
        double zStep = (to.getZ() - from.getZ()) / points;

        Location current = from.clone();
        for (int i = 0; i < points; i++) {
            current.add(xStep, yStep, zStep);
            current.getWorld().spawnParticle(Particle.DUST, current, 3, 0.1, 0.1, 0.1, 0, new Particle.DustOptions(org.bukkit.Color.MAROON, 1.2f));
        }
    }
}
