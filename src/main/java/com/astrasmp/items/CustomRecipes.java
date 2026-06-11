package com.astrasmp.items;

import com.astrasmp.AstraSMPPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;

public final class CustomRecipes {

    private CustomRecipes() {}

    public static void register(AstraSMPPlugin plugin) {
        // Броня
        registerArmor(plugin);
        // Инструменты и Оружие
        registerTools(plugin);
        registerWeapons(plugin);
    }

    private static void add(AstraSMPPlugin plugin, String key, ItemStack result, String[] shape, Object... ingredients) {
        NamespacedKey nsKey = new NamespacedKey(plugin, key);
        try {
            Bukkit.removeRecipe(nsKey); // Удаляем старый рецепт перед добавлением
        } catch (Exception ignored) {}

        ShapedRecipe recipe = new ShapedRecipe(nsKey, result);
        recipe.shape(shape);
        for (int i = 0; i < ingredients.length; i += 2) {
            Character c = (Character) ingredients[i];
            Material m = (Material) ingredients[i + 1];
            if (m != null && m != Material.AIR) {
                recipe.setIngredient(c, m);
            }
        }
        Bukkit.addRecipe(recipe);
    }

    private static void registerTools(AstraSMPPlugin plugin) {
        add(plugin, "recipe_mining3x3", ItemRegistry.mining3x3(),
                new String[]{"DDD", "TST", " S "},
                'D', Material.DIAMOND_BLOCK, 'T', Material.TNT, 'S', Material.STICK);

        add(plugin, "recipe_mining5x5", ItemRegistry.mining5x5(),
                new String[]{"NIN", "IPI", "NIN"},
                'N', Material.NETHERITE_BLOCK, 'I', Material.NETHERITE_INGOT, 'P', Material.NETHERITE_PICKAXE);

        add(plugin, "recipe_vein_miner", ItemRegistry.veinMiner(),
                new String[]{"AAA", "APA", "AAA"},
                'A', Material.AMETHYST_CLUSTER, 'P', Material.NETHERITE_PICKAXE);

        add(plugin, "recipe_auto_smelt", ItemRegistry.autoSmelt(),
                new String[]{"MMM", "MPM", "MMM"},
                'M', Material.MAGMA_BLOCK, 'P', Material.NETHERITE_PICKAXE);

        add(plugin, "recipe_magnet", ItemRegistry.magnet(),
                new String[]{"GIG", "ICI", "GIG"},
                'G', Material.GOLD_INGOT, 'I', Material.IRON_INGOT, 'C', Material.COMPASS);
    }

    private static void registerWeapons(AstraSMPPlugin plugin) {
        add(plugin, "recipe_shadow_blade", ItemRegistry.shadowBlade(),
                new String[]{"ECE", "ESE", "ECE"},
                'E', Material.ECHO_SHARD, 'C', Material.COAL_BLOCK, 'S', Material.NETHERITE_SWORD);

        add(plugin, "recipe_thunder_hammer", ItemRegistry.thunderHammer(),
                new String[]{"CLC", "CAC", "CCC"},
                'C', Material.COPPER_BLOCK, 'L', Material.LIGHTNING_ROD, 'A', Material.NETHERITE_AXE);

        add(plugin, "recipe_vampire_dagger", ItemRegistry.vampireDagger(),
                new String[]{" R ", "GSG", " R "},
                'R', Material.REDSTONE_BLOCK, 'G', Material.GHAST_TEAR, 'S', Material.DIAMOND_SWORD);

        add(plugin, "recipe_inferno_sword", ItemRegistry.infernoSword(),
                new String[]{"BMB", "BSB", "BMB"},
                'B', Material.BLAZE_ROD, 'M', Material.MAGMA_CREAM, 'S', Material.NETHERITE_SWORD);

        add(plugin, "recipe_frost_axe", ItemRegistry.frostAxe(),
                new String[]{"PPP", "PAP", "PPP"},
                'P', Material.PACKED_ICE, 'A', Material.DIAMOND_AXE);

        add(plugin, "recipe_venom_bow", ItemRegistry.venomBow(),
                new String[]{" S ", "SBS", " S "},
                'S', Material.SPIDER_EYE, 'B', Material.BOW);

        add(plugin, "recipe_reaper_scythe", ItemRegistry.reaperScythe(),
                new String[]{"NNN", " H ", " N "},
                'N', Material.NETHERITE_BLOCK, 'H', Material.NETHERITE_HOE);
    }

