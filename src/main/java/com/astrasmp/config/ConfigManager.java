package com.astrasmp.config;

import com.astrasmp.AstraSMPPlugin;
import com.astrasmp.util.ConfigUpdater;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {

    private final AstraSMPPlugin plugin;
    private final Map<String, FileConfiguration> configs = new HashMap<>();
    private final Map<String, File> configFiles = new HashMap<>();

    public ConfigManager(AstraSMPPlugin plugin) {
        this.plugin = plugin;
    }

    public void setup() {
        // config.yml and plugin.yml are handled by Bukkit natively, but we update them
        ConfigUpdater.updateConfig(plugin, "config.yml");
        ConfigUpdater.updateConfig(plugin, "messages.yml");
        ConfigUpdater.updateConfig(plugin, "discord.yml");
        ConfigUpdater.updateConfig(plugin, "items.yml");

        plugin.reloadConfig();
        
        loadConfig("messages.yml");
        loadConfig("discord.yml");
        loadConfig("items.yml");
    }

    public void loadConfig(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        configs.put(fileName, config);
        configFiles.put(fileName, file);
    }

    public FileConfiguration getConfig(String fileName) {
        return configs.get(fileName);
    }

    public void saveConfig(String fileName) {
        try {
            FileConfiguration config = configs.get(fileName);
            File file = configFiles.get(fileName);
            if (config != null && file != null) {
                config.save(file);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Could not save config " + fileName);
            e.printStackTrace();
        }
    }

    public void reloadAll() {
        plugin.reloadConfig();
        for (String fileName : configs.keySet()) {
            loadConfig(fileName);
        }
    }

    public String getMessage(String path, String def) {
        FileConfiguration msgConfig = getConfig("messages.yml");
        if (msgConfig == null) return def;
        return com.astrasmp.util.TextUtil.color(msgConfig.getString(path, def));
    }
}
