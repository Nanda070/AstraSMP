package com.astrasmp.listener;

import com.astrasmp.model.Guild;
import com.astrasmp.service.ServiceManager;
import com.astrasmp.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import com.astrasmp.items.ItemRegistry;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Sound;

public class RegionListener implements Listener {
    private final ServiceManager services;

    public RegionListener(ServiceManager services) {
        this.services = services;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Location loc = event.getBlock().getLocation();
        Guild playerGuild = services.guilds().getPlayerGuild(player.getUniqueId());

        // Проверка: Не ломают ли Сердце собственной гильдии?
        if (playerGuild != null && playerGuild.getCoreLocation() != null) {
            String[] parts = playerGuild.getCoreLocation().split(",");
            Location coreLoc = new Location(Bukkit.getWorld(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));

            // Если игрок сломал своё ядро
            if (loc.equals(coreLoc)) {
                if (!playerGuild.getLeader().equals(player.getUniqueId())) {
                    TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_d6118b", "&cТолько лидер может разрушить Сердце Гильдии!"));
                    event.setCancelled(true);
                    return;
                }
                // Обнуляем базу и возвращаем предмет
                playerGuild.setCoreLocation(null);
                services.guilds().saveGuildAsync(playerGuild);
                event.setDropItems(false); // Отключаем обычный дроп обсидиана
                player.getWorld().dropItemNaturally(loc, ItemRegistry.guildHeart());

                TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_7425aa", "&cБаза гильдии снята. Приват отключен."));
                return;
            }
        }

        // Стандартная защита от чужаков (из прошлого кода)
        if (isProtected(player, loc)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isProtected(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    // Добавь этот метод внутрь RegionListener.java

    @EventHandler
    public void onHeartPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item.getItemMeta() == null) return;

        // Проверяем, что это именно Сердце Гильдии (по нашему NBT-тегу)
        org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey("astrasmp", "custom_id");
        if ("guild_heart".equals(item.getItemMeta().getPersistentDataContainer().get(key, org.bukkit.persistence.PersistentDataType.STRING))) {

            Player player = event.getPlayer();
            Guild guild = services.guilds().getPlayerGuild(player.getUniqueId());

            // Защита от багов
            if (guild == null) {
                TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_7f0356", "&cУ вас нет гильдии!"));
                event.setCancelled(true);
                return;
            }

            if (!guild.getLeader().equals(player.getUniqueId())) {
                TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_bb2e75", "&cТолько лидер может установить Сердце Гильдии!"));
                event.setCancelled(true);
                return;
            }

            if (guild.getCoreLocation() != null) {
                TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_a6be90", "&cУ вашей гильдии уже установлено Сердце! Чтобы перенести его, сломайте старое."));
                event.setCancelled(true);
                return;
            }

            // Сохраняем локацию
            Location loc = event.getBlockPlaced().getLocation();
            String locStr = loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();

            guild.setCoreLocation(locStr);
            services.guilds().saveGuildAsync(guild); // Сразу пишем в БД

            TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_59a1df", "&d&lВы успешно установили базу гильдии!"));
            TextUtil.send(player, "&7Территория в радиусе &e" + guild.getCoreRadius() + " блоков &7теперь под защитой.");

            // Эпичный звук при установке
            player.getWorld().playSound(loc, Sound.ENTITY_WITHER_SPAWN, 0.5f, 1f);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() != null) {
            // Защищаем сундуки, двери, печки и т.д.
            Material type = event.getClickedBlock().getType();
            if (type == Material.CHEST || type.name().endsWith("DOOR") || type == Material.FURNACE) {
                if (isProtected(event.getPlayer(), event.getClickedBlock().getLocation())) {
                    event.setCancelled(true);
                }
            }
        }
    }

    /**
     * Возвращает true, если локация находится в чужом привате
     */
    private boolean isProtected(Player player, Location loc) {
        Guild playerGuild = services.guilds().getPlayerGuild(player.getUniqueId());

        for (Guild guild : services.guilds().getGuilds().values()) {
            String coreStr = guild.getCoreLocation();
            if (coreStr == null) continue;

            try {
                String[] parts = coreStr.split(",");
                Location coreLoc = new Location(
                        Bukkit.getWorld(parts[0]),
                        Double.parseDouble(parts[1]),
                        Double.parseDouble(parts[2]),
                        Double.parseDouble(parts[3])
                );

                // Если действие происходит в чужом мире — пропускаем
                if (!loc.getWorld().equals(coreLoc.getWorld())) continue;

                // Если дистанция меньше радиуса гильдии
                if (loc.distance(coreLoc) <= guild.getCoreRadius()) {
                    // Если это гильдия самого игрока — разрешаем
                    if (playerGuild != null && playerGuild.getId().equals(guild.getId())) {
                        return false;
                    }
                    TextUtil.send(player, "&cЭто территория гильдии &f" + guild.getName() + "&c!");
                    return true; // Блокируем действие
                }
            } catch (Exception ignored) {}
        }
        return false;
    }
}