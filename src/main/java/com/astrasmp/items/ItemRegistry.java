package com.astrasmp.items;

import com.astrasmp.AstraSMPPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import com.astrasmp.util.TextUtil;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class ItemRegistry {
    private static NamespacedKey ITEM_ID;
    private static NamespacedKey ITEM_UPGRADE;

    private ItemRegistry() {}

    public static void init(AstraSMPPlugin plugin) {
        ITEM_ID = new NamespacedKey("astrasmp", "custom_id");
        ITEM_UPGRADE = new NamespacedKey("astrasmp", "upgrade_level");
    }

    public static String id(ItemStack stack) {
        if (stack == null || stack.getType().isAir() || !stack.hasItemMeta()) return null;
        if (ITEM_ID == null) ITEM_ID = new NamespacedKey("astrasmp", "custom_id");
        return stack.getItemMeta().getPersistentDataContainer().get(ITEM_ID, PersistentDataType.STRING);
    }

    public static boolean is(ItemStack stack, String expected) {
        String id = id(stack);
        return expected != null && expected.equalsIgnoreCase(id);
    }

    public static int getUpgradeLevel(ItemStack stack) {
        if (stack == null || stack.getType().isAir() || !stack.hasItemMeta()) return 0;
        if (ITEM_UPGRADE == null) ITEM_UPGRADE = new NamespacedKey("astrasmp", "upgrade_level");
        Integer level = stack.getItemMeta().getPersistentDataContainer().get(ITEM_UPGRADE, PersistentDataType.INTEGER);
        return level == null ? 0 : level;
    }

    public static void setUpgradeLevel(ItemStack stack, int level) {
        if (stack == null || stack.getType().isAir() || !stack.hasItemMeta()) return;
        if (ITEM_UPGRADE == null) ITEM_UPGRADE = new NamespacedKey("astrasmp", "upgrade_level");
        ItemMeta meta = stack.getItemMeta();
        meta.getPersistentDataContainer().set(ITEM_UPGRADE, PersistentDataType.INTEGER, level);
        
        // Update display name
        String id = id(stack);
        if (id != null) {
            // Read base name from somewhere or just prepend [+X]. 
            // Wait, legacy displayName could already have [+X]. So let's strip it first.
            String baseName = getBaseName(id);
            if (baseName != null) {
                if (level > 0) {
                    meta.displayName(LegacyComponentSerializer.legacySection().deserialize("§a[+" + level + "] §r" + baseName));
                } else {
                    meta.displayName(LegacyComponentSerializer.legacySection().deserialize(baseName));
                }
            }
        }
        stack.setItemMeta(meta);
    }

    private static String getBaseName(String id) {
        return switch (id) {
            case "shadowBlade" -> "§8Теневой клинок";
            case "thunderHammer" -> "§bМолот грома";
            case "vampireDagger" -> "§cVampire Dagger";
            case "infernoSword" -> "§6Меч инферно";
            case "frostAxe" -> "§9Ледяной топор";
            case "venomBow" -> "§2Ядовитый Лук";
            case "reaperScythe" -> "§5Коса Жнеца";
            default -> null; // We only support upgrading these weapons for now
        };
    }

    @SuppressWarnings("deprecation")
    public static ItemStack build(String id, Material material, String name, List<String> lore, int customModelData, boolean glowing) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;

        meta.displayName(LegacyComponentSerializer.legacySection().deserialize(name));
        if (lore != null) {
            meta.lore(lore.stream().map(l -> LegacyComponentSerializer.legacySection().deserialize(l)).collect(Collectors.toList()));
        }

        if (customModelData > 0) meta.setCustomModelData(customModelData);
        if (ITEM_ID == null) ITEM_ID = new NamespacedKey("astrasmp", "custom_id");
        meta.getPersistentDataContainer().set(ITEM_ID, PersistentDataType.STRING, id);

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
        stack.setItemMeta(meta);

        if (glowing) {
            Enchantment unbreaking = org.bukkit.Registry.ENCHANTMENT.get(NamespacedKey.minecraft("unbreaking"));
            if (unbreaking != null) stack.addUnsafeEnchantment(unbreaking, 1);

            ItemMeta m = stack.getItemMeta();
            m.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            stack.setItemMeta(m);
        }
        return stack;
    }

    @SuppressWarnings("deprecation")
    private static ItemStack buildTool(String id, Material material, String name, List<String> lore, int customModelData) {
        ItemStack stack = build(id, material, name, lore, customModelData, false);
        Enchantment efficiency = org.bukkit.Registry.ENCHANTMENT.get(NamespacedKey.minecraft("efficiency"));
        if (efficiency != null) stack.addUnsafeEnchantment(efficiency, 3);
        return stack;
    }

    public static ItemStack buildArmor(String id, Material material, String name, List<String> lore, int customModelData, Color leatherColor, boolean knockbackResist) {
        ItemStack stack = build(id, material, name, lore, customModelData, false);
        ItemMeta meta = stack.getItemMeta();

        if (meta instanceof LeatherArmorMeta leatherMeta && leatherColor != null) {
            leatherMeta.setColor(leatherColor);
            stack.setItemMeta(leatherMeta);
        }

        if (knockbackResist) {
            AttributeModifier modifier = new AttributeModifier(new NamespacedKey("astrasmp", id + "_kb"), 0.5, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ARMOR);
            meta.addAttributeModifier(Attribute.KNOCKBACK_RESISTANCE, modifier);
            stack.setItemMeta(meta);
        }

        return stack;
    }

    // --- БИЛДЕР ДЛЯ ME СИСТЕМЫ ---
    private static ItemStack buildME(String meType, Material material, String name, int customModelData) {
        ItemStack stack = build("me_" + meType, material, name, List.of("§7Компонент ME-сети"), customModelData, true);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            // Тег, который ищет MENetworkService
            meta.getPersistentDataContainer().set(new NamespacedKey("astrasmp", "me_component"), PersistentDataType.STRING, meType);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    // ==========================================
    // КОМПОНЕНТЫ ME-СЕТИ
    // ==========================================
    public static ItemStack meController() { return buildME("controller", Material.LODESTONE, "§bME Контроллер", 8001); }
    public static ItemStack meDrive() { return buildME("drive", Material.SMITHING_TABLE, "§bME Дисковод", 8002); }
    public static ItemStack meTerminal() { return buildME("terminal", Material.OBSERVER, "§bME Терминал", 8003); }
    public static ItemStack meCell4k() { return buildME("cell_4k", Material.FLINT, "§eME Ячейка хранения [4K]", 8004); }
    public static ItemStack meCell16k() { return buildME("cell_16k", Material.IRON_INGOT, "§6ME Ячейка хранения [16K]", 8005); }
    public static ItemStack meCell64k() { return buildME("cell_64k", Material.GOLD_INGOT, "§cME Ячейка хранения [64K]", 8006); }

    // ==========================================
    // СЕТЫ БРОНИ (PvP и PvE)
    // ==========================================

    // 1. СЕТ НАЕМНИКА
    public static ItemStack mercenaryHelmet() { ItemStack s = buildArmor("mercenary_helmet", Material.LEATHER_HELMET, "§8Капюшон Наемника", List.of("§7Сет Наемника (1/4)", "§8Бонус: §cУдар в спину (+25% урона)"), 7001, Color.BLACK, false); s.addUnsafeEnchantment(Enchantment.PROTECTION, 3); return s; }
    public static ItemStack mercenaryChestplate() { ItemStack s = buildArmor("mercenary_chestplate", Material.LEATHER_CHESTPLATE, "§8Куртка Наемника", List.of("§7Сет Наемника (2/4)", "§8Бонус: §cУдар в спину (+25% урона)"), 7002, Color.BLACK, false); s.addUnsafeEnchantment(Enchantment.PROTECTION, 3); return s; }
    public static ItemStack mercenaryLeggings() { ItemStack s = buildArmor("mercenary_leggings", Material.LEATHER_LEGGINGS, "§8Штаны Наемника", List.of("§7Сет Наемника (3/4)", "§8Бонус: §cУдар в спину (+25% урона)"), 7003, Color.BLACK, false); s.addUnsafeEnchantment(Enchantment.PROTECTION, 3); return s; }
    public static ItemStack mercenaryBoots() { ItemStack s = buildArmor("mercenary_boots", Material.LEATHER_BOOTS, "§8Ботинки Наемника", List.of("§7Сет Наемника (4/4)", "§8Бонус: §cУдар в спину (+25% урона)"), 7004, Color.BLACK, false); s.addUnsafeEnchantment(Enchantment.PROTECTION, 3); return s; }

    // 2. СЕТ БЕРСЕРКА
    public static ItemStack berserkerHelmet() { ItemStack s = buildArmor("berserker_helmet", Material.IRON_HELMET, "§cШлем Берсерка", List.of("§7Сет Берсерка (1/4)", "§8Бонус: §cСила при низком здоровье"), 7011, null, false); s.addUnsafeEnchantment(Enchantment.PROTECTION, 4); return s; }
    public static ItemStack berserkerChestplate() { ItemStack s = buildArmor("berserker_chestplate", Material.IRON_CHESTPLATE, "§cНагрудник Берсерка", List.of("§7Сет Берсерка (2/4)", "§8Бонус: §cСила при низком здоровье"), 7012, null, false); s.addUnsafeEnchantment(Enchantment.PROTECTION, 4); return s; }
    public static ItemStack berserkerLeggings() { ItemStack s = buildArmor("berserker_leggings", Material.IRON_LEGGINGS, "§cПоножи Берсерка", List.of("§7Сет Берсерка (3/4)", "§8Бонус: §cСила при низком здоровье"), 7013, null, false); s.addUnsafeEnchantment(Enchantment.PROTECTION, 4); return s; }
    public static ItemStack berserkerBoots() { ItemStack s = buildArmor("berserker_boots", Material.IRON_BOOTS, "§cБотинки Берсерка", List.of("§7Сет Берсерка (4/4)", "§8Бонус: §cСила при низком здоровье"), 7014, null, false); s.addUnsafeEnchantment(Enchantment.PROTECTION, 4); return s; }

    // 3. СЕТ ИНКВИЗИТОРА
    public static ItemStack inquisitorHelmet() { ItemStack s = buildArmor("inquisitor_helmet", Material.IRON_HELMET, "§eШлем Инквизитора", List.of("§7Сет Инквизитора (1/4)", "§8Бонус: §eИммунитет к дебаффам оружия"), 7021, null, false); s.addUnsafeEnchantment(Enchantment.PROJECTILE_PROTECTION, 4); return s; }
    public static ItemStack inquisitorChestplate() { ItemStack s = buildArmor("inquisitor_chestplate", Material.IRON_CHESTPLATE, "§eНагрудник Инквизитора", List.of("§7Сет Инквизитора (2/4)", "§8Бонус: §eИммунитет к дебаффам оружия"), 7022, null, false); s.addUnsafeEnchantment(Enchantment.PROJECTILE_PROTECTION, 4); return s; }
    public static ItemStack inquisitorLeggings() { ItemStack s = buildArmor("inquisitor_leggings", Material.IRON_LEGGINGS, "§eПоножи Инквизитора", List.of("§7Сет Инквизитора (3/4)", "§8Бонус: §eИммунитет к дебаффам оружия"), 7023, null, false); s.addUnsafeEnchantment(Enchantment.PROJECTILE_PROTECTION, 4); return s; }
    public static ItemStack inquisitorBoots() { ItemStack s = buildArmor("inquisitor_boots", Material.IRON_BOOTS, "§eСапоги Инквизитора", List.of("§7Сет Инквизитора (4/4)", "§8Бонус: §eИммунитет к дебаффам оружия"), 7024, null, false); s.addUnsafeEnchantment(Enchantment.PROJECTILE_PROTECTION, 4); return s; }

    // 4. СЕТ ДЖАГГЕРНАУТА
    public static ItemStack juggernautHelmet() { ItemStack s = buildArmor("juggernaut_helmet", Material.NETHERITE_HELMET, "§8Шлем Джаггернаута", List.of("§7Сет Джаггернаута (1/4)", "§8Бонус: §7Анти-отбрасывание и -15% урона"), 7031, null, true); s.addUnsafeEnchantment(Enchantment.PROTECTION, 5); return s; }
    public static ItemStack juggernautChestplate() { ItemStack s = buildArmor("juggernaut_chestplate", Material.NETHERITE_CHESTPLATE, "§8Нагрудник Джаггернаута", List.of("§7Сет Джаггернаута (2/4)", "§8Бонус: §7Анти-отбрасывание и -15% урона"), 7032, null, true); s.addUnsafeEnchantment(Enchantment.PROTECTION, 5); return s; }
    public static ItemStack juggernautLeggings() { ItemStack s = buildArmor("juggernaut_leggings", Material.NETHERITE_LEGGINGS, "§8Поножи Джаггернаута", List.of("§7Сет Джаггернаута (3/4)", "§8Бонус: §7Анти-отбрасывание и -15% урона"), 7033, null, true); s.addUnsafeEnchantment(Enchantment.PROTECTION, 5); return s; }
    public static ItemStack juggernautBoots() { ItemStack s = buildArmor("juggernaut_boots", Material.NETHERITE_BOOTS, "§8Сапоги Джаггернаута", List.of("§7Сет Джаггернаута (4/4)", "§8Бонус: §7Анти-отбрасывание и -15% урона"), 7034, null, true); s.addUnsafeEnchantment(Enchantment.PROTECTION, 5); return s; }

    // 5. ШАХТЕРСКИЙ ЭКЗОСКЕЛЕТ
    public static ItemStack minerHelmet() { ItemStack s = buildArmor("miner_helmet", Material.DIAMOND_HELMET, "§bЭкзоскелет: Шлем", List.of("§7Шахтерский Сет (1/4)", "§8Бонус: §bСпешка II и Ночное зрение"), 7041, null, false); s.addUnsafeEnchantment(Enchantment.PROTECTION, 4); return s; }
    public static ItemStack minerChestplate() { ItemStack s = buildArmor("miner_chestplate", Material.DIAMOND_CHESTPLATE, "§bЭкзоскелет: Корпус", List.of("§7Шахтерский Сет (2/4)", "§8Бонус: §bСпешка II и Ночное зрение"), 7042, null, false); s.addUnsafeEnchantment(Enchantment.PROTECTION, 4); return s; }
    public static ItemStack minerLeggings() { ItemStack s = buildArmor("miner_leggings", Material.DIAMOND_LEGGINGS, "§bЭкзоскелет: Поножи", List.of("§7Шахтерский Сет (3/4)", "§8Бонус: §bСпешка II и Ночное зрение"), 7043, null, false); s.addUnsafeEnchantment(Enchantment.PROTECTION, 4); return s; }
    public static ItemStack minerBoots() { ItemStack s = buildArmor("miner_boots", Material.DIAMOND_BOOTS, "§bЭкзоскелет: Ботинки", List.of("§7Шахтерский Сет (4/4)", "§8Бонус: §bСпешка II и Ночное зрение"), 7044, null, false); s.addUnsafeEnchantment(Enchantment.PROTECTION, 4); return s; }

    // 6. СЕТ ОХОТНИКА КРОВАВОЙ НОЧИ
    public static ItemStack bloodHunterHelmet() { ItemStack s = buildArmor("bloodhunter_helmet", Material.DIAMOND_HELMET, "§4Шлем Кровавой Ночи", List.of("§7Сет Охотника (1/4)", "§8Бонус: §cЗащита от мобов Кровавой Ночи"), 7051, null, false); s.addUnsafeEnchantment(Enchantment.PROTECTION, 5); return s; }
    public static ItemStack bloodHunterChestplate() { ItemStack s = buildArmor("bloodhunter_chestplate", Material.DIAMOND_CHESTPLATE, "§4Нагрудник Кровавой Ночи", List.of("§7Сет Охотника (2/4)", "§8Бонус: §cЗащита от мобов Кровавой Ночи"), 7052, null, false); s.addUnsafeEnchantment(Enchantment.PROTECTION, 5); return s; }
    public static ItemStack bloodHunterLeggings() { ItemStack s = buildArmor("bloodhunter_leggings", Material.DIAMOND_LEGGINGS, "§4Поножи Кровавой Ночи", List.of("§7Сет Охотника (3/4)", "§8Бонус: §cЗащита от мобов Кровавой Ночи"), 7053, null, false); s.addUnsafeEnchantment(Enchantment.PROTECTION, 5); return s; }
    public static ItemStack bloodHunterBoots() { ItemStack s = buildArmor("bloodhunter_boots", Material.DIAMOND_BOOTS, "§4Сапоги Кровавой Ночи", List.of("§7Сет Охотника (4/4)", "§8Бонус: §cЗащита от мобов Кровавой Ночи"), 7054, null, false); s.addUnsafeEnchantment(Enchantment.PROTECTION, 5); return s; }

    // ==========================================
    // СУЩЕСТВУЮЩИЕ ПРЕДМЕТЫ
    // ==========================================

    // --- ДОБЫЧА (Алмазные) ---
    public static ItemStack mining3x3() { return buildTool("mining3x3", Material.DIAMOND_PICKAXE, "§bКрушитель 3x3", List.of("§7Ломает блоки в области 3x3."), 1001); }
    public static ItemStack mining5x5() { return buildTool("mining5x5", Material.DIAMOND_PICKAXE, "§5Шахтер 5x5", List.of("§7Ломает блоки в области 5x5."), 1002); }
    public static ItemStack veinMiner() { return buildTool("veinMiner", Material.DIAMOND_PICKAXE, "§bЖильный шахтер", List.of("§7Добывает всю жилу руды."), 1003); }
    public static ItemStack autoSmelt() { return buildTool("autoSmelt", Material.DIAMOND_PICKAXE, "§6Авто-плавка", List.of("§7Мгновенно плавит руду."), 1004); }
    public static ItemStack magnet() { return buildTool("magnet", Material.DIAMOND_PICKAXE, "§eМагнит", List.of("§7Притягивает ресурсы."), 1005); }

    // --- ДОБЫЧА (Незеритовые) ---
    public static ItemStack mining3x3Netherite() { return buildTool("mining3x3", Material.NETHERITE_PICKAXE, "§dКрушитель 3x3+", List.of("§7Ломает блоки в области 3x3."), 1001); }
    public static ItemStack mining5x5Netherite() { return buildTool("mining5x5", Material.NETHERITE_PICKAXE, "§5Шахтер 5x5+", List.of("§7Ломает блоки в области 5x5."), 1002); }
    public static ItemStack veinMinerNetherite() { return buildTool("veinMiner", Material.NETHERITE_PICKAXE, "§dЖильный шахтер+", List.of("§7Добывает всю жилу руды."), 1003); }
    public static ItemStack autoSmeltNetherite() { return buildTool("autoSmelt", Material.NETHERITE_PICKAXE, "§6Авто-плавка+", List.of("§7Мгновенно плавит руду."), 1004); }
    public static ItemStack magnetNetherite() { return buildTool("magnet", Material.NETHERITE_PICKAXE, "§eМагнит+", List.of("§7Притягивает ресурсы."), 1005); }

    // --- ОРУЖИЕ ---
    public static ItemStack shadowBlade() { 
        ItemStack stack = build("shadowBlade", Material.NETHERITE_SWORD, "§8Теневой клинок", List.of("§7Слепота врагу, скорость себе."), 2001, false);
        stack.addUnsafeEnchantment(Enchantment.SHARPNESS, 6);
        return stack; 
    }
    public static ItemStack thunderHammer() { 
        ItemStack stack = build("thunderHammer", Material.NETHERITE_AXE, "§bМолот грома", List.of("§7Призывает молнию при ударе."), 2002, false);
        stack.addUnsafeEnchantment(Enchantment.SHARPNESS, 6);
        stack.addUnsafeEnchantment(Enchantment.SMITE, 5);
        return stack; 
    }
    public static ItemStack vampireDagger() { 
        ItemStack stack = build("vampireDagger", Material.DIAMOND_SWORD, "§cVampire Dagger", List.of("§7Крадет здоровье врага."), 2003, false);
        stack.addUnsafeEnchantment(Enchantment.SHARPNESS, 5);
        return stack; 
    }
    public static ItemStack infernoSword() { 
        ItemStack stack = build("infernoSword", Material.NETHERITE_SWORD, "§6Меч инферно", List.of("§7Поджигает врагов в радиусе 4 блоков."), 2004, false);
        stack.addUnsafeEnchantment(Enchantment.SHARPNESS, 5);
        stack.addUnsafeEnchantment(Enchantment.FIRE_ASPECT, 3);
        return stack; 
    }
    public static ItemStack frostAxe() { 
        ItemStack stack = build("frostAxe", Material.DIAMOND_AXE, "§9Ледяной топор", List.of("§7Замедляет цель."), 2005, false);
        stack.addUnsafeEnchantment(Enchantment.SHARPNESS, 5);
        return stack; 
    }
    public static ItemStack venomBow() { 
        ItemStack stack = build("venomBow", Material.BOW, "§2Ядовитый Лук", List.of("§7Отравляет цель."), 2006, true);
        stack.addUnsafeEnchantment(Enchantment.POWER, 5);
        stack.addUnsafeEnchantment(Enchantment.PUNCH, 2);
        return stack; 
    }
    public static ItemStack reaperScythe() { 
        ItemStack stack = build("reaperScythe", Material.NETHERITE_HOE, "§5Коса Жнеца", List.of("§7Массовый урон по площади."), 2007, true);
        stack.addUnsafeEnchantment(Enchantment.SHARPNESS, 7);
        return stack; 
    }

    // --- ТОТЕМЫ ---
    public static ItemStack totemSpeed() { return build("totemSpeed", Material.TOTEM_OF_UNDYING, "§bТотем скорости", List.of("§7Пассивно дает Скорость во второй руке."), 3001, true); }
    public static ItemStack totemShield() { return build("totemShield", Material.TOTEM_OF_UNDYING, "§7Тотем щита", List.of("§7Пассивно дает Сопротивление во второй руке."), 3002, true); }
    public static ItemStack totemLightning() { return build("totemLightning", Material.TOTEM_OF_UNDYING, "§eТотем молнии", List.of("§7Бьет молнией вокруг."), 3003, true); }
    public static ItemStack totemExplosion() { return build("totemExplosion", Material.TOTEM_OF_UNDYING, "§cТотем взрыва", List.of("§7Создает взрыв."), 3004, true); }
    public static ItemStack totemTeleport() { return build("totemTeleport", Material.TOTEM_OF_UNDYING, "§dТотем телепорта", List.of("§7Прыжок вперед."), 3005, true); }

    // --- РЕДКОСТИ ---
    public static ItemStack trophy(String id, Material mat, String name, String tier) { return build("trophy_"+id, mat, name, List.of("§7Тир: "+tier), 4000, true); }
    public static ItemStack relic(String id, Material mat, String name, String desc) { return build("relic_"+id, mat, name, List.of("§5Реликвия", "§7"+desc), 5000, true); }
    public static ItemStack artifact(String id, Material mat, String name, String desc) { return build("artifact_"+id, mat, name, List.of("§dАртефакт", "§7"+desc), 6000, true); }

    // --- РИТУАЛЫ И САТАНИЗМ ---
    public static ItemStack sacrificialDagger() {
        ItemStack stack = build("sacrificialDagger", Material.NETHERITE_SWORD, "§4Ритуальный Кинжал", List.of("§7Вытягивает кровь из жертв."), 9101, false);
        stack.addUnsafeEnchantment(Enchantment.SHARPNESS, 5);
        return stack;
    }
    public static ItemStack bloodDrop() {
        return build("bloodDrop", Material.REDSTONE, "§cКапля Крови", List.of("§7Базовый ингредиент ритуалов."), 0, true);
    }

    public static ItemStack seedOfAbyss() {
        return build("seedOfAbyss", Material.HEART_OF_THE_SEA, "§5Семя Бездны", List.of(
                "§7Ультимативный артефакт культистов.",
                "§7ПКМ открывает доступ к личному",
                "§7Карманному Измерению (/prunus)."
        ), 0, true);
    }

    public static ItemStack astralCrystal() {
        return build("astralCrystal", Material.AMETHYST_SHARD, "§dАстральный Кристалл", List.of(
                "§7Отделяет вашу душу от тела.",
                "§7Вы становитесь призраком на 2 минуты.",
                "§cОсторожно: ваше тело уязвимо."
        ), 0, true);
    }
    public static ItemStack demonSoul() { return build("demonSoul", Material.GHAST_TEAR, "§5Душа Демона", List.of("§7Осколок души могущественного существа."), 9103, true); }
    public static ItemStack pactOfBlood() { return build("pactOfBlood", Material.PAPER, "§4Контракт Крови", List.of("§cОтнимает 4 макс. здоровья.", "§aДает Вампиризм и Иммунитет к огню.", "§4Пути назад не будет..."), 9104, true); }
    public static ItemStack pactOfVoid() { return build("pactOfVoid", Material.PAPER, "§5Контракт Бездны", List.of("§cОтнимает 4 макс. здоровья.", "§aИммунитет к падению, но слабость к воде.", "§4Пути назад не будет..."), 9114, true); }
    public static ItemStack pactOfShadow() { return build("pactOfShadow", Material.PAPER, "§8Контракт Теней", List.of("§cОтнимает 4 макс. здоровья.", "§aНевидимость в темноте, слабость на свету.", "§4Пути назад не будет..."), 9115, true); }
    public static ItemStack bloodCauldron() { return build("bloodCauldron", Material.CAULDRON, "§4Кровавый Котел", List.of("§7Поставьте рядом с алтарем для сбора крови.", "§7Накапливает кровь убитых рядом существ."), 9105, false); }
    public static ItemStack ritualAltar() { return build("ritualAltar", Material.CRYING_OBSIDIAN, "§5Ядро Алтаря", List.of("§7Центр Ритуального Круга.", "§7Поставьте и окружите нужными блоками."), 9106, true); }
    public static ItemStack cleansingTotem() { return build("cleansingTotem", Material.TOTEM_OF_UNDYING, "§eТотем Очищения", List.of("§7ПКМ по Вратам Бездны,", "§7чтобы закрыть их навсегда."), 9107, true); }
    public static ItemStack bloodChalice() { return build("bloodChalice", Material.DRAGON_BREATH, "§4Карманная Чаша Крови", List.of("§7Автоматически собирает", "§7кровь убитых вами мобов.", "§7Шанс 15% получить Каплю Крови."), 9108, true); }

    public static ItemStack bloodVial(String targetName, String targetUuid) {
        ItemStack item = build("bloodVial", Material.POTION, "§4Флакон с кровью: §c" + targetName, List.of("§7Кровь игрока " + targetName, "§7Используется в темных ритуалах."), 9109, true);
        item.editMeta(meta -> {
            meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(AstraSMPPlugin.getInstance(), "blood_target_uuid"), org.bukkit.persistence.PersistentDataType.STRING, targetUuid);
            meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(AstraSMPPlugin.getInstance(), "blood_target_name"), org.bukkit.persistence.PersistentDataType.STRING, targetName);
            if (meta instanceof org.bukkit.inventory.meta.PotionMeta pmeta) {
                pmeta.setColor(org.bukkit.Color.RED);
            }
        });
        return item;
    }

    public static ItemStack voodooDoll(String targetName, String targetUuid) {
        ItemStack item = build("voodooDoll", Material.TOTEM_OF_UNDYING, "§5Кукла Вуду: §d" + targetName, List.of("§7Проклятая кукла игрока " + targetName, "§7ПКМ, чтобы наложить проклятие.", "§cПрочность: 3/3"), 9110, true);
        item.editMeta(meta -> {
            meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(AstraSMPPlugin.getInstance(), "voodoo_target_uuid"), org.bukkit.persistence.PersistentDataType.STRING, targetUuid);
            meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(AstraSMPPlugin.getInstance(), "voodoo_target_name"), org.bukkit.persistence.PersistentDataType.STRING, targetName);
            meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(AstraSMPPlugin.getInstance(), "voodoo_uses"), org.bukkit.persistence.PersistentDataType.INTEGER, 3);
        });
        return item;
    }

    public static ItemStack soulFragment(String targetName, String targetUuid) {
        ItemStack item = build("soulFragment", Material.GHAST_TEAR, "§bОсколок Души: §3" + targetName, List.of("§7Оторванная часть души " + targetName, "§7Содержит невероятную энергию."), 9111, true);
        item.editMeta(meta -> {
            meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(AstraSMPPlugin.getInstance(), "soul_target_uuid"), org.bukkit.persistence.PersistentDataType.STRING, targetUuid);
            meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(AstraSMPPlugin.getInstance(), "soul_target_name"), org.bukkit.persistence.PersistentDataType.STRING, targetName);
        });
        return item;
    }

    public static ItemStack madnessSphere(String targetName, String targetUuid) {
        ItemStack item = build("madnessSphere", Material.ENDER_PEARL, "§5Сфера Безумия: §d" + targetName, List.of("§7ПКМ, чтобы наслать", "§7ужас на игрока " + targetName), 9112, true);
        item.editMeta(meta -> {
            meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(AstraSMPPlugin.getInstance(), "madness_target_uuid"), org.bukkit.persistence.PersistentDataType.STRING, targetUuid);
            meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(AstraSMPPlugin.getInstance(), "madness_target_name"), org.bukkit.persistence.PersistentDataType.STRING, targetName);
        });
        return item;
    }

    public static ItemStack bloodVial() { return bloodVial("Неизвестный", "null"); }
    public static ItemStack voodooDoll() { return voodooDoll("Неизвестный", "null"); }
    public static ItemStack soulFragment() { return soulFragment("Неизвестный", "null"); }
    public static ItemStack madnessSphere() { return madnessSphere("Неизвестный", "null"); }
    
    public static ItemStack bloodStainedNote() {
        return build("bloodStainedNote", Material.PAPER, "§4Испачканная кровью записка", List.of(
                "§7Темная аура исходит от этой бумаги...",
                "§7ПКМ чтобы прочесть."
        ), 9113, true);
    }

    // Кланы
    public static ItemStack guildHeart() {
        ItemStack item = new ItemStack(Material.CRYING_OBSIDIAN);
        item.editMeta(meta -> {
            meta.displayName(Component.text(TextUtil.color("&d&lСердце Гильдии")));
            meta.lore(List.of(
                    Component.text(TextUtil.color("&7Поставьте этот блок на землю,")),
                    Component.text(TextUtil.color("&7чтобы заприватить территорию гильдии.")),
                    Component.text(""),
                    Component.text(TextUtil.color("&eНачальный радиус: 15 блоков"))
            ));
            meta.getPersistentDataContainer().set(
                    new org.bukkit.NamespacedKey("astrasmp", "custom_id"),
                    org.bukkit.persistence.PersistentDataType.STRING,
                    "guild_heart"
            );
        });
        return item;
    }

    public static ItemStack trampoline() {
        return build("trampoline", Material.SLIME_BLOCK, "§aУлучшенный Батут", List.of(
                "§7Отбивает вас высоко в воздух",
                "§7и поглощает урон от падения."
        ), 9001, true);
    }

    public static ItemStack soulOfNanda() {
        return build("soulOfNanda", Material.NETHER_STAR, "§cДуша Нанды", List.of(
                "§7Ударьте игрока этим предметом,",
                "§7чтобы изгнать его с сервера!"
        ), 9002, true);
    }

    public static ItemStack eventCompass() {
        return build("eventCompass", Material.COMPASS, "§bМагический Компас", List.of(
                "§7Указывает направление на активный ивент.",
                "§7Нажмите §aПКМ§7, чтобы узнать дистанцию."
        ), 9003, true);
    }

    public static List<ItemStack> showcase() {
        return List.of(
                mining3x3(), mining5x5(), veinMiner(), autoSmelt(), magnet(),
                mining3x3Netherite(), mining5x5Netherite(), veinMinerNetherite(), autoSmeltNetherite(), magnetNetherite(),
                shadowBlade(), thunderHammer(), vampireDagger(), infernoSword(), frostAxe(), venomBow(), reaperScythe(),
                totemSpeed(), totemShield(), totemLightning(), totemExplosion(), totemTeleport()
        );
    }

    public static List<ItemStack> getAllItems() {
        List<ItemStack> items = new ArrayList<>(showcase());
        items.addAll(List.of(
                meController(), meDrive(), meTerminal(), meCell4k(), meCell16k(), meCell64k(),
                mercenaryHelmet(), mercenaryChestplate(), mercenaryLeggings(), mercenaryBoots(),
                berserkerHelmet(), berserkerChestplate(), berserkerLeggings(), berserkerBoots(),
                inquisitorHelmet(), inquisitorChestplate(), inquisitorLeggings(), inquisitorBoots(),
                juggernautHelmet(), juggernautChestplate(), juggernautLeggings(), juggernautBoots(),
                minerHelmet(), minerChestplate(), minerLeggings(), minerBoots(),
                bloodHunterHelmet(), bloodHunterChestplate(), bloodHunterLeggings(), bloodHunterBoots(),
                trampoline(), soulOfNanda(), eventCompass(),
                sacrificialDagger(), bloodDrop(), demonSoul(), pactOfBlood(), pactOfVoid(), pactOfShadow(), bloodCauldron(), ritualAltar(), cleansingTotem(), bloodChalice(),
                bloodVial(), voodooDoll(), soulFragment(), madnessSphere(), bloodStainedNote()
        ));
        return items;
    }
}