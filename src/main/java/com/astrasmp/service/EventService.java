package com.astrasmp.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

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

public final class EventService {

    public enum EventType {
        METEOR("§e☄ Метеорит", "Метеорит"),
        AIRDROP("§b✈ Аирдроп", "Аирдроп"),
        BOSS("§c☠ Мировой Босс", "Мировой Босс"),
        CHAOS("§5⚡ Сундук Хаоса", "Сундук Хаоса"),
        TREASURE("§6☁ Затерянный Клад", "Затерянный Клад");

        private final String title;
        private final String cleanName;

        EventType(String title, String cleanName) {
            this.title = title;
            this.cleanName = cleanName;
        }
        public String getTitle() { return title; }
        public String getCleanName() { return cleanName; }
    }

    public record ActiveEvent(EventType type, Location location, long startedAt, long endsAt, LivingEntity boss, List<ArmorStand> holograms) {
        public long getTotalDuration() { return endsAt - startedAt; }
    }

    private final AstraSMPPlugin plugin;
    private final EconomyService economy;
    private final Random random = new Random();
    private final AtomicReference<ActiveEvent> active = new AtomicReference<>();
    private BossBar bossBar;
    private BukkitTask ticker;

    // --- ПЕРЕМЕННЫЕ ДЛЯ КРОВАВОЙ НОЧИ ---
    private boolean isBloodNightActive = false;
    private boolean timeExtended = false;
    private boolean forcedBloodNight = false;

    // Время блокировки сундука в миллисекундах (3 минуты = 180 секунд)
    private static final long CRATE_LOCK_TIME_MS = 3 * 60 * 1000L;

    public EventService(AstraSMPPlugin plugin, EconomyService economy, MMRService mmr) {
        this.plugin = plugin;
        this.economy = economy;
    }

    public boolean isBloodNight() {
        return isBloodNightActive;
    }

