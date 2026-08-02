package io.github.r4t2.nilum.paper.block;

import io.github.r4t2.nilum.common.protocol.ChunkBlockEntry;
import io.github.r4t2.nilum.common.protocol.ChunkBlocksPacket;
import io.github.r4t2.nilum.common.protocol.NilumChannels;
import io.papermc.paper.event.packet.PlayerChunkLoadEvent;
import io.github.r4t2.nilum.paper.NilumPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.List;

/** Sends a chunk's Nilum block positions right when that chunk is actually sent to a player. */
public final class CustomBlockChunkSync implements Listener {

    private final NilumPlugin plugin;
    private final CustomBlockRegistry registry;

    public CustomBlockChunkSync(NilumPlugin plugin, CustomBlockRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    @EventHandler
    public void onChunkLoad(PlayerChunkLoadEvent event) {
        Player player = event.getPlayer();
        if (!plugin.handshakes().hasClient(player.getUniqueId())) {
            return;
        }

        int chunkX = event.getChunk().getX();
        int chunkZ = event.getChunk().getZ();
        List<ChunkBlockEntry> entries = registry.entriesInChunk(event.getChunk().getWorld().getName(), chunkX, chunkZ);
        if (entries.isEmpty()) {
            return;
        }

        player.sendPluginMessage(plugin, NilumChannels.CHUNK_BLOCKS_QUALIFIED,
                new ChunkBlocksPacket(chunkX, chunkZ, entries).encode());
    }
}
