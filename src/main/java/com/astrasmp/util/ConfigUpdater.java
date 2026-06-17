package com.astrasmp.util;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ConfigUpdater {

    /**
     * Безопасное обновление локального файла конфигурации.
     * Недостающие ключи из дефолтного файла внутри .jar переносятся в локальный файл.
     *
     * @param plugin Плагин
     * @param fileName Имя файла (например, "config.yml")
     */
    public static void updateConfig(JavaPlugin plugin, String fileName) {
        File configFile = new File(plugin.getDataFolder(), fileName);
        if (!configFile.exists()) {
            plugin.saveResource(fileName, false);
            return;
        }

        YamlConfiguration localConfig = YamlConfiguration.loadConfiguration(configFile);
        InputStream defConfigStream = plugin.getResource(fileName);
        
        if (defConfigStream == null) {
            plugin.getLogger().warning("[ConfigUpdater] Дефолтный файл " + fileName + " не найден внутри .jar!");
            return;
        }

        YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream, StandardCharsets.UTF_8));
        
        boolean changed = false;
        for (String key : defConfig.getKeys(true)) {
            if (!localConfig.contains(key)) {
                localConfig.set(key, defConfig.get(key));
                changed = true;
            }
        }

        if (changed) {
            try {
                localConfig.save(configFile);
                plugin.getLogger().info("[ConfigUpdater] Файл " + fileName + " успешно обновлен (добавлены новые ключи).");
            } catch (Exception e) {
                plugin.getLogger().severe("[ConfigUpdater] Не удалось сохранить обновленный файл " + fileName);
                e.printStackTrace();
            }
        }
    }
}
