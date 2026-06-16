package com.astrasmp.pacts;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import com.astrasmp.items.ItemRegistry;

import java.util.UUID;

public class PactManager implements Listener {

    public PactManager() {
        startPactTasks();
    }

    public boolean hasPact(UUID uuid) {
        return com.astrasmp.AstraSMPPlugin.getInstance().getServices().store().profile(uuid.toString(), null).hasPact();
    }

    public String getPactType(UUID uuid) {
        return com.astrasmp.AstraSMPPlugin.getInstance().getServices().store().profile(uuid.toString(), null).getPactType();
    }

    @EventHandler
    public void onUsePact(PlayerInteractEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR && 
            event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        String newPactType = "";
        if (item != null && ItemRegistry.is(item, "pactOfBlood")) newPactType = "BLOOD";
        else if (item != null && ItemRegistry.is(item, "pactOfVoid")) newPactType = "VOID";
        else if (item != null && ItemRegistry.is(item, "pactOfShadow")) newPactType = "SHADOW";

        if (!newPactType.isEmpty()) {
            if (hasPact(player.getUniqueId())) {
                player.sendMessage("§cВы уже заключили Демонический Контракт!");
                return;
            }

            // Забираем контракт
            if (item != null) item.setAmount(item.getAmount() - 1);

            // Заключаем контракт
            com.astrasmp.AstraSMPPlugin.getInstance().getServices().store().profile(player.getUniqueId().toString(), null).setPactType(newPactType);
            com.astrasmp.AstraSMPPlugin.getInstance().getServices().store().requestSave();

            // Урезаем максимальное здоровье (отнимаем 8.0 = 4 сердца)
            applyHealthDebuff(player);

            player.sendMessage("§4[Демонический Контракт] §cСделка совершена. Часть вашей жизненной силы навсегда потеряна.");
            
            if (newPactType.equals("BLOOD")) {
                player.sendMessage("§4[Демонический Контракт] §aНо взамен вы получили силу крови и защиту от огня!");
            } else if (newPactType.equals("VOID")) {
                player.sendMessage("§4[Демонический Контракт] §aНо взамен вы получили иммунитет к падениям, но боитесь воды!");
            } else if (newPactType.equals("SHADOW")) {
                player.sendMessage("§4[Демонический Контракт] §aНо взамен вы стали повелителем теней, но боитесь света!");
            }
        }
    }

    private void startPactTasks() {
        org.bukkit.Bukkit.getScheduler().runTaskTimer(com.astrasmp.AstraSMPPlugin.getInstance(), () -> {
            for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                String pact = getPactType(player.getUniqueId());
                if (pact.equals("VOID")) {
                    // Урон в воде
                    if (player.getLocation().getBlock().getType() == org.bukkit.Material.WATER) {
                        player.damage(1.0); // 0.5 сердца урона
                    }
                } else if (pact.equals("SHADOW")) {
                    // Проверка света
                    int light = player.getLocation().getBlock().getLightLevel();
                    boolean inSunlight = player.getLocation().getBlock().getLightFromSky() == 15 && 
                                         player.getWorld().getTime() < 13000 && 
                                         player.getWorld().getTime() > 0 &&
                                         !player.getWorld().hasStorm();
                    
                    if (inSunlight && player.getLocation().getBlock().getLightLevel() > 10) {
                        player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.WEAKNESS, 40, 0, false, false));
                        player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 40, 0, false, false));
                    } else if (light < 7) {
                        player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY, 40, 0, false, false));
                        player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SPEED, 40, 1, false, false));
                    }
                }
            }
        }, 20L, 20L); // Раз в секунду
    }

    private void applyHealthDebuff(Player player) {
        AttributeInstance maxHp = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHp != null) {
            org.bukkit.attribute.AttributeModifier modifier = new org.bukkit.attribute.AttributeModifier(
                    new org.bukkit.NamespacedKey(com.astrasmp.AstraSMPPlugin.getInstance(), "demonic_pact_debuff"),
                    -8.0,
                    org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER
            );
            // Удаляем старый если есть, чтобы не стакалось
            maxHp.removeModifier(modifier);
            maxHp.addModifier(modifier);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (hasPact(player.getUniqueId())) {
            applyHealthDebuff(player);
        } else {
            // Если игрок очистился, на всякий случай снимаем
            AttributeInstance maxHp = player.getAttribute(Attribute.MAX_HEALTH);
            if (maxHp != null) {
                org.bukkit.attribute.AttributeModifier modifier = new org.bukkit.attribute.AttributeModifier(
                        new org.bukkit.NamespacedKey(com.astrasmp.AstraSMPPlugin.getInstance(), "demonic_pact_debuff"),
                        -8.0,
                        org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER
                );
                maxHp.removeModifier(modifier);
            }
        }
        checkSoulDrained(player);
    }

    @EventHandler
    public void onRespawn(org.bukkit.event.player.PlayerRespawnEvent event) {
        // Задержка 1 тик для корректного наложения после респавна
        org.bukkit.Bukkit.getScheduler().runTaskLater(com.astrasmp.AstraSMPPlugin.getInstance(), () -> {
            if (hasPact(event.getPlayer().getUniqueId())) applyHealthDebuff(event.getPlayer());
            checkSoulDrained(event.getPlayer());
        }, 1L);
    }

    private void checkSoulDrained(Player player) {
        org.bukkit.persistence.PersistentDataContainer data = player.getPersistentDataContainer();
        org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(com.astrasmp.AstraSMPPlugin.getInstance(), "soul_drained_until");
        
        org.bukkit.attribute.AttributeInstance maxHp = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        if (maxHp == null) return;
        
        org.bukkit.attribute.AttributeModifier modifier = new org.bukkit.attribute.AttributeModifier(
                new org.bukkit.NamespacedKey(com.astrasmp.AstraSMPPlugin.getInstance(), "soul_drained_debuff"),
                -4.0, // -2 сердца
                org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER
        );

        if (data.has(key, org.bukkit.persistence.PersistentDataType.LONG)) {
            long expiry = data.get(key, org.bukkit.persistence.PersistentDataType.LONG);
            if (System.currentTimeMillis() < expiry) {
                // Все еще действует
                maxHp.removeModifier(modifier);
                maxHp.addModifier(modifier);
                return;
            } else {
                // Истекло
                data.remove(key);
                player.sendMessage("§a[Бездна] §fВаша душа восстановилась.");
            }
        }
        
        // Снимаем дебафф, если его не должно быть
        maxHp.removeModifier(modifier);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            String pact = getPactType(player.getUniqueId());
            if (pact.equals("VOID") && event.getCause() == EntityDamageEvent.DamageCause.FALL) {
                event.setCancelled(true);
            }
            if (pact.equals("BLOOD")) {
                if (event.getCause() == EntityDamageEvent.DamageCause.LAVA || 
                    event.getCause() == EntityDamageEvent.DamageCause.FIRE || 
                    event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // Вампиризм от Контракта Крови
        if (event.getDamager() instanceof Player damager) {
            if (getPactType(damager.getUniqueId()).equals("BLOOD")) {
                double heal = event.getFinalDamage() * 0.15; // 15% вампиризма
                AttributeInstance maxHpAttr = damager.getAttribute(Attribute.MAX_HEALTH);
                if (maxHpAttr != null) {
                    double newHp = Math.min(damager.getHealth() + heal, maxHpAttr.getValue());
                    damager.setHealth(newHp);
                }
            }
        }
    }
}
