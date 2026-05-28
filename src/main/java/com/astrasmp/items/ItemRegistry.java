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

    private ItemRegistry() {}

    public static void init(AstraSMPPlugin plugin) {
        ITEM_ID = new NamespacedKey("astrasmp", "custom_id");
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
    public static ItemStack mercenaryHelmet() { return buildArmor("mercenary_helmet", Material.LEATHER_HELMET, "§8Капюшон Наемника", List.of("§7Сет Наемника (1/4)", "§8Бонус: §cУдар в спину (+25% урона)"), 7001, Color.BLACK, false); }
    public static ItemStack mercenaryChestplate() { return buildArmor("mercenary_chestplate", Material.LEATHER_CHESTPLATE, "§8Куртка Наемника", List.of("§7Сет Наемника (2/4)", "§8Бонус: §cУдар в спину (+25% урона)"), 7002, Color.BLACK, false); }
    public static ItemStack mercenaryLeggings() { return buildArmor("mercenary_leggings", Material.LEATHER_LEGGINGS, "§8Штаны Наемника", List.of("§7Сет Наемника (3/4)", "§8Бонус: §cУдар в спину (+25% урона)"), 7003, Color.BLACK, false); }
    public static ItemStack mercenaryBoots() { return buildArmor("mercenary_boots", Material.LEATHER_BOOTS, "§8Ботинки Наемника", List.of("§7Сет Наемника (4/4)", "§8Бонус: §cУдар в спину (+25% урона)"), 7004, Color.BLACK, false); }

    // 2. СЕТ БЕРСЕРКА
    public static ItemStack berserkerHelmet() { return buildArmor("berserker_helmet", Material.IRON_HELMET, "§cШлем Берсерка", List.of("§7Сет Берсерка (1/4)", "§8Бонус: §cСила при низком здоровье"), 7011, null, false); }
    public static ItemStack berserkerChestplate() { return buildArmor("berserker_chestplate", Material.IRON_CHESTPLATE, "§cНагрудник Берсерка", List.of("§7Сет Берсерка (2/4)", "§8Бонус: §cСила при низком здоровье"), 7012, null, false); }
    public static ItemStack berserkerLeggings() { return buildArmor("berserker_leggings", Material.IRON_LEGGINGS, "§cПоножи Берсерка", List.of("§7Сет Берсерка (3/4)", "§8Бонус: §cСила при низком здоровье"), 7013, null, false); }
    public static ItemStack berserkerBoots() { return buildArmor("berserker_boots", Material.IRON_BOOTS, "§cБотинки Берсерка", List.of("§7Сет Берсерка (4/4)", "§8Бонус: §cСила при низком здоровье"), 7014, null, false); }

    // 3. СЕТ ИНКВИЗИТОРА
    public static ItemStack inquisitorHelmet() { return buildArmor("inquisitor_helmet", Material.IRON_HELMET, "§eШлем Инквизитора", List.of("§7Сет Инквизитора (1/4)", "§8Бонус: §eИммунитет к дебаффам оружия"), 7021, null, false); }
    public static ItemStack inquisitorChestplate() { return buildArmor("inquisitor_chestplate", Material.IRON_CHESTPLATE, "§eНагрудник Инквизитора", List.of("§7Сет Инквизитора (2/4)", "§8Бонус: §eИммунитет к дебаффам оружия"), 7022, null, false); }
    public static ItemStack inquisitorLeggings() { return buildArmor("inquisitor_leggings", Material.IRON_LEGGINGS, "§eПоножи Инквизитора", List.of("§7Сет Инквизитора (3/4)", "§8Бонус: §eИммунитет к дебаффам оружия"), 7023, null, false); }
    public static ItemStack inquisitorBoots() { return buildArmor("inquisitor_boots", Material.IRON_BOOTS, "§eСапоги Инквизитора", List.of("§7Сет Инквизитора (4/4)", "§8Бонус: §eИммунитет к дебаффам оружия"), 7024, null, false); }

    // 4. СЕТ ДЖАГГЕРНАУТА
    public static ItemStack juggernautHelmet() { return buildArmor("juggernaut_helmet", Material.NETHERITE_HELMET, "§8Шлем Джаггернаута", List.of("§7Сет Джаггернаута (1/4)", "§8Бонус: §7Анти-отбрасывание и -15% урона"), 7031, null, true); }
    public static ItemStack juggernautChestplate() { return buildArmor("juggernaut_chestplate", Material.NETHERITE_CHESTPLATE, "§8Нагрудник Джаггернаута", List.of("§7Сет Джаггернаута (2/4)", "§8Бонус: §7Анти-отбрасывание и -15% урона"), 7032, null, true); }
    public static ItemStack juggernautLeggings() { return buildArmor("juggernaut_leggings", Material.NETHERITE_LEGGINGS, "§8Поножи Джаггернаута", List.of("§7Сет Джаггернаута (3/4)", "§8Бонус: §7Анти-отбрасывание и -15% урона"), 7033, null, true); }
    public static ItemStack juggernautBoots() { return buildArmor("juggernaut_boots", Material.NETHERITE_BOOTS, "§8Сапоги Джаггернаута", List.of("§7Сет Джаггернаута (4/4)", "§8Бонус: §7Анти-отбрасывание и -15% урона"), 7034, null, true); }

    // 5. ШАХТЕРСКИЙ ЭКЗОСКЕЛЕТ
    public static ItemStack minerHelmet() { return buildArmor("miner_helmet", Material.DIAMOND_HELMET, "§bЭкзоскелет: Шлем", List.of("§7Шахтерский Сет (1/4)", "§8Бонус: §bСпешка II и Ночное зрение"), 7041, null, false); }
    public static ItemStack minerChestplate() { return buildArmor("miner_chestplate", Material.DIAMOND_CHESTPLATE, "§bЭкзоскелет: Корпус", List.of("§7Шахтерский Сет (2/4)", "§8Бонус: §bСпешка II и Ночное зрение"), 7042, null, false); }
    public static ItemStack minerLeggings() { return buildArmor("miner_leggings", Material.DIAMOND_LEGGINGS, "§bЭкзоскелет: Поножи", List.of("§7Шахтерский Сет (3/4)", "§8Бонус: §bСпешка II и Ночное зрение"), 7043, null, false); }
    public static ItemStack minerBoots() { return buildArmor("miner_boots", Material.DIAMOND_BOOTS, "§bЭкзоскелет: Ботинки", List.of("§7Шахтерский Сет (4/4)", "§8Бонус: §bСпешка II и Ночное зрение"), 7044, null, false); }

    // 6. СЕТ ОХОТНИКА КРОВАВОЙ НОЧИ
    public static ItemStack bloodHunterHelmet() { return buildArmor("bloodhunter_helmet", Material.DIAMOND_HELMET, "§4Шлем Кровавой Ночи", List.of("§7Сет Охотника (1/4)", "§8Бонус: §cЗащита от мобов Кровавой Ночи"), 7051, null, false); }
    public static ItemStack bloodHunterChestplate() { return buildArmor("bloodhunter_chestplate", Material.DIAMOND_CHESTPLATE, "§4Нагрудник Кровавой Ночи", List.of("§7Сет Охотника (2/4)", "§8Бонус: §cЗащита от мобов Кровавой Ночи"), 7052, null, false); }
    public static ItemStack bloodHunterLeggings() { return buildArmor("bloodhunter_leggings", Material.DIAMOND_LEGGINGS, "§4Поножи Кровавой Ночи", List.of("§7Сет Охотника (3/4)", "§8Бонус: §cЗащита от мобов Кровавой Ночи"), 7053, null, false); }
    public static ItemStack bloodHunterBoots() { return buildArmor("bloodhunter_boots", Material.DIAMOND_BOOTS, "§4Сапоги Кровавой Ночи", List.of("§7Сет Охотника (4/4)", "§8Бонус: §cЗащита от мобов Кровавой Ночи"), 7054, null, false); }

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
    public static ItemStack shadowBlade() { return build("shadowBlade", Material.NETHERITE_SWORD, "§8Теневой клинок", List.of("§7Слепота врагу, скорость себе."), 2001, false); }
    public static ItemStack thunderHammer() { return build("thunderHammer", Material.NETHERITE_AXE, "§bМолот грома", List.of("§7Призывает молнию при ударе."), 2002, false); }
    public static ItemStack vampireDagger() { return build("vampireDagger", Material.DIAMOND_SWORD, "§cVampire Dagger", List.of("§7Крадет здоровье врага."), 2003, false); }
    public static ItemStack infernoSword() { return build("infernoSword", Material.NETHERITE_SWORD, "§6Меч инферно", List.of("§7Поджигает врагов в радиусе 4 блоков."), 2004, false); }
    public static ItemStack frostAxe() { return build("frostAxe", Material.DIAMOND_AXE, "§9Ледяной топор", List.of("§7Замедляет цель."), 2005, false); }

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

    public static List<ItemStack> showcase() {
        return List.of(
                mining3x3(), mining5x5(), veinMiner(), autoSmelt(), magnet(),
                mining3x3Netherite(), mining5x5Netherite(), veinMinerNetherite(), autoSmeltNetherite(), magnetNetherite(),
                shadowBlade(), thunderHammer(), vampireDagger(), infernoSword(), frostAxe(),
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
                bloodHunterHelmet(), bloodHunterChestplate(), bloodHunterLeggings(), bloodHunterBoots()
        ));
        return items;
    }
}