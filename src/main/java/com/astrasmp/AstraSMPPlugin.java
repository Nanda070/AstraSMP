package com.astrasmp;

import com.astrasmp.commands.*;
import com.astrasmp.database.DatabaseService;
import com.astrasmp.discord.DiscordBridge;
import com.astrasmp.items.ItemRegistry;
import com.astrasmp.listener.*;
import com.astrasmp.service.RecipeService;
import com.astrasmp.service.ServiceManager;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class AstraSMPPlugin extends JavaPlugin {

    private static AstraSMPPlugin instance;

    private ServiceManager services;
    private DatabaseService database;

    @Override
    public org.bukkit.generator.ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        if ("astrasmp_pockets".equals(worldName)) {
            return new com.astrasmp.util.VoidChunkGenerator();
        }
        return super.getDefaultWorldGenerator(worldName, id);
    }

    @Override
    public void onEnable() {
        instance = this;

        ItemRegistry.init(this);

        saveDefaultConfig();
        saveResource("messages.yml", false);

        database = new DatabaseService(this);
        database.connect();

        // ServiceManager сам инициализирует и поднимет DiscordBridge внутри bootstrap()
        services = new ServiceManager(this);
        services.bootstrap();

        if (services.guilds() != null) {
            services.guilds().loadAll();
        }

        RecipeService recipeService = new RecipeService(this);
        recipeService.registerAll();
        getServer().getPluginManager().registerEvents(recipeService, this);

        registerCommands();

        var pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerListener(this, services), this);
        pm.registerEvents(new MenuListener(services), this);
        pm.registerEvents(new ItemAbilityListener(this, services), this);
        pm.registerEvents(new ArmorMechanicsListener(this, services), this);
        pm.registerEvents(new RegionListener(services), this);
        pm.registerEvents(new MEBlockListener(services), this);
        pm.registerEvents(new TrampolineListener(this, services.store()), this);
        pm.registerEvents(new PocketDimensionListener(services), this);
        pm.registerEvents(services.corruption(), this);
        pm.registerEvents(services.bloodMoon(), this);
        pm.registerEvents(services.astral(), this);
        pm.registerEvents(new com.astrasmp.gui.METerminalGui(services), this);
        pm.registerEvents(new com.astrasmp.gui.MEDriveGui(services), this);

        // Сатанизм и Ритуалы
        com.astrasmp.rituals.RitualService ritualService = new com.astrasmp.rituals.RitualService(this);
        new com.astrasmp.model.CorruptionManager(this);
        com.astrasmp.pacts.PactManager pactManager = new com.astrasmp.pacts.PactManager();
        com.astrasmp.blood.BloodTankManager bloodTankManager = new com.astrasmp.blood.BloodTankManager(this);
        com.astrasmp.rift.RiftManager riftManager = new com.astrasmp.rift.RiftManager(this);
        services.setRiftManager(riftManager);

        pm.registerEvents(new com.astrasmp.listener.RitualListener(ritualService), this);
        pm.registerEvents(pactManager, this);
        pm.registerEvents(bloodTankManager, this);

        // Загрузка карманного измерения
        org.bukkit.WorldCreator wc = new org.bukkit.WorldCreator("astrasmp_pockets");
        wc.generator(new com.astrasmp.util.VoidChunkGenerator());
        org.bukkit.World pocketWorld = org.bukkit.Bukkit.createWorld(wc);
        if (pocketWorld != null) {
            applyPocketWorldRules(pocketWorld);
            pocketWorld.setTime(6000);
            pocketWorld.setStorm(false);
        }

        startPassiveEffectsTask();

        getLogger().info("=======================================");
        getLogger().info("   AstraSMP успешно запущен (2026)");
        getLogger().info("   ME-Система: АКТИВИРОВАНА");
        getLogger().info("   Статус БД: ПОДКЛЮЧЕНО");
        getLogger().info("=======================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("[!] Сохранение данных перед выключением...");

        if (services != null) {
            if (services.guilds() != null) {
                services.guilds().saveAll();
            }
            // Вызов services.shutdown() корректно завершит работу DiscordBridge
            services.shutdown();
        }

        if (database != null) {
            database.close();
        }

        getLogger().info("=== AstraSMP выключен. Данные сохранены. ===");
    }

    public static AstraSMPPlugin getInstance() {
        return instance;
    }

    public ServiceManager getServices() {
        return services;
    }

    // Проксируем получение DiscordBridge через ServiceManager
    public DiscordBridge getDiscord() {
        return services != null ? services.discord() : null;
    }

    public DatabaseService getDatabase() {
        return database;
    }

    private static final NamespacedKey KEY_CUSTOM_ID = new NamespacedKey("astrasmp", "custom_id");

    @SuppressWarnings("removal")
    private void applyPocketWorldRules(org.bukkit.World world) {
        world.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(org.bukkit.GameRule.DO_WEATHER_CYCLE, false);
    }

    private void startPassiveEffectsTask() {
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                ItemStack offHand = p.getInventory().getItemInOffHand();
                if (offHand == null || !offHand.hasItemMeta()) continue;

                String id = offHand.getItemMeta().getPersistentDataContainer()
                        .get(KEY_CUSTOM_ID, PersistentDataType.STRING);

                if (id == null) continue;

                if (id.equals("totemSpeed")) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 1, false, false, true));
                } else if (id.equals("totemShield")) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 40, 1, false, false, true));
                }
            }
        }, 200L, 1200L);
    }

    private void registerCommands() {
        bind("guild", new GuildCommand(services));
        bind("menu", new MenuCommand(services));
        bind("balance", new BalanceCommand(services));
        bind("pay", new PayCommand(services));
        bind("sell", new SellCommand(services));
        bind("ah", new AuctionCommand(services));
        bind("link", new LinkCommand(services));
        bind("admin", new AdminCommand(services));
        getCommand("tutorial").setExecutor(new com.astrasmp.commands.TutorialCommand(services));
        bind("mmr", new MMRCommand(services));
        bind("top", new TopCommand(services));
        bind("stats", new StatsCommand(services));
        bind("contract", new ContractCommand(services));
        bind("items", new ItemsCommand(services));
        bind("quest", new QuestCommand(services));
        bind("blacksmith", new BlacksmithCommand(services));

        com.astrasmp.gui.BountiesGui bountiesGui = new com.astrasmp.gui.BountiesGui(this, services);
        bind("bounty", new BountyCommand(this, services, bountiesGui));

        com.astrasmp.gui.RewardsGui rewardsGui = new com.astrasmp.gui.RewardsGui(this, services);
        bind("rewards", new RewardsCommand(services, rewardsGui));

        com.astrasmp.gui.TalentsGui talentsGui = new com.astrasmp.gui.TalentsGui(this, services);
        bind("talents", new TalentsCommand(talentsGui));

        PrefixCommand prefixExecutor = new PrefixCommand(services);
        bind("prefix", prefixExecutor);
        bind("unprefix", prefixExecutor);

        MarryCommand marryExecutor = new MarryCommand(services);
        bind("marry", marryExecutor);
        bind("unmarry", marryExecutor);

        bind("invsee", new InvseeCommand());
        FreezeCommand freezeExecutor = new FreezeCommand(services);
        bind("freeze", freezeExecutor);
        bind("unfreeze", freezeExecutor);

        bind("spawn", new SpawnCommand(this, services));
        bind("rtp", new RtpCommand(services));
        bind("setrtpblock", new RtpBlockCommand(services));
        bind("help", new HelpCommand(services));

        bind("gm", new GmCommand());
        bind("god", new GodCommand(this));
        bind("heal", new HealCommand());
        bind("feed", new FeedCommand());
        bind("dm", new DmCommand());
        VanishCommand vanishCmd = new VanishCommand(this);
        bind("vanish", vanishCmd);
        bind("unvanish", vanishCmd);
        getServer().getPluginManager().registerEvents(vanishCmd, this);

        LocationCommand locCmd = new LocationCommand(services);
        bind("pvp", locCmd);
        bind("casino", locCmd);
        bind("eventshop", locCmd);
        bind("afk", locCmd);
        bind("duel", locCmd);

        bind("prunus", new PrunusCommand(services));
        bind("malus", new MalusCommand(services));
    }

    private void bind(String name, org.bukkit.command.CommandExecutor executor) {
        PluginCommand cmd = getCommand(name);
        if (cmd == null) return;
        cmd.setExecutor(executor);
        if (executor instanceof org.bukkit.command.TabCompleter completer) {
            cmd.setTabCompleter(completer);
        }
    }
}