    private static void registerArmor(AstraSMPPlugin plugin) {
        // --- MERCENARY ---
        add(plugin, "mercenary_helmet", ItemRegistry.mercenaryHelmet(),
                new String[]{"CLC", "C C"},
                'C', Material.COAL_BLOCK, 'L', Material.LEATHER);
        add(plugin, "mercenary_chestplate", ItemRegistry.mercenaryChestplate(),
                new String[]{"C C", "CLC", "CCC"},
                'C', Material.COAL_BLOCK, 'L', Material.LEATHER);
        add(plugin, "mercenary_leggings", ItemRegistry.mercenaryLeggings(),
                new String[]{"CCC", "L L", "C C"},
                'C', Material.COAL_BLOCK, 'L', Material.LEATHER);
        add(plugin, "mercenary_boots", ItemRegistry.mercenaryBoots(),
                new String[]{"C C", "L L"},
                'C', Material.COAL_BLOCK, 'L', Material.LEATHER);

        // --- BERSERKER ---
        add(plugin, "berserker_helmet", ItemRegistry.berserkerHelmet(),
                new String[]{"III", "R R"},
                'I', Material.IRON_BLOCK, 'R', Material.REDSTONE_BLOCK);
        add(plugin, "berserker_chestplate", ItemRegistry.berserkerChestplate(),
                new String[]{"I I", "IRI", "III"},
                'I', Material.IRON_BLOCK, 'R', Material.REDSTONE_BLOCK);
        add(plugin, "berserker_leggings", ItemRegistry.berserkerLeggings(),
                new String[]{"III", "R R", "I I"},
                'I', Material.IRON_BLOCK, 'R', Material.REDSTONE_BLOCK);
        add(plugin, "berserker_boots", ItemRegistry.berserkerBoots(),
                new String[]{"R R", "I I"},
                'I', Material.IRON_BLOCK, 'R', Material.REDSTONE_BLOCK);

        // --- INQUISITOR ---
        add(plugin, "inquisitor_helmet", ItemRegistry.inquisitorHelmet(),
                new String[]{"GGG", "I I"},
                'G', Material.GOLD_BLOCK, 'I', Material.IRON_BLOCK);
        add(plugin, "inquisitor_chestplate", ItemRegistry.inquisitorChestplate(),
                new String[]{"G G", "GIG", "GGG"},
                'G', Material.GOLD_BLOCK, 'I', Material.IRON_BLOCK);
        add(plugin, "inquisitor_leggings", ItemRegistry.inquisitorLeggings(),
                new String[]{"GGG", "I I", "G G"},
                'G', Material.GOLD_BLOCK, 'I', Material.IRON_BLOCK);
        add(plugin, "inquisitor_boots", ItemRegistry.inquisitorBoots(),
                new String[]{"I I", "G G"},
                'G', Material.GOLD_BLOCK, 'I', Material.IRON_BLOCK);

        // --- JUGGERNAUT ---
        add(plugin, "juggernaut_helmet", ItemRegistry.juggernautHelmet(),
                new String[]{"OOO", "N N"},
                'O', Material.OBSIDIAN, 'N', Material.NETHERITE_INGOT);
        add(plugin, "juggernaut_chestplate", ItemRegistry.juggernautChestplate(),
                new String[]{"O O", "ONO", "OOO"},
                'O', Material.OBSIDIAN, 'N', Material.NETHERITE_INGOT);
        add(plugin, "juggernaut_leggings", ItemRegistry.juggernautLeggings(),
                new String[]{"OOO", "N N", "O O"},
                'O', Material.OBSIDIAN, 'N', Material.NETHERITE_INGOT);
        add(plugin, "juggernaut_boots", ItemRegistry.juggernautBoots(),
                new String[]{"N N", "O O"},
                'O', Material.OBSIDIAN, 'N', Material.NETHERITE_INGOT);

        // --- MINER ---
        add(plugin, "miner_helmet", ItemRegistry.minerHelmet(),
                new String[]{"DDD", "E E"},
                'D', Material.DIAMOND_BLOCK, 'E', Material.EMERALD);
        add(plugin, "miner_chestplate", ItemRegistry.minerChestplate(),
                new String[]{"D D", "DED", "DDD"},
                'D', Material.DIAMOND_BLOCK, 'E', Material.EMERALD);
        add(plugin, "miner_leggings", ItemRegistry.minerLeggings(),
                new String[]{"DDD", "E E", "D D"},
                'D', Material.DIAMOND_BLOCK, 'E', Material.EMERALD);
        add(plugin, "miner_boots", ItemRegistry.minerBoots(),
                new String[]{"E E", "D D"},
                'D', Material.DIAMOND_BLOCK, 'E', Material.EMERALD);

        // --- BLOODHUNTER ---
        add(plugin, "bloodhunter_helmet", ItemRegistry.bloodHunterHelmet(),
                new String[]{"RRR", "D D"},
                'R', Material.REDSTONE_BLOCK, 'D', Material.DIAMOND_BLOCK);
        add(plugin, "bloodhunter_chestplate", ItemRegistry.bloodHunterChestplate(),
                new String[]{"R R", "RDR", "RRR"},
                'R', Material.REDSTONE_BLOCK, 'D', Material.DIAMOND_BLOCK);
        add(plugin, "bloodhunter_leggings", ItemRegistry.bloodHunterLeggings(),
                new String[]{"RRR", "D D", "R R"},
                'R', Material.REDSTONE_BLOCK, 'D', Material.DIAMOND_BLOCK);
        add(plugin, "bloodhunter_boots", ItemRegistry.bloodHunterBoots(),
                new String[]{"D D", "R R"},
                'R', Material.REDSTONE_BLOCK, 'D', Material.DIAMOND_BLOCK);
    }
}