package com.astrasmp.service;

import com.astrasmp.AstraSMPPlugin;
import com.astrasmp.model.MENetwork;
import com.astrasmp.model.MENode;
import com.astrasmp.util.ItemSerializer;
import com.astrasmp.util.LocationKey;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MENetworkService {

    private final Map<UUID, MENetwork> activeNetworks;
    private final Map<LocationKey, UUID> nodeMap;

    private static final int CONNECTION_RADIUS = 16;

    public MENetworkService(Map<UUID, MENetwork> storeMap) {
        this.activeNetworks = storeMap;
        this.nodeMap = new ConcurrentHashMap<>();

        for (MENetwork network : activeNetworks.values()) {
            for (MENode node : network.getNodes()) {
                nodeMap.put(node.getLocation(), network.getNetworkId());
            }
        }
    }

    public Map<UUID, MENetwork> getActiveNetworks() {
        return activeNetworks;
    }

    public MENetwork createNetwork(UUID owner, Location controllerLoc) {
        UUID networkId = UUID.randomUUID();
        MENetwork network = new MENetwork(networkId, owner);
        network.setMaxCapacity(1024); // База контроллера

        LocationKey key = LocationKey.fromLocation(controllerLoc);
        MENode controllerNode = new MENode(key, MENode.NodeType.CONTROLLER);
        network.addNode(controllerNode);

        activeNetworks.put(networkId, network);
        nodeMap.put(key, networkId);
        return network;
    }

    public boolean attachNode(Location nodeLoc, MENode.NodeType type) {
        Optional<MENetwork> nearbyNetwork = findNearbyNetwork(nodeLoc);

        if (nearbyNetwork.isPresent()) {
            MENetwork network = nearbyNetwork.get();
            LocationKey key = LocationKey.fromLocation(nodeLoc);
            MENode node = new MENode(key, type);
            network.addNode(node);
            nodeMap.put(key, network.getNetworkId());

            recalculateCapacity(network);
            return true;
        }
        return false;
    }

    public void removeNode(Location loc) {
        LocationKey key = LocationKey.fromLocation(loc);
        UUID networkId = nodeMap.remove(key);
        
        if (networkId != null) {
            MENetwork network = activeNetworks.get(networkId);
            if (network != null) {
                MENode nodeToRemove = network.getNodes().stream()
                        .filter(n -> n.getLocation().equals(key))
                        .findFirst().orElse(null);

                if (nodeToRemove != null && nodeToRemove.getType() == MENode.NodeType.DRIVE) {
                    dropDriveContents(network, loc); // Используем Bukkit Location для дропа
                }

                network.getNodes().removeIf(node -> node.getLocation().equals(key));

                boolean hasController = network.getNodes().stream()
                        .anyMatch(node -> node.getType() == MENode.NodeType.CONTROLLER);

                if (!hasController) {
                    shutdownNetwork(networkId);
                } else {
                    recalculateCapacity(network);
                }
            }
        }
    }

    public MENetwork getNetworkByLocation(Location loc) {
        UUID networkId = nodeMap.get(LocationKey.fromLocation(loc));
        return networkId != null ? activeNetworks.get(networkId) : null;
    }

    public boolean isMENode(Location loc) {
        return nodeMap.containsKey(LocationKey.fromLocation(loc));
    }

    public String getMETypeFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        NamespacedKey key = new NamespacedKey(AstraSMPPlugin.getInstance(), "me_component");
        if (item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
            return item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
        }
        return null;
    }

    public void recalculateCapacity(MENetwork network) {
        long totalCapacity = 1024; // Дефолт Контроллера

        for (String base64 : network.getDriveInventories().values()) {
            if (base64 == null || base64.isEmpty()) continue;
            ItemStack[] contents = ItemSerializer.itemStackArrayFromBase64(base64);
            for (ItemStack item : contents) {
                totalCapacity += getCellCapacity(item);
            }
        }
        network.setMaxCapacity(totalCapacity);
    }

    private long getCellCapacity(ItemStack item) {
        String type = getMETypeFromItem(item);
        if (type == null) return 0;
        return switch (type) {
            case "cell_4k" -> 4096;
            case "cell_16k" -> 16384;
            case "cell_64k" -> 65536;
            default -> 0;
        };
    }

    private void dropDriveContents(MENetwork network, Location driveLoc) {
        LocationKey key = LocationKey.fromLocation(driveLoc);
        String base64 = network.getDriveInventories().remove(key);
        if (base64 != null) {
            ItemStack[] contents = ItemSerializer.itemStackArrayFromBase64(base64);
            for (ItemStack item : contents) {
                if (item != null && !item.getType().isAir()) {
                    driveLoc.getWorld().dropItemNaturally(driveLoc, item);
                }
            }
        }
    }

    private Optional<MENetwork> findNearbyNetwork(Location loc) {
        return activeNetworks.values().stream()
                .filter(net -> net.getNodes().stream()
                        .anyMatch(node -> node.getType() == MENode.NodeType.CONTROLLER &&
                                node.getLocation().worldName().equals(loc.getWorld().getName()) &&
                                node.getLocation().toLocation().distance(loc) <= CONNECTION_RADIUS))
                .findFirst();
    }

    private void shutdownNetwork(UUID networkId) {
        MENetwork network = activeNetworks.remove(networkId);
        if (network != null) {
            network.getNodes().forEach(node -> nodeMap.remove(node.getLocation()));
        }
    }
}