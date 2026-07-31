package io.github.r4t2.nilum.fabric.network;

import io.github.r4t2.nilum.common.protocol.NilumChannels;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Thin CustomPacketPayload wrapper around a raw {@code HudFramePacket}. See {@link NilumHelloPayload}. */
public record NilumHudFramePayload(byte[] data) implements CustomPacketPayload {

    public static final Type<NilumHudFramePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(NilumChannels.NAMESPACE, NilumChannels.HUD_FRAME));

    public static final StreamCodec<ByteBuf, NilumHudFramePayload> CODEC =
            RawByteArrayCodec.INSTANCE.map(NilumHudFramePayload::new, NilumHudFramePayload::data);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
