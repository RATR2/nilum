package io.github.r4t2.nilum.paper.shader;

import io.github.r4t2.nilum.common.protocol.ActivateShaderPackPacket;
import io.github.r4t2.nilum.common.protocol.DeactivateShaderPackPacket;
import io.github.r4t2.nilum.common.protocol.NilumChannels;
import io.github.r4t2.nilum.paper.NilumPlugin;
import org.bukkit.entity.Player;

/**
 * Server-side entry point for switching a player's active Iris shaderpack. Thin fire-and-forget
 * packet sender, gated on handshake, same pattern as HudAtlasService. Only sends the
 * activate/deactivate signal; the pack itself streams via the normal asset manifest/TCP pipeline.
 */
public final class ShaderPackService {

    private final NilumPlugin plugin;

    public ShaderPackService(NilumPlugin plugin) {
        this.plugin = plugin;
    }

    public void activate(Player player, String packId) {
        send(player, NilumChannels.ACTIVATE_SHADER_PACK_QUALIFIED, new ActivateShaderPackPacket(packId).encode());
    }

    public void deactivate(Player player) {
        send(player, NilumChannels.DEACTIVATE_SHADER_PACK_QUALIFIED, new DeactivateShaderPackPacket().encode());
    }

    private void send(Player player, String channel, byte[] data) {
        if (plugin.handshakes().hasClient(player.getUniqueId())) {
            player.sendPluginMessage(plugin, channel, data);
        }
    }
}
