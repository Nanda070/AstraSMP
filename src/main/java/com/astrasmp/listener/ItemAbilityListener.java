package com.astrasmp.listener;

import java.util.Collection;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.astrasmp.AstraSMPPlugin;
import com.astrasmp.items.ItemRegistry;
import com.astrasmp.service.ServiceManager;

public final class ItemAbilityListener implements Listener {
    private final AstraSMPPlugin plugin;

    public ItemAbilityListener(AstraSMPPlugin plugin, ServiceManager services) {
        this.plugin = plugin;
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::tickPassiveTotems, 20L, 40L);
    }

    private void tickPassiveTotems() {
        for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            ItemStack offHand = p.getInventory().getItemInOffHand();
            if (offHand != null && offHand.hasItemMeta()) {
                String id = ItemRegistry.id(offHand);
                if ("totemSpeed".equals(id)) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 0, true, false));
                } else if ("totemShield".equals(id)) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 0, true, false));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player p = event.getPlayer();
        ItemStack item = p.getInventory().getItemInMainHand();
        String id = ItemRegistry.id(item);
        if (id == null) return;

        Block center = event.getBlock();

        // 1. ЛОГИКА ШАХТЕРА (3x3 и 5x5)
        if (id.equals("mine_3x3") || id.equals("mine_5x5")) {
            int r = id.equals("mine_3x3") ? 1 : 2;
            handleMultiBreak(p, center, r, item);
        }

        // 2. МАГНИТ (притяжение дропа)
        if (id.equals("magnet")) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                Collection<Entity> dropped = center.getWorld().getNearbyEntities(center.getLocation(), 2, 2, 2);
                for (Entity e : dropped) {
                    if (e instanceof Item drop) drop.teleport(p.getLocation());
                }
            }, 1L);
        }

        // 3. АВТОПЛАВКА
        if (id.equals("auto_smelt")) {
            Material type = center.getType();
            Material result = null;
            if (type.name().contains("IRON_ORE") || type.name().contains("RAW_IRON")) result = Material.IRON_INGOT;
            else if (type.name().contains("GOLD_ORE") || type.name().contains("RAW_GOLD")) result = Material.GOLD_INGOT;
            else if (type.name().contains("COPPER_ORE") || type.name().contains("RAW_COPPER")) result = Material.COPPER_INGOT;

            if (result != null) {
                event.setDropItems(false);
                center.getWorld().dropItemNaturally(center.getLocation(), new ItemStack(result));
            }
        }
    }

    private void handleMultiBreak(Player p, Block center, int r, ItemStack tool) {
        // Определяем, куда смотрит игрок (на какую грань блока)
        BlockFace face = p.getTargetBlockFace(5);
        if (face == null) return;

        int sx = 0, ex = 0;
        int sy = 0, ey = 0;
        int sz = 0, ez = 0;

        // Настройка осей в зависимости от направления взгляда
        switch (face) {
            case UP:
            case DOWN:
                // Смотрит вниз или вверх -> копаем по горизонтали (XZ)
                sx = -r; ex = r;
                sz = -r; ez = r;
                break;
            case NORTH:
            case SOUTH:
                // Смотрит на север или юг -> копаем по вертикали (XY)
                sx = -r; ex = r;
                sy = -r; ey = r;
                break;
            case EAST:
            case WEST:
                // Смотрит на восток или запад -> копаем по вертикали (ZY)
                sz = -r; ez = r;
                sy = -r; ey = r;
                break;
            default:
                // Диагональные направления (NE, NW, SE, SW и т.д.) — копаем по горизонтали (XZ)
                sx = -r; ex = r;
                sz = -r; ez = r;
                break;
        }

        for (int x = sx; x <= ex; x++) {
            for (int y = sy; y <= ey; y++) {
                for (int z = sz; z <= ez; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;

                    Block b = center.getRelative(x, y, z);
                    if (b.getType() == Material.BEDROCK || b.getType().isAir()) continue;

                    // ПРИВАТЫ УДАЛЕНЫ - копаем везде
                    b.breakNaturally(tool);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof org.bukkit.entity.Projectile proj && proj.getShooter() instanceof Player p) {
            if (proj instanceof org.bukkit.entity.Arrow) {
                String id = ItemRegistry.id(p.getInventory().getItemInMainHand());
                if (id != null && id.equals("venomBow") && event.getEntity() instanceof LivingEntity target) {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 1));
                }
            }
            return;
        }

        if (!(event.getDamager() instanceof Player p)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        String id = ItemRegistry.id(p.getInventory().getItemInMainHand());
        if (id == null) return;

        switch (id) {
            case "thunderHammer" -> target.getWorld().strikeLightning(target.getLocation());
            case "shadowBlade" -> {
                target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 0));
            }
            case "reaperScythe" -> {
                target.getWorld().spawnParticle(org.bukkit.Particle.SWEEP_ATTACK, target.getLocation().add(0, 1, 0), 5, 1, 0.5, 1, 0);
                for (Entity e : target.getWorld().getNearbyEntities(target.getLocation(), 3, 3, 3)) {
                    if (e instanceof LivingEntity le && e != p && e != target) {
                        le.damage(event.getFinalDamage() * 0.5, p);
                    }
                }
            }
            case "vampire_dagger" -> {
                var maxHpAttr = p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
                double maxHp = maxHpAttr != null ? maxHpAttr.getValue() : 20.0;
                p.setHealth(Math.min(maxHp, p.getHealth() + (event.getFinalDamage() * 0.2)));
            }
            case "infernoSword" -> {
                target.setFireTicks(100);
                target.getWorld().createExplosion(target.getLocation(), 2F, false, false);
            }
            case "frostAxe" -> target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 2));
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player p = event.getPlayer();
        ItemStack item = p.getInventory().getItemInMainHand();
        String id = ItemRegistry.id(item);
        if (id == null) return;

        // ПАССИВКА: Сердце мира
        if (id.equals("artifact_heart_of_world")) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 50, 0));
        }

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (id.equals("totem_explosion")) {
                p.setNoDamageTicks(20);
                p.getWorld().createExplosion(p.getLocation(), 4F, false, false);
                consume(p, item);
            } else if (id.equals("totem_lightning")) {
                p.setNoDamageTicks(20);
                p.getWorld().strikeLightning(p.getLocation());
                consume(p, item);
            } else if (id.equals("totem_teleport")) {
                p.setVelocity(p.getLocation().getDirection().multiply(2).setY(1));
                consume(p, item);
            } else if (id.equals("relic_time_core")) {
                if (checkCooldown(p, "time_core", 30000)) {
                    p.getWorld().spawnParticle(org.bukkit.Particle.ENCHANT, p.getLocation(), 200, 5, 2, 5, 0.1);
                    p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1f, 0.5f);
                    for (Entity e : p.getNearbyEntities(10, 10, 10)) {
                        if (e instanceof LivingEntity le && e != p) {
                            le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 2));
                            le.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 200, 1));
                        }
                    }
                    p.sendMessage("§d[Реликвия] §fВремя вокруг вас замедлилось.");
                }
            } else if (id.equals("relic_void_fragment")) {
                if (checkCooldown(p, "void_fragment", 15000)) {
                    p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                    p.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, p.getLocation(), 50, 0.5, 1, 0.5, 0.1);
                    org.bukkit.Location target = p.getLocation().add(p.getLocation().getDirection().multiply(6));
                    p.teleport(target);
                    p.getWorld().playSound(target, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                    p.sendMessage("§5[Реликвия] §fРывок сквозь пустоту!");
                }
            }
        }
    }

    private final java.util.Map<java.util.UUID, java.util.Map<String, Long>> cooldowns = new java.util.HashMap<>();

    private boolean checkCooldown(Player p, String ability, long cooldownMs) {
        cooldowns.putIfAbsent(p.getUniqueId(), new java.util.HashMap<>());
        java.util.Map<String, Long> pCooldowns = cooldowns.get(p.getUniqueId());
        long lastUse = pCooldowns.getOrDefault(ability, 0L);
        if (System.currentTimeMillis() - lastUse < cooldownMs) {
            long remaining = (cooldownMs - (System.currentTimeMillis() - lastUse)) / 1000;
            p.sendMessage("§cСпособность перезаряжается! Осталось " + remaining + " сек.");
            return false;
        }
        pCooldowns.put(ability, System.currentTimeMillis());
        return true;
    }

    private void consume(Player p, ItemStack item) {
        item.setAmount(item.getAmount() - 1);
        p.playSound(p.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
    }
}