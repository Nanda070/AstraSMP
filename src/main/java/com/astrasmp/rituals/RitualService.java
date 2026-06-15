package com.astrasmp.rituals;

import com.astrasmp.AstraSMPPlugin;
import com.astrasmp.items.ItemRegistry;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RitualService {

    private final RitualCircleManager circleManager;
    private final List<RitualRecipe> recipes = new ArrayList<>();

    public RitualService(AstraSMPPlugin plugin) {
        this.circleManager = new RitualCircleManager();
        loadRecipes();
    }

    public RitualCircleManager getCircleManager() {
        return circleManager;
    }

    private void loadRecipes() {
        // Ритуал 1: Кровь. (Гнилая плоть + Свинья = Капля крови)
        recipes.add(new RitualRecipe(
                "blood_extraction",
                List.of(new ItemStack(Material.ROTTEN_FLESH, 1)),
                EntityType.PIG,
                1,
                ItemRegistry.bloodDrop(),
                5 // +5 Скверны
        ));

        // Ритуал 2: Душа демона. (Алмаз + Иссушитель/Игрок = Душа)
        // Для примера пусть будет Зомби, чтобы было проще тестировать
        recipes.add(new RitualRecipe(
                "demon_soul",
                List.of(new ItemStack(Material.DIAMOND, 1)),
                EntityType.ZOMBIE,
                2,
                ItemRegistry.demonSoul(),
                15 // +15 Скверны
        ));

        // Ритуал 3: Теневой клинок
        recipes.add(new RitualRecipe(
                "shadow_blade_craft",
                List.of(new ItemStack(Material.NETHERITE_SWORD, 1), ItemRegistry.bloodDrop()),
                EntityType.PLAYER,
                3,
                ItemRegistry.shadowBlade(),
                50 // +50 Скверны
        ));

        // Ритуал 4: Демонический контракт
        recipes.add(new RitualRecipe(
                "pact_creation",
                List.of(new ItemStack(Material.PAPER, 1), ItemRegistry.demonSoul()),
                EntityType.WITHER_SKELETON,
                3,
                ItemRegistry.demonicPact(),
                100 // +100 Скверны
        ));

        // Ритуал 5: Врата Бездны
        recipes.add(new RitualRecipe(
                "rift_summon",
                List.of(ItemRegistry.demonSoul(), new ItemStack(Material.CRYING_OBSIDIAN, 1)),
                EntityType.ENDERMAN,
                3,
                new ItemStack(Material.AIR), // Результат - само появление врат
                200 // +200 Скверны
        ));

        // Ритуал 6: Очищение Скверны
        recipes.add(new RitualRecipe(
                "cleansing_ritual",
                List.of(new ItemStack(Material.GOLDEN_APPLE, 1), new ItemStack(Material.GHAST_TEAR, 1)),
                EntityType.VILLAGER,
                2,
                new ItemStack(Material.AIR),
                -50 // -50 Скверны
        ));

        // Ритуал 7: Разрыв Контракта
        recipes.add(new RitualRecipe(
                "pact_break_ritual",
                List.of(ItemRegistry.cleansingTotem(), new ItemStack(Material.DIAMOND_BLOCK, 1)),
                EntityType.WITHER_SKELETON,
                3,
                new ItemStack(Material.AIR),
                -100 // -100 Скверны
        ));

        // Ритуал 8: Кровавая Чаша
        recipes.add(new RitualRecipe(
                "blood_chalice_craft",
                List.of(ItemRegistry.bloodDrop(), new ItemStack(Material.CAULDRON, 1), new ItemStack(Material.GOLD_INGOT, 1)),
                EntityType.WITCH,
                2,
                ItemRegistry.bloodChalice(),
                20 // +20 Скверны
        ));
    }

    /**
     * Пытается выполнить ритуал. Вызывается при смерти существа в круге.
     */
    public boolean attemptRitual(Location center, int tier, EntityType victimType, org.bukkit.entity.Player killer) {
        // Ищем выброшенные предметы (Item) в радиусе 2 блоков от центра
        List<Item> nearbyItems = center.getWorld().getNearbyEntities(center, 2, 2, 2).stream()
                .filter(e -> e instanceof Item)
                .map(e -> (Item) e)
                .collect(Collectors.toList());

        List<ItemStack> groundItemStacks = nearbyItems.stream()
                .map(Item::getItemStack)
                .collect(Collectors.toList());

        for (RitualRecipe recipe : recipes) {
            if (recipe.matches(groundItemStacks, victimType, tier)) {
                executeRitual(center, recipe, nearbyItems, killer);
                return true;
            }
        }
        return false;
    }

    private void executeRitual(Location center, RitualRecipe recipe, List<Item> groundItems, org.bukkit.entity.Player killer) {
        // Удаляем только необходимое количество предметов
        for (ItemStack req : recipe.getRequiredItems()) {
            int needed = req.getAmount();
            for (Item groundItem : groundItems) {
                if (needed <= 0) break;
                ItemStack stack = groundItem.getItemStack();
                if (stack.getType() == req.getType()) {
                    if (stack.getAmount() <= needed) {
                        needed -= stack.getAmount();
                        groundItem.remove();
                    } else {
                        stack.setAmount(stack.getAmount() - needed);
                        groundItem.setItemStack(stack);
                        needed = 0;
                    }
                }
            }
        }

        // Эффекты
        center.getWorld().strikeLightningEffect(center);
        center.getWorld().spawnParticle(Particle.FLAME, center.clone().add(0, 1, 0), 100, 0.5, 0.5, 0.5, 0.1);
        center.getWorld().playSound(center, Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f);

        // Спавн награды (если есть)
        if (recipe.getResult() != null && !recipe.getResult().getType().isAir()) {
            center.getWorld().dropItem(center.clone().add(0, 1, 0), recipe.getResult());
        }

        // Кастомные эффекты по ID рецепта
        if (recipe.getId().equals("rift_summon")) {
            com.astrasmp.AstraSMPPlugin.getInstance().getServices().rift().createRift(center);
        } else if (recipe.getId().equals("cleansing_ritual") && killer != null) {
            killer.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, center, 200, 1, 1, 1, 0.5);
            killer.sendMessage("§e[Очищение] §fСвет омывает вашу душу, смывая пятна Скверны.");
        } else if (recipe.getId().equals("pact_break_ritual") && killer != null) {
            boolean hasPact = com.astrasmp.AstraSMPPlugin.getInstance().getServices().store().profile(killer.getUniqueId().toString(), null).hasPact();
            if (hasPact) {
                com.astrasmp.AstraSMPPlugin.getInstance().getServices().store().profile(killer.getUniqueId().toString(), null).setHasPact(false);
                org.bukkit.attribute.AttributeInstance maxHp = killer.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
                if (maxHp != null) {
                    org.bukkit.attribute.AttributeModifier modifier = new org.bukkit.attribute.AttributeModifier(
                            new org.bukkit.NamespacedKey(com.astrasmp.AstraSMPPlugin.getInstance(), "demonic_pact_debuff"),
                            -8.0,
                            org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER
                    );
                    maxHp.removeModifier(modifier);
                }
                killer.getWorld().spawnParticle(Particle.HEART, center, 100, 1, 1, 1, 0.1);
                killer.sendMessage("§e[Контракт] §fВы разорвали демонический контракт и вернули свою жизненную силу!");
            } else {
                killer.sendMessage("§c[Контракт] У вас нет активного контракта для разрыва.");
            }
        }

        // Начисляем/отнимаем скверну
        if (killer != null && recipe.getCorruptionReward() != 0) {
            int reward = recipe.getCorruptionReward();
            // Удвоенная Скверна во время Кровавой Ночи (только для получения, а не очищения)
            if (reward > 0 && com.astrasmp.AstraSMPPlugin.getInstance().getServices().events().isBloodNight()) {
                reward *= 2;
                killer.sendMessage("§4[Кровавая Луна] §cВаша душа жадно впитывает удвоенную Скверну...");
            }

            int current = com.astrasmp.AstraSMPPlugin.getInstance().getServices().store().profile(killer.getUniqueId().toString(), null).getCorruption();
            int newAmount = Math.max(0, current + reward);
            com.astrasmp.AstraSMPPlugin.getInstance().getServices().store().profile(killer.getUniqueId().toString(), null).setCorruption(newAmount);
            com.astrasmp.AstraSMPPlugin.getInstance().getServices().store().requestSave();
            
            if (reward > 0) {
                killer.sendMessage("§5[Скверна] §dВаша душа покрылась пятнами Скверны (+" + reward + ")");
            } else {
                killer.sendMessage("§e[Скверна] §fВы очистили часть Скверны (" + reward + ")");
            }
        }
    }
}
