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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PlayerListener implements Listener {
    private final AstraSMPPlugin plugin;
    private final ServiceManager services;

    // Статичные константы для NamespacedKey (не создаём объект на каждый тик)
    private static final NamespacedKey KEY_CUSTOM_ID  = new NamespacedKey("astrasmp", "custom_id");
    private static NamespacedKey KEY_MENU_COMPASS = null;   // Инициализируется в конструкторе
    private static NamespacedKey KEY_AWARD_AMOUNT = null;

    // Хранилище кулдаунов для активных тотемов (КД 15 секунд)
    private final Map<UUID, Long> totemCooldowns = new HashMap<>();

    public PlayerListener(AstraSMPPlugin plugin, ServiceManager services) {
        this.plugin = plugin;
        this.services = services;
        KEY_MENU_COMPASS = new NamespacedKey(plugin, "menu_compass");
        KEY_AWARD_AMOUNT = new NamespacedKey(plugin, "award_amount");
    }

    // ==========================================
    // ЛОГИКА ВХОДА И ПРИВЕТСТВИЯ
    // ==========================================
    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        PlayerProfile profile = services.economy().profile(p.getUniqueId(), p.getName());
        
        // --- Дерево Талантов: Гладиатор ---
        int gladiator = profile.getTalentLevel("gladiator");
        if (p.getAttribute(Attribute.MAX_HEALTH) != null) {
            p.getAttribute(Attribute.MAX_HEALTH).setBaseValue(20.0 + (gladiator * 2.0));
        }

        // --- Ежедневные Награды: Логика Стрика ---
        java.time.LocalDate today = java.time.LocalDate.now();
        String todayStr = today.toString();
        String lastLogin = profile.getLastLoginDate();
        
        if (lastLogin == null || lastLogin.isEmpty()) {
            profile.setLoginStreak(1);
            profile.setLastLoginDate(todayStr);
        } else if (!lastLogin.equals(todayStr)) {
            try {
                java.time.LocalDate lastDate = java.time.LocalDate.parse(lastLogin);
                if (lastDate.plusDays(1).equals(today)) {
                    profile.setLoginStreak(profile.getLoginStreak() + 1);
                } else {
                    profile.setLoginStreak(1);
                }
            } catch (Exception e) {
                profile.setLoginStreak(1);
            }
            profile.setLastLoginDate(todayStr);
        }

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
        meta.getPersistentDataContainer().set(KEY_MENU_COMPASS, PersistentDataType.BYTE, (byte) 1);
        compass.setItemMeta(meta);
        p.getInventory().setItem(8, compass);
    }

    private boolean isMenuCompass(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        if (!item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(KEY_MENU_COMPASS, PersistentDataType.BYTE);
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
        if (event.getEntity().getKiller() != null) services.quests().processAction(event.getEntity().getKiller(), com.astrasmp.service.QuestManager.QuestAction.KILL_MOB, event.getEntity().getType().name(), 1);
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
            services.quests().processAction(player, com.astrasmp.service.QuestManager.QuestAction.SMELT, type.name(), event.getItemAmount());
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
                processVein(block, block.getType(), new HashSet<>(), tool);
            }
        }

        Material mat = block.getType();
        if (!block.hasMetadata("placed_by_player")) {
            services.quests().processAction(player, com.astrasmp.service.QuestManager.QuestAction.MINE_BLOCK, mat.name(), 1);
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isRestricted(event.getPlayer())) { event.setCancelled(true); return; }
        event.getBlockPlaced().setMetadata("placed_by_player", new org.bukkit.metadata.FixedMetadataValue(plugin, true));

        ItemStack item = event.getItemInHand();
        if (item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(KEY_AWARD_AMOUNT, PersistentDataType.LONG)) {
            long amount = item.getItemMeta().getPersistentDataContainer().get(KEY_AWARD_AMOUNT, PersistentDataType.LONG);
            services.store().addAwardBlock(event.getBlockPlaced().getLocation(), amount);
            TextUtil.send(event.getPlayer(), "&aБлок-награда успешно установлен и активен!");
        }
    }

    // ==========================================
    // БОЙ И ОРУЖИЕ
    // ==========================================
    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            NamespacedKey godKey = new NamespacedKey(plugin, "god_mode");
            if (player.getPersistentDataContainer().getOrDefault(godKey, PersistentDataType.BYTE, (byte) 0) == 1) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
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
        } else if (customId.equals("soulOfNanda")) {
            if (victim instanceof Player victimPlayer) {
                event.setCancelled(true);
                victimPlayer.kick(net.kyori.adventure.text.Component.text(TextUtil.color("&cВы были изгнаны Душой Нанды!")));
            }
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

            if (customId != null && customId.equals("eventCompass")) {
                event.setCancelled(true);
                var activeEvent = services.events().active();
                if (activeEvent == null) {
                    player.sendActionBar(net.kyori.adventure.text.Component.text(TextUtil.color("&cВ данный момент нет активных ивентов.")));
                } else if (!player.getWorld().equals(activeEvent.location().getWorld())) {
                    player.sendActionBar(net.kyori.adventure.text.Component.text(TextUtil.color("&cИвент проходит в другом измерении.")));
                } else {
                    double distance = player.getLocation().distance(activeEvent.location());
                    org.bukkit.util.Vector toEvent = activeEvent.location().toVector().subtract(player.getLocation().toVector()).normalize();
                    org.bukkit.util.Vector direction = player.getLocation().getDirection().normalize();
                    double dot = direction.dot(toEvent);

                    if (dot > 0.85) {
                        player.sendActionBar(net.kyori.adventure.text.Component.text(TextUtil.color("&aВы смотрите прямо на ивент! Дистанция: &e" + (int) distance + " м.")));
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 2f);
                    } else if (dot > 0.0) {
                        player.sendActionBar(net.kyori.adventure.text.Component.text(TextUtil.color("&eВы смотрите в общую сторону ивента.")));
                    } else {
                        player.sendActionBar(net.kyori.adventure.text.Component.text(TextUtil.color("&cИвент находится позади вас.")));
                    }
                }
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

    /**
     * Безопасный veinMiner: не использует breakNaturally (= не запускает BlockBreakEvent),
     * вместо этого ручно удаляет блок и дропает лут. Сет visited предотвращает
     * повторный обход тех же блоков и ограничивает цепочку до 64 блоков.
     */
    private void processVein(Block block, Material target, Set<org.bukkit.Location> visited, ItemStack tool) {
        if (visited.size() >= 64) return;
        if (block.getType() != target) return;
        if (!visited.add(block.getLocation())) return; // уже обработан

        // Дропаем лут без запуска BlockBreakEvent
        Collection<ItemStack> drops = block.getDrops(tool);
        block.setType(Material.AIR, false);
        for (ItemStack drop : drops) {
            block.getWorld().dropItemNaturally(block.getLocation(), drop);
        }

        for (BlockFace face : List.of(BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST)) {
            processVein(block.getRelative(face), target, visited, tool);
        }
    }

    private String getCustomId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(KEY_CUSTOM_ID, PersistentDataType.STRING);
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

    // ==========================================
    // ТАЛАНТЫ (ПАССИВНЫЕ НАВЫКИ)
    // ==========================================
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntityTalents(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            PlayerProfile profile = services.store().profile(player.getUniqueId().toString(), player.getName());
            if (profile != null) {
                // Ассасин
                int assassin = profile.getTalentLevel("assassin");
                if (assassin > 0) {
                    event.setDamage(event.getDamage() * (1.0 + (0.05 * assassin)));
                }

                // Вампиризм
                int vampirism = profile.getTalentLevel("vampirism");
                if (vampirism > 0) {
                    if (Math.random() < (0.05 * vampirism)) {
                        double newHp = Math.min(player.getHealth() + 2.0, player.getAttribute(Attribute.MAX_HEALTH).getValue());
                        player.setHealth(newHp);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageTalents(org.bukkit.event.entity.EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            PlayerProfile profile = services.store().profile(player.getUniqueId().toString(), player.getName());
            if (profile != null) {
                // Непробиваемый (Танк)
                int tank = profile.getTalentLevel("tank");
                if (tank > 0 && Math.random() < (0.05 * tank)) {
                    event.setDamage(0);
                    player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 1f);
                }

                // Акробат
                if (event.getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.FALL) {
                    int acrobat = profile.getTalentLevel("acrobat");
                    if (acrobat > 0) {
                        event.setDamage(event.getDamage() * (1.0 - (0.10 * acrobat)));
                    }
                }
            }
        }
    }

    @EventHandler
    public void onEntityDeathTalents(EntityDeathEvent event) {
        if (event.getEntity().getKiller() != null && !(event.getEntity() instanceof Player)) {
            Player killer = event.getEntity().getKiller();
            PlayerProfile profile = services.store().profile(killer.getUniqueId().toString(), killer.getName());
            int scavenger = profile.getTalentLevel("scavenger");
            if (scavenger > 0) {
                profile.setCoins(profile.getCoins() + (scavenger * 2L));
            }
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onBowShootTalents(org.bukkit.event.entity.EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player player) {
            PlayerProfile profile = services.store().profile(player.getUniqueId().toString(), player.getName());
            int lucky = profile.getTalentLevel("lucky_shot");
            if (lucky > 0 && Math.random() < (0.10 * lucky)) {
                event.setConsumeItem(false);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreakTalents(BlockBreakEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        PlayerProfile profile = services.store().profile(player.getUniqueId().toString(), player.getName());
        int miner = profile.getTalentLevel("miner");
        if (miner > 0 && isOre(event.getBlock().getType())) {
            if (Math.random() < (0.05 * miner)) {
                for (ItemStack drop : event.getBlock().getDrops(player.getInventory().getItemInMainHand())) {
                    event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), drop);
                }
            }
        }
    }
}