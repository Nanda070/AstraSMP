package com.astrasmp.model;

import com.astrasmp.AstraSMPPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.UUID;

public class CorruptionManager {

    private final AstraSMPPlugin plugin;

    public CorruptionManager(AstraSMPPlugin plugin) {
        this.plugin = plugin;
        startPassiveEffectsTask();
    }

    public int getCorruption(UUID uuid) {
        return plugin.getServices().store().profile(uuid.toString(), null).getCorruption();
    }

    public void addCorruption(UUID uuid, int amount) {
        setCorruption(uuid, getCorruption(uuid) + amount);
    }

    public void setCorruption(UUID uuid, int amount) {
        plugin.getServices().store().profile(uuid.toString(), null).setCorruption(amount);
        plugin.getServices().store().requestSave();
    }

    private void startPassiveEffectsTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                int corr = getCorruption(p.getUniqueId());

                // Низкая скверна: частицы крови вокруг
                if (corr >= 50 && p.isSprinting()) {
                    p.getWorld().spawnParticle(Particle.DUST, p.getLocation().add(0, 1, 0), 2, 0.3, 0.3, 0.3, 0, new Particle.DustOptions(org.bukkit.Color.RED, 1.0f));
                }

                // Средняя скверна: пассивное ночное зрение
                if (corr >= 200) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 300, 0, false, false, false));
                }

                // Высокая скверна
                if (corr >= 500) {
                    // Можно дать пассивное сопротивление или силу
                    p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 300, 0, false, false, false));
                }
            }
        }, 100L, 100L); // Раз в 5 секунд
    }
}
