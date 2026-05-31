package com.astrasmp.listener;

import com.astrasmp.AstraSMPPlugin;
import com.astrasmp.model.PlayerProfile;
import com.astrasmp.service.ServiceManager;
import com.astrasmp.util.TextUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PlayerListener implements Listener {
    private final AstraSMPPlugin plugin;
    private final ServiceManager services;

    // Хранилище кулдаунов для активных тотемов (КД 15 секунд)
    private final Map<UUID, Long> totemCooldowns = new HashMap<>();

    public PlayerListener(AstraSMPPlugin plugin, ServiceManager services) {
        this.plugin = plugin;
        this.services = services;
    }

    // ==========================================
    // ЛОГИКА ВХОДА И ПРИВЕТСТВИЯ
    // ==========================================
    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        services.economy().profile(p.getUniqueId(), p.getName());
        services.events().addPlayerToBossBar(p);
        event.setJoinMessage(TextUtil.color("&8[&a+&8] &f" + p.getName()));

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            sendWelcomeMessage(p);
            updateTab(p);
        }, 20L);

        services.discord().sendJoinQuitMessage(p.getName(), true);
        services.afk().updateActivity(p);
        giveMenuCompass(p);
    }

    public void updateTab(Player p) {
        PlayerProfile profile = services.economy().profile(p.getUniqueId(), p.getName());
        Component header = Component.text(TextUtil.color("\n&b&lChet&f&lCraft SMP\n&7Игроков онлайн: &f" + Bukkit.getOnlinePlayers().size() + "\n"));
        Component footer = Component.text(TextUtil.color("\n&eБаланс: &f" + profile.getCoins() + " ❂  &8|  &dОчки ивентов: &f" + profile.getEventPoints() + " EP\n&b/menu &7для управления\n"));
        p.sendPlayerListHeaderAndFooter(header, footer);
    }

    private void sendWelcomeMessage(Player player) {
        var prefix = Component.text(TextUtil.color("\n&b&lChet&f&lCraft &8» "));
        var welcomeLink = Component.text(TextUtil.color("&7Добро пожаловать на сервер! Discord: "))
                .append(Component.text(TextUtil.color("&b&nНажми сюда"))
                        .clickEvent(ClickEvent.openUrl("https://discord.gg/cheterin"))
                        .hoverEvent(HoverEvent.showText(Component.text(TextUtil.color("&eПрисоединяйся к нам!")))));
        var wishMsg = Component.text(TextUtil.color("\n&b&lChet&f&lCraft &8» &fЖелаем тебе хорошо провести время!\n"));

        player.sendMessage(prefix.append(welcomeLink));
        player.sendMessage(wishMsg);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        event.setQuitMessage(TextUtil.color("&8[&c-&8] &f" + event.getPlayer().getName()));
        services.discord().sendJoinQuitMessage(event.getPlayer().getName(), false);
        services.store().requestSave();
        services.afk().removePlayer(event.getPlayer());

        // Очищаем кулдаун при выходе, чтобы не засорять память
        totemCooldowns.remove(event.getPlayer().getUniqueId());
    }

    // ==========================================
    // КОМПАС: ВЫДАЧА И ЗАЩИТА
    // ==========================================
    private void giveMenuCompass(Player p) {
        for (int i = 0; i < p.getInventory().getSize(); i++) {
            ItemStack item = p.getInventory().getItem(i);
            if (isMenuCompass(item) && i != 8) p.getInventory().setItem(i, null);
        }
        ItemStack compass = new ItemStack(Material.COMPASS);
        var meta = compass.getItemMeta();
        meta.displayName(Component.text(TextUtil.color("&b&lМеню ChetCraft")));
        meta.lore(java.util.List.of(Component.text(TextUtil.color("&7Нажмите ПКМ, чтобы открыть"))));
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "menu_compass"), PersistentDataType.BYTE, (byte) 1);
        compass.setItemMeta(meta);
        p.getInventory().setItem(8, compass);
    }

    private boolean isMenuCompass(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        if (!item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(plugin, "menu_compass"), PersistentDataType.BYTE);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (isMenuCompass(event.getItemDrop().getItemStack())) event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // 1. Запрещаем кликать по самому компасу в любом инвентаре
        if (isMenuCompass(event.getCurrentItem()) || isMenuCompass(event.getCursor())) {
            event.setCancelled(true);
            return;
        }

        // 2. Запрещаем использовать цифры (1-9) на клавиатуре, чтобы заменить компас
        if (event.getHotbarButton() == 8) {
            event.setCancelled(true);
            return;
        }

        // 3. Блокируем клик по 8-му слоту ТОЛЬКО если это инвентарь самого игрока
        if (event.getClickedInventory() != null && event.getClickedInventory().getType() == org.bukkit.event.inventory.InventoryType.PLAYER) {
            if (event.getSlot() == 8) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        giveMenuCompass(event.getPlayer());
    }

    // ==========================================
    // ПЕРЕДВИЖЕНИЕ И НАГРАДЫ
    // ==========================================
    @SuppressWarnings("deprecation")
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (isRestricted(player)) {
            Location from = event.getFrom();
            Location to = event.getTo();
            if (from.getX() != to.getX() || from.getZ() != to.getZ() || from.getY() != to.getY()) {
                event.setTo(event.getFrom());
            }
            return;
        }

        if (event.getFrom().getBlockX() != event.getTo().getBlockX() || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
            services.afk().updateActivity(player);
            Location loc = player.getLocation().getBlock().getLocation();

            if (services.store().getRtpBlocks().contains(com.astrasmp.util.LocationKey.fromLocation(loc))) player.performCommand("rtp");

            Location locBelow = player.getLocation().clone().subtract(0, 0.5, 0).getBlock().getLocation();
            Long awardAmount = services.store().getAwardAmount(locBelow);
            Location targetLoc = locBelow;

            if (awardAmount == null) {
                awardAmount = services.store().getAwardAmount(loc);
                targetLoc = loc;
            }

            if (awardAmount != null && !services.store().hasAlreadyCollected(player, targetLoc)) {
                services.store().markAsCollected(player, targetLoc);
                player.spawnParticle(Particle.TOTEM_OF_UNDYING, targetLoc.clone().add(0.5, 1, 0.5), 30, 0.3, 0.3, 0.3, 0.1);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);

                PlayerProfile profile = services.economy().profile(player.getUniqueId(), player.getName());
                profile.setCoins(profile.getCoins() + awardAmount);
                services.store().requestSave();
                updateTab(player);

                player.sendTitle(TextUtil.color("&6&l+" + awardAmount + " ❂"), TextUtil.color("&eНаграда за локацию получена!"), 10, 40, 10);
                TextUtil.send(player, "&aВы получили бонус за нахождение этого места: &f" + awardAmount + " ❂");
            }
        }
    }

    // ==========================================
    // СИСТЕМА ЧАТА
    // ==========================================
    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        PlayerProfile profile = services.economy().profile(player.getUniqueId(), player.getName());

        if (isRestricted(player)) {
            event.setCancelled(true);
            TextUtil.send(player, "&cВы не можете писать в чат, пока заморожены.");
            return;
        }

        event.renderer((source, sourceDisplayName, message, viewer) -> {
            String colorCode = profile.getPrefixColor().isEmpty() ? "&7" : profile.getPrefixColor();
            String prefixText = profile.getCustomPrefix();
            Component prefixComponent = prefixText.isEmpty() ? (player.isOp() ? Component.text(TextUtil.color("&c[Админ] ")) : Component.text(TextUtil.color("&7[Игрок] "))) : Component.text(TextUtil.color(colorCode + "[" + prefixText + "] "));
            return prefixComponent.append(sourceDisplayName.color(NamedTextColor.WHITE)).append(Component.text(TextUtil.color(" &8» &f"))).append(message.color(NamedTextColor.WHITE));
        });

        String msg = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.message());
        services.discord().sendChat(player.getName(), msg);
    }

    // ==========================================
    // СМЕРТЬ И УБИЙСТВА
    // ==========================================
    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        event.getDrops().removeIf(this::isMenuCompass);

        Player victim = event.getEntity();
        PlayerProfile profile = services.economy().profile(victim.getUniqueId(), victim.getName());
        profile.setDeaths(profile.getDeaths() + 1);
        services.mmr().adjustOnDeath(victim);

        if (victim.getKiller() != null) {
            Player killer = victim.getKiller();
            PlayerProfile killerProfile = services.economy().profile(killer.getUniqueId(), killer.getName());
            killerProfile.setKills(killerProfile.getKills() + 1);
            int gain = services.mmr().adjustOnKill(killer, victim);
            long bounty = services.contracts().handleKillReward(killer, victim);
            killer.sendMessage(TextUtil.color("&aУбийство! &f+" + gain + " MMR" + (bounty > 0 ? " &7и награда &f" + bounty + " ❂" : "")));

            // Уведомление в Discord о PvP
            String deathMsg = "убит игроком **" + killer.getName() + "**";
            services.discord().sendDeathMessage(victim.getName(), deathMsg);
        } else {
            // Смерть не от игрока (окружающая среда)
            Component deathComponent = event.deathMessage();
            String rawReason = deathComponent != null
                    ? net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(deathComponent)
                    : "погиб";
            services.discord().sendDeathMessage(victim.getName(), rawReason);
        }
        services.store().requestSave();
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() != null) services.quests().checkProgress(event.getEntity().getKiller(), 6, 1);
    }

    @EventHandler
    public void onMobSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL && event.getEntity() instanceof Monster monster) {
            if (services.events().isBloodNight()) {
                monster.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 999999, 1));
                monster.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 999999, 0));
                monster.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 999999, 0));
                if (Math.random() < 0.5) monster.getWorld().spawnEntity(monster.getLocation(), monster.getType());
            }
        }
    }

    @EventHandler
    public void onFurnaceExtract(FurnaceExtractEvent event) {
        Player player = event.getPlayer();
        if (isRestricted(player)) return;
        Material type = event.getItemType();
        if (type.name().endsWith("_INGOT") || type == Material.COPPER_INGOT) {
            services.quests().checkProgress(player, 4, event.getItemAmount());
        }
    }

    // ==========================================
    // ДОБЫЧА И СТРОИТЕЛЬСТВО (КИРКИ)
    // ==========================================
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (isRestricted(player)) { event.setCancelled(true); return; }

        Block block = event.getBlock();
        if (services.store().getAwardAmount(block.getLocation()) != null && !player.isOp()) {
            event.setCancelled(true);
            TextUtil.send(player, "&cЭтот блок нельзя сломать!");
            return;
        }

        ItemStack tool = player.getInventory().getItemInMainHand();
        String customId = getCustomId(tool);

        if (customId != null) {
            if (customId.equals("mining3x3") || customId.equals("mining5x5")) {
                int radius = customId.equals("mining3x3") ? 1 : 2;
                for (int x = -radius; x <= radius; x++) {
                    for (int y = -radius; y <= radius; y++) {
                        for (int z = -radius; z <= radius; z++) {
                            if (x == 0 && y == 0 && z == 0) continue;
                            Block b = block.getRelative(x, y, z);
                            if (b.getType() != Material.AIR && b.getType() != Material.BEDROCK) {
                                b.breakNaturally(tool);
                            }
                        }
                    }
                }
            }

            if (customId.equals("magnet")) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    for (Entity e : block.getWorld().getNearbyEntities(block.getLocation(), 6, 6, 6)) {
                        if (e instanceof Item itemDrop) itemDrop.teleport(player);
                    }
                }, 5L);
            }

            if (customId.equals("autoSmelt")) {
                Material result = getSmeltedResult(block.getType());
                if (result != null) {
                    event.setDropItems(false);
                    Collection<ItemStack> drops = block.getDrops(tool);
                    for (ItemStack drop : drops) {
                        block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(result, drop.getAmount()));
                    }
                }
            }

            if (customId.equals("veinMiner") && isOre(block.getType())) {
                processVein(block, block.getType(), 0, tool);
            }
        }

        Material mat = block.getType();
        if (mat.name().contains("LOG")) services.quests().checkProgress(player, 1, 1);
        else if (mat == Material.COBBLESTONE || mat == Material.STONE || mat == Material.DEEPSLATE) services.quests().checkProgress(player, 2, 1);
        else if (mat == Material.DIAMOND_ORE || mat == Material.DEEPSLATE_DIAMOND_ORE) {
            if (!event.getBlock().getDrops(player.getInventory().getItemInMainHand()).isEmpty()) services.quests().checkProgress(player, 9, 1);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isRestricted(event.getPlayer())) { event.setCancelled(true); return; }
        ItemStack item = event.getItemInHand();
        if (item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(plugin, "award_amount"), PersistentDataType.LONG)) {
            long amount = item.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(plugin, "award_amount"), PersistentDataType.LONG);
            services.store().addAwardBlock(event.getBlockPlaced().getLocation(), amount);
            TextUtil.send(event.getPlayer(), "&aБлок-награда успешно установлен и активен!");
        }
    }

    // ==========================================
    // БОЙ И ОРУЖИЕ
    // ==========================================
    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (isRestricted(attacker)) { event.setCancelled(true); return; }

        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        String customId = getCustomId(weapon);
        if (customId == null) return;

        if (customId.equals("infernoSword")) {
            event.getEntity().getNearbyEntities(4, 4, 4).forEach(entity -> {
                if (entity instanceof LivingEntity living && entity != attacker) living.setFireTicks(100);
            });
        }

        if (!(event.getEntity() instanceof LivingEntity victim)) return;

        if (customId.equals("shadowBlade")) {
            victim.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 1));
            attacker.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 1));
        } else if (customId.equals("thunderHammer")) {
            victim.getWorld().strikeLightningEffect(victim.getLocation());
            event.setDamage(event.getDamage() + 4.0); // Исправлен вылет от бесконечного цикла!
        } else if (customId.equals("vampireDagger")) {
            double heal = event.getFinalDamage() * 0.4;
            double newHealth = Math.min(attacker.getAttribute(Attribute.MAX_HEALTH).getValue(), attacker.getHealth() + heal); // Исправлен MAX_HEALTH!
            attacker.setHealth(newHealth);
            attacker.spawnParticle(Particle.HEART, attacker.getLocation().add(0, 1, 0), 5, 0.3, 0.3, 0.3, 0);
        } else if (customId.equals("frostAxe")) {
            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 2));
            victim.getWorld().spawnParticle(Particle.SNOWFLAKE, victim.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0.05);
        }
    }

    // ==========================================
    // ИНТЕРАКТ (ТОТЕМЫ, СУНДУКИ, КОМПАС)
    // ==========================================
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (isRestricted(player)) { event.setCancelled(true); return; }

        ItemStack item = event.getItem();
        String customId = getCustomId(item);

        if (event.getAction().isRightClick()) {
            if (isMenuCompass(item)) {
                event.setCancelled(true);
                player.performCommand("menu");
                return;
            }

            // АКТИВНЫЕ ТОТЕМЫ С КУЛДАУНОМ (15 СЕК)
            if (customId != null && (customId.equals("totemTeleport") || customId.equals("totemExplosion") || customId.equals("totemLightning"))) {
                event.setCancelled(true); // Запрещаем ванильное поведение тотема (если есть)

                long currentTime = System.currentTimeMillis();

                // Проверка кулдауна
                if (totemCooldowns.containsKey(player.getUniqueId())) {
                    long lastUsedTime = totemCooldowns.get(player.getUniqueId());
                    long timeLeft = (lastUsedTime + 15000L) - currentTime;

                    if (timeLeft > 0) {
                        TextUtil.send(player, "&cЭнергия тотема восстанавливается! Осталось: &e" + (timeLeft / 1000L) + " сек.");
                        return;
                    }
                }

                // Выполнение способностей
                if (customId.equals("totemTeleport")) {
                    player.setVelocity(player.getLocation().getDirection().multiply(2.0).setY(0.5));
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                } else if (customId.equals("totemExplosion")) {
                    player.getWorld().createExplosion(player.getLocation(), 4f, false, false);
                } else if (customId.equals("totemLightning")) {
                    player.getNearbyEntities(10, 10, 10).forEach(e -> {
                        if (e instanceof LivingEntity && e != player) {
                            e.getWorld().strikeLightning(e.getLocation());
                            ((LivingEntity) e).damage(5.0, player);
                        }
                    });
                }

                // Записываем время использования для кулдауна
                totemCooldowns.put(player.getUniqueId(), currentTime);
                return;
            }
        }

        Block block = event.getClickedBlock();
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && block != null && block.getType() == Material.CHEST) {
            var activeEvent = services.events().active();
            if (activeEvent != null && services.events().isCrateLocked()) {
                if (block.getLocation().distance(activeEvent.location()) < 3.5) {
                    event.setCancelled(true);
                    long seconds = services.events().getLockTimeLeft();
                    player.sendMessage(TextUtil.color("&cСундук под защитой! Осталось: &f" + seconds + " сек."));
                }
            }
        }
    }

    // ==========================================
    // УТИЛИТЫ И ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // ==========================================
    private boolean isRestricted(Player player) {
        var profile = services.economy().profile(player.getUniqueId(), player.getName());
        return profile != null && profile.isFrozen();
    }

    private void processVein(Block block, Material target, int count, ItemStack tool) {
        if (count > 64 || block.getType() != target) return;
        block.breakNaturally(tool);
        for (BlockFace face : List.of(BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST)) {
            processVein(block.getRelative(face), target, count + 1, tool);
        }
    }

    private String getCustomId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(new NamespacedKey("astrasmp", "custom_id"), PersistentDataType.STRING);
    }

    private Material getSmeltedResult(Material raw) {
        String name = raw.name();
        if (name.contains("IRON_ORE") || name.equals("RAW_IRON")) return Material.IRON_INGOT;
        if (name.contains("GOLD_ORE") || name.equals("RAW_GOLD")) return Material.GOLD_INGOT;
        if (name.contains("COPPER_ORE") || name.equals("RAW_COPPER")) return Material.COPPER_INGOT;
        return null;
    }

    private boolean isOre(Material mat) {
        return mat.name().endsWith("_ORE") || mat.name().startsWith("RAW_");
    }
}