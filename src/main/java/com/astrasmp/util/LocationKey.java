package com.astrasmp.util;

import org.bukkit.Location;
import org.bukkit.World;
import java.util.Objects;

public record LocationKey(String worldName, int x, int y, int z) {

    public org.bukkit.Location toLocation() {
    return new org.bukkit.Location(org.bukkit.Bukkit.getWorld(worldName), x, y, z);
    }

    public static LocationKey fromLocation(Location loc) {
        if (loc == null) return null;
        World world = loc.getWorld();
        if (world == null) return null;
        
        return new LocationKey(world.getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LocationKey)) return false;
        LocationKey that = (LocationKey) o;
        return x == that.x && y == that.y && z == that.z && worldName.equals(that.worldName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(worldName, x, y, z);
    }
}