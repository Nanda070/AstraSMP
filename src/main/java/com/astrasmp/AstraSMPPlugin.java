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

/**
 * Основной класс плагина AstraSMP.
 * ChetCraft Production Base - Релизная версия с поддержкой ME-системы и БД
 */
public final class AstraSMPPlugin extends JavaPlugin {

    private static AstraSMPPlugin instance;

    private ServiceManager services;
    private DatabaseService database;
    private DiscordBridge discordBridge;

    @Override
    public void onEnable() {
        instance = this;

        // Инициализация реестра предметов
        ItemRegistry.init(this);

        // 1. Конфиги
        saveDefaultConfig();
        saveResource("messages.yml", false);

        // 2. Инициализация Базы Данных
        database = new DatabaseService(this);
        database.connect();

        // 3. Сервисы
        services = new ServiceManager(this);
        services.bootstrap();

        // Инициализация и запуск Discord моста (Строго до загрузки гильдий)
        discordBridge = new DiscordBridge(
                this,
                services.economy(),
                services.mmr(),
                services.contracts(),
                services.events(),
                services.leaderboard()
        );
        discordBridge.connect();

        // 4. Данные гильдий
        if (services.guilds() != null) {
            services.guilds().loadAll();
        }

        // 5. Регистрация кастомных рецептов и слушателя валидации
        RecipeService recipeService = new RecipeService(this);
        recipeService.registerAll();
        getServer().getPluginManager().registerEvents(recipeService, this);

        // 6. Команды
        registerCommands();

        // 7. Регистрация Слушателей
        var pm = getServer().getPluginManager();

        // Базовые слушатели
        pm.registerEvents(new PlayerListener(this, services), this);
        pm.registerEvents(new MenuListener(services), this);
        pm.registerEvents(new ItemAbilityListener(this, services), this);
        pm.registerEvents(new ArmorMechanicsListener(this, services), this);
        pm.registerEvents(new RegionListener(services), this);

        // Слушатели ME-системы (Блоки и GUI)
        pm.registerEvents(new MEBlockListener(services), this);
        pm.registerEvents(new com.astrasmp.gui.METerminalGui(services), this);
        pm.registerEvents(new com.astrasmp.gui.MEDriveGui(services), this);

        // 8. Пассивные эффекты
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
            services.shutdown();
        }

        // Отключение Discord бота для высвобождения потоков JDA
        if (discordBridge != null) {
            discordBridge.shutdown();
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

    public DiscordBridge getDiscord() {
        return this.discordBridge;
    }

    public DatabaseService getDatabase() {
        return database;
    }

    private void startPassiveEffectsTask() {
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                ItemStack offHand = p.getInventory().getItemInOffHand();
                if (offHand == null || !offHand.hasItemMeta()) continue;

                String id = offHand.getItemMeta().getPersistentDataContainer()
                        .get(new NamespacedKey("astrasmp", "custom_id"), PersistentDataType.STRING);

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
        // Команды управления и экономики
        bind("guild", new GuildCommand(services));
        bind("menu", new MenuCommand(services));
        bind("balance", new BalanceCommand(services));
        bind("pay", new PayCommand(services));
        bind("sell", new SellCommand(services));
        bind("ah", new AuctionCommand(services));
        bind("link", new LinkCommand(services));
        bind("admin", new AdminCommand(services));

        // Статистика и геймплей
        bind("mmr", new MMRCommand(services));
        bind("top", new TopCommand(services));
        bind("stats", new StatsCommand(services));
        bind("contract", new ContractCommand(services));
        bind("items", new ItemsCommand(services));
        bind("quest", new QuestCommand(services));

        // Кастомизация и социальное
        PrefixCommand prefixExecutor = new PrefixCommand(services);
        bind("prefix", prefixExecutor);
        bind("unprefix", prefixExecutor);

        MarryCommand marryExecutor = new MarryCommand(services);
        bind("marry", marryExecutor);
        bind("unmarry", marryExecutor);

        // Утилиты модерации
        bind("invsee", new InvseeCommand());
        FreezeCommand freezeExecutor = new FreezeCommand(services);
        bind("freeze", freezeExecutor);
        bind("unfreeze", freezeExecutor);

        // Навигация
        bind("spawn", new SpawnCommand(this, services));
        bind("rtp", new RtpCommand(services));
        bind("setrtpblock", new RtpBlockCommand(services));
        bind("help", new HelpCommand(services));

        // Локации
        LocationCommand locCmd = new LocationCommand(services);
        if (getCommand("pvp") != null) getCommand("pvp").setExecutor(locCmd);
        if (getCommand("casino") != null) getCommand("casino").setExecutor(locCmd);
        if (getCommand("eventshop") != null) getCommand("eventshop").setExecutor(locCmd);
        if (getCommand("afk") != null) getCommand("afk").setExecutor(locCmd);
        if (getCommand("duel") != null) getCommand("duel").setExecutor(locCmd);
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