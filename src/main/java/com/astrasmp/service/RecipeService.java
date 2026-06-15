package com.astrasmp.service;

import com.astrasmp.AstraSMPPlugin;
import com.astrasmp.items.ItemRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;

public final class RecipeService implements Listener {
    private final AstraSMPPlugin plugin;

    public RecipeService(AstraSMPPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerAll() {
        Bukkit.getConsoleSender().sendMessage("§b[AstraSMP] Регистрация рецептов (С ручной NBT-валидацией)...");

        // --- ИНСТРУМЕНТЫ ---
        register("miner_3x3", ItemRegistry.mining3x3(), " D ", "DND", " D ",
                'D', Material.DIAMOND_BLOCK, 'N', Material.NETHERITE_PICKAXE);

        register("miner_5x5", ItemRegistry.mining5x5(), "BNB", "NMN", "BNB",
                'B', Material.NETHERITE_BLOCK, 'M', Material.NETHERITE_PICKAXE, 'N', Material.NETHERITE_INGOT);

        register("vein_miner", ItemRegistry.veinMiner(), "AAA", "ANA", "AAA",
                'A', Material.AMETHYST_CLUSTER, 'N', Material.NETHERITE_PICKAXE);

        register("auto_smelt", ItemRegistry.autoSmelt(), "MMM", "MNM", "MMM",
                'M', Material.MAGMA_BLOCK, 'N', Material.NETHERITE_PICKAXE);

        register("magnet", ItemRegistry.magnet(), "GIG", "ICI", "GIG",
                'G', Material.GOLD_INGOT, 'I', Material.IRON_INGOT, 'C', Material.COMPASS);

        // --- ОРУЖИЕ ---
        register("shadow_blade", ItemRegistry.shadowBlade(), "ECE", "ESE", "ECE",
                'E', Material.ECHO_SHARD, 'S', Material.NETHERITE_SWORD, 'C', Material.COAL_BLOCK);

        register("thunder_hammer", ItemRegistry.thunderHammer(), "MLM", "MAM", "MMM",
                'M', Material.COPPER_BLOCK, 'A', Material.NETHERITE_AXE, 'L', Material.LIGHTNING_ROD);

        register("vampire_dagger", ItemRegistry.vampireDagger(), "RGR", "RSR", "RGR",
                'R', Material.REDSTONE_BLOCK, 'S', Material.DIAMOND_SWORD, 'G', Material.GHAST_TEAR);

        register("inferno_sword", ItemRegistry.infernoSword(), "IBI", "ISI", "IBI",
                'I', Material.BLAZE_ROD, 'S', Material.NETHERITE_SWORD, 'B', Material.MAGMA_CREAM);

        register("frost_axe", ItemRegistry.frostAxe(), "III", "IAI", "III",
                'I', Material.PACKED_ICE, 'A', Material.DIAMOND_AXE);

        register("venom_bow", ItemRegistry.venomBow(), " S ", "SBS", " S ",
                'S', Material.SPIDER_EYE, 'B', Material.BOW);

        register("reaper_scythe", ItemRegistry.reaperScythe(), "NNN", " H ", " N ",
                'N', Material.NETHERITE_BLOCK, 'H', Material.NETHERITE_HOE);

        // --- БРОНЯ ---
        // Mercenary
        register("mercenary_helmet", ItemRegistry.mercenaryHelmet(), "CLC", "C C", "   ", 'C', Material.COAL_BLOCK, 'L', Material.LEATHER);
        register("mercenary_chestplate", ItemRegistry.mercenaryChestplate(), "C C", "CLC", "CCC", 'C', Material.COAL_BLOCK, 'L', Material.LEATHER);
        register("mercenary_leggings", ItemRegistry.mercenaryLeggings(), "CCC", "L L", "C C", 'C', Material.COAL_BLOCK, 'L', Material.LEATHER);
        register("mercenary_boots", ItemRegistry.mercenaryBoots(), "   ", "C C", "L L", 'C', Material.COAL_BLOCK, 'L', Material.LEATHER);

        // Berserker
        register("berserker_helmet", ItemRegistry.berserkerHelmet(), "III", "R R", "   ", 'I', Material.IRON_BLOCK, 'R', Material.REDSTONE_BLOCK);
        register("berserker_chestplate", ItemRegistry.berserkerChestplate(), "I I", "IRI", "III", 'I', Material.IRON_BLOCK, 'R', Material.REDSTONE_BLOCK);
        register("berserker_leggings", ItemRegistry.berserkerLeggings(), "III", "R R", "I I", 'I', Material.IRON_BLOCK, 'R', Material.REDSTONE_BLOCK);
        register("berserker_boots", ItemRegistry.berserkerBoots(), "   ", "R R", "I I", 'I', Material.IRON_BLOCK, 'R', Material.REDSTONE_BLOCK);

        // Inquisitor
        register("inquisitor_helmet", ItemRegistry.inquisitorHelmet(), "GGG", "I I", "   ", 'G', Material.GOLD_BLOCK, 'I', Material.IRON_BLOCK);
        register("inquisitor_chestplate", ItemRegistry.inquisitorChestplate(), "G G", "GIG", "GGG", 'G', Material.GOLD_BLOCK, 'I', Material.IRON_BLOCK);
        register("inquisitor_leggings", ItemRegistry.inquisitorLeggings(), "GGG", "I I", "G G", 'G', Material.GOLD_BLOCK, 'I', Material.IRON_BLOCK);
        register("inquisitor_boots", ItemRegistry.inquisitorBoots(), "   ", "I I", "G G", 'G', Material.GOLD_BLOCK, 'I', Material.IRON_BLOCK);

        // Juggernaut
        register("juggernaut_helmet", ItemRegistry.juggernautHelmet(), "OOO", "N N", "   ", 'O', Material.OBSIDIAN, 'N', Material.NETHERITE_INGOT);
        register("juggernaut_chestplate", ItemRegistry.juggernautChestplate(), "O O", "ONO", "OOO", 'O', Material.OBSIDIAN, 'N', Material.NETHERITE_INGOT);
        register("juggernaut_leggings", ItemRegistry.juggernautLeggings(), "OOO", "N N", "O O", 'O', Material.OBSIDIAN, 'N', Material.NETHERITE_INGOT);
        register("juggernaut_boots", ItemRegistry.juggernautBoots(), "   ", "N N", "O O", 'O', Material.OBSIDIAN, 'N', Material.NETHERITE_INGOT);

        // Miner
        register("miner_helmet", ItemRegistry.minerHelmet(), "DDD", "E E", "   ", 'D', Material.DIAMOND_BLOCK, 'E', Material.EMERALD);
        register("miner_chestplate", ItemRegistry.minerChestplate(), "D D", "DED", "DDD", 'D', Material.DIAMOND_BLOCK, 'E', Material.EMERALD);
        register("miner_leggings", ItemRegistry.minerLeggings(), "DDD", "E E", "D D", 'D', Material.DIAMOND_BLOCK, 'E', Material.EMERALD);
        register("miner_boots", ItemRegistry.minerBoots(), "   ", "E E", "D D", 'D', Material.DIAMOND_BLOCK, 'E', Material.EMERALD);

        // Blood Hunter
        register("bloodhunter_helmet", ItemRegistry.bloodHunterHelmet(), "RRR", "D D", "   ", 'R', Material.REDSTONE_BLOCK, 'D', Material.DIAMOND_BLOCK);
        register("bloodhunter_chestplate", ItemRegistry.bloodHunterChestplate(), "R R", "RDR", "RRR", 'R', Material.REDSTONE_BLOCK, 'D', Material.DIAMOND_BLOCK);
        register("bloodhunter_leggings", ItemRegistry.bloodHunterLeggings(), "RRR", "D D", "R R", 'R', Material.REDSTONE_BLOCK, 'D', Material.DIAMOND_BLOCK);
        register("bloodhunter_boots", ItemRegistry.bloodHunterBoots(), "   ", "D D", "R R", 'R', Material.REDSTONE_BLOCK, 'D', Material.DIAMOND_BLOCK);

        // --- ТОТЕМЫ ---
        register("totem_speed", ItemRegistry.totemSpeed(), "SSS", "STS", "SSS",
                'S', Material.SUGAR, 'T', Material.TOTEM_OF_UNDYING);

        register("totem_shield", ItemRegistry.totemShield(), "OOO", "OTO", "OOO",
                'O', Material.OBSIDIAN, 'T', Material.TOTEM_OF_UNDYING);

        register("totem_lightning", ItemRegistry.totemLightning(), "LLL", "LTL", "LLL",
                'L', Material.LIGHTNING_ROD, 'T', Material.TOTEM_OF_UNDYING);

        register("totem_explosion", ItemRegistry.totemExplosion(), "XXX", "XTX", "XXX",
                'X', Material.TNT, 'T', Material.TOTEM_OF_UNDYING);

        register("totem_teleport", ItemRegistry.totemTeleport(), "PPP", "PTP", "PPP",
                'P', Material.ENDER_PEARL, 'T', Material.TOTEM_OF_UNDYING);

        // --- ТЕМНАЯ МАГИЯ (КРАФТЫ) ---
        register("sacrificial_dagger", ItemRegistry.sacrificialDagger(), " I ", " G ", " S ",
                'I', Material.IRON_INGOT, 'G', Material.GOLD_INGOT, 'S', Material.STICK);

        register("ritual_altar", ItemRegistry.ritualAltar(), "DOD", "ORO", "DOD",
                'D', Material.DIAMOND, 'O', Material.OBSIDIAN, 'R', Material.REDSTONE_BLOCK);

        // ==========================================
        // КОМПОНЕНТЫ ME-СЕТИ
        // ==========================================

        register("me_controller", ItemRegistry.meController(), "IQI", "QRQ", "IQI",
                'I', Material.IRON_BLOCK, 'Q', Material.QUARTZ, 'R', Material.REDSTONE_BLOCK);

        register("me_drive", ItemRegistry.meDrive(), "III", " C ", "IRI",
                'I', Material.IRON_INGOT, 'C', Material.CHEST, 'R', Material.REDSTONE_BLOCK);

        register("me_terminal", ItemRegistry.meTerminal(), "IGI", "GRG", "IGI",
                'I', Material.IRON_INGOT, 'G', Material.GLASS, 'R', Material.REDSTONE);

        register("me_cell_4k", ItemRegistry.meCell4k(), " R ", "RQR", " R ",
                'R', Material.REDSTONE, 'Q', Material.QUARTZ);

        // Для сложных ячеек используем базовые материалы (Кремень и Железо),
        // но событие ниже запретит крафт, если это не кастомная ячейка
        register("me_cell_16k", ItemRegistry.meCell16k(), " G ", "GCG", " G ",
                'G', Material.GOLD_INGOT, 'C', Material.FLINT); // FLINT = база для 4k

        register("me_cell_64k", ItemRegistry.meCell64k(), " D ", "DCD", " D ",
                'D', Material.DIAMOND, 'C', Material.IRON_INGOT); // IRON_INGOT = база для 16k
    }

    private void register(String key, ItemStack result, String row1, String row2, String row3, Object... ingredients) {
        try {
            ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(plugin, key), result);
            recipe.shape(row1, row2, row3);

            for (int i = 0; i < ingredients.length; i += 2) {
                Character symbol = (Character) ingredients[i];
                Material mat = (Material) ingredients[i + 1];
                if (mat != Material.AIR) {
                    recipe.setIngredient(symbol, mat);
                }
            }

            Bukkit.removeRecipe(new NamespacedKey(plugin, key));
            Bukkit.addRecipe(recipe);
        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка при регистрации рецепта " + key + ": " + e.getMessage());
        }
    }

