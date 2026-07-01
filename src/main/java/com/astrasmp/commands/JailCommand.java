package com.astrasmp.commands;

import com.astrasmp.AstraSMPPlugin;
import com.astrasmp.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles: /jail, /unjail, /setjail, /deljail, /jailedplayers
 *
 * Jails are stored in plugins/AstraSMP/jails.yml
 * Jailed players are stored in plugins/AstraSMP/jailed.yml
 *
 * When a player tries to move outside the jail radius they are teleported back.
 */
public final class JailCommand implements org.bukkit.command.TabExecutor, Listener {

    private final AstraSMPPlugin plugin;

    // jailName -> {world, x, y, z, radius}
    private final Map<String, JailData> jails = new HashMap<>();
    // playerName -> jailName
    private final Map<String, String> jailed = new HashMap<>();

    private File jailsFile;
    private File jailedFile;
    private FileConfiguration jailsCfg;
    private FileConfiguration jailedCfg;

    public JailCommand(AstraSMPPlugin plugin) {
        this.plugin = plugin;
        loadData();
    }

    // ─────────────────────────────────────────────
    //  Data classes
    // ─────────────────────────────────────────────

    private record JailData(String world, double x, double y, double z, double radius) {}

    // ─────────────────────────────────────────────
    //  Persistence
    // ─────────────────────────────────────────────

    private void loadData() {
        jailsFile = new File(plugin.getDataFolder(), "jails.yml");
        jailedFile = new File(plugin.getDataFolder(), "jailed.yml");

        if (!jailsFile.exists()) {
            try { jailsFile.createNewFile(); } catch (IOException ignored) {}
        }
        if (!jailedFile.exists()) {
            try { jailedFile.createNewFile(); } catch (IOException ignored) {}
        }

        jailsCfg = YamlConfiguration.loadConfiguration(jailsFile);
        jailedCfg = YamlConfiguration.loadConfiguration(jailedFile);

        // Load jails
        if (jailsCfg.contains("jails")) {
            for (String name : jailsCfg.getConfigurationSection("jails").getKeys(false)) {
                String path = "jails." + name + ".";
                String world = jailsCfg.getString(path + "world");
                double x = jailsCfg.getDouble(path + "x");
                double y = jailsCfg.getDouble(path + "y");
                double z = jailsCfg.getDouble(path + "z");
                double radius = jailsCfg.getDouble(path + "radius", 5.0);
                jails.put(name.toLowerCase(), new JailData(world, x, y, z, radius));
            }
        }

        // Load jailed players
        if (jailedCfg.contains("jailed")) {
            for (String name : jailedCfg.getConfigurationSection("jailed").getKeys(false)) {
                String jailName = jailedCfg.getString("jailed." + name);
                jailed.put(name.toLowerCase(), jailName);
            }
        }
    }

    private void saveJails() {
        jailsCfg.set("jails", null);
        for (Map.Entry<String, JailData> entry : jails.entrySet()) {
            String path = "jails." + entry.getKey() + ".";
            JailData d = entry.getValue();
            jailsCfg.set(path + "world", d.world());
            jailsCfg.set(path + "x", d.x());
            jailsCfg.set(path + "y", d.y());
            jailsCfg.set(path + "z", d.z());
            jailsCfg.set(path + "radius", d.radius());
        }
        try { jailsCfg.save(jailsFile); } catch (IOException e) {
            plugin.getLogger().warning("Could not save jails.yml: " + e.getMessage());
        }
    }

    private void saveJailed() {
        jailedCfg.set("jailed", null);
        for (Map.Entry<String, String> entry : jailed.entrySet()) {
            jailedCfg.set("jailed." + entry.getKey(), entry.getValue());
        }
        try { jailedCfg.save(jailedFile); } catch (IOException e) {
            plugin.getLogger().warning("Could not save jailed.yml: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    //  Command handler
    // ─────────────────────────────────────────────

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission("astrasmp.admin")) {
            TextUtil.send(sender, "&cУ вас нет прав для использования этой команды.");
            return true;
        }

        switch (label.toLowerCase()) {
            case "setjail" -> handleSetJail(sender, args);
            case "deljail" -> handleDelJail(sender, args);
            case "jail" -> handleJail(sender, args);
            case "unjail" -> handleUnjail(sender, args);
            case "jailedplayers" -> handleJailedPlayers(sender);
        }
        return true;
    }

    // /setjail <name> [radius]
    private void handleSetJail(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            TextUtil.send(sender, "&cЭта команда доступна только игрокам.");
            return;
        }
        if (args.length == 0) {
            TextUtil.send(sender, "&cИспользование: /setjail <название> [радиус]");
            return;
        }
        String name = args[0].toLowerCase();
        double radius = 5.0;
        if (args.length >= 2) {
            try {
                radius = Double.parseDouble(args[1]);
                if (radius <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                TextUtil.send(sender, "&cРадиус должен быть положительным числом.");
                return;
            }
        }

        Location loc = player.getLocation();
        JailData data = new JailData(loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), radius);
        jails.put(name, data);
        saveJails();

        TextUtil.send(sender, "&aТюрьма &e" + name + " &aсоздана! Радиус: &e" + radius + " &aблоков.");
    }

    // /deljail <name>
    private void handleDelJail(CommandSender sender, String[] args) {
        if (args.length == 0) {
            TextUtil.send(sender, "&cИспользование: /deljail <название>");
            return;
        }
        String name = args[0].toLowerCase();
        if (!jails.containsKey(name)) {
            TextUtil.send(sender, "&cТюрьма &e" + name + " &cне найдена.");
            return;
        }
        jails.remove(name);
        saveJails();
        TextUtil.send(sender, "&aТюрьма &e" + name + " &aудалена.");
    }

