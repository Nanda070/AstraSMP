package com.astrasmp.model;

import com.astrasmp.util.LocationKey;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MENetwork {
    private final UUID networkId;
    private final UUID owner;
    private final Set<MENode> nodes;
    private final Map<String, Long> storage;
    private final Map<LocationKey, String> driveInventories;

    private long maxCapacity;

    public MENetwork(UUID networkId, UUID owner) {
        this.networkId = networkId;
        this.owner = owner;
        this.nodes = ConcurrentHashMap.newKeySet();
        this.storage = new ConcurrentHashMap<>();
        this.driveInventories = new ConcurrentHashMap<>();
        this.maxCapacity = 0;
    }

    public UUID getNetworkId() { return networkId; }
    public UUID getOwner() { return owner; }
    public Set<MENode> getNodes() { return nodes; }
    public Map<String, Long> getStorage() { return storage; }
    public Map<LocationKey, String> getDriveInventories() { return driveInventories; }
    public long getMaxCapacity() { return maxCapacity; }

    public void setMaxCapacity(long maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public void addNode(MENode node) {
        nodes.add(node);
    }

    public void removeNode(MENode node) {
        nodes.remove(node);
    }

    public long getCurrentLoad() {
        return storage.values().stream().mapToLong(Long::longValue).sum();
    }

    public boolean canAcceptItem(long amount) {
        return (getCurrentLoad() + amount) <= maxCapacity;
    }

    public void insertItem(String itemHash, long amount) {
        storage.merge(itemHash, amount, Long::sum);
    }

    public long extractItem(String itemHash, long requestedAmount) {
        long current = storage.getOrDefault(itemHash, 0L);
        if (current == 0) return 0;

        long toExtract = Math.min(current, requestedAmount);
        long remainder = current - toExtract;

        if (remainder <= 0) {
            storage.remove(itemHash);
        } else {
            storage.put(itemHash, remainder);
        }
        return toExtract;
    }
}