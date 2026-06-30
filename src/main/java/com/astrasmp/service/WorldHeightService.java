package com.astrasmp.service;

import com.astrasmp.AstraSMPPlugin;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


public class WorldHeightService {
    
    private final AstraSMPPlugin plugin;
    
    public WorldHeightService(AstraSMPPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void setupDatapacks() {
        // Load the config manually since this runs in onLoad() before ConfigManager setup
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        File configFile = new File(plugin.getDataFolder(), "worldheight.yml");
        if (!configFile.exists()) {
            plugin.saveResource("worldheight.yml", false);
        }
        
        org.bukkit.configuration.file.YamlConfiguration config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(configFile);
        ConfigurationSection section = config.getConfigurationSection("world-height");
        if (section == null) return;
        
        for (String key : section.getKeys(false)) {
            ConfigurationSection worldConfig = section.getConfigurationSection(key);
            if (worldConfig == null) continue;
            
            String worldName = worldConfig.getString("world", "world");
            int minY = worldConfig.getInt("min-y", -64);
            int height = worldConfig.getInt("height", 384);
            int logicalHeight = worldConfig.getInt("logical-height", 384);
            // int cloudHeight = worldConfig.getInt("cloud-height", 192); // Not explicitly in simple dimension_type json usually without extra mods, but we'll include standard fields
            
            String dimType = worldConfig.getString("dimension-type", "overworld"); // overworld, the_nether, the_end
            
            generateDatapack(worldName, dimType, minY, height, logicalHeight);
        }
    }
    
    private void generateDatapack(String worldFolder, String dimType, int minY, int height, int logicalHeight) {
        File datapacksDir = new File(worldFolder, "datapacks");
        if (!datapacksDir.exists()) {
            datapacksDir.mkdirs();
        }
        
        File packDir = new File(datapacksDir, "astrasmp-height");
        packDir.mkdirs();
        
        // pack.mcmeta
        File mcmeta = new File(packDir, "pack.mcmeta");
        try (FileWriter writer = new FileWriter(mcmeta)) {
            writer.write("""
            {
                "pack": {
                    "pack_format": 48,
                    "description": "AstraSMP Custom World Height"
                }
            }
            """);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        // dimension_type
        File dimDir = new File(packDir, "data/minecraft/dimension_type");
        dimDir.mkdirs();
        
        String dimName = dimType.equals("custom") ? "overworld" : dimType; // fallback to overworld if custom
        File dimJson = new File(dimDir, dimName + ".json");
        
        // Use overworld template
        String json = """
        {
          "min_y": %d,
          "height": %d,
          "logical_height": %d,
          "coordinate_scale": 1.0,
          "ambient_light": 0.0,
          "fixed_time": false,
          "has_lightning": true,
          "has_raids": true,
          "has_skylight": true,
          "has_ceiling": false,
          "ultrawarm": false,
          "natural": true,
          "piglin_safe": false,
          "bed_works": true,
          "respawn_anchor_works": false,
          "infiniburn": "#minecraft:infiniburn_overworld",
          "effects": "minecraft:overworld",
          "monster_spawn_block_light_limit": 0,
          "monster_spawn_light_level": {
            "type": "minecraft:uniform",
            "value": {
              "min_inclusive": 0,
              "max_inclusive": 7
            }
          }
        }
        """.formatted(minY, height, logicalHeight);
        
        try (FileWriter writer = new FileWriter(dimJson)) {
            writer.write(json);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        plugin.getLogger().info("Generated custom world height datapack for world: " + worldFolder);
    }
}