    // /jail <player> <jailname>
    private void handleJail(CommandSender sender, String[] args) {
        if (args.length < 2) {
            TextUtil.send(sender, "&cИспользование: /jail <игрок> <тюрьма>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            TextUtil.send(sender, "&cИгрок &e" + args[0] + " &cне найден.");
            return;
        }
        String jailName = args[1].toLowerCase();
        JailData jailData = jails.get(jailName);
        if (jailData == null) {
            TextUtil.send(sender, "&cТюрьма &e" + jailName + " &cне существует. Доступные: &e" + String.join(", ", jails.keySet()));
            return;
        }

        jailed.put(target.getName().toLowerCase(), jailName);
        saveJailed();

        Location jailLoc = buildLocation(jailData);
        if (jailLoc != null) {
            target.teleport(jailLoc);
        }

        TextUtil.send(target, "&cВы были помещены в тюрьму &e" + jailName + "&c.");
        TextUtil.send(sender, "&aИгрок &e" + target.getName() + " &aпомещён в тюрьму &e" + jailName + "&a.");
    }

    // /unjail <player>
    private void handleUnjail(CommandSender sender, String[] args) {
        if (args.length == 0) {
            TextUtil.send(sender, "&cИспользование: /unjail <игрок>");
            return;
        }
        String targetName = args[0].toLowerCase();
        if (!jailed.containsKey(targetName)) {
            TextUtil.send(sender, "&cИгрок &e" + args[0] + " &cне находится в тюрьме.");
            return;
        }

        jailed.remove(targetName);
        saveJailed();

        Player online = Bukkit.getPlayerExact(args[0]);
        if (online != null) {
            TextUtil.send(online, "&aВы освобождены из тюрьмы!");
        }
        TextUtil.send(sender, "&aИгрок &e" + args[0] + " &aосвобождён из тюрьмы.");
    }

    // /jailedplayers
    private void handleJailedPlayers(CommandSender sender) {
        if (jailed.isEmpty()) {
            TextUtil.send(sender, "&7Список заключённых пуст.");
            return;
        }
        TextUtil.send(sender, "&b&lЗаключённые &7(" + jailed.size() + ")&b:");
        for (Map.Entry<String, String> entry : jailed.entrySet()) {
            boolean online = Bukkit.getPlayerExact(entry.getKey()) != null;
            String status = online ? "&a(онлайн)" : "&7(оффлайн)";
            TextUtil.send(sender, " &e" + entry.getKey() + " &7→ тюрьма &f" + entry.getValue() + " " + status);
        }
    }

    // ─────────────────────────────────────────────
    //  Listeners
    // ─────────────────────────────────────────────

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        String name = player.getName().toLowerCase();

        if (!jailed.containsKey(name)) return;

        String jailName = jailed.get(name);
        JailData jailData = jails.get(jailName);
        if (jailData == null) return;

        Location jailLoc = buildLocation(jailData);
        if (jailLoc == null) return;

        // Only process if they actually changed block position (optimisation)
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        double dist = event.getTo().distance(jailLoc);
        if (dist > jailData.radius()) {
            // Teleport back to jail centre
            player.teleport(jailLoc);
            TextUtil.send(player, "&cВы не можете покинуть тюрьму!");
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String name = player.getName().toLowerCase();

        if (!jailed.containsKey(name)) return;

        String jailName = jailed.get(name);
        JailData jailData = jails.get(jailName);
        if (jailData == null) return;

        Location jailLoc = buildLocation(jailData);
        if (jailLoc != null) {
            // Teleport after 1 tick to ensure the player is fully loaded
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.teleport(jailLoc);
                TextUtil.send(player, "&cВы всё ещё находитесь в тюрьме &e" + jailName + "&c.");
            }, 5L);
        }
    }

    // ─────────────────────────────────────────────
    //  Tab Completion
    // ─────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (!sender.hasPermission("astrasmp.admin")) return Collections.emptyList();

        String label = alias.toLowerCase();

        return switch (label) {
            case "setjail" -> {
                if (args.length == 1) yield List.of("<название>");
                if (args.length == 2) yield List.of("5", "10", "15", "20");
                yield Collections.emptyList();
            }
            case "deljail" -> {
                if (args.length == 1) {
                    yield jails.keySet().stream()
                            .filter(n -> n.startsWith(args[0].toLowerCase()))
                            .collect(Collectors.toList());
                }
                yield Collections.emptyList();
            }
            case "jail" -> {
                if (args.length == 1) {
                    yield Bukkit.getOnlinePlayers().stream()
                            .map(p -> p.getName())
                            .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                            .collect(Collectors.toList());
                }
                if (args.length == 2) {
                    yield jails.keySet().stream()
                            .filter(n -> n.startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                }
                yield Collections.emptyList();
            }
            case "unjail" -> {
                if (args.length == 1) {
                    yield jailed.keySet().stream()
                            .filter(n -> n.startsWith(args[0].toLowerCase()))
                            .collect(Collectors.toList());
                }
                yield Collections.emptyList();
            }
            default -> Collections.emptyList();
        };
    }

    // ─────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────

    private Location buildLocation(JailData data) {
        org.bukkit.World world = Bukkit.getWorld(data.world());
        if (world == null) return null;
        return new Location(world, data.x(), data.y(), data.z());
    }

    /** Expose the jailed map for external checks if needed */
    public boolean isJailed(Player player) {
        return jailed.containsKey(player.getName().toLowerCase());
    }
}
