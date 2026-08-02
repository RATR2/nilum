package io.github.r4t2.nilum.paper.block;

import org.bukkit.Location;

/** Immutable world-block coordinate, used as a map key; Location's mutability makes it unsuitable for that. */
public record BlockKey(String world, int x, int y, int z) {

    public static BlockKey of(Location location) {
        return new BlockKey(location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public int chunkX() {
        return x >> 4;
    }

    public int chunkZ() {
        return z >> 4;
    }
}
