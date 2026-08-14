package io.github.r4t2.nilum.paper.animation;

import io.github.r4t2.nilum.common.protocol.BlockAnimationPlayPacket;
import io.github.r4t2.nilum.common.protocol.BlockAnimationStopPacket;
import io.github.r4t2.nilum.common.protocol.EntityAnimationPlayPacket;
import io.github.r4t2.nilum.common.protocol.EntityAnimationStopPacket;
import io.github.r4t2.nilum.common.protocol.NilumChannels;
import io.github.r4t2.nilum.paper.NilumPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

/** Server-side entry point for triggering in-world animation playback; a thin fire-and-forget broadcaster, playback state lives client-side. */
public final class AnimationService {

    private final NilumPlugin plugin;

    public AnimationService(NilumPlugin plugin) {
        this.plugin = plugin;
    }

    public void playEntityAnimation(UUID entityId, String animationName) {
        broadcast(NilumChannels.ENTITY_ANIMATION_PLAY_QUALIFIED,
                new EntityAnimationPlayPacket(entityId, animationName, System.currentTimeMillis()).encode());
    }

    public void stopEntityAnimation(UUID entityId) {
        broadcast(NilumChannels.ENTITY_ANIMATION_STOP_QUALIFIED, new EntityAnimationStopPacket(entityId).encode());
    }

    public void playBlockAnimation(Location location, String animationName) {
        broadcast(NilumChannels.BLOCK_ANIMATION_PLAY_QUALIFIED, new BlockAnimationPlayPacket(
                location.getBlockX(), location.getBlockY(), location.getBlockZ(),
                animationName, System.currentTimeMillis()).encode());
    }

    public void stopBlockAnimation(Location location) {
        broadcast(NilumChannels.BLOCK_ANIMATION_STOP_QUALIFIED, new BlockAnimationStopPacket(
                location.getBlockX(), location.getBlockY(), location.getBlockZ()).encode());
    }

    private void broadcast(String channel, byte[] data) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (plugin.handshakes().hasClient(player.getUniqueId())) {
                player.sendPluginMessage(plugin, channel, data);
            }
        }
    }
}
