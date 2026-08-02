package io.github.r4t2.nilum.paper.block;

import io.github.r4t2.nilum.common.protocol.ChunkBlockEntry;
import io.github.r4t2.nilum.common.protocol.ChunkBlocksPacket;
import io.github.r4t2.nilum.common.protocol.NilumChannels;
import io.github.r4t2.nilum.paper.NilumPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

/** Immediate single-block push to every connected player; PlayerChunkLoadEvent only fires on new chunk loads. */
public final class CustomBlockBroadcaster {

    private CustomBlockBroadcaster() {
    }

    public static void broadcastPlacement(NilumPlugin plugin, Location location, String modelId) {
        broadcast(plugin, location, modelId);
    }

    /** modelId null tells the client to stop treating this position as a Nilum block. */
    public static void broadcastRemoval(NilumPlugin plugin, Location location) {
        broadcast(plugin, location, null);
    }

    private static void broadcast(NilumPlugin plugin, Location location, String modelId) {
        var entry = new ChunkBlockEntry(location.getBlockX(), location.getBlockY(), location.getBlockZ(), modelId);
        byte[] data = new ChunkBlocksPacket(location.getBlockX() >> 4, location.getBlockZ() >> 4, List.of(entry)).encode();

        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (plugin.handshakes().hasClient(online.getUniqueId())) {
                online.sendPluginMessage(plugin, NilumChannels.CHUNK_BLOCKS_QUALIFIED, data);
            }
        }
    }
}
