package com.astrasmp.rituals;

import com.astrasmp.AstraSMPPlugin;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AstralManager implements Listener {

    private final AstraSMPPlugin plugin;

    // Хранит UUID ArmorStand -> UUID Игрока
    private final Map<UUID, UUID> astralBodies = new HashMap<>();
    
    // Хранит данные для восстановления (инвентарь, локация, хп) можно хранить в памяти, пока игрок в астрале.
    private final Map<UUID, AstralSession> sessions = new HashMap<>();

    // Хранит UUID Игрока -> UUID захваченного моба
    private final Map<UUID, UUID> possessedMobs = new HashMap<>();

    public AstralManager(AstraSMPPlugin plugin) {
        this.plugin = plugin;
        startPossessionTask();
    }

    private void startPossessionTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                // Копия для избежания ConcurrentModificationException
                for (Map.Entry<UUID, UUID> entry : new HashMap<>(possessedMobs).entrySet()) {
                    Player player = Bukkit.getPlayer(entry.getKey());
                    if (player == null || !player.isOnline()) {
                        possessedMobs.remove(entry.getKey());
                        continue;
                    }
                    
                    org.bukkit.entity.Entity entity = Bukkit.getEntity(entry.getValue());
                    if (entity == null || entity.isDead() || !(entity instanceof org.bukkit.entity.LivingEntity mob)) {
                        endPossession(player);
                        continue;
                    }

                    // Синхронизация позиции (моб следует за игроком)
                    Location loc = player.getLocation().clone();
                    mob.teleport(loc);
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void endPossession(Player player) {
        UUID mobUuid = possessedMobs.remove(player.getUniqueId());
        if (mobUuid != null) {
            org.bukkit.entity.Entity entity = Bukkit.getEntity(mobUuid);
            if (entity instanceof org.bukkit.entity.Mob mob) {
                mob.setAware(true);
            }
        }
        if (sessions.containsKey(player.getUniqueId())) {
            player.setGameMode(GameMode.SPECTATOR);
            player.removePotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY);
            player.sendMessage("§d[Астрал] §5Вы покинули тело существа.");
        }
    }

    public void enterAstral(Player player) {
        if (sessions.containsKey(player.getUniqueId())) {
            player.sendMessage("§cВы уже в Астрале.");
            return;
        }

        Location loc = player.getLocation();
        
        // Создаем тело
        ArmorStand body = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
        body.customName(net.kyori.adventure.text.Component.text("§7Тело: §f" + player.getName()));
        body.setCustomNameVisible(true);
        body.setGravity(true);
        body.setBasePlate(false);
        body.setArms(true);
        
        // Одеваем
        body.getEquipment().setArmorContents(player.getInventory().getArmorContents());
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(player);
        head.setItemMeta(meta);
        body.getEquipment().setHelmet(head);
        
        astralBodies.put(body.getUniqueId(), player.getUniqueId());

        AstralSession session = new AstralSession(player.getGameMode(), loc, body.getUniqueId());
        sessions.put(player.getUniqueId(), session);

        player.setGameMode(GameMode.SPECTATOR);
        player.sendMessage("§d[Астрал] §5Ваша душа покинула тело. У вас есть 2 минуты.");

        new BukkitRunnable() {
            @Override
            public void run() {
                if (sessions.containsKey(player.getUniqueId())) {
                    returnToBody(player, false);
                    player.sendMessage("§d[Астрал] §5Время вышло. Душа возвращена в тело.");
                }
            }
        }.runTaskLater(plugin, 120 * 20L); // 2 минуты
    }

    public void returnToBody(Player player, boolean bodyDestroyed) {
        AstralSession session = sessions.remove(player.getUniqueId());
        if (session == null) return;

        endPossession(player);

        ArmorStand body = (ArmorStand) Bukkit.getEntity(session.bodyUuid);
        if (body != null) {
            astralBodies.remove(body.getUniqueId());
            body.remove();
        }

        if (bodyDestroyed) {
            player.setGameMode(session.previousGameMode);
            player.teleport(session.originalLocation);
            player.setHealth(0.0); // Убиваем игрока
            player.sendMessage("§c[Астрал] §4Ваше тело было уничтожено! Ваша душа разорвана!");
        } else {
            player.teleport(session.originalLocation);
            player.setGameMode(session.previousGameMode);
        }
    }

    @EventHandler
    public void onBodyDamage(EntityDamageEvent event) {
        if (astralBodies.containsKey(event.getEntity().getUniqueId())) {
            UUID playerUuid = astralBodies.get(event.getEntity().getUniqueId());
            Player player = Bukkit.getPlayer(playerUuid);
            
            // Любой урон по телу фатален или можно сделать ему ХП? 
            // Для упрощения хардкора - 1 удар = смерть. Или можно сэмулировать ХП.
            // Сделаем 1 удар смертельным, если это не падение.
            if (event.getCause() != EntityDamageEvent.DamageCause.FALL) {
                event.getEntity().getWorld().spawnParticle(org.bukkit.Particle.DAMAGE_INDICATOR, event.getEntity().getLocation(), 50, 0.5, 1, 0.5, 0.1);
                if (player != null && player.isOnline()) {
                    returnToBody(player, true);
                } else {
                    // Игрок оффлайн. Удаляем тело, помечаем его на смерть при заходе.
                    // Для простоты, просто убьем стенд, лут выпадет из стенда? Нет, броня на стенде пропадет, 
                    // так как мы ее не дропаем, но игрок потеряет вещи при заходе если мы сделаем флаг смерти.
                    // Упрощенно: стенд ломается и дропает броню.
                }
            }
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerSneak(org.bukkit.event.player.PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (event.isSneaking() && sessions.containsKey(player.getUniqueId())) {
            if (possessedMobs.containsKey(player.getUniqueId())) {
                endPossession(player);
            } else {
                returnToBody(player, false);
                player.sendMessage("§d[Астрал] §5Вы досрочно вернулись в свое тело.");
            }
        }
    }

    @EventHandler
    public void onEntityInteract(org.bukkit.event.player.PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (sessions.containsKey(player.getUniqueId()) && !possessedMobs.containsKey(player.getUniqueId())) {
            if (event.getRightClicked() instanceof org.bukkit.entity.Mob mob) {
                event.setCancelled(true);
                possessedMobs.put(player.getUniqueId(), mob.getUniqueId());
                mob.setAware(false);
                player.setGameMode(GameMode.ADVENTURE);
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));
                player.teleport(mob.getLocation());
                player.sendMessage("§d[Астрал] §5Вы захватили разум существа. Нажмите Shift, чтобы покинуть его.");
            }
        }
    }

    @EventHandler
    public void onDamageByPossessed(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            if (possessedMobs.containsKey(player.getUniqueId())) {
                UUID mobUuid = possessedMobs.get(player.getUniqueId());
                org.bukkit.entity.Entity mob = Bukkit.getEntity(mobUuid);
                if (mob instanceof org.bukkit.entity.LivingEntity livingMob && event.getEntity() instanceof org.bukkit.entity.LivingEntity target) {
                    event.setCancelled(true);
                    target.damage(event.getDamage(), livingMob); // Урон от лица моба
                    if (livingMob instanceof org.bukkit.entity.Mob m) m.swingMainHand();
                }
            }
        }
    }

    @EventHandler
    public void onPossessedPlayerDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (possessedMobs.containsKey(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    private static class AstralSession {
        GameMode previousGameMode;
        Location originalLocation;
        UUID bodyUuid;

        AstralSession(GameMode gm, Location loc, UUID body) {
            this.previousGameMode = gm;
            this.originalLocation = loc;
            this.bodyUuid = body;
        }
    }
}
