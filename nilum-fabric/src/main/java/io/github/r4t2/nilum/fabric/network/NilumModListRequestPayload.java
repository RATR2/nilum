package io.github.r4t2.nilum.fabric.network;

import io.github.r4t2.nilum.common.protocol.NilumChannels;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Thin CustomPacketPayload wrapper around a raw ModListRequestPacket. See NilumHelloPayload. */
public record NilumModListRequestPayload(byte[] data) implements CustomPacketPayload {

    public static final Type<NilumModListRequestPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(NilumChannels.NAMESPACE, NilumChannels.MOD_LIST_REQUEST));

    public static final StreamCodec<ByteBuf, NilumModListRequestPayload> CODEC =
            RawByteArrayCodec.INSTANCE.map(NilumModListRequestPayload::new, NilumModListRequestPayload::data);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
