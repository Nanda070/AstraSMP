package com.astrasmp.service;

import com.astrasmp.AstraSMPPlugin;
import com.astrasmp.casino.CasinoService;
import com.astrasmp.casino.games.ClassicGame;
import com.astrasmp.discord.DiscordBridge;
import com.astrasmp.gui.GuiManager;
import com.astrasmp.casino.games.RouletteGame;
import com.astrasmp.casino.games.DrumsGame;
import com.astrasmp.casino.games.BlackjackGame;
import com.astrasmp.casino.games.TicTacToeGame;

public final class ServiceManager {
    private final AstraSMPPlugin plugin;
    private final DataStore store;
    private final EconomyService economy;
    private final MMRService mmr;
    private final EventService events;
    private final AuctionService auction;
    private final LeaderboardService leaderboard;
    private final BlackjackGame blackjackGame;
    private final ContractService contracts;
    private final DiscordBridge discord;
    private final GuiManager gui;
    private final TabService tab;
    private final TicTacToeGame ticTacToeGame;
    private final NpcShopService shops;
    private final QuestManager quests;
    private final AfkService afk;
    private final DuelService duels;
    private final DrumsGame drumsGame;
    private final GuildService guilds;

    // Инкапсулированные сервисы казино
    private final CasinoService casinoService;
    private final ClassicGame classicGame;
    private final RouletteGame rouletteGame;

    // ME-сеть
    private final MENetworkService meNetwork;

    public ServiceManager(AstraSMPPlugin plugin) {
        this.plugin = plugin;

        // 1. Инициализация базы данных и менеджеров
        this.store = new DataStore(plugin);
        this.quests = new QuestManager(plugin, this);

        // 2. Экономика и зависимые сервисы
        this.economy = new EconomyService(plugin, store);
        this.mmr = new MMRService(plugin, store);
        this.contracts = new ContractService(plugin, store);
        this.leaderboard = new LeaderboardService(plugin, store);

        // 3. Геймплей
        this.auction = new AuctionService(plugin, store, economy);
        this.events = new EventService(plugin, economy, mmr);

        // 4. Интерфейсы и внешние связи
        this.discord = new DiscordBridge(plugin, economy, mmr, contracts, events, leaderboard);
        this.gui = new GuiManager(plugin, this, auction, contracts);
        this.tab = new TabService(plugin, this);

        // 5. Новые системы (NPC, AFK, Дуэли)
        this.shops = new NpcShopService(plugin, this);
        this.afk = new AfkService(plugin, this);
        this.duels = new DuelService(plugin, this);
        this.guilds = new GuildService(plugin);

        // 6. Нативный гемблинг AstraSMP
        this.casinoService = new CasinoService(plugin, this);
        this.classicGame = new ClassicGame(this.casinoService);
        this.rouletteGame = new RouletteGame(this.casinoService);
        this.drumsGame = new DrumsGame(this.casinoService);
        this.blackjackGame = new BlackjackGame(this.casinoService);
        this.ticTacToeGame = new TicTacToeGame(this.casinoService);

        // 7. Инициализация ME-системы
        this.meNetwork = new MENetworkService(store.meNetworks());

        // Регистрация ивентов для сервисов
        plugin.getServer().getPluginManager().registerEvents(shops, plugin);
        plugin.getServer().getPluginManager().registerEvents(duels, plugin);

        // plugin.getServer().getPluginManager().registerEvents(meNetwork, plugin);
    }

    public void bootstrap() {
        if (store != null) {
            store.loadAll();
            plugin.getLogger().info("[DataStore] Все данные (профили, квесты, аукционы) загружены!");
        }

        if (discord != null && discord.isEnabled()) discord.connect();
        if (events != null) events.startSchedulers();
        if (leaderboard != null) leaderboard.startUpdateTask();
        if (tab != null) tab.start();

        plugin.getLogger().info("=== Все сервисы AstraSMP успешно запущены! ===");
    }

    public void shutdown() {
        plugin.getLogger().info("=== Начало процесса остановки сервисов... ===");

        if (discord != null) discord.shutdown();

        if (store != null) {
            store.saveAllNow();
            plugin.getLogger().info("[DataStore] Принудительное сохранение завершено.");
        }

        if (events != null) events.finish();
        if (tab != null) tab.stop();

        plugin.getLogger().info("=== Все сервисы остановлены. Данные в безопасности. ===");
    }

    public String discordPrefix() {
        return plugin.getConfig().getString("discord.prefix", "!as");
    }

    // --- Геттеры для доступа из команд и слушателей ---
    public AstraSMPPlugin plugin() { return plugin; }
    public DataStore store() { return store; }
    public EconomyService economy() { return economy; }
    public MMRService mmr() { return mmr; }
    public EventService events() { return events; }
    public AuctionService auction() { return auction; }
    public LeaderboardService leaderboard() { return leaderboard; }
    public ContractService contracts() { return contracts; }
    public DiscordBridge discord() { return discord; }
    public GuiManager gui() { return gui; }
    public TabService tab() { return tab; }
    public QuestManager quests() { return quests; }
    public NpcShopService shops() { return shops; }
    public AfkService afk() { return afk; }
    public DuelService duels() { return duels; }
    public BlackjackGame blackjackGame() { return blackjackGame; }
    public GuildService guilds() { return guilds; }
    public DrumsGame drumsGame() { return drumsGame; }
    public TicTacToeGame ticTacToeGame() { return ticTacToeGame; }
    public RouletteGame rouletteGame() { return rouletteGame; }

    // Геттеры казино
    public CasinoService casino() { return casinoService; }
    public ClassicGame classicGame() { return classicGame; }

    // Геттер ME-сети
    public MENetworkService meNetwork() { return meNetwork; }
}