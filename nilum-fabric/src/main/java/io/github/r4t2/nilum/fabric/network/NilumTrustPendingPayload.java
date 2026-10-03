package io.github.r4t2.nilum.fabric.network;

import io.github.r4t2.nilum.common.protocol.NilumChannels;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Thin CustomPacketPayload wrapper around a raw TrustPendingPacket. See NilumHelloPayload. */
public record NilumTrustPendingPayload(byte[] data) implements CustomPacketPayload {

    public static final Type<NilumTrustPendingPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(NilumChannels.NAMESPACE, NilumChannels.TRUST_PENDING));

    public static final StreamCodec<ByteBuf, NilumTrustPendingPayload> CODEC =
            RawByteArrayCodec.INSTANCE.map(NilumTrustPendingPayload::new, NilumTrustPendingPayload::data);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
