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

    public AstralManager(AstraSMPPlugin plugin) {
        this.plugin = plugin;
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
