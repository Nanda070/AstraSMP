package com.astrasmp.listener;

import com.astrasmp.model.MENetwork;
import com.astrasmp.model.MENode;
import com.astrasmp.service.ServiceManager;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class MEBlockListener implements Listener {

    private final ServiceManager services;

    public MEBlockListener(ServiceManager services) {
        this.services = services;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        String type = services.meNetwork().getMETypeFromItem(item);

        if (type == null) return;

        if (type.equalsIgnoreCase("controller")) {
            services.meNetwork().createNetwork(event.getPlayer().getUniqueId(), event.getBlock().getLocation());
            event.getPlayer().sendMessage("§b[ME] Контроллер установлен. Сеть создана.");
        } else if (type.equalsIgnoreCase("drive") || type.equalsIgnoreCase("terminal")) {
            MENode.NodeType nodeType = MENode.NodeType.valueOf(type.toUpperCase());
            boolean linked = services.meNetwork().attachNode(event.getBlock().getLocation(), nodeType);

            if (linked) {
                event.getPlayer().sendMessage("§7[ME] Узел подключен к ближайшей сети.");
            } else {
                event.getPlayer().sendMessage("§c[ME] Ошибка: Рядом нет активного контроллера! Сеть не найдена.");
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (services.meNetwork().isMENode(event.getBlock().getLocation())) {
            services.meNetwork().removeNode(event.getBlock().getLocation());
            event.getPlayer().sendMessage("§8[ME] Узел отключен от системы.");
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        Location loc = event.getClickedBlock().getLocation();

        if (services.meNetwork().isMENode(loc)) {
            MENetwork network = services.meNetwork().getNetworkByLocation(loc);
            if (network != null) {
                event.setCancelled(true); // Отменяем стандартное использование блока

                MENode clickedNode = network.getNodes().stream()
                        .filter(n -> n.getLocation().equals(com.astrasmp.util.LocationKey.fromLocation(loc)))
                        .findFirst().orElse(null);

                if (clickedNode == null) return;

                boolean hasController = network.getNodes().stream()
                        .anyMatch(n -> n.getType() == MENode.NodeType.CONTROLLER);

                if (!hasController) {
                    event.getPlayer().sendMessage("§c[ME] Сеть обесточена! Отсутствует Контроллер.");
                    return;
                }

                if (clickedNode.getType() == MENode.NodeType.TERMINAL) {
                    // Изменен вызов метода open
                    new com.astrasmp.gui.METerminalGui(services).open(event.getPlayer(), network);
                } else if (clickedNode.getType() == MENode.NodeType.DRIVE) {
                    new com.astrasmp.gui.MEDriveGui(services).open(event.getPlayer(), network, loc);
                }
            }
        }
    }
}