package io.github.r4t2.nilum.fabric.network;

import io.github.r4t2.nilum.common.protocol.NilumChannels;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Thin CustomPacketPayload wrapper around a raw AtlasPatchPacket. See NilumHelloPayload. */
public record NilumAtlasPatchPayload(byte[] data) implements CustomPacketPayload {

    public static final Type<NilumAtlasPatchPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(NilumChannels.NAMESPACE, NilumChannels.ATLAS_PATCH));

    public static final StreamCodec<ByteBuf, NilumAtlasPatchPayload> CODEC =
            RawByteArrayCodec.INSTANCE.map(NilumAtlasPatchPayload::new, NilumAtlasPatchPayload::data);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