    public void startSchedulers() {
        for (EventType type : EventType.values()) {
            scheduleRandom("events." + type.name().toLowerCase() + ".interval-minutes", type);
        }
        startBloodNightTask();
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
                    Bukkit.broadcastMessage(TextUtil.color("&8[&bChetCraft&8] &aКровавая Ночь завершилась. Вы выжили."));
                }
            }
        }, 100L, 100L);
    }

    private void scheduleRandom(String path, EventType type) {
        long minutes = plugin.getConfig().getLong(path, 120L);
        long ticks = 20L * 60L * minutes;
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (active.get() == null) start(type, null);
        }, ticks, ticks);
    }

    public boolean start(EventType type, Player forcedBy) {
        if (active.get() != null) return false;

        World world = Bukkit.getWorld(plugin.getConfig().getString("server.world-name", "world"));
        if (world == null) world = Bukkit.getWorlds().get(0);

        int radius = plugin.getConfig().getInt("server.event-radius", 500);
        Location loc = randomLocation(world, radius);

        if (type == EventType.METEOR) {
            animateMeteor(loc);
        } else {
            completeStart(type, loc);
        }
        return true;
    }

    public boolean startAt(EventType type, Location loc) {
        if (active.get() != null) return false;

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

        if (type != EventType.BOSS) {
            dropCrate(loc, type);
        }

        switch (type) {
            case AIRDROP -> holograms.add(spawnHologram(loc.clone().add(0, 1.5, 0), "§b§lАИРДРОП"));
            case CHAOS -> holograms.add(spawnHologram(loc.clone().add(0, 1.5, 0), "§5§lЗОНА ХАОСА"));
            case TREASURE -> holograms.add(spawnHologram(loc.clone().add(0, 1.5, 0), "§6§lСОКРОВИЩА"));
            case METEOR -> holograms.add(spawnHologram(loc.clone().add(0, 1.5, 0), "§e§lМЕТЕОРИТ"));
            case BOSS -> {
                // Голограмму больше не спавним, имя привязано к самому боссу
                boss = spawnBoss(loc);
            }
        }

        ActiveEvent ev = new ActiveEvent(type, loc, now, now + durationMs, boss, holograms);
        active.set(ev);
        startBossBar(type);

        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§b§lChetCraft §8» §fНа сервере начался ивент: " + type.getTitle() + "§f!");
        Bukkit.broadcastMessage("§fКоординаты: §e" + shortLocation(loc));
        if (type != EventType.BOSS) {
            Bukkit.broadcastMessage("§fСундук под защитой! Открыть можно будет через §e3 минуты§f.");
        }
        Bukkit.broadcastMessage("");

        if (plugin.getServices().discord() != null && plugin.getServices().discord().isEnabled()) {
            plugin.getServices().discord().sendEventEmbed(type.getCleanName());
        }

        if (ticker != null) ticker.cancel();
        ticker = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    private void animateMeteor(Location target) {
        Location startPos = target.clone().add(-30, 60, -30);
        Vector direction = target.toVector().subtract(startPos.toVector()).normalize();
        announce("§eВ небе над ChetCraft замечен падающий объект...");

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
    public boolean isCrateLocked() {
        ActiveEvent ev = active.get();
        if (ev == null) return false;
        if (ev.type() == EventType.BOSS) return false;
        long elapsed = System.currentTimeMillis() - ev.startedAt();
        return elapsed < CRATE_LOCK_TIME_MS;
    }

    public long getLockTimeLeft() {
        ActiveEvent ev = active.get();
        if (ev == null) return 0;
        long unlockTime = ev.startedAt() + CRATE_LOCK_TIME_MS;
        long left = (unlockTime - System.currentTimeMillis()) / 1000L;
        return Math.max(0, left);
    }

    private void dropCrate(Location loc, EventType type) {
        loc.getBlock().setType(Material.CHEST);
        if (loc.getBlock().getState() instanceof Chest chest) {
            Inventory inv = chest.getInventory();
            inv.clear();

            // 1. Заполнение базовым пулом (от 3 до 5 слотов)
            int itemsCount = 3 + random.nextInt(3);
            for (int i = 0; i < itemsCount; i++) {
                int slot = getEmptySlot(inv);
                if (slot == -1) break; // инвентарь полный, прекращаем
                inv.setItem(slot, generateBasicLoot());
            }

            // 2. Единичный ролл на кастомный предмет для всего сундука
            double customChance = getCustomChanceByEvent(type);
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
            case CHAOS -> 0.20;    // Самый редкий ивент - 20% шанс
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
        Material[] basicLoot = {
            Material.IRON_INGOT, Material.GOLD_INGOT, 
            Material.DIAMOND, Material.EMERALD, 
            Material.EXPERIENCE_BOTTLE, Material.GOLDEN_APPLE
        };
        // Увеличен объем базового лута для рентабельности
        int amount = 3 + random.nextInt(6); 
        return new ItemStack(basicLoot[random.nextInt(basicLoot.length)], amount);
    }

    private void tick() {
        ActiveEvent ev = active.get();
        if (ev == null) return;

        // Прогресс присутствия у ивента: засчитываем раз в 60 секунд, а не каждый тик
        long now = System.currentTimeMillis();
        if ((now / 60000L) % 1 == 0) { // раз в 60 секунд
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getWorld().equals(ev.location().getWorld())) {
                    if (player.getLocation().distance(ev.location()) <= 30.0) {
                        plugin.getServices().quests().processAction(player, com.astrasmp.service.QuestManager.QuestAction.ATTEND_EVENT, "", 1);
                    }
                }
            }
        }

        if (ev.type() == EventType.BOSS && (ev.boss() == null || ev.boss().isDead())) {
            finish();
            return;
        }

        long left = ev.endsAt() - System.currentTimeMillis();
        if (left <= 0) {
            finish();
            return;
        }

        if (bossBar != null) {
            bossBar.setTitle(TextUtil.color(ev.type().getTitle() + " &7| &f" + formatTime(left)));
            double progress = (double) left / ev.getTotalDuration();
            bossBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
        }
    }

    public void finish() {
        ActiveEvent ev = active.getAndSet(null);
        if (ev == null) return;

        // 1. Удаление босса
        if (ev.boss() != null && ev.boss().isValid()) ev.boss().remove();

        // 2. Радарная зачистка голограмм (даже если чанк выгружался)
        ev.holograms().forEach(h -> {
            if (h != null && h.isValid()) h.remove();
        });

        // Дополнительная страховка: ищем все арморстенды в радиусе 5 блоков и удаляем наши голограммы
        if (ev.location().getWorld() != null && ev.location().getChunk().isLoaded()) {
            ev.location().getNearbyEntitiesByType(ArmorStand.class, 5).forEach(as -> {
                if (as.isMarker() && !as.isVisible()) as.remove();
            });

            // 3. Зачистка сундука после окончания ивента
            if (ev.type() != EventType.BOSS) {
                ev.location().getBlock().setType(Material.AIR);
            }
        }

        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }
        if (ticker != null) { ticker.cancel(); ticker = null; }

        Bukkit.broadcast(Component.text("§8[§bChetCraft§8] §aИвент " + ev.type().getCleanName() + " завершен."));
        rewardNearby(ev.location());
    }

    public ActiveEvent active() { return active.get(); }

    public void addPlayerToBossBar(Player player) {
        if (bossBar != null) bossBar.addPlayer(player);
    }

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
            entity.setCustomName("§c☠ Страж ChetCraft");
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

    private Location randomLocation(World world, int radius) {
        int x = random.nextInt(radius * 2) - radius;
        int z = random.nextInt(radius * 2) - radius;
        return world.getHighestBlockAt(x, z).getLocation().add(0.5, 1, 0.5);
    }

    private void startBossBar(EventType type) {
        bossBar = Bukkit.createBossBar(TextUtil.color(type.getTitle()), BarColor.YELLOW, BarStyle.SEGMENTED_10);
        Bukkit.getOnlinePlayers().forEach(bossBar::addPlayer);
    }

    private void announce(String msg) {
        Bukkit.broadcast(Component.text("§8[§bChetCraft§8] §f" + msg));
    }

    public void rewardNearby(Location location) {
        double radiusSq = 50.0 * 50.0;
        for (Player player : location.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(location) <= radiusSq) {
                PlayerProfile profile = economy.profile(player.getUniqueId(), player.getName());

                profile.setCoins(profile.getCoins() + 200L);
                profile.setEventPoints(profile.getEventPoints() + 10);

                TextUtil.send(player, "&a+200 ❂ и 10 очков ивента за участие!");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);

                plugin.getServices().quests().processAction(player, com.astrasmp.service.QuestManager.QuestAction.ATTEND_EVENT, "", 1);
            }
        }
    }
}