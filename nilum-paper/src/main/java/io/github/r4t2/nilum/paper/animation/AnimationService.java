package io.github.r4t2.nilum.paper.animation;

import io.github.r4t2.nilum.common.protocol.BlockAnimationPlayPacket;
import io.github.r4t2.nilum.common.protocol.BlockAnimationStopPacket;
import io.github.r4t2.nilum.common.protocol.EntityAnimationPlayPacket;
import io.github.r4t2.nilum.common.protocol.EntityAnimationStopPacket;
import io.github.r4t2.nilum.common.protocol.ItemAnimationPlayPacket;
import io.github.r4t2.nilum.common.protocol.ItemAnimationStopPacket;
import io.github.r4t2.nilum.common.protocol.NilumChannels;
import io.github.r4t2.nilum.paper.NilumPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.MainHand;

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

    /** mainHand is the caller's intent ("their main hand"); resolved here to the visual left/right side the client keys state by. */
    public void playHeldItemAnimation(Player player, boolean mainHand, String animationName) {
        broadcast(NilumChannels.ITEM_ANIMATION_PLAY_QUALIFIED, new ItemAnimationPlayPacket(
                player.getUniqueId(), resolveVisualRightHand(player, mainHand), animationName, System.currentTimeMillis()).encode());
    }

    public void stopHeldItemAnimation(Player player, boolean mainHand) {
        broadcast(NilumChannels.ITEM_ANIMATION_STOP_QUALIFIED, new ItemAnimationStopPacket(
                player.getUniqueId(), resolveVisualRightHand(player, mainHand)).encode());
    }

    private static boolean resolveVisualRightHand(Player player, boolean mainHand) {
        boolean mainIsRight = player.getMainHand() == MainHand.RIGHT;
        return mainHand == mainIsRight;
    }

    private void broadcast(String channel, byte[] data) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (plugin.handshakes().hasClient(player.getUniqueId())) {
                player.sendPluginMessage(plugin, channel, data);
            }
        }
    }
}
