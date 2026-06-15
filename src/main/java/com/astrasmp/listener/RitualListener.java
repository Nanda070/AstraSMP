package com.astrasmp.listener;

import com.astrasmp.items.ItemRegistry;
import com.astrasmp.rituals.RitualService;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class RitualListener implements Listener {

    private final RitualService ritualService;

    public RitualListener(RitualService ritualService) {
        this.ritualService = ritualService;
    }

    @EventHandler
    public void onRiftInteract(PlayerInteractEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) return;
        
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        
        if (ItemRegistry.is(hand, "cleansingTotem")) {
            Location loc = event.getClickedBlock() != null ? event.getClickedBlock().getLocation() : player.getLocation();
            Location riftLoc = com.astrasmp.AstraSMPPlugin.getInstance().getServices().rift().getNearbyRift(loc, 5.0);
            
            if (riftLoc != null) {
                event.setCancelled(true);
                hand.setAmount(hand.getAmount() - 1);
                com.astrasmp.AstraSMPPlugin.getInstance().getServices().rift().closeRift(riftLoc);
                player.sendMessage("§e[Очищение] §fВы закрыли Врата Бездны и остановили распространение Скверны.");
            } else {
                // Иначе очищаем душу от Скверны и расторгаем контракт
                event.setCancelled(true);
                
                boolean hasPact = com.astrasmp.AstraSMPPlugin.getInstance().getServices().store().profile(player.getUniqueId().toString(), null).hasPact();
                int current = com.astrasmp.AstraSMPPlugin.getInstance().getServices().store().profile(player.getUniqueId().toString(), null).getCorruption();

                if (hasPact || current > 0) {
                    hand.setAmount(hand.getAmount() - 1);
                    
                    if (hasPact) {
                        com.astrasmp.AstraSMPPlugin.getInstance().getServices().store().profile(player.getUniqueId().toString(), null).setHasPact(false);
                        org.bukkit.attribute.AttributeInstance maxHp = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
                        if (maxHp != null) {
                            org.bukkit.attribute.AttributeModifier modifier = new org.bukkit.attribute.AttributeModifier(
                                    new org.bukkit.NamespacedKey(com.astrasmp.AstraSMPPlugin.getInstance(), "demonic_pact_debuff"),
                                    -8.0,
                                    org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER
                            );
                            maxHp.removeModifier(modifier);
                        }
                        player.getWorld().spawnParticle(org.bukkit.Particle.HEART, player.getLocation(), 100, 0.5, 0.5, 0.5, 0.2);
                        player.sendMessage("§e[Контракт] §fВы использовали Тотем! Демонический контракт разорван, сердца восстановлены!");
                    }

                    if (current > 0) {
                        int newAmount = Math.max(0, current - 50);
                        com.astrasmp.AstraSMPPlugin.getInstance().getServices().store().profile(player.getUniqueId().toString(), null).setCorruption(newAmount);
                        player.getWorld().spawnParticle(org.bukkit.Particle.TOTEM_OF_UNDYING, player.getLocation(), 100, 0.5, 0.5, 0.5, 0.2);
                        player.sendMessage("§e[Очищение] §fВаша Скверна уменьшена на 50.");
                    }
                    
                    com.astrasmp.AstraSMPPlugin.getInstance().getServices().store().requestSave();
                    player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
                } else {
                    player.sendMessage("§e[Очищение] §fВаша душа и так чиста, а контрактов нет.");
                }
            }
        }
    }

    @EventHandler
    public void onSatanicItemUse(PlayerInteractEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getItemMeta() == null) return;
        
        // Voodoo Doll
        if (ItemRegistry.is(item, "voodooDoll")) {
            event.setCancelled(true);
            org.bukkit.persistence.PersistentDataContainer data = item.getItemMeta().getPersistentDataContainer();
            org.bukkit.NamespacedKey uuidKey = new org.bukkit.NamespacedKey(com.astrasmp.AstraSMPPlugin.getInstance(), "voodoo_target_uuid");
            org.bukkit.NamespacedKey nameKey = new org.bukkit.NamespacedKey(com.astrasmp.AstraSMPPlugin.getInstance(), "voodoo_target_name");
            
            if (data.has(uuidKey, org.bukkit.persistence.PersistentDataType.STRING)) {
                String targetUuid = data.get(uuidKey, org.bukkit.persistence.PersistentDataType.STRING);
                String targetName = data.get(nameKey, org.bukkit.persistence.PersistentDataType.STRING);
                com.astrasmp.AstraSMPPlugin.getInstance().getServices().gui().openVoodooGui(player, targetName, targetUuid);
            }
            return;
        }
        
        // Madness Sphere
        if (ItemRegistry.is(item, "madnessSphere")) {
            event.setCancelled(true);
            org.bukkit.persistence.PersistentDataContainer data = item.getItemMeta().getPersistentDataContainer();
            org.bukkit.NamespacedKey uuidKey = new org.bukkit.NamespacedKey(com.astrasmp.AstraSMPPlugin.getInstance(), "madness_target_uuid");
            org.bukkit.NamespacedKey nameKey = new org.bukkit.NamespacedKey(com.astrasmp.AstraSMPPlugin.getInstance(), "madness_target_name");
            
            if (data.has(uuidKey, org.bukkit.persistence.PersistentDataType.STRING)) {
                String targetUuid = data.get(uuidKey, org.bukkit.persistence.PersistentDataType.STRING);
                String targetName = data.get(nameKey, org.bukkit.persistence.PersistentDataType.STRING);
                
                org.bukkit.entity.Player target = org.bukkit.Bukkit.getPlayer(java.util.UUID.fromString(targetUuid));
                if (target != null && target.isOnline()) {
                    long expiryTime = System.currentTimeMillis() + (10L * 60L * 1000L);
                    target.getPersistentDataContainer().set(
                            new org.bukkit.NamespacedKey(com.astrasmp.AstraSMPPlugin.getInstance(), "madness_until"),
                            org.bukkit.persistence.PersistentDataType.LONG, 
                            expiryTime
                    );
                    target.sendMessage("§5[Безумие] §dМрак поглощает ваш разум...");
                    player.sendMessage("§5[Безумие] §dВы наслали ужас на " + targetName + ".");
                    player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_GHAST_SCREAM, 1.0f, 0.5f);
                    
                    item.setAmount(item.getAmount() - 1);
                } else {
                    player.sendMessage("§cИгрок вне сети или недоступен.");
                }
            }
            return;
        }
    }

    @EventHandler
    public void onPortalInteract(PlayerInteractEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != org.bukkit.Material.CRYING_OBSIDIAN) return;

        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();

        if (ItemRegistry.is(hand, "demonSoul")) {
            Location loc = block.getLocation().add(0, 1, 0); // Спавним разлом внутри рамки
            if (loc.getBlock().getType().isAir()) {
                event.setCancelled(true);
                hand.setAmount(hand.getAmount() - 1);
                com.astrasmp.AstraSMPPlugin.getInstance().getServices().rift().createRift(loc);
                
                int current = com.astrasmp.AstraSMPPlugin.getInstance().getServices().store().profile(player.getUniqueId().toString(), null).getCorruption();
                com.astrasmp.AstraSMPPlugin.getInstance().getServices().store().profile(player.getUniqueId().toString(), null).setCorruption(current + 200);
                com.astrasmp.AstraSMPPlugin.getInstance().getServices().store().requestSave();
                player.sendMessage("§5[Скверна] §dОткрытие Врат Бездны наполнило вашу душу Скверной (+200)");
            }
        }
    }

    @EventHandler
    public void onAltarInteract(PlayerInteractEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();

        // Проверяем, что кликнули пустой рукой по Плачущему обсидиану (или кастомному блоку алтаря)
        if (block.getType() != org.bukkit.Material.CRYING_OBSIDIAN) return;

        int tier = ritualService.getCircleManager().getCircleTier(block.getLocation());
        if (tier > 0) {
            // Если игрок держит ритуальный кинжал, не прерываем (возможно он готовится), 
            // но если пустая рука - рисуем пентаграмму.
            if (hand.getType().isAir()) {
                ritualService.getCircleManager().drawPentagram(block.getLocation(), tier);
                player.sendMessage("§5[Темная Магия] §7Алтарь " + tier + " уровня готов к ритуалу.");
            }
        }
    }

    @EventHandler
    public void onSacrifice(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        ItemStack weapon = killer.getInventory().getItemInMainHand();
        if (!ItemRegistry.is(weapon, "sacrificialDagger")) return;

        Location deathLoc = event.getEntity().getLocation();

        // Ищем ближайший алтарь в радиусе 5 блоков
        // Для простоты сканируем блоки вокруг, но в идеале нужно проверять закэшированные координаты
        Location altarLoc = findNearbyAltar(deathLoc);
        if (altarLoc != null) {
            int tier = ritualService.getCircleManager().getCircleTier(altarLoc);
            if (tier > 0) {
                
                // --- ОПУСТОШЕНИЕ ДУШИ (Приношение игрока на алтаре) ---
                if (event.getEntity() instanceof Player victim) {
                    // Выдаем Осколок Души
                    deathLoc.getWorld().dropItemNaturally(deathLoc, ItemRegistry.soulFragment(victim.getName(), victim.getUniqueId().toString()));
                    
                    // Накладываем дебафф "Опустошенная душа" на 30 минут (через PersistentDataContainer)
                    long expiryTime = System.currentTimeMillis() + (30L * 60L * 1000L);
                    victim.getPersistentDataContainer().set(
                            new org.bukkit.NamespacedKey(com.astrasmp.AstraSMPPlugin.getInstance(), "soul_drained_until"),
                            org.bukkit.persistence.PersistentDataType.LONG, 
                            expiryTime
                    );
                    victim.sendMessage("§8[Бездна] §cВаша душа была разорвана на Алтаре... Вы прокляты на 30 минут.");
                    killer.sendMessage("§4[Ритуал] §cВы вырвали Осколок Души у " + victim.getName() + "!");
                }
                
                boolean success = ritualService.attemptRitual(altarLoc, tier, event.getEntity().getType(), killer);
                if (success) {
                    killer.sendMessage("§4[Ритуал] §cЖертвоприношение принято!");
                }
            }
        } else {
            // Если убит игрок кинжалом вне алтаря - гарантированно выпадает флакон крови
            if (event.getEntity() instanceof Player victim) {
                deathLoc.getWorld().dropItemNaturally(deathLoc, ItemRegistry.bloodVial(victim.getName(), victim.getUniqueId().toString()));
                killer.sendMessage("§4[Ритуал] §cВы собрали Флакон с Кровью " + victim.getName() + ".");
            }
        }
    }

    @EventHandler
    public void onDaggerHit(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;
        
        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        if (ItemRegistry.is(weapon, "sacrificialDagger")) {
            // 10% шанс выбить кровь при ударе
            if (Math.random() <= 0.10) {
                victim.getWorld().dropItemNaturally(victim.getLocation(), ItemRegistry.bloodVial(victim.getName(), victim.getUniqueId().toString()));
                attacker.sendMessage("§4[Ритуал] §cВам удалось добыть Флакон с Кровью " + victim.getName() + " прямо в бою!");
            }
        }
    }

    private Location findNearbyAltar(Location loc) {
        int radius = 5;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block b = loc.clone().add(x, y, z).getBlock();
                    if (b.getType() == org.bukkit.Material.CRYING_OBSIDIAN) {
                        return b.getLocation();
                    }
                }
            }
        }
        return null;
    }
}
