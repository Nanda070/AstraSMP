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
        // Ритуал 0: Ритуальный Кинжал. (Железный меч + Гнилая плоть + Овца = Кинжал)
        recipes.add(new RitualRecipe(
                "sacrificial_dagger_craft",
                List.of(new ItemStack(Material.IRON_SWORD, 1), new ItemStack(Material.ROTTEN_FLESH, 1)),
                EntityType.SHEEP,
                1,
                ItemRegistry.sacrificialDagger(),
                10 // +10 Скверны
        ));

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

        // Ритуал 4.1: Контракт Крови
        recipes.add(new RitualRecipe(
                "pact_blood_craft",
                List.of(ItemRegistry.bloodDrop(), ItemRegistry.demonSoul()),
                EntityType.WITHER_SKELETON,
                3,
                ItemRegistry.pactOfBlood(),
                100 // +100 Скверны
        ));

        // Ритуал 4.2: Контракт Бездны
        recipes.add(new RitualRecipe(
                "pact_void_craft",
                List.of(new ItemStack(Material.ENDER_PEARL, 1), ItemRegistry.demonSoul()),
                EntityType.ENDERMAN,
                3,
                ItemRegistry.pactOfVoid(),
                100 // +100 Скверны
        ));

        // Ритуал 4.3: Контракт Теней
        recipes.add(new RitualRecipe(
                "pact_shadow_craft",
                List.of(new ItemStack(Material.PHANTOM_MEMBRANE, 1), ItemRegistry.demonSoul()),
                EntityType.PHANTOM,
                3,
                ItemRegistry.pactOfShadow(),
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

        // Ритуал 9: Кукла Вуду
        recipes.add(new RitualRecipe(
                "voodoo_doll_craft",
                List.of(ItemRegistry.bloodVial(), new ItemStack(Material.ROTTEN_FLESH, 1), new ItemStack(Material.STRING, 1)),
                EntityType.ZOMBIE,
                2,
                new ItemStack(Material.AIR), // Создается динамически
                25
        ));

        // Ритуал 10: Сфера Безумия
        recipes.add(new RitualRecipe(
                "madness_sphere_craft",
                List.of(ItemRegistry.soulFragment(), ItemRegistry.demonSoul()),
                EntityType.WITCH,
                3,
                new ItemStack(Material.AIR), // Создается динамически
                50
        ));

        // Ритуал 11: Призыв Игрока
        recipes.add(new RitualRecipe(
                "summoning_ritual",
                List.of(ItemRegistry.bloodVial(), new ItemStack(Material.ENDER_PEARL, 1)),
                EntityType.ENDERMAN,
                3,
                new ItemStack(Material.AIR), // Эффект
                50
        ));

        // Ритуал 12: Семя Бездны (Карманное измерение)
        recipes.add(new RitualRecipe(
                "seed_of_abyss_ritual",
                List.of(
                        new ItemStack(Material.NETHER_STAR, 1),
                        ItemRegistry.soulFragment(),
                        ItemRegistry.bloodVial()
                ),
                EntityType.WITHER_SKELETON,
                3,
                ItemRegistry.seedOfAbyss(),
                250
        ));

        // Ритуал 13: Астральная Проекция
        recipes.add(new RitualRecipe(
                "astral_ritual",
                List.of(
                        ItemRegistry.soulFragment(),
                        new ItemStack(Material.PHANTOM_MEMBRANE, 1),
                        ItemRegistry.bloodVial()
                ),
                EntityType.PHANTOM,
                3,
                ItemRegistry.astralCrystal(),
                150
        ));

        // Фоновый таск для Безумия (Одержимость)
        startMadnessTask();
    }

    private void startMadnessTask() {
        org.bukkit.Bukkit.getScheduler().runTaskTimer(com.astrasmp.AstraSMPPlugin.getInstance(), () -> {
            org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(com.astrasmp.AstraSMPPlugin.getInstance(), "madness_until");
            for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                org.bukkit.persistence.PersistentDataContainer data = player.getPersistentDataContainer();
                if (data.has(key, org.bukkit.persistence.PersistentDataType.LONG)) {
                    long expiry = data.get(key, org.bukkit.persistence.PersistentDataType.LONG);
                    if (System.currentTimeMillis() < expiry) {
                        // Игрок одержим, 20% шанс на пугалку каждую секунду
                        if (Math.random() < 0.20) {
                            int scareType = new java.util.Random().nextInt(4);
                            switch (scareType) {
                                case 0 -> player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_CREEPER_PRIMED, 1f, 1f);
                                case 1 -> player.playSound(player.getLocation(), org.bukkit.Sound.AMBIENT_CAVE, 1f, 0.5f);
                                case 2 -> {
                                    player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS, 60, 0));
                                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_STARE, 1f, 0.5f);
                                }
                                case 3 -> player.damage(0.1); // Фейковый урон
                            }
                        }
                    } else {
                        data.remove(key);
                        player.sendMessage("§a[Безумие] §fВаш разум прояснился. Мрак отступил.");
                    }
                }
            }
        }, 100L, 100L); // Каждые 5 секунд проверка (100 тиков)
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
        // Ищем целевые UUID в предметах (до того как они исчезли или после, но мы сохраним их)
        String targetUuid = null;
        String targetName = null;
        for (Item groundItem : groundItems) {
            org.bukkit.persistence.PersistentDataContainer data = groundItem.getItemStack().getItemMeta().getPersistentDataContainer();
            if (data.has(new org.bukkit.NamespacedKey(com.astrasmp.AstraSMPPlugin.getInstance(), "blood_target_uuid"), org.bukkit.persistence.PersistentDataType.STRING)) {
                targetUuid = data.get(new org.bukkit.NamespacedKey(com.astrasmp.AstraSMPPlugin.getInstance(), "blood_target_uuid"), org.bukkit.persistence.PersistentDataType.STRING);
                targetName = data.get(new org.bukkit.NamespacedKey(com.astrasmp.AstraSMPPlugin.getInstance(), "blood_target_name"), org.bukkit.persistence.PersistentDataType.STRING);
            }
            if (data.has(new org.bukkit.NamespacedKey(com.astrasmp.AstraSMPPlugin.getInstance(), "soul_target_uuid"), org.bukkit.persistence.PersistentDataType.STRING)) {
                targetUuid = data.get(new org.bukkit.NamespacedKey(com.astrasmp.AstraSMPPlugin.getInstance(), "soul_target_uuid"), org.bukkit.persistence.PersistentDataType.STRING);
                targetName = data.get(new org.bukkit.NamespacedKey(com.astrasmp.AstraSMPPlugin.getInstance(), "soul_target_name"), org.bukkit.persistence.PersistentDataType.STRING);
            }
        }

        boolean consumeItems = !com.astrasmp.AstraSMPPlugin.getInstance().getServices().bloodMoon().isBloodMoonActive();

        // Удаляем только необходимое количество предметов
        if (consumeItems) {
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
        }

        // Эффекты
        center.getWorld().strikeLightningEffect(center);
        center.getWorld().spawnParticle(Particle.FLAME, center.clone().add(0, 1, 0), 100, 0.5, 0.5, 0.5, 0.1);
        center.getWorld().playSound(center, Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f);

        // Спавн награды (если есть статичная)
        if (recipe.getResult() != null && !recipe.getResult().getType().isAir()) {
            center.getWorld().dropItem(center.clone().add(0, 1, 0), recipe.getResult());
        }

        // Заражение чанка
        int corruptionAmount = com.astrasmp.AstraSMPPlugin.getInstance().getServices().bloodMoon().isBloodMoonActive() ? 30 : 10;
        com.astrasmp.AstraSMPPlugin.getInstance().getServices().corruption().addCorruption(center.getChunk(), corruptionAmount);

        // Кастомные эффекты по ID рецепта
        if (recipe.getId().equals("rift_summon")) {
            com.astrasmp.AstraSMPPlugin.getInstance().getServices().rift().createRift(center);
        } else if (recipe.getId().equals("cleansing_ritual") && killer != null) {
            killer.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, center, 200, 1, 1, 1, 0.5);
            com.astrasmp.AstraSMPPlugin.getInstance().getServices().corruption().setCorruption(center.getChunk(), 0);
            killer.sendMessage("§e[Очищение] §fСвет омывает вашу душу и очищает землю, смывая пятна Скверны.");
        } else if (recipe.getId().equals("pact_break_ritual") && killer != null) {
            boolean hasPact = com.astrasmp.AstraSMPPlugin.getInstance().getServices().store().profile(killer.getUniqueId().toString(), null).hasPact();
            if (hasPact) {
                com.astrasmp.AstraSMPPlugin.getInstance().getServices().store().profile(killer.getUniqueId().toString(), null).setPactType("");
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
                if (killer != null) killer.sendMessage("§c[Контракт] У вас нет активного контракта для разрыва.");
            }
        } else if (recipe.getId().equals("voodoo_doll_craft")) {
            if (targetUuid != null) {
                center.getWorld().dropItem(center.clone().add(0, 1, 0), ItemRegistry.voodooDoll(targetName, targetUuid));
                if (killer != null) killer.sendMessage("§4[Ритуал] §cКукла Вуду создана. Теперь судьба " + targetName + " в ваших руках.");
            } else {
                if (killer != null) killer.sendMessage("§cКровь была испорчена. Ритуал не удался.");
            }
        } else if (recipe.getId().equals("madness_sphere_craft")) {
            if (targetUuid != null) {
                center.getWorld().dropItem(center.clone().add(0, 1, 0), ItemRegistry.madnessSphere(targetName, targetUuid));
                if (killer != null) killer.sendMessage("§5[Ритуал] §dСфера Безумия соткана из ужаса. Используйте её на " + targetName + ".");
            } else {
                if (killer != null) killer.sendMessage("§cОсколок души был пуст. Ритуал не удался.");
            }
        } else if (recipe.getId().equals("summoning_ritual")) {
            if (targetUuid != null) {
                org.bukkit.entity.Player target = org.bukkit.Bukkit.getPlayer(java.util.UUID.fromString(targetUuid));
                if (target != null && target.isOnline()) {
                    target.teleport(center.clone().add(0, 1, 0));
                    target.sendMessage("§4[Призыв] §cВас насильно притянули во Врата Тьмы!");
                    if (killer != null) killer.sendMessage("§4[Ритуал] §cЖертва прибыла.");
                    
                    // Барьер
                    for (int i = 0; i < 20; i++) {
                        org.bukkit.Bukkit.getScheduler().runTaskLater(com.astrasmp.AstraSMPPlugin.getInstance(), () -> {
                            for (double t = 0; t <= 2 * Math.PI; t += Math.PI / 8) {
                                double x = 3 * Math.cos(t);
                                double z = 3 * Math.sin(t);
                                center.getWorld().spawnParticle(Particle.PORTAL, center.clone().add(x, 1, z), 5, 0, 1, 0, 0);
                            }
                        }, i * 10L);
                    }
                } else {
                    if (killer != null) killer.sendMessage("§cИгрок оффлайн. Ритуал потрачен впустую.");
                }
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
