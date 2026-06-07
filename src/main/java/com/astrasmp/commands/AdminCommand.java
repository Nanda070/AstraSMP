package com.astrasmp.commands;

import com.astrasmp.items.ItemRegistry;
import com.astrasmp.model.PlayerProfile;
import com.astrasmp.service.EventService;
import com.astrasmp.service.ServiceManager;
import com.astrasmp.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class AdminCommand implements org.bukkit.command.TabExecutor {
    private final ServiceManager services;

    public AdminCommand(ServiceManager services) {
        this.services = services;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            return noPerm(sender);
        }

        if (args.length == 0) {
            if (sender instanceof Player player) {
                services.gui().openAdmin(player);
            } else {
                TextUtil.send(sender, "&cЭта команда только для игроков. Используйте подкоманды в консоли.");
            }
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                services.plugin().reloadConfig();
                TextUtil.send(sender, "&aКонфигурация плагина успешно перезагружена.");
            }

            // Управление монетами
            case "setcoins" -> {
                if (args.length < 3) return usage(sender, "/admin setcoins <игрок> <количество>");
                try {
                    OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                    long amount = Long.parseLong(args[2]);
                    services.economy().setBalance(target.getUniqueId(), target.getName() == null ? args[1] : target.getName(), amount);
                    TextUtil.send(sender, "&aБаланс игрока &f" + args[1] + " &aизменен на &f" + amount + " &aмонет.");
                } catch (NumberFormatException e) {
                    TextUtil.send(sender, "&cОшибка: количество должно быть числом.");
                }
            }

            // Управление Event Points
            case "setevent" -> {
                if (args.length < 3) return usage(sender, "/admin setevent <игрок> <количество>");
                try {
                    OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                    int amount = Integer.parseInt(args[2]);
                    PlayerProfile profile = services.economy().profile(target.getUniqueId(), target.getName() == null ? args[1] : target.getName());

                    if (profile != null) {
                        profile.setEventPoints(amount);
                        services.store().requestSave();
                        TextUtil.send(sender, "&dEvent Points &aигрока &f" + args[1] + " &aизменены на &f" + amount + "&a.");
                    }
                } catch (NumberFormatException e) {
                    TextUtil.send(sender, "&cОшибка: количество должно быть целым числом.");
                }
            }

            case "give" -> {
                if (args.length < 3) return usage(sender, "/admin give <игрок> <предмет> [количество]");
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    TextUtil.send(sender, "&cОшибка: игрок должен быть в сети.");
                    return true;
                }

                int amount = 1;
                if (args.length >= 4) {
                    try { amount = Integer.parseInt(args[3]); } catch (NumberFormatException ignored) {}
                }

                ItemStack stack = createItem(args[2], amount);
                if (stack == null) {
                    TextUtil.send(sender, "&cОшибка: неизвестный ID предмета.");
                    return true;
                }

                target.getInventory().addItem(stack);
                TextUtil.send(sender, "&aПредмет &f" + args[2] + " &aвыдан игроку &f" + target.getName());
            }

            // Выдача компонентов ME-сети
            case "me" -> {
                if (args.length < 3) return usage(sender, "/admin me <игрок> <компонент> [количество]");
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    TextUtil.send(sender, "&cОшибка: игрок должен быть в сети.");
                    return true;
                }

                int amount = 1;
                if (args.length >= 4) {
                    try { amount = Integer.parseInt(args[3]); } catch (NumberFormatException ignored) {}
                }

                ItemStack stack = getMeItem(args[2], amount);
                if (stack == null) {
                    TextUtil.send(sender, "&cОшибка: неизвестный компонент ME-сети. Используйте Tab.");
                    return true;
                }

                target.getInventory().addItem(stack);
                TextUtil.send(sender, "&b[ME] &aПредмет &f" + args[2] + " &aвыдан игроку &f" + target.getName());
            }

            case "npc" -> {
                if (!(sender instanceof Player p)) {
                    TextUtil.send(sender, "&cЭту команду может использовать только игрок.");
                    return true;
                }
                if (args.length < 2) {
                    TextUtil.send(p, "&cИспользование: /admin npc <1..8>");
                    TextUtil.send(p, "&75=Гайд, 6=Рулетка, 7=PvP-Рулетка, 8=ИвентШоп");
                    return true;
                }
                services.shops().spawnNpc(p, args[1]);
            }

            case "event" -> {
                if (args.length < 2) return usage(sender, "/admin event <тип|stop|bloodnight>");

                if (args[1].equalsIgnoreCase("stop")) {
                    if (services.events().active() == null) {
                        TextUtil.send(sender, "&cВ данный момент нет активных ивентов.");
                        return true;
                    }
                    services.events().finish();
                    TextUtil.send(sender, "&aАктивный ивент был принудительно завершен.");
                    return true;
                }

                if (args[1].equalsIgnoreCase("bloodnight")) {
                    services.events().toggleBloodNight();
                    return true;
                }

                try {
                    EventService.EventType type = EventService.EventType.valueOf(args[1].toUpperCase());
                    boolean started = services.events().start(type, sender instanceof Player p ? p : null);
                    if (started) {
                        TextUtil.send(sender, "&aИвент &f" + type.name() + " &aуспешно запущен на случайных координатах.");
                    } else {
                        TextUtil.send(sender, "&cНе удалось запустить ивент (возможно, другой ивент уже идет).");
                    }
                } catch (IllegalArgumentException ex) {
                    TextUtil.send(sender, "&cОшибка: неизвестный тип ивента.");
                }
            }

            case "awardblock" -> {
                if (!(sender instanceof Player p)) return true;
                if (args.length < 2) return usage(sender, "/admin awardblock <сумма>");
                try {
                    long amount = Long.parseLong(args[1]);
                    ItemStack block = new ItemStack(Material.GOLD_BLOCK);
                    var meta = block.getItemMeta();
                    meta.displayName(net.kyori.adventure.text.Component.text(TextUtil.color("&e&lБлок с наградой")));
                    meta.lore(java.util.List.of(net.kyori.adventure.text.Component.text(TextUtil.color("&7Наступите, чтобы получить: &a" + amount + " ❂"))));
                    meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(services.plugin(), "award_amount"), org.bukkit.persistence.PersistentDataType.LONG, amount);
                    block.setItemMeta(meta);
                    p.getInventory().addItem(block);
                    TextUtil.send(p, "&aСекретный блок выдан! Поставьте его, и первый, кто наступит, получит монеты.");
                } catch (NumberFormatException e) {
                    TextUtil.send(sender, "&cОшибка: сумма должна быть числом.");
                }
            }

            // Спавн ивента прямо на координатах админа
            case "spawnevent" -> {
                if (!(sender instanceof Player p)) {
                    TextUtil.send(sender, "&cЭту команду может использовать только игрок.");
                    return true;
                }
                if (args.length < 2) return usage(sender, "/admin spawnevent <meteor|airdrop|boss|chaos|treasure>");

                try {
                    EventService.EventType type = EventService.EventType.valueOf(args[1].toUpperCase());
                    boolean started = services.events().startAt(type, p.getLocation());
                    if (started) {
                        TextUtil.send(p, "&aИвент &f" + type.name() + " &aзапущен прямо на ваших координатах!");
                    } else {
                        TextUtil.send(p, "&cНе удалось запустить ивент (возможно, другой ивент уже идет).");
                    }
                } catch (IllegalArgumentException ex) {
                    TextUtil.send(p, "&cОшибка: неизвестный тип ивента.");
                }
            }

            // Установка точки спавна для компаса и команды /spawn
            case "setspawn" -> {
                if (!(sender instanceof Player p)) return true;
                Location l = p.getLocation();
                String locData = l.getWorld().getName() + ";" + l.getX() + ";" + l.getY() + ";" + l.getZ() + ";" + l.getYaw() + ";" + l.getPitch();
                services.plugin().getConfig().set("locations.spawn", locData);
                services.plugin().saveConfig();
                TextUtil.send(p, "&aТочка спавна успешно установлена!");
            }

            default -> TextUtil.send(sender, "&cНеизвестная подкоманда. Используйте: reload, setcoins, setevent, give, me, event, spawnevent, npc, awardblock, setspawn.");
        }
        return true;
    }

    private ItemStack getMeItem(String key, int amount) {
        ItemStack item = switch (key.toLowerCase()) {
            case "controller" -> ItemRegistry.meController();
            case "drive" -> ItemRegistry.meDrive();
            case "terminal" -> ItemRegistry.meTerminal();
            case "cell_4k" -> ItemRegistry.meCell4k();
            case "cell_16k" -> ItemRegistry.meCell16k();
            case "cell_64k" -> ItemRegistry.meCell64k();
            default -> null;
        };

        if (item != null) {
            item.setAmount(Math.max(1, amount));
        }
        return item;
    }

    private ItemStack createItem(String key, int amount) {
        ItemStack item = switch (key.toLowerCase()) {
            // Алмазные инструменты
            case "mine3", "3x3", "mine_3x3" -> ItemRegistry.mining3x3();
            case "mine5", "5x5", "mine_5x5" -> ItemRegistry.mining5x5();
            case "vein" -> ItemRegistry.veinMiner();
            case "smelt" -> ItemRegistry.autoSmelt();
            case "magnet" -> ItemRegistry.magnet();

            // Незеритовые инструменты
            case "mine3netherite" -> ItemRegistry.mining3x3Netherite();
            case "mine5netherite" -> ItemRegistry.mining5x5Netherite();
            case "veinnetherite" -> ItemRegistry.veinMinerNetherite();
            case "smeltnetherite" -> ItemRegistry.autoSmeltNetherite();
            case "magnetnetherite" -> ItemRegistry.magnetNetherite();

            // Оружие и тотемы
            case "shadow" -> ItemRegistry.shadowBlade();
            case "thunder" -> ItemRegistry.thunderHammer();
            case "vampire" -> ItemRegistry.vampireDagger();
            case "inferno" -> ItemRegistry.infernoSword();
            case "frost" -> ItemRegistry.frostAxe();
            case "speedtotem" -> ItemRegistry.totemSpeed();
            case "shieldtotem" -> ItemRegistry.totemShield();
            case "lightningtotem" -> ItemRegistry.totemLightning();
            case "explosivetotem" -> ItemRegistry.totemExplosion();
            case "teleporttotem" -> ItemRegistry.totemTeleport();

            // Броня Наемника
            case "mercenary_helmet" -> ItemRegistry.mercenaryHelmet();
            case "mercenary_chestplate" -> ItemRegistry.mercenaryChestplate();
            case "mercenary_leggings" -> ItemRegistry.mercenaryLeggings();
            case "mercenary_boots" -> ItemRegistry.mercenaryBoots();

            // Броня Берсерка
            case "berserker_helmet" -> ItemRegistry.berserkerHelmet();
            case "berserker_chestplate" -> ItemRegistry.berserkerChestplate();
            case "berserker_leggings" -> ItemRegistry.berserkerLeggings();
            case "berserker_boots" -> ItemRegistry.berserkerBoots();

            // Броня Инквизитора
            case "inquisitor_helmet" -> ItemRegistry.inquisitorHelmet();
            case "inquisitor_chestplate" -> ItemRegistry.inquisitorChestplate();
            case "inquisitor_leggings" -> ItemRegistry.inquisitorLeggings();
            case "inquisitor_boots" -> ItemRegistry.inquisitorBoots();

            // Броня Джаггернаута
            case "juggernaut_helmet" -> ItemRegistry.juggernautHelmet();
            case "juggernaut_chestplate" -> ItemRegistry.juggernautChestplate();
            case "juggernaut_leggings" -> ItemRegistry.juggernautLeggings();
            case "juggernaut_boots" -> ItemRegistry.juggernautBoots();

            // Экзоскелет Шахтера
            case "miner_helmet" -> ItemRegistry.minerHelmet();
            case "miner_chestplate" -> ItemRegistry.minerChestplate();
            case "miner_leggings" -> ItemRegistry.minerLeggings();
            case "miner_boots" -> ItemRegistry.minerBoots();

            // Броня Охотника Кровавой Ночи
            case "bloodhunter_helmet" -> ItemRegistry.bloodHunterHelmet();
            case "bloodhunter_chestplate" -> ItemRegistry.bloodHunterChestplate();
            case "bloodhunter_leggings" -> ItemRegistry.bloodHunterLeggings();
            case "bloodhunter_boots" -> ItemRegistry.bloodHunterBoots();

            // Редкости
            case "trophy_common" -> ItemRegistry.trophy("common", Material.PAPER, "§fОбычный трофей", "Обычный");
            case "trophy_legendary" -> ItemRegistry.trophy("legendary", Material.NETHER_STAR, "§6Легендарный трофей", "Легендарный");
            case "relic_time_core" -> ItemRegistry.relic("time_core", Material.CLOCK, "§dЯдро времени", "Замедляет время вокруг владельца.");
            case "relic_void_fragment" -> ItemRegistry.relic("void_fragment", Material.AMETHYST_SHARD, "§5Фрагмент пустоты", "Рывок через пустоту.");
            case "artifact_heart_of_world" -> ItemRegistry.artifact("heart_of_world", Material.HEART_OF_THE_SEA, "§bСердце мира", "Пассивная защита для носителя.");
            
            // Спец. предметы
            case "nanda" -> ItemRegistry.soulOfNanda();
            
            // Батут
            case "trampoline" -> ItemRegistry.trampoline();
            default -> null;
        };

        if (item != null) {
            item.setAmount(Math.max(1, amount));
        }
        return item;
    }

    private boolean noPerm(CommandSender sender) {
        TextUtil.send(sender, "&cУ вас недостаточно прав для выполнения этой команды.");
        return true;
    }

    private boolean usage(CommandSender sender, String text) {
        TextUtil.send(sender, "&eИспользование: " + text);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.isOp()) return List.of();

        if (args.length == 1) {
            return List.of("reload", "setcoins", "setevent", "spawnevent", "give", "me", "event", "npc", "awardblock", "setspawn").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("event")) {
                return List.of("meteor", "airdrop", "boss", "merchant", "treasure", "galleon", "loot", "stop", "bloodnight").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase())).toList();
            }
            if (args[0].equalsIgnoreCase("spawnevent")) {
                return List.of("meteor", "airdrop", "boss", "merchant", "treasure", "galleon", "loot").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase())).toList();
            }
            if (args[0].equalsIgnoreCase("npc")) {
                return List.of("1", "2", "3", "4", "5", "6", "7", "8").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase())).toList();
            }
            if (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("me") || args[0].equalsIgnoreCase("setcoins") || args[0].equalsIgnoreCase("setevent")) {
                return null; // Предлагает список ников онлайна
            }
        }

        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("give")) {
                return List.of(
                        "mine3", "mine5", "vein", "smelt", "magnet",
                        "mine3netherite", "mine5netherite", "veinnetherite", "smeltnetherite", "magnetnetherite",
                        "shadow", "thunder", "vampire", "inferno", "frost",
                        "speedtotem", "shieldtotem", "lightningtotem", "explosivetotem", "teleporttotem",
                        "mercenary_helmet", "mercenary_chestplate", "mercenary_leggings", "mercenary_boots",
                        "berserker_helmet", "berserker_chestplate", "berserker_leggings", "berserker_boots",
                        "inquisitor_helmet", "inquisitor_chestplate", "inquisitor_leggings", "inquisitor_boots",
                        "juggernaut_helmet", "juggernaut_chestplate", "juggernaut_leggings", "juggernaut_boots",
                        "miner_helmet", "miner_chestplate", "miner_leggings", "miner_boots",
                        "bloodhunter_helmet", "bloodhunter_chestplate", "bloodhunter_leggings", "bloodhunter_boots",
                        "trophy_common", "trophy_legendary", "relic_time_core", "relic_void_fragment", "artifact_heart_of_world",
                        "trampoline", "nanda"
                ).stream().filter(s -> s.startsWith(args[2].toLowerCase())).toList();
            }
            if (args[0].equalsIgnoreCase("me")) {
                return List.of("controller", "drive", "terminal", "cell_4k", "cell_16k", "cell_64k").stream()
                        .filter(s -> s.startsWith(args[2].toLowerCase())).toList();
            }
        }

        return List.of();
    }

}
