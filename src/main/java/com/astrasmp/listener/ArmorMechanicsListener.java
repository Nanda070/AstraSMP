package com.astrasmp.listener;

import com.astrasmp.AstraSMPPlugin;
import com.astrasmp.items.ItemRegistry;
import com.astrasmp.service.ServiceManager;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public final class ArmorMechanicsListener implements Listener {
    private final AstraSMPPlugin plugin;
    private final ServiceManager services;

    public ArmorMechanicsListener(AstraSMPPlugin plugin, ServiceManager services) {
        this.plugin = plugin;
        this.services = services;
        startPassiveEffectsTask();
    }

    // Хелпер: Проверяет, одет ли на игроке полный сет определенной брони
    public static boolean hasFullSet(Player player, String prefix) {
        org.bukkit.inventory.ItemStack[] equip = player.getInventory().getArmorContents();
        if (equip == null || equip.length != 4) return false;

        for (org.bukkit.inventory.ItemStack item : equip) {
            String id = ItemRegistry.id(item);
            if (id == null || !id.startsWith(prefix)) return false;
        }
        return true;
    }

    // ==========================================
    // ПАССИВНЫЕ ЭФФЕКТЫ (Шахтер, Джаггернаут)
    // ==========================================
    private void startPassiveEffectsTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {

                // Шахтерский Экзоскелет (Спешка II и Ночное зрение)
                if (hasFullSet(p, "miner_")) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 40, 1, false, false, true));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 300, 0, false, false, true));
                }

                // Сет Джаггернаута (Постоянное замедление из-за тяжести брони)
                if (hasFullSet(p, "juggernaut_")) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0, false, false, true));
                }
            }
        }, 20L, 20L); // Проверяем каждую секунду
    }

    // ==========================================
    // БОЕВЫЕ ЭФФЕКТЫ (Наемник, Берсерк, Охотник)
    // ==========================================
    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {

        // --- ЕСЛИ ИГРОК АТАКУЕТ ---
        if (event.getDamager() instanceof Player attacker) {

            // Второе дыхание (Берсерк): +30% урона, если здоровье меньше 4 сердечек (8.0 HP)
            if (hasFullSet(attacker, "berserker_") && attacker.getHealth() <= 8.0) {
                event.setDamage(event.getDamage() * 1.30);
            }

            // Удар в спину (Наемник): +25% урона со спины
            if (hasFullSet(attacker, "mercenary_") && event.getEntity() instanceof LivingEntity victim) {
                Vector attackerDir = attacker.getLocation().getDirection();
                Vector victimDir = victim.getLocation().getDirection();

                // Если направления взгляда совпадают (игрок бьет в спину)
                if (attackerDir.dot(victimDir) > 0.5) {
                    event.setDamage(event.getDamage() * 1.25);
                    attacker.getWorld().spawnParticle(Particle.CRIT, victim.getLocation().add(0, 1, 0), 15);
                }
            }
        }

        // --- ЕСЛИ ИГРОК ПОЛУЧАЕТ УРОН ---
        if (event.getEntity() instanceof Player victim) {

            // Джаггернаут: -15% входящего урона
            if (hasFullSet(victim, "juggernaut_")) {
                event.setDamage(event.getDamage() * 0.85);
            }

            // Охотник Кровавой Ночи: -35% урона от мобов во время Кровавой Ночи
            if (services.events() != null && services.events().isBloodNight() && !(event.getDamager() instanceof Player)) {
                if (hasFullSet(victim, "bloodhunter_")) {
                    event.setDamage(event.getDamage() * 0.65);
                }
            }
        }
    }

    // ==========================================
    // ЭФФЕКТЫ ПРИ УБИЙСТВЕ (Наемник)
    // ==========================================
    @EventHandler
    public void onKill(EntityDeathEvent event) {
        if (event.getEntity().getKiller() != null) {
            Player killer = event.getEntity().getKiller();

            // Скорость II на 5 секунд после убийства
            if (hasFullSet(killer, "mercenary_")) {
                killer.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 1, false, true, true));
            }
        }
    }

    // ==========================================
    // АНТИ-ДЕБАФФЫ (Инквизитор)
    // ==========================================
    @EventHandler
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (event.getEntity() instanceof Player p && hasFullSet(p, "inquisitor_")) {
            PotionEffect newEffect = event.getNewEffect();
            if (newEffect != null) {
                PotionEffectType type = newEffect.getType();

                // Блокируем негативные эффекты (от Теневого клинка, Ледяного топора и Яда)
                if (type.equals(PotionEffectType.BLINDNESS) ||
                        type.equals(PotionEffectType.SLOWNESS) ||
                        type.equals(PotionEffectType.POISON)) {

                    event.setCancelled(true);
                }
            }
        }
    }

    // ==========================================
    // РЫВОК У КЛАССОВ
    // ==========================================
    private final java.util.Map<java.util.UUID, Long> dashCooldowns = new java.util.HashMap<>();

    @EventHandler
    public void onInteract(org.bukkit.event.player.PlayerInteractEvent event) {
        org.bukkit.event.block.Action action = event.getAction();
        // Right-clicking air with a sword doesn't send packets in modern versions,
        // so we also allow LEFT_CLICK_AIR to trigger the dash "in the air".
        if (!action.isRightClick() && action != org.bukkit.event.block.Action.LEFT_CLICK_AIR) return;
        Player p = event.getPlayer();
        
        org.bukkit.Material mainHand = p.getInventory().getItemInMainHand().getType();
        boolean validItem = mainHand.isAir() || mainHand.name().endsWith("_SWORD") || mainHand.name().endsWith("_AXE");
        
        if (validItem) {
            if (hasFullSet(p, "miner_") || hasFullSet(p, "juggernaut_") ||
                hasFullSet(p, "berserker_") || hasFullSet(p, "mercenary_") ||
                hasFullSet(p, "bloodhunter_") || hasFullSet(p, "inquisitor_") ||
                hasFullSet(p, "assassin_")) {
                
                long now = System.currentTimeMillis();
                long last = dashCooldowns.getOrDefault(p.getUniqueId(), 0L);
                if (now - last < 5000) {
                    long timeLeft = (5000 - (now - last)) / 1000;
                    if (timeLeft > 0 && mainHand.isAir()) {
                        com.astrasmp.util.TextUtil.send(p, "&cРывок перезаряжается! Осталось: " + timeLeft + "с.");
                    }
                    return;
                }
                
                dashCooldowns.put(p.getUniqueId(), now);
                Vector dir = p.getLocation().getDirection().normalize().multiply(1.5).setY(0.4);
                p.setVelocity(dir);
                p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 1.5f);
                p.spawnParticle(Particle.CLOUD, p.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
            }
        }
    }
}