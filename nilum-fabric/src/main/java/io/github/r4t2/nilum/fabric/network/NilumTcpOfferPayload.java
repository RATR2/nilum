package io.github.r4t2.nilum.fabric.network;

import io.github.r4t2.nilum.common.protocol.NilumChannels;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Thin CustomPacketPayload wrapper around a raw {@code TcpOfferPacket}. See {@link NilumHelloPayload}. */
public record NilumTcpOfferPayload(byte[] data) implements CustomPacketPayload {

    public static final Type<NilumTcpOfferPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(NilumChannels.NAMESPACE, NilumChannels.TCP_OFFER));

    public static final StreamCodec<ByteBuf, NilumTcpOfferPayload> CODEC =
            RawByteArrayCodec.INSTANCE.map(NilumTcpOfferPayload::new, NilumTcpOfferPayload::data);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
