package com.astrasmp.gui;

import com.astrasmp.model.MENetwork;
import com.astrasmp.service.ServiceManager;
import com.astrasmp.util.ItemSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class METerminalGui implements Listener {

    private final ServiceManager services;
    private static final int ITEMS_PER_PAGE = 45;
    private final NamespacedKey HASH_KEY;

    private static final Map<UUID, TerminalSession> sessions = new ConcurrentHashMap<>();
    private static final Set<UUID> awaitingSearch = ConcurrentHashMap.newKeySet();

    public METerminalGui(ServiceManager services) {
        this.services = services;
        this.HASH_KEY = new NamespacedKey(services.plugin(), "me_item_hash");
    }

    public void open(Player player, MENetwork network) {
        TerminalSession session = sessions.computeIfAbsent(player.getUniqueId(), k -> new TerminalSession(network));
        session.network = network;

        String rawTitle = "§8ME Терминал | " + network.getNetworkId().toString().substring(0, 8);
        Component title = LegacyComponentSerializer.legacySection().deserialize(rawTitle);
        Inventory inv = Bukkit.createInventory(null, 54, title);

        List<Map.Entry<String, Long>> filteredItems = new ArrayList<>();

        for (Map.Entry<String, Long> entry : network.getStorage().entrySet()) {
            ItemStack item = ItemSerializer.fromBase64(entry.getKey());
            if (item == null) continue;

            if (session.searchQuery != null) {
                String itemName = item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                        ? item.getItemMeta().getDisplayName()
                        : item.getType().name();

                String cleanName = org.bukkit.ChatColor.stripColor(itemName).toLowerCase();
                if (!cleanName.contains(session.searchQuery.toLowerCase())) {
                    continue;
                }
            }
            filteredItems.add(entry);
        }

        filteredItems.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

        int startIndex = session.page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, filteredItems.size());

        for (int i = startIndex; i < endIndex; i++) {
            Map.Entry<String, Long> entry = filteredItems.get(i);
            ItemStack displayItem = ItemSerializer.fromBase64(entry.getKey());

            if (displayItem != null) {
                long amount = entry.getValue();
                ItemMeta meta = displayItem.getItemMeta();
                if (meta != null) {
                    List<Component> lore = new ArrayList<>();
                    if (meta.hasLore()) {
                        for (String l : meta.getLore()) {
                            lore.add(LegacyComponentSerializer.legacySection().deserialize(l));
                        }
                    }
                    lore.add(LegacyComponentSerializer.legacySection().deserialize("§8§m-----------------"));
                    lore.add(LegacyComponentSerializer.legacySection().deserialize("§7В сети: §e" + amount));
                    lore.add(Component.empty());
                    lore.add(LegacyComponentSerializer.legacySection().deserialize("§fЛКМ §7- Взять стак"));
                    lore.add(LegacyComponentSerializer.legacySection().deserialize("§fПКМ §7- Взять 1 шт"));
                    meta.lore(lore);

                    meta.getPersistentDataContainer().set(HASH_KEY, PersistentDataType.STRING, entry.getKey());
                    displayItem.setItemMeta(meta);
                }
                displayItem.setAmount(amount > 64 ? 1 : (int) amount);
                inv.setItem(i - startIndex, displayItem);
            }
        }

        if (session.page > 0) {
            inv.setItem(45, createButton(Material.ARROW, "§aПред. страница", "§7Текущая: " + (session.page + 1)));
        }

        if (filteredItems.size() > endIndex) {
            inv.setItem(53, createButton(Material.ARROW, "§aСлед. страница", "§7Текущая: " + (session.page + 1)));
        }

        String searchLore = session.searchQuery == null ? "§7Кликните, чтобы найти предмет" : "§7Текущий фильтр: §e" + session.searchQuery;
        inv.setItem(48, createButton(Material.NAME_TAG, "§eПоиск", searchLore));

        if (session.searchQuery != null) {
            inv.setItem(50, createButton(Material.BARRIER, "§cСбросить поиск", "§7Кликните, чтобы показать все предметы"));
        }

        long currentLoad = network.getCurrentLoad();
        inv.setItem(49, createButton(Material.COMPASS, "§bСтатус сети",
                "§7Занято: §f" + currentLoad + " §7/ §f" + network.getMaxCapacity(),
                "§7Владелец: §f" + Bukkit.getOfflinePlayer(network.getOwner()).getName()));

        player.openInventory(inv);
    }

    private ItemStack createButton(Material mat, String name, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LegacyComponentSerializer.legacySection().deserialize(name));
            List<Component> lore = new ArrayList<>();
            for (String l : loreLines) {
                lore.add(LegacyComponentSerializer.legacySection().deserialize(l));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @SuppressWarnings("unused")
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String viewTitle = event.getView().getTitle();
        if (!viewTitle.startsWith("§8ME Терминал") && !viewTitle.startsWith("ME Терминал")) return;
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        Inventory clickedInv = event.getClickedInventory();
        if (clickedInv == null) return;

        TerminalSession session = sessions.get(player.getUniqueId());
        if (session == null || session.network == null) {
            player.closeInventory();
            return;
        }

        MENetwork network = session.network;

        // Положить в сеть
        if (clickedInv.equals(event.getView().getBottomInventory())) {
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType().isAir()) return;

            String meType = services.meNetwork().getMETypeFromItem(clickedItem);
            if (meType != null && meType.startsWith("cell")) {
                player.sendMessage("§cВы не можете положить ME-ячейку в сеть!");
                return;
            }

            long amountToAdd = event.isShiftClick() ? clickedItem.getAmount() : 1;

            if (!network.canAcceptItem(amountToAdd)) {
                player.sendMessage("§cME-сеть переполнена!");
                return;
            }

            String hash = ItemSerializer.getHash(clickedItem);
            network.insertItem(hash, amountToAdd);

            clickedItem.setAmount((int) (clickedItem.getAmount() - amountToAdd));
            open(player, network);
            return;
        }

        // Взять из сети или кнопки
        if (clickedInv.equals(event.getView().getTopInventory())) {
            int slot = event.getSlot();
            ItemStack currentItem = event.getCurrentItem();

            if (currentItem == null || currentItem.getType().isAir()) return;

            if (slot == 45 && currentItem.getType() == Material.ARROW) {
                session.page = Math.max(0, session.page - 1);
                open(player, network);
                return;
            }
            if (slot == 53 && currentItem.getType() == Material.ARROW) {
                session.page++;
                open(player, network);
                return;
            }
            if (slot == 48 && currentItem.getType() == Material.NAME_TAG) {
                player.closeInventory();
                awaitingSearch.add(player.getUniqueId());
                player.sendMessage("§e[ME] Введите название предмета в чат. Для отмены введите '-'.");
                return;
            }
            if (slot == 50 && currentItem.getType() == Material.BARRIER) {
                session.searchQuery = null;
                session.page = 0;
                open(player, network);
                return;
            }

            if (!currentItem.hasItemMeta()) return;

            String hash = currentItem.getItemMeta().getPersistentDataContainer().get(HASH_KEY, PersistentDataType.STRING);
            if (hash == null) return;

            ItemStack realItem = ItemSerializer.fromBase64(hash);
            if (realItem == null) return;

            long amountToExtract = event.isRightClick() ? 1 : realItem.getMaxStackSize();
            long extracted = network.extractItem(hash, amountToExtract);

            if (extracted > 0) {
                realItem.setAmount((int) extracted);
                Map<Integer, ItemStack> leftover = player.getInventory().addItem(realItem);

                if (!leftover.isEmpty()) {
                    network.insertItem(hash, leftover.values().iterator().next().getAmount());
                    player.sendMessage("§cВаш инвентарь заполнен!");
                }
                open(player, network);
            }
        }
    }

    @SuppressWarnings("unused")
    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (awaitingSearch.contains(player.getUniqueId())) {
            event.setCancelled(true);
            awaitingSearch.remove(player.getUniqueId());

            String query = event.getMessage().trim();
            TerminalSession session = sessions.get(player.getUniqueId());

            if (session != null) {
                if (query.equals("-")) {
                    session.searchQuery = null;
                } else {
                    session.searchQuery = query;
                }
                session.page = 0;

                Bukkit.getScheduler().runTask(services.plugin(), () -> {
                    if (player.isOnline()) {
                        open(player, session.network);
                    }
                });
            }
        }
    }

    @SuppressWarnings("unused")
    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        // Оставлено для расширения
    }

    private static class TerminalSession {
        MENetwork network;
        int page = 0;
        String searchQuery = null;

        TerminalSession(MENetwork network) {
            this.network = network;
        }
    }
}