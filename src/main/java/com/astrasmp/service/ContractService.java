package com.astrasmp.service;

import com.astrasmp.AstraSMPPlugin;
import com.astrasmp.model.ContractRecord;
import com.astrasmp.model.PlayerProfile;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.UUID;

public final class ContractService {
    private final AstraSMPPlugin plugin;
    private final DataStore store;

    public ContractService(AstraSMPPlugin plugin, DataStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    public ContractRecord createBounty(Player creator, Player target, long reward, String note) {
        PlayerProfile profile = store.profile(creator.getUniqueId().toString(), creator.getName());
        double feeRate = plugin.getConfig().getDouble("contracts.bounty-fee", 0.10);
        long fee = Math.max(1L, Math.round(reward * feeRate));
        long total = reward + fee;
        if (profile.getCoins() < total) {
            return null;
        }
        profile.setCoins(profile.getCoins() - total);
        ContractRecord record = new ContractRecord(
                store.nextContractId(),
                creator.getUniqueId().toString(),
                target.getUniqueId().toString(),
                reward,
                "BOUNTY",
                note == null ? "" : note,
                true,
                System.currentTimeMillis()
        );
        store.contracts().put(record.getId(), record);
        store.requestSave();
        return record;
    }

    public ContractRecord byId(long id) {
        return store.contracts().get(id);
    }

    public Collection<ContractRecord> active() {
        return store.activeContracts();
    }

    public boolean cancel(Player requester, long id) {
        ContractRecord record = store.contracts().get(id);
        if (record == null || !record.isActive()) return false;
        if (!record.getCreatorUuid().equals(requester.getUniqueId().toString()) && !requester.hasPermission("astrasmp.admin")) return false;
        record.setActive(false);
        store.requestSave();
        return true;
    }

    public long handleKillReward(Player killer, Player victim) {
        long paid = 0L;
        for (ContractRecord contract : store.contracts().values()) {
            if (!contract.isActive()) continue;
            if (!"BOUNTY".equalsIgnoreCase(contract.getType())) continue;
            if (contract.getTargetUuid().equals(victim.getUniqueId().toString())) {
                PlayerProfile profile = store.profile(killer.getUniqueId().toString(), killer.getName());
                profile.setCoins(profile.getCoins() + contract.getReward());
                contract.setActive(false);
                paid += contract.getReward();
            }
        }
        if (paid > 0) store.requestSave();
        return paid;
    }

    public int activeCountFor(UUID creator) {
        return (int) store.contracts().values().stream().filter(c -> c.isActive() && c.getCreatorUuid().equals(creator.toString())).count();
    }
}
