package io.github.r4t2.nilum.neoforge.network;

import io.github.r4t2.nilum.common.protocol.NilumChannels;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Thin CustomPacketPayload wrapper around a raw packet. See NilumModelSpawnPayload. */
public record NilumSetClientVarPayload(byte[] data) implements CustomPacketPayload {

    public static final Type<NilumSetClientVarPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(NilumChannels.NAMESPACE, NilumChannels.SET_CLIENT_VAR));

    public static final StreamCodec<ByteBuf, NilumSetClientVarPayload> CODEC =
            RawByteArrayCodec.INSTANCE.map(NilumSetClientVarPayload::new, NilumSetClientVarPayload::data);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
