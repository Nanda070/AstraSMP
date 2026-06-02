package com.astrasmp.service;

import com.astrasmp.AstraSMPPlugin;
import com.astrasmp.model.PlayerProfile;
import com.astrasmp.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AfkService {
    private final AstraSMPPlugin plugin;
    private final ServiceManager services;
    private final Map<UUID, Long> lastActivity = new HashMap<>();
    private Location afkLocation;

    public AfkService(AstraSMPPlugin plugin, ServiceManager services) {
        this.plugin = plugin;
        this.services = services;
        loadLocation();
        startAfkTask();
        startParticleTask();
    }

    private void loadLocation() {
        String locStr = plugin.getConfig().getString("locations.afk");
        if (locStr != null && locStr.contains(";")) {
            String[] parts = locStr.split(";");
            if (parts.length >= 4) {
                afkLocation = new Location(Bukkit.getWorld(parts[0]),
                        Double.parseDouble(parts[1]),
                        Double.parseDouble(parts[2]),
                        Double.parseDouble(parts[3]));
            }
        }
    }

    public void updateActivity(Player p) {
        lastActivity.put(p.getUniqueId(), System.currentTimeMillis());
    }

    public void removePlayer(Player p) {
        lastActivity.remove(p.getUniqueId());
    }

    public void teleportToLocation(Player p, String locName) {
        String locStr = plugin.getConfig().getString("locations." + locName);
        if (locStr != null && locStr.contains(";")) {
            String[] parts = locStr.split(";");
            Location loc = new Location(Bukkit.getWorld(parts[0]),
                    Double.parseDouble(parts[1]),
                    Double.parseDouble(parts[2]),
                    Double.parseDouble(parts[3]));
            p.teleport(loc);
        } else {
            TextUtil.send(p, "&cЛокация " + locName + " не настроена в config.yml.");
        }
    }

    private void startAfkTask() {
        // Запускается каждую минуту (1200 тиков)
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (afkLocation == null) return;

            for (Player p : Bukkit.getOnlinePlayers()) {
                // ПРОВЕРКА РАДИУСА: Игрок должен быть в том же мире и в радиусе 5 блоков -> исправили на 15
                if (p.getWorld().equals(afkLocation.getWorld()) && p.getLocation().distance(afkLocation) <= 15.0) {
                    PlayerProfile profile = services.economy().profile(p.getUniqueId(), p.getName());
                    profile.setCoins(profile.getCoins() + 5);
                    TextUtil.send(p, "&e&lAFK &8» &aВам начислено 5 ❂ за нахождение в зоне отдыха.");
                    p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.5f);
                    services.store().requestSave();
                }
            }
        }, 1200L, 1200L);
    }

    private void startParticleTask() {
        // Запускается каждые 10 тиков (0.5 сек) для отрисовки границ
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (afkLocation == null) return;
            double radius = 15.0;
            // Рисуем круг из частиц
            for (double t = 0; t <= 2 * Math.PI; t += Math.PI / 16) {
                double x = radius * Math.cos(t);
                double z = radius * Math.sin(t);
                Location particleLoc = afkLocation.clone().add(x, 0.5, z);
                if (particleLoc.getWorld() != null) {
                    particleLoc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, particleLoc, 1, 0, 0, 0, 0);
                }
            }
        }, 20L, 10L);
    }
}