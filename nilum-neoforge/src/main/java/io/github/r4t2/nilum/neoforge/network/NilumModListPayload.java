package io.github.r4t2.nilum.neoforge.network;

import io.github.r4t2.nilum.common.protocol.NilumChannels;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Thin CustomPacketPayload wrapper around a raw {@code ModListPacket}. See {@link NilumHelloPayload}. */
public record NilumModListPayload(byte[] data) implements CustomPacketPayload {

    public static final Type<NilumModListPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(NilumChannels.NAMESPACE, NilumChannels.MOD_LIST));

    public static final StreamCodec<ByteBuf, NilumModListPayload> CODEC =
            RawByteArrayCodec.INSTANCE.map(NilumModListPayload::new, NilumModListPayload::data);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
