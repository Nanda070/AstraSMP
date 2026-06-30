package com.astrasmp.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Chest;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import com.astrasmp.AstraSMPPlugin;
import com.astrasmp.items.ItemRegistry;
import com.astrasmp.model.PlayerProfile;
import com.astrasmp.util.TextUtil;

import net.kyori.adventure.text.Component;

public final class EventService implements org.bukkit.event.Listener {

    public enum EventType {
        METEOR("meteor"),
        AIRDROP("airdrop"),
        BOSS("boss"),
        MERCHANT("merchant"),
        GALLEON("galleon"),
        TREASURE("treasure");

        private final String key;

        EventType(String key) {
            this.key = key;
        }
        public String getTitle() { 
            return AstraSMPPlugin.getInstance().getConfigManager().getMessage("events.types." + key + ".title", "§e" + name()); 
        }
        public String getCleanName() { 
            return AstraSMPPlugin.getInstance().getConfigManager().getMessage("events.types." + key + ".cleanName", name()); 
        }
    }

    public enum EventModifier {
        NONE("none"),
        GENEROUS("generous"),
        CURSED("cursed"),
        FAST("fast");

        private final String key;

        EventModifier(String key) {
            this.key = key;
        }
        public String getPrefix() { 
            if (this == NONE) return "";
            return AstraSMPPlugin.getInstance().getConfigManager().getMessage("events.modifiers." + key + ".prefix", "§7[" + name() + "] "); 
        }
        public String getCleanName() { 
            if (this == NONE) return "";
            return AstraSMPPlugin.getInstance().getConfigManager().getMessage("events.modifiers." + key + ".cleanName", name()); 
        }
    }

    public record ActiveEvent(EventType type, EventModifier modifier, Location location, long startedAt, long endsAt, LivingEntity boss, List<ArmorStand> holograms, BossBar bossBar) {
        public long getTotalDuration() { return endsAt - startedAt; }
    }

    private final AstraSMPPlugin plugin;
    private final EconomyService economy;
    private final Random random = new Random();
    private final java.util.concurrent.CopyOnWriteArrayList<ActiveEvent> activeEvents = new java.util.concurrent.CopyOnWriteArrayList<>();
    private BukkitTask ticker;
    private int tickCounter = 0;

    // --- ПЕРЕМЕННЫЕ ДЛЯ КРОВАВОЙ НОЧИ ---
    private boolean isBloodNightActive = false;
    private boolean timeExtended = false;
    private boolean forcedBloodNight = false;

    // Время блокировки сундука в миллисекундах (3 минуты = 180 секунд)
    private long getCrateLockTimeMs() {
        return plugin.getConfig().getLong("events.crate-lock-time-seconds", 180) * 1000L;
    }

