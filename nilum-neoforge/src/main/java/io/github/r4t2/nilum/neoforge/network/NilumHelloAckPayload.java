package io.github.r4t2.nilum.neoforge.network;

import io.github.r4t2.nilum.common.protocol.NilumChannels;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Thin CustomPacketPayload wrapper around a raw HelloAckPacket. See NilumHelloPayload. */
public record NilumHelloAckPayload(byte[] data) implements CustomPacketPayload {

    public static final Type<NilumHelloAckPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(NilumChannels.NAMESPACE, NilumChannels.HELLO_ACK));

    public static final StreamCodec<ByteBuf, NilumHelloAckPayload> CODEC =
            RawByteArrayCodec.INSTANCE.map(NilumHelloAckPayload::new, NilumHelloAckPayload::data);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
