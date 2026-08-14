package io.github.r4t2.nilum.paper.hud;

import io.github.r4t2.nilum.common.protocol.AtlasPatchPacket;
import io.github.r4t2.nilum.common.protocol.HudFrameOverridePacket;
import io.github.r4t2.nilum.common.protocol.HudFramePacket;
import io.github.r4t2.nilum.common.protocol.HudFrameReleasePacket;
import io.github.r4t2.nilum.common.protocol.NilumChannels;
import io.github.r4t2.nilum.common.protocol.RegisterClientVarPacket;
import io.github.r4t2.nilum.common.protocol.SetClientVarPacket;
import io.github.r4t2.nilum.common.protocol.SetHudTextPacket;
import io.github.r4t2.nilum.paper.NilumPlugin;
import org.bukkit.entity.Player;

/** Server-side entry point for driving HUD atlas elements; a thin fire-and-forget packet sender, gated on the Nilum handshake. */
public final class HudAtlasService {

    private final NilumPlugin plugin;

    public HudAtlasService(NilumPlugin plugin) {
        this.plugin = plugin;
    }

    public void setHudFrame(Player player, String atlasId, String elementId, int frame) {
        send(player, NilumChannels.HUD_FRAME_QUALIFIED, new HudFramePacket(atlasId, elementId, frame).encode());
    }

    public void setHudFrameForAll(String atlasId, String elementId, int frame) {
        byte[] data = new HudFramePacket(atlasId, elementId, frame).encode();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            send(player, NilumChannels.HUD_FRAME_QUALIFIED, data);
        }
    }

    public void sendAtlasPatch(Player player, String atlasId, String elementId, int frame, byte[] png) {
        send(player, NilumChannels.ATLAS_PATCH_QUALIFIED, new AtlasPatchPacket(atlasId, elementId, frame, png).encode());
    }

    public void overrideHudFrame(Player player, String atlasId, String elementId, int frame, int durationTicks) {
        send(player, NilumChannels.HUD_FRAME_OVERRIDE_QUALIFIED,
                new HudFrameOverridePacket(atlasId, elementId, frame, durationTicks).encode());
    }

    public void releaseHudFrame(Player player, String atlasId, String elementId) {
        send(player, NilumChannels.HUD_FRAME_RELEASE_QUALIFIED,
                new HudFrameReleasePacket(atlasId, elementId).encode());
    }

    public void registerClientVar(Player player, String name, double initialValue) {
        send(player, NilumChannels.REGISTER_CLIENT_VAR_QUALIFIED,
                new RegisterClientVarPacket(name, initialValue).encode());
    }

    public void setClientVar(Player player, String name, double value) {
        send(player, NilumChannels.SET_CLIENT_VAR_QUALIFIED, new SetClientVarPacket(name, value).encode());
    }

    public void setClientVarForAll(String name, double value) {
        byte[] data = new SetClientVarPacket(name, value).encode();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            send(player, NilumChannels.SET_CLIENT_VAR_QUALIFIED, data);
        }
    }

    public void setHudText(Player player, String atlasId, String elementId, String text) {
        send(player, NilumChannels.SET_HUD_TEXT_QUALIFIED, new SetHudTextPacket(atlasId, elementId, text).encode());
    }

    private void send(Player player, String channel, byte[] data) {
        if (plugin.handshakes().hasClient(player.getUniqueId())) {
            player.sendPluginMessage(plugin, channel, data);
        }
    }
}
