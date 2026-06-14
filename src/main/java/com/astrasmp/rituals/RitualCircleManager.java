package com.astrasmp.rituals;

import com.astrasmp.AstraSMPPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;

public class RitualCircleManager {

    /**
     * Возвращает уровень круга (1, 2, 3) или 0, если круг не собран.
     */
    public int getCircleTier(Location center) {
        if (checkTier3(center)) return 3;
        if (checkTier2(center)) return 2;
        if (checkTier1(center)) return 1;
        return 0;
    }

    private boolean checkTier1(Location center) {
        // Проверяем редстоун крестом
        int[][] cross = {{1,0}, {-1,0}, {0,1}, {0,-1}};
        for (int[] c : cross) {
            Block b = center.clone().add(c[0], 0, c[1]).getBlock();
            if (b.getType() != Material.REDSTONE_WIRE) return false;
        }

        // Проверяем 4 свечи по углам
        int[][] corners = {{1,1}, {-1,1}, {1,-1}, {-1,-1}};
        int candleCount = 0;
        for (int[] c : corners) {
            Block b = center.clone().add(c[0], 0, c[1]).getBlock();
            if (b.getType().name().contains("CANDLE")) {
                candleCount++;
            }
        }
        return candleCount == 4;
    }

    private boolean checkTier2(Location center) {
        // Проверяем рамку 5х5 из редстоуна (от -2 до 2)
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (Math.abs(x) == 2 || Math.abs(z) == 2) {
                    // Это периметр
                    Block b = center.clone().add(x, 0, z).getBlock();
                    // Углы - черепа
                    if (Math.abs(x) == 2 && Math.abs(z) == 2) {
                        if (b.getType() != Material.WITHER_SKELETON_SKULL && b.getType() != Material.WITHER_SKELETON_WALL_SKULL) {
                            return false;
                        }
                    } else {
                        if (b.getType() != Material.REDSTONE_WIRE) return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean checkTier3(Location center) {
        // Для примера: 5х5 как во втором тире, но еще нужны блоки незерита по внутренним диагоналям и огонь душ.
        if (!checkTier2(center)) return false;

        int[][] innerCorners = {{1,1}, {-1,1}, {1,-1}, {-1,-1}};
        for (int[] c : innerCorners) {
            Block b = center.clone().add(c[0], 0, c[1]).getBlock();
            if (b.getType() != Material.NETHERITE_BLOCK) return false;
            Block fire = b.getLocation().add(0, 1, 0).getBlock();
            if (fire.getType() != Material.SOUL_FIRE && fire.getType() != Material.SOUL_CAMPFIRE) return false;
        }
        return true;
    }

    /**
     * Рисует пентаграмму (красные партиклы) над алтарем в течение нескольких секунд.
     */
    public void drawPentagram(Location center, int tier) {
        double radius = tier == 1 ? 2.0 : (tier == 2 ? 3.5 : 4.5);
        Location loc = center.clone().add(0.5, 1.2, 0.5); // Центр чуть выше блока

        new BukkitRunnable() {
            int ticks = 0;
            double rotation = 0;

            @Override
            public void run() {
                if (ticks > 40) { // 2 секунды рисования
                    this.cancel();
                    return;
                }

                // Вращение пентаграммы
                rotation += Math.PI / 20;

                // 5 точек пентаграммы
                Location[] points = new Location[5];
                for (int i = 0; i < 5; i++) {
                    double angle = rotation + (i * 2 * Math.PI / 5);
                    double x = radius * Math.cos(angle);
                    double z = radius * Math.sin(angle);
                    points[i] = loc.clone().add(x, 0, z);
                }

                // Рисуем линии между точками (через одну, чтобы получилась звезда)
                Particle.DustOptions dust = new Particle.DustOptions(org.bukkit.Color.RED, 1.5f);
                for (int i = 0; i < 5; i++) {
                    Location p1 = points[i];
                    Location p2 = points[(i + 2) % 5];
                    drawLine(p1, p2, dust);
                }

                ticks++;
            }
        }.runTaskTimer(AstraSMPPlugin.getInstance(), 0L, 1L);
    }

    private void drawLine(Location loc1, Location loc2, Particle.DustOptions dust) {
        double distance = loc1.distance(loc2);
        double step = 0.2;
        int points = (int) (distance / step);
        double xStep = (loc2.getX() - loc1.getX()) / points;
        double yStep = (loc2.getY() - loc1.getY()) / points;
        double zStep = (loc2.getZ() - loc1.getZ()) / points;

        Location current = loc1.clone();
        for (int i = 0; i < points; i++) {
            current.add(xStep, yStep, zStep);
            current.getWorld().spawnParticle(Particle.DUST, current, 1, 0, 0, 0, 0, dust);
        }
    }
}
