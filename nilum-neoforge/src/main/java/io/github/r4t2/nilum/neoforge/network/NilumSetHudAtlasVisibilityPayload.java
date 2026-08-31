package io.github.r4t2.nilum.neoforge.network;

import io.github.r4t2.nilum.common.protocol.NilumChannels;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Thin CustomPacketPayload wrapper around a raw SetHudAtlasVisibilityPacket. See NilumModelSpawnPayload. */
public record NilumSetHudAtlasVisibilityPayload(byte[] data) implements CustomPacketPayload {

    public static final Type<NilumSetHudAtlasVisibilityPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(NilumChannels.NAMESPACE, NilumChannels.SET_HUD_ATLAS_VISIBILITY));

    public static final StreamCodec<ByteBuf, NilumSetHudAtlasVisibilityPayload> CODEC =
            RawByteArrayCodec.INSTANCE.map(NilumSetHudAtlasVisibilityPayload::new, NilumSetHudAtlasVisibilityPayload::data);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
