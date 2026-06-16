package com.astrasmp.rituals;

import com.astrasmp.AstraSMPPlugin;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class BloodMoonManager implements Listener {

    private final AstraSMPPlugin plugin;
    private boolean isBloodMoonActive = false;

    public BloodMoonManager(AstraSMPPlugin plugin) {
        this.plugin = plugin;
        startTimer();
    }

    public boolean isBloodMoonActive() {
        return isBloodMoonActive;
    }

    private void startTimer() {
        new BukkitRunnable() {
            @Override
            public void run() {
                World world = Bukkit.getWorlds().get(0);
                long time = world.getTime(); // 0 to 24000
                long fullTime = world.getFullTime();
                long days = fullTime / 24000L;

                // Кровавая луна каждую 7 ночь
                boolean isNight = time >= 13000 && time <= 23000;
                boolean is7thDay = (days % 7) == 0;

                if (isNight && is7thDay) {
                    if (!isBloodMoonActive) {
                        isBloodMoonActive = true;
                        Bukkit.getServer().sendMessage(net.kyori.adventure.text.Component.text("§4[!] Взошла Кровавая Луна... Демоны Бездны жаждут плоти!"));
                        for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
                            p.playSound(p.getLocation(), org.bukkit.Sound.AMBIENT_CAVE, 1f, 0.5f);
                        }
                    }

                    // Кровавый дождь
                    if (world.hasStorm()) {
                        for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
                            if (p.getWorld().getEnvironment() != org.bukkit.World.Environment.NORMAL) continue;
                            
                            // Проверка, что игрок под небом и без шлема
                            if (p.getWorld().getHighestBlockYAt(p.getLocation()) <= p.getLocation().getBlockY()) {
                                if (p.getInventory().getHelmet() == null) {
                                    p.damage(1.0); // 0.5 сердца урона
                                    p.sendMessage("§4[!] §cКислотная кровь обжигает вашу кожу!");
                                }
                            }
                        }
                    }

                } else {
                    if (isBloodMoonActive) {
                        isBloodMoonActive = false;
                        Bukkit.getServer().sendMessage(net.kyori.adventure.text.Component.text("§c[!] Кровавая Луна зашла. Силы зла отступают."));
                    }
                }
            }
        }.runTaskTimer(plugin, 40L, 40L); // Раз в 2 секунды
    }

    @EventHandler
    public void onMobSpawn(EntitySpawnEvent event) {
        if (!isBloodMoonActive) return;
        if (!(event.getEntity() instanceof LivingEntity entity)) return;

        EntityType type = event.getEntityType();
        if (type == EntityType.ZOMBIE || type == EntityType.SKELETON || type == EntityType.CREEPER || type == EntityType.SPIDER || type == EntityType.ENDERMAN) {
            entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, false, false));
            entity.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 0, false, false));
            
            // Немного партиклов для стиля
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (entity.isDead() || !isBloodMoonActive) {
                        this.cancel();
                        return;
                    }
                    entity.getWorld().spawnParticle(org.bukkit.Particle.DUST, entity.getLocation().add(0, 1, 0), 2, 0.3, 0.3, 0.3, new org.bukkit.Particle.DustOptions(org.bukkit.Color.RED, 1.0f));
                }
            }.runTaskTimer(plugin, 20L, 20L);
        }
    }
}