    public EventService(AstraSMPPlugin plugin, EconomyService economy, MMRService mmr) {
        this.plugin = plugin;
        this.economy = economy;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public boolean isBloodNight() {
        return isBloodNightActive;
    }

    public void startSchedulers() {
        cleanupAllHolograms();
        for (EventType type : EventType.values()) {
            scheduleRandom("events." + type.name().toLowerCase() + ".interval-minutes", type);
        }
        startBloodNightTask();
    }

    public void cleanupAllHolograms() {
        World world = Bukkit.getWorld(plugin.getConfig().getString("server.world-name", "world"));
        if (world != null) {
            for (org.bukkit.entity.ArmorStand as : world.getEntitiesByClass(org.bukkit.entity.ArmorStand.class)) {
                if (isEventHologram(as)) as.remove();
            }
        }
    }

    private boolean isEventHologram(ArmorStand as) {
        if (!as.isMarker() || as.isVisible()) return false;
        if (as.customName() == null) return false;
        String name = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(as.customName());
        
        String lower = name.toLowerCase();
        return lower.contains("аирдроп") || lower.contains("сокровища") || lower.contains("метеорит") 
            || lower.contains("заблокировано") || lower.contains("открыт") || lower.contains("галеон");
    }

    @org.bukkit.event.EventHandler
    public void onChunkLoad(org.bukkit.event.world.ChunkLoadEvent event) {
        for (org.bukkit.entity.Entity ent : event.getChunk().getEntities()) {
            if (ent instanceof ArmorStand as) {
                if (isEventHologram(as)) {
                    boolean keep = false;
                    for (ActiveEvent ev : activeEvents) {
                        if (ev.holograms().contains(as)) {
                            keep = true;
                            break;
                        }
                    }
                    if (keep) continue;
                    as.remove();
                }
            }
        }
    }

    // ==========================================
    // ЛОГИКА КРОВАВОЙ НОЧИ (BLOOD NIGHT)
    // ==========================================
    public void toggleBloodNight() {
        World world = Bukkit.getWorld(plugin.getConfig().getString("server.world-name", "world"));
        if (world == null) return;

        if (!isBloodNightActive) {
            this.forcedBloodNight = true;
            this.timeExtended = false;
            world.setTime(13500);
        } else {
            this.forcedBloodNight = false;
            world.setTime(24000);
        }
    }

    @SuppressWarnings("deprecation")
    private void startBloodNightTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            World world = Bukkit.getWorld(plugin.getConfig().getString("server.world-name", "world"));
            if (world == null) return;

            long fullTime = world.getFullTime();
            long days = fullTime / 24000;
            long time = world.getTime();

            boolean isNatural = (days > 0 && days % 5 == 0);
            boolean isNightTime = (time > 13000 && time < 23500);

            if ((isNatural && isNightTime) || (forcedBloodNight && isNightTime)) {
                if (!isBloodNightActive) {
                    isBloodNightActive = true;
                    timeExtended = false;
                    Bukkit.broadcastMessage(TextUtil.color("&4&lВНИМАНИЕ! &cНачалась Кровавая Ночь. Монстры усилились, их стало больше!"));
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1f, 0.5f);
                        p.sendTitle(TextUtil.color("&4&lBLOOD NIGHT"), TextUtil.color("&cОни идут за вами..."), 10, 70, 20);
                    }
                }
                if (time > 22000 && !timeExtended) {
                    world.setTime(16000);
                    timeExtended = true;
                }
            } else {
                if (isBloodNightActive) {
                    isBloodNightActive = false;
                    forcedBloodNight = false;
                    String prefix = TextUtil.color(plugin.getConfig().getString("messages.prefix", "&8[&dChetCraft&8] &7"));
                    Bukkit.broadcastMessage(prefix + TextUtil.color("&aКровавая Ночь завершилась. Вы выжили."));
                }
            }
        }, 100L, 100L);
    }

    private void scheduleRandom(String path, EventType type) {
        long minutes = plugin.getConfig().getLong(path, 120L);
        long ticks = 20L * 60L * minutes;
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            start(type, null);
        }, ticks, ticks);
    }

    public boolean start(EventType type, Player forcedBy) {
        if (activeEvents.size() >= 2) return false;
        for (ActiveEvent ev : activeEvents) {
            if (ev.type() == type) return false;
        }

        World world = Bukkit.getWorld(plugin.getConfig().getString("server.world-name", "world"));
        if (world == null) world = Bukkit.getWorlds().get(0);

        int radius = plugin.getConfig().getInt("server.event-radius", 500);
        Location loc = type == EventType.GALLEON ? findOceanLocation(world, radius) : findSafeLocation(world, radius);

        if (type == EventType.METEOR) {
            animateMeteor(loc);
        } else {
            completeStart(type, loc);
        }
        return true;
    }

    public boolean startAt(EventType type, Location loc) {
        if (activeEvents.size() >= 2) return false;
        for (ActiveEvent ev : activeEvents) {
            if (ev.type() == type) return false;
        }

        if (type == EventType.METEOR) {
            animateMeteor(loc);
        } else {
            completeStart(type, loc);
        }
        return true;
    }

    @SuppressWarnings("deprecation")
    private void completeStart(EventType type, Location loc) {
        LivingEntity boss = null;
        List<ArmorStand> holograms = new ArrayList<>();
        long durationMs = durationFor(type);
        long now = System.currentTimeMillis();

        if (type == EventType.TREASURE) {
            int y = loc.getWorld().getHighestBlockYAt(loc) - (5 + random.nextInt(5));
            loc.setY(y);
        }

        EventModifier modifier = EventModifier.NONE;
        if (type != EventType.BOSS && type != EventType.MERCHANT && type != EventType.GALLEON) {
            if (random.nextDouble() < 0.20) {
                EventModifier[] values = {EventModifier.GENEROUS, EventModifier.CURSED, EventModifier.FAST};
                modifier = values[random.nextInt(values.length)];
                if (type != EventType.AIRDROP && modifier == EventModifier.FAST) {
                    modifier = EventModifier.GENEROUS;
                }
            }
        }

        if (type != EventType.BOSS && type != EventType.MERCHANT) {
            dropCrate(loc, type, modifier);
        }

        switch (type) {
            case AIRDROP -> holograms.add(spawnHologram(loc.clone().add(0, 1.5, 0), "§b§lАИРДРОП"));
            case TREASURE -> holograms.add(spawnHologram(loc.clone().add(0, 1.5, 0), "§6§lСОКРОВИЩА"));
            case METEOR -> holograms.add(spawnHologram(loc.clone().add(0, 1.5, 0), "§e§lМЕТЕОРИТ"));
            case GALLEON -> holograms.add(spawnHologram(loc.clone().add(0, 1.5, 0), "§3§lЗАБЛОКИРОВАНО"));
            case BOSS -> {
                boss = spawnBoss(loc);
            }
            case MERCHANT -> {
                boss = plugin.getServices().shops().spawnNpcAt(loc, "9");
            }
        }

        if (type == EventType.GALLEON) {
            for (int i = 0; i < 4; i++) {
                loc.getWorld().spawn(loc.clone().add(random.nextInt(6)-3, 1, random.nextInt(6)-3), org.bukkit.entity.Drowned.class, entity -> {
                    entity.setCustomName("§3Страж Глубин");
                    entity.setCustomNameVisible(true);
                    var attr = entity.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
                    if (attr != null) attr.setBaseValue(60.0);
                    entity.setHealth(60.0);
                    var equipment = entity.getEquipment();
                    if (equipment != null) equipment.setItemInMainHand(new ItemStack(Material.TRIDENT));
                });
            }
        }

        if (type == EventType.METEOR) {
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    if (random.nextDouble() < 0.3) {
                        Location fLoc = loc.clone().add(dx, 0, dz);
                        if (fLoc.getBlock().getType().isAir()) fLoc.getBlock().setType(Material.FIRE);
                    }
                }
            }
            for (int i = 0; i < 3; i++) {
                loc.getWorld().spawn(loc.clone().add(random.nextInt(4)-2, 1, random.nextInt(4)-2), org.bukkit.entity.Blaze.class, entity -> {
                    entity.setCustomName("§cСтраж Метеорита");
                    entity.setCustomNameVisible(true);
                    var attr = entity.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
                    if (attr != null) attr.setBaseValue(100.0);
                    entity.setHealth(100.0);
                });
            }
        }

        BossBar bar = Bukkit.createBossBar(TextUtil.color(type.getTitle()), BarColor.YELLOW, BarStyle.SEGMENTED_10);
        ActiveEvent ev = new ActiveEvent(type, modifier, loc, now, now + durationMs, boss, holograms, bar);
        activeEvents.add(ev);

        String title = modifier.getPrefix() + type.getTitle();
        Bukkit.broadcastMessage("");
        String prefix = TextUtil.color(plugin.getConfig().getString("messages.prefix", "&8[&dChetCraft&8] &7"));
        Bukkit.broadcastMessage(prefix + "§fНа сервере начался ивент: " + title + "§f!");
        
        if (type == EventType.TREASURE) {
            // Скрываем точные координаты для клада
            int approxX = (loc.getBlockX() / 50) * 50 + (random.nextInt(20) - 10);
            int approxZ = (loc.getBlockZ() / 50) * 50 + (random.nextInt(20) - 10);
            
            net.kyori.adventure.text.Component msg = net.kyori.adventure.text.Component.text(TextUtil.color("&fПримерные координаты: &eX: ~" + approxX + " Z: ~" + approxZ))
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(net.kyori.adventure.text.Component.text(TextUtil.color("&aНажмите, чтобы скопировать"))))
                .clickEvent(net.kyori.adventure.text.event.ClickEvent.copyToClipboard("X: ~" + approxX + " Z: ~" + approxZ));
            Bukkit.broadcast(msg);
            
            Bukkit.broadcastMessage("§fСундук зарыт под землей. Ищите!");
        } else {
            net.kyori.adventure.text.Component msg = net.kyori.adventure.text.Component.text(TextUtil.color("&fКоординаты: &e" + shortLocation(loc)))
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(net.kyori.adventure.text.Component.text(TextUtil.color("&aНажмите, чтобы скопировать"))))
                .clickEvent(net.kyori.adventure.text.event.ClickEvent.copyToClipboard(loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ()));
            Bukkit.broadcast(msg);
            
            if (type == EventType.AIRDROP || type == EventType.GALLEON) {
                long lockMinutes = getLockTimeMs(modifier) / 60000L;
                Bukkit.broadcastMessage("§fСундук под защитой! Открыть можно будет через §e" + lockMinutes + " минуты§f.");
            }
        }
        Bukkit.broadcastMessage("");

        if (plugin.getServices().discord() != null && plugin.getServices().discord().isEnabled()) {
            plugin.getServices().discord().sendEventEmbed(type.getCleanName());
        }

        if (ticker == null || ticker.isCancelled()) {
            ticker = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
        }
    }

    private void animateMeteor(Location target) {
        Location startPos = target.clone().add(-30, 60, -30);
        Vector direction = target.toVector().subtract(startPos.toVector()).normalize();
        announce("§eВ небе над миром замечен падающий объект...");

        new BukkitRunnable() {
            Location current = startPos.clone();
            int ticks = 0;
            @Override
            public void run() {
                if (ticks++ > 100 || current.distance(target) < 2.0) {
                    target.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, target, 1);
                    target.getWorld().playSound(target, Sound.ENTITY_GENERIC_EXPLODE, 3f, 0.5f);
                    completeStart(EventType.METEOR, target);
                    this.cancel();
                    return;
                }
                current.add(direction.clone().multiply(1.5));
                current.getWorld().spawnParticle(Particle.FLAME, current, 10, 0.2, 0.2, 0.2, 0.05);
                if (ticks % 4 == 0) current.getWorld().playSound(current, Sound.ENTITY_GHAST_SHOOT, 0.5f, 0.5f);
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    // ==========================================
    // ЛОГИКА ЗАЩИТЫ СУНДУКА И ЛУТА
    // ==========================================
    public long getLockTimeMs(EventModifier modifier) {
        if (modifier == EventModifier.FAST) return 60 * 1000L; // 1 минута
        return getCrateLockTimeMs(); // 3 минуты
    }

    public ActiveEvent getLockedEventAt(Location loc) {
        for (ActiveEvent ev : activeEvents) {
            if (ev.type() == EventType.AIRDROP || ev.type() == EventType.GALLEON) {
                if (ev.location().getBlockX() == loc.getBlockX() && ev.location().getBlockY() == loc.getBlockY() && ev.location().getBlockZ() == loc.getBlockZ()) {
                    long elapsed = System.currentTimeMillis() - ev.startedAt();
                    if (elapsed < getLockTimeMs(ev.modifier())) return ev;
                }
            }
        }
        return null;
    }

    public boolean isCrateLocked() { // Legacy support, better use getLockedEventAt
        ActiveEvent ev = active();
        if (ev == null) return false;
        if (ev.type() != EventType.AIRDROP && ev.type() != EventType.GALLEON) return false;
        long elapsed = System.currentTimeMillis() - ev.startedAt();
        return elapsed < getLockTimeMs(ev.modifier());
    }

    public long getLockTimeLeft(ActiveEvent ev) {
        if (ev == null) return 0;
        long unlockTime = ev.startedAt() + getLockTimeMs(ev.modifier());
        long left = (unlockTime - System.currentTimeMillis()) / 1000L;
        return Math.max(0, left);
    }
    
    public long getLockTimeLeft() { return getLockTimeLeft(active()); }

    private void dropCrate(Location loc, EventType type, EventModifier modifier) {
        loc.getBlock().setType(Material.CHEST);
        if (loc.getBlock().getState() instanceof Chest chest) {
            Inventory inv = chest.getInventory();
            inv.clear();

            // 1. Заполнение базовым пулом
            int itemsCount = 3 + random.nextInt(3);
            if (modifier == EventModifier.GENEROUS) itemsCount *= 2; // Щедрый мутатор

            for (int i = 0; i < itemsCount; i++) {
                int slot = getEmptySlot(inv);
                if (slot == -1) break; // инвентарь полный, прекращаем
                inv.setItem(slot, generateBasicLoot());
            }

            // 2. Единичный ролл на кастомный предмет для всего сундука
            double customChance = getCustomChanceByEvent(type);
            if (modifier == EventModifier.CURSED) customChance *= 3.0; // Проклятый увеличивает шанс в 3 раза

            if (random.nextDouble() <= customChance) {
                int slot = getEmptySlot(inv);
                if (slot != -1) inv.setItem(slot, generateCustomLoot());
            }
        }
    }

    private int getEmptySlot(Inventory inv) {
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) return i;
        }
        return -1; // инвентарь полный
    }

    private double getCustomChanceByEvent(EventType type) {
        // Шансы масштабируются в зависимости от редкости/сложности ивента
        return switch (type) {
            case MERCHANT -> 0.0;  // Торговец не использует сундуки
            case GALLEON -> 0.20;  // Галеон - высокий шанс
            case METEOR -> 0.12;
            case BOSS -> 0.0;      // У босса свой лут
            case TREASURE -> 0.08;
            case AIRDROP -> 0.05;  // Частый ивент - низкий шанс (5%)
        };
    }

    private ItemStack generateCustomLoot() {
        List<ItemStack> customs = new ArrayList<>(ItemRegistry.showcase());
        
        // Артефакты интегрируются прямо в выборку
        customs.add(ItemRegistry.relic("time_core", Material.CLOCK, "§dЯдро времени", "Замедляет время вокруг владельца."));
        customs.add(ItemRegistry.artifact("heart_of_world", Material.HEART_OF_THE_SEA, "§bСердце мира", "Пассивная защита для носителя."));

        return customs.get(random.nextInt(customs.size())).clone();
    }

    private ItemStack generateBasicLoot() {
        List<String> basicLootKeys = plugin.getConfig().getStringList("events.loot.basic");
        if (basicLootKeys.isEmpty()) {
            basicLootKeys = List.of("IRON_INGOT", "GOLD_INGOT", "DIAMOND", "EMERALD");
        }
        String chosen = basicLootKeys.get(random.nextInt(basicLootKeys.size()));
        Material mat = Material.matchMaterial(chosen);
        if (mat == null) mat = Material.IRON_INGOT;

        // Увеличен объем базового лута для рентабельности
        int amount = 3 + random.nextInt(6); 
        return new ItemStack(mat, amount);
    }

    private void spawnGuards(ActiveEvent ev) {
        Location loc = ev.location();
        int amount = random.nextInt(2) + 1;
        for (int i = 0; i < amount; i++) {
            Location spawnLoc = loc.clone().add(random.nextInt(10) - 5, 1, random.nextInt(10) - 5);
            if (spawnLoc.getBlock().getType().isAir()) {
                if (ev.type() == EventType.GALLEON) {
                    loc.getWorld().spawn(spawnLoc, org.bukkit.entity.Drowned.class, entity -> {
                        entity.customName(Component.text("§cСтраж груза"));
                        entity.setCustomNameVisible(true);
                    });
                } else {
                    loc.getWorld().spawn(spawnLoc, org.bukkit.entity.Zombie.class, entity -> {
                        entity.customName(Component.text("§cСтраж груза"));
                        entity.setCustomNameVisible(true);
                    });
                }
            }
        }
    }

    private void tick() {
        if (activeEvents.isEmpty()) return;

        long now = System.currentTimeMillis();
        tickCounter++;
        boolean doMinuteTick = (tickCounter % 60 == 0);

        // 1. Управление видимостью BossBar (к ближайшему ивенту)
        for (Player p : Bukkit.getOnlinePlayers()) {
            ActiveEvent nearest = null;
            double minDist = Double.MAX_VALUE;
            
            for (ActiveEvent ev : activeEvents) {
                if (ev.location().getWorld().equals(p.getWorld())) {
                    double dist = p.getLocation().distanceSquared(ev.location());
                    if (dist < minDist) {
                        minDist = dist;
                        nearest = ev;
                    }
                }
            }
            
            if (nearest == null && !activeEvents.isEmpty()) {
                nearest = activeEvents.get(0);
            }

            for (ActiveEvent ev : activeEvents) {
                if (ev == nearest) {
                    if (!ev.bossBar().getPlayers().contains(p)) ev.bossBar().addPlayer(p);
                } else {
                    if (ev.bossBar().getPlayers().contains(p)) ev.bossBar().removePlayer(p);
                }
            }
        }

        // 2. Обработка каждого активного ивента
        for (ActiveEvent ev : new ArrayList<>(activeEvents)) {
            boolean finished = false;

            if (doMinuteTick) { 
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getWorld().equals(ev.location().getWorld()) && player.getLocation().distance(ev.location()) <= 30.0) {
                        plugin.getServices().quests().processAction(player, com.astrasmp.service.QuestManager.QuestAction.ATTEND_EVENT, "", 1);
                    }
                }
            }

            if (ev.modifier() == EventModifier.CURSED && (now / 1000L) % 2 == 0) {
                for (Player p : ev.location().getWorld().getPlayers()) {
                    if (p.getLocation().distanceSquared(ev.location()) < 20 * 20) {
                        p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS, 60, 0));
                        p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 60, 1));
                    }
                }
            }

            if (ev.type() == EventType.TREASURE) {
                for (Player p : ev.location().getWorld().getPlayers()) {
                    double dist = p.getLocation().distance(ev.location());
                    if (dist < 250.0) {
                        String color = dist < 50 ? "§a" : (dist < 150 ? "§e" : "§c");
                        p.sendActionBar(Component.text("§6🧭 Сигнал клада: " + color + String.format("%.1f", dist) + " м."));
                    }
                }
            }

            if (ev.type() == EventType.AIRDROP || ev.type() == EventType.GALLEON) {
                long leftLock = getLockTimeLeft(ev);
                if (!ev.holograms().isEmpty()) {
                    org.bukkit.entity.ArmorStand holo = ev.holograms().get(0);
                    if (leftLock > 0) {
                        holo.customName(Component.text(TextUtil.color("&b&l" + ev.type().getCleanName().toUpperCase() + " &7| &f" + formatTime(leftLock * 1000L))));
                        if (random.nextInt(10) == 0) spawnGuards(ev);
                    } else {
                        holo.customName(Component.text(TextUtil.color("&a&lОТКРЫТ")));
                    }
                }
            }

            if (ev.type() == EventType.BOSS && ev.boss() != null) {
                if (ev.boss().isDead()) {
                    Location deathLoc = ev.boss().getLocation();
                    org.bukkit.entity.Firework fw = deathLoc.getWorld().spawn(deathLoc, org.bukkit.entity.Firework.class);
                    org.bukkit.inventory.meta.FireworkMeta meta = fw.getFireworkMeta();
                    meta.addEffect(org.bukkit.FireworkEffect.builder().withColor(org.bukkit.Color.RED).withFade(org.bukkit.Color.YELLOW).with(org.bukkit.FireworkEffect.Type.BALL_LARGE).build());
                    meta.setPower(1);
                    fw.setFireworkMeta(meta);
                    finished = true;
                } else if (random.nextInt(12) == 0) {
                    useBossSkill(ev.boss(), ev.location());
                }
            }

            long left = ev.endsAt() - System.currentTimeMillis();
            if (left <= 0 || (ev.type() == EventType.BOSS && ev.boss() == null)) {
                finished = true;
            }

            if (!finished) {
                ev.bossBar().setTitle(TextUtil.color(ev.type().getTitle() + " &7| &f" + formatTime(left)));
                double progress = (double) left / ev.getTotalDuration();
                ev.bossBar().setProgress(Math.max(0.0, Math.min(1.0, progress)));
            } else {
                finishEvent(ev);
            }
        }
    }

    public void finish() {
        for (ActiveEvent ev : new ArrayList<>(activeEvents)) {
            finishEvent(ev);
        }
    }

    public void finishEvent(ActiveEvent ev) {
        if (!activeEvents.remove(ev)) return;

        if (ev.boss() != null && ev.boss().isValid()) ev.boss().remove();

        ev.holograms().forEach(h -> { if (h != null && h.isValid()) h.remove(); });

        if (ev.location().getWorld() != null && ev.location().getChunk().isLoaded()) {
            ev.location().getNearbyEntitiesByType(ArmorStand.class, 5).forEach(as -> {
                if (as.isMarker() && !as.isVisible()) as.remove();
            });

            if (ev.type() != EventType.BOSS) {
                ev.location().getBlock().setType(Material.AIR);
            }
        }

        if (ev.bossBar() != null) {
            ev.bossBar().removeAll();
        }

        if (activeEvents.isEmpty() && ticker != null) {
            ticker.cancel();
            ticker = null;
        }

        String prefix = TextUtil.color(plugin.getConfig().getString("messages.prefix", "&8[&dChetCraft&8] &7"));
        Bukkit.broadcast(Component.text(prefix + "§aИвент " + ev.type().getCleanName() + " завершен."));
        rewardNearby(ev.location());
    }

    public ActiveEvent active() { return activeEvents.isEmpty() ? null : activeEvents.get(0); }

    public void addPlayerToBossBar(Player player) { }

    private long durationFor(EventType type) {
        return plugin.getConfig().getLong("events." + type.name().toLowerCase() + ".duration-minutes", 15L) * 60L * 1000L;
    }

    @SuppressWarnings("deprecation")
    private ArmorStand spawnHologram(Location loc, String text) {
        return loc.getWorld().spawn(loc, ArmorStand.class, as -> {
            as.setVisible(false);
            as.setGravity(false);
            as.setCustomName(text);
            as.setCustomNameVisible(true);
            as.setMarker(true);
        });
    }

    @SuppressWarnings("deprecation")
    private LivingEntity spawnBoss(Location loc) {
        return (WitherSkeleton) loc.getWorld().spawn(loc, WitherSkeleton.class, entity -> {
            // Титул теперь привязан к самому боссу
            String bossName = TextUtil.color(plugin.getConfig().getString("messages.boss-name", "§c☠ Страж"));
            entity.setCustomName(bossName);
            entity.setCustomNameVisible(true);

            // ХП уменьшено до 350
            var healthAttr = entity.getAttribute(Attribute.MAX_HEALTH);
            if (healthAttr != null) healthAttr.setBaseValue(350.0);
            entity.setHealth(350.0);

            // Экипировка (Сет Джаггернаута + Меч Инферно)
            var equipment = entity.getEquipment();
            if (equipment != null) {
                equipment.setItemInMainHand(ItemRegistry.infernoSword());
                equipment.setHelmet(ItemRegistry.juggernautHelmet());
                equipment.setChestplate(ItemRegistry.juggernautChestplate());
                equipment.setLeggings(ItemRegistry.juggernautLeggings());
                equipment.setBoots(ItemRegistry.juggernautBoots());

                // Небольшой шанс выбить крутые вещи с босса
                equipment.setItemInMainHandDropChance(0.05f);
                equipment.setHelmetDropChance(0.02f);
                equipment.setChestplateDropChance(0.02f);
            }
        });
    }

    private String shortLocation(Location loc) {
        return "X: " + loc.getBlockX() + " Y: " + loc.getBlockY() + " Z: " + loc.getBlockZ();
    }

    private String formatTime(long ms) {
        long sec = ms / 1000L;
        return String.format("%02d:%02d", sec / 60L, sec % 60L);
    }

    private final java.util.LinkedList<Location> recentLocations = new java.util.LinkedList<>();
    private static final int MAX_RECENT = 5;
    private static final int MIN_DISTANCE = 300;

    private boolean isTooClose(Location loc) {
        for (Location recent : recentLocations) {
            if (recent.getWorld().equals(loc.getWorld()) && recent.distance(loc) < MIN_DISTANCE) {
                return true;
            }
        }
        return false;
    }

    private int[] generateEventCoords(World world, int radius, List<Player> players) {
        int x, z;
        if (!players.isEmpty()) {
            Player p = players.get(random.nextInt(players.size()));
            int dist = 150 + random.nextInt(350); // Спавн в радиусе 150-500 блоков от игрока
            double angle = random.nextDouble() * 2 * Math.PI;
            x = p.getLocation().getBlockX() + (int)(Math.cos(angle) * dist);
            z = p.getLocation().getBlockZ() + (int)(Math.sin(angle) * dist);
        } else {
            x = random.nextInt(radius * 2) - radius;
            z = random.nextInt(radius * 2) - radius;
        }

        // Ограничение по границе мира (отступ 10 блоков от края для страховки)
        org.bukkit.WorldBorder border = world.getWorldBorder();
        double borderSize = border.getSize() / 2.0;
        int centerX = border.getCenter().getBlockX();
        int centerZ = border.getCenter().getBlockZ();
        
        int minX = (int) (centerX - borderSize + 10);
        int maxX = (int) (centerX + borderSize - 10);
        int minZ = (int) (centerZ - borderSize + 10);
        int maxZ = (int) (centerZ + borderSize - 10);
        
        x = Math.max(minX, Math.min(maxX, x));
        z = Math.max(minZ, Math.min(maxZ, z));

        return new int[]{x, z};
    }

    private Location findSafeLocation(World world, int radius) {
        List<Player> players = new ArrayList<>(world.getPlayers());
        for (int i = 0; i < 50; i++) {
            int[] coords = generateEventCoords(world, radius, players);
            int x = coords[0];
            int z = coords[1];

            // Запрет спавна около спавна (в радиусе 150 блоков от 0,0)
            if (Math.abs(x) < 150 && Math.abs(z) < 150) continue;

            org.bukkit.block.Block highest = world.getHighestBlockAt(x, z);
            Location loc = highest.getLocation().add(0.5, 1, 0.5);

            Material type = highest.getType();
            if (type == Material.WATER || type == Material.LAVA || type.name().contains("LEAVES") || type.name().contains("LOG")) {
                continue;
            }
            if (isTooClose(loc)) continue;
            
            recentLocations.addFirst(loc);
            if (recentLocations.size() > MAX_RECENT) recentLocations.removeLast();
            return loc;
        }
        // Fallback
        int x = random.nextInt(radius * 2) - radius;
        int z = random.nextInt(radius * 2) - radius;
        // Даже в фолбэке стараемся избегать 0,0, но если совсем плохо, берем как есть
        if (Math.abs(x) < 150 && Math.abs(z) < 150) {
            x += 300;
            z += 300;
        }
        return world.getHighestBlockAt(x, z).getLocation().add(0.5, 1, 0.5);
    }

    private Location findOceanLocation(World world, int radius) {
        List<Player> players = new ArrayList<>(world.getPlayers());
        for (int i = 0; i < 50; i++) {
            int[] coords = generateEventCoords(world, radius, players);
            int x = coords[0];
            int z = coords[1];

            if (Math.abs(x) < 150 && Math.abs(z) < 150) continue;

            org.bukkit.block.Block highest = world.getHighestBlockAt(x, z);
            
            if (highest.getType() != Material.WATER) continue;
            
            org.bukkit.block.Block current = highest;
            while (current.getType() == Material.WATER && current.getY() > world.getMinHeight()) {
                current = current.getRelative(org.bukkit.block.BlockFace.DOWN);
            }
            Location loc = current.getLocation().add(0.5, 1, 0.5);
            if (isTooClose(loc)) continue;
            
            recentLocations.addFirst(loc);
            if (recentLocations.size() > MAX_RECENT) recentLocations.removeLast();
            return loc;
        }
        return findSafeLocation(world, radius);
    }



    private void useBossSkill(LivingEntity boss, Location loc) {
        String bossName = TextUtil.color(plugin.getConfig().getString("messages.boss-name", "§c☠ Страж"));
        int skill = random.nextInt(3);
        switch (skill) {
            case 0 -> { // Притягивание
                announce(bossName + " §fпритягивает врагов!");
                for (Player p : loc.getWorld().getPlayers()) {
                    if (p.getLocation().distanceSquared(loc) < 20 * 20) {
                        org.bukkit.util.Vector dir = loc.toVector().subtract(p.getLocation().toVector()).normalize().multiply(1.5);
                        p.setVelocity(dir);
                    }
                }
            }
            case 1 -> { // Отбрасывание
                announce(bossName + " §fотбрасывает всех!");
                loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 2);
                loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2f, 1f);
                for (Player p : loc.getWorld().getPlayers()) {
                    if (p.getLocation().distanceSquared(loc) < 10 * 10) {
                        org.bukkit.util.Vector dir = p.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(1.5).setY(0.5);
                        p.setVelocity(dir);
                    }
                }
            }
            case 2 -> { // Призыв миньонов
                announce(bossName + " §fпризывает помощников!");
                for (int i = 0; i < 3; i++) {
                    loc.getWorld().spawn(loc.clone().add(random.nextInt(4)-2, 0, random.nextInt(4)-2), org.bukkit.entity.Skeleton.class, entity -> {
                        entity.customName(Component.text(TextUtil.color("&7Слуга Стража")));
                        entity.setCustomNameVisible(true);
                        var equipment = entity.getEquipment();
                        if (equipment != null) equipment.setHelmet(new ItemStack(Material.IRON_HELMET));
                    });
                }
            }
        }
    }

    private void announce(String msg) {
        String prefix = TextUtil.color(plugin.getConfig().getString("messages.prefix", "&8[&dChetCraft&8] &7"));
        Bukkit.broadcast(Component.text(prefix + msg));
    }

    public void rewardNearby(Location location) {
        double radiusSq = 50.0 * 50.0;
        for (Player player : location.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(location) <= radiusSq) {
                PlayerProfile profile = economy.profile(player.getUniqueId(), player.getName());

                profile.setCoins(profile.getCoins() + 200L);
                profile.setEventPoints(profile.getEventPoints() + 10);

                TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_7d724b", "&a+200 ❂ и 10 очков ивента за участие!"));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);

                plugin.getServices().quests().processAction(player, com.astrasmp.service.QuestManager.QuestAction.ATTEND_EVENT, "", 1);
            }
        }
    }
}
