package com.astrasmp.model;

import org.bukkit.Location;
import java.util.List;
import java.util.UUID;

public record ClaimRecord(
        UUID id,
        UUID owner,
        List<UUID> members,
        String world,
        int x, int y, int z,
        int radius
) {
    public boolean isInside(Location loc) {
        if (!loc.getWorld().getName().equals(world)) return false;
        return Math.abs(loc.getBlockX() - x) <= radius &&
                Math.abs(loc.getBlockZ() - z) <= radius;
    }
}