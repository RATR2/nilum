package io.github.r4t2.nilum.fabric.network;

import io.github.r4t2.nilum.common.protocol.NilumChannels;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Thin CustomPacketPayload wrapper around a raw {@code TcpUnavailablePacket}. See {@link NilumHelloPayload}. */
public record NilumTcpUnavailablePayload(byte[] data) implements CustomPacketPayload {

    public static final Type<NilumTcpUnavailablePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(NilumChannels.NAMESPACE, NilumChannels.TCP_UNAVAILABLE));

    public static final StreamCodec<ByteBuf, NilumTcpUnavailablePayload> CODEC =
            ByteBufCodecs.BYTE_ARRAY.map(NilumTcpUnavailablePayload::new, NilumTcpUnavailablePayload::data);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
