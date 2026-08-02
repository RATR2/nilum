package io.github.r4t2.nilum.fabric.network;

import io.github.r4t2.nilum.common.protocol.NilumChannels;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Thin CustomPacketPayload wrapper around a raw HelloPacket. The field encoding lives in
 * nilum-common so the wire format is identical across platforms; this adapts it to Fabric's
 * payload registration API.
 */
public record NilumHelloPayload(byte[] data) implements CustomPacketPayload {

    public static final Type<NilumHelloPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(NilumChannels.NAMESPACE, NilumChannels.HELLO));

    public static final StreamCodec<ByteBuf, NilumHelloPayload> CODEC =
            RawByteArrayCodec.INSTANCE.map(NilumHelloPayload::new, NilumHelloPayload::data);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
