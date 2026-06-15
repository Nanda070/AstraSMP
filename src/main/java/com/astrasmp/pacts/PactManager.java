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

    public PactManager() {}

    public boolean hasPact(UUID uuid) {
        return com.astrasmp.AstraSMPPlugin.getInstance().getServices().store().profile(uuid.toString(), null).hasPact();
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

        if (item != null && ItemRegistry.is(item, "demonicPact")) {
            if (hasPact(player.getUniqueId())) {
                player.sendMessage("§cВы уже заключили Демонический Контракт!");
                return;
            }

            // Забираем контракт
            item.setAmount(item.getAmount() - 1);

            // Заключаем контракт
            com.astrasmp.AstraSMPPlugin.getInstance().getServices().store().profile(player.getUniqueId().toString(), null).setHasPact(true);
            com.astrasmp.AstraSMPPlugin.getInstance().getServices().store().requestSave();

            // Урезаем максимальное здоровье (отнимаем 8.0 = 4 сердца)
            applyHealthDebuff(player);

            player.sendMessage("§4[Демонический Контракт] §cСделка совершена. Часть вашей жизненной силы навсегда потеряна.");
            player.sendMessage("§4[Демонический Контракт] §aНо взамен вы получили силу крови и защиту от огня!");
        }
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
    public void onDamage(EntityDamageByEntityEvent event) {
        // Вампиризм от Контракта
        if (event.getDamager() instanceof Player damager) {
            if (hasPact(damager.getUniqueId())) {
                double heal = event.getFinalDamage() * 0.15; // 15% вампиризма
                double newHp = Math.min(damager.getHealth() + heal, damager.getAttribute(Attribute.MAX_HEALTH).getValue());
                damager.setHealth(newHp);
            }
        }
    }

    @EventHandler
    public void onFireDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (hasPact(player.getUniqueId())) {
                // Иммунитет к огню и лаве
                if (event.getCause() == EntityDamageEvent.DamageCause.LAVA || 
                    event.getCause() == EntityDamageEvent.DamageCause.FIRE || 
                    event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK) {
                    event.setCancelled(true);
                }
            }
        }
    }
}
