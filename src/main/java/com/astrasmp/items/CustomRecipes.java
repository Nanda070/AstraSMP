package com.astrasmp.items;

import com.astrasmp.AstraSMPPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;

public final class CustomRecipes {

    private CustomRecipes() {}

    public static void register(AstraSMPPlugin plugin) {
        // Броня
        registerBerserker(plugin);
        registerJuggernaut(plugin);

        // Инструменты и Оружие
        registerTools(plugin);
        registerWeapons(plugin);
    }

    private static void registerTools(AstraSMPPlugin plugin) {
        // --- КРУШИТЕЛЬ 3x3 ---
        ShapedRecipe mining3x3 = new ShapedRecipe(new NamespacedKey(plugin, "recipe_mining3x3"), ItemRegistry.mining3x3());
        mining3x3.shape("DDD", "TST", " S ");
        mining3x3.setIngredient('D', Material.DIAMOND_BLOCK);
        mining3x3.setIngredient('T', Material.TNT);
        mining3x3.setIngredient('S', Material.STICK);
        Bukkit.addRecipe(mining3x3);

        // --- МАГНИТ ---
        ShapedRecipe magnet = new ShapedRecipe(new NamespacedKey(plugin, "recipe_magnet"), ItemRegistry.magnet());
        magnet.shape("RIR", "IPI", "RIR");
        magnet.setIngredient('R', Material.REDSTONE_BLOCK);
        magnet.setIngredient('I', Material.IRON_BLOCK);
        magnet.setIngredient('P', Material.DIAMOND_PICKAXE);
        Bukkit.addRecipe(magnet);
    }

    private static void registerWeapons(AstraSMPPlugin plugin) {
        // --- ЛЕДЯНОЙ ТОПОР ---
        ShapedRecipe frostAxe = new ShapedRecipe(new NamespacedKey(plugin, "recipe_frost_axe"), ItemRegistry.frostAxe());
        frostAxe.shape("BBB", "BAB", "BBB");
        frostAxe.setIngredient('B', Material.BLUE_ICE);
        frostAxe.setIngredient('A', Material.DIAMOND_AXE);
        Bukkit.addRecipe(frostAxe);

        // --- ВАМПИРСКИЙ КИНЖАЛ ---
        ShapedRecipe vampireDagger = new ShapedRecipe(new NamespacedKey(plugin, "recipe_vampire_dagger"), ItemRegistry.vampireDagger());
        vampireDagger.shape(" R ", "GSG", " R ");
        vampireDagger.setIngredient('R', Material.REDSTONE_BLOCK);
        vampireDagger.setIngredient('G', Material.GHAST_TEAR);
        vampireDagger.setIngredient('S', Material.DIAMOND_SWORD);
        Bukkit.addRecipe(vampireDagger);

        // --- МЕЧ ИНФЕРНО ---
        ShapedRecipe infernoSword = new ShapedRecipe(new NamespacedKey(plugin, "recipe_inferno_sword"), ItemRegistry.infernoSword());
        infernoSword.shape("MBM", "BSB", "MBM");
        infernoSword.setIngredient('M', Material.MAGMA_BLOCK);
        infernoSword.setIngredient('B', Material.BLAZE_ROD);
        infernoSword.setIngredient('S', Material.NETHERITE_SWORD);
        Bukkit.addRecipe(infernoSword);
    }

    private static void registerBerserker(AstraSMPPlugin plugin) {
        // --- ШЛЕМ БЕРСЕРКА ---
        ShapedRecipe helmet = new ShapedRecipe(new NamespacedKey(plugin, "berserker_helmet"), ItemRegistry.berserkerHelmet());
        helmet.shape("III", "R R");
        helmet.setIngredient('I', Material.IRON_BLOCK);
        helmet.setIngredient('R', Material.REDSTONE_BLOCK);
        Bukkit.addRecipe(helmet);

        // --- НАГРУДНИК БЕРСЕРКА ---
        ShapedRecipe chestplate = new ShapedRecipe(new NamespacedKey(plugin, "berserker_chestplate"), ItemRegistry.berserkerChestplate());
        chestplate.shape("I I", "IRI", "III");
        chestplate.setIngredient('I', Material.IRON_BLOCK);
        chestplate.setIngredient('R', Material.REDSTONE_BLOCK);
        Bukkit.addRecipe(chestplate);

        // --- ПОНОЖИ БЕРСЕРКА ---
        ShapedRecipe leggings = new ShapedRecipe(new NamespacedKey(plugin, "berserker_leggings"), ItemRegistry.berserkerLeggings());
        leggings.shape("III", "R R", "I I");
        leggings.setIngredient('I', Material.IRON_BLOCK);
        leggings.setIngredient('R', Material.REDSTONE_BLOCK);
        Bukkit.addRecipe(leggings);

        // --- БОТИНКИ БЕРСЕРКА ---
        ShapedRecipe boots = new ShapedRecipe(new NamespacedKey(plugin, "berserker_boots"), ItemRegistry.berserkerBoots());
        boots.shape("R R", "I I");
        boots.setIngredient('I', Material.IRON_BLOCK);
        boots.setIngredient('R', Material.REDSTONE_BLOCK);
        Bukkit.addRecipe(boots);
    }

    private static void registerJuggernaut(AstraSMPPlugin plugin) {
        // --- ШЛЕМ ДЖАГГЕРНАУТА ---
        ShapedRecipe helmet = new ShapedRecipe(new NamespacedKey(plugin, "juggernaut_helmet"), ItemRegistry.juggernautHelmet());
        helmet.shape("OOO", "N N");
        helmet.setIngredient('O', Material.OBSIDIAN);
        helmet.setIngredient('N', Material.NETHERITE_INGOT);
        Bukkit.addRecipe(helmet);

        // --- НАГРУДНИК ДЖАГГЕРНАУТА ---
        ShapedRecipe chestplate = new ShapedRecipe(new NamespacedKey(plugin, "juggernaut_chestplate"), ItemRegistry.juggernautChestplate());
        chestplate.shape("O O", "OAO", "OOO");
        chestplate.setIngredient('O', Material.OBSIDIAN);
        // Убрали строку с 'N', так как Незеритового слитка в нагруднике больше нет
        chestplate.setIngredient('A', Material.GOLDEN_APPLE);
        Bukkit.addRecipe(chestplate);

        // --- ПОНОЖИ ДЖАГГЕРНАУТА ---
        ShapedRecipe leggings = new ShapedRecipe(new NamespacedKey(plugin, "juggernaut_leggings"), ItemRegistry.juggernautLeggings());
        leggings.shape("OOO", "N N", "O O");
        leggings.setIngredient('O', Material.OBSIDIAN);
        leggings.setIngredient('N', Material.NETHERITE_INGOT);
        Bukkit.addRecipe(leggings);

        // --- БОТИНКИ ДЖАГГЕРНАУТА ---
        ShapedRecipe boots = new ShapedRecipe(new NamespacedKey(plugin, "juggernaut_boots"), ItemRegistry.juggernautBoots());
        boots.shape("N N", "O O");
        boots.setIngredient('O', Material.OBSIDIAN);
        boots.setIngredient('N', Material.NETHERITE_INGOT);
        Bukkit.addRecipe(boots);
    }
}