package com.astrasmp;

import com.astrasmp.commands.*;
import com.astrasmp.database.DatabaseService;
import com.astrasmp.discord.DiscordBridge;
import com.astrasmp.items.ItemRegistry;
import com.astrasmp.listener.*;
import com.astrasmp.service.RecipeService;
import com.astrasmp.service.ServiceManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class AstraSMPPlugin extends JavaPlugin {

    private static AstraSMPPlugin instance;

    private ServiceManager services;
    private DatabaseService database;

    private com.astrasmp.config.ConfigManager configManager;
    private com.astrasmp.service.SitLayService sitLayService;

    @Override
    public org.bukkit.generator.ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        if ("astrasmp_pockets".equals(worldName)) {
            return new com.astrasmp.util.VoidChunkGenerator();
        }
        return super.getDefaultWorldGenerator(worldName, id);
    }

    @Override
    public void onLoad() {
        com.astrasmp.service.WorldHeightService worldHeightService = new com.astrasmp.service.WorldHeightService(this);
        worldHeightService.setupDatapacks();
    }

    @Override
    public void onEnable() {
        instance = this;

        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        configManager = new com.astrasmp.config.ConfigManager(this);
        configManager.setup();

        ItemRegistry.init(this);


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
        
        sitLayService = new com.astrasmp.service.SitLayService(this);
        pm.registerEvents(sitLayService, this);
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



        getLogger().info("=======================================");
        getLogger().info("   ChetCraft успешно запущен (2026)");
        getLogger().info("   ME-Система: АКТИВИРОВАНА");
        getLogger().info("   Статус БД: ПОДКЛЮЧЕНО");
        getLogger().info("=======================================");
    }

    @Override
    public void onDisable() {
        if (sitLayService != null) {
            sitLayService.shutdown();
        }

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

        getLogger().info("=== ChetCraft выключен. Данные сохранены. ===");
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

    public org.bukkit.configuration.file.FileConfiguration getDiscordConfig() {
        return configManager.getConfig("discord.yml");
    }

    public com.astrasmp.config.ConfigManager getConfigManager() {
        return configManager;
    }


    @SuppressWarnings("removal")
    private void applyPocketWorldRules(org.bukkit.World world) {
        world.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(org.bukkit.GameRule.DO_WEATHER_CYCLE, false);
    }



    private void registerCommands() {
        bind("sit", new SitCommand(sitLayService, this));
        bind("lay", new LayCommand(sitLayService, this));
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
        com.astrasmp.gui.RtpGui rtpGui = new com.astrasmp.gui.RtpGui(this, services);
        bind("rtp", new RtpCommand(rtpGui));
        bind("setrtpblock", new RtpBlockCommand(services));
        bind("help", new HelpCommand(services));
        
        bind("warp", new WarpCommand(services));
        
        HomeCommand homeCmd = new HomeCommand(services);
        bind("home", homeCmd);
        bind("sethome", homeCmd);
        bind("delhome", homeCmd);
        bind("homelist", homeCmd);
        bind("homesreload", homeCmd);

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

        ArenaCommand arenaCmd = new ArenaCommand(this, services);
        bind("arena", arenaCmd);
        bind("leave", arenaCmd);

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