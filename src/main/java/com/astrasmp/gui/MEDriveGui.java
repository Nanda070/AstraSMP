package com.astrasmp.gui;

import com.astrasmp.model.MENetwork;
import com.astrasmp.service.ServiceManager;
import com.astrasmp.util.ItemSerializer;
import com.astrasmp.util.LocationKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MEDriveGui implements Listener {
    private final ServiceManager services;

    public MEDriveGui(ServiceManager services) {
        this.services = services;
    }

    public void open(Player player, MENetwork network, Location driveLoc) {
        DriveHolder holder = new DriveHolder(network, driveLoc);
        Component title = LegacyComponentSerializer.legacySection().deserialize("§8ME Дисковод");
        Inventory inv = Bukkit.createInventory(holder, 27, title);
        holder.setInventory(inv);

        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) {
            glassMeta.displayName(LegacyComponentSerializer.legacySection().deserialize("§8[Слот заблокирован]"));
            glass.setItemMeta(glassMeta);
        }

        for (int i = 10; i < 27; i++) {
            inv.setItem(i, glass);
        }

        ItemStack status = new ItemStack(Material.REDSTONE_TORCH);
        ItemMeta statusMeta = status.getItemMeta();
        if (statusMeta != null) {
            statusMeta.displayName(LegacyComponentSerializer.legacySection().deserialize("§aСтатус Дисковода"));
            List<Component> lore = List.of(
                    LegacyComponentSerializer.legacySection().deserialize("§7Сеть: §f" + network.getNetworkId().toString().substring(0, 8)),
                    LegacyComponentSerializer.legacySection().deserialize("§7Ячеек установлено: §e" + countCells(network, driveLoc) + " §7/ 10")
            );
            statusMeta.lore(lore);
            status.setItemMeta(statusMeta);
        }
        inv.setItem(22, status);

        LocationKey key = LocationKey.fromLocation(driveLoc);
        String base64 = network.getDriveInventories().get(key);
        if (base64 != null) {
            ItemStack[] contents = ItemSerializer.itemStackArrayFromBase64(base64);
            for (int i = 0; i < 10; i++) {
                if (i < contents.length && contents[i] != null && !contents[i].getType().isAir()) {
                    inv.setItem(i, formatCell(contents[i]));
                }
            }
        }

        player.openInventory(inv);
    }

    private int countCells(MENetwork network, Location driveLoc) {
        LocationKey key = LocationKey.fromLocation(driveLoc);
        String base64 = network.getDriveInventories().get(key);
        if (base64 == null) return 0;
        
        ItemStack[] contents = ItemSerializer.itemStackArrayFromBase64(base64);
        int count = 0;
        for (ItemStack item : contents) {
            if (item != null && !item.getType().isAir()) count++;
        }
        return count;
    }

    private ItemStack formatCell(ItemStack item) {
        if (item == null || item.getType().isAir()) return item;
        String type = services.meNetwork().getMETypeFromItem(item);
        if (type == null || !type.startsWith("cell_")) return item;

        long limit = switch (type) {
            case "cell_4k" -> 4096;
            case "cell_16k" -> 16384;
            case "cell_64k" -> 65536;
            default -> 0;
        };

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<Component> lore = new ArrayList<>();
            lore.add(LegacyComponentSerializer.legacySection().deserialize("§7Компонент ME-сети"));
            lore.add(LegacyComponentSerializer.legacySection().deserialize("§8§m-----------------"));
            lore.add(LegacyComponentSerializer.legacySection().deserialize("§fСтатус: §aПодключено"));
            lore.add(LegacyComponentSerializer.legacySection().deserialize("§fЕмкость: §e" + limit + " байт"));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack resetCell(ItemStack item) {
        if (item == null || item.getType().isAir()) return item;
        String type = services.meNetwork().getMETypeFromItem(item);
        if (type == null || !type.startsWith("cell_")) return item;

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.lore(List.of(LegacyComponentSerializer.legacySection().deserialize("§7Компонент ME-сети")));
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof DriveHolder)) return;

        int slot = event.getRawSlot();

        if (slot >= 10 && slot < 27) {
            event.setCancelled(true);
            return;
        }

        Inventory clickedInv = event.getClickedInventory();
        if (clickedInv == null) return;

        InventoryHolder clickedHolder = clickedInv.getHolder();
        InventoryHolder topHolder = event.getInventory().getHolder();

        if (clickedHolder == null || !clickedHolder.equals(topHolder)) {
            if (event.isShiftClick() && event.getCurrentItem() != null && !event.getCurrentItem().getType().isAir()) {
                String type = services.meNetwork().getMETypeFromItem(event.getCurrentItem());
                if (type == null || !type.startsWith("cell_")) {
                    event.setCancelled(true);
                    event.getWhoClicked().sendMessage("§cВ Дисковод можно класть только ME Ячейки!");
                }
            }
        }
        else if (slot >= 0 && slot < 10) {
            ItemStack cursor = event.getCursor();
            if (!cursor.getType().isAir()) {
                String type = services.meNetwork().getMETypeFromItem(cursor);
                if (type == null || !type.startsWith("cell_")) {
                    event.setCancelled(true);
                    event.getWhoClicked().sendMessage("§cВ Дисковод можно класть только ME Ячейки!");
                }
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof DriveHolder holder) {
            ItemStack[] cells = new ItemStack[10];
            for (int i = 0; i < 10; i++) {
                ItemStack item = event.getInventory().getItem(i);
                cells[i] = resetCell(item);
            }

            String base64 = ItemSerializer.itemStackArrayToBase64(cells);
            MENetwork network = holder.getNetwork();
            
            LocationKey key = LocationKey.fromLocation(holder.getDriveLoc());
            network.getDriveInventories().put(key, base64);

            services.meNetwork().recalculateCapacity(network);
        }
    }

    @SuppressWarnings("ClassCanBeRecord")
    public static class DriveHolder implements InventoryHolder {
        private final MENetwork network;
        private final Location driveLoc;
        private Inventory inventory;

        public DriveHolder(MENetwork network, Location driveLoc) {
            this.network = network;
            this.driveLoc = driveLoc;
        }

        public void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        public MENetwork getNetwork() { return network; }
        public Location getDriveLoc() { return driveLoc; }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory != null ? inventory : Bukkit.createInventory(null, 9);
        }
    }
}