    /**
     * ЖЕСТКАЯ ВАЛИДАЦИЯ NBT:
     * Проверяем, чтобы игроки не могли скрафтить 16K ячейку из обычного кремня.
     */
    @EventHandler
    public void onCraftPrepare(PrepareItemCraftEvent event) {
        if (event.getRecipe() == null) return;

        ItemStack result = event.getRecipe().getResult();
        if (result.getType() == Material.AIR) return;

        // Получаем наш кастомный ID результата
        String resultId = ItemRegistry.id(result);
        if (resultId == null) return;

        CraftingInventory inv = event.getInventory();
        ItemStack[] matrix = inv.getMatrix(); // Индексы 0-8, центр = 4

        // Валидация ячейки 16K (В центре должна быть строго ячейка 4K)
        if (resultId.equals("me_cell_16k")) {
            ItemStack centerItem = matrix[4];
            if (centerItem == null || !ItemRegistry.is(centerItem, "me_cell_4k")) {
                inv.setResult(null); // Блокируем крафт (пустой результат)
            }
        }
        // Валидация ячейки 64K (В центре должна быть строго ячейка 16K)
        else if (resultId.equals("me_cell_64k")) {
            ItemStack centerItem = matrix[4];
            if (centerItem == null || !ItemRegistry.is(centerItem, "me_cell_16k")) {
                inv.setResult(null); // Блокируем крафт
            }
        }
    }
}