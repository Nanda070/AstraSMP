package com.astrasmp.listener;

import com.astrasmp.gui.GuiManager;
import com.astrasmp.model.Guild;
import com.astrasmp.service.ServiceManager;
import com.astrasmp.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MenuListener implements Listener {
    private final ServiceManager services;

    // --- STATE MACHINE ДЛЯ ВВОДА В ЧАТ ---
    public enum PromptType { RENAME_RANK, CREATE_RANK, CHANGE_PRIORITY, ECONOMY_SET, CORRUPTION_SET }
    
    public record ChatPrompt(UUID guildId, String rankId, PromptType type) {}
    
    private static final Map<UUID, ChatPrompt> activePrompts = new ConcurrentHashMap<>();

    public static void addPrompt(UUID uuid, ChatPrompt prompt) {
        activePrompts.put(uuid, prompt);
    }

    public static void removePrompt(UUID uuid) {
        activePrompts.remove(uuid);
    }

    public MenuListener(ServiceManager services) {
        this.services = services;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    @SuppressWarnings("deprecation")
    public void onChatInput(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        ChatPrompt prompt = activePrompts.remove(player.getUniqueId());
        
        if (prompt == null) return;
        event.setCancelled(true);
        
        String input = event.getMessage().trim();
        
        if (input.equalsIgnoreCase("отмена")) {
            TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_c66eaf", "&cДействие отменено."));
            return;
        }

        Bukkit.getScheduler().runTask(com.astrasmp.AstraSMPPlugin.getInstance(), () -> {
            if (prompt.type() == PromptType.ECONOMY_SET) {
                try {
                    long amount = Long.parseLong(input);
                    UUID targetUuid = UUID.fromString(prompt.rankId());
                    String targetName = Bukkit.getOfflinePlayer(targetUuid).getName();
                    services.economy().setBalance(targetUuid, targetName, amount);
                    TextUtil.send(player, "&aБаланс игрока &f" + targetName + " &aустановлен на &e" + amount + " ❂");
                } catch (NumberFormatException e) {
                    TextUtil.send(player, "&cСумма должна быть числом!");
                }
                return;
            } else if (prompt.type() == PromptType.CORRUPTION_SET) {
                if (input.equalsIgnoreCase("отмена") || input.equalsIgnoreCase("cancel")) {
                    TextUtil.send(player, "&cВвод отменен.");
                    services.gui().openAdminDarkMagic(player);
                    return;
                }
                player.performCommand("admin corruption add " + input);
                return;
            }

            Guild guild = services.guilds().getGuilds().get(prompt.guildId());
            if (guild == null) return;

            try {
                if (prompt.type() == PromptType.RENAME_RANK) {
                    Guild.Rank rank = guild.getRanks().get(prompt.rankId());
                    if (rank != null) {
                        rank.setName(TextUtil.color(input));
                        services.guilds().saveGuildAsync(guild);
                        TextUtil.send(player, "&aНазвание ранга успешно изменено на " + rank.getName());
                    }
                    services.gui().openRankSettings(player, guild, prompt.rankId());
                } 
                else if (prompt.type() == PromptType.CHANGE_PRIORITY) {
                    Guild.Rank rank = guild.getRanks().get(prompt.rankId());
                    if (rank != null) {
                        int weight = Integer.parseInt(input);
                        rank.setPriority(weight);
                        services.guilds().saveGuildAsync(guild);
                        TextUtil.send(player, "&aПриоритет ранга изменен на &f" + weight);
                    }
                    services.gui().openRankSettings(player, guild, prompt.rankId());
                }
                else if (prompt.type() == PromptType.CREATE_RANK) {
                    String id = UUID.randomUUID().toString().substring(0, 8);
                    Guild.Rank newRank = new Guild.Rank(id, TextUtil.color(input), 10, new java.util.HashSet<>());
                    guild.getRanks().put(id, newRank);
                    services.guilds().saveGuildAsync(guild);
                    TextUtil.send(player, "&aНовый ранг " + newRank.getName() + " &aуспешно создан!");
                    services.gui().openRankSettings(player, guild, id); 
                }
            } catch (NumberFormatException e) {
                TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_148d04", "&cОшибка: Приоритет должен быть числом!"));
                services.gui().openGuildRanksList(player, guild);
            }
        });
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        InventoryHolder topHolder = event.getView().getTopInventory().getHolder();
        if (topHolder instanceof RecipePreviewHolder || topHolder instanceof GuiManager.MenuHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        InventoryHolder topHolder = event.getView().getTopInventory().getHolder();

        if (topHolder instanceof RecipePreviewHolder) {
            event.setCancelled(true);
            if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.BARRIER) {
                services.gui().openItems(player, "Броня");
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            }
            return;
        }

        if (!(topHolder instanceof GuiManager.MenuHolder holder)) return;

        event.setCancelled(true);
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;
        
        ItemStack current = event.getCurrentItem();
        if (current == null || current.getType() == Material.AIR) return;

        Guild guild = services.guilds().getPlayerGuild(player.getUniqueId());

        if (holder.type() == GuiManager.MenuType.GUILD) {
            if (guild == null) return;
            switch (event.getSlot()) {
                case 10 -> services.gui().openGuildMembers(player, guild);
                case 15 -> services.gui().openGuildTreasury(player, guild);
                case 16 -> {
                    if (!guild.getLeader().equals(player.getUniqueId())) {
                        TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_416ff9", "&cТолько лидер гильдии может настраивать права!"));
                        return;
                    }
                    services.gui().openGuildRanksList(player, guild); 
                }
                case 22 -> services.gui().openMain(player);
            }
            return;
        }

        if (holder.type() == GuiManager.MenuType.GUILD_TREASURY) {
            if (guild == null) return;
            long amount = event.isShiftClick() ? 10000 : 1000;

            if (event.getSlot() == 11) { 
                if (services.economy().getBalance(player.getUniqueId()) < amount) {
                    TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_acc6a9", "&cУ вас недостаточно монет!"));
                    return;
                }
                services.economy().addBalance(player.getUniqueId(), player.getName(), -amount);
                guild.setBalance(guild.getBalance() + amount);
                TextUtil.send(player, "&aВы внесли &f" + amount + " ❂ &aв казну!");
                services.gui().openGuildTreasury(player, guild);
            }
            else if (event.getSlot() == 15) { 
                if (!guild.hasPermission(player.getUniqueId(), "guild.bank")) {
                    TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_4791b6", "&cУ вас нет прав снимать деньги!"));
                    return;
                }
                if (guild.getBalance() < amount) {
                    TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_fbf05a", "&cВ казне недостаточно денег!"));
                    return;
                }
                guild.setBalance(guild.getBalance() - amount);
                services.economy().addBalance(player.getUniqueId(), player.getName(), amount);
                TextUtil.send(player, "&aВы сняли &f" + amount + " ❂ &aиз казны!");
                services.gui().openGuildTreasury(player, guild);
            }
            else if (event.getSlot() == 22) {
                services.gui().openGuildMain(player, guild);
            }
            return;
        }

        if (holder.type() == GuiManager.MenuType.GUILD_MEMBERS) {
            if (event.getSlot() == 49) {
                services.gui().openGuildMain(player, guild);
                return;
            }
            if (current.getType() != Material.PLAYER_HEAD) return;

            SkullMeta meta = (SkullMeta) current.getItemMeta();
            if (meta == null || meta.getOwningPlayer() == null) return;
            UUID targetUuid = meta.getOwningPlayer().getUniqueId();

            if (guild == null || !guild.hasPermission(player.getUniqueId(), "guild.promote")) {
                TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_bb790b", "&cУ вас нет прав управлять составом!"));
                return;
            }
            
            if (targetUuid.equals(player.getUniqueId())) return;

            if (event.isShiftClick() && event.isRightClick()) {
                if (!guild.hasPermission(player.getUniqueId(), "guild.kick")) {
                    TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_8083d4", "&cУ вас нет прав исключать игроков!"));
                    return;
                }
                services.guilds().kick(guild, targetUuid);
            } else if (event.isLeftClick()) {
                services.guilds().promote(guild, targetUuid);
            } else if (event.isRightClick()) {
                services.guilds().demote(guild, targetUuid);
            }
            services.gui().openGuildMembers(player, guild);
            return;
        }

        if (holder.type() == GuiManager.MenuType.GUILD_RANKS_LIST) {
            if (guild == null) return;
            if (!guild.getLeader().equals(player.getUniqueId())) {
                TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_f26722", "&cТолько лидер гильдии может управлять рангами!"));
                player.closeInventory();
                return;
            }
            
            if (event.getSlot() == 49) {
                services.gui().openGuildMain(player, guild);
            } else if (event.getSlot() == 53) {
                activePrompts.put(player.getUniqueId(), new ChatPrompt(guild.getId(), null, PromptType.CREATE_RANK));
                player.closeInventory();
                TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_33729a", "&eВведите название нового ранга в чат (или 'отмена' для выхода):"));
            } else if (current.getType() == Material.IRON_CHESTPLATE) {
                if (!current.hasItemMeta() || !current.getItemMeta().hasLore()) return;
                String rawLore = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(current.getItemMeta().lore().get(0));
                String rankId = rawLore.replace("ID:", "").trim();
                services.gui().openRankSettings(player, guild, rankId);
            }
            return;
        }

        if (holder.type() == GuiManager.MenuType.GUILD_RANK_SETTINGS) {
            if (guild == null) return;
            if (!guild.getLeader().equals(player.getUniqueId())) {
                TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_f26722", "&cТолько лидер гильдии может управлять рангами!"));
                player.closeInventory();
                return;
            }
            String rankId = holder.metadata();
            
            if (event.getSlot() == 22) {
                services.gui().openGuildRanksList(player, guild);
            } else if (event.getSlot() == 10) {
                activePrompts.put(player.getUniqueId(), new ChatPrompt(guild.getId(), rankId, PromptType.RENAME_RANK));
                player.closeInventory();
                TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_cde61e", "&eВведите новое название ранга (можно использовать цвета &c&l) или 'отмена':"));
            } else if (event.getSlot() == 14) {
                activePrompts.put(player.getUniqueId(), new ChatPrompt(guild.getId(), rankId, PromptType.CHANGE_PRIORITY));
                player.closeInventory();
                TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_5880da", "&eВведите новый приоритет (число, например 50) или 'отмена':"));
            } else if (event.getSlot() == 12) {
                services.gui().openRankPermissions(player, guild, rankId);
            } else if (event.getSlot() == 16 && event.isShiftClick()) {
                guild.getRanks().remove(rankId);
                for (java.util.Map.Entry<UUID, String> member : guild.getMembers().entrySet()) {
                    if (member.getValue().equals(rankId)) {
                        member.setValue("recruit");
                    }
                }
                services.guilds().saveGuildAsync(guild);
                TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_ec03a8", "&cРанг был удален."));
                services.gui().openGuildRanksList(player, guild);
            }
            return;
        }

        if (holder.type() == GuiManager.MenuType.GUILD_RANK_PERMISSIONS) {
            if (guild == null) return;
            if (!guild.getLeader().equals(player.getUniqueId())) {
                TextUtil.send(player, com.astrasmp.AstraSMPPlugin.getInstance().getConfigManager().getMessage("msg_f26722", "&cТолько лидер гильдии может управлять рангами!"));
                player.closeInventory();
                return;
            }
            String rankId = holder.metadata();

            if (event.getSlot() == 40) {
                services.gui().openRankSettings(player, guild, rankId);
                return;
            }

            String node = switch (event.getSlot()) {
                case 10 -> "guild.home.set";
                case 11 -> "guild.bank";
                case 12 -> "guild.invite";
                case 13 -> "guild.kick";
                case 14 -> "guild.promote";
                case 15 -> "guild.upgrade";
                default -> null;
            };

            if (node != null) {
                Guild.Rank rank = guild.getRanks().get(rankId);
                if (rank != null) {
                    if (rank.getPermissions().contains(node)) rank.getPermissions().remove(node);
                    else rank.getPermissions().add(node);
                    
                    services.guilds().saveGuildAsync(guild);
                    services.gui().openRankPermissions(player, guild, rankId);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1.2f);
                }
            }
            return;
        }

        if (holder.type() == GuiManager.MenuType.QUESTS) {
            if (event.getSlot() == 31) {
                services.gui().openMain(player);
            }
            return;
        }

        services.gui().handleClick(player, event);
    }

    private static class RecipePreviewHolder implements InventoryHolder {
        @Override public @NotNull Inventory getInventory() { return null; }
    }
}